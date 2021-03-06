package enigma.redbeemedia.com.customcontrols;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.redbeemedia.enigma.core.error.AssetGeoBlockedError;
import com.redbeemedia.enigma.core.error.AssetNotAvailableError;
import com.redbeemedia.enigma.core.error.EnigmaError;
import com.redbeemedia.enigma.core.error.InvalidAssetError;
import com.redbeemedia.enigma.core.error.NoSupportedMediaFormatsError;
import com.redbeemedia.enigma.core.playable.AssetPlayable;
import com.redbeemedia.enigma.core.playable.IPlayable;
import com.redbeemedia.enigma.core.playbacksession.BasePlaybackSessionListener;
import com.redbeemedia.enigma.core.playbacksession.IPlaybackSession;
import com.redbeemedia.enigma.core.playbacksession.IPlaybackSessionListener;
import com.redbeemedia.enigma.core.player.EnigmaPlayer;
import com.redbeemedia.enigma.core.player.EnigmaPlayerState;
import com.redbeemedia.enigma.core.player.IEnigmaPlayer;
import com.redbeemedia.enigma.core.player.IPlayerImplementation;
import com.redbeemedia.enigma.core.player.listener.BaseEnigmaPlayerListener;
import com.redbeemedia.enigma.core.playrequest.BasePlayResultHandler;
import com.redbeemedia.enigma.core.playrequest.IPlayRequest;
import com.redbeemedia.enigma.core.playrequest.PlayRequest;
import com.redbeemedia.enigma.core.session.ISession;
import com.redbeemedia.enigma.core.time.Duration;
import com.redbeemedia.enigma.core.virtualui.BaseVirtualButtonListener;
import com.redbeemedia.enigma.core.virtualui.IVirtualButton;
import com.redbeemedia.enigma.core.virtualui.IVirtualControls;
import com.redbeemedia.enigma.core.virtualui.IVirtualControlsSettings;
import com.redbeemedia.enigma.core.virtualui.VirtualControlsSettings;
import com.redbeemedia.enigma.core.virtualui.impl.VirtualControls;
import com.redbeemedia.enigma.exoplayerintegration.ExoPlayerTech;


public class PlaybackActivity extends Activity{
    private static final String TAG = "custom_controls";
    public static final String EXTRA_SESSION = "session";
    private static final Duration SEEK_JUMP = Duration.seconds(30);

