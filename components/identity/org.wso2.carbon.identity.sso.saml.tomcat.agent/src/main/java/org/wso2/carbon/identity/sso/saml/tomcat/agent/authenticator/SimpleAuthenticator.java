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

import org.wso2.carbon.identity.sso.saml.tomcat.agent.SSOConfigs;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This set all attribute of users in to the session after authentication
 * You can configure attribute id of the SAML subject name using web.xml
 */
public class SimpleAuthenticator implements Authenticator {

	@Override
	public void authenticate(HttpServletRequest request, HttpServletResponse response,
                  String samlResponse, Map<String, String> samlAttributeMap) throws Exception {
		
		for (Map.Entry<String, String> attributePair : samlAttributeMap
				.entrySet()) {
			request.getSession().setAttribute(
					attributePair.getKey(),
					attributePair.getValue());

		}
		response.sendRedirect(SSOConfigs.getHomePage());
	}
}
