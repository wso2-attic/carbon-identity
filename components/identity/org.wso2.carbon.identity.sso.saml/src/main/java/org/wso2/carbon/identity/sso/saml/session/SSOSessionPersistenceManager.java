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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.axis2.AxisFault;
import org.apache.axis2.clustering.ClusteringAgent;
import org.apache.axis2.clustering.ClusteringFault;
import org.apache.axis2.clustering.state.Replicator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.core.util.AnonymousSessionUtil;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.model.SAMLSSOServiceProviderDO;
import org.wso2.carbon.identity.core.persistence.IdentityPersistenceManager;
import org.wso2.carbon.identity.sso.saml.SSOServiceProviderConfigManager;
import org.wso2.carbon.identity.sso.saml.util.SAMLSSOUtil;

/**
 * This class is used to persist the sessions established with Service providers
 */
public class SSOSessionPersistenceManager {

    private static Log log = LogFactory.getLog(SSOSessionPersistenceManager.class);
    private static SSOSessionPersistenceManager sessionPersistenceManager;
    
    /**
     * Holds sessionIds (i.e. values of samlssoTokenId/samlssoRememberMe cookies) 
     * and SessionIndexes created for each of them
     */
    private Map<String, String> sessionMap = new ConcurrentHashMap<String, String>();
    
    /**
     * Holds session related info mapped by SessionIndexes 
     */
    private Map<String, SessionInfoData> sessionParticipantMap = new ConcurrentHashMap<String, SessionInfoData>();

    public static SSOSessionPersistenceManager getPersistenceManager() {
        if (sessionPersistenceManager == null) {
            sessionPersistenceManager = new SSOSessionPersistenceManager();
        }
        return sessionPersistenceManager;
    }

    /**
     * Get the session map
     *
     * @return
     */
    public Map<String, SessionInfoData> getSessionParticipantMap() {
        return sessionParticipantMap;
    }

    /**
     * Persist session in memory
     *
     * @param sessionIndex
     * @param subject
     * @param spDO
     */
    public void persistSession(String sessionId, String sessionIndex, String subject, SAMLSSOServiceProviderDO spDO,
                               String rpSessionId, String authenticators)
            throws IdentityException {
        if (!sessionParticipantMap.containsKey(sessionIndex)) {
            SessionInfoData sessionInfoData = new SessionInfoData(subject);
            sessionInfoData.addServiceProvider(spDO.getIssuer(), spDO, rpSessionId);
            sessionInfoData.setAuthenticators(authenticators);
            sessionParticipantMap.put(sessionIndex, sessionInfoData);
            replicateSessionInfo(sessionId, sessionIndex, subject, spDO, rpSessionId);
        }
        else{
            persistSession(sessionId, sessionIndex, spDO.getIssuer(), spDO.getAssertionConsumerUrl(), rpSessionId);
        }
    }

