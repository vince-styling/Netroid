package com.duowan.mobile.example.netroid;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity implements View.OnClickListener {
    private Button mButtonCommon;
    private Button mButtonImage;
    private Button btnBatchImageMem;
    private Button btnBatchImageDisk;
    private Button btnBatchImageMultCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButtonCommon = (Button) findViewById(R.id.button_common_http_request);
		mButtonCommon.setOnClickListener(this);

		mButtonImage = (Button) findViewById(R.id.button_image_request);
		mButtonImage.setOnClickListener(this);

		btnBatchImageMem = (Button) findViewById(R.id.btnBatchImageMem);
		btnBatchImageMem.setOnClickListener(this);

		btnBatchImageDisk = (Button) findViewById(R.id.btnBatchImageDisk);
		btnBatchImageDisk.setOnClickListener(this);

		btnBatchImageMultCache = (Button) findViewById(R.id.btnBatchImageMultCache);
		btnBatchImageMultCache.setOnClickListener(this);
	}

    @Override
    public void onClick(View view) {
        if (view.equals(mButtonCommon)) {
            Intent intent = new Intent(this, CommonHttpRequestActivity.class);
            startActivity(intent);
        }
		else if (view.equals(mButtonImage)) {
            Intent intent = new Intent(this, ImageRequestActivity.class);
            startActivity(intent);
        }
		else if (view.equals(btnBatchImageDisk)) {
			Intent intent = new Intent(this, BatchImageRequestDiskActivity.class);
			startActivity(intent);
		}
		else if (view.equals(btnBatchImageMem)) {
			Intent intent = new Intent(this, BatchImageRequestMemActivity.class);
			startActivity(intent);
		}
		else if (view.equals(btnBatchImageMultCache)) {
			Intent intent = new Intent(this, BatchImageRequestMultCacheActivity.class);
			startActivity(intent);
		}
    }
}