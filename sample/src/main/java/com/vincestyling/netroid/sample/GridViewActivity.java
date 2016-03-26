package com.vincestyling.netroid.sample;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.vincestyling.netroid.cache.BitmapImageCache;
import com.vincestyling.netroid.sample.mock.Book;
import com.vincestyling.netroid.sample.mock.BookDataMock;
import com.vincestyling.netroid.sample.netroid.Netroid;
import com.vincestyling.netroid.sample.netroid.SelfImageLoader;
import com.vincestyling.netroid.widget.NetworkImageView;

import java.util.Collections;
import java.util.List;

/**
 * This sample aim to show you how Netroid resolved GridView.getView(position == 0)
 * invoke too many times then make the grid disorder during dataset changed. It's
 * totally benefit by NetworkImageView widget which provided by Netroid.
 */
public class GridViewActivity extends BaseActivity {
    private BaseAdapter mAdapter;
    private List<Book> bookList;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gridview_p0);

        bookList = BookDataMock.getData();
        Collections.shuffle(bookList);
        while (bookList.size() > 5) {
            bookList.remove(0);
        }

        final View btnAddView = findViewById(R.id.btnAddView);
        final View btnRemoveView = findViewById(R.id.btnRemoveView);

        btnAddView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<Book> tmpBookList = BookDataMock.getData();
                Collections.shuffle(tmpBookList);

                root: for (Book book : tmpBookList) {
                    for (Book eBook : bookList) {
                        if (eBook.equals(book)) continue root;
                    }

                    bookList.add(book);
                    mAdapter.notifyDataSetChanged();
                    btnRemoveView.setVisibility(View.VISIBLE);

                    return;
                }

                btnAddView.setVisibility(View.GONE);
            }
        });

        btnRemoveView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bookList.remove(bookList.size() - 1);
                mAdapter.notifyDataSetChanged();

                if (bookList.isEmpty()) {
                    btnRemoveView.setVisibility(View.GONE);
                }
            }
        });

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
                Holder holder;
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.grid_item, null);
                    holder = new Holder();
                    holder.imvCover = (NetworkImageView) convertView.findViewById(R.id.imvCover);
                    holder.txvName = (TextView) convertView.findViewById(R.id.txvName);
                    convertView.setTag(holder);
                } else {
                    holder = (Holder) convertView.getTag();
                }

                if (position == bookList.size()) {
                    Netroid.displayImage(SelfImageLoader.RES_DRAWABLE + R.drawable.click_to_add_more, holder.imvCover);
                    holder.txvName.setText("");
                } else {
                    Book book = getItem(position);

                    Netroid.displayImage(book.getImageUrl(), holder.imvCover,
                            android.R.drawable.ic_menu_rotate, android.R.drawable.ic_delete);
                    holder.txvName.setText(book.getName());
                }

                return convertView;
            }

            class Holder {
                NetworkImageView imvCover;
                TextView txvName;
            }
        };

        GridView grvDemonstration = (GridView) findViewById(R.id.grvDemonstration);
        grvDemonstration.setAdapter(mAdapter);

        grvDemonstration.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == bookList.size()) {
                    btnAddView.performClick();
                } else {
                    showToast("clicked [" + bookList.get(position).getName() + "]");
                }
            }
        });
    }

    private Toast mToast;

    private void showToast(CharSequence msg) {
        if (mToast != null) mToast.cancel();
        mToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        mToast.show();
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
