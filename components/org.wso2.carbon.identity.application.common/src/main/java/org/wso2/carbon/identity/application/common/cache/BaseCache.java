/*
 *Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.application.common.cache;

import org.wso2.carbon.caching.impl.CacheImpl;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.cache.*;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * A base class for all cache implementations in Identity Application Management modules.
 */
public class BaseCache<K extends Serializable, V extends Serializable> {

    private static final String APP_AUTH_FRAMEWORK_CACHE_MANAGER
            = "IdentityApplicationManagementCacheManager";
    private CacheBuilder<K, V> cacheBuilder;
    private CacheBuilder<String, V> stringCacheBuilder;
    private String cacheName;
    private int cacheTimeout;
    private int capacity = 0;

    public BaseCache(String cacheName) {
        this.cacheName = cacheName;
        this.cacheTimeout = -1;

    }

    public BaseCache(String cacheName, int timeout) {
        this.cacheName = cacheName;

        if (timeout > 0) {
            this.cacheTimeout = timeout;
        } else {
            this.cacheTimeout = -1;
        }
    }


    public BaseCache(String cacheName, int timeout, int capacity) {
        this.cacheName = cacheName;
        this.capacity = capacity;
        if (timeout > 0) {
            this.cacheTimeout = timeout;
        } else {
            this.cacheTimeout = -1;
        }
    }

    private Cache<String, V> getBaseCacheWithStringKey() {

        Cache<String, V> cache = null;
        try {

            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext
                    .getThreadLocalCarbonContext();
            carbonContext.setTenantId(MultitenantConstants.SUPER_TENANT_ID);
            carbonContext.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);

            CacheManager cacheManager = Caching.getCacheManagerFactory()
                    .getCacheManager(APP_AUTH_FRAMEWORK_CACHE_MANAGER);

            if (cacheTimeout > 0) {
                if (stringCacheBuilder == null) {
                    synchronized (cacheName.intern()) {
                        if (stringCacheBuilder == null) {
                            cacheManager.removeCache(cacheName);
                            stringCacheBuilder = cacheManager.<String, V>createCacheBuilder(cacheName).
                                    setExpiry(CacheConfiguration.ExpiryType.ACCESSED,
                                            new CacheConfiguration
                                                    .Duration(TimeUnit.SECONDS, cacheTimeout)).
                                    setStoreByValue(false);
                            cache = stringCacheBuilder.build();
                            if (capacity != 0) {
                                ((CacheImpl) cache).setCapacity(capacity);
                            }
                        } else {
                            cache = cacheManager.getCache(cacheName);
                        }
                    }
                } else {
                    cache = cacheManager.getCache(cacheName);
                }
            } else {
                cache = cacheManager.getCache(cacheName);

            }
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }

        return cache;
    }

    private Cache<K, V> getBaseCache() {

        Cache<K, V> cache = null;
        try {

            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext
                    .getThreadLocalCarbonContext();
            carbonContext.setTenantId(MultitenantConstants.SUPER_TENANT_ID);
            carbonContext.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);

            CacheManager cacheManager = Caching.getCacheManagerFactory()
                    .getCacheManager(APP_AUTH_FRAMEWORK_CACHE_MANAGER);

            if (cacheTimeout > 0) {
                if (cacheBuilder == null) {
                    synchronized (cacheName.intern()) {
                        if (cacheBuilder == null) {
                            cacheManager.removeCache(cacheName);
                            cacheBuilder = cacheManager.<K, V>createCacheBuilder(cacheName).
                                    setExpiry(CacheConfiguration.ExpiryType.ACCESSED,
                                            new CacheConfiguration
                                                    .Duration(TimeUnit.SECONDS, cacheTimeout)).
                                    setStoreByValue(false);
                            cache = cacheBuilder.build();
                            if (capacity != 0) {
                                ((CacheImpl) cache).setCapacity(capacity);
                            }
                        } else {
                            cache = cacheManager.getCache(cacheName);
                        }
                    }
                } else {
                    cache = cacheManager.getCache(cacheName);
                }
            } else {
                cache = cacheManager.getCache(cacheName);

            }
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }

        return cache;
    }

    /**
     * Add a cache entry.
     *
     * @param key   Key which cache entry is indexed.
     * @param entry Actual object where cache entry is placed.
     */
    public void addToCache(K key, V entry) {
        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext
                    .getThreadLocalCarbonContext();
            carbonContext.setTenantId(MultitenantConstants.SUPER_TENANT_ID);
            carbonContext.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
            // Element already in the cache. Remove it first
            //clearCacheEntry(key);
            Cache<K, V> cache = getBaseCache();
            if (cache != null) {
                cache.put(key, entry);
            }
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    public void addToCache(String key, V entry) {
        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext
                    .getThreadLocalCarbonContext();
            carbonContext.setTenantId(MultitenantConstants.SUPER_TENANT_ID);
            carbonContext.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
            // Element already in the cache. Remove it first
            //clearCacheEntry(key);
            Cache<String, V> cache = getBaseCacheWithStringKey();
            if (cache != null) {
                cache.put(key, entry);
            }
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    /**
     * Retrieves a cache entry.
     *
     * @param key CacheKey
     * @return Cached entry.
     */
    public V getValueFromCache(K key) {

        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext
                    .getThreadLocalCarbonContext();
            carbonContext.setTenantId(MultitenantConstants.SUPER_TENANT_ID);
            carbonContext.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
            Cache<K, V> cache = getBaseCache();
            if (cache != null) {
                return (V) cache.get(key);
            }
            return null;
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    public V getValueFromCache(String key) {

        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext
                    .getThreadLocalCarbonContext();
            carbonContext.setTenantId(MultitenantConstants.SUPER_TENANT_ID);
            carbonContext.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
            Cache<String, V> cache = getBaseCacheWithStringKey();
            if (cache != null) {
                return (V) cache.get(key);
            }
            return null;
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    public void clearCacheEntry(String key) {

        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext
                    .getThreadLocalCarbonContext();
            carbonContext.setTenantId(MultitenantConstants.SUPER_TENANT_ID);
            carbonContext.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
            Cache<String, V> cache = getBaseCacheWithStringKey();
            if (cache != null) {
                cache.remove(key);
            }
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    /**
     * Clears a cache entry.
     *
     * @param key Key to clear cache.
     */
    public void clearCacheEntry(K key) {

        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext
                    .getThreadLocalCarbonContext();
            carbonContext.setTenantId(MultitenantConstants.SUPER_TENANT_ID);
            carbonContext.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
            Cache<K, V> cache = getBaseCache();
            if (cache != null) {
                cache.remove(key);
            }
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    /**
     * Remove everything in the cache.
     */
    public void clear() {

        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext
                    .getThreadLocalCarbonContext();
            carbonContext.setTenantId(MultitenantConstants.SUPER_TENANT_ID);
            carbonContext.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
            Cache<K, V> cache = getBaseCache();
            if (cache != null) {
                cache.removeAll();
            }
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }
}
