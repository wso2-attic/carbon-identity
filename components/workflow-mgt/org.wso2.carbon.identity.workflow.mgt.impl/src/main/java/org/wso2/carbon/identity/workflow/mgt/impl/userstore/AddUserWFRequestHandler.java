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
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.persistence.JDBCPersistenceManager;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.workflow.mgt.dao.SQLConstants;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;
import org.wso2.carbon.identity.workflow.mgt.extension.AbstractWorkflowRequestHandler;
import org.wso2.carbon.identity.workflow.mgt.impl.internal.IdentityWorkflowDataHolder;
import org.wso2.carbon.identity.workflow.mgt.util.WorkflowDataType;
import org.wso2.carbon.identity.workflow.mgt.util.WorkflowRequestStatus;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AddUserWFRequestHandler extends AbstractWorkflowRequestHandler {

    private static final String FRIENDLY_NAME = "Add User";
    private static final String FRIENDLY_DESCRIPTION = "Triggered when a new user is created.";

    private static final String USERNAME = "Username";
    private static final String USER_STORE_DOMAIN = "User Store Domain";
    private static final String CREDENTIAL = "Credential";
    private static final String ROLE_LIST = "Roles";
    private static final String CLAIM_LIST = "Claims";
    private static final String PROFILE = "Profile";

    private static final Map<String, String> PARAM_DEFINITION;
    private static Log log = LogFactory.getLog(AddUserWFRequestHandler.class);

    static {
        PARAM_DEFINITION = new LinkedHashMap<>();
        PARAM_DEFINITION.put(USERNAME, WorkflowDataType.STRING_TYPE);
        PARAM_DEFINITION.put(USER_STORE_DOMAIN, WorkflowDataType.STRING_TYPE);
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
        if (roleList == null) {
            roleList = new String[0];
        }
        if (claims == null) {
            claims = new HashMap<>();
        }
        String tenant = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        String nameWithTenant = UserCoreUtil.addTenantDomainToEntry(userName,tenant);
        String fullyQualifiedName = UserCoreUtil.addDomainToName(nameWithTenant,userStoreDomain);
        boolean isExistingUser = updateEntityLockedState(fullyQualifiedName, "USER", "ADD");
        if(!isExistingUser){
            return false;
        }
        wfParams.put(USERNAME, userName);
        wfParams.put(USER_STORE_DOMAIN, userStoreDomain);
        wfParams.put(ROLE_LIST, Arrays.asList(roleList));
        wfParams.put(CLAIM_LIST, claims);
        wfParams.put(PROFILE, profile);
        nonWfParams.put(CREDENTIAL, credential.toString());
        return startWorkFlow(wfParams, nonWfParams);
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

        String userName;
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
        Object credential = requestParams.get(CREDENTIAL);
        List<String> roleList = ((List<String>) requestParams.get(ROLE_LIST));
        String[] roles;
        if (roleList != null) {
            roles = new String[roleList.size()];
            roles = roleList.toArray(roles);
        } else {
            roles = new String[0];
        }
        Map<String, String> claims = (Map<String, String>) requestParams.get(CLAIM_LIST);
        String profile = (String) requestParams.get(PROFILE);

        if (WorkflowRequestStatus.APPROVED.toString().equals(status) ||
                WorkflowRequestStatus.SKIPPED.toString().equals(status)) {
            try {
                RealmService realmService = IdentityWorkflowDataHolder.getInstance().getRealmService();
                UserRealm userRealm = realmService.getTenantUserRealm(tenantId);
                userRealm.getUserStoreManager().addUser(userName, credential, roles, claims, profile);
                String tenant = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
                String nameWithTenant = UserCoreUtil.addTenantDomainToEntry(userName,tenant);
                String fullyQualifiedName = UserCoreUtil.addDomainToName(nameWithTenant,userStoreDomain);
                deleteEntityLockedState(fullyQualifiedName,"USER","ADD");
            } catch (UserStoreException e) {
                throw new WorkflowException("Error when re-requesting addUser operation for " + userName, e);
            }
        } else {
            if (retryNeedAtCallback()) {
                //unset threadlocal variable
                unsetWorkFlowCompleted();
            }
            if (log.isDebugEnabled()) {
                log.debug(
                        "Adding user is aborted for user '" + userName + "', Reason: Workflow response was " + status);
            }
        }
    }

    private boolean updateEntityLockedState(String entityName, String entityType, String operation) throws WorkflowException{

        Connection connection = null;
        PreparedStatement prepStmtGet = null;
        PreparedStatement prepStmtSelect = null;
        ResultSet results;
        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            prepStmtGet = connection.prepareStatement(SQLConstants.GET_ENTITY_STATE_QUERY);
            prepStmtGet.setString(1, entityName);
            prepStmtGet.setString(2, entityType);
            prepStmtGet.setString(3, "Operation");
            prepStmtGet.setString(4, operation);
            results = prepStmtGet.executeQuery();
            if (results.next()) {
                return false;
            }else{
                prepStmtSelect = connection.prepareStatement(SQLConstants.ADD_ENTITY_STATE_QUERY);
                prepStmtSelect.setString(1, entityName);
                prepStmtSelect.setString(2, entityType);
                prepStmtSelect.setString(3, "Operation");
                prepStmtSelect.setString(4, operation);
                prepStmtSelect.execute();
            }
            connection.commit();
        } catch (SQLException | IdentityException e) {
            log.error("Error while saving new user data for Identity database.", e);
            throw new WorkflowException("Error while saving new user data for Identity database.", e);
        } finally {
            IdentityDatabaseUtil.closeStatement(prepStmtSelect);
            IdentityDatabaseUtil.closeStatement(prepStmtGet);
            IdentityDatabaseUtil.closeConnection(connection);
        }
        return true;
    }

    private void deleteEntityLockedState(String entityName, String entityType, String operation) throws
            WorkflowException{

        Connection connection = null;
        PreparedStatement prepStmt = null;
        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            prepStmt = connection.prepareStatement(SQLConstants.DELETE_ENTITY_STATE_QUERY);
            prepStmt.setString(1, entityName);
            prepStmt.setString(2, entityType);
            prepStmt.setString(3, "Operation");
            prepStmt.setString(4, operation);
            prepStmt.execute();
            connection.commit();
        } catch (SQLException | IdentityException e) {
            log.error("Error while saving new user data for Identity database.", e);
            throw new WorkflowException("Error while deleting temporary user record from Identity database.", e);
        } finally {
            IdentityDatabaseUtil.closeStatement(prepStmt);
            IdentityDatabaseUtil.closeConnection(connection);
        }
    }
}
