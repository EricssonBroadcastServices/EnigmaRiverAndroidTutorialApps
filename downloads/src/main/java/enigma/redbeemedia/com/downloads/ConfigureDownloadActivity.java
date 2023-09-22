package enigma.redbeemedia.com.downloads;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.util.Log;
import com.redbeemedia.enigma.core.error.EnigmaError;
import com.redbeemedia.enigma.core.util.AndroidThreadUtil;
import com.redbeemedia.enigma.download.AudioDownloadable;
import com.redbeemedia.enigma.download.DownloadStartRequest;
import com.redbeemedia.enigma.download.EnigmaDownload;
import com.redbeemedia.enigma.download.IDownloadableInfo;
import com.redbeemedia.enigma.download.IDownloadablePart;
import com.redbeemedia.enigma.download.IEnigmaDownload;
import com.redbeemedia.enigma.download.SubtitleDownloadable;
import com.redbeemedia.enigma.download.VideoDownloadable;
import com.redbeemedia.enigma.download.resulthandler.BaseResultHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import enigma.redbeemedia.com.downloads.user.UserData;
import enigma.redbeemedia.com.downloads.user.UserDataHolder;
import enigma.redbeemedia.com.downloads.util.DialogUtil;
import enigma.redbeemedia.com.downloads.view.AsyncButton;
import enigma.redbeemedia.com.downloads.view.MultiSelection;

public class ConfigureDownloadActivity extends AppCompatActivity {
    private static final String EXTRA_ASSET_ID = "assetId";

    private final IEnigmaDownload enigmaDownload = new EnigmaDownload(MyApplication.getBusinessUnit());


