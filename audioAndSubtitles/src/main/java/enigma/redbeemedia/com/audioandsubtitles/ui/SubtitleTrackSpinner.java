package enigma.redbeemedia.com.audioandsubtitles.ui;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import com.redbeemedia.enigma.core.subtitle.ISubtitleTrack;

import java.util.ArrayList;
import java.util.List;

import enigma.redbeemedia.com.audioandsubtitles.R;

public class SubtitleTrackSpinner extends AbstractSpinner<ISubtitleTrack> {
    public SubtitleTrackSpinner(Context context) {
        super(context, ISubtitleTrack.class);
    }

    public SubtitleTrackSpinner(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, ISubtitleTrack.class);
    }

    public SubtitleTrackSpinner(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr, ISubtitleTrack.class);
    }

    @Override
    protected void setTracks(List<ISubtitleTrack> newTracks) {
        List<ISubtitleTrack> newTracksWithNull = new ArrayList<>();
        newTracksWithNull.add(null); //Option for no subtitles
        newTracksWithNull.addAll(newTracks);
        super.setTracks(newTracksWithNull);
    }

    @Override
    protected ItemAdapter<ISubtitleTrack> wrap(ISubtitleTrack object) {
        return new ItemAdapter<ISubtitleTrack>(object) {
            @Override
            protected String getLabel(ISubtitleTrack obj) {
                if(obj == null) {
                    return getResources().getString(R.string.no_subtitles);
                } else {
                    return obj.getLabel();
                }
            }
        };
    }
}
