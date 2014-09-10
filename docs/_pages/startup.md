title: Netroid Startup
decorator: post
slug: startup.html
‡‡‡‡‡‡‡‡‡‡‡‡‡‡

# 开始使用

本文档帮助你快速并正确地集成Netroid到应用中。


### 开发集成

方式一：下载[最新版本](http://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=com.duowan.android.netroid&a=netroid&v=LATEST)的jar，在工程中直接添加依赖。

方式二：Netroid已经发布于 **Maven** 库，你可以通过添加以下的依赖来引用：

```xml
<dependency>
  <groupId>com.duowan.android.netroid</groupId>
  <artifactId>netroid</artifactId>
  <version>(insert latest version)</version>
</dependency>
```

方式三：同样地，使用 **Gradle** 的开发者可以添加以下描述来引用：

```groovy
compile 'com.duowan.android.netroid:netroid:(insert latest version)'
```

#### 注：当前的最新版本是 `1.2.1`。


### 添加权限

配置 `AndroidManifest.xml`，添加Netroid SDK需要的权限到 `<manifest>` 标签下：

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

### 初始化

由于Http在Android中属于基础服务组件，为了达到随处可用的目标，通常情况都采用单例模式进行初始化并集中管理：

```java
// 在 Android Application 这个程序入口处进行Netroid的初始化
public class YourApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Netroid.init(this);
    }
}
```

`Netroid` 是开发者实现的用于管理所有包括 **RequestQueue**、**ImageLoader**、**FileDownloader** 实例的核心类：

```java
public class Netroid {
    // Netroid入口，私有该实例，提供方法对外服务。
    private static RequestQueue mRequestQueue;

    // 图片加载管理器，私有该实例，提供方法对外服务。
    private static ImageLoader mImageLoader;

    // 文件下载管理器，私有该实例，提供方法对外服务。
    private static FileDownloader mFileDownloader;

    private Netroid() {}

    public static void init(Context ctx) {
        if (mRequestQueue != null) throw new IllegalStateException("initialized");

        // 创建Netroid主类，指定硬盘缓存方案
        Network network = new BasicNetwork(new HurlStack(Const.USER_AGENT, null), HTTP.UTF_8);
        mRequestQueue = new RequestQueue(network, 4, new DiskCache(
            new File(ctx.getCacheDir(), Const.HTTP_DISK_CACHE_DIR_NAME), Const.HTTP_DISK_CACHE_SIZE));

        // 创建ImageLoader实例，指定内存缓存方案
        // 注：SelfImageLoader的实现示例请查看图片加载的相关文档
        // 注：ImageLoader及FileDownloader不是必须初始化的组件，如果没有用处，不需要创建实例
        mImageLoader = new SelfImageLoader(
            mRequestQueue, new BitmapImageCache(Const.HTTP_MEMORY_CACHE_SIZE));

        mFileDownloader = new FileDownloader(mRequestQueue, 1);

        mRequestQueue.start();
    }

    // 示例做法：执行自定义请求以获得书籍列表
    public static void getBookList(int pageNo, Listener<List<Book>> listener) {
        mRequestQueue.add(new BookListRequest("http://server.com/book_list/" + pageNo, listener));
    }

    // 加载单张图片
    public static void displayImage(String url, ImageView imageView) {
        ImageLoader.ImageListener listener = ImageLoader.getImageListener(imageView, 0, 0);
        mImageLoader.get(url, listener, 0, 0);
    }

    // 批量加载图片
    public static void displayImage(String url, NetworkImageView imageView) {
        imageView.setImageUrl(url, mImageLoader);
    }

    // 执行文件下载请求
    public static FileDownloader.DownloadController addFileDownload(String storeFilePath, String url, Listener<Void> listener) {
        return mFileDownloader.add(storeFilePath, url, listener);
    }
}

public class Const {
    // http parameters
    public static final int HTTP_MEMORY_CACHE_SIZE = 2 * 1024 * 1024; // 2MB
    public static final int HTTP_DISK_CACHE_SIZE = 50 * 1024 * 1024; // 50MB
    public static final String HTTP_DISK_CACHE_DIR_NAME = "netroid";
    public static final String USER_AGENT = "netroid.cn";
}
```

至此，Netroid的所有初始化工作已经完成，你可以添加各种接口来执行请求，使用Netroid提供的强大服务！！

开发者可查看[使用示例](/usecase.html)来了解典型的代码写法，更多细节请查看[请求处理](/request.html)的全面描述。

