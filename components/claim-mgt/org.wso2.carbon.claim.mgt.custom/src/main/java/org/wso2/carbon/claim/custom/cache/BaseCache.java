package org.wso2.carbon.claim.custom.cache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import java.io.Serializable;

/**
 * Created by Chanuka on 6/23/15 AD.
 */
public class BaseCache <K extends Serializable, V extends Serializable>{
    private static final String CLAIM_CACHE_MANAGER = "ClaimCacheManager";
    private static Log log = LogFactory.getLog(BaseCache.class);
    private String CACHE_NAME;

    public BaseCache(String cacheName) {
        this.CACHE_NAME = cacheName;
    }

    private Cache<K, V> getBaseCache() {
        CacheManager manager = Caching.getCacheManagerFactory().getCacheManager(BaseCache.CLAIM_CACHE_MANAGER);
        Cache<K, V> cache = manager.getCache(CACHE_NAME);
        return cache;
    }


    /**
     * Add a cache entry.
     *
     * @param key   Key which cache entry is indexed.
     * @param entry Actual object where cache entry is placed.
     */
    public void addToCache(K key, V entry) {
        Cache<K, V> cache = getBaseCache();
        if (cache != null) {
            cache.put(key, entry);
        }
    }

    /**
     * Retrieves a cache entry.
     *
     * @param key CacheKey
     * @return Cached entry.
     */
    public V getValueFromCache(K key) {
        Cache<K, V> cache = getBaseCache();
        if (cache != null) {
            return (V) cache.get(key);
        }
        return null;
    }

    /**
     * Clears a cache entry.
     *
     * @param key Key to clear cache.
     */
    public void clearCacheEntry(K key) {
        Cache<K, V> cache = getBaseCache();
        if (cache != null) {
            cache.remove(key);
        }
    }

    /**
     * Remove everything in the cache.
     */
    public void clear() {
        Cache<K, V> cache = getBaseCache();
        if (cache != null) {
            cache.removeAll();
        }
    }
}
