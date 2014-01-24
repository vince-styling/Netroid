package com.duowan.mobile.netroid.cache;

import android.util.SparseArray;
import com.duowan.mobile.netroid.Cache;
import com.duowan.mobile.netroid.NetroidLog;

public class CacheChain {
	private SparseArray<Cache> mCaches = new SparseArray<Cache>(2);

	/**
	 * put a cache instance to the chain.
	 * @param key Cache key for find it
	 * @param value Cache for retrieving and storing respones
	 */
	public void putCache(int key, Cache value) {
		mCaches.put(key, value);
	}

	/**
	 * init all Cache, invoke by the CacheDispatcher,
	 * Notice: Make a blocking call and just call once.
	 */
	public void initialize() {
		for (int i = 0; i < mCaches.size(); i++) {
			mCaches.valueAt(i).initialize();
		}
	}

	/**
	 * Retrieves an entry from the cache chain.
	 * @param entryKey The entry key
	 * @param cacheSequence The cache sequence where to find it
	 * @return An {@link Cache.Entry} or null in the event of a cache miss
	 */
	public Cache.Entry getEntry(String entryKey, int[] cacheSequence) {
		for (int i = 0; i < cacheSequence.length; i++) {
			int cacheKey = cacheSequence[i];
			Cache cache = mCaches.get(cacheKey);
			if (cache != null) {
				Cache.Entry entry = cache.getEntry(entryKey);
				if (entry != null) {
					NetroidLog.d(cache.getClass().getSimpleName() + " : " + entryKey);

					// reverse put entry to ahead caches
					for (int j = --i; j >= 0; j--) {
						cacheKey = cacheSequence[j];
						cache = mCaches.get(cacheKey);
						if (cache != null) {
							cache.putEntry(entryKey, entry);
						}
					}

					return entry;
				}
			}
		}
		return null;
	}

	/**
	 * Adds or replaces an entry to the cache chain.
	 * @param entryKey Cache key
	 * @param entry Data to store and metadata for cache coherency
	 * @param cacheSequence The cache sequence which to put it
	 */
	public void putEntry(String entryKey, Cache.Entry entry, int[] cacheSequence) {
		for (int cacheKey : cacheSequence) {
			Cache cache = mCaches.get(cacheKey);
			if (cache != null) {
				NetroidLog.d(cache.getClass().getSimpleName() + " : " + entryKey);
				cache.putEntry(entryKey, entry);
			}
		}
	}

	/**
	 * Remove entry when expired.
	 * @param entryKey Cache key
	 * @param cacheSequence The cache sequence which to put it
	 */
	public void removeEntry(String entryKey, int[] cacheSequence) {
		for (int cacheKey : cacheSequence) {
			Cache cache = mCaches.get(cacheKey);
			if (cache != null) {
				NetroidLog.d(cache.getClass().getSimpleName() + " : " + entryKey);
				cache.removeEntry(entryKey);
			}
		}
	}

}
