package com.duowan.mobile.example.netroid;

import android.app.ListActivity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.duowan.mobile.example.netroid.mock.Book;
import com.duowan.mobile.example.netroid.mock.BookDataMock;
import com.duowan.mobile.example.netroid.netroid.Netroid;
import com.duowan.mobile.example.netroid.netroid.SelfImageLoader;
import com.duowan.mobile.netroid.RequestQueue;
import com.duowan.mobile.netroid.cache.CacheWrapper;
import com.duowan.mobile.netroid.cache.DiskBasedCache;
import com.duowan.mobile.netroid.cache.MemoryBasedCache;
import com.duowan.mobile.netroid.image.NetworkImageView;
import com.duowan.mobile.netroid.request.ImageRequest;
import com.duowan.mobile.netroid.toolbox.ImageLoader;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BatchImageRequestMultCacheActivity extends ListActivity {
	private RequestQueue mQueue;
	private ImageLoader mImageLoader;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		int memoryCacheSize = 5 * 1024 * 1024; // 5MB

		File diskCacheDir = new File(getCacheDir(), "netroid");
		int diskCacheSize = 50 * 1024 * 1024; // 50MB

		mQueue = Netroid.newRequestQueue(getApplicationContext(),
				new CacheWrapper(Const.CACHE_KEY_MEMORY, new MemoryBasedCache(memoryCacheSize)),
				new CacheWrapper(Const.CACHE_KEY_DISK, new DiskBasedCache(diskCacheDir, diskCacheSize)));

		mImageLoader = new SelfImageLoader(mQueue, getResources(), getAssets()) {
			@Override
			public void makeRequest(ImageRequest request) {
				request.setCacheSequence(Const.CACHE_KEY_MEMORY, Const.CACHE_KEY_DISK);
				request.setCacheExpireTime(TimeUnit.MINUTES, 1);
			}
		};

		getListView().setDivider(new ColorDrawable(Color.parseColor("#efefef")));
		getListView().setFastScrollEnabled(true);
		getListView().setDividerHeight(1);

		final List<Book> bookList = BookDataMock.getData();

		setListAdapter(new BaseAdapter() {
			@Override
			public int getCount() {
				return bookList.size();
			}

			@Override
			public Book getItem(int position) {
				return bookList.get(position);
			}

			@Override
			public long getItemId(int position) {
				return position;
			}

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				if (convertView == null) {
					convertView = getLayoutInflater().inflate(R.layout.list_item, null);
				}

				NetworkImageView imvCover = (NetworkImageView) convertView.findViewById(R.id.imvCover);
				TextView txvAuthor = (TextView) convertView.findViewById(R.id.txvAuthor);
				TextView txvName = (TextView) convertView.findViewById(R.id.txvName);

				Book book = getItem(position);

				imvCover.setDefaultImageResId(android.R.drawable.ic_menu_rotate);
				imvCover.setImageUrl(book.getImageUrl(), mImageLoader);

				txvAuthor.setText(book.getAuthor());
				txvName.setText(book.getName());

				return convertView;
			}
		});
	}

	@Override
	public void finish() {
		mQueue.stop();
		super.finish();
	}

}
