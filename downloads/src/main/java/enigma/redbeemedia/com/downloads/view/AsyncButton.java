package enigma.redbeemedia.com.downloads.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.redbeemedia.enigma.core.util.AndroidThreadUtil;

import enigma.redbeemedia.com.downloads.R;

public class AsyncButton extends FrameLayout {

    private OnClickListener onClickListener = null;
    private boolean widgetEnabled = true;
    private boolean waiting = false;

    private Button button;
    private ProgressBar progressBar;

    public AsyncButton(@NonNull Context context) {
        super(context);
        init(null);
    }

    public AsyncButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public AsyncButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    protected void init(AttributeSet attributeSet) {
        inflate(getContext(), R.layout.view_async_button, this);

        this.button = findViewById(R.id.button);
        this.progressBar = findViewById(R.id.progressBar);

        button.setOnClickListener(v -> {
            if(onClickListener != null) {
                onClickListener.onClick(v);
            }
        });


        if(attributeSet != null) {
            TypedArray typedArray = getContext().obtainStyledAttributes(attributeSet, R.styleable.AsyncButton);
            try {
                String text = typedArray.getString(R.styleable.AsyncButton_text);
                button.setText(text);
            } finally {
                typedArray.recycle();
            }
        }

        updateChildren();
    }

    @Override
    public void setEnabled(boolean enabled) {
        AndroidThreadUtil.runOnUiThread(() -> {
            AsyncButton.this.widgetEnabled = enabled;
            updateChildren();
        });
    }

    public void setText(CharSequence text) {
        AndroidThreadUtil.runOnUiThread(() -> button.setText(text));
    }

    public void setText(int redId) {
        AndroidThreadUtil.runOnUiThread(() -> button.setText(redId));
    }

    private void updateChildren() {
        button.setEnabled(widgetEnabled && !waiting);
        progressBar.setVisibility((widgetEnabled && waiting) ? VISIBLE : INVISIBLE);
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener listener) {
        if(!isClickable()) {
            setClickable(true);
        }
        this.onClickListener = listener;
    }

    public void setWaiting(boolean waiting) {
        AndroidThreadUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AsyncButton.this.waiting = waiting;
                updateChildren();
            }
        });
    }
}
