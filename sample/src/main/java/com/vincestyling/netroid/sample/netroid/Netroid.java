package com.vincestyling.netroid.sample.netroid;

import android.os.Build;
import android.widget.ImageView;
import com.vincestyling.netroid.*;
import com.vincestyling.netroid.cache.DiskCache;
import com.vincestyling.netroid.widget.NetworkImageView;
import com.vincestyling.netroid.stack.HttpClientStack;
import com.vincestyling.netroid.stack.HttpStack;
import com.vincestyling.netroid.stack.HurlStack;
import com.vincestyling.netroid.toolbox.BasicNetwork;
import com.vincestyling.netroid.toolbox.FileDownloader;
import com.vincestyling.netroid.toolbox.ImageLoader;
import org.apache.http.protocol.HTTP;

import java.util.concurrent.Executor;

public class Netroid {
    public static final String USER_AGENT = "netroid_sample";
    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;
    private Network mNetwork;
    private DiskCache mDiskCache;
    private FileDownloader mFileDownloader;

    private Netroid() {
        /* cannot be instantiated */
    }

    private static Netroid mInstance;

    public static void init(DiskCache cache) {
        mInstance = new Netroid();

        mInstance.mDiskCache = cache;

        HttpStack stack;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            stack = new HurlStack(USER_AGENT, null);
        } else {
            // Prior to Gingerbread, HttpUrlConnection was unreliable.
            // See: http://android-developers.blogspot.com/2011/09/androids-http-clients.html
            stack = new HttpClientStack(USER_AGENT);
        }

        mInstance.mNetwork = new BasicNetwork(stack, HTTP.UTF_8);
        int poolSize = RequestQueue.DEFAULT_NETWORK_THREAD_POOL_SIZE;
        mInstance.mRequestQueue = new RequestQueue(mInstance.mNetwork, poolSize, cache);
        mInstance.mRequestQueue.start();
    }

    public static RequestQueue getRequestQueue() {
        if (mInstance.mRequestQueue != null) {
            return mInstance.mRequestQueue;
        } else {
            throw new IllegalStateException("RequestQueue not initialized");
        }
    }

    public static void add(Request request) {
        getRequestQueue().add(request);
    }

    /**
     * Perform given request as blocking mode. Note make sure won't invoke on main thread.
     */
    public static void perform(Request request) {
        // you might want to keep the ExecutorDelivery instance as Field, but it's
        // cheap constructing every time, depends how often you use this way.
        RequestPerformer.perform(request, Netroid.getNetwork(), new ExecutorDelivery(new Executor() {
            @Override
            public void execute(Runnable command) {
                // invoke run() directly.
                command.run();
            }
        }));
    }

    public static void setImageLoader(ImageLoader imageLoader) {
        mInstance.mImageLoader = imageLoader;
    }

    public static ImageLoader getImageLoader() {
        if (mInstance.mImageLoader != null) {
            return mInstance.mImageLoader;
        } else {
            throw new IllegalStateException("ImageLoader not initialized");
        }
    }

    public static Network getNetwork() {
        if (mInstance.mNetwork != null) {
            return mInstance.mNetwork;
        } else {
            throw new IllegalStateException("Network not initialized");
        }
    }

    public static DiskCache getDiskCache() {
        return mInstance.mDiskCache;
    }

    public static void setFileDownloder(FileDownloader downloder) {
        mInstance.mFileDownloader = downloder;
    }

    public static FileDownloader getFileDownloader() {
        if (mInstance.mFileDownloader != null) {
            return mInstance.mFileDownloader;
        } else {
            throw new IllegalStateException("FileDownloader not initialized");
        }
    }

    public static void displayImage(String url, ImageView imageView, int defaultImageResId, int errorImageResId) {
        ImageLoader.ImageListener listener = ImageLoader.getImageListener(imageView, defaultImageResId, errorImageResId);
        getImageLoader().get(url, listener, 0, 0);
    }

    public static void displayImage(String url, ImageView imageView) {
        displayImage(url, imageView, 0, 0);
    }

    public static void displayImage(String url, NetworkImageView imageView, int defaultImageResId, int errorImageResId) {
        imageView.setDefaultImageResId(defaultImageResId);
        imageView.setErrorImageResId(errorImageResId);
        imageView.setImageUrl(url, getImageLoader());
    }

    public static void displayImage(String url, NetworkImageView imageView) {
        displayImage(url, imageView, 0, 0);
    }

    public static void destroy() {
        if (mInstance.mRequestQueue != null) {
            mInstance.mRequestQueue.stop();
            mInstance.mRequestQueue = null;
        }

        if (mInstance.mFileDownloader != null) {
            mInstance.mFileDownloader.clearAll();
            mInstance.mFileDownloader = null;
        }

        mInstance.mNetwork = null;
        mInstance.mImageLoader = null;
        mInstance.mDiskCache = null;
    }
}
