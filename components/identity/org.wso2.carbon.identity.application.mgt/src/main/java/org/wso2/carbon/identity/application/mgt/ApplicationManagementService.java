/*
 *Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.application.common.model.ApplicationBasicInfo;
import org.wso2.carbon.identity.application.common.model.ApplicationPermission;
import org.wso2.carbon.identity.application.common.model.FederatedIdentityProvider;
import org.wso2.carbon.identity.application.common.model.LocalAuthenticator;
import org.wso2.carbon.identity.application.common.model.PermissionsAndRoleConfiguration;
import org.wso2.carbon.identity.application.common.model.RequestPathAuthenticator;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.mgt.dao.ApplicationDAO;
import org.wso2.carbon.identity.application.mgt.dao.IdentityProviderDAO;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.user.api.ClaimMapping;

public class ApplicationManagementService {

    private static Log log = LogFactory.getLog(ApplicationManagementService.class);
    private static ApplicationManagementService appMgtService = new ApplicationManagementService();

    /**
     *     
     * @return
     */
    public static ApplicationManagementService getInstance() {
    	return appMgtService;
    }
    
    /**
     * Creates a service provider with basic information.First we need to create a role with the
     * application name. Only the users in this role will be able to edit/update the application.The
     * user will assigned to the created role.Internal roles used.
     * 
     * @param serviceProvider
     * @return
     * @throws IdentityException
     */
    public int createApplication(ServiceProvider serviceProvider) throws IdentityException {
        try {
            // first we need to create a role with the application name.
            // only the users in this role will be able to edit/update the application.
            ApplicationMgtUtil.createAppRole(serviceProvider.getApplicationName());
            ApplicationDAO appDAO = ApplicationMgtSystemConfig.getInstance().getApplicationDAO();
            // create the service provider.
            return appDAO.createApplication(serviceProvider);
        } catch (Exception e) {
            log.error(
                    "Error occurred while creating the application, "
                            + serviceProvider.getApplicationName(), e);
            throw new IdentityException("Error occurred while creating the application", e);

        }
    }

    /**
     * 
     * @param applicationName
     * @return
     * @throws IdentityException
     */
    public ServiceProvider getApplication(String applicationName) throws IdentityException {

        try {
            if (!ApplicationConstants.LOCAL_SP.equals(applicationName)
                    && !ApplicationMgtUtil.isUserAuthorized(applicationName)) {
                log.warn("Illegale Access! User " + CarbonContext.getCurrentContext().getUsername()
                        + " does not have access to the application " + applicationName);
                throw new IdentityException("User not authorized");
            }

            ApplicationDAO appDAO = ApplicationMgtSystemConfig.getInstance().getApplicationDAO();
            ServiceProvider serviceProvider = appDAO.getApplication(applicationName);
            List<ApplicationPermission> permissionList = ApplicationMgtUtil
                    .loadPermissions(applicationName);
            if (permissionList != null) {
                PermissionsAndRoleConfiguration permissionAndRoleConfig = null;
                if (serviceProvider.getPermissionAndRoleConfiguration() == null) {
                    permissionAndRoleConfig = new PermissionsAndRoleConfiguration();
                } else {
                    permissionAndRoleConfig = serviceProvider.getPermissionAndRoleConfiguration();
                }
                permissionAndRoleConfig.setPermissions(permissionList
                        .toArray(new ApplicationPermission[permissionList.size()]));
                serviceProvider.setPermissionAndRoleConfiguration(permissionAndRoleConfig);
            }
            return serviceProvider;
        } catch (Exception e) {
            log.error("Error occurred while retreiving the application, " + applicationName, e);
            throw new IdentityException("Error occurred while retreiving the application", e);
        }
    }

    /**
     * 
     * @return
     * @throws IdentityException
     */
    public ApplicationBasicInfo[] getAllApplicationBasicInfo() throws IdentityException {
        try {
            ApplicationDAO appDAO = ApplicationMgtSystemConfig.getInstance().getApplicationDAO();
            return appDAO.getAllApplicationBasicInfo();
        } catch (Exception e) {
            log.error("Error occurred while retreiving the all applications", e);
            throw new IdentityException("Error occurred while retreiving the all applications", e);
        }
    }

    /**
     * 
     * @param serviceProvider
     * @throws IdentityException
     */
    public void updateApplication(ServiceProvider serviceProvider) throws IdentityException {
        try {

            // check whether use is authorized to update the application.
            if (!ApplicationConstants.LOCAL_SP.equals(serviceProvider.getApplicationName())
                    && !ApplicationMgtUtil.isUserAuthorized(serviceProvider.getApplicationName(),
                            serviceProvider.getApplicationID())) {
                log.warn("Illegale Access! User " + CarbonContext.getCurrentContext().getUsername()
                        + " does not have access to the application "
                        + serviceProvider.getApplicationName());
                throw new IdentityException("User not authorized");
            }

            ApplicationDAO appDAO = ApplicationMgtSystemConfig.getInstance().getApplicationDAO();
            appDAO.updateApplication(serviceProvider);
            ApplicationPermission[] permissions = serviceProvider
                    .getPermissionAndRoleConfiguration().getPermissions();
            if (permissions != null && permissions.length > 0) {
                ApplicationMgtUtil.updatePermissions(serviceProvider.getApplicationName(),
                        permissions);
            }
        } catch (Exception e) {
            log.error("Error occurred while updating the application", e);
            throw new IdentityException("Error occurred while updating the application", e);
        }
    }

    /**
     * 
     * @param applicationName
     * @throws IdentityException
     */
    public void deleteApplication(String applicationName) throws IdentityException {
        try {
            if (!ApplicationMgtUtil.isUserAuthorized(applicationName)) {
                log.warn("Illegale Access! User " + CarbonContext.getCurrentContext().getUsername()
                        + " does not have access to the application " + applicationName);
                throw new IdentityException("User not authorized");
            }

            ApplicationDAO appDAO = ApplicationMgtSystemConfig.getInstance().getApplicationDAO();
            appDAO.deleteApplication(applicationName);

            ApplicationMgtUtil.deleteAppRole(applicationName);
            ApplicationMgtUtil.deletePermissions(applicationName);
        } catch (Exception e) {
            log.error("Error occurred while deleting the application", e);
            throw new IdentityException("Error occurred while deleting the application", e);
        }
    }

    /**
     * 
     * @param federatedIdPName
     * @return
     * @throws IdentityException
     */
    public FederatedIdentityProvider getFederatedIdentityProvider(String federatedIdPName)
            throws IdentityException {
        try {
            IdentityProviderDAO idpdao = ApplicationMgtSystemConfig.getInstance()
                    .getIdentityProviderDAO();
            return idpdao.getFederatedIdentityProvider(federatedIdPName);
        } catch (Exception e) {
            log.error("Error occurred while deleting the application", e);
            throw new IdentityException("Error occurred while deleting the application", e);
        }
    }

    /**
     * 
     * @return
     * @throws IdentityException
     */
    public FederatedIdentityProvider[] getAllFederatedIdentityProviders() throws IdentityException {
        try {
            IdentityProviderDAO idpdao = ApplicationMgtSystemConfig.getInstance()
                    .getIdentityProviderDAO();
            List<FederatedIdentityProvider> fedIdpList = idpdao.getAllFederatedIdentityProviders();
            if (fedIdpList != null) {
                return fedIdpList.toArray(new FederatedIdentityProvider[fedIdpList.size()]);
            }
            return null;
        } catch (Exception e) {
            log.error("Error occurred while deleting the application", e);
            throw new IdentityException("Error occurred while deleting the application", e);
        }
    }

    /**
     * 
     * @return
     * @throws IdentityException
     */
    public LocalAuthenticator[] getAllLocalAuthenticators() throws IdentityException {
        try {
            IdentityProviderDAO idpdao = ApplicationMgtSystemConfig.getInstance()
                    .getIdentityProviderDAO();
            List<LocalAuthenticator> localAuthenticators = idpdao.getAllLocalAuthenticators();
            if (localAuthenticators != null) {
                return localAuthenticators.toArray(new LocalAuthenticator[localAuthenticators
                        .size()]);
            }
            return null;
        } catch (Exception e) {
            log.error("Error occurred while deleting the application", e);
            throw new IdentityException("Error occurred while deleting the application", e);
        }
    }

    /**
     * 
     * @return
     * @throws IdentityException
     */
    public RequestPathAuthenticator[] getAllRequestPathAuthenticators() throws IdentityException {
        try {
            IdentityProviderDAO idpdao = ApplicationMgtSystemConfig.getInstance()
                    .getIdentityProviderDAO();
            List<RequestPathAuthenticator> reqPathAuthenticators = idpdao
                    .getAllRequestPathAuthenticators();
            if (reqPathAuthenticators != null) {
                return reqPathAuthenticators
                        .toArray(new RequestPathAuthenticator[reqPathAuthenticators.size()]);
            }
            return null;
        } catch (Exception e) {
            log.error("Error occurred while deleting the application", e);
            throw new IdentityException("Error occurred while deleting the application", e);
        }
    }

    /**
     * 
     * @return
     * @throws IdentityException
     */
    public String[] getAllLocalClaimUris() throws IdentityException {
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
            throw new IdentityException("Error while reading system claims");
        }
    }

}
