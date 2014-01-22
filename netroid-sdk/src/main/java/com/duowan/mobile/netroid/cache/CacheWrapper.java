package com.duowan.mobile.netroid.cache;

import com.duowan.mobile.netroid.Cache;

/**
 * a cache wrapper allows developer specify cacheKey which stored by #{@link CacheChain}
 */
public class CacheWrapper {
	private int cacheKey;
	private Cache cache;

	public CacheWrapper(int cacheKey, Cache cache) {
		this.cacheKey = cacheKey;
		this.cache = cache;
	}

	public int getCacheKey() {
		return cacheKey;
	}

	public Cache getCache() {
		return cache;
	}
}
