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

package org.wso2.carbon.identity.workflow.mgt;

import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkFlowRequest;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowParameter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractWorkflowRequestHandler implements WorkflowRequestHandler {

    /**
     * Used to skip the workflow execution on the successive call after workflow completion.
     */
    private static ThreadLocal<Boolean> workFlowCompleted = new ThreadLocal<Boolean>();

    public static void unsetWorkFlowCompleted() {
        AbstractWorkflowRequestHandler.workFlowCompleted.remove();
    }

    public static Boolean getWorkFlowCompleted() {
        return workFlowCompleted.get();
    }

    public static void setWorkFlowCompleted(Boolean workFlowCompleted) {
        AbstractWorkflowRequestHandler.workFlowCompleted.set(workFlowCompleted);
    }

    /**
     * Prepare the workflow request and calls {@link #engageWorkflow(WorkFlowRequest)}
     *
     * @param wfParams    The parameters that should be sent to the workflow executor service
     * @param nonWfParams The parameters that should not be sent to the workflow executor service (eg. passwords)
     * @throws WorkflowException
     */
    public void engageWorkflow(Map<String, Object> wfParams, Map<String, Object> nonWfParams) throws WorkflowException {
        //Additional check to prevent executor being called again and again
        if (getWorkFlowCompleted() != null && getWorkFlowCompleted()) {
            return;
        }

        WorkFlowRequest workFlowRequest = new WorkFlowRequest();
        List<WorkflowParameter> parameters = new ArrayList<WorkflowParameter>(wfParams.size() + nonWfParams.size());
        for (Map.Entry<String, Object> paramEntry : wfParams.entrySet()) {
            parameters.add(getParameter(paramEntry.getKey(), paramEntry.getValue(), true));
        }
        for (Map.Entry<String, Object> paramEntry : nonWfParams.entrySet()) {
            parameters.add(getParameter(paramEntry.getKey(), paramEntry.getValue(), false));
        }
        workFlowRequest.setWorkflowParameters(parameters);
        workFlowRequest.setTenantId(CarbonContext.getThreadLocalCarbonContext().getTenantId());
        engageWorkflow(workFlowRequest);
    }

    /**
     * Wraps the parameters to the WorkflowParameter
     *
     * @param name     Name of the parameter
     * @param value    Value of the parameter
     * @param required Whether it is required to sent to the workflow executor
     * @return
     */
    protected WorkflowParameter getParameter(String name, Object value, boolean required) {
        WorkflowParameter parameter = new WorkflowParameter();
        parameter.setName(name);
        parameter.setValue(value);
        if (value == null) {
            parameter.setValueType(WorkflowDataType.WF_PARAM_TYPE_UNDEFINED);
        } else if (WorkFlowConstants.NUMERIC_CLASSES.contains(value.getClass())) {
            parameter.setValueType(WorkflowDataType.WF_PARAM_TYPE_NUMERIC);
        } else if (value instanceof Boolean) {
            parameter.setValueType(WorkflowDataType.WF_PARAM_TYPE_BOOLEAN);
        } else if (value instanceof String) {
            parameter.setValueType(WorkflowDataType.WF_PARAM_TYPE_STRING);
        } else if (value instanceof Collection) {
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
        workFlowRequest.setEventId(getEventId());
        WorkFlowExecutorManager.getInstance().executeWorkflow(workFlowRequest);
    }

    @Override
    public void onWorkflowCompletion(String status, WorkFlowRequest originalRequest, Object additionalData) throws WorkflowException {
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
        setWorkFlowCompleted(true);
        onWorkflowCompletion(status, requestParams, additionalResponseParams, originalRequest.getTenantId());
    }

    /**
     * Callback method from the executor, implementation should be similar to the
     * {@link #engageWorkflow(java.util.Map, java.util.Map)}
     * @param status The return status from the workflow executor
     * @param requestParams The params that were in the original request
     * @param responseAdditionalParams The params sent from the workflow executor
     * @param tenantId
     * @throws WorkflowException
     */
    public abstract void onWorkflowCompletion(String status, Map<String, Object> requestParams, Map<String, Object>
            responseAdditionalParams, int tenantId) throws WorkflowException;
}
