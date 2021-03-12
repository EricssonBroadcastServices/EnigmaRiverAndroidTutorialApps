package enigma.redbeemedia.com.downloads;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.redbeemedia.enigma.core.error.EnigmaError;
import com.redbeemedia.enigma.core.error.MaxDownloadCountLimitReachedError;
import com.redbeemedia.enigma.core.player.controls.IControlResultHandler;
import com.redbeemedia.enigma.core.session.ISession;
import com.redbeemedia.enigma.core.util.AndroidThreadUtil;
import com.redbeemedia.enigma.download.DownloadedPlayable;
import com.redbeemedia.enigma.download.EnigmaDownload;
import com.redbeemedia.enigma.download.IDrmLicence;
import com.redbeemedia.enigma.download.assetdownload.AssetDownloadState;
import com.redbeemedia.enigma.download.assetdownload.IAssetDownload;
import com.redbeemedia.enigma.download.listener.BaseAssetDownloadListener;
import com.redbeemedia.enigma.download.listener.IAssetDownloadListener;
import com.redbeemedia.enigma.download.resulthandler.BaseDrmLicenceRenewResultHandler;
import com.redbeemedia.enigma.download.resulthandler.BaseResultHandler;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import enigma.redbeemedia.com.downloads.user.UserData;
import enigma.redbeemedia.com.downloads.user.UserDataHolder;
import enigma.redbeemedia.com.downloads.util.DialogUtil;
import enigma.redbeemedia.com.downloads.view.AsyncButton;

