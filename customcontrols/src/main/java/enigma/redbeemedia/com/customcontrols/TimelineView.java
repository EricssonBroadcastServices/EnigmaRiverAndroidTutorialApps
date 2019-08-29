package enigma.redbeemedia.com.customcontrols;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.redbeemedia.enigma.core.player.IEnigmaPlayer;
import com.redbeemedia.enigma.core.player.controls.IEnigmaPlayerControls;
import com.redbeemedia.enigma.core.player.timeline.BaseTimelineListener;
import com.redbeemedia.enigma.core.player.timeline.ITimelinePosition;
import com.redbeemedia.enigma.core.player.timeline.TimelinePositionFormat;
import com.redbeemedia.enigma.core.time.Duration;

import java.text.SimpleDateFormat;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public class TimelineView extends View {
    private static final String TAG = "custom_controls";

    private Paint paint;
    private int timelinePad = 15;

    private ITimelinePosition start;
    private ITimelinePosition end;
    private ITimelinePosition pos;

    private TimelinePositionFormat timelinePositionFormat = TimelinePositionFormat.newFormat("${minutes}m${sec}s", new SimpleDateFormat("hh:mm:ss a"));

    private double currentPos;
    private IEnigmaPlayerControls controls;
    private Handler handler;

    public TimelineView(Context context) {
        super(context);
        init();
    }

    public TimelineView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TimelineView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public TimelineView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    protected void init() {
        this.handler = new Handler();
        setWillNotDraw(false);
        this.paint = new Paint();
        paint.setColor(Color.RED);
        if(isInEditMode()) {
            currentPos = (float) Math.random();
        }
        setClickable(true);
        setVisibility(INVISIBLE);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getActionMasked() == MotionEvent.ACTION_UP) {
            float relativeClick = ((float) (event.getX()-timelinePad))/(getMeasuredWidth()-timelinePad*2);
            relativeClick = relativeClick < 0 ? 0f : (relativeClick > 1 ? 1f : relativeClick);
            if(start != null && end != null) {
                Duration durationAfterStart = end.subtract(start).multiply(relativeClick);
                ITimelinePosition seekPosition = start.add(durationAfterStart);
                controls.seekTo(seekPosition);
            }
        }
        return super.onTouchEvent(event);
    }

    public void connectTo(IEnigmaPlayer enigmaPlayer) {
        this.controls = enigmaPlayer.getControls();
        enigmaPlayer.getTimeline().addListener(new BaseTimelineListener() {
            @Override
            public void onVisibilityChanged(boolean visible) {
                TimelineView.this.setVisibility(visible ? VISIBLE: INVISIBLE);
            }

            private void recalculatePos() {
                if(start != null && end != null && pos != null) {
                    TimelineView.this.currentPos = pos.subtract(start).inUnits(Duration.Unit.MILLISECONDS)/end.subtract(start).inUnits(Duration.Unit.MILLISECONDS);
                } else {
                    TimelineView.this.currentPos = 0f;
                }
                TimelineView.this.postInvalidate();
            }

            @Override
            public void onCurrentPositionChanged(ITimelinePosition timelinePosition) {
                TimelineView.this.pos = timelinePosition;
                recalculatePos();
            }

            @Override
            public void onBoundsChanged(ITimelinePosition start, ITimelinePosition end) {
                TimelineView.this.start = start;
                TimelineView.this.end = end;
                recalculatePos();
            }
        }, handler);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //draw the whole bar
        paint.setColor(Color.BLACK);
        canvas.drawRect(canvas.getClipBounds(),paint);

        //draw the progress
        Rect rect = new Rect();
        rect.set(timelinePad ,timelinePad, ((int) ((getMeasuredWidth()-2*timelinePad)*currentPos)+timelinePad), getMeasuredHeight()-timelinePad);
        paint.setColor(Color.GREEN);
        float cornerRadius = timelinePad;
        canvas.drawRoundRect(new RectF(rect), cornerRadius, cornerRadius, paint);

        //draw time bounds
        paint.setColor(Color.WHITE);
        paint.setTextSize(1.5f*(getMeasuredHeight()-timelinePad*2)/2);
        float fontPad = (getMeasuredHeight()-paint.getFontMetrics(null)*0.8f)/2;
        if(pos != null && end != null) {
            String text = pos.toString(timelinePositionFormat)+" / "+end.toString(timelinePositionFormat);
            canvas.drawText(text, rect.left+(getMeasuredWidth()-timelinePad*2-paint.measureText(text))/2, getMeasuredHeight()-fontPad, paint);
        }
    }
}