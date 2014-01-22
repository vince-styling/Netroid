package com.duowan.mobile.example.netroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import com.duowan.mobile.example.netroid.netroid.Netroid;
import com.duowan.mobile.netroid.*;
import com.duowan.mobile.netroid.cache.CacheWrapper;
import com.duowan.mobile.netroid.cache.DiskBasedCache;
import com.duowan.mobile.netroid.cache.MemoryBasedCache;
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
    private Button btnUsedDiskCache;
    private Button btnForceUpdate;
    private Button btnEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.common_http_request);

		btnJsonObject = (Button) findViewById(R.id.btnJsonObject);
		btnJsonArray = (Button) findViewById(R.id.btnJsonArray);
		btnStringRequs = (Button) findViewById(R.id.btnStringRequs);
		btnUsedDiskCache = (Button) findViewById(R.id.btnUsedDiskCache);
		btnForceUpdate = (Button) findViewById(R.id.btnForceUpdate);
		btnEventListener = (Button) findViewById(R.id.btnEventListener);

        btnJsonObject.setOnClickListener(this);
        btnJsonArray.setOnClickListener(this);
        btnStringRequs.setOnClickListener(this);
        btnUsedDiskCache.setOnClickListener(this);
        btnForceUpdate.setOnClickListener(this);
		btnEventListener.setOnClickListener(this);

		int memoryCacheSize = 5 * 1024 * 1024; // 5MB

		File diskCacheDir = new File(getCacheDir(), "netroid");
		int diskCacheSize = 50 * 1024 * 1024; // 50MB

		mQueue = Netroid.newRequestQueue(getApplicationContext(),
				new CacheWrapper(Const.CACHE_KEY_MEMORY, new MemoryBasedCache(memoryCacheSize)),
				new CacheWrapper(Const.CACHE_KEY_DISK, new DiskBasedCache(diskCacheDir, diskCacheSize)));
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
		else if (view.equals(btnUsedDiskCache)) {
			processUsedDiskCache();
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
		JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(url, null,
				new Listener<JSONObject>() {
					@Override
					public void onSuccess(JSONObject response) {
						showResult(response.toString());
					}
				});
		jsonObjectRequest.addHeader("HeaderTest", "11");
		jsonObjectRequest.setCacheSequence(Const.CACHE_KEY_MEMORY);
		jsonObjectRequest.setCacheExpireTime(TimeUnit.SECONDS, 10);
		mQueue.add(jsonObjectRequest);
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
		jsonArrayRequest.setCacheSequence(Const.CACHE_KEY_MEMORY);
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
		request.setCacheSequence(Const.CACHE_KEY_MEMORY);
		mQueue.add(request);
    }

    private void processUsedDiskCache() {
        String url = "http://client.azrj.cn/json/cook/cook_list.jsp?type=1&p=2&size=10";
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Listener<String>() {
                    @Override
                    public void onSuccess(String response) {
                        showResult(response);
                    }
                    @Override
                    public void onError(NetroidError error) {
                    }
                });
        stringRequest.setCacheSequence(Const.CACHE_KEY_DISK);
        mQueue.add(stringRequest);
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
		stringRequest.setCacheSequence(Const.CACHE_KEY_DISK);
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
			}

			@Override
			public void onFinish() {
				NetroidLog.e(REQUESTS_TAG);
			}

			@Override
			public void onSuccess(JSONObject response) {
				NetroidLog.e(REQUESTS_TAG);
			}

			@Override
			public void onRetry() {
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
				NetroidLog.e(REQUESTS_TAG);
			}
		});

		request.setRetryPolicy(new DefaultRetryPolicy(5000, 20, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
		request.setTag(REQUESTS_TAG);
		mQueue.add(request);
	}

	private void showResult(String result) {
		Log.i(NetroidLog.TAG, result);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(result);
		builder.show();
	}

}
