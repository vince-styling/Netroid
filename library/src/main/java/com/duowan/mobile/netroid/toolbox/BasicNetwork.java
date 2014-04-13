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

package com.duowan.mobile.netroid.toolbox;

import android.os.SystemClock;
import com.duowan.mobile.netroid.*;
import com.duowan.mobile.netroid.stack.HttpStack;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.conn.ConnectTimeoutException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;

/**
 * A network performing Netroid requests over an {@link HttpStack}.
 */
public class BasicNetwork implements Network {
    protected static final boolean DEBUG = NetroidLog.DEBUG;

    private static int SLOW_REQUEST_THRESHOLD_MS = 3000;

    private static int DEFAULT_POOL_SIZE = 4096;

	private final HttpStack mHttpStack;

	/** The default charset only use when response doesn't offer the Content-Type header. */
	private final String mDefaultCharset;

	/** Request delivery mechanism. */
	private Delivery mDelivery;

    /**
     * @param httpStack HTTP stack to be used
	 * @param defaultCharset default charset if response does not provided.
     */
    public BasicNetwork(HttpStack httpStack, String defaultCharset) {
        // If a pool isn't passed in, then build a small default pool that will give us a lot of
        // benefit and not use too much memory.
        this(httpStack, DEFAULT_POOL_SIZE, defaultCharset);
    }

    /**
     * @param httpStack HTTP stack to be used
     * @param bytePoolSize Size of buffer pool that improves GC performance in copy operations.
	 * @param defaultCharset when Http Header doesn't offer the 'Content-Type:Charset', it will be use.
     */
    public BasicNetwork(HttpStack httpStack, int bytePoolSize, String defaultCharset) {
		ByteArrayPool.init(bytePoolSize);
		mDefaultCharset = defaultCharset;
		mHttpStack = httpStack;
    }

	@Override
	public void setDelivery(Delivery delivery) {
		mDelivery = delivery;
	}

	@Override
	public NetworkResponse performRequest(Request<?> request) throws NetroidError {
		// Determine if request had non-http perform.
		NetworkResponse networkResponse = request.perform();
		if (networkResponse != null) return networkResponse;

		long requestStart = SystemClock.elapsedRealtime();
		while (true) {
			// If the request was cancelled already,
			// do not perform the network request.
			if (request.isCanceled()) {
				request.finish("perform-discard-cancelled");
				mDelivery.postCancel(request);
				throw new NetworkError(networkResponse);
			}

			HttpResponse httpResponse = null;
			byte[] responseContents = null;
			try {
				// prepare to perform this request, normally is reset the request headers.
				request.prepare();

				httpResponse = mHttpStack.performRequest(request);

				StatusLine statusLine = httpResponse.getStatusLine();
				int statusCode = statusLine.getStatusCode();
				if (statusCode < 200 || statusCode > 299) throw new IOException();

				responseContents = request.handleResponse(httpResponse, mDelivery);

				// if the request is slow, log it.
				long requestLifetime = SystemClock.elapsedRealtime() - requestStart;
				logSlowRequests(requestLifetime, request, responseContents, statusLine);

				return new NetworkResponse(statusCode, responseContents, parseCharset(httpResponse));
			} catch (SocketTimeoutException e) {
				attemptRetryOnException("socket", request, new TimeoutError());
			} catch (ConnectTimeoutException e) {
				attemptRetryOnException("connection", request, new TimeoutError());
			} catch (MalformedURLException e) {
				throw new RuntimeException("Bad URL " + request.getUrl(), e);
			} catch (IOException e) {
				if (httpResponse == null) throw new NoConnectionError(e);

				int statusCode = httpResponse.getStatusLine().getStatusCode();
				NetroidLog.e("Unexpected response code %d for %s", statusCode, request.getUrl());
				if (responseContents != null) {
					networkResponse = new NetworkResponse(statusCode, responseContents, parseCharset(httpResponse));
					if (statusCode == HttpStatus.SC_UNAUTHORIZED || statusCode == HttpStatus.SC_FORBIDDEN) {
						attemptRetryOnException("auth", request, new AuthFailureError(networkResponse));
					} else {
						// TODO: Only throw ServerError for 5xx status codes.
						throw new ServerError(networkResponse);
					}
				} else {
					throw new NetworkError(networkResponse);
				}
			}
		}
	}

    /**
     * Logs requests that took over SLOW_REQUEST_THRESHOLD_MS to complete.
     */
    private void logSlowRequests(long requestLifetime, Request<?> request,
            byte[] responseContents, StatusLine statusLine) {
        if (DEBUG || requestLifetime > SLOW_REQUEST_THRESHOLD_MS) {
            NetroidLog.d("HTTP response for request=<%s> [lifetime=%d], [size=%s], " +
					"[rc=%d], [retryCount=%s]", request, requestLifetime,
					responseContents != null ? responseContents.length : "null",
					statusLine.getStatusCode(), request.getRetryPolicy().getCurrentRetryCount());
        }
    }

    /**
     * Attempts to prepare the request for a retry. If there are no more attempts remaining in the
     * request's retry policy, a timeout exception is thrown.
     * @param request The request to use.
     */
    private void attemptRetryOnException(String logPrefix, Request<?> request,
            NetroidError exception) throws NetroidError {
        RetryPolicy retryPolicy = request.getRetryPolicy();
        int oldTimeout = request.getTimeoutMs();

        try {
            retryPolicy.retry(exception);
        } catch (NetroidError e) {
            request.addMarker(String.format("%s-timeout-giveup [timeout=%s]", logPrefix, oldTimeout));
            throw e;
        }

		request.addMarker(String.format("%s-retry [timeout=%s]", logPrefix, oldTimeout));
		mDelivery.postRetry(request);
    }

    protected void logError(String what, String url, long start) {
        long now = SystemClock.elapsedRealtime();
        NetroidLog.v("HTTP ERROR(%s) %d ms to fetch %s", what, (now - start), url);
    }

	/**
	 * Returns the charset specified in the Content-Type of this header,
	 * or the defaultCharset if none can be found.
	 */
	private String parseCharset(HttpResponse response) {
		String charset = HttpUtils.getCharset(response);
		return charset == null ? mDefaultCharset : charset;
	}

	public String getDefaultCharset() {
		return mDefaultCharset;
	}

}
