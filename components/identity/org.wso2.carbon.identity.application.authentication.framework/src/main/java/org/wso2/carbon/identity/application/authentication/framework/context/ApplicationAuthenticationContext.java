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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.wso2.carbon.identity.application.authentication.framework.config.SequenceConfig;

/**
 * This class is used for holding data about the
 * authentication request sent from a servlet.
 *
 */
public class ApplicationAuthenticationContext {

	private String contextIdentifier;
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
	private String externalIdP;
	
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

	public void setQueryParams(String queryParams) {
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

	public String getExternalIdP() {
		return externalIdP;
	}

	public void setExternalIdP(String externalIdP) {
		this.externalIdP = externalIdP;
	}
}
