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

import android.os.Process;
import com.vincestyling.netroid.cache.DiskCache;

import java.util.concurrent.BlockingQueue;

/**
 * Provides a thread for performing cache triage on a queue of requests.
 * <p/>
 * Requests added to the specified cache queue are resolved from cache.
 * Any deliverable response is posted back to the caller via a
 * {@link Delivery}. Cache misses and responses that require
 * refresh are enqueued on the specified network queue for processing
 * by a {@link NetworkDispatcher}.
 */
public class CacheDispatcher extends Thread {

    private static final boolean DEBUG = NetroidLog.DEBUG;

    /**
     * The queue of requests coming in for triage.
     */
    private final BlockingQueue<Request> mCacheQueue;

    /**
     * The queue of requests going out to the network.
     */
    private final BlockingQueue<Request> mNetworkQueue;

    /**
     * The cache to read from.
     */
    private final DiskCache mCache;

    /**
     * For posting responses.
     */
    private final Delivery mDelivery;

    /**
     * Used for telling us to die.
     */
    private volatile boolean mQuit;

    /**
     * Creates a new cache triage dispatcher thread.  You must call {@link #start()}
     * in order to begin processing.
     *
     * @param cacheQueue   Queue of incoming requests for triage
     * @param networkQueue Queue to post requests that require network to
     * @param cache        Cache interface to use for resolution
     * @param delivery     Delivery interface to use for posting responses
     */
    public CacheDispatcher(BlockingQueue<Request> cacheQueue, BlockingQueue<Request> networkQueue,
                           DiskCache cache, Delivery delivery) {
        mCache = cache;
        mDelivery = delivery;
        mCacheQueue = cacheQueue;
        mNetworkQueue = networkQueue;
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
        if (DEBUG) NetroidLog.v("start new dispatcher");
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        // Make a blocking call to initialize the cache.
        if (mCache != null) mCache.initialize();

        while (true) {
            try {
                // Get a request from the cache triage queue, blocking until
                // at least one is available.
                final Request request = mCacheQueue.take();
                request.addMarker("cache-queue-take");
                mDelivery.postPreExecute(request);

                // If the request has been canceled, don't bother dispatching it.
                if (request.isCanceled()) {
                    request.finish("cache-discard-canceled");
                    mDelivery.postCancel(request);
                    mDelivery.postFinish(request);
                    continue;
                }

                // Attempt to retrieve this item from cache.
                DiskCache.Entry entry = mCache != null ? mCache.getEntry(request.getCacheKey()) : null;
                if (entry == null) {
                    request.addMarker("cache-miss");
                    // Cache miss; send off to the network dispatcher.
                    mNetworkQueue.put(request);
                    mDelivery.postNetworking(request);
                    continue;
                }

                // If it is completely expired, just send it to the network.
                if (entry.isExpired()) {
                    request.addMarker("cache-hit-expired");
                    mNetworkQueue.put(request);
                    mDelivery.postNetworking(request);
                    continue;
                }

                // We have a cache hit; parse its data for delivery back to the request.
                request.addMarker("cache-hit");
                Response<?> response = request.parseNetworkResponse(new NetworkResponse(entry.getData(), entry.getCharset()));
                request.addMarker("cache-hit-parsed");
                mDelivery.postUsedCache(request);

                if (!entry.refreshNeeded()) {
                    // Completely unexpired cache hit. Just deliver the response.
                    mDelivery.postResponse(request, response);
                } else {
                    // Soft-expired cache hit. We can deliver the cached response,
                    // but we need to also send the request to the network for
                    // refreshing.
                    request.addMarker("cache-hit-refresh-needed");

                    // Mark the response as intermediate.
                    response.intermediate = true;

                    // Post the intermediate response back to the user and have
                    // the delivery then forward the request along to the network.
                    mDelivery.postResponse(request, response, new Runnable() {
                        @Override
                        public void run() {
                            try {
                                mNetworkQueue.put(request);
                            } catch (InterruptedException e) {
                                // Not much we can do about this.
                            }
                        }
                    });
                }
            } catch (InterruptedException e) {
                // We may have been interrupted because it was time to quit.
                if (mQuit) return;
            }
        }
    }

}
