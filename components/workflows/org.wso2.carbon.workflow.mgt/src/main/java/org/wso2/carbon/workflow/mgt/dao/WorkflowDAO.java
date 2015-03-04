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

import org.wso2.carbon.workflow.mgt.bean.WorkflowPersistenceDataBean;
import org.wso2.carbon.workflow.mgt.WorkflowException;

public class WorkflowDAO {

    /**
     * Persists WorkflowDTO to Database
     * @param workflow
     * @throws org.wso2.carbon.workflow.mgt.WorkflowException
     */
    public void addWorkflowEntry(WorkflowPersistenceDataBean workflow) throws WorkflowException {
//        Connection connection = null;
//        PreparedStatement prepStmt = null;
//        ResultSet rs = null;
//
//        String query = SQLConstants.ADD_WORKFLOW_QUERY;
//        try {
//            Timestamp cratedDateStamp = new Timestamp(workflow.getCreatedTime());
//
//            prepStmt.execute();
//
//            connection.commit();
//        } catch (IdentityException e) {
//            e.printStackTrace();
//        } catch (SQLException e) {
//            handleException("Error while adding Workflow : " + workflow.getExternalWorkflowReference() + " to the database", e);
//        } finally {
//            APIMgtDBUtil.closeAllConnections(prepStmt, connection, rs);
//        }
    }

    public void updateWorkflowStatus(WorkflowPersistenceDataBean workflowDataBean) {
    }

    public WorkflowPersistenceDataBean retrieveWorkflow(String uuid){
        //todo: implementation
        return null;
    }
}
