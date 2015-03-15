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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.workflow.mgt.WorkflowException;
import org.wso2.carbon.workflow.mgt.WorkflowStatus;
import org.wso2.carbon.workflow.mgt.bean.WorkFlowRequest;

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

    protected static Log log = LogFactory.getLog(WorkflowRequestDAO.class);

    /**
     * Persists WorkflowDTO to Database
     *
     * @param workflow
     * @throws org.wso2.carbon.workflow.mgt.WorkflowException
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
            prepStmt.setString(5, WorkflowStatus.PENDING);
            prepStmt.executeUpdate();
            connection.commit();
        } catch (IdentityException e) {
            //todo
        } catch (SQLException e) {
            //todo
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
    }

    private byte[] serializeWorkflowRequest(WorkFlowRequest workFlowRequest) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(workFlowRequest);
            oos.close();
            return baos.toByteArray();
        } catch (IOException e) {
            //todo
            return null;
        }
    }

    public void updateWorkflowStatus(WorkFlowRequest workflowDataBean) {
    }

    public WorkFlowRequest retrieveWorkflow(String uuid) {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;

        String query = SQLConstants.GET_WORKFLOW_REQUEST_QUERY;
        try {
            Timestamp createdDateStamp = new Timestamp(System.currentTimeMillis());
            connection = IdentityDatabaseUtil.getDBConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, uuid);
            rs = prepStmt.executeQuery();
            if (rs.next()) {
                byte[] requestBytes = rs.getBytes(SQLConstants.REQUEST_COLUMN);
                return deserializeWorkflowRequest(requestBytes);
            } else {
                //todo
            }
        } catch (IdentityException e) {
            //todo
        } catch (SQLException e) {
            //todo
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }

        return null;
    }

    private WorkFlowRequest deserializeWorkflowRequest(byte[] serializedData) {
        ByteArrayInputStream bais = new ByteArrayInputStream(serializedData);
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(bais);
            Object objectRead = ois.readObject();
            if (objectRead != null && objectRead instanceof WorkFlowRequest) {
                return (WorkFlowRequest) objectRead;
            } else {
                //todo
            }
        } catch (IOException e) {
            //todo
        } catch (ClassNotFoundException e) {
            //todo
        } finally {
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {
                    log.error("Error occurred when closing input stream to read serialized request.", e);
                }
            }
        }
        return null;
    }
}
