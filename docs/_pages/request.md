title: Netroid Request
decorator: post
slug: request.html
‡‡‡‡‡‡‡‡‡‡‡‡‡‡

# 请求处理

Netroid提供了几个通用的请求实现类，包括：

| |
| :------------- |
| `StringRequest`提取响应结果为字符串的请求。 |
| `JsonObjectRequest`提取响应结果为Json对象的请求。 |
| `JsonArrayRequest`提取响应结果为Json数组的请求。 |
| `ImageRequest`提取响应结果为图片(Bitmap)的请求。 |

这些不同的请求方式最核心的部分来自于实现`Request`基类的 **parseNetworkResponse()** 方法，各自对返回结果进行解析处理：

```java
// 解析为字符串的实现示例
public class StringImplRequest extends Request<String> {
    // NetworkResponse中包含返回结果的byte[]数组以及返回结果编码方式，通过对结果进行处理，构造成目标对象返回
    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        String parsed;
        try {
            parsed = new String(response.data, response.charset);
        } catch (UnsupportedEncodingException e) {
            parsed = new String(response.data);
        }
        return Response.success(parsed, response);
    }
}

// 解析为Bitmap的实现示例
public class ImageImplRequest extends Request<Bitmap> {
    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        Bitmap bitmap;
        try {
            bitmap = BitmapFactory.decodeByteArray(response.data, 0, response.data.length);
        } catch (OutOfMemoryError e) {
            return Response.error(new ParseError(e));
        }
        return Response.success(bitmap, response);
    }
}
```


## 事件监听：

每个请求均允许传入监听器用于处理不同的执行状态，Netroid实现了开始、完成、成功、重试、失败、取消、执行网络操作、应用缓存、下载进度九种状态回调功能：

```java
public abstract class Listener<T> {
    // 在请求开始执行时回调，可做一些例如弹出ProgressDialog等等的UI操作来通知用户进行等待。
    // 由于请求是异步线程操作，在队列过长的情况下无法保证这个方法在addRequest()后立即被调用。
    public void onPreExecute() {}

    // 无论请求的执行情况是成功还是失败，这个方法都会在执行完成时回调。
    // 如果在onPreExecute()中做了一些UI提醒，建议在这个方法中取消。
    public void onFinish() {}

    // 在请求成功时回调
    public abstract void onSuccess(T response);

    // 在请求失败时回调
    public void onError(NetroidError error) {}

    // 在请求真正被取消时回调。注：Request.cancel() 方法只是做取消的标记，
    // 请求不会马上被终止，这个方法是在请求真正由于取消而终止时的回调通知。
    public void onCancel() {}

    // 如果请求选择使用缓存，但缓存不存在或过期时回调。这个方法相对于onUsedCache(), 意味着Netroid将会执行http操作。
    // 场景解释：在缓存可用的情况下，onPreExecute()跟onFinish()的回调间隔时间非常短，如果在onPreExecute()
    // 内弹出ProgressDialog来通知用户等待，可能会出现Dialog一闪而过的问题，因此提供这个回调方法作为替代。
    // 这个方法的回调同时意味着请求将会由于执行http操作而有相对长时间的等待（视网络情况而定）。
    public void onNetworking() {}

    // 如果请求设置了使用Cache，Netroid在获取到可用缓存时会回调这个方法。
    // 这个方法的回调意味着请求很快会使用缓存数据返回，不再执行实际的Http操作。
    public void onUsedCache() {}

    // 这个方法用于在请求超时需要重试前回调，可对重试次数进行统计。
    // 提示：可通过请求facebook等等被墙的网站来测试其可用性。
    public void onRetry() {}

    // 这个回调方法提供给文件下载功能使用，用于计算下载进度
    public void onProgressChange(long fileSize, long downloadedSize) {}
}
```

**Listener**中的所有方法都会由UI线程负责调用，可方便地进行用户界面交互。除 **onSuccess()**方法是抽象方法外，其它方法均可在不需要使用时不重写，避免冗余大量无用代码带来的阅读障碍。

注：Listener对于Request来说不是必须的参数，如果你不关注请求的任何执行结果，完全可以不创建Listener对象：

```java
// Listener参数为null
StringRequest request = new StringRequest(Request.Method.GET, url, null);
```

如果你需要在请求前弹出Dialog提醒用户等待：

