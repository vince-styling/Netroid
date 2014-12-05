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
import com.duowan.mobile.netroid.RequestQueue;
import com.duowan.mobile.netroid.cache.BitmapImageCache;
import com.duowan.mobile.netroid.image.NetworkImageView;
import com.duowan.mobile.netroid.toolbox.ImageLoader;

import java.util.Collections;
import java.util.List;

public class GridViewActivity extends Activity {
    private RequestQueue mQueue;
    private ImageLoader mImageLoader;
    private BaseAdapter mAdapter;
    private List<Book> bookList;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gridview_acty);

        mQueue = Netroid.newRequestQueue(getApplicationContext(), null);
        int memoryCacheSize = 5 * 1024 * 1024;
        ImageLoader.ImageCache cache = new BitmapImageCache(memoryCacheSize);
        mImageLoader = new SelfImageLoader(mQueue, cache, getResources(), getAssets());
//		mImageLoader = new SelfImageLoader(mQueue, null, getResources(), getAssets());

        bookList = BookDataMock.getData();
        while (bookList.size() > 5) {
            bookList.remove(0);
        }

        mAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return bookList.size() + 1;
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
                NetworkImageView imvCover = (NetworkImageView) convertView.findViewById(R.id.imvCover);
                TextView txvName = (TextView) convertView.findViewById(R.id.txvName);

                if (position == bookList.size()) {
                    imvCover.setDefaultImageResId(0);
                    imvCover.setImageUrl(SelfImageLoader.RES_DRAWABLE + R.drawable.default190x338, mImageLoader);
                    txvName.setText("");
                } else {
                    Book book = getItem(position);

                    imvCover.setDefaultImageResId(android.R.drawable.ic_menu_rotate);
                    imvCover.setImageUrl(book.getImageUrl(), mImageLoader);
                    txvName.setText(book.getName());
                }

                return convertView;
            }
        };

        GridView grvDemonstration = (GridView) findViewById(R.id.grvDemonstration);
        grvDemonstration.setAdapter(mAdapter);

        findViewById(R.id.btnAddView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<Book> tmpBookList = BookDataMock.getData();
                Collections.shuffle(tmpBookList);
                bookList.add(tmpBookList.get(0));
                mAdapter.notifyDataSetChanged();
            }
        });

        findViewById(R.id.btnRemoveView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bookList.size() > 0) {
                    bookList.remove(bookList.size() - 1);
                    mAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    public void finish() {
        mQueue.stop();
        super.finish();
    }

}