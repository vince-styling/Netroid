
Netroid library for Android
===========================

Netroid is a http library for Android that based on [Volley](https://www.captechconsulting.com/blog/raymond-robinson/google-io-2013-volley-image-cache-tutorial),
That purpose is make your android development easier than before, provide fast, handly, useful way to do async http operation by background thread.

Feature
=========

### 1. Base async http interaction.

As most android apps done, Netroid allow you to retrive data over http with background thread, exchange invoke result to main thread.

### 2. Response cache base disk.

Netroid can cache your http response to disk and the cache expire time was configurable.

### 3. Image loading solution.

Provide a powerful solution of image load, used LruImageCache as bitmap memory cache.

### 4. Big file download solution.

Provide file download management, allows create, pause, continue, discard operation with download task, also download progress callback.

principle
=========

When Netroid startup, it create lots of thread that amount specify by developer, each thread will block with **Request Queue**,
after a new request comes, whether thread will be awake then perform http operation, finally it back to block status if that perform is done.

Basic usage
===========

The main entry of Netroid is `RequestQueue` :

```java
Network network = new BasicNetwork(new HurlStack(Const.USER_AGENT, null), HTTP.UTF_8);
RequestQueue mQueue = new RequestQueue(network, 4,
    new DiskCache(new File(ctx.getCacheDir(), Const.HTTP_DISK_CACHE_DIR_NAME), Const.HTTP_DISK_CACHE_SIZE));
```

we can perform a request simply add a request instance into RequestQueue :

```java
StringRequest request = new StringRequest(url, new Listener<String>() {
    ProgressDialog mPrgsDialog;

    @Override
    public void onPreExecute() {
        mPrgsDialog = ProgressDialog.show(Activity.this, null, "loading...", true, true);
    }

    // we should cacne the dialog with onFinish() callback
    @Override
    public void onFinish() {
        mPrgsDialog.cancel();
    }

    @Override
    public void onSuccess(String response) {
        Toast.makeText(Activity.this, "response is ： " + response, 2000).show();
    }

    @Override
    public void onError(NetroidError error) {
        Toast.makeText(Activity.this, error.getMessage(), 2000).show();
    }

    @Override
    public void onCancel() {
        Toast.makeText(Activity.this, "request was cancel", 2000).show();
    }
});

// add the request to RequestQueue, will execute quickly if has idle thread
mQueue.add(request);
```

Do not forget add internet permission to the `AndroidManifest.xml` file :

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

Integration
===========

Netroid has not yet to publish to Maven Centra Repository or Gradle, In order to use this library from you Android project,
please place the [core](http://netroid.cn/attach/netroid-1.2.1.jar) jar file to your project, we'll publish as soon as possible.

For more detail about what Netroid can do, pay attention to the [docs](http://netroid.cn/), that written by chinese.

We provide the [sample apk](http://netroid.cn/attach/netroid-sample-1.2.1.apk), you can try all function without any source code.