```java
String url = "http://facebook.com/";
JsonObjectRequest request = new JsonObjectRequest(url, null, new Listener<JSONObject>() {
    ProgressDialog mPrgsDialog;

    @Override
    public void onPreExecute() {
        mPrgsDialog = ProgressDialog.show(Activity.this, null, "正在加载中", true, true, new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                // 在用户关闭dialog时标记请求为取消状态
                RequestQueue.cancelAll(REQUESTS_TAG);
            }
        });
    }

    // 在onFinish()时cancel dialog
    @Override
    public void onFinish() {
        mPrgsDialog.cancel();
    }

    @Override
    public void onSuccess(JSONObject response) {
        Toast.makeText(Activity.this, "请求结果：" + response, 2000);
    }

    @Override
    public void onError(NetroidError error) {
        Toast.makeText(Activity.this, "请求失败", 2000);
    }

    @Override
    public void onCancel() {
        Toast.makeText(Activity.this, "请求已取消", 2000);
    }
});
```

Netroid默认的请求重试次数是一次，超时时间是2500ms，可以通过 **Request.setRetryPolicy()** 进行设置。Netroid在每次请求重试前都会对请求是否已取消进行判断，目的是当遇到网络或服务器缓慢等问题时能及时退出，避免余下的重试次数继续生效。**Listener.onRetry()** 方法可用于在请求执行过程中统计重试次数或请求执行时间，在判定请求无法正常返回时取消请求：

```java
final String REQUESTS_TAG = "Request-Demo";
String url = "http://facebook.com/";
JsonObjectRequest request = new JsonObjectRequest(url, null, new Listener<JSONObject>() {
    long startTimeMs;
    int retryCount;

    @Override
    public void onPreExecute() {
        startTimeMs = SystemClock.elapsedRealtime();
        NetroidLog.e(REQUESTS_TAG);
    }

    @Override
    public void onFinish() {
        // 这里可以重新再执行这个请求
        RequestQueue.add(request);
        NetroidLog.e(REQUESTS_TAG);
    }

    @Override
    public void onRetry() {
        long executedTime = SystemClock.elapsedRealtime() - startTimeMs;
        if (++retryCount > 5 || executedTime > 30000) {
            NetroidLog.e("retryCount : " + retryCount + " executedTime : " + executedTime);
            mQueue.cancelAll(REQUESTS_TAG);
        } else {
            NetroidLog.e(REQUESTS_TAG);
        }
    }

    @Override
    public void onCancel() {
        NetroidLog.e(REQUESTS_TAG);
    }
});

request.setRetryPolicy(new DefaultRetryPolicy(5000, 20, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
request.setTag(REQUESTS_TAG);
RequestQueue.add(request);
```

示例中创建了一个超时时长为5000ms，重试20次的请求，在onPreExecute()时记录开始时间，在每次onRetry()时对重试次数及已运行时间进行判断，超出限制时取消这次请求。有些情况下，第一次请求会重试多次，但只要再发起一次请求，就非常快能获取结果，所以示例中的onFinish()方法再次执行了这个请求，这里出现的死循环问题不作解决。以下是运行过程中打印的日志：

```
01-15 22:08:29.175: ERROR/Netroid(22043): [1] 4.onPreExecute: Request-Demo
01-15 22:08:40.023: ERROR/Netroid(22043): [1] 4.onRetry: Request-Demo
01-15 22:08:50.088: ERROR/Netroid(22043): [1] 4.onRetry: Request-Demo
01-15 22:09:10.213: ERROR/Netroid(22043): [1] 4.onRetry: retryCount : 3 executedTime : 35223
01-15 22:09:50.323: ERROR/Netroid(22043): [1] 4.onRetry: retryCount : 4 executedTime : 75337
01-15 22:09:50.338: ERROR/Netroid(22043): [1] 4.onCancel: Request-Demo
01-15 22:09:51.021: ERROR/Netroid(22043): [1] 4.onFinish: Request-Demo
```

可以明显看出，在第三次重试时由于超出限定运行时间的原因已经调用了取消请求的方法，但仍然执行了第四次才真正取消，是因为回调操作与执行操作不在同一线程所致。这个例子充分说明了调用取消方法后，请求并不能立即真正地终止，几乎所有Http库都无法安全地实现立即终止正在调用的请求。


## 验证返回结果：

通常情况下，请求成功与否依赖于服务端返回的状态码，200代表成功，Netroid将回调 **Listener.onSuccess()**方法。但实际上我们往往需要对服务端的返回结果进行判断，以确认响应是否真正成功，在确认后将其中的某些字符串解析成特定的对象返回：

