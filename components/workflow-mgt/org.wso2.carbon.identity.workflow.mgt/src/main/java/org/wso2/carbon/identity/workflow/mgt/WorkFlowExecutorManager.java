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

package org.wso2.carbon.identity.workflow.mgt;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkFlowRequest;
import org.wso2.carbon.identity.workflow.mgt.bean.RequestParameter;
import org.wso2.carbon.identity.workflow.mgt.dao.WorkflowRequestDAO;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;
import org.wso2.carbon.identity.workflow.mgt.internal.WorkflowMgtServiceComponent;
import org.wso2.carbon.identity.workflow.mgt.internal.WorkflowServiceDataHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WorkFlowExecutorManager {

    private static WorkFlowExecutorManager instance = new WorkFlowExecutorManager();

    private static Log log = LogFactory.getLog(WorkFlowExecutorManager.class);

    private WorkFlowExecutorManager() {
    }

    public static WorkFlowExecutorManager getInstance() {
        return instance;
    }

    public void executeWorkflow(WorkFlowRequest workFlowRequest) throws WorkflowException {

        workFlowRequest.setUuid(UUID.randomUUID().toString());
        //executors are sorted by priority by the time they are added.
        for (WorkFlowExecutor workFlowExecutor : WorkflowMgtServiceComponent.getWorkFlowExecutors()) {
            if (workFlowExecutor.canHandle(workFlowRequest)) {
                try {
                    WorkflowRequestDAO requestDAO = new WorkflowRequestDAO();
                    requestDAO.addWorkflowEntry(workFlowRequest);

                    //Drop parameters that should not be sent to the workflow executor (all params are persisted by now)
                    List<RequestParameter> parameterListToSend = new ArrayList<RequestParameter>();
                    for (RequestParameter parameter : workFlowRequest.getRequestParameters()) {
                        if (parameter.isRequiredInWorkflow()) {
                            parameterListToSend.add(parameter);
                        }
                    }
                    workFlowRequest.setRequestParameters(parameterListToSend);

                    workFlowExecutor.execute(workFlowRequest);
                    return;
                } catch (WorkflowException e) {
                    log.error("Error executing workflow at " + workFlowExecutor.getName(), e);
                }
                return;
            }
        }
        //If none of the executors were called
        handleCallback(workFlowRequest, WorkflowRequestStatus.SKIPPED.toString(), null);
    }

    private void handleCallback(WorkFlowRequest request, String status, Map<String, Object> additionalParams)
            throws WorkflowException {
        if (request != null) {
            String eventId = request.getEventType();
            WorkflowRequestHandler requestHandler = WorkflowServiceDataHolder.getInstance().getRequestHandler(eventId);
            if (requestHandler == null) {
                throw new WorkflowException("No request handlers registered for the id: " + eventId);
            }
            requestHandler.onWorkflowCompletion(status, request, additionalParams);
        }
    }

    public void handleCallback(String uuid, String status, Map<String, Object> additionalParams)
            throws WorkflowException {
        WorkflowRequestDAO requestDAO = new WorkflowRequestDAO();
        WorkFlowRequest request = requestDAO.retrieveWorkflow(uuid);
        handleCallback(request, status, additionalParams);
    }
}
