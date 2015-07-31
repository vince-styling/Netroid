package com.vincestyling.netroid.sample;

import android.app.AlertDialog;
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

public class CommonHttpRequestActivity extends BaseActivity implements View.OnClickListener {
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
        String url = "http://client.azrj.cn/json/cook/cook_list.jsp?type=1&p=2&size=10";
        JsonObjectRequest request = new JsonObjectRequest(url, null,
                new Listener<JSONObject>() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        showResult(response.toString());
                    }
                });
        request.setCacheExpireTime(TimeUnit.MINUTES, 1);
        request.addHeader("HeaderTest", "11");
        Netroid.add(request);
    }

    private void processJsonArray() {
        String url = "http://172.17.2.243:8080/json_array.json";
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(url,
                new Listener<JSONArray>() {
                    @Override
                    public void onSuccess(JSONArray response) {
                        showResult(response.toString());
                    }
                });
        jsonArrayRequest.addHeader("HeaderTest", "11");
        jsonArrayRequest.setCacheExpireTime(TimeUnit.SECONDS, 10);
        Netroid.add(jsonArrayRequest);
    }

    private void processStringRequs() {
        String url = "http://client.azrj.cn/json/cook/cook_list.jsp?type=1&p=2&size=10";
        StringRequest request = new StringRequest(Request.Method.GET, url,
                new Listener<String>() {
                    @Override
                    public void onSuccess(String response) {
                        showResult(response);
                    }

                    @Override
                    public void onError(NetroidError error) {
                    }
                });
        request.addHeader("HeaderTest", "11");
        Netroid.add(request);
    }

    private void processForceUpdate() {
        String url = "http://client.azrj.cn/json/cook/cook_list.jsp?type=1&p=2&size=10";
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Listener<String>() {
                    @Override
                    public void onSuccess(String response) {
                        showResult(response);
                    }
                });
        stringRequest.setForceUpdate(true);
        Netroid.add(stringRequest);
    }

    private void processEventListener() {
        final String REQUESTS_TAG = "Request-Demo";
        String url = "http://facebook.com/";
        JsonObjectRequest request = new JsonObjectRequest(url, null, new Listener<JSONObject>() {
            long startTimeMs;
            int retryCount;

            @Override
            public void onPreExecute() {
                startTimeMs = SystemClock.elapsedRealtime();
                AppLog.e(REQUESTS_TAG);
                Toast.makeText(CommonHttpRequestActivity.this, "onPreExecute()", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFinish() {
                AppLog.e(REQUESTS_TAG);
                Toast.makeText(CommonHttpRequestActivity.this, "onFinish()", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSuccess(JSONObject response) {
                AppLog.e(REQUESTS_TAG);
                Toast.makeText(CommonHttpRequestActivity.this, "onSuccess()", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRetry() {
                Toast.makeText(CommonHttpRequestActivity.this, "onRetry()", Toast.LENGTH_SHORT).show();
                long executedTime = SystemClock.elapsedRealtime() - startTimeMs;
                if (++retryCount > 5 || executedTime > 30000) {
                    AppLog.e("retryCount : " + retryCount + " executedTime : " + executedTime);
                    Netroid.getRequestQueue().cancelAll(REQUESTS_TAG);
                } else {
                    AppLog.e(REQUESTS_TAG);
                }
            }

            @Override
            public void onCancel() {
                Toast.makeText(CommonHttpRequestActivity.this, "onCancel()", Toast.LENGTH_SHORT).show();
                AppLog.e(REQUESTS_TAG);
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

}
