package enigma.redbeemedia.com.customcontrols;

import android.app.Application;
import com.redbeemedia.enigma.core.context.EnigmaRiverContext;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        String exposureBaseUrl = Placeholders.EXPOSURE_BASE_URL;
        EnigmaRiverContext.initialize(this, exposureBaseUrl);
    }
}
