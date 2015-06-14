/*
 * Copyright (c) 2004-2005, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.provider.openid.cache;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;

/**
 * A base class for all cache implementations in user core module.
 */
public abstract class OpenIDBaseCache<K extends OpenIDCacheKey, V extends OpenIDCacheEntry> {

    private static final String OPENID_CACHE_MANAGER = "OpenIDCacheManager";
    private String openidCacheName;

    protected OpenIDBaseCache(String cacheName) {
        this.openidCacheName = cacheName;
    }

    /**
     * Getting existing cache if the cache available, else returns a newly created cache.
     * This logic handles by javax.cache implementation
     */
    private Cache<K, V> getOpenIDCache() {
        CacheManager cacheManager = Caching.getCacheManagerFactory().getCacheManager(OPENID_CACHE_MANAGER);
        return cacheManager.getCache(openidCacheName);
    }

    /**
     * Add a cache entry.
     *
     * @param key   Key which cache entry is indexed.
     * @param entry Actual object where cache entry is placed.
     */
    public void addToCache(K key, V entry) {
        // Element already in the cache. Remove it first
        clearCacheEntry(key);

        Cache<K, V> cache = getOpenIDCache();
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
        Cache<K, V> cache = getOpenIDCache();
        if (cache != null && cache.containsKey(key)) {
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
        Cache<K, V> cache = getOpenIDCache();
        if (cache != null && cache.containsKey(key)) {
            cache.remove(key);
        }
    }

    /**
     * Remove everything in the cache.
     */
    public void clear() {
        Cache<K, V> cache = getOpenIDCache();
        if (cache != null) {
            cache.removeAll();
        }
    }

}
