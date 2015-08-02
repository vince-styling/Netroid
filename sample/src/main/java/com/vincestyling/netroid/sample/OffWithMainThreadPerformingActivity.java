package com.vincestyling.netroid.sample;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import com.vincestyling.netroid.Listener;
import com.vincestyling.netroid.request.StringRequest;
import com.vincestyling.netroid.sample.netroid.TransactionalRequest;
import com.vincestyling.netroid.sample.netroid.Netroid;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Sometimes, we may invoke our logical in an AsyncTask or just a Thread,
 * when the time we want to perform a Http Request in that, it is messy
 * to leave current thread to performing. Thus we brought you this sample
 * to demonstrating how to perform a blocking request in background threads.
 */
public class OffWithMainThreadPerformingActivity extends BaseActivity {
    private List<String> urls;

    private TextView txvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_off_with_main_thread);

        findViewById(R.id.btnPerformByAsyncTask).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new CombineTask().execute();
            }
        });

        findViewById(R.id.btnPerformByCustomRequest).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                produceDummyUrls();

                // the two requests would be perform just like a transaction.
                Netroid.add(new TransactionalRequest(urls.get(0), urls.get(1), new Listener<String>() {
                    private ProgressDialog mProgressDialog;

                    @Override
                    public void onPreExecute() {
                        mProgressDialog = ProgressDialog.show(OffWithMainThreadPerformingActivity.this, null, "task executing");
                        txvResult.setText("");
                    }

                    @Override
                    public void onSuccess(String result) {
                        if (mProgressDialog != null) {
                            mProgressDialog.cancel();
                        }

                        if (TextUtils.isEmpty(result)) {
                            result = "result is empty!";
                        }

                        txvResult.setText(result);
                    }
                }));
            }
        });

        txvResult = (TextView) findViewById(R.id.txvResult);
    }

    // initialize netroid, this code should be invoke at Application in product stage.
    @Override
    protected void initNetroid() {
        Netroid.init(null);
    }

    private class CombineTask extends AsyncTask<Void, Void, String> {
        private ProgressDialog mProgressDialog;

        @Override
        protected void onPreExecute() {
            mProgressDialog = ProgressDialog.show(OffWithMainThreadPerformingActivity.this, null, "task executing");
            txvResult.setText("");
        }

        @Override
        protected String doInBackground(Void... params) {
            return perform();
        }

        @Override
        protected void onPostExecute(String result) {
            if (mProgressDialog != null) {
                mProgressDialog.cancel();
            }

            if (TextUtils.isEmpty(result)) {
                result = "result is empty!";
            }

            txvResult.setText(result);
        }
    }

    /**
     * In this method, we perform two requests randomly in blocking way, compare the response whether equals at last.
     * Notice : this method must run on backgroud thread.
     *
     * @return the result of two requests.
     */
    private String perform() {
        AppLog.e("Is run on main thread : %s", Looper.myLooper() == Looper.getMainLooper());

        produceDummyUrls();

        final String[] results = new String[2];

        for (int i = 0; i < 2; i++) {
            final int index = i;
            String url = urls.get(i);
            AppLog.e("perform url : %s", url);

            // perform request in blocking mode.
            Netroid.perform(new StringRequest(url, new Listener<String>() {
                @Override
                public void onSuccess(String response) {
                    results[index] = response;
                    AppLog.e("perform url[%d] result : %s", index, response);
                }
            }));
        }

        return "result <1>[" + results[0] + "] result <2>[" + results[1] + "] equals : " + TextUtils.equals(results[0], results[1]);
    }

    private void produceDummyUrls() {
        String prefix = "http://netroid.cn/dummy/";

        urls = Arrays.asList(prefix + "offwith_1", prefix + "offwith_2", prefix + "offwith_1_1");
        // shuffling the List so we can make the results not always identical.
        Collections.shuffle(urls);
    }
}
