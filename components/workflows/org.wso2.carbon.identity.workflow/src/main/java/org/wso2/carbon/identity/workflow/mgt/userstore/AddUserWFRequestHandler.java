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

package org.wso2.carbon.identity.workflow.mgt.userstore;

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.workflow.mgt.AbstractWorkflowRequestHandler;
import org.wso2.carbon.workflow.mgt.WorkflowException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AddUserWFRequestHandler extends AbstractWorkflowRequestHandler {

    private static final String USERNAME = "username";
    private static final String CREDENTIAL = "credential";
    private static final String ROLE_LIST = "roleList";
    private static final String CLAIM_LIST = "claimList";
    private static final String PROFILE = "profile";

    private static final String APPROVED_ID_CLAIM = "approvalId";

    private static Set<String> pendingRequests = new HashSet<String>(); //todo:move to cache or db
    private static Set<String> approvedRequests = new HashSet<String>(); //todo:move to cache or db

    public boolean startAddUserFlow(String userName, Object credential, String[] roleList, Map<String, String> claims,
                                    String profile) throws WorkflowException {
        if (claims != null) {
            String approvalId = claims.get(APPROVED_ID_CLAIM);
            if (StringUtils.isNotBlank(approvalId) && approvedRequests.contains(approvalId)) {
                return true;
            }
        }
        if (!pendingRequests.contains(userName)) {
            Map<String, Object> wfParams = new HashMap<String, Object>();
            wfParams.put(USERNAME, userName);
//            wfParams.put(CREDENTIAL,credential);
            wfParams.put(ROLE_LIST, roleList);
            wfParams.put(CLAIM_LIST, claims);
            wfParams.put(PROFILE, profile);
            Map<String, Object> nonWfParams = new HashMap<String, Object>();
            nonWfParams.put(CREDENTIAL, credential);
            engageWorkflow(wfParams, nonWfParams);
        }
        //else : possible duplicate request, hence ignoring

        return false;
    }

    @Override
    public String getActionIdentifier() {
        return UserStoreWFConstants.ADD_USER_REQUESTER;
    }

    @Override
    public void onWorkflowCompletion(String status, Map<String, Object> requestParams, Map<String, Object>
            responseAdditionalParams) {
        String userName = "";
        Object credential = null;
        String[] roleList = null;
        Map<String, String> claims = null;
        String profile = null;
        Object requestUsername = requestParams.get(USERNAME);
        if (requestUsername == null || !(requestUsername instanceof String)) {
            //error: not possible
        }
        userName = (String) requestUsername;
        credential = requestParams.get(CREDENTIAL);
        roleList = (String[]) requestParams.get(ROLE_LIST);
        claims = (Map<String, String>) requestParams.get(CLAIM_LIST);
        profile = (String) requestParams.get(PROFILE);
        if (pendingRequests.contains(userName)) {
            pendingRequests.remove(userName);
        }
        //todo error if not?
        try {
            //add a uuid as a claim to identify and filter out the request from the next execution
            if (claims == null) {
                claims = new HashMap<String, String>();
            }
            String uuid = UUID.randomUUID().toString();
            claims.put(APPROVED_ID_CLAIM, uuid);
            approvedRequests.add(uuid);
            CarbonContext.getThreadLocalCarbonContext().getUserRealm().getUserStoreManager().addUser(userName,
                    credential, roleList, claims, profile);
        } catch (UserStoreException e) {
            //todo
        }
    }
}
