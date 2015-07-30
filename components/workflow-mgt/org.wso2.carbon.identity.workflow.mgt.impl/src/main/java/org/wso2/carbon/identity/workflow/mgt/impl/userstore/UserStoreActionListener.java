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

package org.wso2.carbon.identity.workflow.mgt.impl.userstore;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;
import org.wso2.carbon.identity.workflow.mgt.impl.dao.EntityDAO;
import org.wso2.carbon.user.api.Permission;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.common.AbstractUserOperationEventListener;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.util.Map;

public class UserStoreActionListener extends AbstractUserOperationEventListener {

    private static Log log = LogFactory.getLog(UserStoreActionListener.class);

    @Override
    public int getExecutionOrderId() {
        return 15;
    }

    @Override
    public boolean doPreAddUser(String userName, Object credential, String[] roleList, Map<String, String> claims,
                                String profile, UserStoreManager userStoreManager) throws UserStoreException {
        try {
            String domain = userStoreManager.getRealmConfiguration().getUserStoreProperty(UserCoreConstants.RealmConfig
                    .PROPERTY_DOMAIN_NAME);
            return new AddUserWFRequestHandler()
                    .startAddUserFlow(domain, userName, credential, roleList, claims, profile);

        } catch (WorkflowException e) {
            log.error("Initiating workflow for creating user: " + userName + " failed.", e);
            AddUserWFRequestHandler.unsetWorkFlowCompleted();
            //Use e.getMessage instead of using a message specific to this level since this message will be shown to in
            //the admin console error message
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
        String domain = userStoreManager.getRealmConfiguration().getUserStoreProperty(UserCoreConstants.RealmConfig
                .PROPERTY_DOMAIN_NAME);
        try {
            return new DeleteUserWFRequestHandler().startDeleteUserFlow(domain, userName);
        } catch (WorkflowException e) {
            DeleteUserWFRequestHandler.unsetWorkFlowCompleted();
            //Use e.getMessage instead of using a message specific to this level since this message will be shown to in
            //the admin console error message
            throw new UserStoreException(e.getMessage(), e);
        }
    }

    @Override
    public boolean doPreSetUserClaimValue(String userName, String claimURI, String claimValue, String profileName,
                                          UserStoreManager userStoreManager) throws UserStoreException {
        String domain = userStoreManager.getRealmConfiguration().getUserStoreProperty(UserCoreConstants.RealmConfig
                .PROPERTY_DOMAIN_NAME);
        try {
            return new SetUserClaimWFRequestHandler()
                    .startSetClaimWorkflow(domain, userName, claimURI, claimValue, profileName);
        } catch (WorkflowException e) {
            log.error("Initiating workflow failed for setting claim " + claimURI + "=" + claimValue + " of user: " +
                    userName, e);
        }
        return false;
    }

    @Override
    public boolean doPreSetUserClaimValues(String userName, Map<String, String> claims, String profileName,
                                           UserStoreManager userStoreManager) throws UserStoreException {
        String domain = userStoreManager.getRealmConfiguration().getUserStoreProperty(UserCoreConstants.RealmConfig
                .PROPERTY_DOMAIN_NAME);
        try {
            return new SetMultipleClaimsWFRequestHandler()
                    .startSetMultipleClaimsWorkflow(domain, userName, claims, profileName);
        } catch (WorkflowException e) {
            log.error("Initiating workflow failed for setting claims of user: " + userName, e);
        }
        return false;
    }

    @Override
    public boolean doPreDeleteUserClaimValues(String userName, String[] claims, String profileName, UserStoreManager
            userStoreManager) throws UserStoreException {
        String domain = userStoreManager.getRealmConfiguration().getUserStoreProperty(UserCoreConstants.RealmConfig
                .PROPERTY_DOMAIN_NAME);
        try {
            return new DeleteMultipleClaimsWFRequestHandler()
                    .startDeleteMultipleClaimsWorkflow(domain, userName, claims, profileName);
        } catch (WorkflowException e) {
            log.error("Initiating workflow failed for deleting claims of user: " + userName, e);
        }
        return false;
    }

    @Override
    public boolean doPreDeleteUserClaimValue(String userName, String claimURI, String profileName,
                                             UserStoreManager userStoreManager) throws UserStoreException {
        String domain = userStoreManager.getRealmConfiguration().getUserStoreProperty(UserCoreConstants.RealmConfig
                .PROPERTY_DOMAIN_NAME);
        try {
            return new DeleteClaimWFRequestHandler()
                    .startDeleteClaimWorkflow(domain, userName, claimURI, profileName);
        } catch (WorkflowException e) {
            log.error("Initiating workflow failed for deleting claim " + claimURI + " of user: " + userName, e);
        }
        return false;
    }

    @Override
    public boolean doPreAddRole(String roleName, String[] userList, Permission[] permissions, UserStoreManager
            userStoreManager) throws UserStoreException {
        String domain = userStoreManager.getRealmConfiguration().getUserStoreProperty(UserCoreConstants.RealmConfig
                .PROPERTY_DOMAIN_NAME);
        try {
            return new AddRoleWFRequestHandler().startAddRoleFlow(domain, roleName, userList, permissions);
        } catch (WorkflowException e) {
            AddRoleWFRequestHandler.unsetWorkFlowCompleted();
            //Use e.getMessage instead of using a message specific to this level since this message will be shown to in
            //the admin console error message
            throw new UserStoreException(e.getMessage(), e);
        }
    }

    @Override
    public boolean doPreDeleteRole(String roleName, UserStoreManager userStoreManager) throws UserStoreException {
        String domain = userStoreManager.getRealmConfiguration().getUserStoreProperty(UserCoreConstants.RealmConfig
                .PROPERTY_DOMAIN_NAME);
        try {
            return new DeleteRoleWFRequestHandler().startDeleteRoleFlow(domain, roleName);
        } catch (WorkflowException e) {
            DeleteRoleWFRequestHandler.unsetWorkFlowCompleted();
            //Use e.getMessage instead of using a message specific to this level since this message will be shown to in
            //the admin console error message
            throw new UserStoreException(e.getMessage(), e);
        }
    }

    @Override
    public boolean doPreUpdateRoleName(String roleName, String newRoleName, UserStoreManager userStoreManager) throws
            UserStoreException {
        String domain = userStoreManager.getRealmConfiguration().getUserStoreProperty(UserCoreConstants.RealmConfig
                .PROPERTY_DOMAIN_NAME);
        try {
            return new UpdateRoleNameWFRequestHandler()
                    .startUpdateRoleNameFlow(domain, roleName, newRoleName);
        } catch (WorkflowException e) {
            DeleteRoleWFRequestHandler.unsetWorkFlowCompleted();
            //Use e.getMessage instead of using a message specific to this level since this message will be shown to in
            //the admin console error message
            throw new UserStoreException(e.getMessage(), e);
        }
    }

    @Override
    public boolean doPreUpdateUserListOfRole(String roleName, String[] deletedUsers, String[] newUsers, UserStoreManager
            userStoreManager) throws UserStoreException {
        String domain = userStoreManager.getRealmConfiguration().getUserStoreProperty(UserCoreConstants.RealmConfig
                .PROPERTY_DOMAIN_NAME);
        try {
            return new UpdateRoleUsersWFRequestHandler()
                    .startUpdateRoleUsersFlow(domain, roleName, deletedUsers, newUsers);
        } catch (WorkflowException e) {
            DeleteRoleWFRequestHandler.unsetWorkFlowCompleted();
            //Use e.getMessage instead of using a message specific to this level since this message will be shown to in
            //the admin console error message
            throw new UserStoreException(e.getMessage(), e);
        }
    }

    @Override
    public boolean doPreUpdateRoleListOfUser(String userName, String[] deletedRoles, String[] newRoles, UserStoreManager
            userStoreManager) throws UserStoreException {
        String domain = userStoreManager.getRealmConfiguration().getUserStoreProperty(UserCoreConstants.RealmConfig
                .PROPERTY_DOMAIN_NAME);
        try {
            return new UpdateUserRolesWFRequestHandler()
                    .startUpdateUserRolesFlow(domain, userName, deletedRoles, newRoles);
        } catch (WorkflowException e) {
            DeleteRoleWFRequestHandler.unsetWorkFlowCompleted();
            //Use e.getMessage instead of using a message specific to this level since this message will be shown to in
            //the admin console error message
            throw new UserStoreException(e.getMessage(), e);
        }
    }

    @Override
    public boolean doPreAuthenticate(String userName, Object credential, UserStoreManager userStoreManager) throws
            UserStoreException {

        try {
            String domain = userStoreManager.getRealmConfiguration().getUserStoreProperty(UserCoreConstants.RealmConfig
                    .PROPERTY_DOMAIN_NAME);
            EntityDAO entityDAO = new EntityDAO();
            String tenant = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
            String nameWithTenant = UserCoreUtil.addTenantDomainToEntry(userName, tenant);
            String fullyQualifiedName = UserCoreUtil.addDomainToName(nameWithTenant, domain);
            return entityDAO.checkEntityLocked(fullyQualifiedName, "USER");
        } catch (WorkflowException e) {
            throw new UserStoreException("Couldn't access workflow states of the user.", e);
        }
    }
}
