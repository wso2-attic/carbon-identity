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

package org.wso2.carbon.identity.oauth.util;


import org.wso2.carbon.identity.application.authentication.framework.store.SessionDataStore;
import org.wso2.carbon.identity.application.common.cache.BaseCache;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.oauth.cache.CacheEntry;
import org.wso2.carbon.identity.oauth.cache.CacheKey;
import org.wso2.carbon.utils.CarbonUtils;

public class ClaimCache extends BaseCache<String, CacheEntry> {

    private static final String CLAIM_CACHE_NAME = "ClaimCache";

    private static ClaimCache instance;
    private boolean enableRequestScopeCache = false;

    private ClaimCache(String cacheName) {
        super(cacheName);
    }

    private ClaimCache(String cacheName, int timeout) {
        super(cacheName, timeout);
        if (IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.Temporary") != null) {
            enableRequestScopeCache = Boolean.parseBoolean(IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.Temporary"));
        }
    }

    public static ClaimCache getInstance(int timeout) {
        CarbonUtils.checkSecurity();
        if (instance == null) {
            synchronized (ClaimCache.class) {
                if (instance == null) {
                    instance = new ClaimCache(CLAIM_CACHE_NAME,timeout);
                }
            }
        }
        return instance;
    }

    public void addToCache(CacheKey key, CacheEntry entry) {
        super.addToCache(key.toString(), entry);
        if (enableRequestScopeCache) {
            SessionDataStore.getInstance().storeSessionData(key.toString(), CLAIM_CACHE_NAME, entry);
        }
    }

    public CacheEntry getValueFromCache(CacheKey key) {
        return super.getValueFromCache(key.toString());
    }

    public void clearCacheEntry(CacheKey key) {
        super.clearCacheEntry(key.toString());
        if (enableRequestScopeCache) {
            SessionDataStore.getInstance().clearSessionData(key.toString(), CLAIM_CACHE_NAME);
        }
    }
}
