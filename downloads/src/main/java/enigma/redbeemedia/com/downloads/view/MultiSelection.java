package enigma.redbeemedia.com.downloads.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import enigma.redbeemedia.com.downloads.R;

public class MultiSelection<T> extends RelativeLayout {
    private TextView value;
    private OptionsModel optionsModel = new OptionsModel(Collections.emptyList());
    private ISelectedChangedListener<T> selectedChangedListener = (list) -> {};

    public MultiSelection(@NonNull Context context) {
        super(context);
        init(null);
    }

    public MultiSelection(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public MultiSelection(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    protected void init(AttributeSet attributeSet) {
        inflate(getContext(), R.layout.view_multi_selection, this);

        this.value = findViewById(R.id.value);

        TextView label = findViewById(R.id.label);
        if(attributeSet != null) {
            TypedArray typedArray = getContext().obtainStyledAttributes(attributeSet, R.styleable.MultiSelection);
            try {
                String text = typedArray.getString(R.styleable.MultiSelection_text);
                label.setText(text);
            } finally {
                typedArray.recycle();
            }
        }

        setClickable(true);
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showSelectionDialog(getContext(), label.getText(), optionsModel);
            }
        });
    }

    private void showSelectionDialog(Context context, CharSequence title, OptionsModel optionsModel) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        final boolean[] tentativeSelection = optionsModel.createTentativeSelection();
        builder.setMultiChoiceItems(optionsModel.labels, tentativeSelection, (dialog, which, isChecked) -> {
            tentativeSelection[which] = isChecked;
        });
        builder.setPositiveButton("Confirm", (dialog, which) -> {
            optionsModel.confirm(tentativeSelection);
            List<T> selected = optionsModel.getSelected();
            selectedChangedListener.onSelectedChanged(selected);
            value.setText(optionsModel.getSelectedText());
            dialog.dismiss();
        });
        builder.show();
    }

    public void setOptions(List<OptionItem<T>> options, ISelectedChangedListener<T> selectedChangedListener) {
        this.optionsModel = new OptionsModel(options);
        this.selectedChangedListener = selectedChangedListener;
    }

    public static class OptionItem<T> {
        public final String label;
        public final T item;

        public OptionItem(String label, T item) {
            this.label = label;
            this.item = item;
        }
    }

    public interface ISelectedChangedListener<T> {
        void onSelectedChanged(List<T> selected);
    }

    private class OptionsModel {
        private String[] labels;
        private Object[] items;
        private boolean[] selected;

        public OptionsModel(List<OptionItem<T>> options) {
            this.labels = new String[options.size()];
            this.items = new Object[options.size()];
            this.selected = new boolean[options.size()];
            for(int i = 0; i < options.size(); ++i) {
                OptionItem<T> optionItem = options.get(i);
                labels[i] = optionItem.label;
                items[i] = optionItem.item;
            }
        }

        public List<T> getSelected() {
            List<T> list = new ArrayList<>();
            for(int i = 0; i < selected.length; ++i) {
                if(selected[i]) {
                    list.add((T) items[i]);
                }
            }
            return list;
        }

        public boolean[] createTentativeSelection() {
            return Arrays.copyOf(selected, selected.length);
        }

        public void confirm(boolean[] tentativeSelection) {
            System.arraycopy(tentativeSelection, 0, selected, 0, selected.length);
        }

        public String getSelectedText() {
            StringBuilder stringBuilder = new StringBuilder();
            boolean first = true;
            for(int i = 0; i < selected.length; ++i) {
                if(selected[i]) {
                    if(first) {
                        first = false;
                    } else {
                        stringBuilder.append("\n");
                    }
                    stringBuilder.append(labels[i]);
                }
            }
            return stringBuilder.toString();
        }
    }
}
