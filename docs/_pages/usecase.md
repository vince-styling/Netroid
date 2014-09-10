title: Netroid Usecase
decorator: post
slug: usecase.html
‡‡‡‡‡‡‡‡‡‡‡‡‡‡

# 使用示例

使用`JsonObjectRequest`获取一个json对象：

```java
String url = "http://server.domain/json_object.do";
JsonObjectRequest request = new JsonObjectRequest(url, null, new Listener<JSONObject>() {
    @Override
    public void onSuccess(JSONObject response) {
        Toast.makeText(Activity.this, response.getString("result"), 2000).show();
    }

    @Override
    public void onError(NetroidError error) {
        Toast.makeText(Activity.this, "error occurred : " + error.getMessage(), 2000).show();
    }
});

// 设置请求标识，这个标识可用于终止该请求时传入的Key
request.setTag("json-request");
RequestQueue.add(request);
```

使用`JsonArrayRequest`获取一个json对象列表：

```java
String url = "http://server.domain/json_array.do";
JsonArrayRequest request = new JsonArrayRequest(url, new Listener<JSONArray>() {
    @Override
    public void onSuccess(JSONArray response) {
        Toast.makeText(Activity.this, "JSONArray length : " + response.length(), 2000).show();
    }

    @Override
    public void onError(NetroidError error) {
        Toast.makeText(Activity.this, "error occurred : " + error.getMessage(), 2000).show();
    }
});

// 忽略请求的硬盘缓存，直接执行Http操作
request.setForceUpdate(true);
// Http操作成功后保存进缓存的过期时间
request.setCacheExpireTime(TimeUnit.MINUTES, 10);
RequestQueue.add(request);
```

使用`StringRequest`获取一个字符串结果：

```java
String url = "http://server.domain/string.do";
StringRequest request = new StringRequest(Request.Method.GET, url, new Listener<String>() {
    @Override
    public void onSuccess(String response) {
        Toast.makeText(Activity.this, "response : " + response, 2000).show();
    }

    @Override
    public void onError(NetroidError error) {
        Toast.makeText(Activity.this, "error occurred : " + error.getMessage(), 2000).show();
    }
});

// 设置请求Header
request.addHeader("Accept-Encoding", "gzip, deflate");
RequestQueue.add(request);
```

使用`ImageRequest`获取一张图片：

```java
String url = "http://server.domain/sample.jpg";
ImageRequest request = new ImageRequest(url, new Listener<Bitmap>() {
    @Override
    public void onSuccess(Bitmap response) {
        ImageView.setImageBitmap(response);
    }

    @Override
    public void onError(NetroidError error) {
        Toast.makeText(Activity.this, "error occurred : " + error.getMessage(), 2000).show();
    }
}, ImageView.getWidth(), ImageView.getHeight(), Bitmap.Config.RGB_565);

// 设置返回结果在硬盘缓存中的过期时间为10天
request.setCacheExpireTime(TimeUnit.DAYS, 10);
RequestQueue.add(request);
```

#### 注：不建议直接使用ImageRequest，推荐使用 **ImageLoader** 来加载图片。

使用Netroid，你的所有Http操作代码都将类似于这样的书写方式。

