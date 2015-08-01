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

        mButtonCommon = (Button) findViewById(R.id.btnCommonHttpRequest);
        mButtonCommon.setOnClickListener(this);

        mButtonImage = (Button) findViewById(R.id.btnImageRequest);
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
        Intent intent = null;
        if (view.equals(mButtonCommon)) {
            intent = new Intent(this, CommonHttpRequestActivity.class);
        }
        else if (view.equals(mButtonImage)) {
            intent = new Intent(this, ImageRequestActivity.class);
        }
        else if (view.equals(btnBatchImageDisk)) {
            intent = new Intent(this, BatchImageRequestDiskActivity.class);
        }
        else if (view.equals(btnBatchImageMem)) {
            intent = new Intent(this, BatchImageRequestMemActivity.class);
        }
        else if (view.equals(btnBatchImageMultCache)) {
            intent = new Intent(this, BatchImageRequestMultCacheActivity.class);
        }
        else if (view.equals(btnFileDownload)) {
            intent = new Intent(this, FileDownloadActivity.class);
        }
        else if (view.equals(btnBlockingRequest)) {
            intent = new Intent(this, OffWithMainThreadPerformingActivity.class);
        }
        startActivity(intent);
    }

}
