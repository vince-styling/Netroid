package com.duowan.mobile.netroid.toolbox;

import android.os.Looper;
import com.duowan.mobile.netroid.Listener;
import com.duowan.mobile.netroid.NetroidError;
import com.duowan.mobile.netroid.RequestQueue;
import com.duowan.mobile.netroid.request.FileDownloadRequest;

import java.util.LinkedList;

/**
 * This class provided continuous transmission on the breakpoint download task management utilities.
 * As long as the site support(enable 'Content-Range' Header), we'd retrieve last download temporary
 * file when we start to download, fetch the temporary file length as current Range begin point,
 * then the last downloaded data effective.
 * Sadly is when the server-side didn't support download by Range,
 * we'll delete the temporary file and start download from beginning.
 *
 * Usage: like {@link ImageLoader}, the best way to use this class is create by {@link RequestQueue}
 * and stand Singleton, just get the only one instance to do everything in anywhere.
 * To start a new download request, invoke the {@link FileDownloader#add(String, String, com.duowan.mobile.netroid.Listener)}
 * to pass in the task parameters, it will deploy to the Task Queue and execute as soon as possible.
 *
 * Note: For the multithreading and bandwidth limit reason,
 * we normally start two parallel tasks to download data,
 * don't over three, and remember keep one idle thread to
 * perform common http request at least.
 */
public class FileDownloader {
	/** RequestQueue for dispatching DownloadRequest. */
	private final RequestQueue mRequestQueue;

	/** The parallel task count, recommend less than 3. */
	private final int mParallelTaskCount;

	/** The linked Task Queue. */
	private final LinkedList<DownloadController> mTaskQueue;

	/**
	 * Construct Downloader and init the Task Queue.
	 * @param queue The RequestQueue for dispatching Download task.
	 * @param parallelTaskCount
	 * 				Allows parallel task count,
	 * 				don't forget the value must less than ThreadPoolSize of the RequestQueue.
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
	 *
	 * Note: don't perform this method twice or more with same parameters, because we didn't check for
	 * duplicate tasks, it rely on developer done.
	 *
	 * Note: this method should invoke in the main thread.
	 *
	 * @param storeFilePath Once download successed, we'll find it by the store file path.
	 * @param url The download url.
	 * @param listener The event callback by status;
	 * @return The task controller allows pause or resume or discard operation.
	 */
	public DownloadController add(String storeFilePath, String url, Listener<Void> listener) {
		// only fulfill requests that were initiated from the main thread.(reason for the Delivery?)
		throwIfNotOnMainThread();

		DownloadController controller = new DownloadController(storeFilePath, url, listener);
		synchronized (mTaskQueue) {
			mTaskQueue.add(controller);
		}
		schedule();
		return controller;
	}

	/**
	 * Scanning the Task Queue, fetch a {@link DownloadController} who match the two parameters.
	 * @param storeFilePath The storeFilePath to compare.
	 * @param url The url to compare.
	 * @return The matched {@link DownloadController}.
	 */
	public DownloadController get(String storeFilePath, String url) {
		synchronized (mTaskQueue) {
			for (DownloadController controller : mTaskQueue) {
				if (controller.mStoreFilePath.equals(storeFilePath) &&
						controller.mUrl.equals(url)) return controller;
			}
		}
		return null;
	}

	/**
	 * Traverse the Task Queue, count the running task then deploy more if it can be.
	 */
	private void schedule() {
		// make sure only one thread can manipulate the Task Queue.
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
			while (mTaskQueue.size() > 0) {
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
	 * such as add customize headers or handle the response himself. <br/>
	 * Note : before you override this, make sure you are understood the {@link FileDownloadRequest} very well.
	 */
	public FileDownloadRequest buildRequest(String storeFilePath, String url) {
		return new FileDownloadRequest(storeFilePath, url);
	}

	/**
	 * This class included all such as PAUSE, RESUME, DISCARD to manipulating download task,
	 * it created by {@link FileDownloader#add(String, String, com.duowan.mobile.netroid.Listener)},
	 * offer three params to constructing {@link FileDownloadRequest} then perform http downloading,
	 * you can check the download status whenever you want to know.
	 */
	public class DownloadController {
		// Persist the Request createing params for re-create it when pause operation gone.
		private Listener<Void> mListener;
		private String mStoreFilePath;
		private String mUrl;

		// The download request.
		private FileDownloadRequest mRequest;

		private int mStatus;
		public static final int STATUS_WAITING = 0;
		public static final int STATUS_DOWNLOADING = 1;
		public static final int STATUS_PAUSE = 2;
		public static final int STATUS_SUCCESS = 3;
		public static final int STATUS_DISCARD = 4;

		private DownloadController(String storeFilePath, String url, Listener<Void> listener) {
			mStoreFilePath = storeFilePath;
			mListener = listener;
			mUrl = url;
		}

		/**
		 * For the parallel reason, only the {@link FileDownloader#schedule()} can call this method.
		 * @return true if deploy is successed.
		 */
		private boolean deploy() {
			if (mStatus != STATUS_WAITING) return false;

			mRequest = buildRequest(mStoreFilePath, mUrl);

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
		 * Pause this task when it status was DOWNLOADING, in fact, we just marked the request should be cancel,
		 * http request cannot stop immediately, we assume it will finish soon, thus we set the status as PAUSE,
		 * let Task Queue deploy a new Request, that will cause parallel tasks growing beyond maximum task count,
		 * but it doesn't matter, we believe that situation never longer.
		 * @return true if did the pause operation.
		 */
		public boolean pause() {
			if (mStatus == STATUS_DOWNLOADING) {
				mStatus = STATUS_PAUSE;
				mRequest.cancel();
				schedule();
				return true;
			}
			return false;
		}

		/**
		 * Resume this task when it status was PAUSE, we will turn the status as WAITING, then re-schedule the Task Queue,
		 * if parallel counter take an idle place, this task will re-deploy instantly,
		 * if not, the status will stay WAITING till idle occur.
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
