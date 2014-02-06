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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.wso2.carbon.identity.application.authentication.framework.context.ApplicationAuthenticationContext;

/**
 * API of the Application Authenticators.
 *
 */
public interface ApplicationAuthenticator {

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
											ApplicationAuthenticationContext context);
	
	/**
	 * Do logout
	 * @param request
	 * @param response
	 * @return
	 */
	public AuthenticatorStatus logout(HttpServletRequest request, 
									  HttpServletResponse response, 
									  ApplicationAuthenticationContext context);
	
	/**
	 * Common Servlet will call this if a common login page is not defined.
	 * @param request
	 * @param response
	 */
	public void sendInitialRequest(HttpServletRequest request, 
								   HttpServletResponse response, 
								   ApplicationAuthenticationContext context);
	
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
	public String getResponseAttributes(HttpServletRequest request);
	
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
}