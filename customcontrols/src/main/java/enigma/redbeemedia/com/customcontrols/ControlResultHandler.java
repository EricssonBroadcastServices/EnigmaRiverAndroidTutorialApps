package enigma.redbeemedia.com.customcontrols;

import android.util.Log;

import com.redbeemedia.enigma.core.error.Error;
import com.redbeemedia.enigma.core.player.controls.IControlResultHandler;

public class ControlResultHandler implements IControlResultHandler {
    private final String logTag;
    private final String controlType;

    public ControlResultHandler(String logTag, String controlType) {
        this.logTag = logTag;
        this.controlType = controlType;
    }

    @Override
    public void onRejected(IRejectReason reason) {
        Log.d(logTag, controlType + " onRejected reason details: " + reason.getDetails());
    }

    @Override
    public void onCancelled() {
        Log.d(logTag, controlType + " onCancelled");
    }

    @Override
    public void onError(Error error) {
        Log.d(logTag, controlType + " onError: " + error.toString());
    }

    @Override
    public void onDone() {
        Log.d(logTag, controlType + " onDone");
    }
}