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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.identity.application.authentication.framework.internal.FrameworkServiceDataHolder;
import org.wso2.carbon.identity.application.authentication.framework.store.SessionDataStore;
import org.wso2.carbon.identity.application.common.cache.BaseCache;
import org.wso2.carbon.identity.application.common.cache.CacheEntry;
import org.wso2.carbon.identity.application.common.cache.CacheKey;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.user.api.UserStoreException;

public class AuthenticationResultCache extends
        BaseCache<AuthenticationResultCacheKey, AuthenticationResultCacheEntry> {

    private static Log log = LogFactory.getLog(AuthenticationResultCache.class);

    private static final String CACHE_NAME = "AuthenticationResultCache";

    private static volatile AuthenticationResultCache instance;

    private boolean useCache = true;

    private boolean enableTemporaryCaches = true;

    public AuthenticationResultCache(String cacheName) {
        super(cacheName);
        useCache = !Boolean.parseBoolean(IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.Only"));
        if (IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.Temporary") != null) {
            enableTemporaryCaches = Boolean.parseBoolean(IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.Temporary"));
        }
    }

    public static AuthenticationResultCache getInstance() {

        if (instance == null) {
            synchronized (AuthenticationResultCache.class) {
                if (instance == null) {
                    instance = new AuthenticationResultCache(CACHE_NAME);
                }
            }
        }
        return instance;
    }

    public void addToCache(AuthenticationResultCacheKey key, AuthenticationResultCacheEntry entry) {
        if (useCache) {
            super.addToCache(key, entry);
        }
        if (enableTemporaryCaches) {
            int tenantId = MultitenantConstants.INVALID_TENANT_ID;
            if (entry.getResult() != null && entry.getResult().getSubject() != null) {
                String tenantDomain = entry.getResult().getSubject().getTenantDomain();
                if (tenantDomain != null) {
                    tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
                }
            }
            SessionDataStore.getInstance().storeSessionData(key.getResultId(), CACHE_NAME, entry, tenantId);
        }
    }

    public AuthenticationResultCacheEntry getValueFromCache(AuthenticationResultCacheKey key) {
        AuthenticationResultCacheEntry entry = null;
        if (useCache) {
            entry = super.getValueFromCache(key);
        }
        if (entry == null && enableTemporaryCaches) {
            entry = (AuthenticationResultCacheEntry) SessionDataStore.getInstance().
                    getSessionData(key.getResultId(), CACHE_NAME);
        }
        return entry;
    }

    public void clearCacheEntry(AuthenticationResultCacheKey key) {
        if (useCache) {
            super.clearCacheEntry(key);
        }
        if (enableTemporaryCaches) {
            SessionDataStore.getInstance().clearSessionData(key.getResultId(), CACHE_NAME);
        }
    }
}
