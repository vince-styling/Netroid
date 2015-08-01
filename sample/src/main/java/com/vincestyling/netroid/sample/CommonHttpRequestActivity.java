package com.vincestyling.netroid.sample;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.vincestyling.netroid.DefaultRetryPolicy;
import com.vincestyling.netroid.Listener;
import com.vincestyling.netroid.NetroidError;
import com.vincestyling.netroid.Request;
import com.vincestyling.netroid.cache.DiskCache;
import com.vincestyling.netroid.request.JsonArrayRequest;
import com.vincestyling.netroid.request.JsonObjectRequest;
import com.vincestyling.netroid.request.StringRequest;
import com.vincestyling.netroid.sample.netroid.Netroid;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * This sample demonstrating a bunch of most basic request operations. included JsonObject,
 * JsonArray, String, Ignore Disk Cache to Force Performing, Various Listener Events.
 */
public class CommonHttpRequestActivity extends BaseActivity implements View.OnClickListener {
    private String PREFIX = "http://netroid.cn/dummy/";

    private Button btnJsonObject;
    private Button btnJsonArray;
    private Button btnStringRequs;
    private Button btnForceUpdate;
    private Button btnEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.common_http_request);

        btnJsonObject = (Button) findViewById(R.id.btnJsonObject);
        btnJsonArray = (Button) findViewById(R.id.btnJsonArray);
        btnStringRequs = (Button) findViewById(R.id.btnStringRequs);
        btnForceUpdate = (Button) findViewById(R.id.btnForceUpdate);
        btnEventListener = (Button) findViewById(R.id.btnEventListener);

        btnJsonObject.setOnClickListener(this);
        btnJsonArray.setOnClickListener(this);
        btnStringRequs.setOnClickListener(this);
        btnForceUpdate.setOnClickListener(this);
        btnEventListener.setOnClickListener(this);
    }

    // initialize netroid, this code should be invoke at Application in product stage.
    @Override
    protected void initNetroid() {
        File diskCacheDir = new File(getCacheDir(), "netroid");
        int diskCacheSize = 50 * 1024 * 1024; // 50MB
        Netroid.init(new DiskCache(diskCacheDir, diskCacheSize));
    }

    @Override
    public void onClick(View view) {
        if (view.equals(btnJsonObject)) {
            processJsonObject();
        } else if (view.equals(btnJsonArray)) {
            processJsonArray();
        } else if (view.equals(btnStringRequs)) {
            processStringRequs();
        } else if (view.equals(btnForceUpdate)) {
            processForceUpdate();
        } else if (view.equals(btnEventListener)) {
            processEventListener();
        }
    }

    private void processJsonObject() {
        String url = PREFIX + "hot_keywords";
        JsonObjectRequest request = new JsonObjectRequest(url, null, new BaseListener<JSONObject>());
        request.setCacheExpireTime(TimeUnit.MINUTES, 1);
        request.addHeader("HeaderTest", "11");
        Netroid.add(request);
    }

    private void processJsonArray() {
        String url = PREFIX + "categories";
        JsonArrayRequest request = new JsonArrayRequest(url, new BaseListener<JSONArray>());
        request.setCacheExpireTime(TimeUnit.SECONDS, 10);
        Netroid.add(request);
    }

    private void processStringRequs() {
        String url = PREFIX + "offwith_1";
        StringRequest request = new StringRequest(Request.Method.GET, url, new BaseListener<String>());
        request.setCacheExpireTime(TimeUnit.HOURS, 1);
        Netroid.add(request);
    }

    private void processForceUpdate() {
        String url = PREFIX + "offwith_1";
        StringRequest request = new StringRequest(url, new BaseListener<String>());
        request.setForceUpdate(true);
        Netroid.add(request);
    }

    private void processEventListener() {
        final String REQUESTS_TAG = "Request-Demo";
        String url = "http://facebook.com/";
        StringRequest request = new StringRequest(url, new BaseListener<String>() {
            long startTimeMs;
            int retryCount;

            @Override
            public void onPreExecute() {
                super.onPreExecute();
                startTimeMs = SystemClock.elapsedRealtime();
                Toast.makeText(CommonHttpRequestActivity.this, "onPreExecute()", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFinish() {
                super.onFinish();
                Toast.makeText(CommonHttpRequestActivity.this, "onFinish()", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSuccess(String response) {
                super.onSuccess(response);
                Toast.makeText(CommonHttpRequestActivity.this, "onSuccess()", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(NetroidError error) {
                super.onError(error);
                Toast.makeText(CommonHttpRequestActivity.this, "onError()", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRetry() {
                super.onRetry();
                Toast.makeText(CommonHttpRequestActivity.this, "onRetry()", Toast.LENGTH_SHORT).show();
                long executedTime = SystemClock.elapsedRealtime() - startTimeMs;
                if (++retryCount > 5 || executedTime > 30000) {
                    AppLog.e("retryCount : %d executedTime : %d", retryCount, executedTime);
                    Netroid.getRequestQueue().cancelAll(REQUESTS_TAG);
                }
            }

            @Override
            public void onCancel() {
                super.onCancel();
                Toast.makeText(CommonHttpRequestActivity.this, "onCancel()", Toast.LENGTH_SHORT).show();
            }
        });

        request.setRetryPolicy(new DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, 20, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        request.setTag(REQUESTS_TAG);
        Netroid.add(request);
    }

    private void showResult(String result) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(result);
        builder.show();
    }

    private class BaseListener<T> extends Listener<T> {
        private ProgressDialog mProgressDialog;

        @Override
        public void onPreExecute() {
            mProgressDialog = ProgressDialog.show(CommonHttpRequestActivity.this, null, "request executing");
        }

        @Override
        public void onFinish() {
            if (mProgressDialog != null) {
                mProgressDialog.cancel();
            }
        }

        @Override
        public void onSuccess(T response) {
            showResult(response.toString());
        }

        @Override
        public void onError(NetroidError error) {
            showResult("REQUEST ERROR : " + error.getMessage());
        }
    }
}
