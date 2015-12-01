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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.identity.application.authentication.framework.store.SessionDataStore;
import org.wso2.carbon.identity.application.common.cache.BaseCache;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;

public class AuthenticationContextCache extends
        BaseCache<AuthenticationContextCacheKey, AuthenticationContextCacheEntry> {

    private static Log log = LogFactory.getLog(AuthenticationContextCache.class);
    private static final String AUTHENTICATION_CONTEXT_CACHE_NAME = "AuthenticationContextCache";
    private static volatile AuthenticationContextCache instance;
    private boolean isTemporarySessionDataPersistEnabled = false;

    private AuthenticationContextCache() {
        super(AUTHENTICATION_CONTEXT_CACHE_NAME);
        if (IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.Temporary") != null) {
            isTemporarySessionDataPersistEnabled = Boolean.parseBoolean(
                    IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.Temporary"));
        }
    }

    public static AuthenticationContextCache getInstance() {
        if (instance == null) {
            synchronized (AuthenticationContextCache.class) {
                if (instance == null) {
                    instance = new AuthenticationContextCache();
                }
            }
        }
        return instance;
    }

    public void addToCache(AuthenticationContextCacheKey key, AuthenticationContextCacheEntry entry) {
        super.addToCache(key, entry);
        if (isTemporarySessionDataPersistEnabled) {
            int tenantId = MultitenantConstants.INVALID_TENANT_ID;
            String tenantDomain = entry.getContext().getTenantDomain();
            if (tenantDomain != null) {
                tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
            }
            SessionDataStore.getInstance().storeSessionData(key.getContextId(), AUTHENTICATION_CONTEXT_CACHE_NAME,
                    entry, tenantId);
        }
    }

    public AuthenticationContextCacheEntry getValueFromCache(AuthenticationContextCacheKey key) {
        AuthenticationContextCacheEntry entry = super.getValueFromCache(key);
        if (entry == null && isTemporarySessionDataPersistEnabled) {
            entry = (AuthenticationContextCacheEntry) SessionDataStore.getInstance().
                    getSessionData(key.getContextId(), AUTHENTICATION_CONTEXT_CACHE_NAME);
        }
        return entry;
    }

    public void clearCacheEntry(AuthenticationContextCacheKey key) {
        super.clearCacheEntry(key);
        if (isTemporarySessionDataPersistEnabled) {
            SessionDataStore.getInstance().clearSessionData(key.getContextId(), AUTHENTICATION_CONTEXT_CACHE_NAME);
        }
    }
}
