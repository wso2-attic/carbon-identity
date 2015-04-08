/*
 * Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.oauth.endpoint.user.impl;

import org.apache.amber.oauth2.common.error.OAuthError;
import org.wso2.carbon.identity.oauth.endpoint.user.UserInfoEndpointException;
import org.wso2.carbon.identity.oauth.endpoint.user.UserInfoRequestValidator;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;

/**
 * Validates the schema and authorization header according to the specification
 * 
 * @see http://openid.net/specs/openid-connect-basic-1_0-22.html#anchor6
 */
public class UserInforRequestDefaultValidator implements UserInfoRequestValidator {

	public String validateRequest(HttpServletRequest request) throws UserInfoEndpointException {

		String schema = request.getParameter("schema");
		String authzHeaders = request.getHeader(HttpHeaders.AUTHORIZATION);

		if (!"openid".equals(schema)) {
			throw new UserInfoEndpointException(UserInfoEndpointException.ERROR_CODE_INVALID_SCHEMA,
			                                    "Schema should be openid");
		}

		if (authzHeaders == null) {
			throw new UserInfoEndpointException(OAuthError.ResourceResponse.INVALID_REQUEST,
			                                    "Authorization header missing");
		}
		
		String[] authzHeaderInfo = ((String) authzHeaders).trim().split(" ");
		if (!"Bearer".equals(authzHeaderInfo[0])) {
			throw new UserInfoEndpointException(OAuthError.ResourceResponse.INVALID_REQUEST, "Bearer token missing");
		}
		return authzHeaderInfo[1];
	}

}
