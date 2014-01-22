package com.duowan.mobile.example.netroid.netroid;

import android.content.res.AssetManager;
import com.duowan.mobile.example.netroid.Const;
import com.duowan.mobile.netroid.NetworkResponse;
import com.duowan.mobile.netroid.RequestQueue;
import com.duowan.mobile.netroid.request.ImageRequest;
import com.duowan.mobile.netroid.toolbox.ImageLoader;
import org.apache.http.protocol.HTTP;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

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
		request.setCacheSequence(Const.CACHE_KEY_MEMORY);
		request.setCacheExpireTime(TimeUnit.MINUTES, 1);
	}

	public static byte[] toBytes(InputStream is) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		int nRead;
		byte[] data = new byte[16384];
		while ((nRead = is.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}
		buffer.flush();

		return buffer.toByteArray();
	}

}
