package enigma.redbeemedia.com.downloads;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.redbeemedia.enigma.core.error.EnigmaError;
import com.redbeemedia.enigma.download.DownloadStartRequest;
import com.redbeemedia.enigma.download.IEnigmaDownload;
import com.redbeemedia.enigma.download.assetdownload.IAssetDownload;
import com.redbeemedia.enigma.download.resulthandler.BaseDownloadStartResultHandler;
import com.redbeemedia.enigma.download.resulthandler.BaseResultHandler;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

public class DownloadBackgroundService extends Service {

    private static final String CHANNEL_ID = "Channel-id";
    private Handler handler;
    private Queue<DownloadStartRequest> downloadStartRequestQueue = new LinkedList<>();
    private IEnigmaDownload enigmaDownload = null;
    private Queue<Context> contexts = new LinkedList<>();
    private AtomicBoolean isDownloading = new AtomicBoolean(false);
    private static final int NOTIFICATION_ID = 123; // Unique ID for the notification
    private final IBinder binder = new DownloadServiceBinder();
    private AtomicBoolean isStarted = new AtomicBoolean();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        Notification notification = createNotification();
        startForeground(NOTIFICATION_ID, notification);
    }

    private Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Enigma Download")
                .setContentText("Enigma Download notification")
                .setSmallIcon(R.drawable.cast_ic_notification_small_icon)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // Create a notification channel for Android Oreo and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Download Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            // Register the notification channel with the system
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
        return builder.build();
    }

    public void addToDownloadQueue(IEnigmaDownload enigmaDownload, Context context, String assetUrl, DownloadStartRequest request) {
        downloadStartRequestQueue.add(request);
        contexts.add(context);
        if (isStarted.compareAndSet(false, true)) {
            this.enigmaDownload = enigmaDownload;
            startDownload();
        }
    }

    public class DownloadServiceBinder extends Binder {
        public DownloadBackgroundService getService() {
            return DownloadBackgroundService.this;
        }
    }

    private void startDownload() {
        new Thread() {
            @Override
            public void run() {
                // Download completed, process the next item in the queue

                while (true) {
                    System.out.println(" How many queue size : " + downloadStartRequestQueue.size());
                    // check if download is in progres
                    if (!isDownloading.getAndSet(true)) {
                        initializeDownload();
                    }

                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    enigmaDownload.getDownloadsInProgress(new BaseResultHandler<List<IAssetDownload>>() {
                        @Override
                        public void onResult(List<IAssetDownload> result) {
                            if (result.size() == 0) {
                                isDownloading.set(false);
                            }
                        }

                        @Override
                        public void onError(EnigmaError error) {
                        }
                    });

                }

            }
        }.start();
    }

    private void initializeDownload() {
        System.out.println("Starting a download");
        DownloadStartRequest downloadStartRequest = downloadStartRequestQueue.poll();
        if (downloadStartRequest != null) {
            Context context = contexts.poll();

            enigmaDownload.startAssetDownload(context, downloadStartRequest, new BaseDownloadStartResultHandler() {
                @Override
                public void onStarted() {
                }

                @Override
                public void onError(EnigmaError error) {
                }
            }, handler);
        }
    }


    public Queue<DownloadStartRequest> getDownloadStartRequestQueue() {
        return downloadStartRequestQueue;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
}
