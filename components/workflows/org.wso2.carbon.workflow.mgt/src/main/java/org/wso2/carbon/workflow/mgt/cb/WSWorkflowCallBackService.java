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

package org.wso2.carbon.workflow.mgt.cb;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.workflow.mgt.WorkFlowExecutorManager;
import org.wso2.carbon.workflow.mgt.WorkflowException;

/**
 * This is the Callback service for the WS Workflow requests. Once workflow executor completes its workflow,
 * it will call back this service with the results.
 */
public class WSWorkflowCallBackService {

    private static Log log = LogFactory.getLog(WSWorkflowCallBackService.class);

    public void onCallback(WSWorkflowResponse response) {
        try {
            WorkFlowExecutorManager.getInstance()
                    .handleCallback(response.getUuid(), response.getStatus(), response.getOutputParams());
        } catch (WorkflowException e) {
            log.error("Error when handling callback for the workflow, id:" + response.getUuid() + ", event:" + response
                    .getRequesterId());
        }
    }
}
