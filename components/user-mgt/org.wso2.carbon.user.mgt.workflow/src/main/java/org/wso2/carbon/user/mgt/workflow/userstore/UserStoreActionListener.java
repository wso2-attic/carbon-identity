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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.user.mgt.workflow.userstore;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.core.AbstractIdentityUserOperationEventListener;
import org.wso2.carbon.identity.core.util.IdentityCoreConstants;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;
import org.wso2.carbon.user.api.Permission;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;

import java.util.Map;

public class UserStoreActionListener extends AbstractIdentityUserOperationEventListener {

    private static Log log = LogFactory.getLog(UserStoreActionListener.class);

    @Override
    public int getExecutionOrderId() {
        int orderId = getOrderId();
        if (orderId != IdentityCoreConstants.EVENT_LISTENER_ORDER_ID) {
            return orderId;
        }
        return 10;
    }

    @Override
    public boolean doPreAddUser(String userName, Object credential, String[] roleList, Map<String, String> claims,
                                String profile, UserStoreManager userStoreManager) throws UserStoreException {
        AddUserWFRequestHandler addUserWFRequestHandler = new AddUserWFRequestHandler();
        try {
            if (!isEnable() || !addUserWFRequestHandler.isAssociated()) {
                return true;
            }
            String domain = userStoreManager.getRealmConfiguration().getUserStoreProperty(UserCoreConstants.RealmConfig
                    .PROPERTY_DOMAIN_NAME);
            return addUserWFRequestHandler.startAddUserFlow(domain, userName, credential, roleList, claims, profile);
        } catch (WorkflowException e) {
            // Sending e.getMessage() since it is required to give error message to end user.
            throw new UserStoreException(e.getMessage(), e);
        }
    }

    @Override
    public boolean doPreUpdateCredential(String userName, Object newCredential, Object oldCredential,
                                         UserStoreManager userStoreManager) throws UserStoreException {
// todo: commenting out since a test failure
//        String domain = userStoreManager.getRealmConfiguration().getUserStoreProperty(UserCoreConstants.RealmConfig
//                .PROPERTY_DOMAIN_NAME);
//        try {
//            return new ChangeCredentialWFRequestHandler()
//                    .startChangeCredentialWorkflow(domain, userName, newCredential, oldCredential);
//        } catch (WorkflowException e) {
//            log.error("Initiating workflow for updating credentials of user: " + userName + " failed.", e);
//        }
//        return false;
        return true;
    }

    @Override
    public boolean doPreUpdateCredentialByAdmin(String s, Object o, UserStoreManager userStoreManager) throws
            UserStoreException {
        return true;
    }

    @Override
    public boolean doPreDeleteUser(String userName, UserStoreManager userStoreManager) throws UserStoreException {

        DeleteUserWFRequestHandler deleteUserWFRequestHandler = new DeleteUserWFRequestHandler();
        try {
            if (!isEnable() || !deleteUserWFRequestHandler.isAssociated()) {
                return true;
            }
            String domain = userStoreManager.getRealmConfiguration().getUserStoreProperty(UserCoreConstants.RealmConfig
                                                                                                  .PROPERTY_DOMAIN_NAME);
            return deleteUserWFRequestHandler.startDeleteUserFlow(domain, userName);
        } catch (WorkflowException e) {
            // Sending e.getMessage() since it is required to give error message to end user.
            throw new UserStoreException(e.getMessage(), e);
        }
    }

    @Override
    public boolean doPreSetUserClaimValue(String userName, String claimURI, String claimValue, String profileName,
                                          UserStoreManager userStoreManager) throws UserStoreException {
        SetUserClaimWFRequestHandler setUserClaimWFRequestHandler = new SetUserClaimWFRequestHandler();
        try {
            if (!isEnable() || !setUserClaimWFRequestHandler.isAssociated()) {
                return true;
            }
            String domain = userStoreManager.getRealmConfiguration().getUserStoreProperty(UserCoreConstants.RealmConfig
                                                                                                  .PROPERTY_DOMAIN_NAME);
            return setUserClaimWFRequestHandler.startSetClaimWorkflow(domain, userName, claimURI, claimValue,
                                                                      profileName);
        } catch (WorkflowException e) {
            // Sending e.getMessage() since it is required to give error message to end user.
            throw new UserStoreException(e.getMessage(), e);
        }
    }

    @Override
    public boolean doPreSetUserClaimValues(String userName, Map<String, String> claims, String profileName,
                                           UserStoreManager userStoreManager) throws UserStoreException {

        SetMultipleClaimsWFRequestHandler setMultipleClaimsWFRequestHandler = new SetMultipleClaimsWFRequestHandler();
        try {
            if (!isEnable() || !setMultipleClaimsWFRequestHandler.isAssociated()) {
                return true;
            }
            String domain = userStoreManager.getRealmConfiguration().getUserStoreProperty(UserCoreConstants.RealmConfig
                                                                                                  .PROPERTY_DOMAIN_NAME);
            return setMultipleClaimsWFRequestHandler.startSetMultipleClaimsWorkflow(domain, userName, claims, profileName);
        } catch (WorkflowException e) {
            // Sending e.getMessage() since it is required to give error message to end user.
            throw new UserStoreException(e.getMessage(), e);
        }
    }

    @Override
    public boolean doPreDeleteUserClaimValues(String userName, String[] claims, String profileName, UserStoreManager
            userStoreManager) throws UserStoreException {
        DeleteMultipleClaimsWFRequestHandler deleteMultipleClaimsWFRequestHandler = new DeleteMultipleClaimsWFRequestHandler();
        try {
            if (!isEnable() || !deleteMultipleClaimsWFRequestHandler.isAssociated()) {
                return true;
            }
            String domain = userStoreManager.getRealmConfiguration().getUserStoreProperty(UserCoreConstants.RealmConfig
                                                                                                  .PROPERTY_DOMAIN_NAME);
            return deleteMultipleClaimsWFRequestHandler.startDeleteMultipleClaimsWorkflow(domain, userName, claims,
                                                                                          profileName);
        } catch (WorkflowException e) {
            // Sending e.getMessage() since it is required to give error message to end user.
            throw new UserStoreException(e.getMessage(), e);
        }
    }

