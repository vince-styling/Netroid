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
import com.duowan.mobile.netroid.Request;
import com.duowan.mobile.netroid.Request.Method;
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

    /**
     * @param sslSocketFactory SSL factory to use for HTTPS connections
     */
    public HurlStack(String userAgent, SSLSocketFactory sslSocketFactory) {
        mSslSocketFactory = sslSocketFactory;
		mUserAgent = userAgent;
    }

	public HurlStack(String userAgent) {
		this(userAgent, null);
	}

    @Override
    public HttpResponse performRequest(Request<?> request) throws IOException, AuthFailureError {
		HashMap<String, String> map = new HashMap<String, String>();
		if (!TextUtils.isEmpty(mUserAgent)) {
			map.put(HTTP.USER_AGENT, mUserAgent);
		}
		map.putAll(request.getHeaders());

		URL parsedUrl = new URL(request.getUrl());
		HttpURLConnection connection = openConnection(parsedUrl, request);
		for (String headerName : map.keySet()) {
			connection.addRequestProperty(headerName, map.get(headerName));
		}

		setConnectionParametersForRequest(connection, request);

		int responseCode = connection.getResponseCode();
		if (responseCode == -1) {
			// -1 is returned by getResponseCode() if the response code could not be retrieved.
			// Signal to the caller that something was wrong with the connection.
			throw new IOException("Could not retrieve response code from HttpUrlConnection.");
		}

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
            case Method.HEAD:
                connection.setRequestMethod("HEAD");
                break;
            case Method.OPTIONS:
                connection.setRequestMethod("OPTIONS");
                break;
            case Method.TRACE:
                connection.setRequestMethod("TRACE");
                break;
            case Method.PATCH:
                addBodyIfExists(connection, request);
                connection.setRequestMethod("PATCH");
                break;
            default:
                throw new IllegalStateException("Unknown method type.");
        }
    }

    private static void addBodyIfExists(
			HttpURLConnection connection, Request<?> request) throws IOException, AuthFailureError {
        byte[] body = request.getBody();
        if (body != null) {
            connection.setDoOutput(true);
            connection.addRequestProperty(HTTP.CONTENT_TYPE, request.getBodyContentType());
            DataOutputStream out = new DataOutputStream(connection.getOutputStream());
            out.write(body);
            out.close();
        }
    }
}
