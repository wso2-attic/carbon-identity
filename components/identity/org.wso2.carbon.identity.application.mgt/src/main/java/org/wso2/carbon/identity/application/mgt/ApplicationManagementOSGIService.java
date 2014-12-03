/*
 *Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.application.mgt;

import java.util.ArrayList;
import java.util.List;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.rahas.impl.SAMLTokenIssuerConfig;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.core.RegistryResources;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ApplicationBasicInfo;
import org.wso2.carbon.identity.application.common.model.ApplicationPermission;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.identity.application.common.model.InboundAuthenticationRequestConfig;
import org.wso2.carbon.identity.application.common.model.LocalAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.PermissionsAndRoleConfig;
import org.wso2.carbon.identity.application.common.model.RequestPathAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.mgt.cache.IdentityServiceProviderCache;
import org.wso2.carbon.identity.application.mgt.cache.IdentityServiceProviderCacheKey;
import org.wso2.carbon.identity.application.mgt.dao.ApplicationDAO;
import org.wso2.carbon.identity.application.mgt.dao.IdentityProviderDAO;
import org.wso2.carbon.identity.application.mgt.dao.OAuthApplicationDAO;
import org.wso2.carbon.identity.application.mgt.dao.SAMLApplicationDAO;
import org.wso2.carbon.identity.application.mgt.internal.ApplicationManagementServiceComponentHolder;
import org.wso2.carbon.identity.application.mgt.internal.ApplicationMgtListenerServiceComponent;
import org.wso2.carbon.identity.application.mgt.listener.ApplicationMgtListener;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.security.SecurityConfigException;
import org.wso2.carbon.security.config.SecurityServiceAdmin;
import org.wso2.carbon.user.api.ClaimMapping;
import org.wso2.carbon.utils.ServerConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

/**
 * This class is already deprecated and can use ApplicationManagementServiceImpl for same purpose as osgi service
 */

@Deprecated
public class ApplicationManagementOSGIService{

    private static Log log = LogFactory.getLog(ApplicationManagementOSGIService.class);
    private static ApplicationManagementOSGIService appMgtService = new ApplicationManagementOSGIService();

    /**
     * 
     * @return
     */
    public static ApplicationManagementOSGIService getInstance() {
        return appMgtService;
    }

    /**
     * Creates a service provider with basic information.First we need to create a role with the
     * application name. Only the users in this role will be able to edit/update the application.The
     * user will assigned to the created role.Internal roles used.
     * 
     * @param serviceProvider
     * @return
     * @throws IdentityApplicationManagementException
     */
    public int createApplication(ServiceProvider serviceProvider)
            throws IdentityApplicationManagementException {
        try {

            // invoking the listeners
            List<ApplicationMgtListener> listerns = ApplicationMgtListenerServiceComponent
                    .getListners();
            for (ApplicationMgtListener listner : listerns) {
                listner.createApplication(serviceProvider);
            }

            // first we need to create a role with the application name.
            // only the users in this role will be able to edit/update the application.
            String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
            ApplicationMgtOSGIUtil.createAppRole(serviceProvider.getApplicationName());
            ApplicationDAO appDAO = ApplicationMgtSystemConfig.getInstance().getApplicationDAO();
            ApplicationMgtOSGIUtil.storePermission(serviceProvider.getApplicationName(),
                    serviceProvider.getPermissionAndRoleConfig());
            // create the service provider.
            return appDAO.createApplication(serviceProvider, tenantDomain);
        } catch (Exception e) {
            log.error(
                    "Error occurred while creating the application, "
                            + serviceProvider.getApplicationName(), e);
            throw new IdentityApplicationManagementException(
                    "Error occurred while creating the application", e);

        }
    }

