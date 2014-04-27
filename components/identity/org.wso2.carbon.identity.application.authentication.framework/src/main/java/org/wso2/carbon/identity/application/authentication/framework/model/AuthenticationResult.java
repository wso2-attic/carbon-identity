/*
 *Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */

package org.wso2.carbon.identity.application.authentication.framework.model;

import java.util.Map;

public class AuthenticationResult {
	
	private boolean authenticated;
	private String subject;
	private Map<String, String> userAttributes;
	private String authenticatedAuthenticators;
	private boolean loggedOut;
	private Map<String, String> claimMapping;
	
	public AuthenticationResult(){}
	
	public AuthenticationResult(boolean authenticated, String subject,
                                Map<String, String> userAttributes, String authenticatedAuthenticators) {

        this.authenticated = authenticated;
		this.subject = subject;
		this.userAttributes = userAttributes;
		this.authenticatedAuthenticators = authenticatedAuthenticators;
	}
	
	public boolean isAuthenticated() {
		return authenticated;
	}
	
	public void setAuthenticated(boolean authenticated) {
		this.authenticated = authenticated;
	}
	
	public String getSubject() {
		return subject;
	}
	
	public void setSubject(String subject) {
		this.subject = subject;
	}
	
	public Map<String, String> getUserAttributes() {
		return userAttributes;
	}
	
	public void setUserAttributes(Map<String, String> userAttributes) {
		this.userAttributes = userAttributes;
	}
	
	public String getAuthenticatedAuthenticators() {
		return authenticatedAuthenticators;
	}
	
	public void setAuthenticatedAuthenticators(String authenticatedAuthenticators) {
		this.authenticatedAuthenticators = authenticatedAuthenticators;
	}

	public boolean isLoggedOut() {
		return loggedOut;
	}

	public void setLoggedOut(boolean loggedOut) {
		this.loggedOut = loggedOut;
	}

	public Map<String, String> getClaimMapping() {
		return claimMapping;
	}

	public void setClaimMapping(Map<String, String> claimMapping) {
		this.claimMapping = claimMapping;
	}
	
}