    private Handler handler;
    private DownloadStartRequest downloadStartRequest;
    private AsyncButton downloadButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_configure_download);

        this.handler = new Handler();

        String assetId = getIntent().getStringExtra(EXTRA_ASSET_ID);
        if(assetId == null) {
            finishAndToast("No asset id");
            return;
        }

        UserData userData = UserDataHolder.getUserData();

        this.downloadButton = findViewById(R.id.downloadButton);
        downloadButton.setEnabled(false);

        if(userData != null) {
            downloadStartRequest = new DownloadStartRequest(assetId, userData.getSession());

            Spinner videoTrackSpinner = findViewById(R.id.videoTrackSpinner);
            DownloadablePartsSpinnerAdapter<VideoDownloadable> videoTrackSpinnerAdapter = new DownloadablePartsSpinnerAdapter<>();
            videoTrackSpinner.setAdapter(videoTrackSpinnerAdapter);
            videoTrackSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    VideoDownloadable videoDownloadable = videoTrackSpinnerAdapter.getItem(position);
                    downloadStartRequest.setVideo(videoDownloadable);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    downloadStartRequest.setVideo(null);
                }
            });

            ScrollView scrollView = findViewById(R.id.scrollView);
            View loadOptionsProgressBar = findViewById(R.id.loadOptionsProgressBar);
            scrollView.setVisibility(View.GONE);
            loadOptionsProgressBar.setVisibility(View.VISIBLE);
            enigmaDownload.getDownloadableInfo(assetId, userData.getSession(), new BaseResultHandler<IDownloadableInfo>() {
                @Override
                public void onResult(IDownloadableInfo result) {
                    loadOptionsProgressBar.setVisibility(View.GONE);

                    List<VideoDownloadable> videoOptions = new ArrayList<>();
                    videoOptions.add(null); //Auto
                    videoOptions.addAll(result.getVideoTracks());
                    videoTrackSpinnerAdapter.setDownloadableParts(videoOptions);

                    MultiSelection<AudioDownloadable> audioTrackSelection = findViewById(R.id.audioTrackSelection);
                    audioTrackSelection.setOptions(createOptions(result.getAudioTracks(), (track) -> track.getName()), selected -> {
                        downloadStartRequest.setAudios(selected);
                    });

                    MultiSelection<SubtitleDownloadable> subtitleTrackSelection = findViewById(R.id.subtitleTrackSelection);
                    subtitleTrackSelection.setOptions(createOptions(result.getSubtitleTracks(), (track) -> track.getName()), selected -> {
                        downloadStartRequest.setSubtitles(selected);
                    });

                    updateDownloadCountText(result);

                    if(result.isMaxDownloadCountReached()) {
                        downloadButton.setEnabled(false);
                        downloadButton.setText("Max download count reached");
                    } else {
                        downloadButton.setEnabled(true);
                        downloadButton.setOnClickListener(v -> startDownload());
                    }
                    scrollView.setVisibility(View.VISIBLE);
                }

                @Override
                public void onError(EnigmaError error) {
                    loadOptionsProgressBar.setVisibility(View.GONE);
                    showError("Could not get download options", error);
                }
            }, handler);
        } else {
            finishAndToast("Please sign in");
        }
    }

    private void updateDownloadCountText(IDownloadableInfo downloadableInfo) {
        AndroidThreadUtil.runOnUiThread(() -> {
            TextView downloadCountText = findViewById(R.id.downloadCountText);
            int downloadCount = downloadableInfo.getDownloadCount();
            int maxDownloadCount = downloadableInfo.getMaxDownloadCount();
            if(downloadCount != IDownloadableInfo.UNAVAILABLE_INT
               && maxDownloadCount != IDownloadableInfo.UNAVAILABLE_INT) {
                downloadCountText.setVisibility(View.VISIBLE);
                downloadCountText.setText("Asset downloaded "+downloadCount+"/"+maxDownloadCount+" times");
            } else {
                downloadCountText.setVisibility(View.GONE);
            }
        });
    }

    private void startDownload() {
        Intent serviceIntent = new Intent(this, DownloadBackgroundService.class);
        //startForegroundService(serviceIntent);
        startService(serviceIntent); // Start the service if it's not already running
        downloadButton.setWaiting(true);
        ConfigureDownloadActivity thisC = this;
        ServiceConnection serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                // Add download to the queue
                Log.d(DOWNLOAD_SERVICE,"Added asset to the queue");
                DownloadBackgroundService.DownloadServiceBinder binder = (DownloadBackgroundService.DownloadServiceBinder) service;
                DownloadBackgroundService downloadService = binder.getService();

                // Add assets to the download queue
                downloadService.addToDownloadQueue(enigmaDownload, thisC, "", downloadStartRequest);
                // Add more assets as needed
                //downloadButton.setWaiting(false);
                Toast.makeText(getApplicationContext(), "Download is in queue and it will start shortly.", Toast.LENGTH_SHORT).show();

                finish();
                //ListDownloadsActivity.startActivity(ConfigureDownloadActivity.this);

            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        };

        serviceIntent.setAction("enigma.DOWNLOAD_SERVICE_ACTION"); // Set the action to match the one declared in the manifest
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void finishAndToast(String message) {
        AndroidThreadUtil.runOnUiThread(() -> {
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void showError(String description, EnigmaError error) {
        DialogUtil.showError(this, description, error);
    }

    private void showInfo(String title, String details) {
        DialogUtil.showInfo(this, title, details);
    }

    public static void startActivity(Context context, String assetId) {
        Intent intent = new Intent(context, ConfigureDownloadActivity.class);
        intent.putExtra(EXTRA_ASSET_ID, assetId);
        context.startActivity(intent);
    }

    private static class DownloadablePartsSpinnerAdapter<T extends IDownloadablePart> extends BaseAdapter {
        private List<T> downloadableParts = new ArrayList<>();

        @Override
        public int getCount() {
            return downloadableParts.size();
        }

        @Override
        public T getItem(int position) {
            return downloadableParts.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public void setDownloadableParts(List<? extends T> downloadableParts) {
            this.downloadableParts.clear();
            this.downloadableParts.addAll(downloadableParts);
            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView textView = new TextView(parent.getContext());
            T item = getItem(position);
            textView.setText(item != null ? item.getName() : "Auto");
            return textView;
        }
    }

    private static <T> List<MultiSelection.OptionItem<T>> createOptions(Collection<? extends T> items, IToStringFunction<T> getLabel) {
        List<MultiSelection.OptionItem<T>> options = new ArrayList<>();
        for(T item : items) {
            options.add(new MultiSelection.OptionItem<>(getLabel.toString(item), item));
        }
        return options;
    }

    private interface IToStringFunction<T> {
        String toString(T item);
    }
}
