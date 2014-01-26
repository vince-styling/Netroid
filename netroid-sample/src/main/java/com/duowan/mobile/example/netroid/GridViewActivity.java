package com.duowan.mobile.example.netroid;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;
import com.duowan.mobile.example.netroid.mock.Book;
import com.duowan.mobile.example.netroid.mock.BookDataMock;
import com.duowan.mobile.example.netroid.netroid.Netroid;
import com.duowan.mobile.example.netroid.netroid.SelfImageLoader;
import com.duowan.mobile.netroid.NetroidLog;
import com.duowan.mobile.netroid.RequestQueue;
import com.duowan.mobile.netroid.image.NetworkImageView;
import com.duowan.mobile.netroid.toolbox.ImageLoader;

import java.util.List;

public class GridViewActivity extends Activity {
	private RequestQueue mQueue;
	private ImageLoader mImageLoader;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.gridview_acty);

		mQueue = Netroid.newRequestQueue(getApplicationContext());

		mImageLoader = new SelfImageLoader(mQueue, getAssets());

		GridView grvDemonstration = (GridView) findViewById(R.id.grvDemonstration);

		final List<Book> bookList = BookDataMock.getData();
		while (bookList.size() > 10) {
			bookList.remove(0);
		}

		grvDemonstration.setAdapter(new BaseAdapter() {
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
					convertView = getLayoutInflater().inflate(R.layout.grid_item, null);
				}
				NetroidLog.e("getView position : " + position);

				NetworkImageView imvCover = (NetworkImageView) convertView.findViewById(R.id.imvCover);
				TextView txvName = (TextView) convertView.findViewById(R.id.txvName);

				Book book = getItem(position);

				imvCover.setDefaultImageResId(android.R.drawable.ic_menu_rotate);
				imvCover.setImageUrl(book.getImageUrl(), mImageLoader);

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