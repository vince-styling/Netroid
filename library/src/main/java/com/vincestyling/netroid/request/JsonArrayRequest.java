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

import com.vincestyling.netroid.IListener;
import com.vincestyling.netroid.NetworkResponse;
import com.vincestyling.netroid.ParseError;
import com.vincestyling.netroid.Response;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.UnsupportedEncodingException;

/**
 * A request for retrieving a {@link JSONArray} response body at a given URL.
 */
public class JsonArrayRequest extends JsonRequest<JSONArray> {
    /**
     * Creates a new request.
     *
     * @param url      URL to fetch the JSON from
     * @param listener Listener to receive the JSON response or error message
     */
    public JsonArrayRequest(String url, IListener<JSONArray> listener) {
        super(Method.GET, url, null, listener);
    }

    @Override
    protected Response<JSONArray> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data, response.charset);
            return Response.success(new JSONArray(jsonString), response);
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JSONException je) {
            return Response.error(new ParseError(je));
        }
    }
}
