/*
*Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.wso2.carbon.identity.oauth.cache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import java.io.Serializable;

/**
 * A base class for all cache implementations in oauth module.
 */
public class BaseCache <K extends Serializable, V extends Serializable> {

	private static Log log = LogFactory.getLog(BaseCache.class);
	
    private static final String OAUTH_CACHE_MANAGER = "OAuthCacheManager";
    private String CACHE_NAME;

	public BaseCache(String cacheName) {
		this.CACHE_NAME = cacheName;
	}

	private Cache<K,V> getBaseCache() {
    	CacheManager manager = Caching.getCacheManagerFactory().getCacheManager(BaseCache.OAUTH_CACHE_MANAGER);
        Cache<K,V> cache = manager.getCache(CACHE_NAME);
        return cache;
	}
    

	/**
	 * Add a cache entry.
	 * 
	 * @param key
	 *            Key which cache entry is indexed.
	 * @param entry
	 *            Actual object where cache entry is placed.
	 */
	public void addToCache(K key, V entry) {
		Cache<K,V> cache = getBaseCache();
		if (cache != null) {
			cache.put(key, entry);
		}
	}

	/**
	 * Retrieves a cache entry.
	 * 
	 * @param key
	 *            CacheKey
	 * @return Cached entry.
	 */
	public V getValueFromCache(K key) {
		Cache<K,V> cache = getBaseCache();
		if (cache != null) {
            return (V) cache.get(key);
		}
		return null;
	}

	/**
	 * Clears a cache entry.
	 * 
	 * @param key
	 *            Key to clear cache.
	 */
	public void clearCacheEntry(K key) {
		Cache<K,V> cache = getBaseCache();
		if (cache != null) {
            cache.remove(key);
		}
	}

	/**
	 * Remove everything in the cache.
	 */
	public void clear() {
		Cache<K,V> cache = getBaseCache();
		if (cache != null) {
			cache.removeAll();
		}
	}

}
