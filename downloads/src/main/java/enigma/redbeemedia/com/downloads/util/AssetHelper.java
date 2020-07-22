package enigma.redbeemedia.com.downloads.util;

import com.redbeemedia.enigma.core.error.EnigmaError;
import com.redbeemedia.enigma.exposureutils.EnigmaExposure;
import com.redbeemedia.enigma.exposureutils.FieldSet;
import com.redbeemedia.enigma.exposureutils.GetAllAssetsRequest;
import com.redbeemedia.enigma.exposureutils.IExposureResultHandler;
import com.redbeemedia.enigma.exposureutils.models.asset.ApiAsset;
import com.redbeemedia.enigma.exposureutils.models.asset.ApiAssetList;

import java.util.List;

import enigma.redbeemedia.com.downloads.MyApplication;

public class AssetHelper {
    private static EnigmaExposure enigmaExposure = new EnigmaExposure(MyApplication.getBusinessUnit());


    public static void getAllAssets(IAssetResultHandler resultHandler) {
        getAllAssets(resultHandler, 1);
    }

    public static void getAllAssets(IAssetResultHandler resultHandler, int page) {
        enigmaExposure.doRequest(new GetAllAssetsRequest(new IExposureResultHandler<ApiAssetList>() {
            @Override
            public void onSuccess(ApiAssetList result) {
                boolean hasMore = result.getTotalCount() > result.getPageNumber()*result.getPageSize();
                resultHandler.onAssets(result.getItems(), (int) result.getPageNumber(), hasMore);
            }

            @Override
            public void onError(EnigmaError error) {
                resultHandler.onError(error);
            }
        })
        .setOnlyPublished(true)
        .setFieldSet(FieldSet.ALL) //Important! Needed to check isAvailableToDownload
        .setPageSize(100)
        .setPageNumber(page));
    }

    public interface IAssetResultHandler {
        void onAssets(List<ApiAsset> apiAssets, int page, boolean hasPotentiallyMore);
        void onError(EnigmaError error);
    }
}
