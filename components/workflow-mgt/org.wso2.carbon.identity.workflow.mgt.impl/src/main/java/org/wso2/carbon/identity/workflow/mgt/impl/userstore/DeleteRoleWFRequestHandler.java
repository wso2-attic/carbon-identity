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
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;
import org.wso2.carbon.identity.workflow.mgt.util.WorkflowRequestStatus;
import org.wso2.carbon.identity.workflow.mgt.impl.internal.IdentityWorkflowDataHolder;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class DeleteRoleWFRequestHandler extends AbstractWorkflowRequestHandler {

    private static final String FRIENDLY_NAME = "Delete Role";
    private static final String FRIENDLY_DESCRIPTION = "Triggered when a user deletes a role.";

    private static final String ROLENAME = "Role Name";
    private static final String USER_STORE_DOMAIN = "User Store Domain";

    private static final Map<String, String> PARAM_DEFINITION;
    private static Log log = LogFactory.getLog(DeleteRoleWFRequestHandler.class);


    static {
        PARAM_DEFINITION = new LinkedHashMap<>();
        PARAM_DEFINITION.put(ROLENAME, WorkflowDataType.STRING_TYPE);
        PARAM_DEFINITION.put(USER_STORE_DOMAIN, WorkflowDataType.STRING_TYPE);
    }

    public boolean startDeleteRoleFlow(String userStoreDomain, String roleName) throws WorkflowException {
        Map<String, Object> wfParams = new HashMap<>();
        Map<String, Object> nonWfParams = new HashMap<>();
        UserStoreManager userStoreManager;
        if (!Boolean.TRUE.equals(getWorkFlowCompleted())) {
            String[] userListOfRole = new String[0];
            try {
                userStoreManager = ((AbstractUserStoreManager) CarbonContext.getThreadLocalCarbonContext()
                        .getUserRealm().getUserStoreManager()).getSecondaryUserStoreManager(userStoreDomain);
                userListOfRole = userStoreManager.getUserListOfRole(roleName);
            } catch (UserStoreException e) {
                throw new WorkflowException("Error while retrieving userStoreManager.", e);
            }
            EntityDAO entityDAO = new EntityDAO();
            EntityRelationshipDAO entityRelationshipDAO = new EntityRelationshipDAO();
            String[] fullyQulalifiedUserList = new String[userListOfRole.length];
            String tenant = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
            for (int i = 0; i < userListOfRole.length; i++) {
                String nameWithTenant = UserCoreUtil.addTenantDomainToEntry(userListOfRole[i], tenant);
                fullyQulalifiedUserList[i] = UserCoreUtil.addDomainToName(nameWithTenant, userStoreDomain);
            }
            if (fullyQulalifiedUserList.length > 0 && !entityDAO.checkEntityListLocked(fullyQulalifiedUserList,
                    "USER")) {
                throw new WorkflowException("1 or more users of the role are in pending workflow states.");
            }
            String nameWithTenant = UserCoreUtil.addTenantDomainToEntry(roleName, tenant);
            String fullyQualifiedName = UserCoreUtil.addDomainToName(nameWithTenant, userStoreDomain);
            boolean alreadyDeleted = entityDAO.updateEntityLockedState(fullyQualifiedName, "ROLE", "DELETE");
            if (!alreadyDeleted) {
                throw new WorkflowException("Already deleted role.");
            }
            if (!entityRelationshipDAO.checkIfEntityHasAnyRelationShip(fullyQualifiedName, "ROLE")) {
                throw new WorkflowException("Role has pending workflows.");
            }
        }
        wfParams.put(ROLENAME, roleName);
        wfParams.put(USER_STORE_DOMAIN, userStoreDomain);
        return startWorkFlow(wfParams, nonWfParams);
    }

    @Override
    public void onWorkflowCompletion(String status, Map<String, Object> requestParams,
                                     Map<String, Object> responseAdditionalParams, int tenantId)
            throws WorkflowException {
        String roleName = (String) requestParams.get(ROLENAME);
        if (roleName == null) {
            throw new WorkflowException("Callback request for delete role received without the mandatory " +
                    "parameter 'username'");
        }

        String userStoreDomain = (String) requestParams.get(USER_STORE_DOMAIN);
        if (StringUtils.isNotBlank(userStoreDomain)) {
            roleName = userStoreDomain + "/" + roleName;
        }

        if (WorkflowRequestStatus.APPROVED.toString().equals(status) ||
                WorkflowRequestStatus.SKIPPED.toString().equals(status)) {
            try {
                RealmService realmService = IdentityWorkflowDataHolder.getInstance().getRealmService();
                UserRealm userRealm = realmService.getTenantUserRealm(tenantId);
                userRealm.getUserStoreManager().deleteRole(roleName);
                if (WorkflowRequestStatus.APPROVED.toString().equals(status)) {
                    String roleNameWithoutDomain = UserCoreUtil.removeDomainFromName(roleName);
                    EntityDAO entityDAO = new EntityDAO();
                    String tenant = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
                    String nameWithTenant = UserCoreUtil.addTenantDomainToEntry(roleNameWithoutDomain, tenant);
                    String fullyQualifiedName = UserCoreUtil.addDomainToName(nameWithTenant, userStoreDomain);
                    entityDAO.deleteEntityLockedState(fullyQualifiedName, "ROLE", "DELETE");
                }
            } catch (UserStoreException e) {
                throw new WorkflowException("Error when re-requesting deleteRole operation for " + roleName, e);
            }
        } else {
            String roleNameWithoutDomain = UserCoreUtil.removeDomainFromName(roleName);
            EntityDAO entityDAO = new EntityDAO();
            String tenant = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
            String nameWithTenant = UserCoreUtil.addTenantDomainToEntry(roleNameWithoutDomain, tenant);
            String fullyQualifiedName = UserCoreUtil.addDomainToName(nameWithTenant, userStoreDomain);
            entityDAO.deleteEntityLockedState(fullyQualifiedName, "ROLE", "DELETE");
            if (retryNeedAtCallback()) {
                //unset threadlocal variable
                unsetWorkFlowCompleted();
            }
            if (log.isDebugEnabled()) {
                log.debug("Deleting role is aborted for role '" + roleName + "', Reason: Workflow response was " +
                        status);
            }
        }
    }

    @Override
    public boolean retryNeedAtCallback() {
        return true;
    }

    @Override
    public String getEventId() {
        return UserStoreWFConstants.DELETE_ROLE_EVENT;
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

}
