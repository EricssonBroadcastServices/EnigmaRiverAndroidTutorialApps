package enigma.redbeemedia.com.audioandsubtitles.ui;

import com.redbeemedia.enigma.core.audio.IAudioTrack;
import com.redbeemedia.enigma.core.playbacksession.BasePlaybackSessionListener;
import com.redbeemedia.enigma.core.playbacksession.IPlaybackSession;
import com.redbeemedia.enigma.core.player.controls.IEnigmaPlayerControls;
import com.redbeemedia.enigma.core.subtitle.ISubtitleTrack;

import java.util.Collections;
import java.util.List;

public abstract class TrackListener<T> extends BasePlaybackSessionListener {
    private static final int TYPE_AUDIO = 0;
    private static final int TYPE_SUBTITLE = 1;

    private final int type;

    public TrackListener(Class<T> trackType) {
        if(IAudioTrack.class.equals(trackType)) {
            this.type = TYPE_AUDIO;
        } else if(ISubtitleTrack.class.equals(trackType)) {
            this.type = TYPE_SUBTITLE;
        } else {
            throw new IllegalArgumentException(String.valueOf(trackType));
        }
    }

    @Override
    public void onSubtitleTracks(List<ISubtitleTrack> tracks) {
        if(type == TYPE_SUBTITLE) {
            onTracks((List<T>) tracks);
        }
    }

    @Override
    public void onSelectedSubtitleTrackChanged(ISubtitleTrack oldSelectedTrack, ISubtitleTrack newSelectedTrack) {
        if(type == TYPE_SUBTITLE) {
            onSelectedTrackChanged((T) oldSelectedTrack, (T) newSelectedTrack);
        }
    }

    @Override
    public void onAudioTracks(List<IAudioTrack> tracks) {
        if(type == TYPE_AUDIO) {
            onTracks((List<T>) tracks);
        }
    }

    @Override
    public void onSelectedAudioTrackChanged(IAudioTrack oldSelectedTrack, IAudioTrack newSelectedTrack) {
        if(type == TYPE_AUDIO) {
            onSelectedTrackChanged((T) oldSelectedTrack, (T) newSelectedTrack);
        }
    }


    public abstract void onTracks(List<T> tracks);

    public abstract void onSelectedTrackChanged(T oldSelectedTrack, T newSelectedTrack);

    public final List<T> getAvailableTracks(IPlaybackSession playbackSession) {
        if(type == TYPE_AUDIO) {
            return (List<T>) playbackSession.getAudioTracks();
        } else if(type == TYPE_SUBTITLE) {
            return (List<T>) playbackSession.getSubtitleTracks();
        } else {
            return Collections.emptyList();
        }
    }

    public final T getSelectedTrack(IPlaybackSession playbackSession) {
        if(type == TYPE_AUDIO) {
            return (T) playbackSession.getSelectedAudioTrack();
        } else if(type == TYPE_SUBTITLE) {
            return (T) playbackSession.getSelectedSubtitleTrack();
        } else {
            return null;
        }
    }

    public final void selectTrack(IEnigmaPlayerControls controls, T track) {
        if(type == TYPE_AUDIO) {
            controls.setAudioTrack((IAudioTrack) track);
        } else if(type == TYPE_SUBTITLE) {
            controls.setSubtitleTrack((ISubtitleTrack) track);
        }
    }
}
