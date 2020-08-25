package enigma.redbeemedia.com.downloads.user;

import android.os.Handler;

import com.redbeemedia.enigma.core.error.EnigmaError;
import com.redbeemedia.enigma.core.error.ServerError;
import com.redbeemedia.enigma.core.error.UnexpectedHttpStatusError;
import com.redbeemedia.enigma.core.login.EnigmaLogin;
import com.redbeemedia.enigma.core.login.ILoginResultHandler;
import com.redbeemedia.enigma.core.login.UserLoginRequest;
import com.redbeemedia.enigma.core.session.ISession;
import com.redbeemedia.enigma.core.util.OpenContainer;
import com.redbeemedia.enigma.core.util.OpenContainerUtil;
import com.redbeemedia.enigma.exposureutils.BaseExposureResultHandler;
import com.redbeemedia.enigma.exposureutils.EnigmaExposure;
import com.redbeemedia.enigma.exposureutils.GetAvailabilityKeys;
import com.redbeemedia.enigma.exposureutils.models.availability.ApiAvailabilityKeys;

import enigma.redbeemedia.com.downloads.MyApplication;
import enigma.redbeemedia.com.downloads.Placeholders;

public class UserDataHolder {
    private static final OpenContainer<UserData> userData = new OpenContainer<>(null);

    public static UserData getUserData() {
        return OpenContainerUtil.getValueSynchronized(userData);
    }

    public static void login(ILoginResultHandler loginResultHandler, Handler handler) {
        EnigmaLogin enigmaLogin = new EnigmaLogin(MyApplication.getBusinessUnit());
        enigmaLogin.setCallbackHandler(handler);
        enigmaLogin.login(new UserLoginRequest(Placeholders.USERNAME, Placeholders.PASSWORD, new ILoginResultHandler() {

            @Override
            public void onSuccess(ISession session) {
                EnigmaExposure enigmaExposure = new EnigmaExposure(session);
                enigmaExposure.setCallbackHandler(handler);
                enigmaExposure.doRequest(new GetAvailabilityKeys(new BaseExposureResultHandler<ApiAvailabilityKeys>() {
                    @Override
                    public void onSuccess(ApiAvailabilityKeys result) {
                        UserData userData = new UserData(session, result.getAvailabilityKeys());
                        OpenContainerUtil.setValueSynchronized(UserDataHolder.userData, userData, null);
                        loginResultHandler.onSuccess(session);
                    }

                    @Override
                    public void onError(EnigmaError error) {
                        loginResultHandler.onError(error);
                    }
                }));
            }

            @Override
            public void onError(EnigmaError error) {
                loginResultHandler.onError(error);
            }
        }));
    }

    public static void logout() {
        OpenContainerUtil.setValueSynchronized(UserDataHolder.userData, null, null);
    }
}
