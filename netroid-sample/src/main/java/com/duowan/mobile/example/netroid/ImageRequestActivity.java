package com.duowan.mobile.example.netroid;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import com.duowan.mobile.example.netroid.netroid.Netroid;
import com.duowan.mobile.example.netroid.netroid.SelfImageLoader;
import com.duowan.mobile.netroid.*;
import com.duowan.mobile.netroid.cache.CacheWrapper;
import com.duowan.mobile.netroid.cache.DiskBasedCache;
import com.duowan.mobile.netroid.cache.MemoryBasedCache;
import com.duowan.mobile.netroid.image.NetworkImageView;
import com.duowan.mobile.netroid.toolbox.ImageLoader;

import java.io.File;

public class ImageRequestActivity extends Activity implements View.OnClickListener {
	private RequestQueue mQueue;
	private ImageLoader imageLoader;
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
		setContentView(R.layout.image_request);

		mImageView = (ImageView) findViewById(R.id.image_view);
		mNetworkImageView = (NetworkImageView) findViewById(R.id.network_image_view);
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

		int memoryCacheSize = 5 * 1024 * 1024; // 5MB

		File diskCacheDir = new File(getCacheDir(), "netroid");
		int diskCacheSize = 50 * 1024 * 1024; // 50MB

		mQueue = Netroid.newRequestQueue(getApplicationContext(),
				new CacheWrapper(Const.CACHE_KEY_MEMORY, new MemoryBasedCache(memoryCacheSize)),
				new CacheWrapper(Const.CACHE_KEY_DISK, new DiskBasedCache(diskCacheDir, diskCacheSize)));

		imageLoader = new SelfImageLoader(mQueue, getResources(), getAssets());
	}

	@Override
	public void finish() {
		mQueue.stop();
		super.finish();
	}

	@Override
	public void onClick(View view) {
		if (view.equals(btnLoadSingleImage)) {
			loadSingleImage();
		}
		else if (view.equals(btnImageLoaderHttp)) {
			loadHttpImage();
		}
		else if (view.equals(btnImageLoaderAssets)) {
			loadAssetsImage();
		}
		else if (view.equals(btnImageLoaderSdcard)) {
			loadSdcardImage();
		}
		else if (view.equals(btnGridView)) {
			loadGridView();
		}
	}

	private void loadSingleImage() {
		String url = "http://upload.newhua.com/3/3e/1292303714308.jpg";
		ImageLoader.ImageListener listener = ImageLoader.getImageListener(mImageView,
						android.R.drawable.ic_menu_rotate, android.R.drawable.ic_delete);
		imageLoader.get(url, listener);
	}

	private void loadHttpImage() {
		String url = "http://i3.sinaimg.cn/blog/sports/idx/2014/0114/U5295P346T302D1F7961DT20140114132743.jpg";
		mNetworkImageView.setImageUrl(url, imageLoader);
	}

	private void loadAssetsImage() {
		mNetworkImageView.setImageUrl(SelfImageLoader.RES_ASSETS + "cover_16539.jpg", imageLoader);
	}

	private void loadSdcardImage() {
		mNetworkImageView.setImageUrl(SelfImageLoader.RES_SDCARD + "/sdcard/sample.jpg", imageLoader);
	}

	private void loadGridView() {
		startActivity(new Intent(this, GridViewActivity.class));
	}

}
