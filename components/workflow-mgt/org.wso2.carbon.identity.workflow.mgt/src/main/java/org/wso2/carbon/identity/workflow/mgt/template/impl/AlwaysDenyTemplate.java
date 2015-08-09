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
import org.wso2.carbon.identity.workflow.mgt.bean.TemplateParameterDef;
import org.wso2.carbon.identity.workflow.mgt.template.AbstractWorkflowTemplate;

public class AlwaysDenyTemplate extends AbstractWorkflowTemplate {

    private static final String DESCRIPTION = "The operation will denied immediately";
    private static final String TEMPLATE_FRIENDLY_NAME = "Deny Immediately";

    @Override
    public TemplateParameterDef[] getParamDefinitions() {

        return new TemplateParameterDef[0];
    }

    @Override
    public String getTemplateId() {

        return WorkFlowConstants.TemplateConstants.IMMEDIATE_DENY_TEMPLATE_ID;
    }

    @Override
    public String getFriendlyName() {

        return TEMPLATE_FRIENDLY_NAME;
    }

    @Override
    public String getDescription() {

        return DESCRIPTION;
    }
}
