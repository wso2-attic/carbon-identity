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

import org.wso2.carbon.workflow.mgt.bean.WorkFlowRequest;
import org.wso2.carbon.workflow.mgt.dao.WorkflowRequestDAO;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

public class WorkFlowExecutorManager {

    private static WorkFlowExecutorManager instance = new WorkFlowExecutorManager();
    private SortedSet<WorkFlowExecutor> workFlowExecutors;
    private Map<String, WorkflowRequestHandler> workflowRequestHandlers;

    private WorkFlowExecutorManager() {
        Comparator<WorkFlowExecutor> priorityBasedComparator = new Comparator<WorkFlowExecutor>() {
            @Override
            public int compare(WorkFlowExecutor o1, WorkFlowExecutor o2) {
                return o1.getPriority() - o2.getPriority();
            }
        };
        workFlowExecutors = new TreeSet<WorkFlowExecutor>(priorityBasedComparator);
        workflowRequestHandlers = new HashMap<String, WorkflowRequestHandler>();
    }

    public static WorkFlowExecutorManager getInstance() {
        return instance;
    }

    public void executeWorkflow(WorkFlowRequest workFlowRequest) throws WorkflowException {

        workFlowRequest.setUuid(UUID.randomUUID().toString());
        //executors are sorted by priority by the time they are added.
        for (WorkFlowExecutor workFlowExecutor : workFlowExecutors) {
            if (workFlowExecutor.canHandle(workFlowRequest)) {
                try {
                    WorkflowRequestDAO requestDAO = new WorkflowRequestDAO();
                    requestDAO.addWorkflowEntry(workFlowRequest);
                    //todo: drop unused parameters?
                    workFlowExecutor.execute(workFlowRequest);
                    return;
                } catch (WorkflowException e) {
                    //todo
                }
                return;
            }
        }
        //If none of the executors were called
        handleCallback(workFlowRequest, WorkFlowConstants.WF_STATUS_NO_MATCHING_EXECUTORS, null);
    }

    private void handleCallback(WorkFlowRequest request, String status, Object additionalParams)
            throws WorkflowException {
        if (request != null) {
            String requesterId = request.getRequesterId();
            WorkflowRequestHandler requestHandler = workflowRequestHandlers.get(requesterId);
            if (requestHandler == null) {
                throw new WorkflowException("No request handlers registered for the id: " + requesterId);
            }
            requestHandler.onWorkflowCompletion(status, request, additionalParams);
        }
    }

    public void handleCallback(String uuid, String status, Object additionalParams) throws WorkflowException {
        WorkflowRequestDAO requestDAO = new WorkflowRequestDAO();
        WorkFlowRequest request = requestDAO.retrieveWorkflow(uuid);
        handleCallback(request, status, additionalParams);
    }

    public void registerExecutor(WorkFlowExecutor workFlowExecutor) {
        workFlowExecutors.add(workFlowExecutor);
    }

    public void registerRequester(WorkflowRequestHandler workflowRequestHandler) {
        workflowRequestHandlers.put(workflowRequestHandler.getActionIdentifier(), workflowRequestHandler);
    }
}
