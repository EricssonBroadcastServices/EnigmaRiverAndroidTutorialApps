package enigma.redbeemedia.com.downloads;

import android.app.Application;

import com.redbeemedia.enigma.core.businessunit.BusinessUnit;
import com.redbeemedia.enigma.core.businessunit.IBusinessUnit;
import com.redbeemedia.enigma.core.context.EnigmaRiverContext;

public class MyApplication extends Application {
    private static IBusinessUnit businessUnit = new BusinessUnit(Placeholders.CUSTOMER_NAME, Placeholders.BUSINESS_UNIT_NAME);

    @Override
    public void onCreate() {
        super.onCreate();
        String exposureBaseUrl = Placeholders.EXPOSURE_BASE_URL;
        EnigmaRiverContext.initialize(this, exposureBaseUrl);
    }

    public static IBusinessUnit getBusinessUnit() {
        return businessUnit;
    }
}
