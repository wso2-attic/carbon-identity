/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.sso.saml.session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.model.SAMLSSOServiceProviderDO;
import org.wso2.carbon.identity.sso.saml.cache.SAMLSSOParticipantCache;
import org.wso2.carbon.identity.sso.saml.cache.SAMLSSOParticipantCacheEntry;
import org.wso2.carbon.identity.sso.saml.cache.SAMLSSOParticipantCacheKey;
import org.wso2.carbon.identity.sso.saml.cache.SAMLSSOSessionIndexCache;
import org.wso2.carbon.identity.sso.saml.cache.SAMLSSOSessionIndexCacheEntry;
import org.wso2.carbon.identity.sso.saml.cache.SAMLSSOSessionIndexCacheKey;

/**
 * This class is used to persist the sessions established with Service providers
 */
public class SSOSessionPersistenceManager {

    private static final int CACHE_TIME_OUT = 157680000;
    private static Log log = LogFactory.getLog(SSOSessionPersistenceManager.class);
    private static SSOSessionPersistenceManager sessionPersistenceManager;

    public static SSOSessionPersistenceManager getPersistenceManager() {
        if (sessionPersistenceManager == null) {
            sessionPersistenceManager = new SSOSessionPersistenceManager();
        }
        return sessionPersistenceManager;
    }

    public static void addSessionInfoDataToCache(String key, SessionInfoData sessionInfoData) {

        SAMLSSOParticipantCacheKey cacheKey = new SAMLSSOParticipantCacheKey(key);
        SAMLSSOParticipantCacheEntry cacheEntry = new SAMLSSOParticipantCacheEntry();
        cacheEntry.setSessionInfoData(sessionInfoData);
        SAMLSSOParticipantCache.getInstance().addToCache(cacheKey, cacheEntry);
    }

    public static void addSessionIndexToCache(String key, String sessionIndex) {

        SAMLSSOSessionIndexCacheKey cacheKey = new SAMLSSOSessionIndexCacheKey(key);
        SAMLSSOSessionIndexCacheEntry cacheEntry = new SAMLSSOSessionIndexCacheEntry();
        cacheEntry.setSessionIndex(sessionIndex);
        SAMLSSOSessionIndexCache.getInstance().addToCache(cacheKey, cacheEntry);
    }

    public static SessionInfoData getSessionInfoDataFromCache(String key) {

        SessionInfoData sessionInfoData = null;
        SAMLSSOParticipantCacheKey cacheKey = new SAMLSSOParticipantCacheKey(key);
        SAMLSSOParticipantCacheEntry cacheEntry = SAMLSSOParticipantCache.getInstance().getValueFromCache(cacheKey);

        if (cacheEntry != null) {
            sessionInfoData = cacheEntry.getSessionInfoData();
        }

        return sessionInfoData;
    }

    public static String getSessionIndexFromCache(String key) {

        String sessionIndex = null;
        SAMLSSOSessionIndexCacheKey cacheKey = new SAMLSSOSessionIndexCacheKey(key);
        SAMLSSOSessionIndexCacheEntry cacheEntry = SAMLSSOSessionIndexCache.getInstance().getValueFromCache(cacheKey);

        if (cacheEntry != null) {
            sessionIndex = cacheEntry.getSessionIndex();
        }

        return sessionIndex;
    }

    public static void removeSessionInfoDataFromCache(String key) {

        if (key != null) {
            SAMLSSOParticipantCacheKey cacheKey = new SAMLSSOParticipantCacheKey(key);
            SAMLSSOParticipantCache.getInstance().clearCacheEntry(cacheKey);
        }
    }

    public static void removeSessionIndexFromCache(String key) {

        if (key != null) {
            SAMLSSOSessionIndexCacheKey cacheKey = new SAMLSSOSessionIndexCacheKey(key);
            SAMLSSOSessionIndexCache.getInstance().clearCacheEntry(cacheKey);
        }
    }

    public void persistSession(String sessionIndex, String subject, SAMLSSOServiceProviderDO spDO,
                               String rpSessionId, String issuer, String assertionConsumerURL)
            throws IdentityException {

        SessionInfoData sessionInfoData = getSessionInfoDataFromCache(sessionIndex);

        if (sessionInfoData == null) {
            sessionInfoData = new SessionInfoData();
        }

        //give priority to assertion consuming URL if specified in the request
        if (assertionConsumerURL != null) {
            spDO.setAssertionConsumerUrl(assertionConsumerURL);
        }
        sessionInfoData.setSubject(issuer, subject);
        sessionInfoData.addServiceProvider(spDO.getIssuer(), spDO, rpSessionId);
        addSessionInfoDataToCache(sessionIndex, sessionInfoData);

    }

    /**
     * Get the session infodata for a particular session
     *
     * @param sessionIndex
     * @return
     */
    public SessionInfoData getSessionInfo(String sessionIndex) {
        return getSessionInfoDataFromCache(sessionIndex);
    }

    /**
     * Remove a particular session
     *
     * @param sessionIndex
     */
    public void removeSession(String sessionIndex) {
        removeSessionInfoDataFromCache(sessionIndex);
    }

    /**
     * Check whether this is an existing session
     *
     * @return
     */
    public boolean isExistingSession(String sessionIndex) {
        SessionInfoData sessionInfoData = getSessionInfoDataFromCache(sessionIndex);
        if (sessionInfoData != null) {
            return true;
        }
        return false;
    }

    public void persistSession(String tokenId, String sessionIndex) {
        if (tokenId == null) {
            log.debug("SSO Token Id is null.");
            return;
        }
        if (sessionIndex == null) {
            log.debug("SessionIndex is null.");
            return;
        }
        addSessionIndexToCache(tokenId, sessionIndex);

    }

    public boolean isExistingTokenId(String tokenId) {

        String sessionIndex = getSessionIndexFromCache(tokenId);

        if (sessionIndex != null) {
            return true;
        }
        return false;
    }

    public String getSessionIndexFromTokenId(String tokenId) {
        return getSessionIndexFromCache(tokenId);
    }

    public void removeTokenId(String sessionId) {
        removeSessionIndexFromCache(sessionId);
    }
}
