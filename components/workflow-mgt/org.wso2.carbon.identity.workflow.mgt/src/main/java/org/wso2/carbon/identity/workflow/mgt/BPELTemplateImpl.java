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

import java.util.ArrayList;
import java.util.List;

public class BPELTemplateImpl extends AbstractWorkflowTemplateImpl {

    private static final List<String> TEMPLATES;
    private static final TemplateParameterDef[] PARAMETER_DEFINITIONS;

    static {
        TEMPLATES = new ArrayList<>();
        TEMPLATES.add(WorkFlowConstants.TemplateConstants.APPROVAL_TEMPLATE_NAME);

        TemplateParameterDef bpsProfile = new TemplateParameterDef();
        bpsProfile.setParamName(WorkFlowConstants.TemplateConstants.BPEL_IMPL_BPS_PROFILE);
        bpsProfile.setParamType(WorkflowTemplateParamType.BPS_PROFILE);
        PARAMETER_DEFINITIONS = new TemplateParameterDef[]{bpsProfile};
    }

    @Override
    public List<String> getImplementedTemplateIds() {
        return null;
    }

    @Override
    public TemplateParameterDef[] getImplParamDefinitions() {
        return new TemplateParameterDef[0];
    }

    @Override
    public String getImplementationId() {
        return null;
    }
}
