package com.vincestyling.netroid.sample;

import android.app.ListActivity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.vincestyling.netroid.RequestQueue;
import com.vincestyling.netroid.cache.BitmapImageCache;
import com.vincestyling.netroid.image.NetworkImageView;
import com.vincestyling.netroid.request.ImageRequest;
import com.vincestyling.netroid.sample.mock.Book;
import com.vincestyling.netroid.sample.mock.BookDataMock;
import com.vincestyling.netroid.sample.netroid.Netroid;
import com.vincestyling.netroid.sample.netroid.SelfImageLoader;
import com.vincestyling.netroid.toolbox.ImageLoader;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class BatchImageRequestMemActivity extends ListActivity {
    private RequestQueue mQueue;
    private ImageLoader mImageLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int memoryCacheSize = 5 * 1024 * 1024; // 5MB
        mQueue = Netroid.newRequestQueue(getApplicationContext(), null);

        mImageLoader = new SelfImageLoader(mQueue, new BitmapImageCache(memoryCacheSize), getResources(), getAssets()) {
            @Override
            public void makeRequest(ImageRequest request) {
                request.setCacheExpireTime(TimeUnit.DAYS, 10);
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
