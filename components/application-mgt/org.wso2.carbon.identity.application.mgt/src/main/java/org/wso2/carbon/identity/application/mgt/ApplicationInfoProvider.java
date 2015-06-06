/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.application.mgt;

import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.AuthenticationStep;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants;
import org.wso2.carbon.identity.application.mgt.cache.IdentityServiceProviderCache;
import org.wso2.carbon.identity.application.mgt.cache.IdentityServiceProviderCacheEntry;
import org.wso2.carbon.identity.application.mgt.cache.IdentityServiceProviderCacheKey;
import org.wso2.carbon.identity.application.mgt.dao.ApplicationDAO;
import org.wso2.carbon.identity.application.mgt.dao.impl.FileBasedApplicationDAO;
import org.wso2.carbon.identity.application.mgt.internal.ApplicationManagementServiceComponent;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.util.List;
import java.util.Map;

public class ApplicationInfoProvider {

    private static ApplicationInfoProvider appInfo = new ApplicationInfoProvider();

    /**
     *
     */
    private ApplicationInfoProvider() {

    }

    /**
     * @return
     */
    public static ApplicationInfoProvider getInstance() {
        return appInfo;
    }

    /**
     * [sp-claim-uri,local-idp-claim-uri]
     *
     * @param serviceProviderName
     * @param tenantDomain
     * @return
     * @throws IdentityApplicationManagementException
     */
    public Map<String, String> getServiceProviderToLocalIdPClaimMapping(String serviceProviderName,
                                                                        String tenantDomain)
            throws IdentityApplicationManagementException {

        ApplicationDAO appDAO = ApplicationMgtSystemConfig.getInstance().getApplicationDAO();
        Map<String, String> claimMap = appDAO.getServiceProviderToLocalIdPClaimMapping(
                serviceProviderName, tenantDomain);

        if (claimMap == null
                || claimMap.isEmpty()
                && ApplicationManagementServiceComponent.getFileBasedSPs().containsKey(
                serviceProviderName)) {
            return new FileBasedApplicationDAO().getServiceProviderToLocalIdPClaimMapping(
                    serviceProviderName, tenantDomain);
        }

        return claimMap;
    }

    /**
     * [local-idp-claim-uri,sp-claim-uri]
     *
     * @param serviceProviderName
     * @param tenantDomain
     * @return
     * @throws IdentityApplicationManagementException
     */
    public Map<String, String> getLocalIdPToServiceProviderClaimMapping(String serviceProviderName,
                                                                        String tenantDomain)
            throws IdentityApplicationManagementException {

        ApplicationDAO appDAO = ApplicationMgtSystemConfig.getInstance().getApplicationDAO();
        Map<String, String> claimMap = appDAO.getLocalIdPToServiceProviderClaimMapping(
                serviceProviderName, tenantDomain);

        if (claimMap == null
                || claimMap.isEmpty()
                && ApplicationManagementServiceComponent.getFileBasedSPs().containsKey(
                serviceProviderName)) {
            return new FileBasedApplicationDAO().getLocalIdPToServiceProviderClaimMapping(
                    serviceProviderName, tenantDomain);
        }
        return claimMap;

    }

    /**
     * Returns back the requested set of claims by the provided service provider in local idp claim
     * dialect.
     *
     * @param serviceProviderName
     * @param tenantDomain
     * @return
     * @throws IdentityApplicationManagementException
     */
    public List<String> getAllRequestedClaimsByServiceProvider(String serviceProviderName,
                                                               String tenantDomain)
            throws IdentityApplicationManagementException {

        ApplicationDAO appDAO = ApplicationMgtSystemConfig.getInstance().getApplicationDAO();
        List<String> reqClaims = appDAO.getAllRequestedClaimsByServiceProvider(serviceProviderName,
                tenantDomain);

        if (reqClaims == null
                || reqClaims.isEmpty()
                && ApplicationManagementServiceComponent.getFileBasedSPs().containsKey(
                serviceProviderName)) {
            return new FileBasedApplicationDAO().getAllRequestedClaimsByServiceProvider(
                    serviceProviderName, tenantDomain);
        }

        return reqClaims;
    }

    /**
     * @param clientId
     * @param clientType
     * @param tenantDomain
     * @return
     * @throws IdentityApplicationManagementException
     */
    public String getServiceProviderNameByClientId(String clientId, String clientType,
                                                   String tenantDomain) throws IdentityApplicationManagementException {

        String name;

        ApplicationDAO appDAO = ApplicationMgtSystemConfig.getInstance().getApplicationDAO();
        name = appDAO.getServiceProviderNameByClientId(clientId, clientType, tenantDomain);

        if (name == null) {
            name = new FileBasedApplicationDAO().getServiceProviderNameByClientId(clientId,
                    clientType, tenantDomain);
        }

        if (name == null) {
            ServiceProvider defaultSP = ApplicationManagementServiceComponent.getFileBasedSPs()
                    .get(IdentityApplicationConstants.DEFAULT_SP_CONFIG);
            name = defaultSP.getApplicationName();
        }

        return name;

    }

