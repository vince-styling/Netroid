/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duowan.mobile.netroid;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import com.duowan.mobile.netroid.NetroidLog.MarkerLog;
import org.apache.http.HttpResponse;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Base class for all network requests.
` * @param <T> The type of parsed response this request expects.
 */
public abstract class Request<T> implements Comparable<Request<T>> {

    /**
     * Default encoding for POST or PUT parameters. See {@link #getParamsEncoding()}.
     */
    private static final String DEFAULT_PARAMS_ENCODING = "UTF-8";

    /**
     * Supported request methods.
     */
    public interface Method {
        int GET = 0;
        int POST = 1;
        int PUT = 2;
        int DELETE = 3;
        int HEAD = 4;
        int OPTIONS = 5;
        int TRACE = 6;
        int PATCH = 7;
    }

    /** An event log tracing the lifetime of this request; for debugging. */
    private final MarkerLog mEventLog = MarkerLog.ENABLED ? new MarkerLog() : null;

	/**
	 * Request method of this request.  Currently supports GET, POST, PUT, DELETE, HEAD, OPTIONS,
	 * TRACE, and PATCH.
	 */
    private final int mMethod;

    /** URL of this request. */
    private final String mUrl;

	/** The additional headers. */
	private HashMap<String, String> mHashHeaders;

    /** Listener interface for response and error. */
    private Listener<T> mListener;

    /** Sequence number of this request, used to enforce FIFO ordering. */
    private Integer mSequence;

    /** The request queue this request is associated with. */
    private RequestQueue mRequestQueue;

	/** perform request directly, ignore which caches should be used. */
	private boolean mForceUpdate;

    /** Whether or not this request has been canceled. */
    private boolean mCanceled = false;

    /** Whether or not a response has been delivered for this request yet. */
    private boolean mResponseDelivered = false;

    /** A cheap variant of request tracing used to dump slow requests. */
    private long mRequestBirthTime = 0;

    /** Threshold at which we should log the request (even when debug logging is not enabled). */
    private static final long SLOW_REQUEST_THRESHOLD_MS = 3000;

    /** The retry policy for this request. */
    private RetryPolicy mRetryPolicy;

	/** What time the cache is expired, in milliSeconds. */
	private long mCacheExpireTime;

    /** An opaque token tagging this request; used for bulk cancellation. */
    private Object mTag;

    /**
     * Creates a new request with the given method (one of the values from {@link Method}),
     * URL, and error listener.  Note that the normal response listener is not provided here as
     * delivery of responses is provided by subclasses, who have a better idea of how to deliver
     * an already-parsed response.
     */
    public Request(int method, String url, Listener<T> listener) {
		mUrl = url;
		mMethod = method;
        mListener = listener;
        setRetryPolicy(new DefaultRetryPolicy());
		mHashHeaders = new HashMap<String, String>();
    }

	/** Creates a new request with GET method */
	public Request(String url, Listener<T> listener) {
		this(Method.GET, url, listener);
	}

    /**
     * Return the method for this request.
	 * Can be one of the values in {@link Method}.
     */
    public int getMethod() {
        return mMethod;
    }

	/** Set the response listener. */
	public void setListener(Listener<T> listener) {
		mListener = listener;
	}

    /**
     * Set a tag on this request. Can be used to cancel all requests with this
     * tag by {@link RequestQueue#cancelAll(Object)}.
     */
    public void setTag(Object tag) {
        mTag = tag;
    }

    /**
     * Returns this request's tag.
     * @see Request#setTag(Object)
     */
    public Object getTag() {
        return mTag;
    }

    /**
     * Sets the retry policy for this request.
     */
    public void setRetryPolicy(RetryPolicy retryPolicy) {
        mRetryPolicy = retryPolicy;
    }

    /**
     * Adds an event to this request's event log; for debugging.
     */
    public void addMarker(String tag) {
        if (MarkerLog.ENABLED) {
            mEventLog.add(tag, Thread.currentThread().getId());
        } else if (mRequestBirthTime == 0) {
            mRequestBirthTime = SystemClock.elapsedRealtime();
        }
    }

    /**
     * Notifies the request queue that this request has finished (successfully or with error).
     *
     * <p>Also dumps all events from this request's event log; for debugging.</p>
     */
    public void finish(final String tag) {
        if (mRequestQueue != null) {
            mRequestQueue.finish(this);
        }
        if (MarkerLog.ENABLED) {
            final long threadId = Thread.currentThread().getId();
            if (Looper.myLooper() != Looper.getMainLooper()) {
                // If we finish marking off of the main thread, we need to
                // actually do it on the main thread to ensure correct ordering.
                Handler mainThread = new Handler(Looper.getMainLooper());
                mainThread.post(new Runnable() {
                    @Override
                    public void run() {
                        mEventLog.add(tag, threadId);
                        mEventLog.finish(this.toString());
                    }
                });
                return;
            }

            mEventLog.add(tag, threadId);
            mEventLog.finish(this.toString());
        } else {
            long requestTime = SystemClock.elapsedRealtime() - mRequestBirthTime;
            if (requestTime >= SLOW_REQUEST_THRESHOLD_MS) {
                NetroidLog.d("%d ms: %s", requestTime, this.toString());
            }
        }
    }

    /**
     * Associates this request with the given queue. The request queue will be notified when this
     * request has finished.
     */
    public void setRequestQueue(RequestQueue requestQueue) {
        mRequestQueue = requestQueue;
    }

    /**
     * Sets the sequence number of this request.  Used by {@link RequestQueue}.
     */
    public final void setSequence(int sequence) {
        mSequence = sequence;
    }

    /**
     * Returns the sequence number of this request.
     */
    public final int getSequence() {
        if (mSequence == null) {
            throw new IllegalStateException("getSequence called before setSequence");
        }
        return mSequence;
    }

    /**
     * Returns the URL of this request.
     */
    public String getUrl() {
        return mUrl;
    }

	/**
	 * Returns the cache key for this request.
	 * By default, this is the URL.
	 */
	public String getCacheKey() {
		return getUrl();
	}

	/** Ask if should force update. */
	public boolean isForceUpdate() {
		return mForceUpdate;
	}

	/** tell {@link Network} should force update or no. */
	public void setForceUpdate(boolean forceUpdate) {
		this.mForceUpdate = forceUpdate;
	}

	public long getCacheExpireTime() {
		return mCacheExpireTime;
	}

	/**
	 * Set how long the cache is expired, {@link com.duowan.mobile.netroid.cache.DiskCache}
	 * will determine the cache entry is expired or not.
	 * For example :
	 * Request.setCacheExpireTime(TimeUnit.MINUTES, 1); // cache stays one minute
	 * Request.setCacheExpireTime(TimeUnit.DAYS, 2); // cache stays two days
	 * @param timeUnit what unit for the amount value
	 * @param amount how much unit should calculate
	 */
	public void setCacheExpireTime(TimeUnit timeUnit, int amount) {
		this.mCacheExpireTime = System.currentTimeMillis() + timeUnit.toMillis(amount);
	}

	/**
     * Mark this request as canceled.  No callback will be delivered.
     */
    public void cancel() {
        mCanceled = true;
    }

    /**
     * Returns true if this request has been canceled.
     */
    public boolean isCanceled() {
        return mCanceled;
    }

    /**
     * Returns a list of extra HTTP headers to go along with this request. Can
     * throw {@link AuthFailureError} as authentication may be required to
     * provide these values.
     * @throws AuthFailureError In the event of auth failure
     */
    public final Map<String, String> getHeaders() throws AuthFailureError {
        return mHashHeaders;
    }

	/**
	 * Put a Header to RequestHeaders.
	 * @param field header key
	 * @param value header value
	 */
	public final void addHeader(String field, String value) {
		// We don't accept duplicate header.
		removeHeader(field);
		mHashHeaders.put(field, value);
	}

	/**
	 * Remove a header from RequestHeaders
	 * @param field header key
	 */
	public final void removeHeader(String field) {
		mHashHeaders.remove(field);
	}

    /**
     * Returns a Map of parameters to be used for a POST or PUT request.  Can throw
     * {@link AuthFailureError} as authentication may be required to provide these values.
     *
     * <p>Note that you can directly override {@link #getBody()} for custom data.</p>
     *
     * @throws AuthFailureError in the event of auth failure
     */
	public Map<String, String> getParams() throws AuthFailureError {
        return null;
    }

    /**
     * Returns which encoding should be used when converting POST or PUT parameters returned by
     * {@link #getParams()} into a raw POST or PUT body.
     *
     * <p>This controls both encodings:
     * <ol>
     *     <li>The string encoding used when converting parameter names and values into bytes prior
     *         to URL encoding them.</li>
     *     <li>The string encoding used when converting the URL encoded parameters into a raw
     *         byte array.</li>
     * </ol>
     */
	public String getParamsEncoding() {
        return DEFAULT_PARAMS_ENCODING;
    }

    public String getBodyContentType() {
        return "application/x-www-form-urlencoded; charset=" + getParamsEncoding();
    }

    /**
     * Returns the raw POST or PUT body to be sent.
     *
     * @throws AuthFailureError in the event of auth failure
     */
    public byte[] getBody() throws AuthFailureError {
        Map<String, String> params = getParams();
        if (params != null && params.size() > 0) {
            return encodeParameters(params, getParamsEncoding());
        }
        return null;
    }

    /**
     * Converts <code>params</code> into an application/x-www-form-urlencoded encoded string.
     */
    public static byte[] encodeParameters(Map<String, String> params, String paramsEncoding) {
        StringBuilder encodedParams = new StringBuilder();
        try {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                encodedParams.append(URLEncoder.encode(entry.getKey(), paramsEncoding));
                encodedParams.append('=');
                encodedParams.append(URLEncoder.encode(entry.getValue(), paramsEncoding));
                encodedParams.append('&');
            }
            return encodedParams.toString().getBytes(paramsEncoding);
        } catch (UnsupportedEncodingException uee) {
            throw new RuntimeException("Encoding not supported: " + paramsEncoding, uee);
        }
    }

	/**
	 * Prepare to execute, invoke before {@link com.duowan.mobile.netroid.stack.HttpStack#performRequest}.
	 * it's original purpose is reset some request parameters after timeout then retry, especially headers,
	 * the situation is when you have some headers need to init or reset every perform, for example the "Range"
	 * header for download a file, you obviously must retrieve the begin position of file and reset the "Range"
	 * for every time you going to retry, so that's why we add this method.
	 */
	public void prepare() {
	}

	/**
	 * Handle the response for various request, normally, a request was a short and low memory-usage request,
	 * thus we can parse the response-content as byte[] in memory.
	 * However the {@link com.duowan.mobile.netroid.request.FileDownloadRequest}
	 * itself was a large memory-usage case, that's inadvisable for parse all
	 * response content to memory, so it had self-implement mechanism.
	 */
	public byte[] handleResponse(HttpResponse response, Delivery delivery) throws IOException, ServerError {
		// Some responses such as 204s do not have content.
		if (response.getEntity() != null) {
			return HttpUtils.responseToBytes(response);
		} else {
			// Add 0 byte response as a way of honestly representing a no-content request.
			return new byte[0];
		}
	}

	/**
	 * By default, everyone Request is http-base request, if you wants to load
	 * local file or perform others, also wants to use Cache, you can override
	 * this method to implement non-http request.
	 */
	public NetworkResponse perform() {
		return null;
	}

	/**
     * Returns true if responses to this request should be cached.
     */
    public final boolean shouldCache() {
		return mCacheExpireTime > 0;
	}

	/**
     * Priority values.  Requests will be processed from higher priorities to
     * lower priorities, in FIFO order.
     */
    public enum Priority {
        LOW,
        NORMAL,
        HIGH,
        IMMEDIATE
    }

    /**
     * Returns the {@link Priority} of this request; {@link Priority#NORMAL} by default.
     */
    public Priority getPriority() {
        return Priority.NORMAL;
    }

    /**
     * Returns the socket timeout in milliseconds per retry attempt. (This value can be changed
     * per retry attempt if a backoff is specified via backoffTimeout()). If there are no retry
     * attempts remaining, this will cause delivery of a {@link TimeoutError} error.
     */
    public final int getTimeoutMs() {
        return mRetryPolicy.getCurrentTimeout();
    }

    /**
     * Returns the retry policy that should be used  for this request.
     */
    public RetryPolicy getRetryPolicy() {
        return mRetryPolicy;
    }

    /**
     * Mark this request as having a response delivered on it. This can be used
     * later in the request's lifetime for suppressing identical responses.
     */
    public void markDelivered() {
        mResponseDelivered = true;
    }

    /**
     * Returns true if this request has had a response delivered for it.
     */
    public boolean hasHadResponseDelivered() {
        return mResponseDelivered;
    }

    /**
     * Subclasses must implement this to parse the raw network response
     * and return an appropriate response type. This method will be
     * called from a worker thread.  The response will not be delivered
     * if you return null.
     * @param response Response from the network
     * @return The parsed response, or null in the case of an error
     */
    protected abstract Response<T> parseNetworkResponse(NetworkResponse response);

    /**
     * Subclasses can override this method to parse 'networkError' and return a more specific error.
     *
     * <p>The default implementation just returns the passed 'networkError'.</p>
     *
     * @param netroidError the error retrieved from the network
     * @return an NetworkError augmented with additional information
     */
    protected NetroidError parseNetworkError(NetroidError netroidError) {
        return netroidError;
    }

    /**
     * Perform delivery of the parsed response. The given response is guaranteed to
     * be non-null; responses that fail to parse are not delivered.
     * @param response The parsed response returned by
     * {@link #parseNetworkResponse(NetworkResponse)}
     */
    public void deliverSuccess(T response) {
		if (mListener != null) {
			mListener.onSuccess(response);
		}
	}

    /**
     * Delivers error message to the Listener that the Request was initialized with.
     * @param error Error details
     */
    public void deliverError(NetroidError error) {
        if (mListener != null) {
            mListener.onError(error);
        }
    }

	/** Delivers request has truly cancelled to the Listener. */
	public void deliverCancel() {
		if (mListener != null) {
			mListener.onCancel();
		}
	}

	/** Indicates DeliverPreExecute operation is done or not,
	 * because the {@link CacheDispatcher} and {@link NetworkDispatcher}
	 * both will call this deliver, and we must ensure just invoke once. */
	private boolean mIsDeliverPreExecute;

	/** Delivers request is handling to the Listener. */
	public void deliverPreExecute() {
		if (mListener != null && !mIsDeliverPreExecute) {
			mIsDeliverPreExecute = true;
			mListener.onPreExecute();
		}
	}

	/** Delivers when cache used to the Listener. */
	public void deliverUsedCache() {
		if (mListener != null) {
			mListener.onUsedCache();
		}
	}

	/** Delivers when cache used to the Listener. */
	public void deliverFinish() {
		if (mListener != null) {
			mListener.onFinish();
		}
	}

	/** Delivers when request timeout and retry to the Listener. */
	public void deliverRetry() {
		if (mListener != null) {
			mListener.onRetry();
		}
	}

	/** Delivers when request going to do networking to the Listener. */
	public void deliverNetworking() {
		if (mListener != null) {
			mListener.onNetworking();
		}
	}

	/** Delivers when download request progress change to the Listener. */
	public void deliverDownloadProgress(long fileSize, long downloadedSize) {
		if (mListener != null) {
			mListener.onProgressChange(fileSize, downloadedSize);
		}
	}

    /**
     * Our comparator sorts from high to low priority, and secondarily by
     * sequence number to provide FIFO ordering.
     */
    @Override
    public int compareTo(Request<T> other) {
        Priority left = this.getPriority();
        Priority right = other.getPriority();

        // High-priority requests are "lesser" so they are sorted to the front.
        // Equal priorities are sorted by sequence number to provide FIFO ordering.
        return left == right ?
                this.mSequence - other.mSequence :
                right.ordinal() - left.ordinal();
    }

    @Override
    public String toString() {
        return (mCanceled ? "[X] " : "[ ] ") + getUrl() + " " + getPriority() + " " + mSequence;
    }
}
