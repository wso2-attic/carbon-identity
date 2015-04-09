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

package org.wso2.carbon.identity.workflow.mgt.impl.userstore;

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.identity.workflow.mgt.AbstractWorkflowRequestHandler;
import org.wso2.carbon.identity.workflow.mgt.WorkflowDataType;
import org.wso2.carbon.identity.workflow.mgt.WorkflowException;
import org.wso2.carbon.identity.workflow.mgt.impl.internal.IdentityWorkflowServiceComponent;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.HashMap;
import java.util.Map;

public class DeleteUserWFRequestHandler extends AbstractWorkflowRequestHandler {

    private static final String USERNAME = "username";
    private static final String USER_STORE_DOMAIN = "userStoreDomain";

    private static final Map<String, String> PARAM_DEFINITION;

    static {
        PARAM_DEFINITION = new HashMap<>();
        PARAM_DEFINITION.put(USERNAME, WorkflowDataType.STRING_TYPE);
        PARAM_DEFINITION.put(USER_STORE_DOMAIN, WorkflowDataType.STRING_TYPE);
    }

    public boolean startDeleteUserFlow(String userStoreDomain, String userName) throws WorkflowException {
        Map<String, Object> wfParams = new HashMap<>();
        Map<String, Object> nonWfParams = new HashMap<>();
        wfParams.put(USERNAME, userName);
        wfParams.put(USER_STORE_DOMAIN, userStoreDomain);
        return startWorkFlow(wfParams, nonWfParams);
    }

    @Override
    public void onWorkflowCompletion(String status, Map<String, Object> requestParams,
                                     Map<String, Object> responseAdditionalParams, int tenantId)
            throws WorkflowException {
        String userName = "";
        Object requestUsername = requestParams.get(USERNAME);
        if (requestUsername == null || !(requestUsername instanceof String)) {
            throw new WorkflowException("Callback request for delete user received without the mandatory " +
                    "parameter 'username'");
        }
        String userStoreDomain = (String) requestParams.get(USER_STORE_DOMAIN);
        if (StringUtils.isNotBlank(userStoreDomain)) {
            userName = userStoreDomain + "/" + requestUsername;
        } else {
            userName = (String) requestUsername;
        }
        try {
            RealmService realmService = IdentityWorkflowServiceComponent.getRealmService();
            UserRealm userRealm = realmService.getTenantUserRealm(tenantId);
            userRealm.getUserStoreManager().deleteUser(userName);
        } catch (UserStoreException e) {
            throw new WorkflowException("Error when re-requesting addUser operation for " + userName, e);
        }
    }

    @Override
    public boolean retryNeedAtCallback() {
        return true;
    }

    @Override
    public String getEventId() {
        return UserStoreWFConstants.DELETE_USER_EVENT;
    }

    @Override
    public Map<String, String> getParamDefinitions() {
        return PARAM_DEFINITION;
    }
}
