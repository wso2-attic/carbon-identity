/*
 * Copyright (c) 2015 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.scim.provider.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ProvisioningServiceProviderType;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.common.model.ThreadLocalProvisioningServiceProvider;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationManagementUtil;
import org.wso2.carbon.identity.application.mgt.ApplicationInfoProvider;
import org.wso2.carbon.identity.scim.common.config.SCIMProvisioningConfigManager;
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
import org.wso2.charon.core.attributes.Attribute;
import org.wso2.charon.core.exceptions.CharonException;
import org.wso2.charon.core.exceptions.DuplicateResourceException;
import org.wso2.charon.core.exceptions.NotFoundException;
import org.wso2.charon.core.extensions.UserManager;
import org.wso2.charon.core.objects.Group;
import org.wso2.charon.core.objects.SCIMObject;
import org.wso2.charon.core.objects.User;
import org.wso2.charon.core.provisioning.ProvisioningHandler;
import org.wso2.charon.core.schema.SCIMConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SCIMUserManager implements UserManager {
    private static Log log = LogFactory.getLog(SCIMUserManager.class);
    private UserStoreManager carbonUM = null;
    private ClaimManager carbonClaimManager = null;
    private String consumerName;
    //to make provisioning to other providers asynchronously happen.
    private ExecutorService provisioningThreadPool = Executors.newCachedThreadPool();

    public SCIMUserManager(UserStoreManager carbonUserStoreManager, String userName,
                           ClaimManager claimManager) {
        carbonUM = carbonUserStoreManager;
        consumerName = userName;
        carbonClaimManager = claimManager;
    }

    public User createUser(User user) throws CharonException, DuplicateResourceException {
        return createUser(user, false);
    }
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
                serviceProvider = ApplicationInfoProvider.getInstance()
                                                         .getServiceProviderByClienId(threadLocalSP.getServiceProviderName(),
                                                                                      "oauth2", threadLocalSP.getTenantDomain());
            } else {
                serviceProvider = ApplicationInfoProvider.getInstance().getServiceProvider(
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

        SCIMProvisioningConfigManager provisioningConfigManager =
                SCIMProvisioningConfigManager.getInstance();
        //if operating in dumb mode, do not persist the operation, only provision to providers
        if (provisioningConfigManager.isDumbMode()) {

            if (log.isDebugEnabled()) {
                log.debug("This instance is operating in dumb mode. " +
                          "Hence, operation is not persisted, it will only be provisioned.");
            }
            this.provisionSCIMOperation(SCIMConstants.POST, user, SCIMConstants.USER_INT, null);
            return user;

        } else {
            //else, persist in carbon user store
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
            try {
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
                String errMsg = e.getMessage()+ " ";
                errMsg += "Error in adding the user: " + user.getUserName() +
                          " to the user store..";
                throw new CharonException(errMsg,e);
            }
            return user;
        }
    }

    public User getUser(String userId) throws CharonException {
        if (log.isDebugEnabled()) {
            log.debug("Retrieving user: " + userId);
        }
        User scimUser = null;
        try {
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
                //we assume (since id is unique per user) only one user exists for a given id
                scimUser = this.getSCIMUser(userNames[0]);

                log.info("User: " + scimUser.getUserName() + " is retrieved through SCIM.");
            }

        } catch (UserStoreException e) {
            throw new CharonException("Error in getting user information from Carbon User Store for" +
                                      "user: " + userId, e);
        }
        return scimUser;
    }

    public List<User> listUsers() throws CharonException {
        List<User> users = new ArrayList<User>();
        try {
            String[] userNames = carbonUM.getUserList(SCIMConstants.ID_URI, "*", null);
            if (userNames != null && userNames.length != 0) {
                for (String userName : userNames) {
                    if (userName.contains(UserCoreConstants.NAME_COMBINER)) {
                        userName = userName.split("\\" + UserCoreConstants.NAME_COMBINER)[0];
                    }
                    User scimUser = this.getSCIMUser(userName);
                    Map<String, Attribute> attrMap = scimUser.getAttributeList();
                    if (attrMap != null && !attrMap.isEmpty()) {
                        users.add(scimUser);
                    }
                }
            }
        } catch (org.wso2.carbon.user.core.UserStoreException e) {
            throw new CharonException("Error while retrieving users from user store..", e);
        }
        return users;
    }

    public List<User> listUsersByAttribute(Attribute attribute) {
        return null;
    }

    public List<User> listUsersByFilter(String attributeName, String filterOperation,
                                        String attributeValue) throws CharonException {
        //since we only support eq filter operation at the moment, no need to check for that.
        if (log.isDebugEnabled()) {
            log.debug("Listing users by filter: " + attributeName + filterOperation +
                      attributeValue);
        }
        List<User> filteredUsers = new ArrayList<User>();
        User scimUser = null;
        try {
            //get the user name of the user with this id
        	String[] userNames = null;
			if (attributeName.equals(SCIMConstants.USER_NAME_URI)) {
				if (carbonUM.isExistingUser(attributeValue)) {
					userNames = new String[] { attributeValue };
				}
			} else {
				userNames =
				            carbonUM.getUserList(attributeName, attributeValue,
				                                 UserCoreConstants.DEFAULT_PROFILE);
			}

            if (userNames == null || userNames.length == 0) {
                if (log.isDebugEnabled()) {
                    log.debug("Users with filter: " + attributeName + filterOperation +
                              attributeValue + " does not exist in the system.");
                }
                return null;
            } else {
                for (String userName : userNames) {
                    if (CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME.equals(userName)) {
                        continue;
                    }
                    scimUser = this.getSCIMUser(userName);
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
            String errMsg = "Error in getting user information from Carbon User Store for" +
                    "users:" + attributeValue + " ";
            errMsg += e.getMessage();
            throw new CharonException(errMsg, e);
        }
        return filteredUsers;
    }

    public List<User> listUsersBySort(String s, String s1) {
        return null;
    }

    public List<User> listUsersWithPagination(int i, int i1) {
        return null;
    }

    public User updateUser(User user) throws CharonException {
        SCIMProvisioningConfigManager provisioningConfigManager =
                SCIMProvisioningConfigManager.getInstance();
        //if operating in dumb mode, do not persist the operation, only provision to providers
        if (provisioningConfigManager.isDumbMode()) {

            if (log.isDebugEnabled()) {
                log.debug("This instance is operating in dumb mode. " +
                          "Hence, operation is not persisted, it will only be provisioned.");
            }
            this.provisionSCIMOperation(SCIMConstants.PUT, user, SCIMConstants.USER_INT, null);
            return user;

        } else {
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
                
                if(claims.containsKey(SCIMConstants.USER_NAME_URI)){
                	claims.remove(SCIMConstants.USER_NAME_URI);
                }

                //set user claim values
                carbonUM.setUserClaimValues(user.getUserName(), claims, null);
                //if password is updated, set it separately
                if (user.getPassword() != null) {
                    carbonUM.updateCredentialByAdmin(user.getUserName(), user.getPassword());
                }
                log.info("User: " + user.getUserName() + " updated updated through SCIM.");
            } catch (org.wso2.carbon.user.core.UserStoreException e) {
                String errMsg = "Error while updating attributes of user: " + user.getUserName();
                errMsg += " " + e.getMessage();
                throw new CharonException(errMsg, e);
            }

            return user;
        }
    }

    public User updateUser(List<Attribute> attributes) {
        return null;
    }

    public void deleteUser(String userId) throws NotFoundException, CharonException {
        SCIMProvisioningConfigManager provisioningConfigManager =
                SCIMProvisioningConfigManager.getInstance();

        //if operating in dumb mode, do not persist the operation, only provision to providers
        if (provisioningConfigManager.isDumbMode()) {
            if (log.isDebugEnabled()) {
                log.debug("This instance is operating in dumb mode. " +
                          "Hence, operation is not persisted, it will only be provisioned.");
            }
            User user = new User();
            user.setUserName(userId);
            this.provisionSCIMOperation(SCIMConstants.DELETE, user, SCIMConstants.USER_INT, null);
        } else {
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

                String errMsg = "Error in deleting user: " + userName + " ";
                errMsg += e.getMessage();
                throw new CharonException(errMsg, e);
            }
        }
    }

    public Group createGroup(Group group) throws CharonException, DuplicateResourceException {
        SCIMProvisioningConfigManager provisioningConfigManager =
                SCIMProvisioningConfigManager.getInstance();

        //if operating in dumb mode, do not persist the operation, only provision to providers
        if (provisioningConfigManager.isDumbMode()) {
            if (log.isDebugEnabled()) {
                log.debug("This instance is operating in dumb mode. " +
                          "Hence, operation is not persisted, it will only be provisioned.");
            }
            this.provisionSCIMOperation(SCIMConstants.POST, group, SCIMConstants.GROUP_INT, null);
            return group;
        } else {
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
                if (userIds != null && userIds.size() != 0) {
                    List<String> members = new ArrayList<String>();
                    for (String userId : userIds) {
                        String[] userNames = carbonUM.getUserList(SCIMConstants.ID_URI, userId,
                                                                  UserCoreConstants.DEFAULT_PROFILE);
                        if (userNames == null || userNames.length == 0) {
                            String error = "User: " + userId + " doesn't exist in the user store. " +
                                           "Hence, can not create the group: " + group.getDisplayName();
                            throw new IdentitySCIMException(error);
                        }
                        else if(userNames[0].indexOf(UserCoreConstants.DOMAIN_SEPARATOR) > 0 && !userNames[0].contains(domainName)){
                            String error = "User: " + userId + " doesn't exist in the same user store. " +
                                    "Hence, can not create the group: " + group.getDisplayName();
                            throw new IdentitySCIMException(error);
                        }
                        else {
                            members.add(userNames[0]);
							if (userDisplayNames != null && userDisplayNames.size() != 0) {
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
                throw new CharonException(e.getMessage(), e);
            } catch (IdentitySCIMException e) {
                throw new CharonException(e.getMessage(), e);
            }
            //TODO:after the group is added, read it from user store and return
            return group;
        }
    }

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
            String errMsg = "Error in retrieving group: " + id + " ";
            errMsg += e.getMessage();
            throw new CharonException(errMsg, e);
        } catch (IdentitySCIMException e) {
            throw new CharonException("Error in retrieving SCIM Group information from database.", e);
        }
        return group;
    }

    public List<Group> listGroups() throws CharonException {
        List<Group> groupList = new ArrayList<Group>();
        try {
            SCIMGroupHandler groupHandler = new SCIMGroupHandler(carbonUM.getTenantId());
            Set<String> roleNames = groupHandler.listSCIMRoles();
            for (String roleName : roleNames) {
                Group group = this.getGroupWithName(roleName);
                groupList.add(group);
            }
        } catch (org.wso2.carbon.user.core.UserStoreException e) {
            String errMsg = "Error in obtaining role names from user store." ;
            errMsg += e.getMessage();
            throw new CharonException(errMsg, e);
        } catch (IdentitySCIMException e) {
            throw new CharonException("Error in retrieving SCIM Group information from database.", e);
        }
        return groupList;
    }

    public List<Group> listGroupsByAttribute(Attribute attribute) throws CharonException {
        return null;
    }

    public List<Group> listGroupsByFilter(String filterAttribute, String filterOperation,
                                          String attributeValue) throws CharonException {
        //since we only support "eq" filter operation for group name currently, no need to check for that.
        if (log.isDebugEnabled()) {
            log.debug("Listing groups with filter: " + filterAttribute + filterOperation +
                      attributeValue);
        }
        List<Group> filteredGroups = new ArrayList<Group>();
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
                group = getGroupWithName(groupNameWithDomain);
                filteredGroups.add(group);
            } else {
                //returning null will send a resource not found error to client by Charon.
                return null;
            }
		} catch (org.wso2.carbon.user.core.UserStoreException e) {
            String errMsg ="Error in filtering group with filter: "
                    + filterAttribute + filterOperation + attributeValue;
            errMsg += e.getMessage();
            throw new CharonException(errMsg, e);
		} catch (org.wso2.carbon.user.api.UserStoreException e) {
			throw new CharonException("Error in filtering group with filter: "
					+ filterAttribute + filterOperation + attributeValue, e);
		} catch (IdentitySCIMException e) {
            throw new CharonException("Error in retrieving SCIM Group information from database.", e);
        }
        return filteredGroups;
    }

    public List<Group> listGroupsBySort(String s, String s1) throws CharonException {
        return null;
    }

    public List<Group> listGroupsWithPagination(int i, int i1) {
        return null;
    }

    public Group updateGroup(Group oldGroup, Group newGroup) throws CharonException {
        SCIMProvisioningConfigManager provisioningConfigManager =
                SCIMProvisioningConfigManager.getInstance();
        
        newGroup.setDisplayName(SCIMCommonUtils.getGroupNameWithDomain(newGroup.getDisplayName()));
        oldGroup.setDisplayName(SCIMCommonUtils.getGroupNameWithDomain(oldGroup.getDisplayName()));


        //if operating in dumb mode, do not persist the operation, only provision to providers
        if (provisioningConfigManager.isDumbMode()) {
            if (log.isDebugEnabled()) {
                log.debug("This instance is operating in dumb mode. " +
                          "Hence, operation is not persisted, it will only be provisioned.");
            }
            //add old role name details.
            Map<String, Object> additionalInformation = new HashMap<String, Object>();
            additionalInformation.put(SCIMCommonConstants.IS_ROLE_NAME_CHANGED_ON_UPDATE, true);
            additionalInformation.put(SCIMCommonConstants.OLD_GROUP_NAME, oldGroup.getDisplayName());

            this.provisionSCIMOperation(SCIMConstants.PUT, newGroup, SCIMConstants.GROUP_INT,
                                        additionalInformation);
            return newGroup;

        } else {
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
                String userStoreDomainForGroup = null;
                //Check domain name of the group
                int domainSeparatorIndexForGroup = groupName.indexOf(UserCoreConstants
                        .DOMAIN_SEPARATOR);
                if (domainSeparatorIndexForGroup > 0) {
                    userStoreDomainForGroup = groupName.substring(0, domainSeparatorIndexForGroup);
                                        /*User list and role should belong to same domain. throw exceptions if there
                     is mismatch*/
                    for (int i = 0; i < userDisplayNames.size(); i++) {
                        String userDisplayName = userDisplayNames.get(i);
                        int userDomainSeparatorIndex = userDisplayName.indexOf(UserCoreConstants
                                .DOMAIN_SEPARATOR);
                        if (userDomainSeparatorIndex > 0) {
                            String userStoreDomainForUser = groupName.substring(0,
                                    userDomainSeparatorIndex);
                            if (userStoreDomainForGroup.equals(userStoreDomainForUser)) {
                                continue;
                            } else {
                                throw new IdentitySCIMException(userDisplayName + " does not " +
                                        "belongs to user store " + userStoreDomainForGroup);
                            }

                        } else {
                            throw new IdentitySCIMException(userDisplayName + " does not " +
                                    "belongs to user store " + userStoreDomainForGroup);
                        }
                    }
                }

                if (userIds != null && userIds.size() != 0) {
                    String[] userNames = null;
                    for (String userId : userIds) {
                        userNames = carbonUM.getUserList(SCIMConstants.ID_URI, userId,
                                                         UserCoreConstants.DEFAULT_PROFILE);
                        if (userStoreDomainForGroup != null) {
                            userNames = carbonUM.getUserList(SCIMConstants.ID_URI,
                                    userStoreDomainForGroup + UserCoreConstants.DOMAIN_SEPARATOR + userId,
                                    UserCoreConstants.DEFAULT_PROFILE);
                        } else {
                            userNames = carbonUM.getUserList(SCIMConstants.ID_URI, userId,
                                    UserCoreConstants.DEFAULT_PROFILE);
                        }
                        if (userNames == null || userNames.length == 0) {
                            String error = "User: " + userId + " doesn't exist in the user store. " +
                                           "Hence, can not update the group: " + oldGroup.getDisplayName();
                            throw new IdentitySCIMException(error);
                        } else {
                            if (!userDisplayNames.contains(userNames[0])) {
                                throw new IdentitySCIMException("Given SCIM user Id and name not matching..");
                            }
                        }
                    }
                }
                //we do not update Identity_SCIM DB here since it is updated in SCIMUserOperationListener's methods.

                //update name if it is changed
                if (!(oldGroup.getDisplayName().equals(newGroup.getDisplayName()))) {
                    //update group name in carbon UM
                    carbonUM.updateRoleName(oldGroup.getDisplayName(), newGroup.getDisplayName());

                    updated = true;
                }

                //find out added members and deleted members..
                List<String> oldMembers = oldGroup.getMembersWithDisplayName();
                List<String> newMembers = newGroup.getMembersWithDisplayName();
                if (newMembers != null) {

                    List<String> addedMembers = new ArrayList<String>();
                    List<String> deletedMembers = new ArrayList<String>();

                    //check for deleted members
                    if (oldMembers != null && oldMembers.size() != 0) {
                        for (String oldMember : oldMembers) {
                            if (newMembers != null && newMembers.contains(oldMember)) {
                                continue;
                            }
                            deletedMembers.add(oldMember);
                        }
                    }

                    //check for added members
                    if (newMembers != null && newMembers.size() != 0) {
                        for (String newMember : newMembers) {
                            if (oldMembers != null && oldMembers.contains(newMember)) {
                                continue;
                            }
                            addedMembers.add(newMember);
                        }
                    }

                    if (addedMembers.size() != 0 || deletedMembers.size() != 0) {
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

            } catch (UserStoreException e) {
                throw new CharonException(e.getMessage());
            } catch (IdentitySCIMException e) {
                throw new CharonException(e.getMessage());
            }
            return newGroup;
        }
    }

    public Group updateGroup(List<Attribute> attributes) throws CharonException {
        return null;
    }

    public void deleteGroup(String groupId) throws NotFoundException, CharonException {
        SCIMProvisioningConfigManager provisioningConfigManager =
                SCIMProvisioningConfigManager.getInstance();
        //if operating in dumb mode, do not persist the operation, only provision to providers
        if (provisioningConfigManager.isDumbMode()) {
            if (log.isDebugEnabled()) {
                log.debug("This instance is operating in dumb mode. " +
                          "Hence, operation is not persisted, it will only be provisioned.");
            }
            Group group = new Group();
            group.setDisplayName(groupId);
            this.provisionSCIMOperation(SCIMConstants.DELETE, group, SCIMConstants.GROUP_INT, null);
        } else {
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
            } catch (UserStoreException e) {
                throw new CharonException(e.getMessage(), e);
            } catch (IdentitySCIMException e) {
                throw new CharonException(e.getMessage(), e);
            }
        }
    }

    private User getSCIMUser(String userName) throws CharonException {
        User scimUser = null;
        try {
            //get claims related to SCIM claim dialect
            ClaimMapping[] claims = carbonClaimManager.getAllClaimMappings(SCIMCommonUtils.SCIM_CLAIM_DIALECT);

            List<String> claimURIList = new ArrayList<String>();
            for (ClaimMapping claim : claims) {
                claimURIList.add(claim.getClaim().getClaimUri());
            }
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
        } catch (UserStoreException e) {
            String errMsg = "Error in getting user information from Carbon User Store for " +
                    "user: " + userName + " ";
            errMsg += e.getMessage();
            throw new CharonException(errMsg, e);
        } catch (CharonException e) {
            throw new CharonException("Error in getting user information from Carbon User Store for " +
                                      "user: " + userName, e);
        } catch (NotFoundException e) {
            throw new CharonException("Error in getting user information from Carbon User Store for " +
                                      "user: " + userName, e);
        } catch (IdentitySCIMException e) {
            throw new CharonException("Error in getting group information from Identity DB for " +
                                      "user: " + userName, e);
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
     *
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
                User user = this.getSCIMUser(userName);
                if (user != null) {
                    group.setMember(user.getId(), userName);
                }
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
     *
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

    /**
     * Provision the SCIM operation received at SCIM endpoint. In SCIMUserOperationListener,
     * we authorize the user who is performing the provisioning operation. But here, we do not need to
     * authorize since it is already done when obtaining the user manager instance.
     *
     * @param provisioningMethod
     * @param provisioningObject
     * @param provisioningObjectType
     * @throws CharonException
     */
    private void provisionSCIMOperation(int provisioningMethod, SCIMObject provisioningObject,
                                        int provisioningObjectType, Map<String, Object> properties)
            throws CharonException {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Server is operating in dumb mode. " +
                          "Hence, operation is not persisted, it will only be provisioned.");
            }
            SCIMProvisioningConfigManager provisioningConfigManager =
                    SCIMProvisioningConfigManager.getInstance();
            //read the connectors
            String[] provisioningHandlers = provisioningConfigManager.getProvisioningHandlers();
            if (provisioningHandlers != null && provisioningHandlers.length != 0) {
                //iterate configured set of connectors, initialize them, set properties and provision
                for (String provisioningHandler : provisioningHandlers) {
                    Class provisioningClass = Class.forName(provisioningHandler);
                    ProvisioningHandler provisioningAgent = (ProvisioningHandler) provisioningClass.newInstance();
                    provisioningAgent.setProvisioningConsumer(consumerName);
                    provisioningAgent.setProvisioningMethod(provisioningMethod);
                    provisioningAgent.setProvisioningObject(provisioningObject);
                    provisioningAgent.setProvisioningObjectType(provisioningObjectType);
                    provisioningAgent.setProperties(properties);
                    provisioningThreadPool.submit(provisioningAgent);
                }
            } else {
                throw new CharonException("Server is operating in dumb mode, " +
                                          "but no provisioning connectors are registered.");
            }
        } catch (ClassNotFoundException e) {
            throw new CharonException("Error in initializing provisioning handler", e);
        } catch (InstantiationException e) {
            throw new CharonException("Error in initializing provisioning handler", e);
        } catch (IllegalAccessException e) {
            throw new CharonException("Error in initializing provisioning handler", e);
        }
    }
}
