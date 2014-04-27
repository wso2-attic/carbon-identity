/*
 *Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.application.authenticator.oidc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.amber.oauth2.common.utils.JSONUtils;
import org.wso2.carbon.identity.application.authentication.framework.config.dto.ExternalIdPConfig;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;

class OIDCUserInfoClaimsRetriever implements ClaimsRetriever {
	
	private static Log log = LogFactory.getLog(OIDCUserInfoClaimsRetriever.class);

	@Override
	public Map<String, String> retrieveClaims(String accessToken,
			ExternalIdPConfig externalIdPConfig, AuthenticationContext context) {

		String userEndpoint = null;

		return null;

	}

	private Map<String, String> getUserInfoResponse(String accessToken, String userEndpoint) {

		try {
			
			HttpClient client = new HttpClient();
			client.getParams().setAuthenticationPreemptive(true);
			HttpMethod method = new GetMethod(userEndpoint);
			method.addRequestHeader("Authorization", "Bearer " + accessToken);
			int code = client.executeMethod(method);
			
			if(code != 200 ) {
				log.error("Failed to fetch claims. HTTP Code: " + code);
				return null;
			}
			
			String responseString = method.getResponseBodyAsString();
			
			if(log.isDebugEnabled()) {
				log.debug("Retrieved Claims: " + responseString);
			}
			
			if(responseString == null || "".equals(responseString)) {
				return null;
			}
			method.releaseConnection();
			
			Map<String, Object> claims = JSONUtils.parseJSON(responseString);
			
			Map<String, String> response = new HashMap<String, String>();
			
			for(Map.Entry<String, Object> entry : claims.entrySet()) {
				response.put(entry.getKey(), (String) entry.getValue());
			}
			
			return response;
			
			
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} 

		return null;
		
	}

}
