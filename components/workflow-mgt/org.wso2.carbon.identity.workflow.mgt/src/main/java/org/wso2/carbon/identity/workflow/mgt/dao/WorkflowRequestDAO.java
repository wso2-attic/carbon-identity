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
import org.wso2.carbon.identity.workflow.mgt.exception.InternalWorkflowException;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;
import org.wso2.carbon.identity.workflow.mgt.util.WorkflowRequestStatus;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkFlowRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class WorkflowRequestDAO {

    private static Log log = LogFactory.getLog(WorkflowRequestDAO.class);

    /**
     * Persists WorkflowRequest to be used when workflow is completed
     *
     * @param workflow The workflow object to be persisted
     * @throws WorkflowException
     */
    public void addWorkflowEntry(WorkFlowRequest workflow) throws WorkflowException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        String query = SQLConstants.ADD_WORKFLOW_REQUEST_QUERY;
        try {
            Timestamp createdDateStamp = new Timestamp(System.currentTimeMillis());
            connection = IdentityDatabaseUtil.getDBConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, workflow.getUuid());
            prepStmt.setTimestamp(2, createdDateStamp);
            prepStmt.setTimestamp(3, createdDateStamp);
            prepStmt.setBytes(4, serializeWorkflowRequest(workflow));
            prepStmt.setString(5, WorkflowRequestStatus.PENDING.toString());
            prepStmt.executeUpdate();
            connection.commit();
        } catch (IdentityException e) {
            throw new InternalWorkflowException("Error when connecting to the Identity Database.", e);
        } catch (SQLException e) {
            throw new InternalWorkflowException("Error when executing the sql query:" + query, e);
        } catch (IOException e) {
            throw new InternalWorkflowException("Error when serializing the workflow request: " + workflow, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
    }

    /**
     * Serialize the workflow request to be persisted as blob
     *
     * @param workFlowRequest The workflow request to be persisted
     * @return
     * @throws IOException
     */
    private byte[] serializeWorkflowRequest(WorkFlowRequest workFlowRequest) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(workFlowRequest);
        oos.close();
        return baos.toByteArray();
    }

    public void updateWorkflowStatus(WorkFlowRequest workflowDataBean) throws InternalWorkflowException{
    }

    /**
     * Retrieve workflow request specified by the given uuid
     * @param uuid The uuid of the request to be retrieved
     * @return
     * @throws WorkflowException
     */
    public WorkFlowRequest retrieveWorkflow(String uuid) throws InternalWorkflowException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;

        String query = SQLConstants.GET_WORKFLOW_REQUEST_QUERY;
        try {
            connection = IdentityDatabaseUtil.getDBConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, uuid);
            rs = prepStmt.executeQuery();
            if (rs.next()) {
                byte[] requestBytes = rs.getBytes(SQLConstants.REQUEST_COLUMN);
                return deserializeWorkflowRequest(requestBytes);
            }
        } catch (IdentityException e) {
            throw new InternalWorkflowException("Error when connecting to the Identity Database.", e);
        } catch (SQLException e) {
            throw new InternalWorkflowException("Error when executing the sql query:" + query, e);
        } catch (ClassNotFoundException | IOException e) {
            throw new InternalWorkflowException("Error when deserializing the workflow request. uuid = " + uuid, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
        return null;
    }

    /**
     * Deserialize the persisted Workflow request
     *
     * @param serializedData Serialized request
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private WorkFlowRequest deserializeWorkflowRequest(byte[] serializedData) throws IOException,
            ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(serializedData);
        ObjectInputStream ois = new ObjectInputStream(bais);
        Object objectRead = ois.readObject();
        if (objectRead != null && objectRead instanceof WorkFlowRequest) {
            return (WorkFlowRequest) objectRead;
        }
        return null;
    }
}