    private Handler handler;
    private IEnigmaPlayer enigmaPlayer;
    private TimelineView timelineView;
    private ImageButton ibtnReset;
    private ImageButton ibtnSeekBack;
    private ImageButton ibtnSeekForward;
    private PausePlayImageButton ibtnPlayPause;
    private ImageButton ibtnStop;
    private ProgressBar pbLoader;
    private TextView txtIsLive;
    private IPlaybackSessionListener playbackSessionListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playback);

        //Create a Handler for the main thread.
        this.handler = new Handler();

        //Retrieve the session from the intent.
        Intent intent = getIntent();
        ISession session = intent.getParcelableExtra(EXTRA_SESSION);

        ibtnReset = this.findViewById(R.id.ibtnReset);
        ibtnSeekBack = this.findViewById(R.id.ibtnSeekBack);
        ibtnSeekForward = this.findViewById(R.id.ibtnSeekForward);
        ibtnPlayPause = this.findViewById(R.id.ibtnPlayPause);
        ibtnStop = this.findViewById(R.id.ibtnStop);
        pbLoader = this.findViewById(R.id.pbLoader);
        txtIsLive = this.findViewById(R.id.txtIsLive);
        timelineView = this.findViewById(R.id.timelineView);

        //Create ExoPlayerTech and connect a compatible view.
        ExoPlayerTech exoPlayerTech = new ExoPlayerTech(this, "customcontrols");
        exoPlayerTech.attachView(this.findViewById(R.id.player_view));
        exoPlayerTech.hideController();

        //Create an EnigmaPlayer.
        enigmaPlayer = createEnigmaPlayer(session, exoPlayerTech, handler);
        timelineView.connectTo(enigmaPlayer);
        ibtnPlayPause.connectTo(enigmaPlayer);

        //Hook up controls
        IVirtualControlsSettings virtualControlsSettings = new VirtualControlsSettings()
                .setSeekBackwardStep(SEEK_JUMP)
                .setSeekForwardStep(SEEK_JUMP);
        IVirtualControls virtualControls = VirtualControls.create(enigmaPlayer, virtualControlsSettings);

        bindButton(ibtnReset, virtualControls.getRestart());
        bindButton(ibtnSeekBack, virtualControls.getRewind());
        bindButton(ibtnSeekForward, virtualControls.getFastForward());

        ibtnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlaybackActivity.this.finish();
            }
        });

        playbackSessionListener = new BasePlaybackSessionListener(){
            @Override
            public void onPlayingFromLiveChanged(boolean live) {
                updateIsLive(live);
            }
        };

        enigmaPlayer.addListener(new BaseEnigmaPlayerListener(){
            @Override
            public void onStateChanged(EnigmaPlayerState from, EnigmaPlayerState to) {
                if (to == EnigmaPlayerState.LOADING || to == EnigmaPlayerState.BUFFERING){
                    pbLoader.setVisibility(View.VISIBLE);
                }else if (to == EnigmaPlayerState.LOADED || from == EnigmaPlayerState.BUFFERING){
                    pbLoader.setVisibility(View.GONE);
                }
            }

            @Override
            public void onPlaybackSessionChanged(IPlaybackSession from, IPlaybackSession to) {
                if(from != null) {
                    from.removeListener(playbackSessionListener);
                }
                if(to != null) {
                    updateIsLive(to.isPlayingFromLive());
                    to.addListener(playbackSessionListener, handler);
                }
            }
        }, handler);
    }

    private void bindButton(View button, IVirtualButton virtualButton) {
        button.setOnClickListener(v -> virtualButton.click());
        syncButtonState(button, virtualButton);

        virtualButton.addListener(new BaseVirtualButtonListener() {
            @Override
            public void onStateChanged() {
                syncButtonState(button, virtualButton);
            }
        }, handler);
    }

    private void syncButtonState(View button, IVirtualButton virtualButton) {
        button.setEnabled(virtualButton.isEnabled());
        button.setVisibility(virtualButton.isRelevant() ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Create a playable
        IPlayable playable = new AssetPlayable(Placeholders.ASSET_ID);

        //Create a play request
        IPlayRequest playRequest = new PlayRequest(playable, new BasePlayResultHandler() {
            @Override
            public void onError(EnigmaError error) {
                Log.e(TAG, "onError: " + error.getTrace());
                if(error instanceof AssetGeoBlockedError) {
                    showMessage("This asset it not available for your region");
                } else if(error instanceof AssetNotAvailableError) {
                    showMessage("This asset is not available");
                } else if(error instanceof NoSupportedMediaFormatsError) {
                    showMessage("This asset cannot be played on your device");
                } else if(error instanceof InvalidAssetError) {
                    showMessage("Could not find asset " + ((InvalidAssetError) error).getAssetId());
                } else {
                    showMessage("Could not start playback of asset");
                }
            }
        });

        //Start playback
        this.enigmaPlayer.play(playRequest);
    }


    private IEnigmaPlayer createEnigmaPlayer(ISession session, IPlayerImplementation playerImplementation, Handler callbackHandler) {
        EnigmaPlayer enigmaPlayer = new EnigmaPlayer(session, playerImplementation);
        enigmaPlayer.setActivity(this); //Binds the EnigmaPlayer to the lifecycle of this activity.
        enigmaPlayer.setCallbackHandler(callbackHandler);
        return enigmaPlayer;
    }

    private void updateIsLive(boolean isLive){
        if (isLive){
            txtIsLive.setText("Live");
            txtIsLive.setTextColor(Color.RED);
        }else{
            txtIsLive.setText("Not live");
            txtIsLive.setTextColor(Color.DKGRAY);
        }
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}