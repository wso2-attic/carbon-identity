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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.store.SessionDataStore;
import org.wso2.carbon.identity.application.common.cache.BaseCache;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.dao.TokenMgtDAO;
import org.wso2.carbon.utils.CarbonUtils;

/**
 * Stores authenticated user attributes and OpenID Connect specific attributes during OIDC Authorization request
 * processing. Those values are later required to serve OIDC Token request and build IDToken.
 */
public class AuthorizationGrantCache extends BaseCache<AuthorizationGrantCacheKey, AuthorizationGrantCacheEntry> {
    private static final String AUTHORIZATION_GRANT_CACHE_NAME = "AuthorizationGrantCache";

    private static volatile AuthorizationGrantCache instance;
    private static final Log log = LogFactory.getLog(AuthorizationGrantCache.class);

    private AuthorizationGrantCache() {
        super(AUTHORIZATION_GRANT_CACHE_NAME);
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

    public void addToCacheByToken(AuthorizationGrantCacheKey key, AuthorizationGrantCacheEntry entry) {
        super.addToCache(key, entry);
        String tokenId = entry.getTokenId();
        if (tokenId != null) {
            storeToSessionStore(tokenId, entry);
        } else {
            storeToSessionStore(replaceFromTokenId(key.getUserAttributesId()), entry);
        }

    }


    public AuthorizationGrantCacheEntry getValueFromCacheByToken(AuthorizationGrantCacheKey key) {
        AuthorizationGrantCacheEntry cacheEntry = super.getValueFromCache(key);
        if (cacheEntry == null) {
            cacheEntry = getFromSessionStore(replaceFromTokenId(key.getUserAttributesId()));
        }
        return cacheEntry;
    }

    public void clearCacheEntryByToken(AuthorizationGrantCacheKey key) {
        super.clearCacheEntry(key);
        clearFromSessionStore(replaceFromTokenId(key.getUserAttributesId()));
    }


    public void addToCacheByCode(AuthorizationGrantCacheKey key, AuthorizationGrantCacheEntry entry) {
        super.addToCache(key, entry);
        storeToSessionStore(entry.getCodeId(), entry);
    }


    public AuthorizationGrantCacheEntry getValueFromCacheByCode(AuthorizationGrantCacheKey key) {
        AuthorizationGrantCacheEntry cacheEntry = super.getValueFromCache(key);
        if (cacheEntry == null) {
            getFromSessionStore(replaceFromCodeId(key.getUserAttributesId()));
        }
        return cacheEntry;
    }


    public void clearCacheEntryByCode(AuthorizationGrantCacheKey key) {
        super.clearCacheEntry(key);
        clearFromSessionStore(replaceFromCodeId(key.getUserAttributesId()));
    }


    private String replaceFromCodeId(String authzCode) {
        TokenMgtDAO tokenMgtDAO = new TokenMgtDAO();
        try {
            return tokenMgtDAO.getCodeIdByAuthorizationCode(authzCode);
        } catch (IdentityOAuth2Exception e) {
            log.error("Failed to retrieve authorization code id by authorization code from store for - ." + authzCode, e);
        }
        return authzCode;
    }


    private String replaceFromTokenId(String keyValue) {
        TokenMgtDAO tokenMgtDAO = new TokenMgtDAO();
        try {
            return tokenMgtDAO.getTokenIdByToken(keyValue);
        } catch (IdentityOAuth2Exception e) {
            log.error("Failed to retrieve token id by token from store for - ." + keyValue, e);
        }
        return keyValue;
    }


    private void clearFromSessionStore(String id) {
        SessionDataStore.getInstance().clearSessionData(id, AUTHORIZATION_GRANT_CACHE_NAME);
    }


    private AuthorizationGrantCacheEntry getFromSessionStore(String id) {
        return (AuthorizationGrantCacheEntry) SessionDataStore.getInstance().getSessionData(id, AUTHORIZATION_GRANT_CACHE_NAME);
    }


    private void storeToSessionStore(String id, AuthorizationGrantCacheEntry entry) {
        SessionDataStore.getInstance().storeSessionData(id, AUTHORIZATION_GRANT_CACHE_NAME, entry);
    }

}