package com.duowan.mobile.example.netroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.duowan.mobile.example.netroid.netroid.Netroid;
import com.duowan.mobile.netroid.*;
import com.duowan.mobile.netroid.cache.DiskCache;
import com.duowan.mobile.netroid.request.JsonArrayRequest;
import com.duowan.mobile.netroid.request.JsonObjectRequest;
import com.duowan.mobile.netroid.request.StringRequest;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class CommonHttpRequestActivity extends Activity implements View.OnClickListener {
    private RequestQueue mQueue;

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

		File diskCacheDir = new File(getCacheDir(), "netroid");
		int diskCacheSize = 50 * 1024 * 1024; // 50MB
		mQueue = Netroid.newRequestQueue(getApplicationContext(), new DiskCache(diskCacheDir, diskCacheSize));
	}

    @Override
    public void finish() {
        mQueue.stop();
        super.finish();
    }

    @Override
    public void onClick(View view) {
        if (view.equals(btnJsonObject)) {
            processJsonObject();
        }
		else if (view.equals(btnJsonArray)) {
            processJsonArray();
        }
		else if (view.equals(btnStringRequs)) {
            processStringRequs();
        }
		else if (view.equals(btnForceUpdate)) {
			processForceUpdate();
		}
		else if (view.equals(btnEventListener)) {
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
		mQueue.add(request);
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
		mQueue.add(jsonArrayRequest);
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
		mQueue.add(request);
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
		mQueue.add(stringRequest);
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
				NetroidLog.e(REQUESTS_TAG);
				Toast.makeText(CommonHttpRequestActivity.this, "onPreExecute()", Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onFinish() {
				NetroidLog.e(REQUESTS_TAG);
				Toast.makeText(CommonHttpRequestActivity.this, "onFinish()", Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onSuccess(JSONObject response) {
				NetroidLog.e(REQUESTS_TAG);
				Toast.makeText(CommonHttpRequestActivity.this, "onSuccess()", Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onRetry() {
				Toast.makeText(CommonHttpRequestActivity.this, "onRetry()", Toast.LENGTH_SHORT).show();
				long executedTime = SystemClock.elapsedRealtime() - startTimeMs;
				if (++retryCount > 5 || executedTime > 30000) {
					NetroidLog.e("retryCount : " + retryCount + " executedTime : " + executedTime);
					mQueue.cancelAll(REQUESTS_TAG);
				} else {
					NetroidLog.e(REQUESTS_TAG);
				}
			}

			@Override
			public void onCancel() {
				Toast.makeText(CommonHttpRequestActivity.this, "onCancel()", Toast.LENGTH_SHORT).show();
				NetroidLog.e(REQUESTS_TAG);
			}
		});

		request.setRetryPolicy(new DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, 20, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
		request.setTag(REQUESTS_TAG);
		mQueue.add(request);
	}

	private void showResult(String result) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(result);
		builder.show();
	}

}
