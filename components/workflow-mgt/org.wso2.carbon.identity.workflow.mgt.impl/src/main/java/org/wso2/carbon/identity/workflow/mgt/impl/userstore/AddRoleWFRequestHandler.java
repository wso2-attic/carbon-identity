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
import org.wso2.carbon.identity.workflow.mgt.util.WorkflowDataType;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;
import org.wso2.carbon.identity.workflow.mgt.util.WorkflowRequestStatus;
import org.wso2.carbon.identity.workflow.mgt.impl.internal.IdentityWorkflowDataHolder;
import org.wso2.carbon.user.api.Permission;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AddRoleWFRequestHandler extends AbstractWorkflowRequestHandler {

    private static final String FRIENDLY_NAME = "Add Role";
    private static final String FRIENDLY_DESCRIPTION = "Triggered when a user create a new role.";

    private static final String ROLENAME = "Role Name";
    private static final String USER_STORE_DOMAIN = "User Store Domain";
    private static final String PERMISSIONS = "Permissions";
    private static final String USER_LIST = "Users";

    private static final String SEPARATOR = "->";

    private static final Map<String, String> PARAM_DEFINITION;
    private static Log log = LogFactory.getLog(AddRoleWFRequestHandler.class);

    static {
        PARAM_DEFINITION = new LinkedHashMap<>();
        PARAM_DEFINITION.put(ROLENAME, WorkflowDataType.STRING_TYPE);
        PARAM_DEFINITION.put(USER_STORE_DOMAIN, WorkflowDataType.STRING_TYPE);
        PARAM_DEFINITION.put(USER_LIST, WorkflowDataType.STRING_LIST_TYPE);
        PARAM_DEFINITION.put(PERMISSIONS, WorkflowDataType.STRING_LIST_TYPE);
    }

    public boolean startAddRoleFlow(String userStoreDomain, String role, String[] userList, Permission[] permissions)
            throws WorkflowException {
        if(permissions == null){
            permissions = new Permission[0];
        }
        if(userList == null){
            userList = new String[0];
        }
        List<String> permissionList = new ArrayList<>(permissions.length);
        for (int i = 0; i < permissions.length; i++) {
            permissionList.add(permissions[i].getResourceId() + SEPARATOR + permissions[i].getAction());
        }

        EntityDAO entityDAO = new EntityDAO();
        String[] fullyQulalifiedUserList = new String[userList.length];
        String tenant = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        for (int i=0;i<userList.length;i++){
            String nameWithTenant = UserCoreUtil.addTenantDomainToEntry(userList[i], tenant);
            fullyQulalifiedUserList[i] = UserCoreUtil.addDomainToName(nameWithTenant, userStoreDomain);
        }
        if(fullyQulalifiedUserList.length >0 && !entityDAO.checkEntityListLocked(fullyQulalifiedUserList, "USER") &&
                !Boolean.TRUE.equals(getWorkFlowCompleted())){
            throw new WorkflowException("Can't assign role to 1 or more given users");
        }

        String nameWithTenant = UserCoreUtil.addTenantDomainToEntry(role, tenant);
        String fullyQualifiedName = UserCoreUtil.addDomainToName(nameWithTenant, userStoreDomain);
        boolean isExistingRole = entityDAO.updateEntityLockedState(fullyQualifiedName, "ROLE", "ADD");
        if (!isExistingRole && !Boolean.TRUE.equals(getWorkFlowCompleted())) {
            throw new WorkflowException("Role has already been added before.");
        }

        Map<String, Object> wfParams = new HashMap<>();
        Map<String, Object> nonWfParams = new HashMap<>();
        wfParams.put(ROLENAME, role);
        wfParams.put(USER_STORE_DOMAIN, userStoreDomain);
        wfParams.put(PERMISSIONS, permissionList);
        wfParams.put(USER_LIST, Arrays.asList(userList));
        return startWorkFlow(wfParams, nonWfParams);
    }

    @Override
    public String getEventId() {
        return UserStoreWFConstants.ADD_ROLE_EVENT;
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
            throw new WorkflowException("Callback request for Add role received without the mandatory " +
                    "parameter 'roleName'");
        }

        String userStoreDomain = (String) requestParams.get(USER_STORE_DOMAIN);
        if (StringUtils.isNotBlank(userStoreDomain)) {
            roleName = userStoreDomain + "/" + roleName;
        }

        List<String> userList = (List<String>) requestParams.get(USER_LIST);
        String[] users;
        if (userList != null) {
            users = new String[userList.size()];
            users = userList.toArray(users);
        } else {
            users = new String[0];
        }

        List<String> permissionList = (List<String>) requestParams.get(PERMISSIONS);
        Permission[] permissions;
        if (permissionList != null) {
            permissions = new Permission[permissionList.size()];
            int i = 0;
            for (String permissionString : permissionList) {
                String[] splittedString = permissionString.split(SEPARATOR);
                if (splittedString.length == 2) {
                    permissions[i] = new Permission(splittedString[0], splittedString[1]);
                }
                i++;
            }
        } else {
            permissions = new Permission[0];
        }

        if (WorkflowRequestStatus.APPROVED.toString().equals(status) ||
                WorkflowRequestStatus.SKIPPED.toString().equals(status)) {
            try {
                RealmService realmService = IdentityWorkflowDataHolder.getInstance().getRealmService();
                UserRealm userRealm = realmService.getTenantUserRealm(tenantId);
                userRealm.getUserStoreManager().addRole(roleName, users, permissions);
                if (WorkflowRequestStatus.APPROVED.toString().equals(status)) {
                    EntityDAO entityDAO = new EntityDAO();
                    String tenant = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
                    String nameWithTenant = UserCoreUtil.addTenantDomainToEntry(UserCoreUtil.removeDomainFromName
                                    (roleName),
                            tenant);
                    String fullyQualifiedName = UserCoreUtil.addDomainToName(nameWithTenant, userStoreDomain);
                    entityDAO.deleteEntityLockedState(fullyQualifiedName, "ROLE", "ADD");
                }
            } catch (UserStoreException e) {
                throw new WorkflowException("Error when re-requesting addRole operation for " + roleName, e);
            }
        } else {
            EntityDAO entityDAO = new EntityDAO();
            String tenant = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
            String nameWithTenant = UserCoreUtil.addTenantDomainToEntry(UserCoreUtil.removeDomainFromName(roleName),
                    tenant);
            String fullyQualifiedName = UserCoreUtil.addDomainToName(nameWithTenant, userStoreDomain);
            entityDAO.deleteEntityLockedState(fullyQualifiedName, "ROLE", "ADD");

            if (retryNeedAtCallback()) {
                //unset threadlocal variable
                unsetWorkFlowCompleted();
            }
            if (log.isDebugEnabled()) {
                log.debug(
                        "Adding role is aborted for role '" + roleName + "', Reason: Workflow response was " + status);
            }
        }
    }
}
