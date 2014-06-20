/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.identity.sso.saml.session;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.context.RegistryType;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.model.SAMLSSOServiceProviderDO;
import org.wso2.carbon.identity.core.persistence.IdentityPersistenceManager;
import org.wso2.carbon.identity.sso.saml.SSOServiceProviderConfigManager;
import org.wso2.carbon.identity.sso.saml.cache.SAMLSSOParticipantCache;
import org.wso2.carbon.identity.sso.saml.cache.SAMLSSOParticipantCacheEntry;
import org.wso2.carbon.identity.sso.saml.cache.SAMLSSOParticipantCacheKey;
import org.wso2.carbon.identity.sso.saml.cache.SAMLSSOSessionIndexCache;
import org.wso2.carbon.identity.sso.saml.cache.SAMLSSOSessionIndexCacheEntry;
import org.wso2.carbon.identity.sso.saml.cache.SAMLSSOSessionIndexCacheKey;
import org.wso2.carbon.registry.core.Registry;

/**
 * This class is used to persist the sessions established with Service providers
 */
public class SSOSessionPersistenceManager {

    private static Log log = LogFactory.getLog(SSOSessionPersistenceManager.class);
    private static SSOSessionPersistenceManager sessionPersistenceManager;
    private static final int CACHE_TIME_OUT = 157680000;
    
    public static SSOSessionPersistenceManager getPersistenceManager() {
        if (sessionPersistenceManager == null) {
            sessionPersistenceManager = new SSOSessionPersistenceManager();
        }
        return sessionPersistenceManager;
    }


    /**
     * Persist session in memory
     *
     * @param sessionIndex
     * @param subject
     * @param spDO
     */
    public void persistSession(String sessionId, String sessionIndex, String subject, SAMLSSOServiceProviderDO spDO,
                               String rpSessionId, String authenticators, Map<ClaimMapping, String> userAttributes,
                               String tenantDomain)
            throws IdentityException {
        
        SessionInfoData sessionInfoData = getSessionInfoDataFromCache(sessionIndex);
        
        if (sessionInfoData == null) {
            
            sessionInfoData = new SessionInfoData(subject,tenantDomain);
            sessionInfoData.addServiceProvider(spDO.getIssuer(), spDO, rpSessionId);
            /*sessionInfoData.setAuthenticators(authenticators);
            sessionInfoData.setAttributes(userAttributes);*/
            addSessionInfoDataToCache(sessionIndex, sessionInfoData, CACHE_TIME_OUT);
        }
        else{
            persistSession(sessionId, sessionIndex, spDO.getIssuer(), spDO.getAssertionConsumerUrl(), rpSessionId);
        }
    }

    public boolean persistSession(String sessionId, String sessionIndex, String issuer, String assertionConsumerURL, String rpSessionId)
            throws IdentityException {
        try {
            if (sessionIndex != null) {
                
                SessionInfoData sessionInfoData = getSessionInfoDataFromCache(sessionIndex);
                
                if (sessionInfoData != null) {
                    String subject = sessionInfoData.getSubject();
                    SAMLSSOServiceProviderDO spDO = SSOServiceProviderConfigManager.getInstance().getServiceProvider(issuer);
                    if (spDO == null) {
                        IdentityPersistenceManager identityPersistenceManager = IdentityPersistenceManager
                                .getPersistanceManager();
                        Registry registry = (Registry)PrivilegedCarbonContext.getThreadLocalCarbonContext().getRegistry(RegistryType.SYSTEM_CONFIGURATION);
                        spDO = identityPersistenceManager.getServiceProvider(registry, issuer);
                    }
                    //give priority to assertion consuming URL if specified in the request
                    if (assertionConsumerURL != null) {
                        spDO.setAssertionConsumerUrl(assertionConsumerURL);
                    }
                    sessionInfoData.addServiceProvider(spDO.getIssuer(), spDO, rpSessionId);
                    addSessionInfoDataToCache(sessionIndex, sessionInfoData, CACHE_TIME_OUT);
                    return true;
                } else {
                    log.error("Error persisting the new session, there is no previously established session for this " +
                            "user");
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("Error obtaining the service provider info from registry", e);
            throw new IdentityException("Error obtaining the service provider info from registry", e);
        }
        return false;
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

    public void persistSession(String tokenId, String sessionIndex){
        if(tokenId == null){
            log.debug("SSO Token Id is null.");
            return;
        }
        if(sessionIndex == null){
            log.debug("SessionIndex is null.");
            return;
        }
    	addSessionIndexToCache(tokenId, sessionIndex, CACHE_TIME_OUT);
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
    
    public static void addSessionInfoDataToCache(String key, SessionInfoData sessionInfoData,
            int cacheTimeout) {

        SAMLSSOParticipantCacheKey cacheKey = new SAMLSSOParticipantCacheKey(key);
        SAMLSSOParticipantCacheEntry cacheEntry = new SAMLSSOParticipantCacheEntry();
        cacheEntry.setSessionInfoData(sessionInfoData);
        SAMLSSOParticipantCache.getInstance(cacheTimeout).addToCache(cacheKey, cacheEntry);
    }
    
    public static void addSessionIndexToCache(String key, String sessionIndex,
            int cacheTimeout) {

        SAMLSSOSessionIndexCacheKey cacheKey = new SAMLSSOSessionIndexCacheKey(key);
        SAMLSSOSessionIndexCacheEntry cacheEntry = new SAMLSSOSessionIndexCacheEntry();
        cacheEntry.setSessionIndex(sessionIndex);
        SAMLSSOSessionIndexCache.getInstance(cacheTimeout).addToCache(cacheKey, cacheEntry);
    }
    
    public static SessionInfoData getSessionInfoDataFromCache(String key) {

        SessionInfoData sessionInfoData = null;
        SAMLSSOParticipantCacheKey cacheKey = new SAMLSSOParticipantCacheKey(key);
        Object cacheEntryObj = SAMLSSOParticipantCache.getInstance(0).getValueFromCache(cacheKey);

        if (cacheEntryObj != null) {
            sessionInfoData = ((SAMLSSOParticipantCacheEntry) cacheEntryObj).getSessionInfoData();
        }

        return sessionInfoData;
    }
    
    public static String getSessionIndexFromCache(String key) {

        String sessionIndex = null;
        SAMLSSOSessionIndexCacheKey cacheKey = new SAMLSSOSessionIndexCacheKey(key);
        Object cacheEntryObj = SAMLSSOSessionIndexCache.getInstance(0).getValueFromCache(cacheKey);

        if (cacheEntryObj != null) {
            sessionIndex = ((SAMLSSOSessionIndexCacheEntry) cacheEntryObj).getSessionIndex();
        }

        return sessionIndex;
    }

    public static void removeSessionInfoDataFromCache(String key) {
        
        if (key != null) {
            SAMLSSOParticipantCacheKey cacheKey = new SAMLSSOParticipantCacheKey(key);
            SAMLSSOParticipantCache.getInstance(0).clearCacheEntry(cacheKey);
        }
    }
    
    public static void removeSessionIndexFromCache(String key) {
        
        if (key != null) {
            SAMLSSOSessionIndexCacheKey cacheKey = new SAMLSSOSessionIndexCacheKey(key);
            SAMLSSOSessionIndexCache.getInstance(0).clearCacheEntry(cacheKey);
        }
    }
}