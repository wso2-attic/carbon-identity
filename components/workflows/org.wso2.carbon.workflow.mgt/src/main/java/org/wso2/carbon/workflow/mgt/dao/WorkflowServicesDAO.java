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

package org.wso2.carbon.workflow.mgt.dao;

import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.workflow.mgt.WorkflowException;
import org.wso2.carbon.workflow.mgt.bean.WSServiceBean;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class WorkflowServicesDAO {

    /**
     * Stores Workflow executor service details
     *
     * @param workflowService The service to be stored
     * @throws WorkflowException
     */
    public void addWorkflowService(WSServiceBean workflowService) throws WorkflowException {
        Connection connection = null;
        PreparedStatement prepStmt = null;

        String query = SQLConstants.ADD_WS_SERVICE_QUERY;
        try {
            connection = IdentityDatabaseUtil.getDBConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, workflowService.getAlias());
            prepStmt.setString(2, workflowService.getWsAction());
            prepStmt.setString(3, workflowService.getServiceEndpoint());
            prepStmt.setInt(4, workflowService.getPriority());
            prepStmt.setString(5, workflowService.getUserName());
            prepStmt.setString(6, new String(workflowService.getPassword()));   //todo: encrypt pw?
            prepStmt.executeUpdate();
            connection.commit();
        } catch (IdentityException e) {
            throw new WorkflowException("Error when connecting to the Identity Database.", e);
        } catch (SQLException e) {
            throw new WorkflowException("Error when executing the sql query:" + query, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
    }

    /**
     * Stores the association of workflow executor service to a event type with the condition.
     *
     * @param serviceAlias The service alias
     * @param eventId      The event to be subscribed
     * @param condition    The condition to be match as a XPath Expression.
     */
    public void associateServiceWithEvent(String serviceAlias, String eventId, String condition) {
    }

    /**
     * Gets a map where the keys are the Services that are configured for the event and the values are the
     * condition on which they are called.
     *
     * @param eventId
     * @return
     */
    public Map<WSServiceBean, String> getSubscribedServicesForEvent(String eventId) throws WorkflowException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        Map<WSServiceBean, String> servicesMatched = new HashMap<WSServiceBean, String>();
        String query = SQLConstants.GET_WS_SERVICES_FOR_EVENT_QUERY;
        try {
            connection = IdentityDatabaseUtil.getDBConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, eventId);
            rs = prepStmt.executeQuery();
            while (rs.next()) {
                String alias = rs.getString(SQLConstants.ALIAS_COLUMN);
                String action = rs.getString(SQLConstants.WS_ACTION_COLUMN);
                String serviceEP = rs.getString(SQLConstants.SERVICE_EP_COLUMN);
                String username = rs.getString(SQLConstants.USERNAME_COLUMN);
                String password = rs.getString(SQLConstants.PASSWORD_COLUMN);
                String condition = rs.getString(SQLConstants.CONDITION_COLUMN);
                int priority = rs.getInt(SQLConstants.PRIORITY_COLUMN);
                WSServiceBean serviceBean = new WSServiceBean();
                serviceBean.setAlias(alias);
                serviceBean.setWsAction(action);
                serviceBean.setServiceEndpoint(serviceEP);
                serviceBean.setUserName(username);
                serviceBean.setPassword(password.toCharArray());
                serviceBean.setPriority(priority);
                servicesMatched.put(serviceBean, condition);
            }
        } catch (IdentityException e) {
            throw new WorkflowException("Error when connecting to the Identity Database.", e);
        } catch (SQLException e) {
            throw new WorkflowException("Error when executing the sql query:" + query, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
        return servicesMatched;
    }

    public void removeWorkflowService(String alias){
    }

    public void updateWorkflowService(String alias, WSServiceBean newService){
    }
}
