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
package com.vincestyling.netroid.toolbox;

import android.os.Looper;
import com.vincestyling.netroid.IListener;
import com.vincestyling.netroid.Listener;
import com.vincestyling.netroid.NetroidError;
import com.vincestyling.netroid.RequestQueue;
import com.vincestyling.netroid.request.FileDownloadRequest;

import java.io.File;
import java.util.LinkedList;

/**
 * This class provided continuous transmission on the breakpoint download task management utilities.
 * As long as the site support(enable 'Content-Range' Header), we'd retrieve last download temporary
 * file when we start to download, fetch the temporary file length as current Range begin point,
 * then the last downloaded data effective.
 * Sadly is when the server-side didn't support download by Range,
 * we'll delete the temporary file and start download from beginning.
 * <p/>
 * Usage: like {@link ImageLoader}, the best way to use this class is create by {@link RequestQueue}
 * and stand Singleton, just get the only one instance to do everything in anywhere.
 * To start a new download request, invoke the {@link #add(String, String, IListener)}
 * to pass in the task parameters, it will deploy to the Task Queue and execute as soon as possible.
 * <p/>
 * Note: For the multithreading and bandwidth limit reason,
 * we normally start two parallel tasks to download data,
 * don't over three, and remember keep one idle thread to
 * perform common http request at least.
 */
public class FileDownloader {
    /**
     * RequestQueue for dispatching DownloadRequest.
     */
    private final RequestQueue mRequestQueue;

    /**
     * The parallel task count, recommend less than 3.
     */
    private final int mParallelTaskCount;

    /**
     * The linked Task Queue.
     */
    private final LinkedList<DownloadController> mTaskQueue;

    /**
     * Construct Downloader and init the Task Queue.
     *
     * @param queue             The RequestQueue for dispatching Download task.
     * @param parallelTaskCount Allows parallel task count,
     *                          don't forget the value must less than ThreadPoolSize of the RequestQueue.
     */
    public FileDownloader(RequestQueue queue, int parallelTaskCount) {
        if (parallelTaskCount >= queue.getThreadPoolSize()) {
            throw new IllegalArgumentException("parallelTaskCount[" + parallelTaskCount
                    + "] must less than threadPoolSize[" + queue.getThreadPoolSize() + "] of the RequestQueue.");
        }

        mTaskQueue = new LinkedList<DownloadController>();
        mParallelTaskCount = parallelTaskCount;
        mRequestQueue = queue;
    }

    /**
     * Create a new download request, this request might not run immediately because the parallel task limitation,
     * you can check the status by the {@link DownloadController} which you got after invoke this method.
     * <p/>
     * Note: don't perform this method twice or more with same parameters, because we didn't check for
     * duplicate tasks, it rely on developer done.
     * <p/>
     * Note: this method should invoke in the main thread.
     *
     * @param storeFile Once download successed, we'll found it by the store file.
     * @param url       The download url.
     * @param listener  The event callback by status;
     * @return The task controller allows pause or resume or discard operation.
     */
    public DownloadController add(File storeFile, String url, IListener<Void> listener) {
        // only fulfill requests that were initiated from the main thread.(reason for the Delivery?)
        throwIfNotOnMainThread();

        DownloadController controller = new DownloadController(storeFile, url, listener);
        synchronized (mTaskQueue) {
            mTaskQueue.add(controller);
        }
        schedule();
        return controller;
    }

    /**
     * @see {@link #add(File, String, IListener)}
     */
    public DownloadController add(String storeFilePath, String url, IListener<Void> listener) {
        return add(new File(storeFilePath), url, listener);
    }

    /**
     * Scanning the Task Queue, fetch a {@link DownloadController} who match the two parameters.
     *
     * @param storeFile The store file to looking for.
     * @param url       The url which download for.
     * @return The matched {@link DownloadController}.
     */
    public DownloadController get(File storeFile, String url) {
        synchronized (mTaskQueue) {
            for (DownloadController controller : mTaskQueue) {
                if (controller.mStoreFile.equals(storeFile) &&
                        controller.mUrl.equals(url)) return controller;
            }
        }
        return null;
    }

    /**
     * @see {@link #get(File, String)}
     */
    public DownloadController get(String storeFilePath, String url) {
        return get(new File(storeFilePath), url);
    }

    /**
     * Traverse the Task Queue, count the running task then deploy more if it can be.
     */
    private void schedule() {
        // make sure only one thread able manipulate the Task Queue.
        synchronized (mTaskQueue) {
            // counting ran task.
            int parallelTaskCount = 0;
            for (DownloadController controller : mTaskQueue) {
                if (controller.isDownloading()) parallelTaskCount++;
            }
            if (parallelTaskCount >= mParallelTaskCount) return;

            // try to deploy all Task if they're await.
            for (DownloadController controller : mTaskQueue) {
                if (controller.deploy() && ++parallelTaskCount == mParallelTaskCount) return;
            }
        }
    }

    /**
     * Remove the controller from the Task Queue, re-schedule to make those waiting task deploys.
     *
     * @param controller The controller which will be remove.
     */
    private void remove(DownloadController controller) {
        // also make sure one thread operation
        synchronized (mTaskQueue) {
            mTaskQueue.remove(controller);
        }
        schedule();
    }

