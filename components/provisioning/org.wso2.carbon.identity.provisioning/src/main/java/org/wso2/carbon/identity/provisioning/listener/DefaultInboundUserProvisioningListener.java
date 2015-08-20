/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.provisioning.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.ProvisioningServiceProviderType;
import org.wso2.carbon.identity.application.common.model.ThreadLocalProvisioningServiceProvider;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationManagementUtil;
import org.wso2.carbon.identity.application.mgt.ApplicationConstants;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.core.AbstractIdentityUserOperationEventListener;
import org.wso2.carbon.identity.core.util.IdentityCoreConstants;
import org.wso2.carbon.identity.provisioning.IdentityProvisioningConstants;
import org.wso2.carbon.identity.provisioning.IdentityProvisioningException;
import org.wso2.carbon.identity.provisioning.OutboundProvisioningManager;
import org.wso2.carbon.identity.provisioning.ProvisioningEntity;
import org.wso2.carbon.identity.provisioning.ProvisioningEntityType;
import org.wso2.carbon.identity.provisioning.ProvisioningOperation;
import org.wso2.carbon.user.api.Permission;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.claim.Claim;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultInboundUserProvisioningListener extends AbstractIdentityUserOperationEventListener {

    public static final String WSO2_CARBON_DIALECT = "http://wso2.org/claims";

    private static final Log log = LogFactory.getLog(DefaultInboundUserProvisioningListener.class);

    public DefaultInboundUserProvisioningListener() throws IdentityProvisioningException {

    }

    @Override
    public int getExecutionOrderId() {
        int orderId = getOrderId(DefaultInboundUserProvisioningListener.class.getName());
        if (orderId != IdentityCoreConstants.EVENT_LISTENER_ORDER_ID) {
            return orderId;
        }
        return 30;
    }

    @Override
    /**
     *
     */
    public boolean doPreAddUser(String userName, Object credential, String[] roleList,
                                Map<String, String> inboundAttributes, String profile, UserStoreManager userStoreManager)
            throws UserStoreException {
        if (!isEnable(this.getClass().getName())) {
            return true;
        }

        try {
            Map<ClaimMapping, List<String>> outboundAttributes = new HashMap<>();

            if (credential != null) {
                outboundAttributes.put(ClaimMapping.build(
                                IdentityProvisioningConstants.PASSWORD_CLAIM_URI, null, null, false),
                        Arrays.asList(new String[]{((StringBuffer) credential).toString()}));
            }

            if (userName != null) {
                outboundAttributes.put(ClaimMapping.build(
                                IdentityProvisioningConstants.USERNAME_CLAIM_URI, null, null, false),
                        Arrays.asList(new String[]{userName}));
            }

            if (roleList != null) {
                outboundAttributes.put(ClaimMapping.build(
                        IdentityProvisioningConstants.GROUP_CLAIM_URI, null, null, false), Arrays
                        .asList(roleList));
            }

            String domainName = UserCoreUtil.getDomainName(userStoreManager.getRealmConfiguration());
            if (log.isDebugEnabled()) {
                log.debug("Adding domain name : " + domainName + " to user : " + userName);
            }
            String domainAwareName = UserCoreUtil.addDomainToName(userName, domainName);

            ProvisioningEntity provisioningEntity = new ProvisioningEntity(
                    ProvisioningEntityType.USER, domainAwareName, ProvisioningOperation.POST,
                    outboundAttributes);

            // set the in-bound attribute list.in this particular case this is in the wso2.org claim
            // dialect.
            provisioningEntity.setInboundAttributes(inboundAttributes);

            String tenantDomainName = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();

            ThreadLocalProvisioningServiceProvider threadLocalServiceProvider;
            threadLocalServiceProvider = IdentityApplicationManagementUtil
                    .getThreadLocalProvisioningServiceProvider();

            if (threadLocalServiceProvider != null) {

                String serviceProvider = threadLocalServiceProvider.getServiceProviderName();
                tenantDomainName = threadLocalServiceProvider.getTenantDomain();
                if (threadLocalServiceProvider.getServiceProviderType() == ProvisioningServiceProviderType.OAUTH) {
                    try {
                        serviceProvider = ApplicationManagementService.getInstance()
                                .getServiceProviderNameByClientId(
                                        threadLocalServiceProvider.getServiceProviderName(),
                                        "oauth2", tenantDomainName);
                    } catch (IdentityApplicationManagementException e) {
                        log.error("Error while provisioning", e);
                        return true;
                    }
                }

                // call framework method to provision the user.
                OutboundProvisioningManager.getInstance().provision(provisioningEntity,
                        serviceProvider, threadLocalServiceProvider.getClaimDialect(),
                        tenantDomainName, threadLocalServiceProvider.isJustInTimeProvisioning());
            } else {
                // call framework method to provision the user.
                OutboundProvisioningManager.getInstance()
                        .provision(provisioningEntity, ApplicationConstants.LOCAL_SP,
                                IdentityProvisioningConstants.WSO2_CARBON_DIALECT, tenantDomainName, false);
            }

            return true;
        } finally {
            ThreadLocalProvisioningServiceProvider threadLocalSP = IdentityApplicationManagementUtil
                    .getThreadLocalProvisioningServiceProvider();
            //check bulk user flag before reset ThreadLocalProvisioningServiceProvider, reset does not happen in the
            // middle of bulk user add operation
            if (threadLocalSP != null && !threadLocalSP.isBulkUserAdd()) {
                IdentityApplicationManagementUtil.resetThreadLocalProvisioningServiceProvider();
            }
        }
    }

    @Override
    /**
     *
     */
    public boolean doPreSetUserClaimValues(String userName, Map<String, String> inboundAttributes,
                                           String profileName, UserStoreManager userStoreManager) throws UserStoreException {

        if (!isEnable(this.getClass().getName())) {
            return true;
        }

        try {
            Map<ClaimMapping, List<String>> outboundAttributes = new HashMap<>();

            if (userName != null) {
                outboundAttributes.put(ClaimMapping.build(
                                IdentityProvisioningConstants.USERNAME_CLAIM_URI, null, null, false),
                        Arrays.asList(new String[]{userName}));
            }

            String domainName = UserCoreUtil.getDomainName(userStoreManager.getRealmConfiguration());
            if (log.isDebugEnabled()) {
                log.debug("Adding domain name : " + domainName + " to user : " + userName);
            }
            String domainAwareName = UserCoreUtil.addDomainToName(userName, domainName);

            ProvisioningEntity provisioningEntity = new ProvisioningEntity(
                    ProvisioningEntityType.USER, domainAwareName, ProvisioningOperation.PUT,
                    outboundAttributes);

            // set the in-bound attribute list.
            provisioningEntity.setInboundAttributes(inboundAttributes);

            String tenantDomainName = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();

            ThreadLocalProvisioningServiceProvider threadLocalServiceProvider;
            threadLocalServiceProvider = IdentityApplicationManagementUtil
                    .getThreadLocalProvisioningServiceProvider();

            if (threadLocalServiceProvider != null) {

                String serviceProvider = threadLocalServiceProvider.getServiceProviderName();
                tenantDomainName = threadLocalServiceProvider.getTenantDomain();
                if (threadLocalServiceProvider.getServiceProviderType() == ProvisioningServiceProviderType.OAUTH) {
                    try {
                        serviceProvider = ApplicationManagementService.getInstance()
                                .getServiceProviderNameByClientId(
                                        threadLocalServiceProvider.getServiceProviderName(),
                                        "oauth2", tenantDomainName);
                    } catch (IdentityApplicationManagementException e) {
                        log.error("Error while provisioning", e);
                        return true;
                    }
                }

                // call framework method to provision the user.
                OutboundProvisioningManager.getInstance().provision(provisioningEntity,
                        serviceProvider, threadLocalServiceProvider.getClaimDialect(),
                        tenantDomainName, threadLocalServiceProvider.isJustInTimeProvisioning());
            } else {
                // call framework method to provision the user.
                OutboundProvisioningManager.getInstance()
                        .provision(provisioningEntity, ApplicationConstants.LOCAL_SP,
                                IdentityProvisioningConstants.WSO2_CARBON_DIALECT, tenantDomainName, false);
            }

            return true;
        } finally {
            IdentityApplicationManagementUtil.resetThreadLocalProvisioningServiceProvider();
        }
    }

    @Override
    /**
     *
     */
    public boolean doPreDeleteUser(String userName, UserStoreManager userStoreManager)
            throws UserStoreException {
        if (!isEnable(this.getClass().getName())) {
            return true;
        }

        try {
            Map<ClaimMapping, List<String>> outboundAttributes = new HashMap<>();

            outboundAttributes.put(ClaimMapping.build(
                    IdentityProvisioningConstants.USERNAME_CLAIM_URI, null, null, false), Arrays
                    .asList(new String[]{userName}));

            String domainName = UserCoreUtil.getDomainName(userStoreManager.getRealmConfiguration());
            if (log.isDebugEnabled()) {
                log.debug("Adding domain name : " + domainName + " to user : " + userName);
            }
            String domainAwareName = UserCoreUtil.addDomainToName(userName, domainName);

            ProvisioningEntity provisioningEntity = new ProvisioningEntity(
                    ProvisioningEntityType.USER, domainAwareName, ProvisioningOperation.DELETE,
                    outboundAttributes);

            String tenantDomainName = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();

            ThreadLocalProvisioningServiceProvider threadLocalServiceProvider;
            threadLocalServiceProvider = IdentityApplicationManagementUtil
                    .getThreadLocalProvisioningServiceProvider();

            if (threadLocalServiceProvider != null) {
                String serviceProvider = threadLocalServiceProvider.getServiceProviderName();
                tenantDomainName = threadLocalServiceProvider.getTenantDomain();
                if (threadLocalServiceProvider.getServiceProviderType() == ProvisioningServiceProviderType.OAUTH) {
                    try {
                        serviceProvider = ApplicationManagementService.getInstance()
                                .getServiceProviderNameByClientId(
                                        threadLocalServiceProvider.getServiceProviderName(),
                                        "oauth2", tenantDomainName);
                    } catch (IdentityApplicationManagementException e) {
                        log.error("Error while provisioning", e);
                        return true;
                    }
                }

                // call framework method to provision the user.
                OutboundProvisioningManager.getInstance().provision(provisioningEntity,
                        serviceProvider, threadLocalServiceProvider.getClaimDialect(),
                        tenantDomainName, threadLocalServiceProvider.isJustInTimeProvisioning());
            } else {
                OutboundProvisioningManager.getInstance()
                        .provision(provisioningEntity, ApplicationConstants.LOCAL_SP,
                                IdentityProvisioningConstants.WSO2_CARBON_DIALECT, tenantDomainName, false);
            }

            return true;
        } finally {
            IdentityApplicationManagementUtil.resetThreadLocalProvisioningServiceProvider();
        }
    }

    @Override
    /**
     *
     */
    public boolean doPostUpdateUserListOfRole(String roleName, String[] deletedUsers,
                                              String[] newUsers, UserStoreManager userStoreManager) throws UserStoreException {

        if (!isEnable(this.getClass().getName())) {
            return true;
        }

        try {
            String[] userList = userStoreManager.getUserListOfRole(roleName);

            Map<ClaimMapping, List<String>> outboundAttributes = new HashMap<>();

            outboundAttributes.put(ClaimMapping.build(
                    IdentityProvisioningConstants.GROUP_CLAIM_URI, null, null, false), Arrays
                    .asList(new String[]{roleName}));

            outboundAttributes.put(ClaimMapping.build(IdentityProvisioningConstants.USER_CLAIM_URI,
                    null, null, false), Arrays.asList(userList));

            outboundAttributes.put(ClaimMapping.build(
                    IdentityProvisioningConstants.NEW_USER_CLAIM_URI, null, null, false), Arrays
                    .asList(newUsers));

            outboundAttributes.put(ClaimMapping.build(
                            IdentityProvisioningConstants.DELETED_USER_CLAIM_URI, null, null, false),
                    Arrays.asList(deletedUsers));

            String domainName = UserCoreUtil.getDomainName(userStoreManager.getRealmConfiguration());
            if (log.isDebugEnabled()) {
                log.debug("Adding domain name : " + domainName + " to role : " + roleName);
            }
            String domainAwareName = UserCoreUtil.addDomainToName(roleName, domainName);

            ProvisioningEntity provisioningEntity = new ProvisioningEntity(
                    ProvisioningEntityType.GROUP, domainAwareName, ProvisioningOperation.PUT,
                    outboundAttributes);

            String tenantDomainName = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();

            ThreadLocalProvisioningServiceProvider threadLocalServiceProvider;
            threadLocalServiceProvider = IdentityApplicationManagementUtil
                    .getThreadLocalProvisioningServiceProvider();

            if (threadLocalServiceProvider != null) {
                String serviceProvider = threadLocalServiceProvider.getServiceProviderName();
                tenantDomainName = threadLocalServiceProvider.getTenantDomain();
                if (threadLocalServiceProvider.getServiceProviderType() == ProvisioningServiceProviderType.OAUTH) {
                    try {
                        serviceProvider = ApplicationManagementService.getInstance()
                                .getServiceProviderNameByClientId(
                                        threadLocalServiceProvider.getServiceProviderName(),
                                        "oauth2", tenantDomainName);
                    } catch (IdentityApplicationManagementException e) {
                        log.error("Error while provisioning", e);
                        return true;
                    }
                }

                // call framework method to provision the user.
                OutboundProvisioningManager.getInstance().provision(provisioningEntity,
                        serviceProvider, threadLocalServiceProvider.getClaimDialect(),
                        tenantDomainName, threadLocalServiceProvider.isJustInTimeProvisioning());
            } else {
                // call framework method to provision the group.
                OutboundProvisioningManager.getInstance()
                        .provision(provisioningEntity, ApplicationConstants.LOCAL_SP,
                                IdentityProvisioningConstants.WSO2_CARBON_DIALECT, tenantDomainName, false);
            }

            return true;
        } finally {
            IdentityApplicationManagementUtil.resetThreadLocalProvisioningServiceProvider();
        }
    }

    @Override
    /**
     *
     */
    public boolean doPostUpdateRoleListOfUser(String userName, String[] deletedRoles,
                                              String[] newRoles, UserStoreManager userStoreManager) throws UserStoreException {
        if (!isEnable(this.getClass().getName())) {
            return true;
        }
        try {
            String[] roleList = userStoreManager.getRoleListOfUser(userName);
            Map<String, String> inboundAttributes = new HashMap<>();

            Map<ClaimMapping, List<String>> outboundAttributes = new HashMap<>();

            if (userName != null) {
                outboundAttributes.put(ClaimMapping.build(
                                IdentityProvisioningConstants.USERNAME_CLAIM_URI, null, null, false),
                        Arrays.asList(new String[]{userName}));
            }

            if (roleList != null && roleList.length > 0) {
                outboundAttributes.put(ClaimMapping.build(
                        IdentityProvisioningConstants.GROUP_CLAIM_URI, null, null, false), Arrays
                        .asList(roleList));
            }

            if (newRoles != null && roleList.length > 0) {
                outboundAttributes.put(ClaimMapping.build(
                                IdentityProvisioningConstants.NEW_GROUP_CLAIM_URI, null, null, false),
                        Arrays.asList(newRoles));
            }

            if (deletedRoles != null && deletedRoles.length > 0) {
                outboundAttributes.put(ClaimMapping.build(
                                IdentityProvisioningConstants.DELETED_GROUP_CLAIM_URI, null, null, false),
                        Arrays.asList(deletedRoles));
            }

            String domainName = UserCoreUtil.getDomainName(userStoreManager.getRealmConfiguration());
            if (log.isDebugEnabled()) {
                log.debug("Adding domain name : " + domainName + " to user : " + userName);
            }
            String domainAwareName = UserCoreUtil.addDomainToName(userName, domainName);

            ProvisioningEntity provisioningEntity = new ProvisioningEntity(
                    ProvisioningEntityType.USER, domainAwareName, ProvisioningOperation.PUT,
                    outboundAttributes);

            Claim[] claimArray = userStoreManager.getUserClaimValues(userName, null);
            if (claimArray != null) {
                for (Claim claim : claimArray) {
                    inboundAttributes.put(claim.getClaimUri(), claim.getValue());
                }
            }

            provisioningEntity.setInboundAttributes(inboundAttributes);

            String tenantDomainName = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();

            ThreadLocalProvisioningServiceProvider threadLocalServiceProvider;
            threadLocalServiceProvider = IdentityApplicationManagementUtil
                    .getThreadLocalProvisioningServiceProvider();

            if (threadLocalServiceProvider != null) {
                String serviceProvider = threadLocalServiceProvider.getServiceProviderName();
                tenantDomainName = threadLocalServiceProvider.getTenantDomain();
                if (threadLocalServiceProvider.getServiceProviderType() == ProvisioningServiceProviderType.OAUTH) {
                    try {
                        serviceProvider = ApplicationManagementService.getInstance()
                                .getServiceProviderNameByClientId(
                                        threadLocalServiceProvider.getServiceProviderName(),
                                        "oauth2", tenantDomainName);
                    } catch (IdentityApplicationManagementException e) {
                        log.error("Error while provisioning", e);
                        return true;
                    }
                }

                // call framework method to provision the user.
                OutboundProvisioningManager.getInstance().provision(provisioningEntity,
                        serviceProvider, threadLocalServiceProvider.getClaimDialect(),
                        tenantDomainName, threadLocalServiceProvider.isJustInTimeProvisioning());
            } else {
                // call framework method to provision the user.
                OutboundProvisioningManager.getInstance()
                        .provision(provisioningEntity, ApplicationConstants.LOCAL_SP,
                                IdentityProvisioningConstants.WSO2_CARBON_DIALECT, tenantDomainName, false);
            }

            return true;
        } finally {
            IdentityApplicationManagementUtil.resetThreadLocalProvisioningServiceProvider();
        }
    }

    @Override
    /**
     *
     */
    public boolean doPreAddRole(String roleName, String[] userList, Permission[] permissions,
                                UserStoreManager userStoreManager) throws UserStoreException {

        if (!isEnable(this.getClass().getName())) {
            return true;
        }
        try {
            Map<ClaimMapping, List<String>> outboundAttributes = new HashMap<>();

            if (roleName != null) {
                outboundAttributes.put(ClaimMapping.build(
                        IdentityProvisioningConstants.GROUP_CLAIM_URI, null, null, false), Arrays
                        .asList(new String[]{roleName}));
            }

            if (userList != null && userList.length > 0) {
                outboundAttributes.put(ClaimMapping.build(
                        IdentityProvisioningConstants.USER_CLAIM_URI, null, null, false), Arrays
                        .asList(userList));
            }

            String domainName = UserCoreUtil.getDomainName(userStoreManager.getRealmConfiguration());
            if (log.isDebugEnabled()) {
                log.debug("Adding domain name : " + domainName + " to user : " + roleName);
            }
            String domainAwareName = UserCoreUtil.addDomainToName(roleName, domainName);

            ProvisioningEntity provisioningEntity = new ProvisioningEntity(
                    ProvisioningEntityType.GROUP, domainAwareName, ProvisioningOperation.POST,
                    outboundAttributes);

            String tenantDomainName = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();

            ThreadLocalProvisioningServiceProvider threadLocalServiceProvider;
            threadLocalServiceProvider = IdentityApplicationManagementUtil
                    .getThreadLocalProvisioningServiceProvider();

            if (threadLocalServiceProvider != null) {
                String serviceProvider = threadLocalServiceProvider.getServiceProviderName();
                tenantDomainName = threadLocalServiceProvider.getTenantDomain();
                if (threadLocalServiceProvider.getServiceProviderType() == ProvisioningServiceProviderType.OAUTH) {
                    try {
                        serviceProvider = ApplicationManagementService.getInstance()
                                .getServiceProviderNameByClientId(
                                        threadLocalServiceProvider.getServiceProviderName(),
                                        "oauth2", tenantDomainName);
                    } catch (IdentityApplicationManagementException e) {
                        log.error("Error while provisioning", e);
                        return true;
                    }
                }

                // call framework method to provision the user.
                OutboundProvisioningManager.getInstance().provision(provisioningEntity,
                        serviceProvider, threadLocalServiceProvider.getClaimDialect(),
                        tenantDomainName, threadLocalServiceProvider.isJustInTimeProvisioning());
            } else {
                // call framework method to provision the group.
                OutboundProvisioningManager.getInstance()
                        .provision(provisioningEntity, ApplicationConstants.LOCAL_SP,
                                IdentityProvisioningConstants.WSO2_CARBON_DIALECT, tenantDomainName, false);
            }

            return true;
        } finally {
            IdentityApplicationManagementUtil.resetThreadLocalProvisioningServiceProvider();
        }
    }

    @Override
    /**
     *
     */
    public boolean doPreDeleteRole(String roleName, UserStoreManager userStoreManager)
            throws UserStoreException {

        if (!isEnable(this.getClass().getName())) {
            return true;
        }

        try {

            Map<ClaimMapping, List<String>> outboundAttributes = new HashMap<>();

            if (roleName != null) {
                outboundAttributes.put(ClaimMapping.build(
                        IdentityProvisioningConstants.GROUP_CLAIM_URI, null, null, false), Arrays
                        .asList(new String[]{roleName}));
            }

            String domainName = UserCoreUtil.getDomainName(userStoreManager.getRealmConfiguration());
            if (log.isDebugEnabled()) {
                log.debug("Adding domain name : " + domainName + " to user : " + roleName);
            }
            String domainAwareName = UserCoreUtil.addDomainToName(roleName, domainName);

            ProvisioningEntity provisioningEntity = new ProvisioningEntity(
                    ProvisioningEntityType.GROUP, domainAwareName, ProvisioningOperation.DELETE,
                    outboundAttributes);

            String tenantDomainName = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();

            ThreadLocalProvisioningServiceProvider threadLocalServiceProvider;
            threadLocalServiceProvider = IdentityApplicationManagementUtil
                    .getThreadLocalProvisioningServiceProvider();

            if (threadLocalServiceProvider != null) {
                String serviceProvider = threadLocalServiceProvider.getServiceProviderName();
                tenantDomainName = threadLocalServiceProvider.getTenantDomain();
                if (threadLocalServiceProvider.getServiceProviderType() == ProvisioningServiceProviderType.OAUTH) {
                    try {
                        serviceProvider = ApplicationManagementService.getInstance()
                                .getServiceProviderNameByClientId(
                                        threadLocalServiceProvider.getServiceProviderName(),
                                        "oauth2", tenantDomainName);
                    } catch (IdentityApplicationManagementException e) {
                        log.error("Error while provisioning", e);
                        return true;
                    }
                }

                // call framework method to provision the user.
                OutboundProvisioningManager.getInstance().provision(provisioningEntity,
                        serviceProvider, threadLocalServiceProvider.getClaimDialect(),
                        tenantDomainName, threadLocalServiceProvider.isJustInTimeProvisioning());
            } else {
                // call framework method to provision the group.
                OutboundProvisioningManager.getInstance()
                        .provision(provisioningEntity, ApplicationConstants.LOCAL_SP,
                                IdentityProvisioningConstants.WSO2_CARBON_DIALECT, tenantDomainName, false);
            }

            return true;
        } finally {
            IdentityApplicationManagementUtil.resetThreadLocalProvisioningServiceProvider();
        }
    }
}
