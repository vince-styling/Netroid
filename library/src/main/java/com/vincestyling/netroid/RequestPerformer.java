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

import com.vincestyling.netroid.cache.DiskCache;

/**
 * The standalone request performer, the intention of put it standalone
 * are enable it to invoke either main thread or non.
 * @see NetworkDispatcher#run()
 */
public final class RequestPerformer {

    public static void perform(Request request, Network network, DiskCache cache, Delivery delivery) {
        try {
            request.addMarker("network-queue-take");
            delivery.postPreExecute(request);

            // If the request was cancelled already,
            // do not perform the network request.
            if (request.isCanceled()) {
                request.finish("network-discard-cancelled");
                delivery.postCancel(request);
                delivery.postFinish(request);
                return;
            }

            // Perform the network request.
            NetworkResponse networkResponse = network.performRequest(request);
            request.addMarker("network-http-complete");

            // Parse the response here on the worker thread.
            Response<?> response = request.parseNetworkResponse(networkResponse);
            request.addMarker("network-parse-complete");

            // Write to cache if applicable.
            if (cache != null && request.shouldCache() && response.cacheEntry != null) {
                response.cacheEntry.setExpireTime(request.getCacheExpireTime());
                cache.putEntry(request.getCacheKey(), response.cacheEntry);
                request.addMarker("network-cache-written");
            }

            // Post the response back.
            request.markDelivered();
            delivery.postResponse(request, response);
        } catch (NetroidError netroidError) {
            delivery.postError(request, request.parseNetworkError(netroidError));
        } catch (Exception e) {
            NetroidLog.e(e, "Unhandled exception %s", e.toString());
            delivery.postError(request, new NetroidError(e));
        }
    }

    public static void perform(Request request, Network network, Delivery delivery) {
        perform(request, network, null, delivery);
    }

}
