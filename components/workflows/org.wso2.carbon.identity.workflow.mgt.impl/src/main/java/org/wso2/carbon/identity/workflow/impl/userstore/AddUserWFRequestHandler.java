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

package org.wso2.carbon.identity.workflow.impl.userstore;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.workflow.impl.internal.IdentityWorkflowServiceComponent;
import org.wso2.carbon.identity.workflow.mgt.AbstractWorkflowRequestHandler;
import org.wso2.carbon.identity.workflow.mgt.WorkflowDataType;
import org.wso2.carbon.identity.workflow.mgt.WorkflowException;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddUserWFRequestHandler extends AbstractWorkflowRequestHandler {

    private static final String USERNAME = "username";
    private static final String USER_STORE_DOMAIN = "userStoreDomain";
    private static final String CREDENTIAL = "credential";
    private static final String ROLE_LIST = "roleList";
    private static final String CLAIM_LIST = "claimList";
    private static final String PROFILE = "profile";

    private static final Map<String, String> PARAM_DEFINITION;
    private static Log log = LogFactory.getLog(AddUserWFRequestHandler.class);

    static {
        PARAM_DEFINITION = new HashMap<>();
        PARAM_DEFINITION.put(USERNAME, WorkflowDataType.STRING_TYPE);
        PARAM_DEFINITION.put(USER_STORE_DOMAIN, WorkflowDataType.STRING_TYPE);
        PARAM_DEFINITION.put(CREDENTIAL, WorkflowDataType.STRING_TYPE);
        PARAM_DEFINITION.put(PROFILE, WorkflowDataType.STRING_TYPE);
        PARAM_DEFINITION.put(ROLE_LIST, WorkflowDataType.STRING_LIST_TYPE);
        PARAM_DEFINITION.put(CLAIM_LIST, WorkflowDataType.STRING_STRING_MAP_TYPE);
    }

    /**
     * Starts the workflow execution
     *
     * @param userStoreDomain
     * @param userName
     * @param credential
     * @param roleList
     * @param claims
     * @param profile
     * @return <code>true</code> if the workflow request is ready to be continued (i.e. has been approved from
     * workflow) <code>false</code> otherwise (i.e. request placed for approval)
     * @throws WorkflowException
     */
    public boolean startAddUserFlow(String userStoreDomain, String userName, Object credential, String[] roleList,
                                    Map<String, String> claims, String profile) throws WorkflowException {
        Map<String, Object> wfParams = new HashMap<>();
        Map<String, Object> nonWfParams = new HashMap<>();
        wfParams.put(USERNAME, userName);
        wfParams.put(USER_STORE_DOMAIN, userStoreDomain);
        wfParams.put(ROLE_LIST, Arrays.asList(roleList));
        wfParams.put(CLAIM_LIST, claims);
        wfParams.put(PROFILE, profile);
        nonWfParams.put(CREDENTIAL, credential.toString());
        startWorkFlow(wfParams, nonWfParams);
        return false;
    }

    @Override
    public String getEventId() {
        return UserStoreWFConstants.ADD_USER_EVENT;
    }

    @Override
    public Map<String, String> getParamDefinitions() {
        return PARAM_DEFINITION;
    }

    @Override
    public boolean retryNeedAtCallback() {
        return true;
    }

    @Override
    public void onWorkflowCompletion(String status, Map<String, Object> requestParams, Map<String, Object>
            responseAdditionalParams, int tenantId) throws WorkflowException {
        String userName = "";
        Object credential = null;
        String[] roles = null;
        Map<String, String> claims = null;
        String profile = null;
        Object requestUsername = requestParams.get(USERNAME);
        if (requestUsername == null || !(requestUsername instanceof String)) {
            throw new WorkflowException("Callback request for Add User received without the mandatory " +
                    "parameter 'username'");
        }
        String userStoreDomain = (String) requestParams.get(USER_STORE_DOMAIN);
        if (StringUtils.isNotBlank(userStoreDomain)) {
            userName = userStoreDomain + "/" + requestUsername;
        } else {
            userName = (String) requestUsername;
        }
        credential = requestParams.get(CREDENTIAL);
        List<String> roleList = ((List<String>) requestParams.get(ROLE_LIST));
        if (roleList != null) {
            roles = new String[roleList.size()];
            roles = roleList.toArray(roles);
        } else {
            roles = new String[0];
        }
        claims = (Map<String, String>) requestParams.get(CLAIM_LIST);
        profile = (String) requestParams.get(PROFILE);

        try {
            RealmService realmService = IdentityWorkflowServiceComponent.getRealmService();
            UserRealm userRealm = realmService.getTenantUserRealm(tenantId);
            userRealm.getUserStoreManager().addUser(userName, credential, roles, claims, profile);
        } catch (UserStoreException e) {
            throw new WorkflowException("Error when re-requesting addUser operation for " + userName, e);
        }
    }
}
