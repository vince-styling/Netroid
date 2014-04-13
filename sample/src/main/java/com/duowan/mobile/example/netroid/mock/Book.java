package com.duowan.mobile.example.netroid.mock;

public class Book {
	private String imageUrl;
	private String name;
	private String author;

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
}
