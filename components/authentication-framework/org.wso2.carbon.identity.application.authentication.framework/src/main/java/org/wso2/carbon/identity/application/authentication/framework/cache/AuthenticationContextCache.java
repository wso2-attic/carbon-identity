/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

public class AuthenticationContextCache extends
        BaseCache<AuthenticationContextCacheKey, AuthenticationContextCacheEntry> {

    private static final String AUTHENTICATION_CONTEXT_CACHE_NAME = "AuthenticationContextCache";
    private static volatile AuthenticationContextCache instance;
    private boolean useCache = true;
    private boolean enableRequestScopeCache = false;

    private AuthenticationContextCache(String cacheName) {
        super(cacheName);
        useCache = !Boolean.parseBoolean(IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.Only"));
        if (IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.Temporary") != null) {
            enableRequestScopeCache = Boolean.parseBoolean(IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.Temporary"));
        }
    }

    public static AuthenticationContextCache getInstance() {
        if (instance == null) {
            synchronized (AuthenticationContextCache.class) {
                if (instance == null) {
                    instance = new AuthenticationContextCache(AUTHENTICATION_CONTEXT_CACHE_NAME);
                }
            }
        }
        return instance;
    }

    public void addToCache(AuthenticationContextCacheKey key, AuthenticationContextCacheEntry entry) {
        if (useCache) {
            super.addToCache(key, entry);
        }
        if (enableRequestScopeCache) {
            SessionDataStore.getInstance().storeSessionData(key.getContextId(), AUTHENTICATION_CONTEXT_CACHE_NAME, entry);
        }
    }

    public AuthenticationContextCacheEntry getValueFromCache(AuthenticationContextCacheKey key) {
        AuthenticationContextCacheEntry entry = null;
        if (useCache) {
            entry = super.getValueFromCache(key);
        }
        if (entry == null) {
            entry = (AuthenticationContextCacheEntry) SessionDataStore.getInstance().
                    getSessionData(key.getContextId(), AUTHENTICATION_CONTEXT_CACHE_NAME);
        }
        return entry;
    }

    public void clearCacheEntry(AuthenticationContextCacheKey key) {
        if (useCache) {
            super.clearCacheEntry(key);
        }
        if (enableRequestScopeCache) {
            SessionDataStore.getInstance().clearSessionData(key.getContextId(), AUTHENTICATION_CONTEXT_CACHE_NAME);
        }
    }
}
