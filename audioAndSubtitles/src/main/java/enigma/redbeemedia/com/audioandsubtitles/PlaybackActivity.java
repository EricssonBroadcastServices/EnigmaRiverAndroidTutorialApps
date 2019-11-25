package enigma.redbeemedia.com.audioandsubtitles;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.redbeemedia.enigma.core.error.AssetGeoBlockedError;
import com.redbeemedia.enigma.core.error.AssetNotAvailableError;
import com.redbeemedia.enigma.core.error.EnigmaError;
import com.redbeemedia.enigma.core.error.InvalidAssetError;
import com.redbeemedia.enigma.core.error.NoSupportedMediaFormatsError;
import com.redbeemedia.enigma.core.playable.AssetPlayable;
import com.redbeemedia.enigma.core.playable.IPlayable;
import com.redbeemedia.enigma.core.playbacksession.IPlaybackSessionListener;
import com.redbeemedia.enigma.core.player.EnigmaPlayer;
import com.redbeemedia.enigma.core.player.IEnigmaPlayer;
import com.redbeemedia.enigma.core.player.IPlayerImplementation;
import com.redbeemedia.enigma.core.playrequest.BasePlayResultHandler;
import com.redbeemedia.enigma.core.playrequest.IPlayRequest;
import com.redbeemedia.enigma.core.playrequest.PlayRequest;
import com.redbeemedia.enigma.core.session.ISession;
import com.redbeemedia.enigma.exoplayerintegration.ExoPlayerTech;

import enigma.redbeemedia.com.audioandsubtitles.ui.AbstractSpinner;


public class PlaybackActivity extends Activity {
    private static final String TAG = "PlaybackActivity";
    public static final String EXTRA_SESSION = "session";

    private Handler handler;
    private IEnigmaPlayer enigmaPlayer;
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

        //Create ExoPlayerTech and connect a compatible view.
        ExoPlayerTech exoPlayerTech = new ExoPlayerTech(this, "audioandsubtitles");
        exoPlayerTech.attachView(this.findViewById(R.id.player_view));

        //Create an EnigmaPlayer.
        enigmaPlayer = createEnigmaPlayer(session, exoPlayerTech, handler);

        ((AbstractSpinner<?>) findViewById(R.id.subtitle_spinner)).connectTo(enigmaPlayer);
        ((AbstractSpinner<?>) findViewById(R.id.audio_spinner)).connectTo(enigmaPlayer);
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

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}