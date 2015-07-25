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
package com.vincestyling.netroid;

/**
 * Callback interface for delivering request status or response result.
 * Note : all method are calls over UI thread.
 *
 * @param <T> Parsed type of this response.
 */
public interface IListener<T> {
    /**
     * Inform when start to handle this Request.
     */
    void onPreExecute();

    /**
     * Inform when {@link Request} execute is finish,
     * whatever success or error or cancel, this callback
     * method always invoke if request is done.
     */
    void onFinish();

    /**
     * Called when response success.
     */
    void onSuccess(T response);

    /**
     * Callback method that an error has been occurred with the
     * provided error code and optional user-readable message.
     */
    void onError(NetroidError error);

    /**
     * Inform when the {@link Request} is truly cancelled.
     */
    void onCancel();

    /**
     * Inform When the {@link Request} cache non-exist or expired,
     * this callback method is opposite by the onUsedCache(),
     * means the http retrieving will happen soon.
     */
    void onNetworking();

    /**
     * Inform when the cache already use,
     * it means http networking won't execute.
     */
    void onUsedCache();

    /**
     * Inform when {@link Request} execute is going to retry.
     */
    void onRetry();

    /**
     * Inform when download progress change, this callback method only available
     * when request was {@link com.vincestyling.netroid.request.FileDownloadRequest}.
     */
    void onProgressChange(long fileSize, long downloadedSize);
}
