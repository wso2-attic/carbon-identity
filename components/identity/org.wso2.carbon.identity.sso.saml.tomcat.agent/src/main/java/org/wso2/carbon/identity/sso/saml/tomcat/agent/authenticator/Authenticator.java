/*
*  Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.sso.saml.tomcat.agent.authenticator;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * After SAML Assertion is processed by the agent, user must be login to the web application.
 * If web application already uses authentication mechanism, SAML2 SSO authentication also must be
 * compliant with it. This interface provide bridge for it. You can implement this interface according
 * to your requirement. Basically you can retrieve the attributes or any other data from validated
 * SAML Assertion and use them to compliant with current authentication
 *
 * As an example, your existing web application, would keeps authenticated user's email address as a
 * session attribute (say userEmail). It is the way that web application identifies authenticated session.
 *
 * In your implementation you could retrieve the email from SAML assertion and set it in to the session
 * attribute
 *
 */
public interface Authenticator {

    /**
     * 
     * @param request request
     * @param response response
     * @param samlResponse SAML response as String
     * @param samlAttributeMap  user's attribute that is received in SAML assertion.
     * @throws Exception if any authentication failure
     */
	public void authenticate(HttpServletRequest request, HttpServletResponse response,
                        String samlResponse, Map<String, String> samlAttributeMap) throws Exception;

}
