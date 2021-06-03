package enigma.redbeemedia.com.customcontrols;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.os.Handler;

import com.redbeemedia.enigma.core.playbacksession.IPlaybackSession;
import com.redbeemedia.enigma.core.player.EnigmaPlayerState;
import com.redbeemedia.enigma.core.player.IEnigmaPlayer;
import com.redbeemedia.enigma.core.player.controls.IEnigmaPlayerControls;
import com.redbeemedia.enigma.core.player.listener.BaseEnigmaPlayerListener;

public class PausePlayImageButton extends androidx.appcompat.widget.AppCompatImageButton {

    private static final String TAG = "custom_controls";

    private IEnigmaPlayer enigmaPlayer;
    private Handler handler;
    private boolean usingPauseButton;

    public PausePlayImageButton(Context context) {
        super(context);
        init();
    }

    public PausePlayImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PausePlayImageButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    protected void init() {
        this.handler = new Handler();
    }

    public void connectTo(IEnigmaPlayer enigmaPlayer){
        this.enigmaPlayer = enigmaPlayer;
        this.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (usingPauseButton){
                    enigmaPlayer.getControls().pause(new ControlResultHandler(TAG, "pause"));
                }else {
                    enigmaPlayer.getControls().start(new ControlResultHandler(TAG, "play"));
                }
            }
        });

        this.enigmaPlayer.addListener(new BaseEnigmaPlayerListener(){
            @Override
            public void onStateChanged(EnigmaPlayerState from, EnigmaPlayerState to) {
                if (to == EnigmaPlayerState.PLAYING){
                    usingPauseButton = true;
                    updatePlayPauseButtonImage();
                }else if(from == EnigmaPlayerState.PLAYING){
                    usingPauseButton = false;
                    updatePlayPauseButtonImage();
                }
            }
        }, handler);
    }

    private void updatePlayPauseButtonImage() {
        int icon = usingPauseButton ? R.drawable.exo_icon_pause : R.drawable.exo_icon_play;
        this.setImageResource(icon);
    }
}
