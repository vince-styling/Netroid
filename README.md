
Netroid library for Android
===========================

Netroid is a http library for Android that based on [Volley](https://www.captechconsulting.com/blog/raymond-robinson/google-io-2013-volley-image-cache-tutorial),
That purpose is make your android development easier than before, provide fast, handly, useful way to do async http operation by background thread.

Feature
=========

#### 1. Base async http interaction.

As most android apps done, Netroid allow you to retrive data over http with background thread, exchange invoke result to main thread.

#### 2. Response cache base disk.

Netroid can cache your http response to disk and the cache expire time was configurable.

#### 3. Image loading solution.

Provide a powerful solution of image load, used LruImageCache as bitmap memory cache.

#### 4. Big file download solution.

Provide file download management, allows create, pause, continue, discard operation with download task, also download progress callback.

principle
=========

When Netroid startup, it create lots of thread that amount specify by developer, each thread will block with **Request Queue**,
after a new request comes, whether thread will be awake then perform http operation, finally it back to block status if that perform is done.

Basic usage
===========

The main entry of Netroid is `RequestQueue`, we highly recommnd you init it with `Application` and always let it stand with singleton :

```java
public class YourApplication extends Application {

    public void onCreate() {
        super.onCreate();

        // you can choose HttpURLConnection or HttpClient to execute request.
        Network network = new BasicNetwork(new HurlStack(Const.USER_AGENT, null), HTTP.UTF_8);

        // you can specify parallel thread amount, here is 4.
        // also instance the DiskBaseCache by your settings.
        RequestQueue mQueue = new RequestQueue(network, 4,
            new DiskCache(new File(ctx.getCacheDir(), Const.HTTP_DISK_CACHE_DIR_NAME), Const.HTTP_DISK_CACHE_SIZE));

        // start and waiting requests.
        mQueue.start();
    }

}
```

In anywhere, the only one you should do just take the `RequestQueue` instance, then simply add your request instance into RequestQueue :

```java
StringRequest request = new StringRequest(url, new Listener<String>() {
    ProgressDialog mPrgsDialog;

    @Override
    public void onPreExecute() {
        mPrgsDialog = ProgressDialog.show(Activity.this, null, "loading...", true, true);
    }

    // cancel the dialog with onFinish() callback
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

Download the [latest JAR](http://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=com.duowan.android.netroid&a=netroid&v=LATEST)
or grab via Maven:

```xml
<dependency>
  <groupId>com.duowan.android.netroid</groupId>
  <artifactId>netroid</artifactId>
  <version>(insert latest version)</version>
</dependency>
```

At this point latest version is `1.2.1`.

Documentation
===========

For more detail about what Netroid can do, pay attention to the [docs](http://netroid.cn/), that written by chinese.

Sample Application
==================

We build the [sample apk](http://netroid.cn/attach/netroid-sample-1.2.1.apk), you can try all function without any source code.

License
=======

```text
Copyright 2013 Vince Styling

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