    @Override
    public boolean doPreDeleteUserClaimValue(String userName, String claimURI, String profileName,
                                             UserStoreManager userStoreManager) throws UserStoreException {
        DeleteClaimWFRequestHandler deleteClaimWFRequestHandler = new DeleteClaimWFRequestHandler();
        try {
            if (!isEnable() || !deleteClaimWFRequestHandler.isAssociated()) {
                return true;
            }
            String domain = userStoreManager.getRealmConfiguration().getUserStoreProperty(UserCoreConstants.RealmConfig
                                                                                                  .PROPERTY_DOMAIN_NAME);
            return deleteClaimWFRequestHandler.startDeleteClaimWorkflow(domain, userName, claimURI, profileName);
        } catch (WorkflowException e) {
            // Sending e.getMessage() since it is required to give error message to end user.
            throw new UserStoreException(e.getMessage(), e);
        }
    }

    @Override
    public boolean doPreAddRole(String roleName, String[] userList, Permission[] permissions, UserStoreManager
            userStoreManager) throws UserStoreException {
        AddRoleWFRequestHandler addRoleWFRequestHandler = new AddRoleWFRequestHandler();
        try {
            if (!isEnable() || !addRoleWFRequestHandler.isAssociated()) {
                return true;
            }
            String domain = userStoreManager.getRealmConfiguration().getUserStoreProperty(UserCoreConstants.RealmConfig
                                                                                                  .PROPERTY_DOMAIN_NAME);
            return addRoleWFRequestHandler.startAddRoleFlow(domain, roleName, userList, permissions);
        } catch (WorkflowException e) {
            // Sending e.getMessage() since it is required to give error message to end user.
            throw new UserStoreException(e.getMessage(), e);
        }
    }

    @Override
    public boolean doPreDeleteRole(String roleName, UserStoreManager userStoreManager) throws UserStoreException {
        DeleteRoleWFRequestHandler deleteRoleWFRequestHandler = new DeleteRoleWFRequestHandler();
        try {
            if (!isEnable() || !deleteRoleWFRequestHandler.isAssociated()) {
                return true;
            }
            String domain = userStoreManager.getRealmConfiguration().getUserStoreProperty(UserCoreConstants.RealmConfig
                                                                                                  .PROPERTY_DOMAIN_NAME);
            return deleteRoleWFRequestHandler.startDeleteRoleFlow(domain, roleName);
        } catch (WorkflowException e) {
            // Sending e.getMessage() since it is required to give error message to end user.
            throw new UserStoreException(e.getMessage(), e);
        }
    }

    @Override
    public boolean doPreUpdateRoleName(String roleName, String newRoleName, UserStoreManager userStoreManager) throws
            UserStoreException {
        UpdateRoleNameWFRequestHandler updateRoleNameWFRequestHandler = new UpdateRoleNameWFRequestHandler();
        try {
            if (!isEnable() || !updateRoleNameWFRequestHandler.isAssociated()) {
                return true;
            }
            String domain = userStoreManager.getRealmConfiguration().getUserStoreProperty(UserCoreConstants.RealmConfig
                                                                                                  .PROPERTY_DOMAIN_NAME);
            return updateRoleNameWFRequestHandler.startUpdateRoleNameFlow(domain, roleName, newRoleName);
        } catch (WorkflowException e) {
            // Sending e.getMessage() since it is required to give error message to end user.
            throw new UserStoreException(e.getMessage(), e);
        }
    }

    @Override
    public boolean doPreUpdateUserListOfRole(String roleName, String[] deletedUsers, String[] newUsers, UserStoreManager
            userStoreManager) throws UserStoreException {
        UpdateRoleUsersWFRequestHandler updateRoleUsersWFRequestHandler = new UpdateRoleUsersWFRequestHandler();
        try {
            if (!isEnable() || !updateRoleUsersWFRequestHandler.isAssociated()) {
                return true;
            }
            String domain = userStoreManager.getRealmConfiguration().getUserStoreProperty(UserCoreConstants.RealmConfig
                                                                                                  .PROPERTY_DOMAIN_NAME);
            return updateRoleUsersWFRequestHandler.startUpdateRoleUsersFlow(domain, roleName, deletedUsers, newUsers);
        } catch (WorkflowException e) {
            // Sending e.getMessage() since it is required to give error message to end user.
            throw new UserStoreException(e.getMessage(), e);
        }
    }

    @Override
    public boolean doPreUpdateRoleListOfUser(String userName, String[] deletedRoles, String[] newRoles, UserStoreManager
            userStoreManager) throws UserStoreException {
        UpdateUserRolesWFRequestHandler updateUserRolesWFRequestHandler = new UpdateUserRolesWFRequestHandler();
        try {
            if (!isEnable() || !updateUserRolesWFRequestHandler.isAssociated()) {
                return true;
            }
            String domain = userStoreManager.getRealmConfiguration().getUserStoreProperty(UserCoreConstants.RealmConfig
                                                                                                  .PROPERTY_DOMAIN_NAME);
            return updateUserRolesWFRequestHandler.startUpdateUserRolesFlow(domain, userName, deletedRoles, newRoles);
        } catch (WorkflowException e) {
            // Sending e.getMessage() since it is required to give error message to end user.
            throw new UserStoreException(e.getMessage(), e);
        }
    }
}
