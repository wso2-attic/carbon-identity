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

package org.wso2.carbon.workflow.mgt;

import org.wso2.carbon.workflow.mgt.bean.WFCallBackDTO;
import org.wso2.carbon.workflow.mgt.bean.WorkflowExecutionData;
import org.wso2.carbon.workflow.mgt.bean.WorkflowPersistenceDataBean;
import org.wso2.carbon.workflow.mgt.dao.WorkflowDAO;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

public abstract class WorkflowRequester implements Serializable {

    /**
     * Implements the workflow execution logic.
     * @throws WorkflowException - Thrown when the workflow execution was not fully performed.
     */
    protected WorkflowExecutionData prepareRequest() throws WorkflowException{
        String uuid = generateUUID();
        WorkflowPersistenceDataBean workflowDataBean = createWorkflowDataBean();
        workflowDataBean.setWorkflowId(uuid);
        persistWorkflowData(workflowDataBean);
        WorkflowExecutionData requestData = createExecutionRequestData();
        requestData.setUuid(uuid);
        return requestData;
    }

    protected void persistWorkflowData(WorkflowPersistenceDataBean workflowDataBean) throws WorkflowException {
        WorkflowDAO workflowDAO = new WorkflowDAO();
        workflowDAO.addWorkflowEntry(workflowDataBean);
    }
    
    protected abstract WorkflowPersistenceDataBean createWorkflowDataBean();

    protected abstract WorkflowExecutionData createExecutionRequestData();

    public void execute() throws WorkflowException {
        WorkflowExecutionData executionData = prepareRequest();
        WorkFlowExecutorManager wfExecutorManager = WorkFlowExecutorManager.getInstance();
        wfExecutorManager.executeWorkflow(executionData);
    }

    /**
     * Implements the workflow completion logic.
     */
    public void onCallBack(WFCallBackDTO callBackDTO) throws WorkflowException{
        if(WorkflowStatus.APPROVED.toString().equals(callBackDTO.getStatus())){
            WorkflowPersistenceDataBean persistedWorkflowDataBean = retrieveWorkflowData(callBackDTO);
            completeAction(persistedWorkflowDataBean,callBackDTO.getParams());
        }
    }

    protected WorkflowPersistenceDataBean retrieveWorkflowData(WFCallBackDTO callBackDTO){
        WorkflowDAO workflowDAO = new WorkflowDAO();
        return workflowDAO.retrieveWorkflow(callBackDTO.getUuid());
    }

    protected abstract void completeAction(WorkflowPersistenceDataBean workflowDataBean, Map<String,Object> optionalParams);

    /**
     * Method generates and returns UUID
     * @return UUID
     */
    public String generateUUID(){
        return UUID.randomUUID().toString();
    }

}