    /**
     * @param serviceProviderName
     * @param tenantDomain
     * @return
     * @throws IdentityApplicationManagementException
     */
    public ServiceProvider getServiceProvider(String serviceProviderName, String tenantDomain)
            throws IdentityApplicationManagementException {

        ApplicationDAO appDAO = ApplicationMgtSystemConfig.getInstance().getApplicationDAO();
        ServiceProvider serviceProvider = appDAO.getApplication(serviceProviderName, tenantDomain);

        if (serviceProvider != null
                && ApplicationManagementServiceComponent.getFileBasedSPs().containsKey(
                serviceProviderName)) {
            serviceProvider = ApplicationManagementServiceComponent.getFileBasedSPs().get(
                    serviceProviderName);
        }

        return serviceProvider;
    }

    /**
     * @param clientId
     * @param clientType
     * @param tenantDomain
     * @return
     * @throws IdentityApplicationManagementException
     */
    public ServiceProvider getServiceProviderByClienId(String clientId, String clientType,
                                                       String tenantDomain) throws IdentityApplicationManagementException
    {

        String clientKey = null;

        // client id can contain the @ to identify the tenant domain.
        if (clientId != null && clientId.contains("@")) {
            clientKey = clientId.split("@")[0];
        }

        String serviceProviderName = null;
        ServiceProvider serviceProvider = null;

        serviceProviderName = getServiceProviderNameByClientId(clientKey, clientType, tenantDomain);

        String tenantDomainName = null;
        int tenantId = -1234;

        if (CarbonContext.getThreadLocalCarbonContext() != null) {
            tenantDomainName = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
            tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        }

        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext
                    .getThreadLocalCarbonContext();
            carbonContext.setTenantId(MultitenantConstants.SUPER_TENANT_ID);
            carbonContext.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);

            IdentityServiceProviderCacheKey cacheKey = new IdentityServiceProviderCacheKey(
                    tenantDomain, serviceProviderName);
            IdentityServiceProviderCacheEntry entry = ((IdentityServiceProviderCacheEntry) IdentityServiceProviderCache
                    .getInstance().getValueFromCache(cacheKey));

            if (entry != null) {
                return entry.getServiceProvider();
            }

        } finally {
            PrivilegedCarbonContext.endTenantFlow();

            if (tenantDomainName != null) {
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(
                        tenantDomainName);
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenantId);
            }
        }

        if (serviceProviderName != null) {
            ApplicationDAO appDAO = ApplicationMgtSystemConfig.getInstance().getApplicationDAO();
            serviceProvider = appDAO.getApplication(serviceProviderName, tenantDomain);

            if (serviceProvider != null) {
                // if "Authentication Type" is "Default" we must get the steps from the default SP
                AuthenticationStep[] authenticationSteps = serviceProvider
                        .getLocalAndOutBoundAuthenticationConfig().getAuthenticationSteps();

                if (authenticationSteps == null || authenticationSteps.length == 0) {
                    ServiceProvider defaultSP = ApplicationManagementServiceComponent
                            .getFileBasedSPs().get(IdentityApplicationConstants.DEFAULT_SP_CONFIG);
                    authenticationSteps = defaultSP.getLocalAndOutBoundAuthenticationConfig()
                            .getAuthenticationSteps();
                    serviceProvider.getLocalAndOutBoundAuthenticationConfig()
                            .setAuthenticationSteps(authenticationSteps);
                }
            }
        }

        if (serviceProvider == null
                && serviceProviderName != null
                && ApplicationManagementServiceComponent.getFileBasedSPs().containsKey(
                serviceProviderName)) {
            serviceProvider = ApplicationManagementServiceComponent.getFileBasedSPs().get(
                    serviceProviderName);
        }

        try {
            PrivilegedCarbonContext.startTenantFlow();

            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext
                    .getThreadLocalCarbonContext();
            carbonContext.setTenantId(MultitenantConstants.SUPER_TENANT_ID);
            carbonContext.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);

            IdentityServiceProviderCacheKey cacheKey = new IdentityServiceProviderCacheKey(
                    tenantDomain, serviceProviderName);
            IdentityServiceProviderCacheEntry entry = new IdentityServiceProviderCacheEntry();
            entry.setServiceProvider(serviceProvider);
            IdentityServiceProviderCache.getInstance().addToCache(cacheKey, entry);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();

            if (tenantDomain != null) {
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(
                        tenantDomainName);
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenantId);
            }
        }
        return serviceProvider;
    }
}
