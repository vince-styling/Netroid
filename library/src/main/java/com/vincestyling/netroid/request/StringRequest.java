/*
 * Copyright (C) 2015 Vince Styling
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
package com.vincestyling.netroid.request;

import com.vincestyling.netroid.*;

/**
 * A canned request for retrieving the response body at a given URL as a String.
 */
public class StringRequest extends Request<String> {
    /**
     * Creates a new request with the given method.
     *
     * @param method   the request {@link Method} to use
     * @param url      URL to fetch the string at
     * @param listener Listener to receive the String response or error message
     */
    public StringRequest(int method, String url, IListener<String> listener) {
        super(method, url, listener);
    }

    /**
     * Creates a new GET request.
     *
     * @param url      URL to fetch the string at
     * @param listener Listener to receive the String response
     */
    public StringRequest(String url, IListener<String> listener) {
        this(Method.GET, url, listener);
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        String parsed = HttpUtils.parseResponse(response);
        return Response.success(parsed, response);
    }
}
