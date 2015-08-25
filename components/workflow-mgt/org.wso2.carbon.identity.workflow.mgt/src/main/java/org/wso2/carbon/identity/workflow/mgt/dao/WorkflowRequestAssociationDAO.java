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

package org.wso2.carbon.identity.workflow.mgt.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowRequestAssociationDTO;
import org.wso2.carbon.identity.workflow.mgt.exception.InternalWorkflowException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class WorkflowRequestAssociationDAO {

    private static Log log = LogFactory.getLog(WorkflowRequestDAO.class);

    /**
     * Adds new workflow-request relationship to database
     *
     * @param relationshipId
     * @param workflowId
     * @param requestId
     * @param status
     * @throws InternalWorkflowException
     */
    public void addNewRelationship(String relationshipId, String workflowId, String requestId, String status) throws
            InternalWorkflowException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        String query = SQLConstants.ADD_WORKFLOW_REQUEST_RELATIONSHIP;
        try {
            Timestamp createdDateStamp = new Timestamp(System.currentTimeMillis());
            connection = IdentityDatabaseUtil.getDBConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, relationshipId);
            prepStmt.setString(2, workflowId);
            prepStmt.setString(3, requestId);
            prepStmt.setTimestamp(4, createdDateStamp);
            prepStmt.setString(5, status);
            prepStmt.execute();
            connection.commit();
        } catch (IdentityException e) {
            throw new InternalWorkflowException("Error when connecting to the Identity Database.", e);
        } catch (SQLException e) {
            throw new InternalWorkflowException("Error when executing the sql query:" + query, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
    }

    /**
     * Get requestId of a relationship.
     *
     * @param relationshipId
     * @return
     * @throws InternalWorkflowException
     */
    public String getRequestIdOfRelationship(String relationshipId) throws InternalWorkflowException {

        Connection connection = null;
        PreparedStatement prepStmt = null;
        String query = SQLConstants.GET_REQUEST_ID_OF_RELATIONSHIP;
        ResultSet resultSet = null;
        try {
            connection = IdentityDatabaseUtil.getDBConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, relationshipId);
            resultSet = prepStmt.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString(SQLConstants.REQUEST_ID_COLUMN);
            }
            connection.commit();
        } catch (IdentityException e) {
            throw new InternalWorkflowException("Error when connecting to the Identity Database.", e);
        } catch (SQLException e) {
            throw new InternalWorkflowException("Error when executing the sql query:" + query, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, resultSet, prepStmt);
        }
        return "";
    }

    /**
     * Update state of workflow of a request
     *
     * @param relationshipId
     * @throws InternalWorkflowException
     */
    public void updateStatusOfRelationship(String relationshipId, String status) throws InternalWorkflowException {

        Connection connection = null;
        PreparedStatement prepStmt = null;
        String query = SQLConstants.UPDATE_STATUS_OF_RELATIONSHIP;
        try {
            Timestamp updatedDateStamp = new Timestamp(System.currentTimeMillis());
            connection = IdentityDatabaseUtil.getDBConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, status);
            prepStmt.setTimestamp(2, updatedDateStamp);
            prepStmt.setString(3, relationshipId);
            prepStmt.execute();
            connection.commit();
        } catch (IdentityException e) {
            throw new InternalWorkflowException("Error when connecting to the Identity Database.", e);
        } catch (SQLException e) {
            throw new InternalWorkflowException("Error when executing the sql query:" + query, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
    }

    /**
     * Get list of states of workflows of a request
     *
     * @param requestId
     * @return
     * @throws InternalWorkflowException
     */
    public List<String> getWorkflowStatesOfRequest(String requestId) throws InternalWorkflowException {

        List<String> states = new ArrayList<>();
        Connection connection = null;
        PreparedStatement prepStmt = null;
        String query = SQLConstants.GET_STATES_OF_REQUEST;
        ResultSet resultSet = null;
        try {
            connection = IdentityDatabaseUtil.getDBConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, requestId);
            resultSet = prepStmt.executeQuery();
            if (resultSet.next()) {
                states.add(resultSet.getString(SQLConstants.REQUEST_STATUS_COLUMN));
            }
            connection.commit();
        } catch (IdentityException e) {
            throw new InternalWorkflowException("Error when connecting to the Identity Database.", e);
        } catch (SQLException e) {
            throw new InternalWorkflowException("Error when executing the sql query:" + query, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, resultSet, prepStmt);
        }
        return states;
    }

    /**
     * Get requestId of a relationship.
     *
     * @param relationshipId
     * @return
     * @throws InternalWorkflowException
     */
    public String getStatusOfRelationship(String relationshipId) throws InternalWorkflowException {

        Connection connection = null;
        PreparedStatement prepStmt = null;
        String query = SQLConstants.GET_STATUS_OF_RELATIONSHIP;
        ResultSet resultSet = null;
        try {
            connection = IdentityDatabaseUtil.getDBConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, relationshipId);
            resultSet = prepStmt.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString(SQLConstants.REQUEST_STATUS_COLUMN);
            }
            connection.commit();
        } catch (IdentityException e) {
            throw new InternalWorkflowException("Error when connecting to the Identity Database.", e);
        } catch (SQLException e) {
            throw new InternalWorkflowException("Error when executing the sql query:" + query, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, resultSet, prepStmt);
        }
        return "";
    }

    /**
     * Get array of Workflows of a request
     *
     * @param requestId
     * @return
     * @throws InternalWorkflowException
     */
    public WorkflowRequestAssociationDTO[] getWorkflowsOfRequest(String requestId) throws InternalWorkflowException {

        Connection connection = null;
        PreparedStatement prepStmt = null;
        String query = SQLConstants.GET_WORKFLOWS_OF_REQUEST;
        ResultSet resultSet = null;
        try {
            connection = IdentityDatabaseUtil.getDBConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, requestId);
            resultSet = prepStmt.executeQuery();
            ArrayList<WorkflowRequestAssociationDTO> workflowDTOs = new ArrayList<>();
            while (resultSet.next()) {
                WorkflowRequestAssociationDTO workflowDTO = new WorkflowRequestAssociationDTO();
                workflowDTO.setWorkflowId(resultSet.getString(SQLConstants.ID_COLUMN));
                workflowDTO.setWorkflowName(resultSet.getString(SQLConstants.WF_NAME_COLUMN));
                workflowDTO.setLastUpdatedTime(resultSet.getTimestamp(SQLConstants.REQUEST_UPDATED_AT_COLUMN)
                        .toString());
                workflowDTO.setStatus(resultSet.getString(SQLConstants.REQUEST_STATUS_COLUMN));
                workflowDTOs.add(workflowDTO);
            }
            WorkflowRequestAssociationDTO[] requestArray = new WorkflowRequestAssociationDTO[workflowDTOs.size()];
            for (int i = 0; i < workflowDTOs.size(); i++) {
                requestArray[i] = workflowDTOs.get(i);
            }
            return requestArray;
        } catch (IdentityException e) {
            throw new InternalWorkflowException("Error when connecting to the Identity Database.", e);
        } catch (SQLException e) {
            throw new InternalWorkflowException("Error when executing the sql query:" + query, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, resultSet, prepStmt);
        }
    }

}
