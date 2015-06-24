/*
 * Copyright (c) 2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
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

package org.wso2.carbon.identity.sso.saml.cache;

import org.wso2.carbon.identity.application.authentication.framework.store.SessionDataStore;
import org.wso2.carbon.identity.application.common.cache.BaseCache;
import org.wso2.carbon.identity.core.util.IdentityUtil;

public class SessionDataCache extends BaseCache<String, CacheEntry> {

    private static final String SESSION_DATA_CACHE_NAME = "SAMLSSOSessionDataCache";
    private static volatile SessionDataCache instance;
    private boolean useCache = true;

    private SessionDataCache(String cacheName) {
        super(cacheName);
    }

    private SessionDataCache(String cacheName, int timeout) {
        super(cacheName, timeout);
        useCache = !Boolean.parseBoolean(IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.Only"));
    }

    public static SessionDataCache getInstance(int timeout) {
        if (instance == null) {
            synchronized (SessionDataCache.class) {
                if (instance == null) {
                    instance = new SessionDataCache(SESSION_DATA_CACHE_NAME, timeout);
                }
            }
        }
        return instance;
    }

    public void addToCache(CacheKey key, CacheEntry entry) {
        if (useCache) {
            super.addToCache(((SessionDataCacheKey) key).getSessionDataKey(), entry);
        }
        String keyValue = ((SessionDataCacheKey) key).getSessionDataKey();
        SessionDataStore.getInstance().storeSessionData(keyValue, SESSION_DATA_CACHE_NAME, entry);
    }

    public CacheEntry getValueFromCache(CacheKey key) {
        CacheEntry cacheEntry = null;
        if (useCache) {
            cacheEntry = super.getValueFromCache(((SessionDataCacheKey) key).getSessionDataKey());
        }
        if (cacheEntry == null) {
            String keyValue = ((SessionDataCacheKey) key).getSessionDataKey();
            cacheEntry = (SessionDataCacheEntry) SessionDataStore.getInstance().
                    getSessionData(keyValue, SESSION_DATA_CACHE_NAME);
        }
        return cacheEntry;
    }

    public void clearCacheEntry(CacheKey key) {
        if (useCache) {
            super.clearCacheEntry(((SessionDataCacheKey) key).getSessionDataKey());
        }
        String keyValue = ((SessionDataCacheKey) key).getSessionDataKey();
        SessionDataStore.getInstance().clearSessionData(keyValue, SESSION_DATA_CACHE_NAME);
    }

}
