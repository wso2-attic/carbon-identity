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
package org.wso2.carbon.identity.core.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonException;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.core.util.AdminServicesUtil;
import org.wso2.carbon.core.util.AnonymousSessionUtil;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.ServerConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

public class IdentityTenantUtil {

    private static RealmService realmService;
    private static RegistryService registryService;
    private static Log log = LogFactory.getLog(IdentityTenantUtil.class);
    
    public static Registry getConfigRegistry(int tenantId) throws RegistryException {
        return registryService.getConfigSystemRegistry(tenantId);
    }
    

    public static Registry getRegistry(String domainName, String username) throws IdentityException {
        HttpSession httpSess = getHttpSession();

		if (httpSess != null) {
			if (httpSess.getAttribute(ServerConstants.USER_LOGGED_IN) != null) {
				try {
					return AdminServicesUtil.getSystemRegistry();
				} catch (CarbonException e) {
					log.error("Error obtaining a registry instance", e);
					throw new IdentityException(
							"Error obtaining a registry instance", e);
				}
			}
		}
        return getRegistryForAnonymousSession(domainName, username);
    }

    @SuppressWarnings("deprecation")
    public static Registry getRegistry() throws IdentityException {
        try {
            return AdminServicesUtil.getSystemRegistry();
        } catch (CarbonException e) {
            log.error("Error obtaining a registry instance", e);
            throw new IdentityException("Error obtaining a registry instance", e);
        }
    }

    public static UserRealm getRealm(String domainName, String username) throws IdentityException {
        return getRealmForAnonymousSession(domainName, username);
    }

    @SuppressWarnings("deprecation")
    private static Registry getRegistryForAnonymousSession(String domainName, String username)
            throws IdentityException {
        try {
            if (domainName == null && username == null) {
                domainName = MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
            }
            if (username == null) {
                return AnonymousSessionUtil.getSystemRegistryByDomainName(registryService,
                        realmService, domainName);
            } else {
                return AnonymousSessionUtil.getSystemRegistryByUserName(registryService,
                        realmService, username);
            }
        } catch (CarbonException e) {
            log.error("Error obtaining a registry instance", e);
            throw new IdentityException("Error obtaining a registry instance", e);
        }
    }

    private static UserRealm getRealmForAnonymousSession(String domainName, String username)
            throws IdentityException {
        try {
            if (domainName == null && username == null) {
                domainName = MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
            }

            if (username == null) {
                return AnonymousSessionUtil.getRealmByTenantDomain(registryService, realmService,
                        domainName);
            } else if (username != null) {
                return AnonymousSessionUtil.getRealmByUserName(registryService, realmService,
                        username);
            }
        } catch (CarbonException e) {
            log.error("Error obtaining the realm", e);
            throw new IdentityException("Error Obtaining a realm", e);
        }
		return null;
    }

    public static void setRealmService(RealmService realmService) {
        IdentityTenantUtil.realmService = realmService;
    }

    public static void setRegistryService(RegistryService registryService) {
        IdentityTenantUtil.registryService = registryService;
    }

    public static String getGlobalUserName(String userName) {

        if (userName != null && userName.indexOf("@") > 0) {
            return userName;
        }

        String domain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        if (domain != null) {
            return userName + "@" + domain;
        }
        return userName;
    }

    private static HttpSession getHttpSession() {
        MessageContext msgCtx = MessageContext.getCurrentMessageContext();
        HttpSession httpSess = null;
        if (msgCtx != null) {
            HttpServletRequest request = (HttpServletRequest) msgCtx
                    .getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST);
            httpSess = request.getSession();
        }
        return httpSess;
    }

    public static RegistryService getRegistryService() {
        return registryService;
    }
    
    public static RealmService getRealmService(){
        return realmService;
    }
}
