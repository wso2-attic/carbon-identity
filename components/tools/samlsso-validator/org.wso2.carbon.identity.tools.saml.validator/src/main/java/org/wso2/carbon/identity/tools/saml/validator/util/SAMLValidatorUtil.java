/*
 * Copyright 2005-2014 WSO2, Inc. (http://wso2.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.tools.saml.validator.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonException;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.core.util.AnonymousSessionUtil;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.model.SAMLSSOServiceProviderDO;
import org.wso2.carbon.identity.core.persistence.IdentityPersistenceManager;
import org.wso2.carbon.identity.sso.saml.SSOServiceProviderConfigManager;
import org.wso2.carbon.identity.sso.saml.util.SAMLSSOUtil;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SAMLValidatorUtil {

    private static Log log = LogFactory.getLog(SAMLValidatorUtil.class);

    /**
     * Get all SAML Issuers from configurations
     *
     * @return Issuer List
     * @throws IdentityException
     */
    public static String[] getIssuersOfSAMLServiceProviders() throws IdentityException {
        try {
            IdentityPersistenceManager persistenceManager =
                    IdentityPersistenceManager.getPersistanceManager();
            int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
            UserRegistry registry =
                    SAMLSSOUtil.getRegistryService()
                            .getConfigSystemRegistry(tenantId);
            SAMLSSOServiceProviderDO[] serviceProviderDOs =
                    persistenceManager.getServiceProviders(registry);
            if (serviceProviderDOs != null && serviceProviderDOs.length > 0) {
                List<String> issuers = new ArrayList<String>();
                for (SAMLSSOServiceProviderDO providerDO : serviceProviderDOs) {
                    issuers.add(providerDO.getIssuer());
                }
                return issuers.toArray(new String[issuers.size()]);
            }
        } catch (Exception e) {
            throw IdentityException.error(
                    SAMLValidatorConstants.ValidationMessage.ERROR_LOADING_SP_CONF,
                    e);
        }
        return null;
    }

    /**
     * Load Service Provider Configurations
     *
     * @param issuer
     * @return SAMLSSOServiceProviderDO
     * @throws IdentityException
     */
    public static SAMLSSOServiceProviderDO getServiceProviderConfig(String issuer)
            throws IdentityException {
        try {
            SSOServiceProviderConfigManager idPConfigManager =
                    SSOServiceProviderConfigManager.getInstance();
            SAMLSSOServiceProviderDO ssoIdpConfigs = idPConfigManager.getServiceProvider(issuer);
            if (ssoIdpConfigs == null) {
                IdentityPersistenceManager persistenceManager =
                        IdentityPersistenceManager.getPersistanceManager();
                int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
                UserRegistry registry =
                        SAMLSSOUtil.getRegistryService()
                                .getConfigSystemRegistry(tenantId);
                ssoIdpConfigs = persistenceManager.getServiceProvider(registry, issuer);
            }
            return ssoIdpConfigs;
        } catch (Exception e) {
            throw IdentityException.error(
                    SAMLValidatorConstants.ValidationMessage.ERROR_LOADING_SP_CONF,
                    e);
        }
    }

    /**
     * Extract SAML query string from URL
     *
     * @param url
     * @return query string
     */
    public static String getQueryString(String url) {
        String[] temp = url.split("\\?");
        if (temp != null && temp.length > 1) {
            return temp[1];
        }
        return null;
    }

    /**
     * Get SAML request form URL
     *
     * @param url
     * @return encoded SAML request
     * @throws UnsupportedEncodingException
     */
    public static String getSAMLRequestFromURL(String url) throws UnsupportedEncodingException {
        String decodedURL = java.net.URLDecoder.decode(url, "UTF-8");
        String[] temp = decodedURL.split("\\?");
        if (temp != null && temp.length > 1) {
            String[] parameters = temp[1].split("&");
            if (parameters != null) {
                for (String parameter : parameters) {
                    if (parameter.contains("SAMLRequest")) {
                        String[] keyValuePair = parameter.split("=");
                        return keyValuePair != null && keyValuePair.length > 1 ? keyValuePair[1]
                                : null;
                    }
                }
            }
        }
        return null;
    }

    public static Map<String, String> getUserClaimValues(String username, String[] requestedClaims, String profile)
            throws IdentityException {
        try {
            UserRealm userRealm = AnonymousSessionUtil.getRealmByUserName(SAMLSSOUtil.getRegistryService(),
                    SAMLSSOUtil.getRealmService(), username);
            if(userRealm == null){
                throw IdentityException.error("User realm is not present for this user name:" + username);
            }
            username = MultitenantUtils.getTenantAwareUsername(username);
            UserStoreManager userStoreManager = userRealm.getUserStoreManager();
            return userStoreManager.getUserClaimValues(username, requestedClaims, profile);
        } catch (UserStoreException e) {
            log.error("Error while retrieving claims values", e);
            throw IdentityException.error(
                    "Error while retrieving claims values", e);
        } catch (CarbonException e) {
            log.error("Error while retrieving claims values", e);
            throw IdentityException.error(
                    "Error while retrieving claim values",
                    e);
        }
    }

}
