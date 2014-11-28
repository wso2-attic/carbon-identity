/*
 * Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.application.mgt;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.rahas.impl.SAMLTokenIssuerConfig;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.core.RegistryResources;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.*;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants;
import org.wso2.carbon.identity.application.mgt.cache.IdentityServiceProviderCache;
import org.wso2.carbon.identity.application.mgt.cache.IdentityServiceProviderCacheKey;
import org.wso2.carbon.identity.application.mgt.dao.ApplicationDAO;
import org.wso2.carbon.identity.application.mgt.dao.IdentityProviderDAO;
import org.wso2.carbon.identity.application.mgt.dao.OAuthApplicationDAO;
import org.wso2.carbon.identity.application.mgt.dao.SAMLApplicationDAO;
import org.wso2.carbon.identity.application.mgt.internal.ApplicationManagementServiceComponentHolder;
import org.wso2.carbon.identity.application.mgt.internal.ApplicationMgtListenerServiceComponent;
import org.wso2.carbon.identity.application.mgt.listener.ApplicationMgtListener;
import org.wso2.carbon.registry.api.RegistryException;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.security.SecurityConfigException;
import org.wso2.carbon.security.config.SecurityServiceAdmin;
import org.wso2.carbon.user.api.ClaimMapping;
import org.wso2.carbon.utils.ServerConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * Application management service implementation. Which can use as an osgi
 * service and reuse this in admin service
 * 
 */
public class ApplicationManagementServiceImpl extends ApplicationManagementService {

	private static Log log = LogFactory.getLog(ApplicationManagementServiceImpl.class);
	private static volatile ApplicationManagementServiceImpl appMgtService;

	/**
	 * Private constructor which not allow to create object from outside
	 */
	private ApplicationManagementServiceImpl() {

	}

	/**
	 * Get ApplicationManagementServiceImpl instance
	 * 
	 * @return ApplicationManagementServiceImpl
	 */
	public static ApplicationManagementServiceImpl getInstance() {
		if (appMgtService == null) {
			synchronized (ApplicationManagementServiceImpl.class) {
				if (appMgtService == null) {
					appMgtService = new ApplicationManagementServiceImpl();
				}
			}
		}
		return appMgtService;
	}

	/**
	 * Creates a service provider with basic information.First we need to create
	 * a role with the
	 * application name. Only the users in this role will be able to edit/update
	 * the application.The
	 * user will assigned to the created role.Internal roles used.
	 * 
	 * @param serviceProvider
	 *            Service Provider
	 * @return
	 * @throws org.wso2.carbon.identity.application.common.IdentityApplicationManagementException
	 */
	public int createApplication(ServiceProvider serviceProvider)
	                                                             throws IdentityApplicationManagementException {
		try {

			// invoking the listeners
			List<ApplicationMgtListener> listeners =
			                                         ApplicationMgtListenerServiceComponent.getListners();
			for (ApplicationMgtListener listener : listeners) {
				listener.createApplication(serviceProvider);
			}

			// first we need to create a role with the application name.
			// only the users in this role will be able to edit/update the
			// application.
			String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
			ApplicationMgtUtil.createAppRole(serviceProvider.getApplicationName());
			ApplicationDAO appDAO = ApplicationMgtSystemConfig.getInstance().getApplicationDAO();
			ApplicationMgtUtil.storePermission(serviceProvider.getApplicationName(),
                    serviceProvider.getPermissionAndRoleConfig());
			return appDAO.createApplication(serviceProvider, tenantDomain);
		} catch (Exception e) {
			String error =
			               "Error occurred while creating the application, " +
			                       serviceProvider.getApplicationName();
			log.error(error, e);
			throw new IdentityApplicationManagementException(error, e);

		}
	}

