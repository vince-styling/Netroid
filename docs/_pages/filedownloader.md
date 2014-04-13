---
title: Netroid FileDownloader
layout: index
format: markdown
slug: filedownloader.html
lstModified: 2014-04-13 15:09
---

# 大文件下载

Netroid实现的 `FileDownloader` 对断点续传方式的大文件下载提供了支持，其内部维护一个下载队列，所以在创建时需要指定最大并行任务数，
超出限制的任务将自动进入等待队列。在设置最大并行任务数后，开发者只需要往队列中不断添加任务，其它的事情均由 **FileDownloader** 完成。

FileDownloader将在任务添加成功时返回 `DownloadController` 实例对象，这个对象提供了查看任务执行状态、暂停、继续、取消四项必需的操作功能，
开发者只需要持有这个对象，即可随时掌控任务的所有情况。

FileDownloader的用法类似于 **ImageLoader**，用单例模式创建一个全局的实例，在初始化 **RequestQueue** 时构造：

    int poolSize = RequestQueue.DEFAULT_NETWORK_THREAD_POOL_SIZE; // 默认为4
    RequestQueue mQueue = new RequestQueue(Network, poolSize);
    // 建议并行任务数上限不超过3，在手机带宽有限的条件下，并行任务数的扩大无法加快下载速度。
    // 注：如果并行任务数上限大于或等于RequestQueue中的总线程数，将被视为不合法而抛出异常。
    FileDownloader mDownloader = new FileDownloader(mQueue, 1);

调用 **FileDownloader.add()** 方法即可创建新任务：

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

## 任务优先级：

任务的优先级由添加的先后顺序来确定，当某项任务执行结束或暂停时，**FileDownloader** 将从头开始扫描整个队列，重新执行处于等待状态的任务：

    假设队列中有如下四个任务，并行任务上限为 1：
    A waiting
    B waiting
    C downloading
    D waiting
    当 C 执行完成后，A 将部署并执行，而 D 要等待 A、B 执行完成后才可以执行。

## 实现方式：

Netroid添加了 `FileDownloadRequest` 来实现断点下载功能，核心的实现逻辑都包括在这个请求内。
由于文件下载操作将会相对较长时间地占用线程资源，为了避免所有线程均处于繁忙状态而导致无法执行其它高优先级的Http操作，
建议不要使用这个类单独发起下载请求，应当与 **FileDownloader** 一起使用。

由于文件下载操作的特殊性，不适宜进行缓存处理，为了避免错误地设置，**FileDownloadRequest** 内部直接禁用了缓存，
所以调用 **FileDownloadRequest.setCacheExpireTime()** 来指定缓存过期时间将不生效。

注：在测试中发现大文件下载可能出现连接超时的问题，所以 **FileDownloadRequest** 的重试次数设置了一个比较大的值(200)，以避免下载失败。

注：**FileDownloadRequest** 的优先级为最低，在等待队列中，优先级更高的操作将更快执行。
