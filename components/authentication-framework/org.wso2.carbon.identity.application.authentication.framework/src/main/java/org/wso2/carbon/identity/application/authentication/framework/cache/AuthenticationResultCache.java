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

package org.wso2.carbon.identity.application.authentication.framework.cache;

import org.wso2.carbon.identity.application.authentication.framework.store.SessionDataStore;
import org.wso2.carbon.identity.application.common.cache.BaseCache;
import org.wso2.carbon.identity.application.common.cache.CacheEntry;
import org.wso2.carbon.identity.application.common.cache.CacheKey;
import org.wso2.carbon.identity.core.util.IdentityUtil;

public class AuthenticationResultCache extends BaseCache<String, CacheEntry> {

    private static final String CACHE_NAME = "AuthenticationResultCache";

    private static volatile AuthenticationResultCache instance;

    private boolean useCache = true;

    private boolean enableTemporaryCaches = true;

    public AuthenticationResultCache(String cacheName) {
        super(cacheName);
    }

    public AuthenticationResultCache(String cacheName, int timeout) {
        super(cacheName, timeout);
        useCache = !Boolean.parseBoolean(IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.Only"));
        if (IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.Temporary") != null) {
            enableTemporaryCaches = Boolean.parseBoolean(IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.Temporary"));
        }
    }

    public static AuthenticationResultCache getInstance(int timeout) {

        if (instance == null) {
            synchronized (AuthenticationResultCache.class) {
                if (instance == null) {
                    instance = new AuthenticationResultCache(CACHE_NAME, timeout);
                }
            }
        }
        return instance;
    }

    public void addToCache(CacheKey key, CacheEntry entry) {
        if (useCache) {
            super.addToCache(((AuthenticationResultCacheKey) key).getResultId(), entry);
        }
        if (enableTemporaryCaches) {
            String keyValue = ((AuthenticationResultCacheKey) key).getResultId();
            SessionDataStore.getInstance().storeSessionData(keyValue, CACHE_NAME, entry);
        }
    }

    public CacheEntry getValueFromCache(CacheKey key) {
        CacheEntry cacheEntry = null;
        if (useCache) {
            cacheEntry = super.getValueFromCache(((AuthenticationResultCacheKey) key).getResultId());
        }
        if (cacheEntry == null) {
            String keyValue = ((AuthenticationResultCacheKey) key).getResultId();
            cacheEntry = (AuthenticationResultCacheEntry) SessionDataStore.getInstance().
                    getSessionData(keyValue, CACHE_NAME);
        }
        return cacheEntry;
    }

    public void clearCacheEntry(CacheKey key) {
        if (useCache) {
            super.clearCacheEntry(((AuthenticationResultCacheKey) key).getResultId());
        }
        if (enableTemporaryCaches) {
            String keyValue = ((AuthenticationResultCacheKey) key).getResultId();
            SessionDataStore.getInstance().clearSessionData(keyValue, CACHE_NAME);
        }
    }
}
