/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.application.common.cache;

import org.wso2.carbon.caching.impl.CacheImpl;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.common.listener.AbstractCacheListener;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.cache.Cache;
import javax.cache.CacheBuilder;
import javax.cache.CacheConfiguration;
import javax.cache.CacheManager;
import javax.cache.Caching;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A base class for all cache implementations in Identity Application Management modules.
 */
public class BaseCache<K extends Serializable, V extends Serializable> {

    private static final String CACHE_MANAGER_NAME = "IdentityApplicationManagementCacheManager";
    private CacheBuilder<K, V> cacheBuilder;
    private String cacheName;
    private int cacheTimeout;
    private int capacity = 0;
    private List<AbstractCacheListener> cacheListeners = new ArrayList<AbstractCacheListener>();

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

    private Cache<K, V> getBaseCache() {

        Cache<K, V> cache = null;
        try {

            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext
                    .getThreadLocalCarbonContext();
            carbonContext.setTenantId(MultitenantConstants.SUPER_TENANT_ID);
            carbonContext.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);

            CacheManager cacheManager = Caching.getCacheManagerFactory()
                    .getCacheManager(CACHE_MANAGER_NAME);

            if (cacheTimeout > 0 && cacheBuilder == null) {
                synchronized (cacheName.intern()) {
                    if (cacheBuilder == null) {
                        cacheManager.removeCache(cacheName);
                        cacheBuilder = cacheManager.<K, V>createCacheBuilder(cacheName).
                                setExpiry(CacheConfiguration.ExpiryType.ACCESSED,
                                        new CacheConfiguration
                                                .Duration(TimeUnit.SECONDS, cacheTimeout)).
                                setExpiry(CacheConfiguration.ExpiryType.MODIFIED,
                                        new CacheConfiguration
                                                .Duration(TimeUnit.SECONDS, cacheTimeout)).
                                setStoreByValue(false);
                        cache = cacheBuilder.build();

                        for (AbstractCacheListener cacheListener : cacheListeners) {
                            this.cacheBuilder.registerCacheEntryListener(cacheListener);
                        }

                        if (capacity != 0) {
                            ((CacheImpl) cache).setCapacity(capacity);
                        }
                    } else {
                        cache = cacheManager.getCache(cacheName);
                    }
                }

            } else {
                cache = cacheManager.getCache(cacheName);
                if (capacity != 0) {
                    ((CacheImpl) cache).setCapacity(capacity);
                }

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
            Cache<K, V> cache = getBaseCache();
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

        if(key == null) {
            return null;
        }

        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext
                    .getThreadLocalCarbonContext();
            carbonContext.setTenantId(MultitenantConstants.SUPER_TENANT_ID);
            carbonContext.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
            Cache<K, V> cache = getBaseCache();
            if (cache != null && cache.get(key) != null) {
                return (V) cache.get(key);
            }
            return null;
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

    public void addListener(AbstractCacheListener listener){
        cacheListeners.add(listener);
    }
}
