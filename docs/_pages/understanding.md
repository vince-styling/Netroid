title: Netroid Understanding
decorator: post
slug: understanding.html
‡‡‡‡‡‡‡‡‡‡‡‡‡‡

# 运行原理

Netroid在启动时创建以下几个实例用于提供网络服务：

| |
| :------------- |
| `NetWork`用于执行Http请求的对象，有HttpUrlConnection、HttpClient两种执行方式，可以自行指定使用哪种方式。 |
| `Network Thread Pool`调用Http请求执行对象(**NetWork**)的线程池，默认为四个线程(NetworkDispatcher)的容量，在初始化时可进行指定。每个线程由BlockingQueue.take()方法进行阻塞，然后一直处于等待状态，在新请求进入队列后马上开始执行。 |
| `Cache Dispatcher`每个需要进行缓存操作的请求都将首先由这个线程执行，在缓存未过期时直接返回缓存数据，否则将请求放进 **Network Queue** 队列。 |
| `Cache Queue`需要执行缓存操作的Http请求对象队列，**Cache Dispatcher**通过获取到达这个队列的请求来执行缓存检测。 |
| `Network Queue`需要执行网络操作的Http请求对象队列，**Network Thread Pool**通过获取到达这个队列的请求来执行实际的网络操作。 |
| `Response Delivery`执行返回请求结果的功能，内部通过Handler.post(Runnable)来实现在UI线程上处理响应结果，区分请求成功和请求失败两种情况。 |

在请求到达时，每个组件中将会依次对请求执行处理，流程如下：

![Netroid Request Handling Flowchart](/netroid_request_handling_flowchart.png "Netroid Request Handling Flowchart")


# 组件介绍

程序入口为`RequestQueue`类，提供以下几个主要的接口进行交互执行相关操作。

```java
public interface RequestQueue {
    // 启动Netroid服务的入口
    public void start();

    // 停止Netroid服务
    public void stop();

    // 根据Request的Tag标记来停止某一同类型请求
    // 注：Netroid只是做了退出的标记，请求不会马上被终止
    public void cancelAll(Object tag);

    // 将一个创建好的请求实例添加到队列，由队列执行
    public void add(Request request);
}
```

大多数情况下，**RequestQueue** 类需要接收三个简单的参数来进行初始化，分别是Http请求执行对象(Network)、Netroid线程池容量、请求的硬盘缓存方案：

```java
public RequestQueue(Network network, int threadPoolSize, DiskCache cache);
```

## 构造Network：

**BasicNetwork** 是Netroid提供的 **Network** 接口实现类：

```java
// 请求响应的编码由"Content-Type" Header决定，defaultCharset用于当服务端未返回编码方式时的默认选择
public BasicNetwork(HttpStack httpStack, String defaultCharset);
```

`HttpStack`接口实例用于指示网络请求的执行方式，Netroid提供了**HurlStack**、**HttpClientStack**两种默认实现，对于这两种执行方式的选择，Volley建议GingerBread(API9)或以上的Android系统选择用HurlStack，参考写法如下：

```java
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
    httpStack = new HurlStack(userAgent);
} else {
    // Prior to Gingerbread, HttpUrlConnection was unreliable.
    // See: http://android-developers.blogspot.com/2011/09/androids-http-clients.html
    httpStack = new HttpClientStack(userAgent);
}
```

#### 注：由于Android中的HttpClient在API9之后的版本中不具备优势，HttpClientStack仍有相当多的Bug未解决，鉴于Android 4.0或以上的版本已经相当普及，所以没有投入精力去解决这些问题，建议开发者使用 **HurlStack**。

User-Agent是HttpStack接口的两个实现类中默认提供的 **Header** 设置，你可以根据自己的需要设置UA，也可以直接使用android package name作为UA：

```java
String userAgent = "netroid/0";
try {
    String packageName = context.getPackageName();
    PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
    userAgent = packageName + "/" + info.versionCode;
} catch (NameNotFoundException e) {
}
```

## 缓存方案的用法：

Netroid默认提供两种类型的Cache，均使用了Lru算法：

| |
| :------------- |
| `DiskCache`基于硬盘的持久化缓存，所有请求均可使用。 |
| `BitmapImageCache`基于内存的图片缓存，专属于 **ImageLoader** 的缓存方案。 |

Netroid在执行请求时默认不使用任何缓存，所以在初始化`RequestQueue`对象时，开发者必须手动构造硬盘缓存方案。同样地，**ImageLoader** 也需要做相同的操作来指定内存缓存。

这两种缓存方案需要开发者构建，所以你可以继承它们并加以扩展。但实际上Netroid提供的默认实现已经能够满足大部分的应用场景，典型的写法如下：

```java
// 创建Netroid主类，指定硬盘缓存方案
mRequestQueue = new RequestQueue(..., new DiskCache(new File("/sdcard/netroid/"), 50 * 1024 * 1024));

// 创建ImageLoader实例，指定内存缓存方案
mImageLoader = new SelfImageLoader(mRequestQueue, new BitmapImageCache(2 * 1024 * 1024));
```

所有的Http请求均可使用硬盘缓存方案，开发者必须在发起一个新的Http请求时通过 **setCacheExpireTime(...)** 指定过期时间，才能使缓存生效。

```java
String url = "http://server.domain/json_array.json";
JsonArrayRequest request = new JsonArrayRequest(url, new Listener<JSONArray>() {
    @Override
    public void onSuccess(JSONArray response) {
        showResult(response.toString());
    }

    @Override
    public void onError(NetroidError error) {
        showError(error);
    }
});

// 设置为十天之后过期
request.setCacheExpireTime(TimeUnit.DAYS, 10);
mQueue.add(request);
```

特殊情况下，你需要不读取缓存而是直接执行网络请求，然后再将返回结果保存进缓存，做强制刷新操作，
Netroid提供了 **setForceUpdate(true)** 设置允许你实现这种需求：

```java
request.setForceUpdate(true);
request.setCacheExpireTime(TimeUnit.DAYS, 10);
```

以上的写法指定Netroid将不检索缓存情况，马上执行网络请求操作，获得响应结果后放入硬盘缓存中，缓存的过期时间设置为十天。

