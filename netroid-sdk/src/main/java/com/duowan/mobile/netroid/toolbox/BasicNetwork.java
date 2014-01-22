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
import org.apache.http.*;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.protocol.HTTP;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

/**
 * A network performing Netroid requests over an {@link HttpStack}.
 */
public class BasicNetwork implements Network {
    protected static final boolean DEBUG = NetroidLog.DEBUG;

    private static int SLOW_REQUEST_THRESHOLD_MS = 3000;

    private static int DEFAULT_POOL_SIZE = 4096;

	private final HttpStack mHttpStack;

	private final ByteArrayPool mPool;

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
        this(httpStack, new ByteArrayPool(DEFAULT_POOL_SIZE), defaultCharset);
    }

    /**
     * @param httpStack HTTP stack to be used
     * @param pool a buffer pool that improves GC performance in copy operations
	 * @param defaultCharset when Http Header doesn't offer the 'Content-Type:Charset', it will be use.
     */
    public BasicNetwork(HttpStack httpStack, ByteArrayPool pool, String defaultCharset) {
		mDefaultCharset = defaultCharset;
		mHttpStack = httpStack;
        mPool = pool;
    }

	@Override
	public void setDelivery(Delivery delivery) {
		mDelivery = delivery;
	}

	@Override
	public NetworkResponse performRequest(Request<?> request) throws NetroidError {
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
			Map<String, String> responseHeaders = new HashMap<String, String>();
			try {
				httpResponse = mHttpStack.performRequest(request);
				StatusLine statusLine = httpResponse.getStatusLine();
				int statusCode = statusLine.getStatusCode();

				responseHeaders = convertHeaders(httpResponse.getAllHeaders());
				// Handle cache validation.
				// CHANGES : we assume it never return NOT_MODIFIED Status because we didn't send header of last request.
//                if (statusCode == HttpStatus.SC_NOT_MODIFIED) {
//                    return new NetworkResponse(HttpStatus.SC_NOT_MODIFIED,
//                            request.getCacheEntry().data, responseHeaders, true);
//                }

				// Some responses such as 204s do not have content.  We must check.
				if (httpResponse.getEntity() != null) {
					responseContents = entityToBytes(httpResponse.getEntity());
				} else {
					// Add 0 byte response as a way of honestly representing a
					// no-content request.
					responseContents = new byte[0];
				}

				// if the request is slow, log it.
				long requestLifetime = SystemClock.elapsedRealtime() - requestStart;
				logSlowRequests(requestLifetime, request, responseContents, statusLine);

				if (statusCode < 200 || statusCode > 299) throw new IOException();

				return new NetworkResponse(statusCode, responseContents, parseCharset(responseHeaders));
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
					networkResponse = new NetworkResponse(statusCode, responseContents, parseCharset(responseHeaders));
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

//    private void addCacheHeaders(Map<String, String> headers, Cache.Entry entry) {
//        // If there's no cache entry, we're done.
//        if (entry == null) {
//            return;
//        }
//
//        if (entry.etag != null) {
//            headers.put("If-None-Match", entry.etag);
//        }
//
//        if (entry.serverDate > 0) {
//            Date refTime = new Date(entry.serverDate);
//            headers.put("If-Modified-Since", DateUtils.formatDate(refTime));
//        }
//    }

    protected void logError(String what, String url, long start) {
        long now = SystemClock.elapsedRealtime();
        NetroidLog.v("HTTP ERROR(%s) %d ms to fetch %s", what, (now - start), url);
    }

    /** Reads the contents of HttpEntity into a byte[]. */
    private byte[] entityToBytes(HttpEntity entity) throws IOException, ServerError {
        PoolingByteArrayOutputStream bytes =
                new PoolingByteArrayOutputStream(mPool, (int) entity.getContentLength());
        byte[] buffer = null;
        try {
            InputStream in = entity.getContent();
            if (in == null) {
                throw new ServerError();
            }
            buffer = mPool.getBuf(1024);
            int count;
            while ((count = in.read(buffer)) != -1) {
                bytes.write(buffer, 0, count);
            }
            return bytes.toByteArray();
        } finally {
            try {
                // Close the InputStream and release the resources by "consuming the content".
                entity.consumeContent();
            } catch (IOException e) {
                // This can happen if there was an exception above that left the entity in
                // an invalid state.
                NetroidLog.v("Error occured when calling consumingContent");
            }
            mPool.returnBuf(buffer);
            bytes.close();
        }
    }

    /**
     * Converts Headers[] to Map<String, String>.
     */
    private static Map<String, String> convertHeaders(Header[] headers) {
        Map<String, String> result = new HashMap<String, String>();
		for (Header header : headers) {
			result.put(header.getName(), header.getValue());
		}
        return result;
    }

	/**
	 * Returns the charset specified in the Content-Type of this header,
	 * or the defaultCharset if none can be found.
	 */
	public String parseCharset(Map<String, String> headers) {
		String contentType = headers.get(HTTP.CONTENT_TYPE);
		if (contentType != null) {
			String[] params = contentType.split(";");
			for (int i = 1; i < params.length; i++) {
				String[] pair = params[i].trim().split("=");
				if (pair.length == 2) {
					if (pair[0].equals("charset")) {
						return pair[1];
					}
				}
			}
		}
		return mDefaultCharset;
	}

	public String getDefaultCharset() {
		return mDefaultCharset;
	}

}
