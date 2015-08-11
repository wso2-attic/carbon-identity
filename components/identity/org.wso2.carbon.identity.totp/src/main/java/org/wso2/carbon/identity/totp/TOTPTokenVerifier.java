/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.totp;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import com.warrenstrange.googleauth.KeyRepresentation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.totp.exception.TOTPException;
import org.wso2.carbon.identity.totp.internal.TOTPManagerComponent;
import org.wso2.carbon.identity.totp.util.TOTPUtil;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

/**
 * TOTP Token verifier class.
 */
public class TOTPTokenVerifier {

	private static Log log = LogFactory.getLog(TOTPTokenVerifier.class);
	private static volatile TOTPTokenVerifier instance;

	private TOTPTokenVerifier() {
	}

	;

	/**
	 * Singleton method to get instance of TOTPTokenVerifier.
	 *
	 * @return instance of TOTPTokenVerifier
	 */
	public static TOTPTokenVerifier getInstance() {
		if (instance == null) {
			synchronized (TOTPTokenVerifier.class) {
				if (instance == null) {
					instance = new TOTPTokenVerifier();
				}
			}
		}
		return instance;
	}

	/**
	 * Verify whether a given token is valid for a stored local user.
	 *
	 * @param token    TOTP Token
	 * @param username Username of the user
	 * @return true if token is valid otherwise false
	 * @throws TOTPException
	 */
	public boolean isValidTokenLocalUser(int token, String username) throws TOTPException {

		KeyRepresentation encoding = KeyRepresentation.BASE32;
		try {
			if ("Base64".equals(TOTPUtil.getEncodingMethod())) {
				encoding = KeyRepresentation.BASE64;
			}
		} catch (IdentityApplicationManagementException e) {
			log.error("Error when reading the tenant encoding method");
		}

		GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder gacb = new GoogleAuthenticatorConfig
				.GoogleAuthenticatorConfigBuilder()
				.setKeyRepresentation(encoding);
		GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator(gacb.build());

		try {
			int tenantId = IdentityUtil.getTenantIdOFUser(username);
			UserRealm userRealm = TOTPManagerComponent.getRealmService().getTenantUserRealm(tenantId);
			if (userRealm != null) {
				UserStoreManager userStoreManager = userRealm.getUserStoreManager();
				String secretkey = userStoreManager.getUserClaimValue(MultitenantUtils.getTenantAwareUsername
						(username), Constants.SECRET_KEY, null);
				return googleAuthenticator.authorize(secretkey, token);
			} else {
				throw new TOTPException("Cannot find the user realm for the given tenant domain : " + CarbonContext
						.getThreadLocalCarbonContext().getTenantDomain());
			}
		} catch (IdentityException e) {
			throw new TOTPException("TOTPTokenVerifier failed while trying to get the tenant ID of the user : " + 
			                        username, e);
		} catch (UserStoreException e) {
			throw new TOTPException("TOTPTokenVerifier failed while trying to access userRealm of the user : " +
			                        username, e);
		}
	}

	/**
	 * Verify whether a given token is valid for given secret key.
	 *
	 * @param token     totp token
	 * @param secretKey secret key
	 * @return true if token is valid, return false otherwise.
	 */
	public boolean isValidToken(int token, String secretKey) {

		KeyRepresentation encoding = KeyRepresentation.BASE32;
		try {
			if ("Base64".equals(TOTPUtil.getEncodingMethod())) {
				encoding = KeyRepresentation.BASE64;
			}
		} catch (IdentityApplicationManagementException e) {
			log.error("Error when reading the tenant encoding method");
		}
		GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder gacb = new GoogleAuthenticatorConfig
				.GoogleAuthenticatorConfigBuilder()
				.setKeyRepresentation(encoding);
		GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator(gacb.build());
		return googleAuthenticator.authorize(secretKey, token);
	}


}
