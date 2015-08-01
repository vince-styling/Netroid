package com.vincestyling.netroid.sample;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import com.vincestyling.netroid.cache.BitmapImageCache;
import com.vincestyling.netroid.cache.DiskCache;
import com.vincestyling.netroid.sample.netroid.Netroid;
import com.vincestyling.netroid.sample.netroid.SelfImageLoader;
import com.vincestyling.netroid.widget.NetworkImageView;

import java.io.File;

public class ImageRequestActivity extends BaseActivity implements View.OnClickListener {
    private ImageView mImageView;
    private NetworkImageView mNetworkImageView;
    private Button btnLoadSingleImage;
    private Button btnImageLoaderHttp;
    private Button btnImageLoaderAssets;
    private Button btnImageLoaderSdcard;
    private Button btnGridView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_request);

        mImageView = (ImageView) findViewById(R.id.imvAnchor);
        mNetworkImageView = (NetworkImageView) findViewById(R.id.imvNetwork);
        btnLoadSingleImage = (Button) findViewById(R.id.btnLoadSingleImage);
        btnImageLoaderHttp = (Button) findViewById(R.id.btnImageLoaderHttp);
        btnImageLoaderAssets = (Button) findViewById(R.id.btnImageLoaderAssets);
        btnImageLoaderSdcard = (Button) findViewById(R.id.btnImageLoaderSdcard);
        btnGridView = (Button) findViewById(R.id.btnGridView);

        btnLoadSingleImage.setOnClickListener(this);
        btnImageLoaderHttp.setOnClickListener(this);
        btnImageLoaderAssets.setOnClickListener(this);
        btnImageLoaderSdcard.setOnClickListener(this);
        btnGridView.setOnClickListener(this);
    }

    // initialize netroid, this code should be invoke at Application in product stage.
    @Override
    protected void initNetroid() {
        int memoryCacheSize = 5 * 1024 * 1024; // 5MB

        File diskCacheDir = new File(getCacheDir(), "netroid");
        int diskCacheSize = 50 * 1024 * 1024; // 50MB

        Netroid.init(new DiskCache(diskCacheDir, diskCacheSize));
        Netroid.setImageLoader(new SelfImageLoader(Netroid.getRequestQueue(),
                new BitmapImageCache(memoryCacheSize), getResources(), getAssets()));
    }

    @Override
    public void onClick(View view) {
        if (view.equals(btnLoadSingleImage)) {
            loadSingleImage();
        } else if (view.equals(btnImageLoaderHttp)) {
            loadHttpImage();
        } else if (view.equals(btnImageLoaderAssets)) {
            loadAssetsImage();
        } else if (view.equals(btnImageLoaderSdcard)) {
            loadSdcardImage();
        } else if (view.equals(btnGridView)) {
            loadGridView();
        }
    }

    private void loadSingleImage() {
        String url = "http://upload.newhua.com/3/3e/1292303714308.jpg";
        Netroid.displayImage(url, mImageView, android.R.drawable.ic_menu_rotate, android.R.drawable.ic_delete);
    }

    private void loadHttpImage() {
        String url = "http://i3.sinaimg.cn/blog/sports/idx/2014/0114/U5295P346T302D1F7961DT20140114132743.jpg";
        Netroid.displayImage(url, mNetworkImageView, android.R.drawable.ic_menu_rotate, android.R.drawable.ic_delete);
    }

    private void loadAssetsImage() {
        String url = SelfImageLoader.RES_ASSETS + "cover_16539.jpg";
        Netroid.displayImage(url, mNetworkImageView, android.R.drawable.ic_menu_rotate, android.R.drawable.ic_delete);
    }

    private void loadSdcardImage() {
        String url = SelfImageLoader.RES_SDCARD + Environment.getExternalStorageDirectory() + "/sample.jpg";
        Netroid.displayImage(url, mNetworkImageView, android.R.drawable.ic_menu_rotate, android.R.drawable.ic_delete);
    }

    private void loadGridView() {
        // Note : when back from GridViewActivity then click any others buttons,
        // app will crash because Netroid requirements destroyed by GridViewActivity,
        // this problem not need to be solve in such sample context.
        startActivity(new Intent(this, GridViewActivity.class));
    }
}
