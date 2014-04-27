/*
*  Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.application.authentication.framework.context;

import org.wso2.carbon.identity.application.authentication.framework.config.dto.ExternalIdPConfig;
import org.wso2.carbon.identity.application.authentication.framework.config.dto.SequenceConfig;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is used for holding data about the
 * authentication request sent from a servlet.
 *
 */
public class AuthenticationContext implements Serializable {

	private static final long serialVersionUID = -7007412857088788541L;
	
	private String contextIdentifier;
	private String sessionIdentifier;
	private String callerPath;
	private String callerSessionKey;
	private String queryParams;
	private String requestType;
	private boolean isLogoutRequest;
	private List<String> authenticatedAuthenticators = new ArrayList<String>();
	private int currentStep;
	private SequenceConfig sequenceConfig;
	private String subject;
	private HttpServletRequest currentRequest;
	private ExternalIdPConfig externalIdP; 
	private Map<String, Object> properties = new HashMap<String, Object>();
	private boolean requestAuthenticated;
	private boolean sequenceComplete;
	private Map<String, String> subjectAttributes = new HashMap<String, String>();
	private boolean rememberMe;
	
	public String getCallerPath() {
		return callerPath;
	}

	public void setCallerPath(String callerPath) {
		this.callerPath = callerPath;
	}

	public String getCallerSessionKey() {
		return callerSessionKey;
	}

	public void setCallerSessionKey(String callerSessionKey) {
		this.callerSessionKey = callerSessionKey;
	}

	public String getQueryParams() {
		return queryParams;
	}

	public void setOrignalRequestQueryParams(String queryParams) {
		this.queryParams = queryParams;
	}

	public String getRequestType() {
		return requestType;
	}

	public void setRequestType(String requestType) {
		this.requestType = requestType;
	}

	public boolean isLogoutRequest() {
		return isLogoutRequest;
	}

	public void setLogoutRequest(boolean isLogoutRequest) {
		this.isLogoutRequest = isLogoutRequest;
	}

    public List<String> getAuthenticatedAuthenticators() {
        return authenticatedAuthenticators;
    }

    public void setAuthenticatedAuthenticators(List<String> authenticatedAuthenticators) {
        this.authenticatedAuthenticators = authenticatedAuthenticators;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(int currentStep) {
        this.currentStep = currentStep;
    }

    public SequenceConfig getSequenceConfig() {
        return sequenceConfig;
    }

    public void setSequenceConfig(SequenceConfig sequenceConfig) {
        this.sequenceConfig = sequenceConfig;
    }

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getContextIdentifier() {
		return contextIdentifier;
	}

	public void setContextIdentifier(String contextIdentifier) {
		this.contextIdentifier = contextIdentifier;
	}

	public HttpServletRequest getCurrentRequest() {
		return currentRequest;
	}

	public void setCurrentRequest(HttpServletRequest currentRequest) {
		this.currentRequest = currentRequest;
	}

	public boolean isRequestAuthenticated() {
		return requestAuthenticated;
	}

	public void setRequestAuthenticated(boolean requestAuthenticated) {
		this.requestAuthenticated = requestAuthenticated;
	}

	public boolean isSequenceComplete() {
		return sequenceComplete;
	}

	public void setSequenceComplete(boolean sequenceComplete) {
		this.sequenceComplete = sequenceComplete;
	}

	public Map<String, String> getSubjectAttributes() {
		return subjectAttributes;
	}

	public void setSubjectAttributes(Map<String, String> subjectAttributes) {
		this.subjectAttributes = subjectAttributes;
	}

	public boolean isRememberMe() {
		return rememberMe;
	}

	public void setRememberMe(boolean rememberMe) {
		this.rememberMe = rememberMe;
	}

	public String getSessionIdentifier() {
		return sessionIdentifier;
	}

	public void setSessionIdentifier(String sessionIdentifier) {
		this.sessionIdentifier = sessionIdentifier;
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
	}
	
	public void setProperty(String key, Object value) {
		properties.put(key, value);
	}
	
	public Object getProperty(String key) {
		return properties.get(key);
	}

	public ExternalIdPConfig getExternalIdP() {
		return externalIdP;
	}

	public void setExternalIdP(ExternalIdPConfig externalIdP) {
		this.externalIdP = externalIdP;
	}

}