```java
public class BookListRequest<List<Book>> extends Request<List<Book>> {

    public BookListRequest(String url, Listener<List<Book>> listener) {
        super(url, listener);
        setCacheExpireTime(TimeUnit.MINUTES, 20);
    }

    @Override
    protected Response<List<Book>> parseNetworkResponse(NetworkResponse response) {
        try {
            String json = new String(response.data, response.charset);
            List<Book> data = convert(json);
            // 如果返回结果无效，则请求失败
            if (data == null) {
                return Response.error(new NetroidError("cannot convert respond data."));
            }
            return Response.success(data, response);
        } catch (Exception e) {
            return Response.error(new NetroidError(e));
        }
    }

    // 将服务端返回的json数据解析为列表对象
    protected List<Book> convert(String json) {
        List<Book> bookList = ...(json);
        // 判断返回的列表是否为空
        if (bookList == null || bookList.size() == 0) return null;
        return bookList;
    }

}
```

返回Json示例：

```json
{
  "status" : 200,
  "message" : "",
  "data" : {
    "curPageNo" : 1,
    "totalPageCount" : 85,
    "totalItemCount" : 1681,
    "list" : [ {
      "id" : 2967,
      "name" : "渐爱，深爱",
      "summary" : "在一个遥远的星空，两个不同的灵魂正在发生着剧烈的碰撞，随这种碰撞接踵而至的是璀璨的火花。一开始或许会",
      "coverUrl" : "http://server.com/covers/cover_44525.jpg",
      "updateStatus" : 1,
      "capacity" : 8786340,
      "lastUpdateTime" : "2013-12-30 09:46:51"
    }, {
      "id" : 2643,
      "name" : "乱盛夏倾光年",
      "summary" : "一直都相信着：在这个世界上，有很多和我一样的女孩。她们看《热血高校》、看《艋舺》、看《古惑仔》、看《",
      "coverUrl" : "http://server.com/covers/cover_46868.jpg",
      "updateStatus" : 1,
      "capacity" : 3445264,
      "lastUpdateTime" : "2013-12-30 08:34:25"
    } ]
  }
}
```

在上面的返回Json格式示例中，我们可以看到"status"字段，这个字段是服务端处理请求的状态码，如果状态码不是200，我们认为请求失败。**BookListRequest** 的实现代码中，我们解析返回结果为List后，对List的size进行了判断，如果List为空时也认为请求失败。


## 优先层次：

同样地，Netroid允许设置请求的优先级层次，但需要继承并重写 **getPriority()** 方法，暂不提供 **setPriority()** 方法：

```java
public class SelfRequest extends Request<Void> {
    ...

    @Override
    public Priority getPriority() {
        return Priority.NORMAL;
    }

    ...
}

// 优先级的枚举值如下：
public enum Priority {
    LOW,
    NORMAL,     // 每个请求默认为normal级别
    HIGH,
    IMMEDIATE
}
```

通常情况下，不建议改变请求的优先级顺序，对于需要执行批量任务的场景，建议设置优先级为**LOW**，这样在大量的任务提交到`RequestQueue`时不至于阻塞普通的交互，后续发起的单个请求可以优先执行。


## 请求Header：

在**RequestQueue**初始化并构造HttpStack实例时设置的User-Agent是一个全局默认的请求Header，允许调用每个请求的**Request.addHeader(...)**方法添加更多的Header，通常的做法是在构造Request实例时进行设置：

```java
public class YourRequest extends Request {
    ...
    public YourRequest(String url, Listener<String> listener) {
        super(Method.GET, url, listener);
        addHeader("From", "client");
        addHeader("Via", "netroid");
        addHeader("Accept-Charset", "UTF-8");
        addHeader("Origin", "http://netroid.cn/");
    }
    ...
}
```

有时候可能你需要在每个请求里都添加一些默认Header，而每次都手动调用Request.addHeader(...)的做法显然太麻烦，因此这种场景可以通过继承HttpStack的实例，拦截**performRequest**方法来达到目的：

```java
public class SelfHurlStack extends HurlStack {
    @Override
    public HttpResponse performRequest(Request<?> request) throws IOException, AuthFailureError {

        // 为每个请求添加公共的Header
        request.addHeader("Via", "netroid");
        request.addHeader("Accept-Charset", "UTF-8");
        request.addHeader("Origin", "http://netroid.cn/");

        return super.performRequest(request);
    }
}
```

构造请求对象或拦截**performRequest**方法来设置Header的做法可以添加一些在请求过程中持久化的Header，但例外的情况是，如果你的Header是通过一定的逻辑运算而得到的，这两种做法均显得不合适。Netroid在请求超时后会重新执行一次或多次请求(可设置重试策略来控制次数)，此时如果你的Header需要在重试前再次运算来更新，以上两种做法就满足不了需求，Netroid为此提供了**Request.prepare()**方法用于解决这个问题。

