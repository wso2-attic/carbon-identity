/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.scim.provider.impl;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ProvisioningServiceProviderType;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.common.model.ThreadLocalProvisioningServiceProvider;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationManagementUtil;
import org.wso2.carbon.identity.application.mgt.ApplicationConstants;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.provisioning.IdentityProvisioningConstants;
import org.wso2.carbon.identity.provisioning.IdentityProvisioningException;
import org.wso2.carbon.identity.provisioning.OutboundProvisioningManager;
import org.wso2.carbon.identity.provisioning.ProvisioningEntity;
import org.wso2.carbon.identity.provisioning.ProvisioningEntityType;
import org.wso2.carbon.identity.provisioning.ProvisioningOperation;
import org.wso2.carbon.identity.provisioning.listener.DefaultInboundUserProvisioningListener;
import org.wso2.carbon.identity.scim.common.group.SCIMGroupHandler;
import org.wso2.carbon.identity.scim.common.utils.AttributeMapper;
import org.wso2.carbon.identity.scim.common.utils.IdentitySCIMException;
import org.wso2.carbon.identity.scim.common.utils.SCIMCommonConstants;
import org.wso2.carbon.identity.scim.common.utils.SCIMCommonUtils;
import org.wso2.carbon.user.api.ClaimMapping;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.claim.ClaimManager;
import org.wso2.carbon.user.core.util.UserCoreUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;
import org.wso2.charon.core.attributes.Attribute;
import org.wso2.charon.core.attributes.SimpleAttribute;
import org.wso2.charon.core.exceptions.CharonException;
import org.wso2.charon.core.exceptions.DuplicateResourceException;
import org.wso2.charon.core.exceptions.NotFoundException;
import org.wso2.charon.core.extensions.UserManager;
import org.wso2.charon.core.objects.AbstractSCIMObject;
import org.wso2.charon.core.objects.Group;
import org.wso2.charon.core.objects.SCIMObject;
import org.wso2.charon.core.objects.User;
import org.wso2.charon.core.schema.SCIMConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SCIMUserManager implements UserManager {
    public static final String USER_NAME_STRING = "userName";
    private static Log log = LogFactory.getLog(SCIMUserManager.class);
    private UserStoreManager carbonUM = null;
    private ClaimManager carbonClaimManager = null;
    private String consumerName;

    public SCIMUserManager(UserStoreManager carbonUserStoreManager, String userName,
                           ClaimManager claimManager) {
        carbonUM = carbonUserStoreManager;
        consumerName = userName;
        carbonClaimManager = claimManager;
    }

    @Override
    public User createUser(User user) throws CharonException, DuplicateResourceException {
        return createUser(user, false);
    }

    @Override
    public User createUser(User user, boolean isBulkUserAdd) throws CharonException, DuplicateResourceException {

        try {

            ThreadLocalProvisioningServiceProvider threadLocalSP = IdentityApplicationManagementUtil
                    .getThreadLocalProvisioningServiceProvider();
            //isBulkUserAdd is true indicates bulk user add
            if (isBulkUserAdd) {
                threadLocalSP.setBulkUserAdd(true);
            }

            ServiceProvider serviceProvider = null;
            if (threadLocalSP.getServiceProviderType() == ProvisioningServiceProviderType.OAUTH) {
                serviceProvider = ApplicationManagementService.getInstance()
                        .getServiceProviderByClientId(
                                threadLocalSP.getServiceProviderName(),
                                "oauth2", threadLocalSP.getTenantDomain());
            } else {
                serviceProvider = ApplicationManagementService.getInstance().getServiceProvider(
                        threadLocalSP.getServiceProviderName(), threadLocalSP.getTenantDomain());
            }

            String userStoreName = null;

            if (serviceProvider != null && serviceProvider.getInboundProvisioningConfig() != null) {
                userStoreName = serviceProvider.getInboundProvisioningConfig()
                        .getProvisioningUserStore();

            }

            StringBuilder userName = new StringBuilder();

            if (userStoreName != null && userStoreName.trim().length() > 0) {
                // if we have set a user store under provisioning configuration - we should only use
                // that.
                String currentUserName = user.getUserName();
                currentUserName = UserCoreUtil.removeDomainFromName(currentUserName);
                user.setUserName(userName.append(userStoreName)
                        .append(CarbonConstants.DOMAIN_SEPARATOR).append(currentUserName)
                        .toString());
            }

        } catch (IdentityApplicationManagementException e) {
            throw new CharonException("Error retrieving User Store name. ", e);
        }

        try {
            //TODO: Start tenant flow at the scim authentication point
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            carbonContext.setTenantDomain(MultitenantUtils.getTenantDomain(consumerName));
            carbonContext.getTenantId(true);

            //if operating in dumb mode, do not persist the operation, only provision to providers

                //Persist in carbon user store
                if (log.isDebugEnabled()) {
                    log.debug("Creating user: " + user.getUserName());
                }
                /*set thread local property to signal the downstream SCIMUserOperationListener
                about the provisioning route.*/
                SCIMCommonUtils.setThreadLocalIsManagedThroughSCIMEP(true);
                Map<String, String> claimsMap = AttributeMapper.getClaimsMap(user);

                /*skip groups attribute since we map groups attribute to actual groups in ldap.
                and do not update it as an attribute in user schema*/
                if (claimsMap.containsKey(SCIMConstants.GROUPS_URI)) {
                    claimsMap.remove(SCIMConstants.GROUPS_URI);
                }

                //TODO: Do not accept the roles list - it is read only.
                if (carbonUM.isExistingUser(user.getUserName())) {
                    String error = "User with the name: " + user.getUserName() + " already exists in the system.";
                    throw new DuplicateResourceException(error);
                }
                if (claimsMap.containsKey(SCIMConstants.USER_NAME_URI)) {
                    claimsMap.remove(SCIMConstants.USER_NAME_URI);
                }
                carbonUM.addUser(user.getUserName(), user.getPassword(), null, claimsMap, null);
                log.info("User: " + user.getUserName() + " is created through SCIM.");

        } catch (UserStoreException e) {
            String errMsg = "Error in adding the user: " + user.getUserName() + " to the user store. ";
            errMsg += e.getMessage();
            throw new CharonException(errMsg, e);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
        return user;

    }

    @Override
    public User getUser(String userId) throws CharonException {
        if (log.isDebugEnabled()) {
            log.debug("Retrieving user: " + userId);
        }
        User scimUser = null;
        try {
            ClaimMapping[] claims;
            //get the user name of the user with this id
            String[] userNames = carbonUM.getUserList(SCIMConstants.ID_URI, userId,
                    UserCoreConstants.DEFAULT_PROFILE);

            if (userNames == null || userNames.length == 0) {
                if (log.isDebugEnabled()) {
                    log.debug("User with SCIM id: " + userId + " does not exist in the system.");
                }
                return null;
            } else if (userNames != null && userNames.length == 0) {
                if (log.isDebugEnabled()) {
                    log.debug("User with SCIM id: " + userId + " does not exist in the system.");
                }
                return null;
            } else {
                //get claims related to SCIM claim dialect
                claims = carbonClaimManager.getAllClaimMappings(SCIMCommonConstants.SCIM_CLAIM_DIALECT);
                List<String> claimURIList = new ArrayList<>();
                for (ClaimMapping claim : claims) {
                    claimURIList.add(claim.getClaim().getClaimUri());
                }
                //we assume (since id is unique per user) only one user exists for a given id
                scimUser = this.getSCIMUser(userNames[0], claimURIList);

                log.info("User: " + scimUser.getUserName() + " is retrieved through SCIM.");
            }

        } catch (UserStoreException e) {
            throw new CharonException("Error in getting user information from Carbon User Store for" +
                    "user: " + userId, e);
        }
        return scimUser;
    }

    @Override
    public List<User> listUsers() throws CharonException {

        ClaimMapping[] claims;
        List<User> users = new ArrayList<>();
        try {
            String[] userNames = carbonUM.getUserList(SCIMConstants.ID_URI, "*", null);
            if (userNames != null && userNames.length != 0) {
                //get claims related to SCIM claim dialect
                claims = carbonClaimManager.getAllClaimMappings(SCIMCommonConstants.SCIM_CLAIM_DIALECT);
                List<String> claimURIList = new ArrayList<>();
                for (ClaimMapping claim : claims) {
                    claimURIList.add(claim.getClaim().getClaimUri());
                }
                for (String userName : userNames) {
                    if (userName.contains(UserCoreConstants.NAME_COMBINER)) {
                        userName = userName.split("\\" + UserCoreConstants.NAME_COMBINER)[0];
                    }
                    User scimUser = this.getSCIMMetaUser(userName);
                    Map<String, Attribute> attrMap = scimUser.getAttributeList();
                    if (attrMap != null && !attrMap.isEmpty()) {
                        users.add(scimUser);
                    }
                }
            }
        } catch (UserStoreException e) {
            throw new CharonException("Error while retrieving users from user store..", e);
        }
        return users;
    }

    @Override
    public List<User> listUsersByAttribute(Attribute attribute) {
        return Collections.emptyList();
    }

    @Override
    public List<User> listUsersByFilter(String attributeName, String filterOperation,
                                        String attributeValue) throws CharonException {
        //since we only support eq filter operation at the moment, no need to check for that.
        if (log.isDebugEnabled()) {
            log.debug("Listing users by filter: " + attributeName + filterOperation +
                    attributeValue);
        }
        List<User> filteredUsers = new ArrayList<>();
        ClaimMapping[] claims;
        User scimUser = null;
        try {
            String[] userNames = null;
            if (!SCIMConstants.GROUPS_URI.equals(attributeName)) {
                //get the user name of the user with this id
                userNames = carbonUM.getUserList(attributeName, attributeValue, UserCoreConstants.DEFAULT_PROFILE);
            } else {
                userNames = carbonUM.getUserListOfRole(attributeValue);
            }

            if (userNames == null || userNames.length == 0) {
                if (log.isDebugEnabled()) {
                    log.debug("Users with filter: " + attributeName + filterOperation +
                            attributeValue + " does not exist in the system.");
                }
                return Collections.emptyList();
            } else {
                //get claims related to SCIM claim dialect
                claims = carbonClaimManager.getAllClaimMappings(SCIMCommonConstants.SCIM_CLAIM_DIALECT);
                List<String> claimURIList = new ArrayList<>();
                for (ClaimMapping claim : claims) {
                    claimURIList.add(claim.getClaim().getClaimUri());
                }
                for (String userName : userNames) {

                    if (CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME.equals(userName)) {
                        continue;
                    }

                    scimUser = this.getSCIMMetaUser(userName);
                    //if SCIM-ID is not present in the attributes, skip
                    if (scimUser.getId() == null) {
                        continue;
                    }
                    filteredUsers.add(scimUser);
                }
                log.info("Users filtered through SCIM for the filter: " + attributeName + filterOperation +
                        attributeValue);
            }

        } catch (UserStoreException e) {
            throw new CharonException("Error in filtering users by attribute name : " + attributeName + ", " +
                    "attribute value : " + attributeValue + " and filter operation " + filterOperation, e);
        }
        return filteredUsers;
    }

    @Override
    public List<User> listUsersBySort(String s, String s1) {
        return Collections.emptyList();
    }

    @Override
    public List<User> listUsersWithPagination(int i, int i1) {
        return Collections.emptyList();
    }

    @Override
    public User updateUser(User user) throws CharonException {

            if (log.isDebugEnabled()) {
                log.debug("Updating user: " + user.getUserName());
            }
            try {
                /*set thread local property to signal the downstream SCIMUserOperationListener
                about the provisioning route.*/
                SCIMCommonUtils.setThreadLocalIsManagedThroughSCIMEP(true);
                //get user claim values
                Map<String, String> claims = AttributeMapper.getClaimsMap(user);

                //check if username of the updating user existing in the userstore.
                //TODO:immutable userId can be something else other than username. eg: mail.
                //Therefore, correct way is to check the corresponding SCIM attribute for the
                //UserNameAttribute of user-mgt.xml.
                // Refer: SCIMUserOperationListener#isProvisioningActionAuthorized method.
                if (!carbonUM.isExistingUser(user.getUserName())) {
                    throw new CharonException("User name is immutable in carbon user store.");
                }

                /*skip groups attribute since we map groups attribute to actual groups in ldap.
                and do not update it as an attribute in user schema*/
                if (claims.containsKey(SCIMConstants.GROUPS_URI)) {
                    claims.remove(SCIMConstants.GROUPS_URI);
                }

                if (claims.containsKey(SCIMConstants.USER_NAME_URI)) {
                    claims.remove(SCIMConstants.USER_NAME_URI);
                }

                ClaimMapping[] claimList;
                claimList = carbonClaimManager.getAllClaimMappings(SCIMCommonConstants.SCIM_CLAIM_DIALECT);
                List<String> claimURIList = new ArrayList<>();
                for (ClaimMapping claim : claimList) {
                    claimURIList.add(claim.getClaim().getClaimUri());
                }

                Map<String, String> oldClaimList = carbonUM.getUserClaimValues(user.getUserName(), claimURIList
                        .toArray(new String[claimURIList.size()]), null);

                for (Map.Entry<String, String> entry : oldClaimList.entrySet()) {
                    if (!entry.getKey().equals(SCIMConstants.ID_URI) && !entry.getKey().equals(SCIMConstants
                            .USER_NAME_URI) && !entry.getKey().equals(SCIMConstants.META_CREATED_URI) && !entry
                            .getKey().equals(SCIMConstants.META_LAST_MODIFIED_URI) && !entry.getKey().equals
                            (SCIMConstants.META_LOCATION_URI) && !entry.getKey().equals(SCIMConstants
                            .NAME_FAMILY_NAME_URI)) {
                        carbonUM.deleteUserClaimValue(user.getUserName(), entry.getKey(), null);
                    }
                }

                //set user claim values
                carbonUM.setUserClaimValues(user.getUserName(), claims, null);
                //if password is updated, set it separately
                if (user.getPassword() != null) {
                    carbonUM.updateCredentialByAdmin(user.getUserName(), user.getPassword());
                }
                log.info("User: " + user.getUserName() + " updated updated through SCIM.");
            } catch (UserStoreException e) {
                throw new CharonException("Error while updating attributes of user: " + user.getUserName(), e);
            }

            return user;
    }

    @Override
    public User patchUser(User newUser, User oldUser, String[] attributesToDelete) throws CharonException {

            if (log.isDebugEnabled()) {
                log.debug("Updating user: " + newUser.getUserName());
            }
            try {
                /*set thread local property to signal the downstream SCIMUserOperationListener
                about the provisioning route.*/
                SCIMCommonUtils.setThreadLocalIsManagedThroughSCIMEP(true);
                //get user claim values
                Map<String, String> claims = AttributeMapper.getClaimsMap(newUser);

                //check if username of the updating user existing in the userStore.
                if (!carbonUM.isExistingUser(newUser.getUserName())) {
                    throw new CharonException("User name is immutable in carbon user store.");
                }

                /*skip groups attribute since we map groups attribute to actual groups in ldap.
                and do not update it as an attribute in user schema*/
                if (claims.containsKey(SCIMConstants.GROUPS_URI)) {
                    claims.remove(SCIMConstants.GROUPS_URI);
                }

                if (claims.containsKey(SCIMConstants.USER_NAME_URI)) {
                    claims.remove(SCIMConstants.USER_NAME_URI);
                }

                for (int i = 0; i < attributesToDelete.length; i++) {
                    attributesToDelete[i] = SCIMConstants.CORE_SCHEMA_URI + ":" + attributesToDelete[i];
                }
                carbonUM.deleteUserClaimValues(oldUser.getUserName(), attributesToDelete, null);

                //set user claim values
                carbonUM.setUserClaimValues(oldUser.getUserName(), claims, null);
                //if password is updated, set it separately
                if (StringUtils.isNotEmpty(newUser.getPassword())) {
                    carbonUM.updateCredentialByAdmin(newUser.getUserName(), newUser.getPassword());
                }
                log.info("User: " + newUser.getUserName() + " updated updated through SCIM.");
            } catch (org.wso2.carbon.user.core.UserStoreException e) {
                throw new CharonException("Error while updating attributes of user: " + newUser.getUserName(), e);
            }

            return newUser;
    }

    public User updateUser(List<Attribute> attributes) {
        return null;
    }

    @Override
    public void deleteUser(String userId) throws NotFoundException, CharonException {

            if (log.isDebugEnabled()) {
                log.debug("Deleting user: " + userId);
            }
            //get the user name of the user with this id
            String[] userNames = null;
            String userName = null;
            try {
                /*set thread local property to signal the downstream SCIMUserOperationListener
                about the provisioning route.*/
                SCIMCommonUtils.setThreadLocalIsManagedThroughSCIMEP(true);
                userNames = carbonUM.getUserList(SCIMConstants.ID_URI, userId,
                        UserCoreConstants.DEFAULT_PROFILE);
                if (userNames == null && userNames.length == 0) {
                    //resource with given id not found
                    if (log.isDebugEnabled()) {
                        log.debug("User with id: " + userId + " not found.");
                    }
                    throw new NotFoundException();
                } else if (userNames != null && userNames.length == 0) {
                    //resource with given id not found
                    if (log.isDebugEnabled()) {
                        log.debug("User with id: " + userId + " not found.");
                    }
                    throw new NotFoundException();
                } else {
                    //we assume (since id is unique per user) only one user exists for a given id
                    userName = userNames[0];
                    carbonUM.deleteUser(userName);
                    log.info("User: " + userName + " is deleted through SCIM.");
                }

            } catch (org.wso2.carbon.user.core.UserStoreException e) {
                throw new CharonException("Error in deleting user: " + userName, e);
            }
    }

    @Override
    public Group createGroup(Group group) throws CharonException, DuplicateResourceException {

            if (log.isDebugEnabled()) {
                log.debug("Creating group: " + group.getDisplayName());
            }
            try {
                //modify display name if no domain is specified, in order to support multiple user store feature
                String originalName = group.getDisplayName();
                String roleNameWithDomain = null;
                String domainName = "";
                if (originalName.indexOf(CarbonConstants.DOMAIN_SEPARATOR) > 0) {
                    roleNameWithDomain = originalName;
                    domainName = originalName.split(UserCoreConstants.DOMAIN_SEPARATOR)[0];
                } else {
                    roleNameWithDomain = UserCoreConstants.PRIMARY_DEFAULT_DOMAIN_NAME +
                            CarbonConstants.DOMAIN_SEPARATOR + originalName;
                    domainName = UserCoreConstants.PRIMARY_DEFAULT_DOMAIN_NAME;
                }
                group.setDisplayName(roleNameWithDomain);
                //check if the group already exists
                if (carbonUM.isExistingRole(group.getDisplayName(), false)) {
                    String error = "Group with name: " + group.getDisplayName() +
                            " already exists in the system.";
                    throw new DuplicateResourceException(error);
                }

                /*set thread local property to signal the downstream SCIMUserOperationListener
                about the provisioning route.*/
                SCIMCommonUtils.setThreadLocalIsManagedThroughSCIMEP(true);
                /*if members are sent when creating the group, check whether users already exist in the
                user store*/
                List<String> userIds = group.getMembers();
                List<String> userDisplayNames = group.getMembersWithDisplayName();
                if (CollectionUtils.isNotEmpty(userIds)) {
                    List<String> members = new ArrayList<>();
                    for (String userId : userIds) {
                        String[] userNames = carbonUM.getUserList(SCIMConstants.ID_URI, userId,
                                UserCoreConstants.DEFAULT_PROFILE);
                        if (userNames == null || userNames.length == 0) {
                            String error = "User: " + userId + " doesn't exist in the user store. " +
                                    "Hence, can not create the group: " + group.getDisplayName();
                            throw new IdentitySCIMException(error);
                        } else if (userNames[0].indexOf(UserCoreConstants.DOMAIN_SEPARATOR) > 0 && !userNames[0].contains(domainName)) {
                            String error = "User: " + userId + " doesn't exist in the same user store. " +
                                    "Hence, can not create the group: " + group.getDisplayName();
                            throw new IdentitySCIMException(error);
                        } else {
                            members.add(userNames[0]);
                            if (CollectionUtils.isNotEmpty(userDisplayNames)) {
                                boolean userContains = false;
                                for (String user : userDisplayNames) {
                                    user =
                                            user.indexOf(UserCoreConstants.DOMAIN_SEPARATOR) > 0
                                                    ? user.split(UserCoreConstants.DOMAIN_SEPARATOR)[1]
                                                    : user;
                                    if (user.equalsIgnoreCase(userNames[0].indexOf(UserCoreConstants.DOMAIN_SEPARATOR) > 0
                                            ? userNames[0].split(UserCoreConstants.DOMAIN_SEPARATOR)[1]
                                            : userNames[0])) {
                                        userContains = true;
                                        break;
                                    }
                                }
                                if (!userContains) {
                                    throw new IdentitySCIMException(
                                            "Given SCIM user Id and name not matching..");
                                }
                            }
                        }
                    }

                    //add other scim attributes in the identity DB since user store doesn't support some attributes.
                    SCIMGroupHandler scimGroupHandler =
                            new SCIMGroupHandler(
                                    carbonUM.getTenantId());
                    scimGroupHandler.createSCIMAttributes(group);
                    carbonUM.addRole(group.getDisplayName(),
                            members.toArray(new String[members.size()]), null, false);
                    log.info("Group: " + group.getDisplayName() + " is created through SCIM.");
                } else {
                    //add other scim attributes in the identity DB since user store doesn't support some attributes.
                    SCIMGroupHandler scimGroupHandler = new SCIMGroupHandler(carbonUM.getTenantId());
                    scimGroupHandler.createSCIMAttributes(group);
                    carbonUM.addRole(group.getDisplayName(), null, null, false);
                    log.info("Group: " + group.getDisplayName() + " is created through SCIM.");
                }
            } catch (UserStoreException e) {
                try {
                    SCIMGroupHandler scimGroupHandler = new SCIMGroupHandler(carbonUM.getTenantId());
                    scimGroupHandler.deleteGroupAttributes(group.getDisplayName());
                } catch (UserStoreException | IdentitySCIMException ex) {
                    throw new CharonException("Error occurred while doing rollback operation of the SCIM table entry for role: " + group.getDisplayName(), e);
                }
                throw new CharonException("Error occurred while adding role : " + group.getDisplayName(), e);
            } catch (IdentitySCIMException e) {
                //This exception can occurr because of scimGroupHandler.createSCIMAttributes(group) or
                //userContains=false. Therefore contextual message could not be provided.
                throw new CharonException("Error in creating group", e);
            }
            //TODO:after the group is added, read it from user store and return
            return group;
    }

    @Override
    public Group getGroup(String id) throws CharonException {
        if (log.isDebugEnabled()) {
            log.debug("Retrieving group with id: " + id);
        }
        Group group = null;
        try {
            SCIMGroupHandler groupHandler = new SCIMGroupHandler(carbonUM.getTenantId());
            //get group name by Id
            String groupName = groupHandler.getGroupName(id);

            if (groupName != null) {
                group = getGroupWithName(groupName);
            } else {
                //returning null will send a resource not found error to client by Charon.
                return null;
            }
        } catch (org.wso2.carbon.user.core.UserStoreException e) {
            throw new CharonException("Error in retrieving group : " + id, e);
        } catch (IdentitySCIMException e) {
            throw new CharonException("Error in retrieving SCIM Group information from database.", e);
        }
        return group;
    }

    @Override
    public List<Group> listGroups() throws CharonException {
        List<Group> groupList = new ArrayList<>();
        try {
            SCIMGroupHandler groupHandler = new SCIMGroupHandler(carbonUM.getTenantId());
            Set<String> roleNames = groupHandler.listSCIMRoles();
            for (String roleName : roleNames) {
                Group group = this.getGroupOnlyWithMetaAttributes(roleName);
                if (group.getId() != null) {
                    groupList.add(group);
                }
            }
        } catch (org.wso2.carbon.user.core.UserStoreException e) {
            String errMsg = "Error in obtaining role names from user store.";
            errMsg += e.getMessage();
            throw new CharonException(errMsg, e);
        } catch (IdentitySCIMException e) {
            throw new CharonException("Error in retrieving SCIM Group information from database.", e);
        }
        return groupList;
    }

    @Override
    public List<Group> listGroupsByAttribute(Attribute attribute) throws CharonException {
        return Collections.emptyList();
    }

    @Override
    public List<Group> listGroupsByFilter(String filterAttribute, String filterOperation,
                                          String attributeValue) throws CharonException {
        //since we only support "eq" filter operation for group name currently, no need to check for that.
        if (log.isDebugEnabled()) {
            log.debug("Listing groups with filter: " + filterAttribute + filterOperation +
                    attributeValue);
        }
        List<Group> filteredGroups = new ArrayList<>();
        Group group = null;
        try {
            if (attributeValue != null && carbonUM.isExistingRole(attributeValue, false)) {
                //skip internal roles
                if ((CarbonConstants.REGISTRY_ANONNYMOUS_ROLE_NAME.equals(attributeValue)) ||
                        UserCoreUtil.isEveryoneRole(attributeValue, carbonUM.getRealmConfiguration()) ||
                        UserCoreUtil.isPrimaryAdminRole(attributeValue, carbonUM.getRealmConfiguration())) {
                    throw new IdentitySCIMException("Internal roles do not support SCIM.");
                }
                /********we expect only one result**********/
                //construct the group name with domain -if not already provided, in order to support
                //multiple user store feature with SCIM.
                String groupNameWithDomain = null;
                if (attributeValue.indexOf(CarbonConstants.DOMAIN_SEPARATOR) > 0) {
                    groupNameWithDomain = attributeValue;
                } else {
                    groupNameWithDomain = UserCoreConstants.PRIMARY_DEFAULT_DOMAIN_NAME + CarbonConstants.DOMAIN_SEPARATOR
                            + attributeValue;
                }
                group = getGroupOnlyWithMetaAttributes(groupNameWithDomain);
                filteredGroups.add(group);
            } else {
                //returning null will send a resource not found error to client by Charon.
                return Collections.emptyList();
            }
        } catch (org.wso2.carbon.user.core.UserStoreException e) {
            throw new CharonException("Error in filtering groups by attribute name : " + filterAttribute + ", " +
                    "attribute value : " + attributeValue + " and filter operation " + filterOperation, e);
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            throw new CharonException("Error in filtering group with filter: "
                    + filterAttribute + filterOperation + attributeValue, e);
        } catch (IdentitySCIMException e) {
            throw new CharonException("Error in retrieving SCIM Group information from database.", e);
        }
        return filteredGroups;
    }

    @Override
    public List<Group> listGroupsBySort(String s, String s1) throws CharonException {
        return Collections.emptyList();
    }

    @Override
    public List<Group> listGroupsWithPagination(int i, int i1) {
        return Collections.emptyList();
    }

    @Override
    public Group updateGroup(Group oldGroup, Group newGroup) throws CharonException {

        newGroup.setDisplayName(SCIMCommonUtils.getGroupNameWithDomain(newGroup.getDisplayName()));
        oldGroup.setDisplayName(SCIMCommonUtils.getGroupNameWithDomain(oldGroup.getDisplayName()));

            if (log.isDebugEnabled()) {
                log.debug("Updating group: " + oldGroup.getDisplayName());
            }
            
            /*we need to set the domain name for the newGroup if it doesn't have it */
            // we should be able get the domain name like bellow, cause we set it by force at create group


            try {
                /*set thread local property to signal the downstream SCIMUserOperationListener
                about the provisioning route.*/
                SCIMCommonUtils.setThreadLocalIsManagedThroughSCIMEP(true);

                boolean updated = false;
                /*set thread local property to signal the downstream SCIMUserOperationListener
                about the provisioning route.*/
                SCIMCommonUtils.setThreadLocalIsManagedThroughSCIMEP(true);
                //check if the user ids sent in updated group exist in the user store and the associated user name
                //also a matching one.
                List<String> userIds = newGroup.getMembers();
                List<String> userDisplayNames = newGroup.getMembersWithDisplayName();

                String groupName = newGroup.getDisplayName();
                String userStoreDomainForGroup = UserCoreUtil.extractDomainFromName(groupName);
                /* compare user store domain of group and user store domain of user name , if there is a mismatch do not
                 update the group */
                if (userDisplayNames != null && userDisplayNames.size() > 0) {
                    for (String userDisplayName : userDisplayNames) {
                        String userStoreDomainForUser =
                                UserCoreUtil.extractDomainFromName(userDisplayName);
                        if (!(UserCoreConstants.PRIMARY_DEFAULT_DOMAIN_NAME.equals(userStoreDomainForGroup)) &&
                                (UserCoreConstants.PRIMARY_DEFAULT_DOMAIN_NAME.equals(userStoreDomainForUser))) {
                            throw new IdentitySCIMException(
                                    "User store domain is not indicated for user :" + userDisplayName);
                        }
                        if (!userStoreDomainForGroup.equalsIgnoreCase(userStoreDomainForUser)) {
                            throw new IdentitySCIMException(
                                    userDisplayName + " does not " + "belongs to user store " + userStoreDomainForGroup);
                        }

                    }
                }

                if (CollectionUtils.isNotEmpty(userIds)) {
                    String[] userNames = null;
                    for (String userId : userIds) {
                        userNames = carbonUM.getUserList(SCIMConstants.ID_URI,
                                UserCoreUtil.addDomainToName(userId, userStoreDomainForGroup),
                                UserCoreConstants.DEFAULT_PROFILE);
                        if (userNames == null || userNames.length == 0) {
                            String error = "User: " + userId + " doesn't exist in the user store. " +
                                    "Hence, can not update the group: " + oldGroup.getDisplayName();
                            throw new IdentitySCIMException(error);
                        } else {
                            if (!UserCoreUtil.isContain(UserCoreUtil.removeDomainFromName(userNames[0]),
                                    UserCoreUtil.removeDomainFromNames(userDisplayNames.toArray(
                                            new String[userDisplayNames.size()])))) {
                                throw new IdentitySCIMException("Given SCIM user Id and name not matching..");
                            }
                        }
                    }
                }
                //we do not update Identity_SCIM DB here since it is updated in SCIMUserOperationListener's methods.

                //update name if it is changed
                if (!(oldGroup.getDisplayName().equalsIgnoreCase(newGroup.getDisplayName()))) {
                    //update group name in carbon UM
                    carbonUM.updateRoleName(oldGroup.getDisplayName(), newGroup.getDisplayName());

                    updated = true;
                }

                //find out added members and deleted members..
                List<String> oldMembers = oldGroup.getMembersWithDisplayName();
                List<String> newMembers = newGroup.getMembersWithDisplayName();
                if (newMembers != null) {

                    List<String> addedMembers = new ArrayList<>();
                    List<String> deletedMembers = new ArrayList<>();

                    //check for deleted members
                    if (CollectionUtils.isNotEmpty(oldMembers)) {
                        for (String oldMember : oldMembers) {
                            if (newMembers != null && newMembers.contains(oldMember)) {
                                continue;
                            }
                            deletedMembers.add(oldMember);
                        }
                    }

                    //check for added members
                    if (CollectionUtils.isNotEmpty(newMembers)) {
                        for (String newMember : newMembers) {
                            if (oldMembers != null && oldMembers.contains(newMember)) {
                                continue;
                            }
                            addedMembers.add(newMember);
                        }
                    }

                    if (CollectionUtils.isNotEmpty(addedMembers) || CollectionUtils.isNotEmpty(deletedMembers)) {
                        carbonUM.updateUserListOfRole(newGroup.getDisplayName(),
                                deletedMembers.toArray(new String[deletedMembers.size()]),
                                addedMembers.toArray(new String[addedMembers.size()]));
                        updated = true;
                    }
                }
                if (updated) {
                    log.info("Group: " + newGroup.getDisplayName() + " is updated through SCIM.");
                } else {
                    log.warn("There is no updated field in the group: " + oldGroup.getDisplayName() +
                            ". Therefore ignoring the provisioning.");
                }

            } catch (UserStoreException | IdentitySCIMException e) {
                throw new CharonException("Error occurred while updating old group : " + oldGroup.getDisplayName(), e);
            }
            return newGroup;
    }

    @Override
    public Group updateGroup(List<Attribute> attributes) throws CharonException {
        return null;
    }

    /**
     * this method similar to updateGroup but new changes to group will be merged without changing existing data
     *
     * @param oldGroup existing group meta information
     * @param newGroup new changes required for existing group
     * @return updated group information
     * @throws CharonException
     */
    @Override
    public Group patchGroup(Group oldGroup, Group newGroup) throws CharonException {

            if (log.isDebugEnabled()) {
                log.debug("Updating group: " + oldGroup.getDisplayName());//add from group new name
            }

            /*
             * we need to set the domain name for the newGroup if it doesn't
             * have it
             */
            // we should be able get the domain name like bellow, cause we set
            // it by force at create group

            try {
                /*
                 * set thread local property to signal the downstream
                 * SCIMUserOperationListener
                 * about the provisioning route.
                 */
                SCIMCommonUtils.setThreadLocalIsManagedThroughSCIMEP(true);

                boolean updated = false;
                /*
                 * set thread local property to signal the downstream
                 * SCIMUserOperationListener
                 * about the provisioning route.
                 */
                SCIMCommonUtils.setThreadLocalIsManagedThroughSCIMEP(true);
                // check if the user ids sent in updated group exist in the user
                // store and the associated user name
                // also a matching one.

                List<String> userIds = newGroup.getMembers();
                List<String> userDisplayNames = newGroup.getMembersWithDisplayName();
                String[] userNames = null;
                if (userIds != null) {
                    for (String userId : userIds) {
                        userNames =
                                carbonUM.getUserList(SCIMConstants.ID_URI, userId,
                                                     UserCoreConstants.DEFAULT_PROFILE);
                        if (userNames == null || userNames.length == 0) {
                            String error =
                                    "User: " + userId + " doesn't exist in the user store. " +
                                    "Hence, can not update the group: " + oldGroup.getDisplayName();
                            throw new CharonException(error);
                        } else {
                            if (!userDisplayNames.contains(userNames[0])) {
                                throw new CharonException("Given SCIM user Id and name not matching..");
                            }
                        }
                    }
                }

                // we do not update Identity_SCIM DB here since it is updated in
                // SCIMUserOperationListener's methods.

                // update name if it is changed
                if (!(oldGroup.getDisplayName().equals(newGroup.getDisplayName()))) {
                    // update group name in carbon UM
                    carbonUM.updateRoleName(oldGroup.getDisplayName(), newGroup.getDisplayName());

                    updated = true;
                }

                //SCIM request does not have operation attribute for new members need be added hence parsing null
                List<String> addRequestedMembers = newGroup.getMembersWithDisplayName(null);
                List<String> deleteRequestedMembers =
                        newGroup.getMembersWithDisplayName(SCIMConstants.CommonSchemaConstants.OPERATION_DELETE);

                //Handling meta data attributes coming from SCIM request. Through meta attributes all existing members
                // can be replaced with new set of members
                if (newGroup.getAttributesOfMeta() != null &&
                    SCIMConstants.GroupSchemaConstants.MEMBERS.equals(newGroup.getAttributesOfMeta().get(0))) {
                    if (!deleteRequestedMembers.isEmpty()) {
                        log.warn(
                                "All Existing members will be deleted through SCIM meta attributes Hence operation " +
                                "delete is Invalid");
                        deleteRequestedMembers = new ArrayList<>();
                    }
                    String users[] = carbonUM.getUserListOfRole(newGroup.getDisplayName());
                    if (addRequestedMembers.isEmpty()) {
                        carbonUM.updateUserListOfRole(newGroup.getDisplayName(), users, new String[0]);
                    } else {
                        //If new set of members contains an old members, save those old members without deleting from user store
                        List<String> membersDeleteFromUserStore = new ArrayList<String>();
                        for (String user : users) {
                            if (!addRequestedMembers.contains(user)) {
                                membersDeleteFromUserStore.add(user);
                            }
                        }
                        carbonUM.updateUserListOfRole(newGroup.getDisplayName(), membersDeleteFromUserStore
                                .toArray(new String[membersDeleteFromUserStore.size()]), new String[0]);
                    }
                }
                // find out added members and deleted members..
                List<String> oldMembers = oldGroup.getMembersWithDisplayName();

                List<String> addedMembers = new ArrayList<>();
                List<String> deletedMembers = new ArrayList<>();

                if (addRequestedMembers.isEmpty() && deleteRequestedMembers.isEmpty()) {
                    String users[] = carbonUM.getUserListOfRole(newGroup.getDisplayName());
                    deletedMembers = Arrays.asList(users);
                }

                for (String addRequestedMember : addRequestedMembers) {
                    if ((!oldMembers.isEmpty()) && oldMembers.contains(addRequestedMember)) {
                        continue;
                    }
                    addedMembers.add(addRequestedMember);
                }

                for (String deleteRequestedMember : deleteRequestedMembers) {
                    if ((!oldMembers.isEmpty()) && oldMembers.contains(deleteRequestedMember)) {
                        deletedMembers.add(deleteRequestedMember);
                    } else {
                        continue;
                    }
                }

                if (newGroup.getDisplayName() != null && ((CollectionUtils.isNotEmpty(addedMembers)) || (CollectionUtils.isNotEmpty(deletedMembers)))) {
                    carbonUM.updateUserListOfRole(newGroup.getDisplayName(),
                            deletedMembers.toArray(new String[deletedMembers.size()]),
                            addedMembers.toArray(new String[addedMembers.size()]));
                    updated = true;
                }

                if (updated) {
                    log.info("Group: " + newGroup.getDisplayName() + " is updated through SCIM.");
                } else {
                    log.warn("There is no updated field in the group: " + oldGroup.getDisplayName() +
                            ". Therefore ignoring the provisioning.");
                }

            } catch (UserStoreException e) {
                //throwing real message coming from carbon user manager
                throw new CharonException("Error in patching group", e);
            }
            return newGroup;
    }

    @Override
    public void deleteGroup(String groupId) throws NotFoundException, CharonException {

            if (log.isDebugEnabled()) {
                log.debug("Deleting group: " + groupId);
            }
            try {
                /*set thread local property to signal the downstream SCIMUserOperationListener
                about the provisioning route.*/
                SCIMCommonUtils.setThreadLocalIsManagedThroughSCIMEP(true);

                //get group name by id
                SCIMGroupHandler groupHandler = new SCIMGroupHandler(carbonUM.getTenantId());
                String groupName = groupHandler.getGroupName(groupId);

                if (groupName != null) {
                    //delete group in carbon UM
                    carbonUM.deleteRole(groupName);

                    //we do not update Identity_SCIM DB here since it is updated in SCIMUserOperationListener's methods.
                    log.info("Group: " + groupName + " is deleted through SCIM.");

                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Group with SCIM id: " + groupId + " doesn't exist in the system.");
                    }
                    throw new NotFoundException();
                }
            } catch (UserStoreException | IdentitySCIMException e) {
                throw new CharonException("Error occurred while deleting group " + groupId, e);
            }
    }

    private User getSCIMMetaUser(String userName) throws CharonException {

        List<String> claimURIList = new ArrayList<>();
        claimURIList.add(SCIMConstants.ID_URI);
        claimURIList.add(SCIMConstants.META_LOCATION_URI);
        claimURIList.add(SCIMConstants.META_CREATED_URI);
        claimURIList.add(SCIMConstants.META_LAST_MODIFIED_URI);
        User scimUser = null;

        try {
            Map<String, String> attributes = carbonUM.getUserClaimValues(
                    userName, claimURIList.toArray(new String[claimURIList.size()]), null);
            attributes.put(SCIMConstants.USER_NAME_URI, userName);
            scimUser = (User) AttributeMapper.constructSCIMObjectFromAttributes(
                    attributes, SCIMConstants.USER_INT);
        } catch (UserStoreException | CharonException | NotFoundException e) {
            throw new CharonException("Error in getting user information from Carbon User Store for " +
                    "user: " + userName + " ", e);
        }
        return scimUser;
    }

    private User getSCIMUser(String userName, List<String> claimURIList) throws CharonException {
        User scimUser = null;
        try {

            //obtain user claim values
            Map<String, String> attributes = carbonUM.getUserClaimValues(
                    userName, claimURIList.toArray(new String[claimURIList.size()]), null);
            //skip simple type addresses claim coz it is complex with sub types in the schema
            if (attributes.containsKey(SCIMConstants.ADDRESSES_URI)) {
                attributes.remove(SCIMConstants.ADDRESSES_URI);
            }

            // Add username with domain name
            attributes.put(SCIMConstants.USER_NAME_URI, userName);

            //get groups of user and add it as groups attribute
            String[] roles = carbonUM.getRoleListOfUser(userName);
            //construct the SCIM Object from the attributes
            scimUser = (User) AttributeMapper.constructSCIMObjectFromAttributes(
                    attributes, SCIMConstants.USER_INT);
            //add groups of user:
            for (String role : roles) {
                if (UserCoreUtil.isEveryoneRole(role, carbonUM.getRealmConfiguration())
                        || UserCoreUtil.isPrimaryAdminRole(role, carbonUM.getRealmConfiguration())
                        || CarbonConstants.REGISTRY_ANONNYMOUS_ROLE_NAME.equalsIgnoreCase(role)
                        || role.toLowerCase().startsWith((UserCoreConstants.INTERNAL_DOMAIN +
                        CarbonConstants.DOMAIN_SEPARATOR).toLowerCase())) {
                    // carbon specific roles do not possess SCIM info, hence
                    // skipping them.
                    // skip intenal roles
                    continue;
                }
                Group group = getGroupOnlyWithMetaAttributes(role);
                if (group != null) { // can be null for non SCIM groups
                    scimUser.setGroup(null, group.getId(), role);
                }
            }
        } catch (UserStoreException | CharonException | NotFoundException | IdentitySCIMException e) {
            throw new CharonException("Error in getting user information for user: " + userName, e);
        }
        return scimUser;
    }

    /**
     * Get the full group with all the details including users.
     *
     * @param groupName
     * @return
     * @throws CharonException
     * @throws org.wso2.carbon.user.core.UserStoreException
     * @throws IdentitySCIMException
     */
    private Group getGroupWithName(String groupName)
            throws CharonException, org.wso2.carbon.user.core.UserStoreException,
            IdentitySCIMException {

        Group group = new Group();
        group.setDisplayName(groupName);
        String[] userNames = carbonUM.getUserListOfRole(groupName);

        //get the ids of the users and set them in the group with id + display name
        if (userNames != null && userNames.length != 0) {
            for (String userName : userNames) {
                String userId = carbonUM.getUserClaimValue(userName, SCIMConstants.ID_URI, null);
                group.setMember(userId, userName);
            }
        }
        //get other group attributes and set.
        SCIMGroupHandler groupHandler = new SCIMGroupHandler(carbonUM.getTenantId());
        group = groupHandler.getGroupWithAttributes(group, groupName);
        return group;
    }

    /**
     * Get group with only meta attributes.
     *
     * @param groupName
     * @return
     * @throws CharonException
     * @throws IdentitySCIMException
     * @throws org.wso2.carbon.user.core.UserStoreException
     */
    private Group getGroupOnlyWithMetaAttributes(String groupName)
            throws CharonException, IdentitySCIMException,
            org.wso2.carbon.user.core.UserStoreException {
        //get other group attributes and set.
        Group group = new Group();
        group.setDisplayName(groupName);
        SCIMGroupHandler groupHandler = new SCIMGroupHandler(carbonUM.getTenantId());
        return groupHandler.getGroupWithAttributes(group, groupName);
    }


    private void provision(ProvisioningOperation provisioningMethod, SCIMObject provisioningObject) throws CharonException {

        String domainName = UserCoreUtil.getDomainName(carbonUM.getRealmConfiguration());
        try {

            Map<org.wso2.carbon.identity.application.common.model.ClaimMapping, List<String>> outboundAttributes = new HashMap<org.wso2.carbon.identity.application.common.model.ClaimMapping, List<String>>();

            if (provisioningObject.getAttribute("password") != null) {
                outboundAttributes.put(org.wso2.carbon.identity.application.common.model.ClaimMapping.build(
                                IdentityProvisioningConstants.PASSWORD_CLAIM_URI, null, null, false),
                        Arrays.asList(new String[]{((SimpleAttribute) provisioningObject.getAttribute("password"))
                                .getStringValue()}));
            }

            if (provisioningObject.getAttribute(USER_NAME_STRING) != null) {
                outboundAttributes.put(org.wso2.carbon.identity.application.common.model.ClaimMapping.build(
                                IdentityProvisioningConstants.USERNAME_CLAIM_URI, null, null, false),
                        Arrays.asList(new String[]{((SimpleAttribute) provisioningObject.getAttribute(USER_NAME_STRING))
                                .getStringValue()}));
            }

            String domainAwareName = UserCoreUtil.addDomainToName(provisioningObject.getAttribute(USER_NAME_STRING).getName
                    (), domainName);
            ProvisioningEntity provisioningEntity = new ProvisioningEntity(
                    ProvisioningEntityType.USER, domainAwareName, provisioningMethod,
                    outboundAttributes);
            Map<String, String> inboundAttributes = AttributeMapper.getClaimsMap((AbstractSCIMObject) provisioningObject);

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
                        return;
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
                                DefaultInboundUserProvisioningListener.WSO2_CARBON_DIALECT, tenantDomainName, false);
            }

        } catch (NotFoundException e) {
            throw new CharonException("Failed to find resource in external user store.", e);
        } catch (IdentityProvisioningException e) {
            throw new CharonException("Error while provisioning to externaluser store in dumb mode.", e);
        }
    }
}
