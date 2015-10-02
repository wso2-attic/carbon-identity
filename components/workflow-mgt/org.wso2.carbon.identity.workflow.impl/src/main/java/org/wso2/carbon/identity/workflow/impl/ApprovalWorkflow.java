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
import org.wso2.carbon.identity.workflow.mgt.bean.metadata.ParameterMetaData;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;
import org.wso2.carbon.identity.workflow.mgt.workflow.AbstractWorkflow;
import org.wso2.carbon.identity.workflow.mgt.workflow.TemplateInitializer;
import org.wso2.carbon.identity.workflow.mgt.workflow.WorkFlowExecutor;

import java.util.List;

public class ApprovalWorkflow extends AbstractWorkflow {

    private static Log log = LogFactory.getLog(ApprovalWorkflow.class);

    @Override
    protected InputData getInputData(ParameterMetaData parameterMetaData) throws WorkflowException {
        InputData inputData = null;
        if (parameterMetaData != null && parameterMetaData.getName() != null) {
            String parameterName = parameterMetaData.getName();
            if (parameterName.equals(WFImplConstant.ParameterName.BPS_PROFILE)) {

                int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
                try {
                    List<BPSProfile> bpsProfiles = WorkflowImplServiceDataHolder.getInstance().getWorkflowImplService()
                            .listBPSProfiles(tenantId);
                    if (bpsProfiles != null && bpsProfiles.size() > 0) {
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
                    throw new WorkflowException(errorMsg, e);
                }
            }
        }
        return inputData;
    }

    public ApprovalWorkflow(Class<? extends TemplateInitializer> templateInitializerClass, Class<? extends WorkFlowExecutor> workFlowExecutorClass,
                            String metaDataXML) {
        super(templateInitializerClass, workFlowExecutorClass, metaDataXML);
    }

    @Override
    public void deploy(List<Parameter> parameterList) throws WorkflowException {
        super.deploy(parameterList);
    }

}
