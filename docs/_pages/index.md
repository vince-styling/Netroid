title: Netroid Introduction
decorator: post
slug: index.html
‡‡‡‡‡‡‡‡‡‡‡‡‡‡

# 简介：

Netroid是一个基于[Volley](http://developer.android.com/training/volley/index.html)实现的Android Http库。提供执行网络请求、缓存返回结果、批量图片加载、大文件断点下载的常见Http交互功能。致力于避免每个项目重复开发基础Http功能，实现显著地缩短开发周期的愿景。

# 实现原理：

Netroid自启动后创建由开发者指定的线程数目，每个线程由 **BlockingQueue** 进行阻塞。当有新的请求进入队列时，其中一个线程将被唤醒并获得请求对象，然后开始执行，执行完成后线程重新回到阻塞状态，等待下一次唤醒。Netroid实现了强大的状态回调接口在请求执行过程中进行通知，包括开始、完成、成功、重试、失败、取消、执行网络操作、应用缓存、下载进度九种状态回调，开发者可方便地获取请求的执行情况，对用户进行友好提醒。

#### 注：Netroid的线程池不具备伸缩功能，创建后所有线程均处于启用状态，不支持动态调整。

# 文档组成：

* [开始使用](/startup.html)

快速实现将Netroid集成到应用中。

* [使用示例](/usecase.html)

了解Netroid在应用中的典型代码写法。

* [组件详解](/understanding.html)

Netroid中的各种内部组件及其使用方法的详细解释。

* [请求处理](/request.html)

了解请求执行类的细节，定制请求处理方式，各种请求场景的实现方法。

* [图片加载](/imageloader.html)

了解图片加载器的功能点及其使用方法。

* [大文件下载](/filedownloader.html)

了解Netroid提供的文件断点下载组件的功能。

* [Javadoc文档](/javadoc/index.html)

了解Netroid所有接口的细节。

也可直接点击下载[演示](/attach/netroid-sample-1.2.1.apk)程序查看运行效果。


# 修改日志

| 版本 | 说明 |
| :-------------: | :------------- |
| `1.2.1`<br>2014-05-05 | 1、下载组件允许定制FileDownloadRequest，以解决进度计算由于gzip引发的误差<br>2、执行下载任务时允许Content-Length不可用的问题<br>3、HttpClientStack默认开启接收gzip编码的响应结果设置 |
| `1.2.0`<br>2014-04-13 | 1、重构缓存模块，普通Http请求不再允许使用内存缓存，不兼容之前的API<br>2、ImageLoader回退到Volley的实现方式，提供专属的内存缓存对象，纠正内存图片加载的不必要延时错误 |
| `1.1.1`<br>2014-04-09 | 1、添加Request.prepare()方法实现在连接重试时更新Header<br>2、解决大文件下载由于连接重试导致的断点位置不正确的问题 |
| `1.1.0`<br>2014-03-07 | 1、添加大文件下载管理器<br>2、修正HttpClient无法执行请求的问题 |
| `1.0.3`<br>2013-01-14 | 1、ImageLoader支持执行非http请求<br>2、请求Listener实现多种情况的状态回调 |