public class ListDownloadsActivity extends Activity {
    private Handler handler;
    private EnigmaDownload enigmaDownload;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_list_downloads);

        this.handler = new Handler();

        ProgressBar progressBar = findViewById(R.id.pageProgressBar);
        progressBar.setVisibility(View.VISIBLE);


        this.enigmaDownload = new EnigmaDownload(MyApplication.getBusinessUnit());
        enigmaDownload.getDownloadedAssets(new BaseResultHandler<List<DownloadedPlayable>>() {
            @Override
            public void onResult(List<DownloadedPlayable> result) {
                progressBar.setVisibility(View.INVISIBLE);
                ViewGroup viewGroup = findViewById(R.id.downloadedAssetsList);
                for(DownloadedPlayable playable : result) {
                    createDownloadedCardView(playable, viewGroup);
                }
                viewGroup.setLayoutTransition(new LayoutTransition());
            }

            @Override
            public void onError(EnigmaError error) {
                progressBar.setVisibility(View.INVISIBLE);
                showError("Could not get downloaded assets", error);
            }
        }, handler);

        enigmaDownload.getDownloadsInProgress(new BaseResultHandler<List<IAssetDownload>>() {
            @Override
            public void onResult(List<IAssetDownload> result) {
                progressBar.setVisibility(View.INVISIBLE);
                ViewGroup viewGroup = findViewById(R.id.downloadsInProgressList);
                for(IAssetDownload assetDownload : result) {
                    createDownloadInProgressCardView(assetDownload, viewGroup);
                }
                viewGroup.setLayoutTransition(new LayoutTransition());
            }

            @Override
            public void onError(EnigmaError error) {
                progressBar.setVisibility(View.INVISIBLE);
                showError("Could not get downloads in progress", error);
            }
        }, handler);
    }

    private void createDownloadedCardView(DownloadedPlayable downloadedPlayable, ViewGroup viewGroup) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.card_downloaded, viewGroup, false);


        TextView assetIdView = cardView.findViewById(R.id.assetId);
        assetIdView.setText(downloadedPlayable.getAssetId());

        if (downloadedPlayable.getFileSize() > 0) {
            TextView fileSizeView = cardView.findViewById(R.id.fileSize);
            fileSizeView.setText("Size: " + new DecimalFormat("###.#").format(((downloadedPlayable.getFileSize() / 1024f) / 1024f)) + " Mb");
        }

        Button playButton = cardView.findViewById(R.id.playButton);
        playButton.setOnClickListener(v -> PlaybackActivity.startActivity(ListDownloadsActivity.this, downloadedPlayable));

        AsyncButton deleteButton = cardView.findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(v -> {
            deleteButton.setWaiting(true);
            enigmaDownload.removeDownloadedAsset(downloadedPlayable, new BaseResultHandler<Void>() {
                @Override
                public void onResult(Void result) {
                    deleteButton.setWaiting(false);
                    viewGroup.removeView(cardView);
                }

                @Override
                public void onError(EnigmaError error) {
                    deleteButton.setWaiting(false);
                    showError("Failed to delete downloaded asset", error);
                }
            }, handler);
        });

        View drmDataContainer = cardView.findViewById(R.id.drmDataContainer);
        IDrmLicence drmLicence = downloadedPlayable.getDrmLicence();
        if(drmLicence != null) {
            drmDataContainer.setVisibility(View.VISIBLE);
            setupDrmInfo(drmDataContainer, drmLicence);
        } else {
            drmDataContainer.setVisibility(View.GONE);
        }

        viewGroup.addView(cardView);
    }

    private void createDownloadInProgressCardView(IAssetDownload assetDownload, ViewGroup viewGroup) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.card_download_in_progress, viewGroup, false);


        TextView assetIdView = cardView.findViewById(R.id.assetId);
        assetIdView.setText(assetDownload.getAssetId());

        TextView downloadState = cardView.findViewById(R.id.downloadState);
        downloadState.setText(assetDownload.getState().name());
        assetDownload.addListener(new BaseAssetDownloadListener() {
            @Override
            public void onStateChanged(AssetDownloadState oldState, AssetDownloadState newState) {
                downloadState.setText(newState.name());
            }
        }, handler);

        ProgressBar downloadProgress = cardView.findViewById(R.id.downloadProgress);
        downloadProgress.setIndeterminate(false);
        downloadProgress.setProgress((int) (assetDownload.getProgress() * downloadProgress.getMax()));
        downloadProgress.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            private final IAssetDownloadListener listener = new BaseAssetDownloadListener() {
                @Override
                public void onProgressChanged(float oldProgress, float newProgress) {
                    if (downloadProgress.isAttachedToWindow()) {
                        downloadProgress.setProgress((int) (newProgress * downloadProgress.getMax()));
                        downloadProgress.setIndeterminate(false);
                    }
                }
            };

            @Override
            public void onViewAttachedToWindow(View v) {
                assetDownload.addListener(listener, handler);
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                assetDownload.removeListener(listener);
            }
        });

        AsyncButton pauseButton = cardView.findViewById(R.id.pauseButton);
        pauseButton.setOnClickListener(v -> {
            pauseButton.setWaiting(true);
            assetDownload.pauseDownload(new IControlResultHandler() {
                @Override
                public void onRejected(IRejectReason reason) {
                    showInfo("Pause rejected", reason.getDetails());
                    pauseButton.setWaiting(false);
                }

                @Override
                public void onCancelled() {
                    pauseButton.setWaiting(false);
                }

                @Override
                public void onError(EnigmaError error) {
                    pauseButton.setWaiting(false);
                    showError("Failed to pause download", error);
                }

                @Override
                public void onDone() {
                    showInfo("Download paused!", null);
                    pauseButton.setWaiting(false);
                }
            });
        });

        AsyncButton resumeButton = cardView.findViewById(R.id.resumeButton);
        resumeButton.setOnClickListener(v -> {
            resumeButton.setWaiting(true);
            assetDownload.resumeDownload(new IControlResultHandler() {
                @Override
                public void onRejected(IRejectReason reason) {
                    showInfo("Resume rejected", reason.getDetails());
                    resumeButton.setWaiting(false);
                }

                @Override
                public void onCancelled() {
                    resumeButton.setWaiting(false);
                }

                @Override
                public void onError(EnigmaError error) {
                    resumeButton.setWaiting(false);
                    showError("Failed to resume download", error);
                }

                @Override
                public void onDone() {
                    showInfo("Download resumed!", null);
                    resumeButton.setWaiting(false);
                }
            });
        });
        AsyncButton cancelButton = cardView.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(v -> {
            DialogUtil.showConfirm(
                    this,
                    "Cancel download?",
                    "This will remove all downloaded content for this asset.",
                    () -> {
                        cancelButton.setWaiting(true);
                        assetDownload.cancelDownload(new IControlResultHandler() {
                            @Override
                            public void onRejected(IRejectReason reason) {
                                showInfo("Cancel rejected", reason.getDetails());
                                cancelButton.setWaiting(false);
                            }

                            @Override
                            public void onCancelled() {
                                cancelButton.setWaiting(false);
                            }

                            @Override
                            public void onError(EnigmaError error) {
                                cancelButton.setWaiting(false);
                                showError("Failed to cancel download", error);
                            }

                            @Override
                            public void onDone() {
                                showInfo("Download cancelled!", null);
                                cancelButton.setWaiting(false);
                            }
                        });
                    }
            );
        });

        viewGroup.addView(cardView);
    }

    private void setupDrmInfo(View drmDataContainer, IDrmLicence drmLicence) {
        ExpiryDate expiryDate = new ExpiryDate(drmDataContainer);
        expiryDate.setExpiryTime(drmLicence.getExpiryTime());
        AsyncButton renewButton = drmDataContainer.findViewById(R.id.renewButton);
        UserData userData = UserDataHolder.getUserData();
        final ISession session;
        if(userData != null) {
            session = userData.getSession();
        } else {
            session = null;
        }
        if(session != null) {
            renewButton.setText("Renew licence");
            renewButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    renewButton.setWaiting(true);
                    drmLicence.renew(session, new BaseDrmLicenceRenewResultHandler() {
                        @Override
                        public void onSuccess() {
                            renewButton.setText("Renewed!");
                            renewButton.setWaiting(false);
                            expiryDate.setExpiryTime(drmLicence.getExpiryTime());
                        }

                        @Override
                        public void onError(EnigmaError error) {
                            renewButton.setText("Failed");
                            renewButton.setWaiting(false);
                            if(error instanceof MaxDownloadCountLimitReachedError) {
                                showInfo("Max downloads of asset reached", "You have reached the maximum number of downloads for this asset.");
                            } else {
                                showError("Failed to renew DRM licence", error);
                            }
                        }
                    });
                }
            });
        } else {
            renewButton.setEnabled(false);
            renewButton.setText("Not signed in");
        }
    }

    private void showError(String description, EnigmaError error) {
        DialogUtil.showError(this, description, error);
    }

    private void showInfo(String title, String details) {
        DialogUtil.showInfo(this, title, details);
    }

    public static void startActivity(Context context) {
        Intent intent = new Intent(context, ListDownloadsActivity.class);
        context.startActivity(intent);
    }

    private static class ExpiryDate {
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm MMM dd yyyy");
        private final TextView drmExpiryTimeTextView;

        public ExpiryDate(View cardView) {
            this.drmExpiryTimeTextView = cardView.findViewById(R.id.drmExpiryTime);
        }

        public void setExpiryTime(Long expiryTime) {
            AndroidThreadUtil.runOnUiThread(() -> {
                if(expiryTime == null) {
                    drmExpiryTimeTextView.setText("");
                } else if(expiryTime.longValue() == IDrmLicence.EXPIRY_TIME_UNKNOWN) {
                    drmExpiryTimeTextView.setText("Expiration time unknown");
                } else {
                    drmExpiryTimeTextView.setText("Valid until "+dateFormat.format(new Date(expiryTime)));
                }
            });
        }
    }
}
