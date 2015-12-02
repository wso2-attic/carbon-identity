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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.oauth.cache;

import org.wso2.carbon.identity.application.authentication.framework.store.SessionDataStore;
import org.wso2.carbon.identity.application.common.cache.BaseCache;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.utils.CarbonUtils;

/**
 * Stores authenticated user attributes and OpenID Connect specific attributes during OIDC Authorization request
 * processing. Those values are later required to serve OIDC Token request and build IDToken.
 */
public class AuthorizationGrantCache extends BaseCache<AuthorizationGrantCacheKey, AuthorizationGrantCacheEntry> {
    private static final String AUTHORIZATION_GRANT_CACHE_NAME = "AuthorizationGrantCache";

    private static volatile AuthorizationGrantCache instance;
    private boolean isTemporarySessionDataPersistEnabled = false;

    private AuthorizationGrantCache() {
        super(AUTHORIZATION_GRANT_CACHE_NAME);
        if (IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.Temporary") != null) {
            isTemporarySessionDataPersistEnabled = Boolean.
                    parseBoolean(IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.Temporary"));
        }
    }

    public static AuthorizationGrantCache getInstance() {
        CarbonUtils.checkSecurity();
        if (instance == null) {
            synchronized (AuthorizationGrantCache.class) {
                if (instance == null) {
                    instance = new AuthorizationGrantCache();
                }
            }
        }
        return instance;
    }

    public void addToCache(AuthorizationGrantCacheKey key, AuthorizationGrantCacheEntry entry) {
        super.addToCache(key, entry);
        if (isTemporarySessionDataPersistEnabled) {
            SessionDataStore.getInstance().storeSessionData(key.getUserAttributesId(),
                    AUTHORIZATION_GRANT_CACHE_NAME, entry);
        }
    }

    public AuthorizationGrantCacheEntry getValueFromCache(AuthorizationGrantCacheKey key) {
        AuthorizationGrantCacheEntry cacheEntry = super.getValueFromCache(key);
        if (cacheEntry == null && isTemporarySessionDataPersistEnabled) {
            cacheEntry = (AuthorizationGrantCacheEntry) SessionDataStore.getInstance().
                    getSessionData(key.getUserAttributesId(), AUTHORIZATION_GRANT_CACHE_NAME);
        }
        return cacheEntry;
    }

    public void clearCacheEntry(AuthorizationGrantCacheKey key) {
        super.clearCacheEntry(key);
        if(isTemporarySessionDataPersistEnabled){
            SessionDataStore.getInstance().clearSessionData(key.getUserAttributesId(), AUTHORIZATION_GRANT_CACHE_NAME);
        }
    }
}