    /**
     * 
     * @param applicationName
     * @return
     * @throws IdentityApplicationManagementException
     */
    public ServiceProvider getApplication(String applicationName)
            throws IdentityApplicationManagementException {

        try {
            if (!ApplicationConstants.LOCAL_SP.equals(applicationName)
                    && !ApplicationMgtOSGIUtil.isUserAuthorized(applicationName)) {
                log.warn("Illegale Access! User " + CarbonContext.getThreadLocalCarbonContext().getUsername()
                        + " does not have access to the application " + applicationName);
                throw new IdentityApplicationManagementException("User not authorized");
            }

            String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();

            ApplicationDAO appDAO = ApplicationMgtSystemConfig.getInstance().getApplicationDAO();
            ServiceProvider serviceProvider = appDAO.getApplication(applicationName, tenantDomain);
            List<ApplicationPermission> permissionList = ApplicationMgtOSGIUtil
                    .loadPermissions(applicationName);
            if (permissionList != null) {
                PermissionsAndRoleConfig permissionAndRoleConfig = null;
                if (serviceProvider.getPermissionAndRoleConfig() == null) {
                    permissionAndRoleConfig = new PermissionsAndRoleConfig();
                } else {
                    permissionAndRoleConfig = serviceProvider.getPermissionAndRoleConfig();
                }
                permissionAndRoleConfig.setPermissions(permissionList
                        .toArray(new ApplicationPermission[permissionList.size()]));
                serviceProvider.setPermissionAndRoleConfig(permissionAndRoleConfig);
            }
            return serviceProvider;
        } catch (Exception e) {
            log.error("Error occurred while retrieving the application, " + applicationName, e);
            throw new IdentityApplicationManagementException(
                    "Error occurred while retreiving the application", e);
        }
    }

    /**
     * 
     * @return
     * @throws IdentityApplicationManagementException
     */
    public ApplicationBasicInfo[] getAllApplicationBasicInfo()
            throws IdentityApplicationManagementException {
        try {
            ApplicationDAO appDAO = ApplicationMgtSystemConfig.getInstance().getApplicationDAO();
            return appDAO.getAllApplicationBasicInfo();
        } catch (Exception e) {
            log.error("Error occurred while retrieving the all applications", e);
            throw new IdentityApplicationManagementException(
                    "Error occurred while retrieving the all applications", e);
        }
    }

