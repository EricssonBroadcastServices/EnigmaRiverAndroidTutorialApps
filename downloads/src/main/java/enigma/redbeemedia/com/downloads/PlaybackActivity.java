package enigma.redbeemedia.com.downloads;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import com.redbeemedia.enigma.core.error.AssetGeoBlockedError;
import com.redbeemedia.enigma.core.error.AssetNotAvailableError;
import com.redbeemedia.enigma.core.error.EnigmaError;
import com.redbeemedia.enigma.core.error.InvalidAssetError;
import com.redbeemedia.enigma.core.error.NoSupportedMediaFormatsError;
import com.redbeemedia.enigma.core.playable.IPlayable;
import com.redbeemedia.enigma.core.playbacksession.BasePlaybackSessionListener;
import com.redbeemedia.enigma.core.playbacksession.IPlaybackSession;
import com.redbeemedia.enigma.core.player.EnigmaPlayer;
import com.redbeemedia.enigma.core.player.IEnigmaPlayer;
import com.redbeemedia.enigma.core.player.IPlayerImplementation;
import com.redbeemedia.enigma.core.player.listener.BaseEnigmaPlayerListener;
import com.redbeemedia.enigma.core.playrequest.BasePlayResultHandler;
import com.redbeemedia.enigma.core.playrequest.IPlayRequest;
import com.redbeemedia.enigma.core.playrequest.PlayRequest;
import com.redbeemedia.enigma.core.session.ISession;
import com.redbeemedia.enigma.exoplayerintegration.ExoPlayerTech;

import enigma.redbeemedia.com.downloads.trackui.AbstractSpinner;
import enigma.redbeemedia.com.downloads.user.UserData;
import enigma.redbeemedia.com.downloads.user.UserDataHolder;

public class PlaybackActivity extends Activity{
    private static final String EXTRA_PLAYABLE = "playable";

    private Handler handler;
    private IEnigmaPlayer enigmaPlayer;
    private IPlayable playable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playback);

        //Create a Handler for the main thread.
        this.handler = new Handler();

        //Retrieve the session from the intent.
        Intent intent = getIntent();
        this.playable = intent.getParcelableExtra(EXTRA_PLAYABLE);

        //Create ExoPlayerTech and connect a compatible view.
        ExoPlayerTech exoPlayerTech = new ExoPlayerTech(this, "yourFirstApp");
        exoPlayerTech.attachView(findViewById(R.id.player_view));

        //Create an EnigmaPlayer.
        this.enigmaPlayer = createEnigmaPlayer(exoPlayerTech, handler);

        ((AbstractSpinner<?>) findViewById(R.id.subtitle_spinner)).connectTo(enigmaPlayer);
        ((AbstractSpinner<?>) findViewById(R.id.audio_spinner)).connectTo(enigmaPlayer);

        //Close activity when finished
        enigmaPlayer.addListener(new BaseEnigmaPlayerListener() {
            @Override
            public void onPlaybackSessionChanged(IPlaybackSession from, IPlaybackSession to) {
                if(to != null) {
                    to.addListener(new BasePlaybackSessionListener() {
                        @Override
                        public void onEndReached() {
                            PlaybackActivity.this.finish();
                        }
                    }, handler);
                }
            }
        });
    }

    private IEnigmaPlayer createEnigmaPlayer(IPlayerImplementation playerImplementation, Handler callbackHandler) {
        EnigmaPlayer enigmaPlayer = new EnigmaPlayer(MyApplication.getBusinessUnit(), playerImplementation);
        enigmaPlayer.setActivity(this); //Binds the EnigmaPlayer to the lifecycle of this activity.
        enigmaPlayer.setCallbackHandler(callbackHandler);
        return enigmaPlayer;
    }

    @Override
    protected void onResume() {
        super.onResume();

        ISession session = null;

        UserData userData = UserDataHolder.getUserData();
        if(userData != null) {
            session = userData.getSession();
        }

        //Create a play request
        IPlayRequest playRequest = new PlayRequest(session, playable, new BasePlayResultHandler() {
            @Override
            public void onError(EnigmaError error) {
                if(error instanceof AssetGeoBlockedError) {
                    showMessage("This asset it not available for your region");
                } else if(error instanceof AssetNotAvailableError) {
                    showMessage("This asset is not available");
                } else if(error instanceof NoSupportedMediaFormatsError) {
                    showMessage("This asset cannot be played on your device");
                } else if(error instanceof InvalidAssetError) {
                    showMessage("Could not find asset "+((InvalidAssetError) error).getAssetId());
                } else {
                    showMessage("Could not start playback of asset");
                }
            }
        });

        //Start playback
        this.enigmaPlayer.play(playRequest);
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    public static void startActivity(Context context, IPlayable playable) {
        Intent intent = new Intent(context, PlaybackActivity.class);
        intent.putExtra(PlaybackActivity.EXTRA_PLAYABLE, playable);
        context.startActivity(intent);
    }
}