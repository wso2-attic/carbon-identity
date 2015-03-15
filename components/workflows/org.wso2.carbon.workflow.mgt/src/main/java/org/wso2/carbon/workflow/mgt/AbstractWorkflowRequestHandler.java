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
import org.wso2.carbon.workflow.mgt.bean.WorkflowParameter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractWorkflowRequestHandler implements WorkflowRequestHandler {

    public void engageWorkflow(Map<String, Object> wfParams, Map<String, Object> nonWfParams) throws WorkflowException {
        WorkFlowRequest workFlowRequest = new WorkFlowRequest();
        List<WorkflowParameter> parameters = new ArrayList<WorkflowParameter>(wfParams.size() + nonWfParams.size());
        for (Map.Entry<String, Object> paramEntry : wfParams.entrySet()) {
            parameters.add(getParameter(paramEntry.getKey(), paramEntry.getValue(), true));
        }
        for (Map.Entry<String, Object> paramEntry : nonWfParams.entrySet()) {
            parameters.add(getParameter(paramEntry.getKey(), paramEntry.getValue(), false));
        }
        workFlowRequest.setWorkflowParameters(parameters);
        engageWorkflow(workFlowRequest);
    }

    private WorkflowParameter getParameter(String name, Object value, boolean required) {
        WorkflowParameter parameter = new WorkflowParameter();
        parameter.setName(name);
        parameter.setValue(value);
        if (WorkFlowConstants.NUMERIC_CLASSES.contains(value.getClass())) {
            parameter.setValueType(WorkflowDataType.WF_PARAM_TYPE_NUMERIC);
        } else if (value instanceof Boolean) {
            parameter.setValueType(WorkflowDataType.WF_PARAM_TYPE_BOOLEAN);
        } else if (value instanceof String) {
            parameter.setValueType(WorkflowDataType.WF_PARAM_TYPE_STRING);
        } else if (value instanceof Object[] || value instanceof Collection) {
            parameter.setValueType(WorkflowDataType.WF_PARAM_TYPE_BASIC_LIST);
        } else if (value instanceof Map) {
            parameter.setValueType(WorkflowDataType.WF_PARAM_TYPE_BASIC_MAP);
        } else {
            parameter.setValueType(WorkflowDataType.WF_PARAM_TYPE_UNDEFINED);
        }
        parameter.setRequiredInWorkflow(required);
        return parameter;
    }

    @Override
    public void engageWorkflow(WorkFlowRequest workFlowRequest) throws WorkflowException {
        workFlowRequest.setRequesterId(getActionIdentifier());
        WorkFlowExecutorManager.getInstance().executeWorkflow(workFlowRequest);
    }

    @Override
    public void onWorkflowCompletion(String status, WorkFlowRequest originalRequest, Object additionalData) {
        Map<String, Object> requestParams = new HashMap<String, Object>();
        for (WorkflowParameter parameter : originalRequest.getWorkflowParameters()) {
            requestParams.put(parameter.getName(), parameter.getValue());
        }
        Map<String, Object> additionalResponseParams;
        if (additionalData instanceof Map) {
            additionalResponseParams = (Map<String, Object>) additionalData;
        } else {
            additionalResponseParams = new HashMap<String, Object>();
        }
        onWorkflowCompletion(status, requestParams, additionalResponseParams);
    }

    public abstract void onWorkflowCompletion(String status, Map<String, Object> requestParams, Map<String, Object>
            responseAdditionalParams);
}
