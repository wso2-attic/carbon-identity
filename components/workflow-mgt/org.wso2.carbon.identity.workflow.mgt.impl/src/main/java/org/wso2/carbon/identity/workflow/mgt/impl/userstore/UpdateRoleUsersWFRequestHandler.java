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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.workflow.mgt.extension.AbstractWorkflowRequestHandler;
import org.wso2.carbon.identity.workflow.mgt.impl.dao.EntityDAO;
import org.wso2.carbon.identity.workflow.mgt.impl.dao.EntityRelationshipDAO;
import org.wso2.carbon.identity.workflow.mgt.util.WorkflowDataType;
import org.wso2.carbon.identity.workflow.mgt.util.WorkflowRequestStatus;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;
import org.wso2.carbon.identity.workflow.mgt.impl.internal.IdentityWorkflowDataHolder;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UpdateRoleUsersWFRequestHandler extends AbstractWorkflowRequestHandler {

    private static final String FRIENDLY_NAME = "Update Role Users";
    private static final String FRIENDLY_DESCRIPTION = "Triggered when users are added to/removed from a role.";

    private static final String ROLENAME = "Role Name";
    private static final String USER_STORE_DOMAIN = "User Store Domain";
    private static final String DELETED_USER_LIST = "Users to be Deleted";
    private static final String NEW_USER_LIST = "Users to be Added";

    private static final Map<String, String> PARAM_DEFINITION;
    private static Log log = LogFactory.getLog(AddUserWFRequestHandler.class);

    static {
        PARAM_DEFINITION = new LinkedHashMap<>();
        PARAM_DEFINITION.put(ROLENAME, WorkflowDataType.STRING_TYPE);
        PARAM_DEFINITION.put(USER_STORE_DOMAIN, WorkflowDataType.STRING_TYPE);
        PARAM_DEFINITION.put(DELETED_USER_LIST, WorkflowDataType.STRING_LIST_TYPE);
        PARAM_DEFINITION.put(NEW_USER_LIST, WorkflowDataType.STRING_LIST_TYPE);
    }

    public boolean startUpdateRoleUsersFlow(String userStoreDomain, String roleName, String[] deletedUsers, String[]
            newUsers) throws WorkflowException {
        Map<String, Object> wfParams = new HashMap<>();
        Map<String, Object> nonWfParams = new HashMap<>();

        if (!Boolean.TRUE.equals(getWorkFlowCompleted())) {
            EntityRelationshipDAO entityRelationshipDAO = new EntityRelationshipDAO();
            String tenant = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
            String nameWithTenant = UserCoreUtil.addTenantDomainToEntry(roleName, tenant);
            String fullyQualifiedName = UserCoreUtil.addDomainToName(nameWithTenant, userStoreDomain);

            String[] fullyQulalifiedNewUserList = new String[newUsers.length];
            String[] fullyQulalifiedDeletedUserList = new String[deletedUsers.length];
            for (int i = 0; i < newUsers.length; i++) {
                nameWithTenant = UserCoreUtil.addTenantDomainToEntry(newUsers[i], tenant);
                fullyQulalifiedNewUserList[i] = UserCoreUtil.addDomainToName(nameWithTenant, userStoreDomain);
            }
            for (int i = 0; i < deletedUsers.length; i++) {
                nameWithTenant = UserCoreUtil.addTenantDomainToEntry(deletedUsers[i], tenant);
                fullyQulalifiedDeletedUserList[i] = UserCoreUtil.addDomainToName(nameWithTenant, userStoreDomain);
            }

            Boolean goodToProceed = checkRoleUpdatePossible(fullyQualifiedName, fullyQulalifiedNewUserList,
                    fullyQulalifiedDeletedUserList);
            if (!goodToProceed) {
                throw new WorkflowException("One or more specified entities are in pending workglows");
            }
            entityRelationshipDAO.addNewRelationships(fullyQualifiedName, "ROLE", fullyQulalifiedDeletedUserList,
                    "USER", "DELETE");
            entityRelationshipDAO.addNewRelationships(fullyQualifiedName, "ROLE", fullyQulalifiedNewUserList, "USER",
                    "ADD");
        }

        wfParams.put(ROLENAME, roleName);
        wfParams.put(USER_STORE_DOMAIN, userStoreDomain);
        wfParams.put(DELETED_USER_LIST, Arrays.asList(deletedUsers));
        wfParams.put(NEW_USER_LIST, Arrays.asList(newUsers));
        return startWorkFlow(wfParams, nonWfParams);
    }

    @Override
    public String getEventId() {
        return UserStoreWFConstants.UPDATE_ROLE_USERS_EVENT;
    }

    @Override
    public Map<String, String> getParamDefinitions() {
        return PARAM_DEFINITION;
    }

    @Override
    public String getFriendlyName() {
        return FRIENDLY_NAME;
    }

    @Override
    public String getDescription() {
        return FRIENDLY_DESCRIPTION;
    }

    @Override
    public String getCategory() {
        return UserStoreWFConstants.CATEGORY_USERSTORE_OPERATIONS;
    }

    @Override
    public boolean retryNeedAtCallback() {
        return true;
    }

    @Override
    public void onWorkflowCompletion(String status, Map<String, Object> requestParams, Map<String, Object>
            responseAdditionalParams, int tenantId) throws WorkflowException {

        String roleName = (String) requestParams.get(ROLENAME);
        if (roleName == null) {
            throw new WorkflowException("Callback request for Add User received without the mandatory " +
                    "parameter 'username'");
        }
        String userStoreDomain = (String) requestParams.get(USER_STORE_DOMAIN);
        if (StringUtils.isNotBlank(userStoreDomain)) {
            roleName = userStoreDomain + "/" + roleName;
        }

        List<String> deletedUserList = ((List<String>) requestParams.get(DELETED_USER_LIST));
        String[] deletedUsers;
        if (deletedUserList != null) {
            deletedUsers = new String[deletedUserList.size()];
            deletedUsers = deletedUserList.toArray(deletedUsers);
        } else {
            deletedUsers = new String[0];
        }

        List<String> newUserList = ((List<String>) requestParams.get(NEW_USER_LIST));
        String[] newUsers;
        if (newUserList != null) {
            newUsers = new String[newUserList.size()];
            newUsers = newUserList.toArray(newUsers);
        } else {
            newUsers = new String[0];
        }

        if (WorkflowRequestStatus.APPROVED.toString().equals(status) ||
                WorkflowRequestStatus.SKIPPED.toString().equals(status)) {
            try {
                RealmService realmService = IdentityWorkflowDataHolder.getInstance().getRealmService();
                UserRealm userRealm = realmService.getTenantUserRealm(tenantId);
                userRealm.getUserStoreManager().updateUserListOfRole(roleName, deletedUsers, newUsers);
                if (WorkflowRequestStatus.APPROVED.toString().equals(status)) {
                    String userNameWithoutDomain = UserCoreUtil.removeDomainFromName(roleName);
                    EntityRelationshipDAO entityRelationshipDAO = new EntityRelationshipDAO();
                    String tenant = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
                    String nameWithTenant = UserCoreUtil.addTenantDomainToEntry(userNameWithoutDomain, tenant);
                    String fullyQualifiedName = UserCoreUtil.addDomainToName(nameWithTenant, userStoreDomain);

                    String[] fullyQulalifiedNewRoleList = new String[newUsers.length];
                    String[] fullyQulalifiedDeletedRoleList = new String[deletedUsers.length];
                    for (int i = 0; i < newUsers.length; i++) {
                        nameWithTenant = UserCoreUtil.addTenantDomainToEntry(newUsers[i], tenant);
                        fullyQulalifiedNewRoleList[i] = UserCoreUtil.addDomainToName(nameWithTenant, userStoreDomain);
                    }
                    for (int i = 0; i < deletedUsers.length; i++) {
                        nameWithTenant = UserCoreUtil.addTenantDomainToEntry(deletedUsers[i], tenant);
                        fullyQulalifiedDeletedRoleList[i] = UserCoreUtil.addDomainToName(nameWithTenant,
                                userStoreDomain);
                    }
                    entityRelationshipDAO.deleteEntityRelationshipStates(fullyQualifiedName, "ROLE",
                            fullyQulalifiedNewRoleList, "USER", "ADD");
                    entityRelationshipDAO.deleteEntityRelationshipStates(fullyQualifiedName, "ROLE",
                            fullyQulalifiedDeletedRoleList, "USER", "DELETE");
                }
            } catch (UserStoreException e) {
                throw new WorkflowException("Error when re-requesting updateUserListOfRole operation for " + roleName,
                        e);
            }
        } else {
            String userNameWithoutDomain = UserCoreUtil.removeDomainFromName(roleName);
            EntityRelationshipDAO entityRelationshipDAO = new EntityRelationshipDAO();
            String tenant = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
            String nameWithTenant = UserCoreUtil.addTenantDomainToEntry(userNameWithoutDomain, tenant);
            String fullyQualifiedName = UserCoreUtil.addDomainToName(nameWithTenant, userStoreDomain);

            String[] fullyQulalifiedNewRoleList = new String[newUsers.length];
            String[] fullyQulalifiedDeletedRoleList = new String[deletedUsers.length];
            for (int i = 0; i < newUsers.length; i++) {
                nameWithTenant = UserCoreUtil.addTenantDomainToEntry(newUsers[i], tenant);
                fullyQulalifiedNewRoleList[i] = UserCoreUtil.addDomainToName(nameWithTenant, userStoreDomain);
            }
            for (int i = 0; i < deletedUsers.length; i++) {
                nameWithTenant = UserCoreUtil.addTenantDomainToEntry(deletedUsers[i], tenant);
                fullyQulalifiedDeletedRoleList[i] = UserCoreUtil.addDomainToName(nameWithTenant, userStoreDomain);
            }
            entityRelationshipDAO.deleteEntityRelationshipStates(fullyQualifiedName, "ROLE",
                    fullyQulalifiedNewRoleList, "USER", "ADD");
            entityRelationshipDAO.deleteEntityRelationshipStates(fullyQualifiedName, "ROLE",
                    fullyQulalifiedDeletedRoleList, "USER", "DELETE");
            if (retryNeedAtCallback()) {
                //unset threadlocal variable
                unsetWorkFlowCompleted();
            }
            if (log.isDebugEnabled()) {
                log.debug(
                        "Updating role users is aborted for role '" + roleName + "', Reason: Workflow response was " +
                                status);
            }
        }
    }

    /**
     * Check if updateUserListOfRole possible with given newUsers and deletedUsers
     *
     * @param fullyQualifiedRoleName
     * @param fullyQulalifiedDeletedUsers
     * @param fullyQulalifiedNewUsers
     * @return
     * @throws WorkflowException
     */
    public boolean checkRoleUpdatePossible(String fullyQualifiedRoleName, String[] fullyQulalifiedDeletedUsers,
                                           String[] fullyQulalifiedNewUsers) throws WorkflowException {

        EntityDAO entityDao = new EntityDAO();
        EntityRelationshipDAO entityRelationshipDAO = new EntityRelationshipDAO();
        if (!entityDao.checkEntityLocked(fullyQualifiedRoleName, "ROLE")) {
            throw new WorkflowException("Role is in pending state of a workflow");
        }
        if (fullyQulalifiedDeletedUsers.length > 0 && !entityDao.checkEntityListLocked(fullyQulalifiedDeletedUsers,
                "USER")) {
            throw new WorkflowException("1 or more given users are in pending state in workflows.");
        }
        if (fullyQulalifiedNewUsers.length > 0 && !entityDao.checkEntityListLocked(fullyQulalifiedNewUsers,
                "USER")) {
            throw new WorkflowException("1 or more given users are in pending state in workflows.");
        }

        if (fullyQulalifiedNewUsers.length > 0 && !entityRelationshipDAO.isEntityRelatedToOneInList
                (fullyQualifiedRoleName, "ROLE", fullyQulalifiedNewUsers, "USER")) {
            throw new WorkflowException("1 or more given users are in pending state in workflows to associate with " +
                    "same role.");
        }

        if (fullyQulalifiedDeletedUsers.length > 0 && !entityRelationshipDAO.isEntityRelatedToOneInList
                (fullyQualifiedRoleName, "ROLE", fullyQulalifiedDeletedUsers, "USER")) {
            throw new WorkflowException("1 or more given users are in pending state in workflows to associate with " +
                    "same role.");
        }
        return true;
    }
}
