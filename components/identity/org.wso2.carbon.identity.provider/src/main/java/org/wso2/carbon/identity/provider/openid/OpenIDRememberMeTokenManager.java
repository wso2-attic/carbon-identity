/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.provider.openid;

import java.sql.Timestamp;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.core.model.OpenIDRememberMeDO;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.provider.openid.cache.OpenIDRememberMeTokenCache;
import org.wso2.carbon.identity.provider.openid.dao.OpenIDRememberMeTokenDAO;

/**
 * This class handles RememberMe tokens used in OpenID login
 * @author WSO2 Inc
 *
 */
public class OpenIDRememberMeTokenManager {
	
	private static Log log = LogFactory.getLog(OpenIDRememberMeTokenManager.class);
	private static OpenIDRememberMeTokenCache cache = OpenIDRememberMeTokenCache.getCacheInstance();
	private OpenIDRememberMeTokenDAO dao;
	
	public OpenIDRememberMeTokenManager() {
		dao = new OpenIDRememberMeTokenDAO(); 
	}

	/**
	 * Returns the remembeMe token. Returns null if the token is not found or
	 * expired.
	 * 
	 * @param rememberMe
	 * @return
	 * @throws Exception
	 */
	public String getToken(OpenIDRememberMeDO rememberMe) throws Exception {
		OpenIDRememberMeDO storedDo = null;
		if((storedDo = cache.getTokenData(rememberMe)) == null) {
			storedDo = dao.getTokenData(rememberMe);
			cache.updateTokenData(rememberMe);
		}
		if(storedDo == null) {
			log.debug("No rememberMe token found for " + rememberMe.getUserName());
			return null;
		}
		if(!isExpired(storedDo)) {
			return storedDo.getToken();
		}
		return null;
    }

	/**
	 * Checks if the rememberMe token is expired
	 * 
	 * @param storedDo
	 * @return
	 */
	private boolean isExpired(OpenIDRememberMeDO storedDo) {
		Timestamp timestamp = storedDo.getTimestamp();
		String expiry = IdentityUtil.getProperty(IdentityConstants.ServerConfig.OPENID_REMEMBER_ME_EXPIRY);
		if (timestamp != null && expiry != null) {
			long t0 = timestamp.getTime();
			long t1 = new Date().getTime();
			long delta = Long.parseLong(expiry) * 1000 * 60; 
			
			if (t1 - t0 > delta) {
				log.debug("Remember Me token expired for user " + storedDo.getUserName());
				return true;
			}
		}
		return false;
    }

	/**
	 * Updates the rememberMe token
	 * 
	 * @param rememberMe
	 * @throws Exception
	 */
	public void updateToken(final OpenIDRememberMeDO rememberMe) throws Exception {
		cache.updateTokenData(rememberMe);
		Thread thread = new Thread() {
			public void run() {
				try {
					dao.updateTokenData(rememberMe);
				} catch (Exception e) {
				}
			}
		};
		thread.start();
	}

}