	/**
	 * Get All Application Basic Information
	 *
	 * @param applicationName
	 *            Application name
	 * @return Service provider
	 * @throws org.wso2.carbon.identity.application.common.IdentityApplicationManagementException
	 */
	public ServiceProvider getApplication(String applicationName)
	                                                             throws IdentityApplicationManagementException {

		try {
			String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();

			ApplicationDAO appDAO = ApplicationMgtSystemConfig.getInstance().getApplicationDAO();
			ServiceProvider serviceProvider = appDAO.getApplication(applicationName, tenantDomain);
			List<ApplicationPermission> permissionList =
			                                             ApplicationMgtUtil.loadPermissions(applicationName);
			if (permissionList != null) {
				PermissionsAndRoleConfig permissionAndRoleConfig;
				if (serviceProvider.getPermissionAndRoleConfig() == null) {
					permissionAndRoleConfig = new PermissionsAndRoleConfig();
				} else {
					permissionAndRoleConfig = serviceProvider.getPermissionAndRoleConfig();
				}
				permissionAndRoleConfig.setPermissions(permissionList.toArray(new ApplicationPermission[permissionList.size()]));
				serviceProvider.setPermissionAndRoleConfig(permissionAndRoleConfig);
			}
			return serviceProvider;
		} catch (Exception e) {
			String error = "Error occurred while retrieving the application, " + applicationName;
			log.error(error, e);
			throw new IdentityApplicationManagementException(error, e);
		}
	}

	/**
	 * Get All Application Basic Information
	 *
	 * @return Application basic information array
	 * @throws org.wso2.carbon.identity.application.common.IdentityApplicationManagementException
	 */
	public ApplicationBasicInfo[] getAllApplicationBasicInfo()
	                                                          throws IdentityApplicationManagementException {
		try {
			ApplicationDAO appDAO = ApplicationMgtSystemConfig.getInstance().getApplicationDAO();
			return appDAO.getAllApplicationBasicInfo();
		} catch (Exception e) {
			String error = "Error occurred while retrieving the all applications";
			log.error(error, e);
			throw new IdentityApplicationManagementException(error, e);
		}
	}

	/**
	 * Update application
	 *
	 * @param serviceProvider
	 *            Service providers
	 * @throws org.wso2.carbon.identity.application.common.IdentityApplicationManagementException
	 */
	public void updateApplication(ServiceProvider serviceProvider)
	                                                              throws IdentityApplicationManagementException {
		try {

			String tenantDomainName = null;
			int tenantId = MultitenantConstants.SUPER_TENANT_ID;

			if (CarbonContext.getThreadLocalCarbonContext() != null) {
				tenantDomainName = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
				tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
			}

			try {

				PrivilegedCarbonContext.startTenantFlow();
				PrivilegedCarbonContext carbonContext =
				                                        PrivilegedCarbonContext.getThreadLocalCarbonContext();
				carbonContext.setTenantId(MultitenantConstants.SUPER_TENANT_ID);
				carbonContext.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);

				IdentityServiceProviderCacheKey cacheKey =
				                                           new IdentityServiceProviderCacheKey(
				                                                                               tenantDomainName,
				                                                                               serviceProvider.getApplicationName());
				IdentityServiceProviderCache.getInstance().clearCacheEntry(cacheKey);

			} finally {
				PrivilegedCarbonContext.endTenantFlow();

				if (tenantDomainName != null) {
					PrivilegedCarbonContext.getThreadLocalCarbonContext()
					                       .setTenantDomain(tenantDomainName);
					PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenantId);
				}
			}

			// invoking the listeners
			List<ApplicationMgtListener> listeners =
			                                         ApplicationMgtListenerServiceComponent.getListners();
			for (ApplicationMgtListener listener : listeners) {
				listener.updateApplication(serviceProvider);
			}

			// check whether use is authorized to update the application.
			if (!ApplicationConstants.LOCAL_SP.equals(serviceProvider.getApplicationName()) &&
			    !ApplicationMgtUtil.isUserAuthorized(serviceProvider.getApplicationName(),
                        serviceProvider.getApplicationID())) {
				log.warn("Illegal Access! User " +
				         CarbonContext.getThreadLocalCarbonContext().getUsername() +
				         " does not have access to the application " +
				         serviceProvider.getApplicationName());
				throw new IdentityApplicationManagementException("User not authorized");
			}

