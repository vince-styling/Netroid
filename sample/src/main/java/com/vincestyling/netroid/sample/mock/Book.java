package com.vincestyling.netroid.sample.mock;

import android.text.TextUtils;

public class Book {
    private final String imageUrl;
    private final String name;
    private final String author;

    public Book(String imageUrl, String name, String author) {
        this.imageUrl = imageUrl;
        this.name = name;
        this.author = author;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getName() {
        return name;
    }

    public String getAuthor() {
        return author;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Book && TextUtils.equals(name, ((Book) obj).name);
    }
}
