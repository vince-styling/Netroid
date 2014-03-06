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

package com.duowan.mobile.netroid.stack;

import android.net.http.AndroidHttpClient;
import android.text.TextUtils;
import com.duowan.mobile.netroid.AuthFailureError;
import com.duowan.mobile.netroid.Delivery;
import com.duowan.mobile.netroid.NetroidLog;
import com.duowan.mobile.netroid.Request;
import com.duowan.mobile.netroid.Request.Method;
import com.duowan.mobile.netroid.request.FileDownloadRequest;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An HttpStack that performs request over an {@link HttpClient}.
 */
public class HttpClientStack implements HttpStack {
    protected final HttpClient mClient;

    public HttpClientStack(String userAgent) {
        mClient = AndroidHttpClient.newInstance(userAgent);
    }

	public HttpClientStack(HttpClient client) {
		mClient = client;
	}

    private static void addHeaders(HttpUriRequest httpRequest, Map<String, String> headers) {
        for (String key : headers.keySet()) {
            httpRequest.setHeader(key, headers.get(key));
        }
    }

    @SuppressWarnings("unused")
    private static List<NameValuePair> getPostParameterPairs(Map<String, String> postParams) {
        List<NameValuePair> result = new ArrayList<NameValuePair>(postParams.size());
        for (String key : postParams.keySet()) {
            result.add(new BasicNameValuePair(key, postParams.get(key)));
        }
        return result;
    }

	@Override
	public HttpResponse performRequest(Request<?> request) throws IOException, AuthFailureError {
		HttpUriRequest httpRequest = createHttpRequest(request);
		addHeaders(httpRequest, request.getHeaders());
		onPrepareRequest(httpRequest);
		HttpParams httpParams = httpRequest.getParams();
		int timeoutMs = request.getTimeoutMs();
		// TODO: Reevaluate this connection timeout based on more wide-scale
		// data collection and possibly different for wifi vs. 3G.
		HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
		HttpConnectionParams.setSoTimeout(httpParams, timeoutMs);
		return mClient.execute(httpRequest);
	}

	@Override
	public int performDownloadRequest(FileDownloadRequest request, Delivery delivery) throws IOException, AuthFailureError {
		HttpResponse response = performRequest(request);
		int statusCode = response.getStatusLine().getStatusCode();

		// The file actually size.
		long fileSize = Long.parseLong(getHeader(response, HTTP.CONTENT_LEN));
		long downloadedSize = request.getTemporaryFile().length();

		boolean isSupportRange = isSupportRange(response);
		if (isSupportRange) {
			fileSize += downloadedSize;

			// Verify the Content-Range Header, to ensure temporary file is part of the whole file.
			// Sometime, temporary file length add response content-length might greater than actual file length,
			// in this situation, we consider the temporary file is invalid, then throw an exception.
			String realRangeValue = getHeader(response, "Content-Range");
			// response Content-Range may be null when "Range=bytes=0-"
			if (!TextUtils.isEmpty(realRangeValue)) {
				String assumeRangeValue = "bytes " + downloadedSize + "-" + (fileSize - 1);
				if (TextUtils.indexOf(realRangeValue, assumeRangeValue) == -1) {
					throw new IllegalStateException(
							"The Content-Range Header is invalid Assume[" + assumeRangeValue + "] vs Real[" + realRangeValue + "], " +
									"please remove the temporary file [" + request.getTemporaryFile() + "].");
				}
			}
		}

		// Don't go on if fileSize illegal.
		if (fileSize < 1) throw new IOException("Response's Empty!");

		// Compare the store file size(after download successes have) to server-side Content-Length.
		// temporary file will rename to store file after download success, so we compare the
		// Content-Length to ensure this request already download or not.
		if (request.getStoreFile().length() == fileSize) {
			// Rename the store file to temporary file, mock the download success. ^_^
			request.getStoreFile().renameTo(request.getTemporaryFile());

			// Deliver download progress.
			delivery.postDownloadProgress(request, fileSize, fileSize);

			return statusCode;
		}

		RandomAccessFile tmpFileRaf = new RandomAccessFile(request.getTemporaryFile(), "rw");

		// If server-side support range download, we seek to last point of the temporary file.
		if (isSupportRange) {
			// Seek to last point.
			tmpFileRaf.seek(downloadedSize);
		}
		// If not, truncate the temporary file then start download from beginning.
		else {
			tmpFileRaf.setLength(0);
			downloadedSize = 0;
		}

		// TODO : 写注释描述两个方法的执行流程及逻辑大致相同，尝试提取公共部分避免代码冗余；把8K buffer定义为常量。
		HttpEntity entity = null;
		try {
			entity = response.getEntity();
			InputStream inStream = entity.getContent();
			byte[] buffer = new byte[8 * 1024]; // 8K buffer
			int offset;

			while ((offset = inStream.read(buffer)) != -1) {
				tmpFileRaf.write(buffer, 0, offset);

				downloadedSize += offset;
				delivery.postDownloadProgress(request, fileSize, downloadedSize);

				if (request.isCanceled()) {
					delivery.postCancel(request);
					break;
				}
			}
		} finally {
			try {
				// Close the InputStream and release the resources by "consuming the content".
				if (entity != null) entity.consumeContent();
			} catch (Exception e) {
				// This can happen if there was an exception above that left the entity in
				// an invalid state.
				NetroidLog.v("Error occured when calling consumingContent");
			}
			tmpFileRaf.close();
		}

		return statusCode;
	}

    /**
     * Creates the appropriate subclass of HttpUriRequest for passed in request.
     */
	private static HttpUriRequest createHttpRequest(Request<?> request) throws AuthFailureError {
        switch (request.getMethod()) {
            case Method.GET:
                return new HttpGet(request.getUrl());
            case Method.DELETE:
                return new HttpDelete(request.getUrl());
            case Method.POST: {
                HttpPost postRequest = new HttpPost(request.getUrl());
                postRequest.addHeader(HTTP.CONTENT_TYPE, request.getBodyContentType());
                setEntityIfNonEmptyBody(postRequest, request);
                return postRequest;
            }
            case Method.PUT: {
                HttpPut putRequest = new HttpPut(request.getUrl());
                putRequest.addHeader(HTTP.CONTENT_TYPE, request.getBodyContentType());
                setEntityIfNonEmptyBody(putRequest, request);
                return putRequest;
            }
            default:
                throw new IllegalStateException("Unknown request method.");
        }
    }

    private static void setEntityIfNonEmptyBody(HttpEntityEnclosingRequestBase httpRequest,
            Request<?> request) throws AuthFailureError {
        byte[] body = request.getBody();
        if (body != null) {
            HttpEntity entity = new ByteArrayEntity(body);
            httpRequest.setEntity(entity);
        }
    }

    /**
     * Called before the request is executed using the underlying HttpClient.
     *
     * <p>Overwrite in subclasses to augment the request.</p>
     */
    protected void onPrepareRequest(HttpUriRequest request) throws IOException {
        // Nothing.
    }

	private static String getHeader(HttpResponse response, String key) {
		Header header = response.getFirstHeader(key);
		return header == null ? null : header.getValue();
	}

	private static boolean isSupportRange(HttpResponse response) {
		if (TextUtils.equals(getHeader(response, "Accept-Ranges"), "bytes")) {
			return true;
		}
		String value = getHeader(response, "Content-Range");
		return value != null && value.startsWith("bytes");
	}
}
