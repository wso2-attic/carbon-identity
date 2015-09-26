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

package org.wso2.carbon.identity.workflow.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.workflow.impl.bean.BPSProfile;
import org.wso2.carbon.identity.workflow.impl.internal.WorkflowImplServiceDataHolder;
import org.wso2.carbon.identity.workflow.mgt.bean.Parameter;
import org.wso2.carbon.identity.workflow.mgt.bean.metadata.InputData;
import org.wso2.carbon.identity.workflow.mgt.bean.metadata.Item;
import org.wso2.carbon.identity.workflow.mgt.bean.metadata.MapType;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;
import org.wso2.carbon.identity.workflow.mgt.util.WFConstant;
import org.wso2.carbon.identity.workflow.mgt.workflow.AbstractWorkflow;
import org.wso2.carbon.identity.workflow.mgt.workflow.TemplateInitializer;
import org.wso2.carbon.identity.workflow.mgt.workflow.WorkFlowExecutor;

import java.util.List;
import java.util.Map;

public class ApprovalWorkflow extends AbstractWorkflow {

    private static Log log = LogFactory.getLog(ApprovalWorkflow.class);

    static {
        /*
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
        */
    }

    private TemplateInitializer initializer;
    private WorkFlowExecutor executor;

    @Override
    protected InputData getInputData(String parameterName) throws WorkflowException {
        InputData inputData = null;
        if(parameterName != null){
            if(parameterName.equals(WFImplConstant.ParameterName.BPS_PROFILE)){

                int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
                try {
                    List<BPSProfile> bpsProfiles = WorkflowImplServiceDataHolder.getInstance().getWorkflowImplService()
                            .listBPSProfiles(tenantId);
                    if(bpsProfiles != null && bpsProfiles.size() > 0) {
                        inputData = new InputData();
                        MapType mapType = new MapType();
                        inputData.setMapType(mapType);
                        Item[] items = new Item[bpsProfiles.size()];
                        for (int i = 0; i < bpsProfiles.size(); i++) {
                            BPSProfile bpsProfile = bpsProfiles.get(i);
                            Item item = new Item();
                            item.setKey(bpsProfile.getProfileName());
                            item.setValue(bpsProfile.getProfileName());
                            items[i] = item;
                        }
                        mapType.setItem(items);
                    }
                } catch (WorkflowImplException e) {
                    String errorMsg = "Error occurred while reading BPSProfiles, " + e.getMessage();
                    log.error(errorMsg);
                    throw new WorkflowException(errorMsg,e);
                }
            }
        }
        return inputData;
    }

    public ApprovalWorkflow(String metaDataXML) {
        super(metaDataXML);
        setExecutor(new RequestExecutor());
    }

    @Override
    public void deploy(List<Parameter>  parameterList) throws WorkflowException {

        //Map<String, Object> bpelProfileParams =
                //WorkflowImplServiceDataHolder.getInstance().getBpelService().getBPSProfileParams(
                        //(String) initParams.get(WorkFlowConstants.TemplateConstants.BPEL_IMPL_BPS_PROFILE));
        //initParams.putAll(bpelProfileParams);
        setInitializer(new BPELDeployer());
        super.deploy(parameterList);
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
    public void initializeExecutor(List<Parameter>  parameterList) throws WorkflowException {
        //read profile and add its params
        /*Map<String, Object> bpelProfileParams =
                WorkflowImplServiceDataHolder.getInstance().getWorkflowImplService().getBPSProfileParams(
                        (String) initParams.get(WFConstant.TemplateConstants.BPEL_IMPL_BPS_PROFILE));
        initParams.putAll(bpelProfileParams);*/

        super.initializeExecutor(parameterList);
    }

    @Override
    public String getWorkflowImplId() {
        return WFImplConstant.WORKFLOW_IMPL_ID;
    }

    @Override
    public String getWorkflowImplName() {
        return WFImplConstant.WORKFLOW_IMPL_NAME;
    }
}
