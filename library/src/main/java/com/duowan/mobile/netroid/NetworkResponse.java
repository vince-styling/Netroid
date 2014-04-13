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

import org.apache.http.HttpStatus;

/**
 * Data and headers returned from {@link Network#performRequest(Request)}.
 */
public class NetworkResponse {
    /**
     * Creates a new network response.
     * @param statusCode the HTTP status code
     * @param data Response body
     * @param charset The response body charset, parse by http header
     */
    public NetworkResponse(int statusCode, byte[] data, String charset) {
		this.statusCode = statusCode;
		this.data = data;
        this.charset = charset;
	}

//    public NetworkResponse(byte[] data) {
//        this(HttpStatus.SC_OK, data, Collections.<String, String>emptyMap());
//    }

    public NetworkResponse(byte[] data, String charset) {
        this(HttpStatus.SC_OK, data, charset);
    }

    /** The HTTP status code. */
    public final int statusCode;

    /** Raw data from this response. */
    public final byte[] data;

    /** Charset from this response. */
    public final String charset;
}