package enigma.redbeemedia.com.downloads;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import com.redbeemedia.enigma.core.context.EnigmaRiverContext;
import com.redbeemedia.enigma.core.error.EnigmaError;
import com.redbeemedia.enigma.core.error.NoSessionRejectionError;
import com.redbeemedia.enigma.core.login.ILoginResultHandler;
import com.redbeemedia.enigma.core.network.BaseNetworkMonitorListener;
import com.redbeemedia.enigma.core.network.INetworkMonitor;
import com.redbeemedia.enigma.core.playable.AssetPlayable;
import com.redbeemedia.enigma.core.session.ISession;
import com.redbeemedia.enigma.core.util.AndroidThreadUtil;
import com.redbeemedia.enigma.exposureutils.download.EnigmaDownloadHelper;
import com.redbeemedia.enigma.exposureutils.models.asset.ApiAsset;
import com.redbeemedia.enigma.exposureutils.models.localized.ApiLocalizedData;
import enigma.redbeemedia.com.downloads.user.UserData;
import enigma.redbeemedia.com.downloads.user.UserDataHolder;
import enigma.redbeemedia.com.downloads.util.AssetHelper;
import enigma.redbeemedia.com.downloads.util.DialogUtil;
import enigma.redbeemedia.com.downloads.view.AsyncButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ListAssetsActivity extends AppCompatActivity
{
    private boolean assetLoadPending = false;
    private Handler handler;

    private AtomicBoolean downloadServiceFlag = new AtomicBoolean();

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_list_assets);

        this.handler = new Handler();

        INetworkMonitor networkMonitor = EnigmaRiverContext.getNetworkMonitor();
        if(networkMonitor.hasInternetAccess()) {
            UserDataHolder.login(new ILoginResultHandler() {
                @Override
                public void onSuccess(ISession session) {
                    loadAssetsAsSoonAsPossible();
                }

                @Override
                public void onError(EnigmaError error) {
                    showError("Could not log in.", error);
                }
            }, handler);
        } else {
            loadAssetsAsSoonAsPossible();
        }


        findViewById(R.id.navigateToDownloads).setOnClickListener(v -> ListDownloadsActivity.startActivity(this));
        findViewById(R.id.navigateToLogin).setOnClickListener(v -> LoginActivity.startActivity(this));

        findViewById(R.id.reloadButton).setOnClickListener(v -> {
            if(EnigmaRiverContext.getNetworkMonitor().hasInternetAccess()) {
                ViewGroup assetList = findViewById(R.id.assetList);
                assetList.removeAllViews();
                findViewById(R.id.loadMoreButton).setVisibility(View.GONE);
                loadAssetsAsSoonAsPossible();
            } else {
                showInfo("No internet connection", "Internet access is required to load assets from the server.");
            }
        });
    }

    private void loadAssetsAsSoonAsPossible() {
        if(assetLoadPending) {
            return;
        }
        assetLoadPending = true;
        INetworkMonitor networkMonitor = EnigmaRiverContext.getNetworkMonitor();
        if(networkMonitor.hasInternetAccess()) {
            loadAssets();
        } else {
            networkMonitor.addListener(new BaseNetworkMonitorListener() {
                @Override
                public void onInternetAccessChanged(boolean internetAccess) {
                    if(internetAccess) {
                        networkMonitor.removeListener(this);
                        loadAssets();
                    }
                }
            }, handler);
        }
    }

    private void loadAssets() {
        ProgressBar progressBar = findViewById(R.id.pageProgressBar);
        progressBar.setVisibility(View.VISIBLE);
        Button reloadButton = findViewById(R.id.reloadButton);
        reloadButton.setVisibility(View.GONE);

        AssetHelper.getAllAssets(new AssetResultHandler(() ->  {
            progressBar.setVisibility(View.INVISIBLE);
            reloadButton.setVisibility(View.VISIBLE);
            assetLoadPending = false;
        }));
    }

    private void createCardView(ApiAsset apiAsset, ViewGroup viewGroup) {
        ApiLocalizedData localizedData = getLocalizedData(apiAsset);
        if(localizedData == null) {
            return;
        }

        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View cardView = layoutInflater.inflate(R.layout.card_asset, viewGroup, false);

        TextView assetIdView = cardView.findViewById(R.id.assetId);
        assetIdView.setText(apiAsset.getAssetId());

        TextView titleView = cardView.findViewById(R.id.title);
        titleView.setText(localizedData.getTitle());

        Button playButton = cardView.findViewById(R.id.playButton);
        playButton.setOnClickListener(v -> {
            UserData userData = UserDataHolder.getUserData();
            if(userData == null) {
                showError("Please sign in", new NoSessionRejectionError());
            } else {
                PlaybackActivity.startActivity(ListAssetsActivity.this, new AssetPlayable(apiAsset.getAssetId()));
            }
        });

        Button downloadButton = cardView.findViewById(R.id.downloadButton);
        UserData userData = UserDataHolder.getUserData();
        if(userData != null) {
            if(EnigmaDownloadHelper.isAvailableToDownload(apiAsset, System.currentTimeMillis(), userData.getAvailabilityKeys())) {
                downloadButton.setEnabled(true);
                downloadButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ConfigureDownloadActivity.startActivity(ListAssetsActivity.this, apiAsset.getAssetId());
                    }
                });
            } else {
                downloadButton.setEnabled(false);
            }
        } else {
            downloadButton.setEnabled(false);
            downloadButton.setText("Not signed in");
        }
        viewGroup.addView(cardView);
    }

    private static ApiLocalizedData getLocalizedData(ApiAsset apiAsset) {
        List<ApiLocalizedData> localizedDataList = apiAsset.getLocalized();
        if(localizedDataList != null) {
            for(ApiLocalizedData localizedData : localizedDataList) {
                if("en".equals(localizedData.getLocale())) {
                    return localizedData;
                }
            }
            if(!localizedDataList.isEmpty()) {
                return localizedDataList.iterator().next();
            }
        }
        return null;
    }

    private void showError(String description, EnigmaError error) {
        DialogUtil.showError(this, description, error);
    }

    private void showInfo(String title, String details) {
        DialogUtil.showInfo(this, title, details);
    }

    private class AssetResultHandler implements AssetHelper.IAssetResultHandler {
        private final Runnable onDone;

        private final AsyncButton loadMoreButton = findViewById(R.id.loadMoreButton);

        public AssetResultHandler(Runnable onDone) {
            this.onDone = onDone;
        }

        @Override
        public void onAssets(List<ApiAsset> apiAssets, int page, boolean hasPotentiallyMore) {
            AndroidThreadUtil.runOnUiThread(() -> {
                LinearLayout assetList = findViewById(R.id.assetList);
                for(ApiAsset apiAsset : apiAssets) {
                    createCardView(apiAsset, assetList);
                }
                if(hasPotentiallyMore) {
                    loadMoreButton.setEnabled(true);
                    loadMoreButton.setVisibility(View.VISIBLE);

                    loadMoreButton.setOnClickListener(v -> {
                        loadMoreButton.setWaiting(true);
                        AssetHelper.getAllAssets(new AssetResultHandler(() -> loadMoreButton.setWaiting(false)), page+1);
                    });

                } else {
                    loadMoreButton.setEnabled(false);
                    loadMoreButton.setVisibility(View.GONE);
                }
                assetList.setVisibility(View.VISIBLE);
                onDone.run();
            });
        }

        @Override
        public void onError(EnigmaError error) {
            AndroidThreadUtil.runOnUiThread(() -> {
                onDone.run();
                showError("Failed to load assets", error);
            });
        }
    }
}
