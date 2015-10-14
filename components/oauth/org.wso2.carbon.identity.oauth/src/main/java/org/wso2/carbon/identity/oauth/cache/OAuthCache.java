/*
 * Copyright (c) 2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.oauth.cache;

import org.wso2.carbon.identity.application.authentication.framework.store.SessionDataStore;
import org.wso2.carbon.identity.application.common.cache.BaseCache;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.oauth.listener.OAuthCacheRemoveListener;
import org.wso2.carbon.utils.CarbonUtils;

public class OAuthCache extends BaseCache<String, CacheEntry> {

    private static final String OAUTH_CACHE_NAME = "OAuthCache";

    private static volatile OAuthCache instance;
    private boolean enableRequestScopeCache = false;

    private OAuthCache(String cacheName, int timeout) {
        super(cacheName,timeout);
        super.addListener(new OAuthCacheRemoveListener());
        if (IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.Temporary") != null) {
            enableRequestScopeCache = Boolean.
                    parseBoolean(IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.Temporary"));
        }
    }

    public static OAuthCache getInstance(int timeout) {
        CarbonUtils.checkSecurity();
        if (instance == null) {
            synchronized (SessionDataCache.class) {
                if (instance == null) {
                    instance = new OAuthCache(OAUTH_CACHE_NAME, timeout);
                }
            }
        }
        return instance;
    }

    public void addToCache(CacheKey key, CacheEntry entry) {
        String keyValue = ((OAuthCacheKey)key).getCacheKeyString();
        super.addToCache(keyValue, entry);
        SessionDataStore.getInstance().storeSessionData(keyValue, OAUTH_CACHE_NAME, entry);
        if (enableRequestScopeCache) {
            SessionDataStore.getInstance().storeSessionData(keyValue, OAUTH_CACHE_NAME, entry);
        }
    }

    public CacheEntry getValueFromCache(CacheKey key) {
        String keyValue = ((OAuthCacheKey)key).getCacheKeyString();
        CacheEntry cacheEntry = super.getValueFromCache(keyValue);
        if (cacheEntry == null) {
            cacheEntry = (CacheEntry) SessionDataStore.getInstance().getSessionData(keyValue, OAUTH_CACHE_NAME);
        }
        return cacheEntry;
    }

    public void clearCacheEntry(CacheKey key) {
        String keyValue = ((OAuthCacheKey)key).getCacheKeyString();
        super.clearCacheEntry(keyValue);
        SessionDataStore.getInstance().clearSessionData(keyValue, OAUTH_CACHE_NAME);
        if (enableRequestScopeCache) {
            SessionDataStore.getInstance().clearSessionData(keyValue, OAUTH_CACHE_NAME);
        }
    }
}
