title: Netroid ImageLoader
decorator: post
slug: imageloader.html
‡‡‡‡‡‡‡‡‡‡‡‡‡‡

# 图片加载

使用Netroid提供的`ImageLoader`可以非常方便地实现图片加载功能，ImageLoader是一个抽象类，需要由开发者实现其中的创建请求方法：

```java
public abstract class ImageLoader {
    public abstract ImageRequest buildRequest(String requestUrl, int maxWidth, int maxHeight);
}
```

这样做的原因是在创建请求时需要指定硬盘缓存的过期时间，这些必须由开发者根据需要自行设置：

```java
public class SelfImageLoader extends ImageLoader {
    @Override
    public ImageRequest buildRequest(String requestUrl, int maxWidth, int maxHeight) {
        ImageRequest request = new ImageRequest(requestUrl, maxWidth, maxHeight);
        request.setCacheExpireTime(TimeUnit.MINUTES, 20);
        return request;
    }
}
```

## 单张图片

单张图片的加载可以通过发起`ImageRequest`请求来实现，但为了应用内存缓存，推荐使用 **ImageLoader** 来完成：

```java
String url = "http://server.domain/sample.jpg";
ImageLoader.ImageListener listener = ImageLoader.getImageListener(
    ImageView, R.drawable.defaultImageResId, R.drawable.errorImageResId);

// 允许设置最终显示位置的尺寸，ImageLoader将根据比例缩放图片，不设置或设置为0代表使用图片原始尺寸
imageLoader.get(url, listener, ImageView.getWidth(), ImageView.getHeight());
```

## 批量图片

Netroid提供了`NetworkImageView`专门用于批量图片加载的场景：

```java
public class NetworkImageView extends ImageView {
    private String mUrl;

    // 默认显示的图片
    private int mDefaultImageId;

    // 加载失败时显示的图片
    private int mErrorImageId;

    // 主方法入口
    public void setImageUrl(String url, ImageLoader imageLoader) {
        mUrl = url;
        mImageLoader = imageLoader;
        // 这个方法将会对ImageView的尺寸是否有效、是否为同一张图片进行判断
        // 在执行新请求前，也会取消上一次在这个View里启动的另一个已经失效的请求
        // 由于篇幅的限制以及代码行数太多，这里不贴出具体实现的代码
        loadImageIfNecessary(false);
    }

    // 如果图片已经滑离屏幕，变为不可见，将执行取消请求的操作
    @Override
    protected void onDetachedFromWindow() {
        if (mImageContainer != null) mImageContainer.cancelRequest();
        super.onDetachedFromWindow();
    }
}
```

在 **Adapter.getView()** 中只需要简单的调用 **NetworkImageView.setImageUrl()** 即可完成所有事情：

```java
public View getView(int position, View convertView, ViewGroup parent) {
    if (convertView == null) {
        convertView = getLayoutInflater().inflate(R.layout.list_item, null);
    }

    NetworkImageView imvCover = (NetworkImageView) convertView.findViewById(R.id.imvCover);
    imvCover.setImageUrl(Book.getImageUrl(), mImageLoader);

    return convertView;
}
```

ImageLoader内部对于批量加载做了很好的处理，开发者可以设定每个任务的延时执行时间，在列表快速滑动时，可最大限度避免已离开屏幕的失效请求占用线程资源。在请求执行前，会根据url对请求进行重复性判断，避免相同的url执行多次，在请求失效时会立即调用 **Request.cancel()** 方法尝试终止操作。

注：在图片一次加载成功后不再改变的情况下，可以使用普通的ImageView，但在ListView、GridView这种场景时建议使用 **NetworkImageView**，特别是遇到GridView position 0多次getView的bug时，普通的ImageView将会导致图片错位的异常问题，这种情况在演示程序中有专门的解决方案。

## 非Http图片

默认情况下，ImageLoader只能加载网络图片并使用缓存方案，但在实际开发中你除此之外可能也需要读取assets、raw、drawable、sdcard目录中的图片资源，这种非Http的请求在原来的Volley架构中是无法实现的。

为了避免重复开发，Netroid对此做了扩展，只需要重写 **Request.perform()** 方法，手动构造 **NetworkResponse** 对象，就可以模拟Http请求完成整个流程。

```java
ImageRequest request = new ImageRequest("image_file_name_in_assert_folder.jpg", ...) {
    // 核心代码在于重写perform方法，返回NetworkResponse实例
    @Override
    public NetworkResponse perform() {
        try {
            return new NetworkResponse(InputStreamToBytes(getAssets().open(getUrl())), HTTP.UTF_8);
        } catch (IOException e) {
            return new NetworkResponse(new byte[1], HTTP.UTF_8);
        }
    }
};
```

在 **perform()** 方法中，只需要加载assets资源为字节数组返回即可，InputStreamToBytes()方法的实现在此不详细列出。这个扩展方案解决了无法加载本地资源的问题，得以继续应用Cache、ImageLoader的强大功能。

ImageLoader通过重写 **buildRequest()** 方法来实现同时兼容sdcard、http、assets等不同的图片来源：

```java
public class SelfImageLoader extends ImageLoader {

    public static final String RES_ASSETS = "assets://";
    public static final String RES_SDCARD = "sdcard://";
    public static final String RES_HTTP = "http://";

    private AssetManager mAssetManager;

    public SelfImageLoader(RequestQueue queue, AssetManager assetManager) {
        super(queue);
        mAssetManager = assetManager;
    }

    @Override
    public ImageRequest buildRequest(String requestUrl, int maxWidth, int maxHeight) {
        ImageRequest request;
        if (requestUrl.startsWith(RES_ASSETS)) {
            request = new ImageRequest(requestUrl.substring(RES_ASSETS.length()), maxWidth, maxHeight) {
                @Override
                public NetworkResponse perform() {
                    try {
                        return new NetworkResponse(toBytes(mAssetManager.open(getUrl())), HTTP.UTF_8);
                    } catch (IOException e) {
                        return new NetworkResponse(new byte[1], HTTP.UTF_8);
                    }
                }
            };
        }
        else if (requestUrl.startsWith(RES_SDCARD)) {
            request = new ImageRequest(requestUrl.substring(RES_SDCARD.length()), maxWidth, maxHeight) {
                @Override
                public NetworkResponse perform() {
                    try {
                        return new NetworkResponse(toBytes(new FileInputStream(getUrl())), HTTP.UTF_8);
                    } catch (IOException e) {
                        return new NetworkResponse(new byte[1], HTTP.UTF_8);
                    }
                }
            };
        }
        else if (requestUrl.startsWith(RES_HTTP)) {
            request = new ImageRequest(requestUrl, maxWidth, maxHeight);
        }
        else {
            return null;
        }

        makeRequest(request);
        return request;
    }

    public void makeRequest(ImageRequest request) {
        request.setCacheExpireTime(TimeUnit.MINUTES, 30);
    }

}

// 示例：读取http资源的调用方法
String url = "http://server.domain/sample.jpg";
ImageLoader.get(url, listener);

// 示例：读取assets资源的调用方法
String url = SelfImageLoader.RES_ASSETS + "sample.jpg";
ImageLoader.get(url, listener);

// 示例：读取sdcard资源的调用方法
String url = SelfImageLoader.RES_SDCARD + "/sdcard/sample.jpg";
ImageLoader.get(url, listener);
```

这是一个实现兼容不同图片来源的解决方案，可在此基础上添加读取raw、drawable等不同目录下的图片，由于篇幅的限制不作举例，具体可查看演示程序中的代码。

