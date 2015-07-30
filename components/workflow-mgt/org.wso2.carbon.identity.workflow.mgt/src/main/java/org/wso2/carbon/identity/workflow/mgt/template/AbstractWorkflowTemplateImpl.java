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

package org.wso2.carbon.identity.workflow.mgt.template;

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.identity.workflow.mgt.bean.TemplateParameterDef;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkFlowRequest;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;

import java.util.Map;

public abstract class AbstractWorkflowTemplateImpl {

    protected abstract TemplateInitializer getInitializer();

    protected abstract WorkFlowExecutor getExecutor();

    public void activate(Map<String, Object> initParams) throws WorkflowException {

        TemplateInitializer initializer = getInitializer();
        if (initializer != null && initializer.initNeededAtStartUp()) {
            deploy(initParams);
        }
    }

    public void initializeExecutor(Map<String, Object> initParams) throws WorkflowException {

        WorkFlowExecutor executor = getExecutor();
        if (executor != null) {
            executor.initialize(initParams);
        }
    }

    public void deploy(Map<String, Object> initParams) throws WorkflowException {

        TemplateInitializer initializer = getInitializer();
        if (initializer != null) {
            initializer.initialize(initParams);
        }
    }

    public void execute(WorkFlowRequest workFlowRequest) throws WorkflowException {

        WorkFlowExecutor executor = getExecutor();
        if (executor != null) {
            executor.execute(workFlowRequest);
        }
    }

    public abstract String getTemplateId();

    public abstract TemplateParameterDef[] getImplParamDefinitions();

    public abstract String getImplementationId();

    public abstract String getImplementationName();

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof AbstractWorkflowTemplateImpl)) {
            return false;
        }
        AbstractWorkflowTemplateImpl that = (AbstractWorkflowTemplateImpl) o;
        return StringUtils.equals(getTemplateId(), that.getTemplateId()) &&
                StringUtils.equals(getImplementationId(), that
                        .getImplementationId());

    }

    @Override
    public int hashCode() {

        int result = getTemplateId() != null ? getTemplateId().hashCode() : 0;
        result = 31 * result + (getImplementationId() != null ? getImplementationId().hashCode() : 0);
        return result;
    }
}
