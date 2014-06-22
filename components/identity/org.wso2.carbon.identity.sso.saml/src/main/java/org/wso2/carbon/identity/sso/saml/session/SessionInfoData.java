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

import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.core.model.SAMLSSOServiceProviderDO;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionInfoData implements Serializable {

    private static final long serialVersionUID = -2997545986276529377L;
    
    private String subject;
    private Map<String, String> rpSessionList = new ConcurrentHashMap<String,  String>();
    private Map<String, SAMLSSOServiceProviderDO> serviceProviderList = new ConcurrentHashMap<String, SAMLSSOServiceProviderDO>();
    /*private String authenticators;
    private Map<ClaimMapping, String> attributes = new HashMap<ClaimMapping, String>();*/
    private String tenantDomain;

	public SessionInfoData(String subject, String tenantDomain){
        this.subject = subject;
        this.tenantDomain = tenantDomain;
    }

    public String getSubject() {
        return subject;
    }

    public Map<String, SAMLSSOServiceProviderDO> getServiceProviderList() {
        return serviceProviderList;
    }

    public void addServiceProvider(String issuer,SAMLSSOServiceProviderDO spDO, String rpSessionId){
        this.serviceProviderList.put(issuer, spDO);
        if (rpSessionId != null) {
            this.rpSessionList.put(issuer, rpSessionId);
        }
    }

    public void removeServiceProvider(String issuer){
        serviceProviderList.remove(issuer);
        rpSessionList.remove(issuer);
    }

    public Map<String, String> getRPSessionsList(){
        return rpSessionList;
    }

	/*public String getAuthenticators() {
		return authenticators;
	}

	public void setAuthenticators(String authenticators) {
		this.authenticators = authenticators;
	}
	
	public Map<ClaimMapping, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<ClaimMapping, String> attributes) {
		this.attributes = attributes;
	}*/

    public String getTenantDomain(){
        return tenantDomain;
    }
}

