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

package org.wso2.carbon.identity.application.authentication.framework;

import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.util.Map;

/**
 * API of the Application Authenticators.
 *
 */
public interface ApplicationAuthenticator extends Serializable {

	/**
	 * Check whether the request can be handled by the authenticator
	 * @param request
	 * @return boolean
	 */
	public boolean canHandle(HttpServletRequest request);
	
	/**
	 * Do authentication
	 * @param request
	 * @param response
	 * @return
	 */
	public AuthenticatorStatus authenticate(HttpServletRequest request, 
											HttpServletResponse response, 
											AuthenticationContext context);
	
	/**
	 * Do logout
	 * @param request
	 * @param response
	 * @return
	 */
	public AuthenticatorStatus logout(HttpServletRequest request, 
									  HttpServletResponse response, 
									  AuthenticationContext context,
									  AuthenticatorStateInfo stateInfoDTO);
	
	/**
	 * Common Servlet will call this if a common login page is not defined.
	 * @param request
	 * @param response
	 */
	public void sendInitialRequest(HttpServletRequest request, 
								   HttpServletResponse response, 
								   AuthenticationContext context);
	
	/**
	 * Get the authenticated subject
	 * @param request
	 * @return
	 */
	public String getAuthenticatedSubject(HttpServletRequest request);

	/**
	 * Get the attributes sent in the response
	 * @param request
	 * @return
	 */
	public Map<String, String> getResponseAttributes(HttpServletRequest request, AuthenticationContext context);

	/**
	 * Get any (state) information that would be required by the authenticator
	 * for later processing.
	 * E.g. sessionIndex for SAMLSSOAuthenticator in SLO.
	 * Each authenticator should have an internal DTO that extends the
	 * AuthenticatorStateInfoDTO and set all the required state info in it.
	 * Framework will call this method after successful authentication and
	 * that DTO should be returned from this method.
	 *
	 * @return
	 */
	public AuthenticatorStateInfo getStateInfo(HttpServletRequest request);
	
	/**
     * Get the Context identifier sent with the request
     * @param request
     * @return
     */
	public String getContextIdentifier(HttpServletRequest request);
	
	/**
	 * Get the name of the Authenticator
	 * @return name
	 */
	public String getAuthenticatorName();
	
	/**
	 * Check whether the Authenticator is disabled
	 * @return boolean
	 */
	public boolean isEnabled();

    /**
     * Get the claim dialect URI if this authenticator receives claims in a standard dialect
     * and needs to be mapped to the Carbon dialect http://wso2.org/claims
     * @return boolean
     */
    public String getClaimDialectURIIfStandard();
}