    /**
     * 
     * @param serviceProvider
     * @throws IdentityApplicationManagementException
     */
    public void updateApplication(ServiceProvider serviceProvider)
            throws IdentityApplicationManagementException {
        try {

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
                        tenantDomainName, serviceProvider.getApplicationName());
                IdentityServiceProviderCache.getInstance().clearCacheEntry(cacheKey);

            } finally {
                PrivilegedCarbonContext.endTenantFlow();

                if (tenantDomainName != null) {
                    PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(
                            tenantDomainName);
                    PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenantId);
                }
            }

            // invoking the listeners
            List<ApplicationMgtListener> listerns = ApplicationMgtListenerServiceComponent
                    .getListners();
            for (ApplicationMgtListener listner : listerns) {
                listner.updateApplication(serviceProvider);
            }

            // check whether use is authorized to update the application.
            if (!ApplicationConstants.LOCAL_SP.equals(serviceProvider.getApplicationName())
                    && !ApplicationMgtOSGIUtil.isUserAuthorized(serviceProvider.getApplicationName(),
                            serviceProvider.getApplicationID())) {
                log.warn("Illegale Access! User " + CarbonContext.getThreadLocalCarbonContext().getUsername()
                        + " does not have access to the application "
                        + serviceProvider.getApplicationName());
                throw new IdentityApplicationManagementException("User not authorized");
            }

            ApplicationDAO appDAO = ApplicationMgtSystemConfig.getInstance().getApplicationDAO();
            appDAO.updateApplication(serviceProvider);
            ApplicationPermission[] permissions = serviceProvider.getPermissionAndRoleConfig()
                    .getPermissions();
            if (permissions != null && permissions.length > 0) {
                ApplicationMgtOSGIUtil.updatePermissions(serviceProvider.getApplicationName(),
                        permissions);
            }
        } catch (Exception e) {
            log.error("Error occurred while updating the application", e);
            throw new IdentityApplicationManagementException(
                    "Error occurred while updating the application", e);
        }
    }

    /**
     * 
     * @param applicationName
     * @throws IdentityApplicationManagementException
     */
    public void deleteApplication(String applicationName)
            throws IdentityApplicationManagementException {
        try {

            // invoking the listeners
            List<ApplicationMgtListener> listerns = ApplicationMgtListenerServiceComponent
                    .getListners();
            for (ApplicationMgtListener listner : listerns) {
                listner.deleteApplication(applicationName);
                ;
            }

            if (!ApplicationMgtOSGIUtil.isUserAuthorized(applicationName)) {
                log.warn("Illegal Access! User " + CarbonContext.getThreadLocalCarbonContext().getUsername()
                        + " does not have access to the application " + applicationName);
                throw new IdentityApplicationManagementException("User not authorized");
            }

            ApplicationDAO appDAO = ApplicationMgtSystemConfig.getInstance().getApplicationDAO();
            ServiceProvider serviceProvider = appDAO.getApplication(applicationName, CarbonContext
                    .getThreadLocalCarbonContext().getTenantDomain());
            appDAO.deleteApplication(applicationName);

            ApplicationMgtOSGIUtil.deleteAppRole(applicationName);
            ApplicationMgtOSGIUtil.deletePermissions(applicationName);

            if (serviceProvider != null
                    && serviceProvider.getInboundAuthenticationConfig() != null
                    && serviceProvider.getInboundAuthenticationConfig()
                            .getInboundAuthenticationRequestConfigs() != null) {

                InboundAuthenticationRequestConfig[] configs = serviceProvider
                        .getInboundAuthenticationConfig().getInboundAuthenticationRequestConfigs();

                for (InboundAuthenticationRequestConfig config : configs) {
                    if ("samlsso".equalsIgnoreCase(config.getInboundAuthType())
                            && config.getInboundAuthKey() != null) {
                        SAMLApplicationDAO samlDAO = ApplicationMgtSystemConfig.getInstance()
                                .getSAMLClientDAO();
                        samlDAO.removeServiceProviderConfiguration(config.getInboundAuthKey());

                    } else if ("oauth2".equalsIgnoreCase(config.getInboundAuthType())
                            && config.getInboundAuthKey() != null) {
                        OAuthApplicationDAO oathDAO = ApplicationMgtSystemConfig.getInstance()
                                .getOAuthOIDCClientDAO();
                        oathDAO.removeOAuthApplication(config.getInboundAuthKey());

                    } else if ("wstrust".equalsIgnoreCase(config.getInboundAuthType())
                            && config.getInboundAuthKey() != null) {
                        try {
                           /* AxisService stsService = getAxisConfig().getService(
                                    ServerConstants.STS_NAME);*/
                        	AxisService stsService = ApplicationManagementServiceComponentHolder.getConfigContextService().getServerConfigContext().getAxisConfiguration().getService(
                                    ServerConstants.STS_NAME);
                            Parameter origParam = stsService
                                    .getParameter(SAMLTokenIssuerConfig.SAML_ISSUER_CONFIG
                                            .getLocalPart());
                            if (origParam != null) {
                                OMElement samlConfigElem = origParam.getParameterElement()
                                        .getFirstChildWithName(
                                                SAMLTokenIssuerConfig.SAML_ISSUER_CONFIG);
                                SAMLTokenIssuerConfig samlConfig = new SAMLTokenIssuerConfig(
                                        samlConfigElem);
                                samlConfig.getTrustedServices().remove(config.getInboundAuthKey());
                                setSTSParameter(samlConfig);
                                removeTrustedService(ServerConstants.STS_NAME,
                                        ServerConstants.STS_NAME, config.getInboundAuthKey());
                            } else {
                                throw new IdentityApplicationManagementException(
                                        "missing parameter : "
                                                + SAMLTokenIssuerConfig.SAML_ISSUER_CONFIG
                                                        .getLocalPart());
                            }
                        } catch (Exception e) {
                            log.error("Error while removing a trusted service", e);
                            throw new IdentityApplicationManagementException(e.getMessage(), e);
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error occurred while deleting the application", e);
            throw new IdentityApplicationManagementException(
                    "Error occurred while deleting the application", e);
        }
    }

    /**
     * 
     * @param federatedIdPName
     * @return
     * @throws IdentityApplicationManagementException
     */
    public IdentityProvider getIdentityProvider(String federatedIdPName)
            throws IdentityApplicationManagementException {
        try {
            IdentityProviderDAO idpdao = ApplicationMgtSystemConfig.getInstance()
                    .getIdentityProviderDAO();
            return idpdao.getIdentityProvider(federatedIdPName);
        } catch (Exception e) {
            log.error("Error occurred while deleting the application", e);
            throw new IdentityApplicationManagementException(
                    "Error occurred while deleting the application", e);
        }
    }

    /**
     * 
     * @return
     * @throws IdentityApplicationManagementException
     */
    public IdentityProvider[] getAllIdentityProviders()
            throws IdentityApplicationManagementException {
        try {
            IdentityProviderDAO idpdao = ApplicationMgtSystemConfig.getInstance()
                    .getIdentityProviderDAO();
            List<IdentityProvider> fedIdpList = idpdao.getAllIdentityProviders();
            if (fedIdpList != null) {
                return fedIdpList.toArray(new IdentityProvider[fedIdpList.size()]);
            }
            return null;
        } catch (Exception e) {
            log.error("Error occurred while deleting the application", e);
            throw new IdentityApplicationManagementException(
                    "Error occurred while deleting the application", e);
        }
    }

    /**
     * 
     * @return
     * @throws IdentityApplicationManagementException
     */
    public LocalAuthenticatorConfig[] getAllLocalAuthenticators()
            throws IdentityApplicationManagementException {
        try {
            IdentityProviderDAO idpdao = ApplicationMgtSystemConfig.getInstance()
                    .getIdentityProviderDAO();
            List<LocalAuthenticatorConfig> localAuthenticators = idpdao.getAllLocalAuthenticators();
            if (localAuthenticators != null) {
                return localAuthenticators.toArray(new LocalAuthenticatorConfig[localAuthenticators
                        .size()]);
            }
            return null;
        } catch (Exception e) {
            log.error("Error occurred while deleting the application", e);
            throw new IdentityApplicationManagementException(
                    "Error occurred while deleting the application", e);
        }
    }

    /**
     * 
     * @return
     * @throws IdentityApplicationManagementException
     */
    public RequestPathAuthenticatorConfig[] getAllRequestPathAuthenticators()
            throws IdentityApplicationManagementException {
        try {
            IdentityProviderDAO idpdao = ApplicationMgtSystemConfig.getInstance()
                    .getIdentityProviderDAO();
            List<RequestPathAuthenticatorConfig> reqPathAuthenticators = idpdao
                    .getAllRequestPathAuthenticators();
            if (reqPathAuthenticators != null) {
                return reqPathAuthenticators
                        .toArray(new RequestPathAuthenticatorConfig[reqPathAuthenticators.size()]);
            }
            return null;
        } catch (Exception e) {
            log.error("Error occurred while deleting the application", e);
            throw new IdentityApplicationManagementException(
                    "Error occurred while deleting the application", e);
        }
    }

    /**
     * 
     * @return
     * @throws IdentityApplicationManagementException
     */
    public String[] getAllLocalClaimUris() throws IdentityApplicationManagementException {
        try {

            String claimDialect = ApplicationMgtSystemConfig.getInstance().getClaimDialect();
            ClaimMapping[] claimMappings = CarbonContext.getThreadLocalCarbonContext()
                    .getUserRealm().getClaimManager().getAllClaimMappings(claimDialect);
            List<String> claimUris = new ArrayList<String>();
            for (ClaimMapping claimMap : claimMappings) {
                claimUris.add(claimMap.getClaim().getClaimUri());
            }
            return claimUris.toArray(new String[claimUris.size()]);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IdentityApplicationManagementException("Error while reading system claims");
        }
    }

    private void setSTSParameter(SAMLTokenIssuerConfig samlConfig) throws IdentityApplicationManagementException {
    	Registry registry;
		try {
			registry = (Registry) ApplicationManagementServiceComponentHolder.getRegistryService().getConfigSystemRegistry(getTenantId());
			new SecurityServiceAdmin(ApplicationManagementServiceComponentHolder.getConfigContextService().getServerConfigContext().getAxisConfiguration(), registry)
            .setServiceParameterElement(ServerConstants.STS_NAME, samlConfig.getParameter());
		} catch (Exception ex) {
			throw new IdentityApplicationManagementException(ex);
		}
        
    }

    private void removeTrustedService(String groupName, String serviceName, String trustedService)
            throws SecurityConfigException {
        Registry registry;
        String resourcePath;
        Resource resource;
        try {
            resourcePath = RegistryResources.SERVICE_GROUPS + groupName
                    + RegistryResources.SERVICES + serviceName + "/trustedServices";
            //registry = getConfigSystemRegistry();
            registry = (Registry) ApplicationManagementServiceComponentHolder.getRegistryService().getConfigSystemRegistry(getTenantId());
            if (registry != null) {
                if (registry.resourceExists(resourcePath)) {
                    resource = registry.get(resourcePath);
                    if (resource.getProperty(trustedService) != null) {
                        resource.removeProperty(trustedService);
                    }
                    registry.put(resourcePath, resource);
                }
            }
        } catch (Exception e) {
            log.error("Error occured while removing trusted service for STS", e);
            throw new SecurityConfigException("Error occured while adding trusted service for STS",
                    e);
        }
    }
    
    private int getTenantId(){
    	int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
    	return tenantId;
    }

}
