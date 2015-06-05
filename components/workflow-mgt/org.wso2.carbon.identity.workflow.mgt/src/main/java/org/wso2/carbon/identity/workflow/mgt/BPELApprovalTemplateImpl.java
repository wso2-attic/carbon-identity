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

import org.wso2.carbon.identity.workflow.mgt.bean.TemplateParameterDef;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;

import java.util.Map;

public class BPELApprovalTemplateImpl extends AbstractWorkflowTemplateImpl {

    private static final String TEMPLATE_IMPL_NAME = "BPEL";
    private static final TemplateParameterDef[] PARAMETER_DEFINITIONS;

    static {
        TemplateParameterDef bpsProfile = new TemplateParameterDef();
        bpsProfile.setParamName(WorkFlowConstants.TemplateConstants.BPEL_IMPL_BPS_PROFILE);
        bpsProfile.setParamType(WorkflowTemplateParamType.BPS_PROFILE);
        TemplateParameterDef bpelName = new TemplateParameterDef();
        bpelName.setParamName(WorkFlowConstants.TemplateConstants.BPEL_IMPL_PROCESS_NAME);
        bpelName.setParamType(WorkflowTemplateParamType.STRING);
        PARAMETER_DEFINITIONS = new TemplateParameterDef[]{bpsProfile, bpelName};
    }

    @Override
    public void initializeExecutor(Map<String, Object> initParams) throws WorkflowException {
        //read profile and add its params
        WorkflowService workflowService = new WorkflowService();
        Map<String, Object> bpelProfileParams = workflowService.getBPSProfileParams(
                (String) initParams.get(WorkFlowConstants.TemplateConstants.BPEL_IMPL_BPS_PROFILE));
        initParams.putAll(bpelProfileParams);
        super.initializeExecutor(initParams);
    }

    @Override
    public String getTemplateId() {
        return WorkFlowConstants.TemplateConstants.APPROVAL_TEMPLATE_NAME;
    }

    @Override
    public TemplateParameterDef[] getImplParamDefinitions() {
        return PARAMETER_DEFINITIONS;
    }

    @Override
    public String getImplementationId() {
        return TEMPLATE_IMPL_NAME;
    }

    @Override
    public String getImplementationName() {
        return TEMPLATE_IMPL_NAME;
    }
}
