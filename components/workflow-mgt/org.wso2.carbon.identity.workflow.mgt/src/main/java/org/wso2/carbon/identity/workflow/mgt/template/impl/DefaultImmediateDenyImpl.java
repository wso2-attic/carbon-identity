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
import org.wso2.carbon.identity.workflow.mgt.template.AbstractWorkflowTemplateImpl;
import org.wso2.carbon.identity.workflow.mgt.template.TemplateInitializer;
import org.wso2.carbon.identity.workflow.mgt.template.WorkFlowExecutor;

public class DefaultImmediateDenyImpl extends AbstractWorkflowTemplateImpl {

    private static final String IMPL_NAME = "Default";
    private WorkFlowExecutor executor;

    public DefaultImmediateDenyImpl() {
        executor = new ImmediateDenyExecutor();
    }

    @Override
    protected TemplateInitializer getInitializer() {

        return null;    //no initializer needed for this
    }

    @Override
    protected WorkFlowExecutor getExecutor() {

        return executor;
    }

    @Override
    public String getTemplateId() {

        return WorkFlowConstants.TemplateConstants.IMMEDIATE_DENY_TEMPLATE_ID;
    }

    @Override
    public TemplateParameterDef[] getImplParamDefinitions() {

        return new TemplateParameterDef[0];
    }

    @Override
    public String getImplementationId() {

        return IMPL_NAME;
    }

    @Override
    public String getImplementationName() {

        return IMPL_NAME;
    }
}
