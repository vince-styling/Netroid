package com.duowan.mobile.netroid.cache;

import com.duowan.mobile.netroid.Cache;

public class MemoryBasedCache extends LruCache<String, Cache.Entry> implements Cache {

	public MemoryBasedCache(int maxSize) {
		super(maxSize);
	}

	@Override
	public synchronized Entry getEntry(String key) {
		return super.get(key);
	}

	@Override
	public synchronized void putEntry(String key, Entry entry) {
		super.put(key, entry);
	}

	@Override
	public synchronized void initialize() {
	}

	@Override
	public synchronized void invalidate(String key, long expireTime) {
		Entry entry = getEntry(key);
		if (Entry.invalidate(entry, expireTime)) {
			putEntry(key, entry);
		}
	}

	@Override
	public synchronized void removeEntry(String key) {
		super.remove(key);
	}

	@Override
	public synchronized void clearCache() {
		super.evictAll();
	}

	/**
	 * The cache size will be measured in kilobytes rather than number of items.
	 */
	@Override
	protected int sizeOf(String key, Cache.Entry entry) {
		return entry.getSize();
	}

}
