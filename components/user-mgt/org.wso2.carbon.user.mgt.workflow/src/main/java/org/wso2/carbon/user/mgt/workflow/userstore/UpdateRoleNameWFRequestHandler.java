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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.workflow.mgt.WorkflowService;
import org.wso2.carbon.identity.workflow.mgt.bean.Entity;
import org.wso2.carbon.identity.workflow.mgt.exception.InternalWorkflowException;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;
import org.wso2.carbon.identity.workflow.mgt.extension.AbstractWorkflowRequestHandler;
import org.wso2.carbon.identity.workflow.mgt.util.WorkflowDataType;
import org.wso2.carbon.identity.workflow.mgt.util.WorkflowRequestStatus;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.util.UserCoreUtil;
import org.wso2.carbon.user.mgt.workflow.internal.IdentityWorkflowDataHolder;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class UpdateRoleNameWFRequestHandler extends AbstractWorkflowRequestHandler {

    private static final String FRIENDLY_NAME = "Update Rolename";
    private static final String FRIENDLY_DESCRIPTION = "Triggered when a role name is updates";

    private static final String ROLENAME = "Role Name";
    private static final String NEW_ROLENAME = "New Role Name";
    private static final String USER_STORE_DOMAIN = "User Store Domain";

    private static final Map<String, String> PARAM_DEFINITION;
    private static Log log = LogFactory.getLog(DeleteRoleWFRequestHandler.class);


    static {
        PARAM_DEFINITION = new LinkedHashMap<>();
        PARAM_DEFINITION.put(ROLENAME, WorkflowDataType.STRING_TYPE);
        PARAM_DEFINITION.put(NEW_ROLENAME, WorkflowDataType.STRING_TYPE);
        PARAM_DEFINITION.put(USER_STORE_DOMAIN, WorkflowDataType.STRING_TYPE);
    }

    public boolean startUpdateRoleNameFlow(String userStoreDomain, String roleName, String newRoleName) throws
            WorkflowException {

        WorkflowService workflowService = IdentityWorkflowDataHolder.getInstance().getWorkflowService();
        int tenant = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        String fullyQualifiedOldName = UserCoreUtil.addDomainToName(roleName, userStoreDomain);
        String fullyQualifiedNewName = UserCoreUtil.addDomainToName(newRoleName, userStoreDomain);
        Map<String, Object> wfParams = new HashMap<>();
        Map<String, Object> nonWfParams = new HashMap<>();
        wfParams.put(ROLENAME, roleName);
        wfParams.put(NEW_ROLENAME, newRoleName);
        wfParams.put(USER_STORE_DOMAIN, userStoreDomain);
        String uuid = UUID.randomUUID().toString();
        if (workflowService.eventEngagedWithWorkflows(UserStoreWFConstants.UPDATE_ROLE_NAME_EVENT) && !Boolean.TRUE
                .equals(getWorkFlowCompleted()) && !isValidOperation(new Entity[]{new Entity(fullyQualifiedOldName,
                UserStoreWFConstants.ENTITY_TYPE_ROLE, tenant), new Entity(fullyQualifiedNewName,
                UserStoreWFConstants.ENTITY_TYPE_ROLE, tenant)})) {
            throw new WorkflowException("Operation is not valid.");
        }
        boolean state = startWorkFlow(wfParams, nonWfParams, uuid);

        //WF_REQUEST_ENTITY_RELATIONSHIP table has foreign key to WF_REQUEST, so need to run this after WF_REQUEST is
        // updated
        if (!Boolean.TRUE.equals(getWorkFlowCompleted()) && !state) {
            try {
                workflowService.addRequestEntityRelationships(uuid, new Entity[]{new Entity(fullyQualifiedOldName,
                        UserStoreWFConstants.ENTITY_TYPE_ROLE, tenant), new Entity(fullyQualifiedNewName,
                        UserStoreWFConstants.ENTITY_TYPE_ROLE, tenant)});

            } catch (InternalWorkflowException e) {
                //debug exception which occurs at DB level since no workflows associated with event
                if (log.isDebugEnabled()) {
                    log.debug("No workflow associated with the operation.", e);
                }
            }
        }
        return state;
    }

    @Override
    public void onWorkflowCompletion(String status, Map<String, Object> requestParams,
                                     Map<String, Object> responseAdditionalParams, int tenantId)
            throws WorkflowException {
        String roleName = (String) requestParams.get(ROLENAME);
        String newRoleName = (String) requestParams.get(NEW_ROLENAME);
        if (roleName == null) {
            throw new WorkflowException("Callback request for rename role received without the mandatory " +
                    "parameter 'roleName'");
        }
        if (newRoleName == null) {
            throw new WorkflowException("Callback request for rename role received without the mandatory " +
                    "parameter 'newRoleName'");
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
                userRealm.getUserStoreManager().updateRoleName(roleName, newRoleName);
            } catch (UserStoreException e) {
                log.error("Error when re-requesting updateRoleName operation for " + roleName, e);
                throw new WorkflowException(e);
            }
        } else {
            if (retryNeedAtCallback()) {
                //unset threadlocal variable
                unsetWorkFlowCompleted();
            }
            if (log.isDebugEnabled()) {
                log.debug("Updating role is aborted for role '" + roleName + "', Reason: Workflow response was " +
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
        return UserStoreWFConstants.UPDATE_ROLE_NAME_EVENT;
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
    public boolean isValidOperation(Entity[] entities) throws WorkflowException {

        WorkflowService workflowService = IdentityWorkflowDataHolder.getInstance().getWorkflowService();
        for (int i = 0; i < entities.length; i++) {
            if (entities[i].getEntityType() == UserStoreWFConstants.ENTITY_TYPE_ROLE && workflowService
                        .entityHasPendingWorkflows(entities[i])) {

                    throw new WorkflowException("Role has pending workflows which  blocks this operation.");
            }
        }
        return true;
    }
}
