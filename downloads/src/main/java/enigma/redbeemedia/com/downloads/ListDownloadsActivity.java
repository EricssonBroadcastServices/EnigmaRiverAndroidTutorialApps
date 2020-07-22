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
import com.redbeemedia.enigma.download.DownloadedPlayable;
import com.redbeemedia.enigma.download.EnigmaDownload;
import com.redbeemedia.enigma.download.resulthandler.BaseResultHandler;
import com.redbeemedia.enigma.download.resulthandler.IResultHandler;

import java.util.List;

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
                    createCardView(playable, viewGroup);
                }
                viewGroup.setLayoutTransition(new LayoutTransition());
            }

            @Override
            public void onError(EnigmaError error) {
                progressBar.setVisibility(View.INVISIBLE);
                showError("Could not get downloaded assets", error);
            }
        }, handler);
    }

    private void createCardView(DownloadedPlayable downloadedPlayable, ViewGroup viewGroup) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.card_downloaded, viewGroup, false);


        TextView assetIdView = cardView.findViewById(R.id.assetId);
        assetIdView.setText(downloadedPlayable.getAssetId());

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
        viewGroup.addView(cardView);
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
}
