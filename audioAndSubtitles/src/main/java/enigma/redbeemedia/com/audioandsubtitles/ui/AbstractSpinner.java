package enigma.redbeemedia.com.audioandsubtitles.ui;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Spinner;

import androidx.annotation.Nullable;

import com.redbeemedia.enigma.core.playbacksession.IPlaybackSession;
import com.redbeemedia.enigma.core.player.IEnigmaPlayer;
import com.redbeemedia.enigma.core.player.listener.BaseEnigmaPlayerListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class AbstractSpinner<T> extends androidx.appcompat.widget.AppCompatSpinner {
    private Handler handler;
    private Class<T> trackType;
    private BaseAdapter adapter;
    private final List<ItemAdapter<T>> currentTracks = new ArrayList<>();

    public AbstractSpinner(Context context, Class<T> trackType) {
        super(context);
        init(trackType);
    }

    public AbstractSpinner(Context context, @Nullable AttributeSet attrs, Class<T> trackType) {
        super(context, attrs);
        init(trackType);
    }

    public AbstractSpinner(Context context, @Nullable AttributeSet attrs, int defStyleAttr, Class<T> trackType) {
        super(context, attrs, defStyleAttr);
        init(trackType);
    }

    private void init(Class<T> trackType) {
        this.handler = new Handler();
        this.trackType = trackType;
    }


    public void connectTo(final IEnigmaPlayer enigmaPlayer) {
        final TrackListener<T> trackListener = new TrackListener<T>(trackType) {
            @Override
            public void onTracks(List<T> tracks) {
                //Update UI
                setTracks(tracks);
            }

            @Override
            public void onSelectedTrackChanged(T oldSelectedTrack, T newSelectedTrack) {
                //Update UI
                setSelectedTrack(newSelectedTrack);
            }
        };

        enigmaPlayer.addListener(new BaseEnigmaPlayerListener() {
            @Override
            public void onPlaybackSessionChanged(IPlaybackSession from, IPlaybackSession to) {
                if(from != null) {
                    //Remove listener from old PlaybackSession
                    from.removeListener(trackListener);
                }
                if(to != null) {
                    //Update UI
                    setTracks(trackListener.getAvailableTracks(to));
                    setSelectedTrack(trackListener.getSelectedTrack(to));

                    //Add listener to new PlaybackSession
                    to.addListener(trackListener, handler);
                }
            }
        }, handler);

        ArrayAdapter arrayAdapter = new ArrayAdapter(getContext(), android.R.layout.simple_spinner_item, currentTracks);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.adapter = arrayAdapter;
        setAdapter(adapter);

        setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                T selectedTrack;
                try {
                    selectedTrack = currentTracks.get(position).object;
                } catch (IndexOutOfBoundsException e) {
                    e.printStackTrace(); //Log and ignore.
                    return;
                }

                trackListener.selectTrack(enigmaPlayer.getControls(), selectedTrack);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                trackListener.selectTrack(enigmaPlayer.getControls(), null);
            }
        });
    }

    protected void setTracks(List<T> newTracks) {
        currentTracks.clear();
        for(T track : newTracks) {
            currentTracks.add(wrap(track));
        }
        adapter.notifyDataSetChanged();
    }

    private void setSelectedTrack(T selected) {
        int position = Spinner.INVALID_POSITION;
        for(int i = 0; i < currentTracks.size(); ++i) {
            ItemAdapter<T> itemAdapter = currentTracks.get(i);
            if(Objects.equals(itemAdapter.object, selected)) {
                position = i;
                break;
            }
        }
        setSelection(position);
        adapter.notifyDataSetChanged();
    }

    protected abstract ItemAdapter<T> wrap(T object);
}
