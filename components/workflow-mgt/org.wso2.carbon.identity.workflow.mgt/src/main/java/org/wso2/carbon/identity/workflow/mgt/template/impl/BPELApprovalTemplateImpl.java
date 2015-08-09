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

import org.wso2.carbon.identity.workflow.mgt.util.WorkFlowConstants;
import org.wso2.carbon.identity.workflow.mgt.WorkflowService;
import org.wso2.carbon.identity.workflow.mgt.util.WorkflowTemplateParamType;
import org.wso2.carbon.identity.workflow.mgt.bean.TemplateParameterDef;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;
import org.wso2.carbon.identity.workflow.mgt.template.AbstractWorkflowTemplateImpl;
import org.wso2.carbon.identity.workflow.mgt.template.TemplateInitializer;
import org.wso2.carbon.identity.workflow.mgt.template.WorkFlowExecutor;

import java.util.Map;

public class BPELApprovalTemplateImpl extends AbstractWorkflowTemplateImpl {

    private static final String TEMPLATE_IMPL_NAME = "BPEL";
    private static final TemplateParameterDef[] PARAMETER_DEFINITIONS;

    private TemplateInitializer initializer;
    private WorkFlowExecutor executor;

    static {
        Object[][] paramDef = {
                {WorkFlowConstants.TemplateConstants.BPEL_IMPL_BPS_PROFILE, "BPEL Engine profile",
                        WorkflowTemplateParamType.BPS_PROFILE, "", true},
                {WorkFlowConstants.TemplateConstants.HT_SUBJECT, "Approval Request Subject",
                        WorkflowTemplateParamType.STRING, "Approval required", true},
                {WorkFlowConstants.TemplateConstants.HT_DESCRIPTION, "Approval Request Body",
                        WorkflowTemplateParamType.LONG_STRING,
                        "A request has been made with following details. Please approve to proceed.", true},
        };
        PARAMETER_DEFINITIONS = new TemplateParameterDef[paramDef.length];
        for (int i = 0; i < paramDef.length; i++) {
            Object[] def = paramDef[i];
            TemplateParameterDef parameterDef = new TemplateParameterDef();
            parameterDef.setParamName((String) def[0]);
            parameterDef.setDisplayName((String) def[1]);
            parameterDef.setParamType((String) def[2]);
            parameterDef.setDefaultValue((String) def[3]);
            parameterDef.setMandatory((boolean) def[4]);
            PARAMETER_DEFINITIONS[i] = parameterDef;
        }
    }

    public BPELApprovalTemplateImpl() {

        setExecutor(new DefaultBPELExecutor());
    }

    @Override
    public void deploy(Map<String, Object> initParams) throws WorkflowException {

        WorkflowService workflowService = new WorkflowService();
        Map<String, Object> bpelProfileParams = workflowService.getBPSProfileParams(
                (String) initParams.get(WorkFlowConstants.TemplateConstants.BPEL_IMPL_BPS_PROFILE));
        initParams.putAll(bpelProfileParams);
        setInitializer(new BPELApprovalDeployer());
        super.deploy(initParams);
    }

    @Override
    protected TemplateInitializer getInitializer() {

        return initializer;
    }

    protected void setInitializer(TemplateInitializer initializer) {

        this.initializer = initializer;
    }

    @Override
    protected WorkFlowExecutor getExecutor() {

        return executor;
    }

    protected void setExecutor(WorkFlowExecutor executor) {

        this.executor = executor;
    }

    @Override
    public void initializeExecutor(Map<String, Object> initParams) throws WorkflowException {
        //read profile and add its params
        WorkflowService workflowService = new WorkflowService();
        Map<String, Object> bpelProfileParams = workflowService.getBPSProfileParams(
                (String) initParams.get(WorkFlowConstants.TemplateConstants.BPEL_IMPL_BPS_PROFILE));
        initParams.putAll(bpelProfileParams);
        initParams.put(WorkFlowConstants.TemplateConstants.SERVICE_ACTION, WorkFlowConstants.TemplateConstants
                .DEFAULT_APPROVAL_BPEL_SOAP_ACTION);
        super.initializeExecutor(initParams);
    }

    @Override
    public String getTemplateId() {

        return WorkFlowConstants.TemplateConstants.APPROVAL_TEMPLATE_ID;
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