    /**
     * Clear all tasks, make the Task Queue empty.
     */
    public void clearAll() {
        // make sure only one thread can manipulate the Task Queue.
        synchronized (mTaskQueue) {
            while (!mTaskQueue.isEmpty()) {
                mTaskQueue.get(0).discard();
            }
        }
    }

    private void throwIfNotOnMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("FileDownloader must be invoked from the main thread.");
        }
    }

    /**
     * This method can override by developer to change download behaviour,
     * such as add customize headers even handle the response itself. <br/>
     * Note : before you override this, make sure you are understood the {@link FileDownloadRequest} very well.
     */
    public FileDownloadRequest buildRequest(File storeFile, String url) {
        return new FileDownloadRequest(storeFile, url);
    }

    /**
     * This class included all such as PAUSE, RESUME, DISCARD to manipulating download task,
     * it created by {@link #add(String, String, IListener)},
     * offer three params to constructing {@link FileDownloadRequest} then perform http downloading,
     * you can check the download status whenever you want to know.
     */
    public class DownloadController {
        // Persist the Request createing params for re-create it when pause operation gone.
        private IListener<Void> mListener;
        private File mStoreFile;
        private String mUrl;

        // The download request.
        private FileDownloadRequest mRequest;

        private int mStatus;
        public static final int STATUS_WAITING = 0;
        public static final int STATUS_DOWNLOADING = 1;
        public static final int STATUS_PAUSE = 2;
        public static final int STATUS_SUCCESS = 3;
        public static final int STATUS_DISCARD = 4;

        private DownloadController(String storeFilePath, String url, IListener<Void> listener) {
            this(new File(storeFilePath), url, listener);
        }

        private DownloadController(File storeFile, String url, IListener<Void> listener) {
            mStoreFile = storeFile;
            mListener = listener;
            mUrl = url;
        }

        /**
         * For the parallel reason, only the {@link #schedule()} can call this method.
         *
         * @return true if deploy is successed.
         */
        private boolean deploy() {
            if (mStatus != STATUS_WAITING) return false;

            mRequest = buildRequest(mStoreFile, mUrl);

            // we create a Listener to wrapping that Listener which developer specified,
            // for the onFinish(), onSuccess(), onError() won't call when request was cancel reason.
            mRequest.setListener(new Listener<Void>() {
                boolean isCanceled;

                @Override
                public void onPreExecute() {
                    mListener.onPreExecute();
                }

                @Override
                public void onFinish() {
                    // we don't inform FINISH when it was cancel.
                    if (!isCanceled) {
                        mStatus = STATUS_SUCCESS;
                        mListener.onFinish();
                        // when request was FINISH, remove the task and re-schedule Task Queue.
                        remove(DownloadController.this);
                    }
                }

                @Override
                public void onSuccess(Void response) {
                    // we don't inform SUCCESS when it was cancel.
                    if (!isCanceled) mListener.onSuccess(response);
                }

                @Override
                public void onError(NetroidError error) {
                    // we don't inform ERROR when it was cancel.
                    if (!isCanceled) mListener.onError(error);
                }

                @Override
                public void onCancel() {
                    mListener.onCancel();
                    isCanceled = true;
                }

                @Override
                public void onProgressChange(long fileSize, long downloadedSize) {
                    mListener.onProgressChange(fileSize, downloadedSize);
                }
            });

            mStatus = STATUS_DOWNLOADING;
            mRequestQueue.add(mRequest);
            return true;
        }

        public int getStatus() {
            return mStatus;
        }

        public boolean isDownloading() {
            return mStatus == STATUS_DOWNLOADING;
        }

        /**
         * Pause this task when it status was DOWNLOADING|WAITING. In fact, we just marked the request should be cancel,
         * http request cannot stop immediately, we assume it will finish soon, thus we set the status as PAUSE,
         * let Task Queue deploy a new Request. That will cause parallel tasks growing beyond maximum task count,
         * but it doesn't matter, we expected that situation never stay longer.
         *
         * @return true if did the pause operation.
         */
        public boolean pause() {
            switch (mStatus) {
                case STATUS_DOWNLOADING:
                    mRequest.cancel();
                case STATUS_WAITING:
                    mStatus = STATUS_PAUSE;
                    schedule();
                    return true;
                default:
                    return false;
            }
        }

        /**
         * Resume this task when it status was PAUSE, we will turn the status as WAITING, then re-schedule the Task Queue,
         * if parallel counter take an idle place, this task will re-deploy instantly,
         * if not, the status will stay WAITING till idle occur.
         *
         * @return true if did the resume operation.
         */
        public boolean resume() {
            if (mStatus == STATUS_PAUSE) {
                mStatus = STATUS_WAITING;
                schedule();
                return true;
            }
            return false;
        }

        /**
         * We will discard this task from the Task Queue, if the status was DOWNLOADING,
         * we first cancel the Request, then remove task from the Task Queue,
         * also re-schedule the Task Queue at last.
         *
         * @return true if did the discard operation.
         */
        public boolean discard() {
            if (mStatus == STATUS_DISCARD) return false;
            if (mStatus == STATUS_SUCCESS) return false;
            if (mStatus == STATUS_DOWNLOADING) mRequest.cancel();
            mStatus = STATUS_DISCARD;
            remove(this);
            return true;
        }
    }

}
