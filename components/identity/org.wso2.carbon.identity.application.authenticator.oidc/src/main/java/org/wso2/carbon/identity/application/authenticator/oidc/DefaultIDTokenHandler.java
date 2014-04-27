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

import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.apache.amber.oauth2.common.utils.JSONUtils;

public class DefaultIDTokenHandler implements IDTokenHandler {

	Log log = LogFactory.getLog(DefaultIDTokenHandler.class);

	@Override
	public boolean handle(String idToken, AuthenticationContext context) {

		try {
			String base64Body = idToken.split("\\.")[1];
			byte[] decoded = Base64.decodeBase64(base64Body.getBytes());
			String json = new String(decoded);

			Map<String, Object> jsonObject = JSONUtils.parseJSON(json);
			String authenticatedUser = (String) jsonObject.get("sub");
			context.setSubject(authenticatedUser);
			
		} catch (Exception e) {
			log.error(e);
			return false;
		}

		return true;
	}

}
