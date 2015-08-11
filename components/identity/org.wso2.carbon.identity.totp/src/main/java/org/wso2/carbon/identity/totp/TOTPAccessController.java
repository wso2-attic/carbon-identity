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


public class TOTPAccessController {

	private static Log log = LogFactory.getLog(TOTPAccessController.class);
	private static volatile TOTPAccessController instance;

	private TOTPAccessController() {
	}

	public static TOTPAccessController getInstance() {
		if (instance == null) {
			synchronized (TOTPAccessController.class) {
				if (instance == null) {
					instance = new TOTPAccessController();
				}
			}
		}
		return instance;
	}

	public boolean isTOTPEnabledForLocalUser(String username) throws TOTPException {

		try {
			int tenantId = IdentityUtil.getTenantIdOFUser(username);
			UserRealm userRealm = TOTPManagerComponent.getRealmService().getTenantUserRealm(tenantId);
			if (userRealm != null) {
				UserStoreManager userStoreManager = userRealm.getUserStoreManager();
				String secretKey = userStoreManager.getUserClaimValue(MultitenantUtils.getTenantAwareUsername
						(username), Constants.SECRET_KEY, null);
				String currentEncoding = TOTPUtil.getEncodingMethod();
				String storedEncoding = userStoreManager.getUserClaimValue(MultitenantUtils.getTenantAwareUsername
						(username), Constants.Encoding, null);

				if (!currentEncoding.equals(storedEncoding)) {
					userStoreManager.setUserClaimValue(MultitenantUtils.getTenantAwareUsername(username), 
					                                   Constants.SECRET_KEY, "", null);
					userStoreManager.setUserClaimValue(MultitenantUtils.getTenantAwareUsername(username), 
					                                   Constants.QR_CODE_URL, "", null);
					userStoreManager.setUserClaimValue(MultitenantUtils.getTenantAwareUsername(username), 
					                                   Constants.Encoding, "Invalid", null);
					if (log.isDebugEnabled()) {
						log.debug("TOTP user claims was cleared of the user : " + username);
					}
					return false;
				}

				if ("".equals(secretKey) || secretKey == null) {
					return false;
				} else {
					return true;
				}
			} else {
				throw new TOTPException("Cannot find the user realm for the given tenant domain : " + CarbonContext
						.getThreadLocalCarbonContext().getTenantDomain());
			}
		} catch (IdentityException e) {
			throw new TOTPException("TOTPAccessController failed while trying to get the tenant ID of the user : " + 
			                        username, e);
		} catch (UserStoreException e) {
			throw new TOTPException("TOTPAccessController failed while trying to access userRealm of the user : " + 
			                        username, e);
		} catch (IdentityApplicationManagementException e) {
			throw new TOTPException("TOTPAccessController failed while trying to access encoding method of the user : " +
			                        "" + username, e);
		}
	}
}
