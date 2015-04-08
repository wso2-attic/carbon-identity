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
package org.wso2.carbon.identity.oauth.endpoint.util;

import org.apache.amber.oauth2.common.exception.OAuthSystemException;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.model.OpenIDUserRPDO;
import org.wso2.carbon.identity.provider.openid.dao.OpenIDUserRPDAO;

/**
 * Stores user consent on applications
 * 
 */
public class OpenIDConnectUserRPStore {

	private static OpenIDConnectUserRPStore store = new OpenIDConnectUserRPStore();
	
	private static final String DEFAULT_PROFILE_NAME = "default";
	
	public static OpenIDConnectUserRPStore getInstance() {
		return store;
	}

	private OpenIDConnectUserRPStore() {

	}

	/**
	 * @param username
	 * @param appName
	 * @throws OAuthSystemException 
	 */
	public void putUserRPToStore(String username, String appName, boolean trustedAlways) throws OAuthSystemException {
		OpenIDUserRPDO repDO = new OpenIDUserRPDO();
		repDO.setDefaultProfileName(DEFAULT_PROFILE_NAME);
		repDO.setRpUrl(appName);
		repDO.setUserName(username);
		repDO.setTrustedAlways(trustedAlways);

		OpenIDUserRPDAO dao = new OpenIDUserRPDAO();
		try {
			dao.createOrUpdate(repDO);
		} catch (IdentityException e) {
			throw new OAuthSystemException("Error while storing user consent", e);
		}
	}

	/**
	 * 
	 * @param username
	 * @param appName
	 * @return
	 * @throws OAuthSystemException 
	 */
	public synchronized boolean hasUserApproved(String username, String appName) throws OAuthSystemException {
		OpenIDUserRPDAO dao = new OpenIDUserRPDAO();
		try {
	        OpenIDUserRPDO rpDO = dao.getOpenIDUserRP(username, appName);
	        if(rpDO != null && rpDO.isTrustedAlways()) {
	        	return true;
	        }
        } catch (IdentityException e) {
        	throw new OAuthSystemException("Error while loading user consent", e);
        }
		return false;
	}
}