```java
public class Request<T> {
    ...
    public void prepare() {
        // 下载场景时，每次重试均需要重新计算Range以确保断点续传的起始位置正确
        addHeader("Range", "bytes=" + TemporaryFile.length() + "-");
    }
    ...
}
```

这个方法的典型应用场景在断点下载，由于文件的体积或对方服务器限制超长连接的种种原因，有时并不能一次连接就实现下载所有数据，因此需要重试机制去解决这个问题。上一次的下载实际上已经使临时文件有了一定的增长，所以必须在重试前更新Header来通知服务端起始位置，避免从旧位置开始下载，Netroid认为临时文件不合法而终止的错误。

换句话说，**prepare()** 允许你在开始下载前构造那些需要通过一定的逻辑计算来确定的请求参数，以保证重新发起的请求的状态是最新的。

#### 注：**Request.addHeader(...)** 方法不允许设置多个同名Key-Values，在设置前会执行 **removeHeader(...)** 删除之前的旧数据。


## 响应Header：

开发者可以通过拦截 **Request.handleResponse()** 方法来获取响应的Header：

```java
public class YourRequest extends Request {
    ...
    @Override
    public byte[] handleResponse(HttpResponse response, Delivery delivery) throws IOException, ServerError {
        Header[] headers = response.getAllHeaders();
        for (Header header : headers) {
            NetroidLog.d(header.toString());
        }
        return super.handleResponse(response, delivery);
    }
    ...
}
```

**handleResponse()** 方法是Netroid核心中用于处理返回结果的方法，开发者同样可以重写这个方法自己处理请求结果，具体做法可参考 **FileDownloadRequest** 中的示例。


## 执行Post请求：

默认情况下，所有的请求均为Get请求，执行带实体内容的Post、Put请求可通过以下两种方法来实现：

> 1、重写 **Request.getParams()** 方法：

```java
public class PostByParamsRequest extends StringRequest {
    private Map<String, String> mParams;

    // 传入Post参数的Map集合
    public PostByParamsRequest(String url, Map<String, String> params, Listener<String> listener) {
        super(Method.POST, url, listener);
        mParams = params;
    }

    @Override
    public Map<String, String> getParams() throws AuthFailureError {
        return mParams;
    }
}

RequestQueue.add(new PostByParamsRequest(url, params, listener));
```

> 2、重写 **Request.getBody()** 方法：

```java
public class PostByEncodeBodyRequest extends StringRequest {
    public PostByEncodeBodyRequest(String url, Listener<String> listener) {
        super(Method.POST, url, listener);
    }

    @Override
    public byte[] getBody() throws AuthFailureError {
        // 自己进行请求实体内容的编码，可进行加密等操作再返回，具体做法参照Request.getBody()的实现
        return bytes;
    }
}
RequestQueue.add(new PostByEncodeBodyRequest(url, listener));
```

同样地，Netroid允许开发者指定Post参数的编码格式及Body-Content-Type，可通过重写以下两个方法定制：

```java
public class Request {
    // 指定参数的编码，默认为UTF-8
    protected String getParamsEncoding() {
        return DEFAULT_PARAMS_ENCODING;
    }

    // 指定Post的内容类型
    public String getBodyContentType() {
        return "application/x-www-form-urlencoded; charset=" + getParamsEncoding();
    }
}
```


## 执行非http请求：

为了允许 **ImageLoader** 加载不同来源的图片文件，Netroid实现了执行非http操作的支持：

```java
ImageRequest request = new ImageRequest("sdcard:///sdcard/DCIM/Camera/IMG.jpg", ...) {
    // 核心代码在于重写perform方法，返回NetworkResponse实例
    @Override
    public NetworkResponse perform() {
        try {
            return new NetworkResponse(toBytes(new FileInputStream(getUrl())), HTTP.UTF_8);
        } catch (IOException e) {
            return new NetworkResponse(new byte[1], HTTP.UTF_8);
        }
    }
};
```

Netroid的线程池在应用程序中应该扮演一个高度可复用的组件，您不仅仅可用于执行Http操作，还可以执行DB操作、IO操作、数据运算等等复杂的逻辑。这些代码如果在UI线程上执行，将占用宝贵的界面响应资源，导致"Application Not Responding" (ANR) dialog弹出的问题，严重影响应用程序的用户体验。

现在，你只需要重写 **Request.perform()** 方法，就能够非常方便地将所有阻塞UI线程的非UI操作置于后端，同时还能在有需要的情况下应用Netroid的缓存方案，这对于简化开发进程来讲是很有好处的。

#### 注：DB、SharedPreferences操作实际上就是IO操作，具体可查看Android开发者官网的文章。

