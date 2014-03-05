package com.duowan.mobile.netroid.request;

import com.duowan.mobile.netroid.NetroidError;
import com.duowan.mobile.netroid.NetworkResponse;
import com.duowan.mobile.netroid.Request;
import com.duowan.mobile.netroid.Response;

import java.io.File;

public class FileDownloadRequest extends Request<Void> {
	private File mStoreFile;
	private File mTemporaryFile;

	public FileDownloadRequest(String storeFilePath, String url) {
		super(url, null);
		mStoreFile = new File(storeFilePath);
		mTemporaryFile = new File(storeFilePath + ".tmp");
	}

	@Override
	protected Response<Void> parseNetworkResponse(NetworkResponse response) {
		if (!isCanceled()) {
			if (mTemporaryFile.canRead() && mTemporaryFile.length() > 0) {
				if (mTemporaryFile.renameTo(mStoreFile)) {
					return Response.success(null, response);
				} else {
					return Response.error(new NetroidError("Can't rename the download temporary file!"));
				}
			} else {
				return Response.error(new NetroidError("Download temporary file was invalid!"));
			}
		}
		return Response.error(new NetroidError("Request was Canceled!"));
	}

	@Override
	public Priority getPriority() {
		return Priority.LOW;
	}

	@Override
	public void setCacheSequence(int... cacheSequence) {
	}

	public File getTemporaryFile() {
		return mTemporaryFile;
	}

	public File getStoreFile() {
		return mStoreFile;
	}

}