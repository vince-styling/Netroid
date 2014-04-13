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

import com.duowan.mobile.netroid.cache.DiskCache;

/**
 * Encapsulates a parsed response for delivery.
 * @param <T> Parsed type of this response
 */
public class Response<T> {

    /** Returns a successful response containing the parsed result. */
    public static <T> Response<T> success(T result, NetworkResponse response) {
        return new Response<T>(result, new DiskCache.Entry(response.data, response.charset));
    }

    /**
     * Returns a failed response containing the given error code and an optional
     * localized message displayed to the user.
     */
    public static <T> Response<T> error(NetroidError error) {
        return new Response<T>(error);
    }

    /** Parsed response, or null in the case of error. */
    public final T result;

    /** Cache metadata for this response, or null in the case of error. */
    public final DiskCache.Entry cacheEntry;

    /** Detailed error information if <code>errorCode != OK</code>. */
    public final NetroidError error;

    /** True if this response was a soft-expired one and a second one MAY be coming. */
    public boolean intermediate = false;

    /**
     * Returns whether this response is considered successful.
     */
    public boolean isSuccess() {
        return error == null;
    }

    private Response(T result, DiskCache.Entry cacheEntry) {
        this.result = result;
        this.cacheEntry = cacheEntry;
        this.error = null;
    }

    private Response(NetroidError error) {
        this.result = null;
        this.cacheEntry = null;
        this.error = error;
    }
}
