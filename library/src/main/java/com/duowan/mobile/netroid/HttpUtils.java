package com.duowan.mobile.netroid;

import android.text.TextUtils;
import com.duowan.mobile.netroid.toolbox.ByteArrayPool;
import com.duowan.mobile.netroid.toolbox.PoolingByteArrayOutputStream;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HTTP;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class HttpUtils {

	/** Reads the contents of HttpEntity into a byte[]. */
	public static byte[] responseToBytes(HttpResponse response) throws IOException, ServerError {
		HttpEntity entity = response.getEntity();
		
		byte[] buffer = null;
		try (PoolingByteArrayOutputStream bytes =
                new PoolingByteArrayOutputStream(ByteArrayPool.get(), (int) entity.getContentLength())) {
			InputStream in = entity.getContent();
			if (isGzipContent(response) && !(in instanceof GZIPInputStream)) {
				in = new GZIPInputStream(in);
			}

			if (in == null) {
				throw new ServerError();
			}

			buffer = ByteArrayPool.get().getBuf(1024);
			int count;
			while ((count = in.read(buffer)) != -1) {
				bytes.write(buffer, 0, count);
			}
			return bytes.toByteArray();
		} finally {
			try {
				// Close the InputStream and release the resources by "consuming the content".
				entity.consumeContent();
			} catch (IOException e) {
				// This can happen if there was an exception above that left the entity in
				// an invalid state.
				NetroidLog.v("Error occured when calling consumingContent");
			}
			ByteArrayPool.get().returnBuf(buffer);
		}
	}

	/** Returns the charset specified in the Content-Type of this header. */
	public static String getCharset(HttpResponse response) {
		Header header = response.getFirstHeader(HTTP.CONTENT_TYPE);
		if (header != null) {
			String contentType = header.getValue();
			if (!TextUtils.isEmpty(contentType)) {
				String[] params = contentType.split(";");
				for (int i = 1; i < params.length; i++) {
					String[] pair = params[i].trim().split("=");
					if (pair.length == 2 && pair[0].equals("charset")) {
					    return pair[1];
					}
				}
			}
		}
		return null;
	}

	public static String getHeader(HttpResponse response, String key) {
		Header header = response.getFirstHeader(key);
		return header == null ? null : header.getValue();
	}

	public static boolean isSupportRange(HttpResponse response) {
		if (TextUtils.equals(getHeader(response, "Accept-Ranges"), "bytes")) {
			return true;
		}
		String value = getHeader(response, "Content-Range");
		return value != null && value.startsWith("bytes");
	}

	public static boolean isGzipContent(HttpResponse response) {
		return TextUtils.equals(getHeader(response, "Content-Encoding"), "gzip");
	}

}
