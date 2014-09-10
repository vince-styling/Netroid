title: Netroid FileDownloader
decorator: post
slug: filedownloader.html
‡‡‡‡‡‡‡‡‡‡‡‡‡‡

# 大文件下载

Netroid实现的 `FileDownloader` 对断点续传方式的大文件下载提供了支持，其内部维护一个下载队列，所以在创建时需要指定最大并行任务数，超出限制的任务将自动进入等待队列。在设置最大并行任务数后，开发者只需要往队列中不断添加任务，其它的事情均由 **FileDownloader** 完成。

FileDownloader将在任务添加成功时返回 `DownloadController` 实例对象，这个对象提供了查看任务执行状态、暂停、继续、取消四项必需的操作功能，开发者只需要持有这个对象，即可随时掌控任务的所有情况。

FileDownloader的用法类似于 **ImageLoader**，用单例模式创建一个全局的实例，在初始化 **RequestQueue** 时构造：

```java
int poolSize = RequestQueue.DEFAULT_NETWORK_THREAD_POOL_SIZE; // 默认为4
RequestQueue mQueue = new RequestQueue(Network, poolSize);
// 建议并行任务数上限不超过3，在手机带宽有限的条件下，并行任务数的扩大无法加快下载速度。
// 注：如果并行任务数上限大于或等于RequestQueue中的总线程数，将被视为不合法而抛出异常。
FileDownloader mDownloader = new FileDownloader(mQueue, 1);
```

调用 **FileDownloader.add()** 方法即可创建新任务：

```java
// down.file是保存的文件名，这个文件只在下载成功后才存在，在下载过程中，
// Netroid会在文件路径下创建一个临时文件，命名为：down.file.tmp，下载成功后更名为down.file。
FileDownloader.DownloadController controller = FileDownloader.add(
    "/sdcard/netroid/down.file", "http://server.com/res/down.file",
    new Listener<Void>() {
        // 注：如果暂停或放弃了该任务，onFinish()不会回调
        @Override
        public void onFinish() {
            Toast.makeText("下载完成").show();
        }

        // 注：如果暂停或放弃了该任务，onSuccess()不会回调
        @Override
        public void onSuccess(Void response) {
            Toast.makeText("下载成功").show();
        }

        // 注：如果暂停或放弃了该任务，onError()不会回调
        @Override
        public void onError(NetroidError error) {
            Toast.makeText("下载失败").show();
        }

        // Listener添加了这个回调方法专门用于获取进度
        @Override
        public void onProgressChange(long fileSize, long downloadedSize) {
            // 注：downloadedSize 有可能大于 fileSize，具体原因见下面的描述
            Toast.makeText("下载进度：" + (downloadedSize * 1.0f / fileSize * 100) + "%").show();
        }
});

// 查看该任务的状态
controller.getState();
// 任务的状态分别是：
STATUS_WAITING：         // 等待中
STATUS_DOWNLOADING：     // 下载中
STATUS_PAUSE：           // 已暂停
STATUS_SUCCESS：         // 已成功（标识下载已经正常完成并成功）
STATUS_DISCARD：         // 已取消（放弃）

// 暂停该任务
controller.pause();

// 继续该任务
controller.resume();

// 放弃(删除)该任务
controller.discard();
```

## 任务优先级：

任务的优先级由添加的先后顺序来确定，当某项任务执行结束或暂停时，**FileDownloader** 将从头开始扫描整个队列，重新执行处于等待状态的任务：

```
假设队列中有如下四个任务，并行任务上限为 1：
A waiting
B waiting
C downloading
D waiting
当 C 执行完成后，A 将部署并执行，而 D 要等待 A、B 执行完成后才可以执行。
```

## 实现方式：

Netroid添加了 `FileDownloadRequest` 来实现断点下载功能，核心的实现逻辑都包括在这个请求内。由于文件下载操作将会相对较长时间地占用线程资源，为了避免所有线程均处于繁忙状态而导致无法执行其它高优先级的Http操作，建议不要使用这个类单独发起下载请求，应当与 **FileDownloader** 一起使用。

由于文件下载操作的特殊性，不适宜进行缓存处理，为了避免错误地设置，**FileDownloadRequest** 内部直接禁用了缓存，所以调用 **FileDownloadRequest.setCacheExpireTime()** 来指定缓存过期时间将不生效。

注：在测试中发现大文件下载可能出现连接超时的问题，所以 **FileDownloadRequest** 的重试次数设置了一个比较大的值(200)，以避免下载失败。

注：**FileDownloadRequest** 的优先级为最低，在等待队列中，优先级更高的操作将更快执行。

## 疑难解决：

进度计算可能会出现以下两种异常情况：

> 1、文件总大小为0，但已下载大小大于0，导致进度计算出错或一直为0%。

这种情况是因为服务端使用了Chunked Encoding返回数据，Netroid无法从响应头中获取到Content-Length，所以在进度回调时下载文件的总大小一直为零。有关Transfer-Encoding:chunked的原理，可参考[HttpWatch](http://www.httpwatch.com/httpgallery/chunked/)的详细介绍。

> 2、文件已下载大小大于总大小，导致进度计算超出100%。

这种情况是因为服务端返回了gzip格式的数据，但Netroid在接收到gzip数据时使用了GzipInputStream直接解压缩存放，导致计算出来的已下载大小是解压后的大小，但总大小因为是从Content-Length中取得的压缩大小，所以导致计算误差。

### 问题原因：

无论Netroid使用的 **HurlStack** 或 **HttpClientStack** 均在每次发送请求时添加了接收gzip编码的响应结果：

```java
HttpRequest.addHeader("Accept-Encoding", "gzip");
```

这个Header将通知服务端可返回通过gzip后的响应内容，客户端再进行解压存放，设置可接收gzip编码对于普通的请求操作来讲能够有效地节省流量，但对于文件下载组件来讲直接导致了上述第二个问题的发生，第一个问题也有可能是因为这个设置而导致服务端认为客户端可接收Chunked Encoding而引发的。

如果你将要下载的文件属于gzip作用不大的文件，例如：jpg、apk、rar、dmg等经过压缩的二进制文件格式，你可以禁用接收gzip编码文件的操作，以解决上述两个问题。但如果你去下载一个纯文本文件，gzip压缩可显著地节省流量，是否该允许进度计算出错的问题存在，这其中的利弊需要开发者自己取舍。

### 解决方案：

Netroid允许开发者实现自己的文件下载逻辑，只需要重写 **FileDownloader.buildRequest()** 方法，返回继承自 `FileDownloadRequest` 的实例即可：

```java
FileDownloader mDownloder = new FileDownloader(mQueue, 1) {
    @Override
    public FileDownloadRequest buildRequest(String storeFilePath, String url) {
        return new FileDownloadRequest(storeFilePath, url) {
            @Override
            public void prepare() {
                addHeader("Accept-Encoding", "identity");
                // 父类的prepare()方法做了Range计算，不要忘记调用
                super.prepare();
            }
        };
    }
};
```

示例中返回一个重写了 **prepare()** 方法的 FileDownloadRequest 对象，在prepare()方法中设置Accept-Encoding为identity以代替Netroid默认的gzip设置。这个定制方式允许开发者选择是否启用gzip编码，从而解决进度计算的问题。
