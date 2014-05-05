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

import android.os.Process;
import com.duowan.mobile.netroid.cache.DiskCache;

import java.util.concurrent.BlockingQueue;

/**
 * Provides a thread for performing network dispatch from a queue of requests.
 *
 * Requests added to the specified queue are processed from the network via a
 * specified {@link Network} interface. Responses are committed to cache, if
 * eligible, using a specified {@link com.duowan.mobile.netroid.cache.DiskCache} interface.
 * Valid responses and errors are posted back to the caller via a {@link Delivery}.
 */
@SuppressWarnings("rawtypes")
public class NetworkDispatcher extends Thread {
    /** The queue of requests to service. */
    private final BlockingQueue<Request> mQueue;

    /** The network interface for processing requests. */
    private final Network mNetwork;

	/** The cache to write to. */
    private final DiskCache mCache;

	/** For posting responses and errors. */
    private final Delivery mDelivery;

	/** Used for telling us to die. */
    private volatile boolean mQuit = false;

    /**
     * Creates a new network dispatcher thread.  You must call {@link #start()}
     * in order to begin processing.
     *
     * @param queue Queue of incoming requests for triage
     * @param network Network interface to use for performing requests
	 * @param cache Cache interface to use for writing responses to cache
     * @param delivery Delivery interface to use for posting responses
     */
    public NetworkDispatcher(BlockingQueue<Request> queue,
            Network network, DiskCache cache,
            Delivery delivery) {
        mQueue = queue;
		mCache = cache;
		mNetwork = network;
        mDelivery = delivery;
    }

    /**
     * Forces this dispatcher to quit immediately.  If any requests are still in
     * the queue, they are not guaranteed to be processed.
     */
    public void quit() {
        mQuit = true;
        interrupt();
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        Request request;
        while (true) {
            try {
                // Take a request from the queue.
                request = mQueue.take();
            } catch (InterruptedException e) {
                // We may have been interrupted because it was time to quit.
                if (mQuit) return;
                continue;
            }

            try {
                request.addMarker("network-queue-take");
				mDelivery.postPreExecute(request);

                // If the request was cancelled already,
                // do not perform the network request.
                if (request.isCanceled()) {
                    request.finish("network-discard-cancelled");
					mDelivery.postCancel(request);
					mDelivery.postFinish(request);
                    continue;
                }

                // Perform the network request.
                NetworkResponse networkResponse = mNetwork.performRequest(request);
                request.addMarker("network-http-complete");

                // Parse the response here on the worker thread.
                Response<?> response = request.parseNetworkResponse(networkResponse);
                request.addMarker("network-parse-complete");

                // Write to cache if applicable.
				if (mCache != null && request.shouldCache() && response.cacheEntry != null) {
					response.cacheEntry.expireTime = request.getCacheExpireTime();
					mCache.putEntry(request.getCacheKey(), response.cacheEntry);
					request.addMarker("network-cache-written");
				}

                // Post the response back.
                request.markDelivered();
                mDelivery.postResponse(request, response);
            } catch (NetroidError netroidError) {
				mDelivery.postError(request, request.parseNetworkError(netroidError));
            } catch (Exception e) {
				NetroidLog.e(e, "Unhandled exception %s", e.toString());
				mDelivery.postError(request, new NetroidError(e));
			}
        }
    }

}
