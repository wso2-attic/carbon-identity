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
package org.wso2.carbon.identity.workflow.impl.listener;

import org.wso2.carbon.identity.workflow.impl.WorkflowImplService;
import org.wso2.carbon.identity.workflow.impl.internal.WorkflowImplServiceDataHolder;
import org.wso2.carbon.identity.workflow.mgt.bean.Workflow;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowRequest;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;
import org.wso2.carbon.identity.workflow.mgt.listener.AbstractWorkflowListener;


public class WorkflowListenerImpl extends AbstractWorkflowListener {
    @Override
    public void doPreDeleteWorkflowRequest(WorkflowRequest workflowRequest) throws WorkflowException {
        WorkflowImplService workflowImplService = WorkflowImplServiceDataHolder.getInstance().getWorkflowImplService();
        workflowImplService.deleteHumanTask(workflowRequest);
    }

    @Override
    public void doPostDeleteWorkflowRequest(WorkflowRequest workflowRequest) throws WorkflowException {

    }

    @Override
    public void doPreDeleteWorkflow(Workflow workflow) throws WorkflowException {
        WorkflowImplService workflowImplService = WorkflowImplServiceDataHolder.getInstance().getWorkflowImplService();
        if (workflowImplService == null) {
            throw new WorkflowException("Error when deleting the Workflow " + workflow.getWorkflowName());
        }
        workflowImplService.removeBPSPackage(workflow);
    }

    @Override
    public void doPostDeleteWorkflow(Workflow workflow) throws WorkflowException {

    }


}
