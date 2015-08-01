package com.vincestyling.netroid.sample;

import android.app.ListActivity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.vincestyling.netroid.sample.mock.Book;
import com.vincestyling.netroid.sample.mock.BookDataMock;
import com.vincestyling.netroid.sample.netroid.Netroid;
import com.vincestyling.netroid.widget.NetworkImageView;

import java.util.List;

public abstract class BaseListActivity extends ListActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initNetroid();

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

                Netroid.displayImage(book.getImageUrl(), imvCover,
                        android.R.drawable.ic_menu_rotate, android.R.drawable.ic_delete);
                txvAuthor.setText(book.getAuthor());
                txvName.setText(book.getName());

                return convertView;
            }
        });
    }

    // initialize netroid, this code should be invoke at Application in product stage.
    protected abstract void initNetroid();

    @Override
    public void finish() {
        Netroid.destroy();
        super.finish();
    }
}