			ApplicationDAO appDAO = ApplicationMgtSystemConfig.getInstance().getApplicationDAO();
			appDAO.updateApplication(serviceProvider);
			ApplicationPermission[] permissions =
			                                      serviceProvider.getPermissionAndRoleConfig()
			                                                     .getPermissions();
			if (permissions != null && permissions.length > 0) {
				ApplicationMgtUtil.updatePermissions(serviceProvider.getApplicationName(),
                        permissions);
			}
		} catch (Exception e) {
			String error = "Error occurred while updating the application";
			log.error(error, e);
			throw new IdentityApplicationManagementException(error, e);
		}
	}

	/**
	 * Delete application
	 *
	 * @param applicationName
	 *            Application name
	 * @throws org.wso2.carbon.identity.application.common.IdentityApplicationManagementException
	 */
	public void deleteApplication(String applicationName)
	                                                     throws IdentityApplicationManagementException {
		try {

			// invoking the listeners
			List<ApplicationMgtListener> listeners =
			                                         ApplicationMgtListenerServiceComponent.getListners();
			for (ApplicationMgtListener listener : listeners) {
				listener.deleteApplication(applicationName);
			}

			if (!ApplicationMgtUtil.isUserAuthorized(applicationName)) {
				log.warn("Illegal Access! User " +
				         CarbonContext.getThreadLocalCarbonContext().getUsername() +
				         " does not have access to the application " + applicationName);
				throw new IdentityApplicationManagementException("User not authorized");
			}

			ApplicationDAO appDAO = ApplicationMgtSystemConfig.getInstance().getApplicationDAO();
			ServiceProvider serviceProvider =
			                                  appDAO.getApplication(applicationName,
			                                                        CarbonContext.getThreadLocalCarbonContext()
			                                                                     .getTenantDomain());
			appDAO.deleteApplication(applicationName);

			ApplicationMgtUtil.deleteAppRole(applicationName);
			ApplicationMgtUtil.deletePermissions(applicationName);

			if (serviceProvider != null &&
			    serviceProvider.getInboundAuthenticationConfig() != null &&
			    serviceProvider.getInboundAuthenticationConfig()
			                   .getInboundAuthenticationRequestConfigs() != null) {

				InboundAuthenticationRequestConfig[] configs =
				                                               serviceProvider.getInboundAuthenticationConfig()
				                                                              .getInboundAuthenticationRequestConfigs();

				for (InboundAuthenticationRequestConfig config : configs) {

					if (IdentityApplicationConstants.Authenticator.SAML2SSO.NAME.equalsIgnoreCase(config.getInboundAuthType()) &&
					    config.getInboundAuthKey() != null) {
						SAMLApplicationDAO samlDAO =
						                             ApplicationMgtSystemConfig.getInstance()
						                                                       .getSAMLClientDAO();
						samlDAO.removeServiceProviderConfiguration(config.getInboundAuthKey());

					} else if (IdentityApplicationConstants.OAuth2.NAME.equalsIgnoreCase(config.getInboundAuthType()) &&
					           config.getInboundAuthKey() != null) {
						OAuthApplicationDAO oathDAO =
						                              ApplicationMgtSystemConfig.getInstance()
						                                                        .getOAuthOIDCClientDAO();
						oathDAO.removeOAuthApplication(config.getInboundAuthKey());

					} else if (IdentityApplicationConstants.Authenticator.WSTrust.NAME.equalsIgnoreCase(config.getInboundAuthType()) &&
					           config.getInboundAuthKey() != null) {
						try {
							AxisService stsService =
							                         getAxisConfig().getService(ServerConstants.STS_NAME);
							Parameter origParam =
							                      stsService.getParameter(SAMLTokenIssuerConfig.SAML_ISSUER_CONFIG.getLocalPart());
							if (origParam != null) {
								OMElement samlConfigElem =
								                           origParam.getParameterElement()
								                                    .getFirstChildWithName(SAMLTokenIssuerConfig.SAML_ISSUER_CONFIG);
								SAMLTokenIssuerConfig samlConfig =
								                                   new SAMLTokenIssuerConfig(
								                                                             samlConfigElem);
								samlConfig.getTrustedServices().remove(config.getInboundAuthKey());
								setSTSParameter(samlConfig);
								removeTrustedService(ServerConstants.STS_NAME,
								                     ServerConstants.STS_NAME,
								                     config.getInboundAuthKey());
							} else {
								throw new IdentityApplicationManagementException(
								                                                 "missing parameter : " +
								                                                         SAMLTokenIssuerConfig.SAML_ISSUER_CONFIG.getLocalPart());
							}
						} catch (Exception e) {
							String error = "Error while removing a trusted service";
							log.error(error, e);
							throw new IdentityApplicationManagementException(error, e);
						}
					}
				}
			}

		} catch (Exception e) {
			String error = "Error occurred while deleting the application";
			log.error(error, e);
			throw new IdentityApplicationManagementException(error, e);
		}
	}

	/**
	 * Get identity provider
	 *
	 * @param federatedIdPName
	 *            Identity provider name
	 * @return Identity provider
	 * @throws org.wso2.carbon.identity.application.common.IdentityApplicationManagementException
	 */
	public IdentityProvider getIdentityProvider(String federatedIdPName)
	                                                                    throws IdentityApplicationManagementException {
		try {
			IdentityProviderDAO idpdao =
			                             ApplicationMgtSystemConfig.getInstance()
			                                                       .getIdentityProviderDAO();
			return idpdao.getIdentityProvider(federatedIdPName);
		} catch (Exception e) {
			String error = "Error occurred while retrieving Identity Provider";
			log.error(error, e);
			throw new IdentityApplicationManagementException(error, e);
		}
	}

	/**
	 * Get all identity providers
	 *
	 * @return identity providers array
	 * @throws org.wso2.carbon.identity.application.common.IdentityApplicationManagementException
	 */
	public IdentityProvider[] getAllIdentityProviders()
	                                                   throws IdentityApplicationManagementException {
		try {
			IdentityProviderDAO idpdao =
			                             ApplicationMgtSystemConfig.getInstance()
			                                                       .getIdentityProviderDAO();
			List<IdentityProvider> fedIdpList = idpdao.getAllIdentityProviders();
			if (fedIdpList != null) {
				return fedIdpList.toArray(new IdentityProvider[fedIdpList.size()]);
			}
			return null;
		} catch (Exception e) {
			String error = "Error occurred while retrieving all Identity Providers";
			log.error(error, e);
			throw new IdentityApplicationManagementException(error, e);
		}
	}

	/**
	 * Get all local authenticators
	 *
	 * @return local authenticator config array
	 * @throws org.wso2.carbon.identity.application.common.IdentityApplicationManagementException
	 */
	public LocalAuthenticatorConfig[] getAllLocalAuthenticators()
	                                                             throws IdentityApplicationManagementException {
		try {
			IdentityProviderDAO idpdao =
			                             ApplicationMgtSystemConfig.getInstance()
			                                                       .getIdentityProviderDAO();
			List<LocalAuthenticatorConfig> localAuthenticators = idpdao.getAllLocalAuthenticators();
			if (localAuthenticators != null) {
				return localAuthenticators.toArray(new LocalAuthenticatorConfig[localAuthenticators.size()]);
			}
			return null;
		} catch (Exception e) {
			String error = "Error occurred while retrieving all Local Authenticators";
			log.error(error, e);
			throw new IdentityApplicationManagementException(error, e);
		}
	}

	/**
	 * Get all request authenticators
	 *
	 * @return request path authenticator config array
	 * @throws org.wso2.carbon.identity.application.common.IdentityApplicationManagementException
	 */
	public RequestPathAuthenticatorConfig[] getAllRequestPathAuthenticators()
	                                                                         throws IdentityApplicationManagementException {
		try {
			IdentityProviderDAO idpdao =
			                             ApplicationMgtSystemConfig.getInstance()
			                                                       .getIdentityProviderDAO();
			List<RequestPathAuthenticatorConfig> reqPathAuthenticators =
			                                                             idpdao.getAllRequestPathAuthenticators();
			if (reqPathAuthenticators != null) {
				return reqPathAuthenticators.toArray(new RequestPathAuthenticatorConfig[reqPathAuthenticators.size()]);
			}
			return null;
		} catch (Exception e) {
			String error = "Error occurred while retrieving all Request Path Authenticators";
			log.error(error, e);
			throw new IdentityApplicationManagementException(error, e);
		}
	}

	/**
	 * Get all claim uris
	 *
	 * @return Claim uri array
	 * @throws org.wso2.carbon.identity.application.common.IdentityApplicationManagementException
	 */
	public String[] getAllLocalClaimUris() throws IdentityApplicationManagementException {
		try {

			String claimDialect = ApplicationMgtSystemConfig.getInstance().getClaimDialect();
			ClaimMapping[] claimMappings =
			                               CarbonContext.getThreadLocalCarbonContext()
			                                            .getUserRealm().getClaimManager()
			                                            .getAllClaimMappings(claimDialect);
			List<String> claimUris = new ArrayList<String>();
			for (ClaimMapping claimMap : claimMappings) {
				claimUris.add(claimMap.getClaim().getClaimUri());
			}
			return claimUris.toArray(new String[claimUris.size()]);
		} catch (Exception e) {
			String error = "Error while reading system claims";
			log.error(error, e);
			throw new IdentityApplicationManagementException(error, e);
		}
	}

	/**
	 * Get application data for given client Id and type
	 *
	 * @param clientId
	 * @param type
	 * @return ServiceProvider
	 * @throws org.wso2.carbon.identity.application.common.IdentityApplicationManagementException
	 */
	public String getServiceProviderNameByClientId(String clientId, String type)
	                                                                            throws IdentityApplicationManagementException {

		try {
			String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();

			ApplicationDAO appDAO = ApplicationMgtSystemConfig.getInstance().getApplicationDAO();
			return appDAO.getServiceProviderNameByClientId(clientId, type, tenantDomain);

		} catch (Exception e) {
			String error =
			               "Error occurred while retrieving the service provider for client id :  " +
			                       clientId;
			log.error(error, e);
			throw new IdentityApplicationManagementException(error, e);
		}
	}

	/**
	 * Set STS parameters
	 *
	 * @param samlConfig
	 *            SAML config
	 * @throws org.apache.axis2.AxisFault
	 * @throws org.wso2.carbon.registry.api.RegistryException
	 */
	private void setSTSParameter(SAMLTokenIssuerConfig samlConfig) throws AxisFault,
            RegistryException {
		new SecurityServiceAdmin(getAxisConfig(), getConfigSystemRegistry()).setServiceParameterElement(ServerConstants.STS_NAME,
		                                                                                                samlConfig.getParameter());
	}

	/**
	 * Remove trusted service
	 *
	 * @param groupName
	 *            Group name
	 * @param serviceName
	 *            Service name
	 * @param trustedService
	 *            Trusted service name
	 * @throws org.wso2.carbon.security.SecurityConfigException
	 */
	private void removeTrustedService(String groupName, String serviceName, String trustedService)
	                                                                                              throws SecurityConfigException {
		Registry registry;
		String resourcePath;
		Resource resource;
		try {
			resourcePath =
			               RegistryResources.SERVICE_GROUPS + groupName +
			                       RegistryResources.SERVICES + serviceName + "/trustedServices";
			registry = getConfigSystemRegistry();
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
			String error = "Error occurred while removing trusted service for STS";
			log.error(error, e);
			throw new SecurityConfigException(error, e);
		}
	}

	/**
	 * Get axis config
	 * 
	 * @return axis configuration
	 */
	private AxisConfiguration getAxisConfig() {
		return ApplicationManagementServiceComponentHolder.getConfigContextService()
		                                                  .getServerConfigContext()
		                                                  .getAxisConfiguration();
	}

	/**
	 * Get config system registry
	 * 
	 * @return config system registry
	 * @throws org.wso2.carbon.registry.api.RegistryException
	 */
	private Registry getConfigSystemRegistry() throws RegistryException {
		return (Registry) ApplicationManagementServiceComponentHolder.getRegistryService()
		                                                             .getConfigSystemRegistry();
	}

}