    public boolean persistSession(String sessionId, String sessionIndex, String issuer, String assertionConsumerURL, String rpSessionId)
            throws IdentityException {
        try {
            if (sessionIndex != null) {
                if (sessionParticipantMap.containsKey(sessionIndex)) {
                    SessionInfoData sessionInfoData = sessionParticipantMap.get(sessionIndex);
                    String subject = sessionInfoData.getSubject();
                    SAMLSSOServiceProviderDO spDO = SSOServiceProviderConfigManager.getInstance().getServiceProvider(issuer);
                    if (spDO == null) {
                        IdentityPersistenceManager identityPersistenceManager = IdentityPersistenceManager
                                .getPersistanceManager();
                        spDO = identityPersistenceManager.getServiceProvider(
                                AnonymousSessionUtil.getSystemRegistryByUserName(SAMLSSOUtil.getRegistryService(),
                                        SAMLSSOUtil.getRealmService(), subject), issuer);
                    }
                    //give priority to assertion consuming URL if specified in the request
                    if (assertionConsumerURL != null) {
                        spDO.setAssertionConsumerUrl(assertionConsumerURL);
                    }
                    sessionInfoData.addServiceProvider(spDO.getIssuer(), spDO, rpSessionId);
                    replicateSessionInfo(sessionId, sessionIndex, subject, spDO, rpSessionId);
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
        if (sessionIndex != null) {
            return sessionParticipantMap.get(sessionIndex);
        }
        return null;
    }

    /**
     * Remove a particular session
     *
     * @param sessionIndex
     */
    public void removeSession(String sessionIndex) {
        if (sessionIndex != null) {
            sessionParticipantMap.remove(sessionIndex);
        }
    }

    /**
     * Check whether this is an existing session
     *
     * @return
     */
    public boolean isExistingSession(String sessionIndex) {
        if (sessionParticipantMap.containsKey(sessionIndex)) {
            return true;
        }
        return false;
    }

    private void replicateSessionInfo(String sessionId, String sessionIndex, String subject,
                                      SAMLSSOServiceProviderDO spDO, String rpSessionId) {
        SSOSessionCommand sessionCommand = new SSOSessionCommand();
        sessionCommand.setUsername(subject);
        sessionCommand.setAssertionConsumerURL(spDO.getAssertionConsumerUrl());
        sessionCommand.setIssuer(spDO.getIssuer());
        sessionCommand.setRpSessionID(rpSessionId);
        sessionCommand.setSessionIndex(sessionIndex);
        sessionCommand.setSessionId(sessionId);
        sessionCommand.setSignOut(false);

        if (log.isDebugEnabled()) {
            log.debug("Starting to replicate Session Info for TokenID : " + sessionIndex);
        }
        
        sendClusterSyncMessage(sessionCommand);
        
        if (log.isDebugEnabled()) {
            log.debug("Completed replicating Session Info for TokenID : " + sessionIndex);
        }
    }

    public Map<String, String> getSessionMap() {
		return sessionMap;
	}
    
    public void persistSession(String tokenId, String sessionIndex){
    	sessionMap.put(tokenId, sessionIndex);
    }
    
    public boolean isExistingTokenId(String tokenId){
    	if (tokenId != null) {
    		return sessionMap.containsKey(tokenId);
    	}
    	return false;
    }
    
    public String getSessionIndexFromTokenId(String tokenId){
    	if (isExistingTokenId(tokenId)){
    		return sessionMap.get(tokenId);
    	}
    	return null;
    }
    
    public void removeTokenId(String sessionId) {
        if (sessionId != null) {
            sessionMap.remove(sessionId);
        }
    }
    
    public void sendClusterSyncMessage(SSOSessionCommand sessionCommand) {
        // For sending clustering messages we need to use the super-tenant's AxisConfig (Main Server
        // AxisConfiguration) because we are using the clustering facility offered by the ST in the
        // tenants
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        UUID messageId = UUID.randomUUID();
        
        sessionCommand.setTenantId(tenantId);
        sessionCommand.setTenantDomain(tenantDomain);
        sessionCommand.setMessageId(messageId);

        ClusteringAgent clusteringAgent =
        		SAMLSSOUtil.getConfigCtxService().getServerConfigContext().getAxisConfiguration().getClusteringAgent();
        
        if (clusteringAgent != null) {
            int numberOfRetries = 0;
            
            while (numberOfRetries < 60) {
                try {
                    clusteringAgent.sendMessage(sessionCommand, true);
                    log.info("Sent [" + sessionCommand + "]");
                    break;
                } catch (ClusteringFault e) {
                    numberOfRetries++;
                    
                    if (numberOfRetries < 60) {
                        log.warn("Could not send SSOSessionSynchronizeRequest for tenant " +
                                tenantId + ". Retry will be attempted in 2s. Request: " + sessionCommand, e);
                    } else {
                        log.error("Could not send SSOSessionSynchronizeRequest for tenant " +
                                tenantId + ". Several retries failed. Request:" + sessionCommand, e);
                    }
                    
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
    }
}
