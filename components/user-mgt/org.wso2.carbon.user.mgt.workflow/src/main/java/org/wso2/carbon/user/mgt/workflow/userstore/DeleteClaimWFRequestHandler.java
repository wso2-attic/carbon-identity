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
import org.wso2.carbon.user.core.util.UserCoreUtil;
import org.wso2.carbon.user.mgt.workflow.internal.IdentityWorkflowDataHolder;
import org.wso2.carbon.identity.workflow.mgt.extension.AbstractWorkflowRequestHandler;
import org.wso2.carbon.identity.workflow.mgt.util.WorkflowDataType;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;
import org.wso2.carbon.identity.workflow.mgt.util.WorkflowRequestStatus;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class DeleteClaimWFRequestHandler extends AbstractWorkflowRequestHandler {

    private static final String FRIENDLY_NAME = "Delete User Claim";
    private static final String FRIENDLY_DESCRIPTION = "Triggered when a user's claim is deleted";

    private static final String USERNAME = "Username";
    private static final String USER_STORE_DOMAIN = "User Store Domain";
    private static final String CLAIM_URI = "Claim URI";
    private static final String PROFILE_NAME = "Profile";

    private static final Map<String, String> PARAM_DEFINITION;
    private static Log log = LogFactory.getLog(AddUserWFRequestHandler.class);

    static {
        PARAM_DEFINITION = new LinkedHashMap<>();
        PARAM_DEFINITION.put(USERNAME, WorkflowDataType.STRING_TYPE);
        PARAM_DEFINITION.put(USER_STORE_DOMAIN, WorkflowDataType.STRING_TYPE);
        PARAM_DEFINITION.put(CLAIM_URI, WorkflowDataType.STRING_TYPE);
        PARAM_DEFINITION.put(PROFILE_NAME, WorkflowDataType.STRING_TYPE);
    }

    public boolean startDeleteClaimWorkflow(String userStoreDomain, String userName, String claimURI, String
            profileName) throws WorkflowException {

        WorkflowService workflowService = IdentityWorkflowDataHolder.getInstance().getWorkflowService();

        String tenant = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        String nameWithTenant = UserCoreUtil.addTenantDomainToEntry(userName, tenant);
        String fullyQualifiedName = UserCoreUtil.addDomainToName(nameWithTenant, userStoreDomain);

        Map<String, Object> wfParams = new HashMap<>();
        Map<String, Object> nonWfParams = new HashMap<>();
        wfParams.put(USERNAME, userName);
        wfParams.put(USER_STORE_DOMAIN, userStoreDomain);
        wfParams.put(CLAIM_URI, claimURI);
        wfParams.put(PROFILE_NAME, profileName);
        String uuid = UUID.randomUUID().toString();
        boolean state = startWorkFlow(wfParams, nonWfParams, uuid);

        //WF_REQUEST_ENTITY_RELATIONSHIP table has foreign key to WF_REQUEST, so need to run this after WF_REQUEST is
        // updated
        if (!Boolean.TRUE.equals(getWorkFlowCompleted()) && !state) {
            try {
                workflowService.addRequestEntityRelationships(uuid, new Entity[]{new Entity(fullyQualifiedName,
                        "USER"), new Entity(claimURI, "CLAIM")});

            } catch (InternalWorkflowException e) {
                //Ignore exception which occurs at DB level since no workflows associated with event
                log.info("No workflow associated with the operation.");
            }
        }
        return state;
    }

    @Override
    public void onWorkflowCompletion(String status, Map<String, Object> requestParams,
                                     Map<String, Object> responseAdditionalParams, int tenantId)
            throws WorkflowException {
        String userName;
        Object requestUsername = requestParams.get(USERNAME);
        if (requestUsername == null || !(requestUsername instanceof String)) {
            throw new WorkflowException("Callback request for Set User Claim received without the mandatory " +
                    "parameter 'username'");
        }
        String userStoreDomain = (String) requestParams.get(USER_STORE_DOMAIN);
        if (StringUtils.isNotBlank(userStoreDomain)) {
            userName = userStoreDomain + "/" + requestUsername;
        } else {
            userName = (String) requestUsername;
        }

        String claimURI = (String) requestParams.get(CLAIM_URI);
        String profile = (String) requestParams.get(PROFILE_NAME);

        if (WorkflowRequestStatus.APPROVED.toString().equals(status) ||
                WorkflowRequestStatus.SKIPPED.toString().equals(status)) {
            try {
                RealmService realmService = IdentityWorkflowDataHolder.getInstance().getRealmService();
                UserRealm userRealm = realmService.getTenantUserRealm(tenantId);
                userRealm.getUserStoreManager().deleteUserClaimValue(userName, claimURI, profile);
            } catch (UserStoreException e) {
                throw new WorkflowException("Error when re-requesting deleteUserClaimValue operation for " + userName,
                        e);
            }
        } else {
            if (retryNeedAtCallback()) {
                //unset threadlocal variable
                unsetWorkFlowCompleted();
            }
            if (log.isDebugEnabled()) {
                log.debug("Deleting User Claim is aborted for user '" + userName + "', ClaimURI:" + claimURI +
                        ", Reason: Workflow response was " + status);
            }
        }
    }

    @Override
    public boolean retryNeedAtCallback() {
        return true;
    }

    @Override
    public String getEventId() {
        return UserStoreWFConstants.DELETE_USER_CLAIM_EVENT;
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
