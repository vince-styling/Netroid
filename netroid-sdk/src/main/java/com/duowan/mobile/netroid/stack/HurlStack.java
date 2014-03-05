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

import android.text.TextUtils;
import com.duowan.mobile.netroid.AuthFailureError;
import com.duowan.mobile.netroid.Delivery;
import com.duowan.mobile.netroid.Request;
import com.duowan.mobile.netroid.Request.Method;
import com.duowan.mobile.netroid.request.FileDownloadRequest;
import org.apache.http.*;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.HTTP;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

/**
 * An {@link HttpStack} based on {@link HttpURLConnection}.
 */
public class HurlStack implements HttpStack {

    private String mUserAgent;
    private final SSLSocketFactory mSslSocketFactory;
    private boolean mFollowRedirect;

    /**
     * @param sslSocketFactory SSL factory to use for HTTPS connections
     */
    public HurlStack(String userAgent, SSLSocketFactory sslSocketFactory) {
        mSslSocketFactory = sslSocketFactory;
		mUserAgent = userAgent;
		mFollowRedirect = true;
    }

    /**
     * Sets whether this connection follows redirects.
     * @param followRedirects  true if this connection will follows redirects, false otherwise.
     */
    public void setFollowRedirects(boolean followRedirects) {
        mFollowRedirect = followRedirects;
    }

	// Common part of perform a request.
	private HttpURLConnection perform(Request<?> request, String... headers) throws IOException, AuthFailureError {
		HashMap<String, String> map = new HashMap<String, String>();
		if (!TextUtils.isEmpty(mUserAgent)) {
			map.put(HTTP.USER_AGENT, mUserAgent);
		}
		for (int i = 0; i < headers.length; i++) {
			map.put(headers[i], headers[++i]);
		}
		map.putAll(request.getHeaders());

		URL parsedUrl = new URL(request.getUrl());
		HttpURLConnection connection = openConnection(parsedUrl, request);
		for (String headerName : map.keySet()) {
			connection.addRequestProperty(headerName, map.get(headerName));
		}

		connection.setInstanceFollowRedirects(mFollowRedirect);
		setConnectionParametersForRequest(connection, request);

		int responseCode = connection.getResponseCode();
		if (responseCode == -1) {
			// -1 is returned by getResponseCode() if the response code could not be retrieved.
			// Signal to the caller that something was wrong with the connection.
			throw new IOException("Could not retrieve response code from HttpUrlConnection.");
		}

		return connection;
	}

    @Override
    public HttpResponse performRequest(Request<?> request) throws IOException, AuthFailureError {
        HttpURLConnection connection = perform(request);

		StatusLine responseStatus = new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1),
                connection.getResponseCode(), connection.getResponseMessage());
        BasicHttpResponse response = new BasicHttpResponse(responseStatus);
        response.setEntity(entityFromConnection(connection));

        for (Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) {
            if (header.getKey() != null) {
                Header h = new BasicHeader(header.getKey(), header.getValue().get(0));
                response.addHeader(h);
            }
        }

        return response;
    }

    @Override
    public int performDownloadRequest(FileDownloadRequest request, Delivery delivery) throws IOException, AuthFailureError {
		long downloadedSize = request.getTemporaryFile().length();

		// Note: if the request header "Range" greater than the actual length that server-size have,
		// the response header "Content-Range" will return "bytes */[actual length]", that's wrong.
        HttpURLConnection connection = perform(request, "Range", "bytes=" + downloadedSize + "-");

		// The file actually size.
		long fileSize = Long.parseLong(connection.getHeaderField(HTTP.CONTENT_LEN));

		boolean isSupportRange = isSupportRange(connection);
		if (isSupportRange) {
			fileSize += downloadedSize;

			// Verify the Content-Range Header, to ensure temporary file is part of the whole file.
			// Sometime, temporary file length add response content-length might greater than actual file length,
			// in this situation, we consider the temporary file is invalid, then throw an exception.
			String realRangeValue = connection.getHeaderField("Content-Range");
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

			return connection.getResponseCode();
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

		InputStream inStream = connection.getInputStream();
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
		tmpFileRaf.close();
		inStream.close();

        return connection.getResponseCode();
    }

    /**
     * Initializes an {@link HttpEntity} from the given {@link HttpURLConnection}.
     * @return an HttpEntity populated with data from <code>connection</code>.
     */
    private static HttpEntity entityFromConnection(HttpURLConnection connection) {
        BasicHttpEntity entity = new BasicHttpEntity();
        InputStream inputStream;
        try {
            inputStream = connection.getInputStream();
        } catch (IOException ioe) {
            inputStream = connection.getErrorStream();
        }
        entity.setContent(inputStream);
        entity.setContentLength(connection.getContentLength());
        entity.setContentEncoding(connection.getContentEncoding());
        entity.setContentType(connection.getContentType());
        return entity;
    }

    /**
     * Create an {@link HttpURLConnection} for the specified {@code url}.
     */
    private HttpURLConnection createConnection(URL url) throws IOException {
        return (HttpURLConnection) url.openConnection();
    }

    /**
     * Opens an {@link HttpURLConnection} with parameters.
     * @return an open connection
     */
    private HttpURLConnection openConnection(URL url, Request<?> request) throws IOException {
        HttpURLConnection connection = createConnection(url);

        int timeoutMs = request.getTimeoutMs();
        connection.setConnectTimeout(timeoutMs);
        connection.setReadTimeout(timeoutMs);
        connection.setUseCaches(false);
        connection.setDoInput(true);

        // use caller-provided custom SslSocketFactory, if any, for HTTPS
        if ("https".equals(url.getProtocol()) && mSslSocketFactory != null) {
            ((HttpsURLConnection)connection).setSSLSocketFactory(mSslSocketFactory);
        }

        return connection;
    }

	private static void setConnectionParametersForRequest(
			HttpURLConnection connection, Request<?> request) throws IOException, AuthFailureError {
        switch (request.getMethod()) {
            case Method.GET:
                // Not necessary to set the request method because connection defaults to GET but
                // being explicit here.
                connection.setRequestMethod("GET");
                break;
            case Method.DELETE:
                connection.setRequestMethod("DELETE");
                break;
            case Method.POST:
                connection.setRequestMethod("POST");
                addBodyIfExists(connection, request);
                break;
            case Method.PUT:
                connection.setRequestMethod("PUT");
                addBodyIfExists(connection, request);
                break;
            default:
                throw new IllegalStateException("Unknown method type.");
        }
    }

    private static void addBodyIfExists(HttpURLConnection connection, Request<?> request)
            throws IOException, AuthFailureError {
        byte[] body = request.getBody();
        if (body != null) {
            connection.setDoOutput(true);
            connection.addRequestProperty(HTTP.CONTENT_TYPE, request.getBodyContentType());
            DataOutputStream out = new DataOutputStream(connection.getOutputStream());
            out.write(body);
            out.close();
        }
    }

	private static boolean isSupportRange(HttpURLConnection connection) {
		if (TextUtils.equals(connection.getHeaderField("Accept-Ranges"), "bytes")) {
			return true;
		}
		String value = connection.getHeaderField("Content-Range");
		return value != null && value.startsWith("bytes");
	}
}
