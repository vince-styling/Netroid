package com.vincestyling.netroid.sample;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;
import com.vincestyling.netroid.cache.BitmapImageCache;
import com.vincestyling.netroid.image.NetworkImageView;
import com.vincestyling.netroid.sample.mock.Book;
import com.vincestyling.netroid.sample.mock.BookDataMock;
import com.vincestyling.netroid.sample.netroid.Netroid;
import com.vincestyling.netroid.sample.netroid.SelfImageLoader;

import java.util.Collections;
import java.util.List;

public class GridViewActivity extends BaseActivity {
    private BaseAdapter mAdapter;
    private List<Book> bookList;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gridview_p0);

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
                    Netroid.displayImage(SelfImageLoader.RES_DRAWABLE + R.drawable.default190x338, imvCover);
                    txvName.setText("");
                } else {
                    Book book = getItem(position);

                    Netroid.displayImage(book.getImageUrl(), imvCover,
                            android.R.drawable.ic_menu_rotate, android.R.drawable.ic_delete);
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

    // initialize netroid, this code should be invoke at Application in product stage.
    @Override
    protected void initNetroid() {
        Netroid.init(null);

        int memoryCacheSize = 5 * 1024 * 1024; // 5MB
        Netroid.setImageLoader(new SelfImageLoader(Netroid.getRequestQueue(),
                new BitmapImageCache(memoryCacheSize), getResources(), getAssets()));
    }
}
