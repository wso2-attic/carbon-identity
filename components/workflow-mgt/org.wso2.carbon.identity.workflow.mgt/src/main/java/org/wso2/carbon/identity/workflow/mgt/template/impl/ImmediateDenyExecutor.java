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

package org.wso2.carbon.identity.workflow.mgt.template.impl;

import org.wso2.carbon.identity.workflow.mgt.WorkFlowExecutorManager;
import org.wso2.carbon.identity.workflow.mgt.util.WorkflowRequestStatus;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkFlowRequest;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;
import org.wso2.carbon.identity.workflow.mgt.template.WorkFlowExecutor;

import java.util.Collections;
import java.util.Map;

public class ImmediateDenyExecutor implements WorkFlowExecutor {

    private static final String EXECUTOR_NAME = "ImmediateDenyExecutor";

    @Override
    public boolean canHandle(WorkFlowRequest workFlowRequest) {

        return true;
    }

    @Override
    public void initialize(Map<String, Object> params) {
        //not needed for this executor
    }

    @Override
    public void execute(WorkFlowRequest workFlowRequest) throws WorkflowException {

        WorkFlowExecutorManager.getInstance().handleCallback(workFlowRequest.getUuid(), WorkflowRequestStatus
                .REJECTED.toString(), Collections.EMPTY_MAP);
    }

    @Override
    public String getName() {

        return EXECUTOR_NAME;
    }
}
