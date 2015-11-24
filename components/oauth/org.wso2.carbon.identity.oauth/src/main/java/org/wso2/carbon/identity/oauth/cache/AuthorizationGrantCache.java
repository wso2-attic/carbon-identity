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
    private boolean enableRequestScopeCache = false;
    private static final Log log = LogFactory.getLog(AuthorizationGrantCache.class);

    private AuthorizationGrantCache(String cacheName, int timeout) {
        super(cacheName, timeout);
        if (IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.Temporary") != null) {
            enableRequestScopeCache = Boolean.
                    parseBoolean(IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.Temporary"));
        }
    }

    public static AuthorizationGrantCache getInstance(int timeout) {
        CarbonUtils.checkSecurity();
        if (instance == null) {
            synchronized (AuthorizationGrantCache.class) {
                if (instance == null) {
                    instance = new AuthorizationGrantCache(AUTHORIZATION_GRANT_CACHE_NAME, timeout);
                }
            }
        }
        return instance;
    }

    public void addToCache(AuthorizationGrantCacheKey key, AuthorizationGrantCacheEntry entry) {
        String keyValue = key.getUserAttributesId();
        super.addToCache(key, entry);

        //if key is authorization code, for the first time get the code id from entry else try to get from database layer
        if (key.getIsAuthzCode()) {
            if (key.getCodeId() != null) {
                keyValue = key.getCodeId();
            } else {
                keyValue = replaceFromCodeId(keyValue);
            }
        } else {
            keyValue = key.getTokenId();
        }
        SessionDataStore.getInstance().storeSessionData(keyValue, AUTHORIZATION_GRANT_CACHE_NAME, entry);
        if (enableRequestScopeCache) {
            SessionDataStore.getInstance().storeSessionData(keyValue, AUTHORIZATION_GRANT_CACHE_NAME, entry);
        }
    }

    public AuthorizationGrantCacheEntry getValueFromCache(AuthorizationGrantCacheKey key) {
        String keyValue = key.getUserAttributesId();
        AuthorizationGrantCacheEntry cacheEntry = super.getValueFromCache(key);
        if (cacheEntry == null) {
            //if key is authorization code, convert it to code id which is used in database layer
            if(key.getIsAuthzCode()) {
                keyValue = replaceFromCodeId(keyValue);
            }
            cacheEntry = (AuthorizationGrantCacheEntry) SessionDataStore.getInstance().getSessionData(keyValue,
                    AUTHORIZATION_GRANT_CACHE_NAME);
        }
        return cacheEntry;
    }

    public void clearCacheEntry(AuthorizationGrantCacheKey key) {
        String keyValue = key.getUserAttributesId();
        super.clearCacheEntry(key);

        //if key is authorization code, convert it to code id which is used in database layer
        if(key.getIsAuthzCode()) {
            keyValue = replaceFromCodeId(keyValue);
        }

        SessionDataStore.getInstance().clearSessionData(keyValue, AUTHORIZATION_GRANT_CACHE_NAME);
        if(enableRequestScopeCache){
            SessionDataStore.getInstance().clearSessionData(keyValue,AUTHORIZATION_GRANT_CACHE_NAME);
        }
    }

    private String replaceFromCodeId(String authzCode){
        TokenMgtDAO tokenMgtDAO = new TokenMgtDAO();
        try {
            return tokenMgtDAO.getCodeIdByAuthorizationCode(authzCode);
        } catch (IdentityOAuth2Exception e) {
            log.error("Failed to retrieve authorization code id by authorization code from store for - ."+authzCode, e);
        }
        return authzCode;
    }
}