package enigma.redbeemedia.com.downloads.trackui;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import com.redbeemedia.enigma.core.audio.IAudioTrack;

public class AudioTrackSpinner extends AbstractSpinner<IAudioTrack> {
    public AudioTrackSpinner(Context context) {
        super(context, IAudioTrack.class);
    }

    public AudioTrackSpinner(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, IAudioTrack.class);
    }

    public AudioTrackSpinner(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr, IAudioTrack.class);
    }

    @Override
    protected ItemAdapter<IAudioTrack> wrap(IAudioTrack object) {
        return new ItemAdapter<IAudioTrack>(object) {
            @Override
            protected String getLabel(IAudioTrack obj) {
                return obj.getLanguageCode();
            }
        };
    }
}
