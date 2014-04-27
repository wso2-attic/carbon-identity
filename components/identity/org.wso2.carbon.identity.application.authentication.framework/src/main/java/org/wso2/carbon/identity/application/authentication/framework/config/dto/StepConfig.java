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

package org.wso2.carbon.identity.application.authentication.framework.config.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds the login page and the authenticator objects 
 * of a particular factor
 *
 */
public class StepConfig implements Serializable {
	
	private static final long serialVersionUID = 9139242244264058990L;
	
	private int order;
	private String loginPage;
	private String authenticatedUser;
	private String authenticatedIdP;
	private AuthenticatorConfig authenticatedAutenticator;
	private List<AuthenticatorConfig> authenticatorList = new ArrayList<AuthenticatorConfig>();
	private List<String> authenticatorMappings = new ArrayList<String>();

	public StepConfig() {}
	
	public String getLoginPage() {
		return loginPage;
	}
	
	public void setLoginPage(String loginPage) {
		this.loginPage = loginPage;
	}

	public List<String> getAuthenticatorMappings() {
		return authenticatorMappings;
	}

	public void setAuthenticatorMappings(List<String> authenticatorMappings) {
		this.authenticatorMappings = authenticatorMappings;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public List<AuthenticatorConfig> getAuthenticatorList() {
		return authenticatorList;
	}

	public void setAuthenticatorList(List<AuthenticatorConfig> authenticatorList) {
		this.authenticatorList = authenticatorList;
	}

	public String getAuthenticatedUser() {
		return authenticatedUser;
	}

	public void setAuthenticatedUser(String authenticatedUser) {
		this.authenticatedUser = authenticatedUser;
	}

	public String getAuthenticatedIdP() {
		return authenticatedIdP;
	}

	public void setAuthenticatedIdP(String authenticatedIdP) {
		this.authenticatedIdP = authenticatedIdP;
	}

	public AuthenticatorConfig getAuthenticatedAutenticator() {
		return authenticatedAutenticator;
	}

	public void setAuthenticatedAutenticator(
			AuthenticatorConfig authenticatedAutenticator) {
		this.authenticatedAutenticator = authenticatedAutenticator;
	}
}
