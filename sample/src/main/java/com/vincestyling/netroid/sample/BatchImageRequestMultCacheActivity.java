package com.vincestyling.netroid.sample;

import com.vincestyling.netroid.cache.BitmapImageCache;
import com.vincestyling.netroid.cache.DiskCache;
import com.vincestyling.netroid.request.ImageRequest;
import com.vincestyling.netroid.sample.netroid.Netroid;
import com.vincestyling.netroid.sample.netroid.SelfImageLoader;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class BatchImageRequestMultCacheActivity extends BaseListActivity {
    // initialize netroid, this code should be invoke at Application in product stage.
    @Override
    protected void initNetroid() {
        int memoryCacheSize = 5 * 1024 * 1024; // 5MB

        File diskCacheDir = new File(getCacheDir(), "netroid");
        int diskCacheSize = 50 * 1024 * 1024; // 50MB

        Netroid.init(new DiskCache(diskCacheDir, diskCacheSize));

        Netroid.setImageLoader(new SelfImageLoader(Netroid.getRequestQueue(),
                new BitmapImageCache(memoryCacheSize), getResources(), getAssets()) {
            @Override
            public void makeRequest(ImageRequest request) {
                request.setCacheExpireTime(TimeUnit.MINUTES, 1);
            }
        });
    }
}
