package com.vincestyling.netroid.sample.netroid;

import android.text.TextUtils;
import com.vincestyling.netroid.*;
import com.vincestyling.netroid.request.StringRequest;
import com.vincestyling.netroid.sample.AppLog;

/**
 * This request claim two urls, one perform as blocking mode in {@link #prepare()}, the other perform as normal request.
 * We comparing both url's response in {@link #parseNetworkResponse(NetworkResponse)} and return the compared result.
 * <p/>
 * Just like a transaction flow, the second statement must depends on the first statement's result, and so on.
 * <p/>
 * Considering when we need various clauses which retrieve from remote to making
 * the final verdict, how remarkable we can do it like this elegant code.
 */
public class TransactionalRequest extends Request<String> {
    private String clauseUrl;

    private String clauseResult;

    public TransactionalRequest(String url, String clauseUrl, IListener<String> listener) {
        super(url, listener);
        this.clauseUrl = clauseUrl;
    }

    // perform the clause request in blocking mode first.
    @Override
    public void prepare() {
        // preparing may invoke again when the host request timeouted,
        // so don't perform the clause request repeatedly.
        if (clauseResult != null) return;

        Netroid.perform(new StringRequest(clauseUrl, new Listener<String>() {
            @Override
            public void onSuccess(String response) {
                clauseResult = response;
                AppLog.e("perform clause request url[%s] result : %s", clauseUrl, clauseResult);
            }
        }));
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        String result = HttpUtils.parseResponse(response);
        AppLog.e("perform url[%s] result : %s", getUrl(), result);

        String compared = "ComplexRequest:result <1>[" + clauseResult + "] result <2>[" + result + "] equals : " + TextUtils.equals(clauseResult, result);
        return Response.success(compared, response);
    }
}
