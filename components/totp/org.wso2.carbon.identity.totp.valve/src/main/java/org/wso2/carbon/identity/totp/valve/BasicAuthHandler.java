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

package org.wso2.carbon.identity.totp.valve;

import org.apache.axiom.om.util.Base64;
import org.apache.catalina.connector.Request;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.nio.charset.Charset;
import java.util.Map;

/**
 * Basic Authentication handler class.
 */
public class BasicAuthHandler implements TOTPAuthenticationHandler {
	private static Log log = LogFactory.getLog(BasicAuthHandler.class);
	private int priority;
	private Map<String, String> properties;
	private final int DEFAULT_PRIORITY = 5;

	@Override
	public boolean canHandler(Request request) {

		String authheader = request.getHeader(Constants.AUTHORIZATION_HEADER);
		return (authheader != null && authheader.startsWith(Constants.BASIC_AUTH_HEADER));
	}

	/**
	 * Check whether a given request is authenticated or not.
	 *
	 * @param request
	 * @return
	 */
	@Override
	public boolean isAuthenticated(Request request) {

		String authheader = request.getHeader(Constants.AUTHORIZATION_HEADER);

		if (authheader != null && authheader.startsWith(Constants.BASIC_AUTH_HEADER)) {
			// Authorization: Basic base64credentials
			String base64Credentials = authheader.substring(Constants.BASIC_AUTH_HEADER.length()).trim();
			String credentials = new String(Base64.decode(base64Credentials),
			                                Charset.forName("UTF-8"));
			// credentials = username:password
			final String[] values = credentials.split(":", 2);

			String username = values[0];
			String password = values[1];

			if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
				return false;
			}

			String tenantDomain = MultitenantUtils.getTenantDomain(username);
			String tenantLessUserName = MultitenantUtils.getTenantAwareUsername(username);
			try {
				// get super tenant context and get realm service which is an osgi service
				RealmService realmService = (RealmService) PrivilegedCarbonContext
						.getThreadLocalCarbonContext().getOSGiService(RealmService.class);
				if (realmService != null) {
					int tenantId = realmService.getTenantManager().getTenantId(tenantDomain);
					if (tenantId == MultitenantConstants.INVALID_TENANT_ID) {
						log.error("Invalid tenant domain " + tenantDomain);
						return false;
					}
					// get tenant's user realm
					UserRealm userRealm = realmService.getTenantUserRealm(tenantId);
					return userRealm.getUserStoreManager().authenticate(tenantLessUserName, password);
				}
			} catch (UserStoreException e) {
				log.error("Can't access the user realm of the user : " + username,e);
			}
		}
		return false;
	}

	@Override
	public int getPriority() {
		return this.priority;
	}

	@Override
	public void setPriority(int priority) {
		this.priority = priority;
	}

	@Override
	public void setProperties(Map<String, String> authenticatorProperties) {
		// set the priority read from config
		this.properties = authenticatorProperties;
		String priorityString = properties.get(Constants.PROPERTY_NAME_PRIORITY);
		if (priorityString != null) {
			priority = Integer.parseInt(properties.get(Constants.PROPERTY_NAME_PRIORITY));
		} else {
			priority = DEFAULT_PRIORITY;
		}
	}

	public void setDefaultPriority() {
		priority = DEFAULT_PRIORITY;
	}
}
