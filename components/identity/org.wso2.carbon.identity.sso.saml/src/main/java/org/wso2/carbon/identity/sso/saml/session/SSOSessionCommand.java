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

import java.util.UUID;

import org.apache.axis2.clustering.ClusteringCommand;
import org.apache.axis2.clustering.ClusteringFault;
import org.apache.axis2.clustering.ClusteringMessage;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.wso2.carbon.identity.core.model.SAMLSSOServiceProviderDO;

public class SSOSessionCommand extends ClusteringMessage {
    private static Log log = org.apache.commons.logging.LogFactory.getLog(SSOSessionCommand.class);

    private int tenantId;
    private String tenantDomain;
    private UUID messageId;
    
    private boolean isSignOut;
    private String sessionIndex;
    private String sessionId;
    private String username;
    private String issuer;
    private String rpSessionID;
    private String assertionConsumerURL;

    @Override
    public void execute(ConfigurationContext configurationContext) throws ClusteringFault {
        SSOSessionPersistenceManager ssoPersistManager = SSOSessionPersistenceManager.getPersistenceManager();
        if(isSignOut){
            if (log.isDebugEnabled()) {
                log.info("Starting session replication for sign out request with SessionIndex : " + sessionIndex);
            }
            if(ssoPersistManager.isExistingSession(sessionIndex)){
                ssoPersistManager.removeSession(sessionIndex);
            }
            if(ssoPersistManager.isExistingTokenId(sessionId)){
                ssoPersistManager.removeTokenId(sessionId);
            }
        } else {
            SAMLSSOServiceProviderDO spDO = new SAMLSSOServiceProviderDO();
            spDO.setIssuer(issuer);
            spDO.setAssertionConsumerUrl(assertionConsumerURL);
            if(ssoPersistManager.isExistingSession(sessionIndex)){
                if (log.isDebugEnabled()) {
                    log.debug("Added a new Service Provider entry for : " + issuer +
                              " during the session replication for the SessionIndex : " + sessionIndex);
                }
                SessionInfoData sessionInfoData = ssoPersistManager.getSessionInfo(sessionIndex);
                sessionInfoData.addServiceProvider(issuer, spDO, rpSessionID);
            }
            else{
                if (log.isDebugEnabled()) {
                    log.debug("Added a new Session Info with a Service Provider entry for : " + issuer +
                              " during the session replication for the SessionIndex : " + sessionIndex);
                }
                SessionInfoData sessionInfoData = new SessionInfoData(username);
                sessionInfoData.addServiceProvider(spDO.getIssuer(), spDO, rpSessionID);
                ssoPersistManager.getSessionMap().put(sessionId, sessionIndex);
                ssoPersistManager.getSessionParticipantMap().put(sessionIndex, sessionInfoData);
            }
        }
    }
    
    @Override
    public String toString() {
        return "SSOSessionSyncRequest{" +
        		"type=" + (isSignOut ? "logout" : "login") +
        		" | issuer=" + issuer +
        		" | sessionId=" + sessionId +
        		" | sessionIndex=" + sessionIndex +
        		" | username=" + username + 
        		"}";
    }
    
    public void setSessionIndex(String sessionIndex) {
		this.sessionIndex = sessionIndex;
	}

    public void setUsername(String username) {
        this.username = username;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public void setRpSessionID(String rpSessionID) {
        this.rpSessionID = rpSessionID;
    }

    public void setAssertionConsumerURL(String assertionConsumerURL) {
        this.assertionConsumerURL = assertionConsumerURL;
    }

    public void setSignOut(boolean signOut) {
        isSignOut = signOut;
    }

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

    public ClusteringCommand getResponse() {
	    return null;
    }

	public void setTenantId(int tenantId) {
		this.tenantId = tenantId;
	}

	public void setTenantDomain(String tenantDomain) {
		this.tenantDomain = tenantDomain;
	}

	public void setMessageId(UUID messageId) {
		this.messageId = messageId;
	}
}
