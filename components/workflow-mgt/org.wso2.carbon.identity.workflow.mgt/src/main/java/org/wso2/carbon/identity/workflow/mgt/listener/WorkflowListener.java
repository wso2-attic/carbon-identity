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

package org.wso2.carbon.identity.workflow.mgt.listener;

import org.wso2.carbon.identity.workflow.mgt.bean.Workflow;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowRequest;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;

/**
 * Listener for Workflow Request Delete Process
 */
public interface WorkflowListener {

    /**
     * Trigger before delete the request
     *
     * @param workflowRequest
     * @throws WorkflowException
     */
    void doPreDeleteWorkflowRequest(WorkflowRequest workflowRequest) throws WorkflowException;

    /**
     * Trigger after deleting the request
     *
     * @param workflowRequest
     * @throws WorkflowException
     */
    void doPostDeleteWorkflowRequest(WorkflowRequest workflowRequest) throws WorkflowException;

    /**
     * Trigger before delete the workflow
     *
     * @param workflow
     * @throws WorkflowException
     */
    void doPreDeleteWorkflow(Workflow workflow) throws WorkflowException;


    /**
     *
     * Trigger after delete the workflow
     *
     * @param workflow
     * @throws WorkflowException
     */
    void doPostDeleteWorkflow(Workflow workflow) throws WorkflowException;

}
