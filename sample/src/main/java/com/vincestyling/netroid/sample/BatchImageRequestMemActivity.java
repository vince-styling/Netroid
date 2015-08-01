package com.vincestyling.netroid.sample;

import com.vincestyling.netroid.cache.BitmapImageCache;
import com.vincestyling.netroid.request.ImageRequest;
import com.vincestyling.netroid.sample.netroid.Netroid;
import com.vincestyling.netroid.sample.netroid.SelfImageLoader;

import java.util.concurrent.TimeUnit;

public class BatchImageRequestMemActivity extends BaseListActivity {
    // initialize netroid, this code should be invoke at Application in product stage.
    @Override
    protected void initNetroid() {
        Netroid.init(null);

        int memoryCacheSize = 5 * 1024 * 1024; // 5MB
        Netroid.setImageLoader(new SelfImageLoader(Netroid.getRequestQueue(),
                new BitmapImageCache(memoryCacheSize), getResources(), getAssets()) {
            @Override
            public void makeRequest(ImageRequest request) {
                request.setCacheExpireTime(TimeUnit.DAYS, 10);
            }
        });
    }
}
