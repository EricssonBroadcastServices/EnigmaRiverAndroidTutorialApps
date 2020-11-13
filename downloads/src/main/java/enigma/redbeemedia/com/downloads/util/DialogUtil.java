package enigma.redbeemedia.com.downloads.util;

import android.app.Activity;
import android.app.AlertDialog;

import com.redbeemedia.enigma.core.activity.AbstractActivityLifecycleListener;
import com.redbeemedia.enigma.core.context.EnigmaRiverContext;
import com.redbeemedia.enigma.core.error.EnigmaError;
import com.redbeemedia.enigma.core.error.UnexpectedHttpStatusError;
import com.redbeemedia.enigma.core.http.HttpStatus;
import com.redbeemedia.enigma.core.util.AndroidThreadUtil;

public class DialogUtil {
    public static void showError(Activity activity, String description, EnigmaError error) {
        error.printStackTrace();
        AndroidThreadUtil.runOnUiThread(() -> {
            if(activity.isFinishing() || activity.isDestroyed()) {
                return;
            }

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
            if(error instanceof UnexpectedHttpStatusError) {
                HttpStatus httpStatus = ((UnexpectedHttpStatusError) error).getHttpStatus();
                alertDialogBuilder.setTitle("Http error "+httpStatus);
            } else {
                alertDialogBuilder.setTitle("Error");
            }
            alertDialogBuilder.setMessage(description);
            alertDialogBuilder.setCancelable(false);
            alertDialogBuilder.setNeutralButton("Ok", (dialog, which) -> dialog.dismiss());
            AlertDialog dialog = alertDialogBuilder.show();
            EnigmaRiverContext.getActivityLifecycleManager().add(activity, new AbstractActivityLifecycleListener() {
                @Override
                public void onPause() {
                    dialog.dismiss();
                    EnigmaRiverContext.getActivityLifecycleManager().remove(activity, this);
                }
            });
        });
    }

    public static void showInfo(Activity activity, String title, String details) {
        AndroidThreadUtil.runOnUiThread(() -> {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
            alertDialogBuilder.setIcon(android.R.drawable.ic_dialog_info);
            alertDialogBuilder.setTitle(title);
            if(details != null) {
                alertDialogBuilder.setMessage(details);
            }
            alertDialogBuilder.setCancelable(false);
            alertDialogBuilder.setPositiveButton("Ok", (dialog, which) -> dialog.dismiss());
            AlertDialog dialog = alertDialogBuilder.show();
            EnigmaRiverContext.getActivityLifecycleManager().add(activity, new AbstractActivityLifecycleListener() {
                @Override
                public void onPause() {
                    dialog.dismiss();
                    EnigmaRiverContext.getActivityLifecycleManager().remove(activity, this);
                }
            });
        });
    }

    public static void showConfirm(Activity activity, String title, String details, Runnable onConfirm) {
        AndroidThreadUtil.runOnUiThread(() -> {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
            alertDialogBuilder.setIcon(android.R.drawable.ic_dialog_alert);
            alertDialogBuilder.setTitle(title);
            if(details != null) {
                alertDialogBuilder.setMessage(details);
            }
            alertDialogBuilder.setCancelable(true);
            alertDialogBuilder.setPositiveButton("Confirm", (dialog, which) -> {
                dialog.dismiss();
                onConfirm.run();
            });
            AlertDialog dialog = alertDialogBuilder.show();
            EnigmaRiverContext.getActivityLifecycleManager().add(activity, new AbstractActivityLifecycleListener() {
                @Override
                public void onPause() {
                    dialog.dismiss();
                    EnigmaRiverContext.getActivityLifecycleManager().remove(activity, this);
                }
            });
        });
    }
}
