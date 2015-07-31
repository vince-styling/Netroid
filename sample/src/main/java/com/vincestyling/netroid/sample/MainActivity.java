package com.vincestyling.netroid.sample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * The main entry of all the samples.
 * <p/>
 * Note : every sub-activities acted a standalone test case, never involve of others.
 * The <code>Netroid</code> initialization of them in particularly, best
 * been invokes where app startup(i.e. {@link android.app.Application}).
 */
public class MainActivity extends Activity implements View.OnClickListener {
    private Button mButtonCommon;
    private Button mButtonImage;
    private Button btnBatchImageMem;
    private Button btnBatchImageDisk;
    private Button btnBatchImageMultCache;
    private Button btnFileDownload;
    private Button btnBlockingRequest;

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

        btnFileDownload = (Button) findViewById(R.id.btnFileDownload);
        btnFileDownload.setOnClickListener(this);

        btnBlockingRequest = (Button) findViewById(R.id.btnBlockingRequest);
        btnBlockingRequest.setOnClickListener(this);
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
        else if (view.equals(btnFileDownload)) {
            Intent intent = new Intent(this, FileDownloadActivity.class);
            startActivity(intent);
        }
        else if (view.equals(btnBlockingRequest)) {
            Intent intent = new Intent(this, OffWithMainThreadPerformingActivity.class);
            startActivity(intent);
        }
    }

}
