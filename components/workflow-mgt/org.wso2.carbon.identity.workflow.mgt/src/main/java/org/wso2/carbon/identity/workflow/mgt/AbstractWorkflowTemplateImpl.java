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

import org.wso2.carbon.identity.workflow.mgt.bean.Parameter;
import org.wso2.carbon.identity.workflow.mgt.bean.TemplateParameterDef;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkFlowRequest;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;

import java.util.List;

public abstract class AbstractWorkflowTemplateImpl {

    private TemplateInitializer initializer;
    private WorkFlowExecutor executor;

    public AbstractWorkflowTemplateImpl(TemplateInitializer initializer,
                                        WorkFlowExecutor executor) {
        this.initializer = initializer;
        this.executor = executor;
    }

    public TemplateInitializer getInitializer() {
        return initializer;
    }

    public void setInitializer(TemplateInitializer initializer) {
        this.initializer = initializer;
    }

    public WorkFlowExecutor getExecutor() {
        return executor;
    }

    public void setExecutor(WorkFlowExecutor executor) {
        this.executor = executor;
    }

    public void activate(String templateName, Parameter[] initParams) {
        if(initializer != null && initializer.initNeededAtStartUp()){
            initializer.initialize(templateName, initParams);
        }
    }

    public void initialize(String templateName, Parameter[] initParams) {
        if (initializer != null) {
            initializer.initialize(templateName, initParams);
        }
    }

    public abstract List<String> getImplementedTemplateIds();

    public abstract TemplateParameterDef[] getImplParamDefinitions();

    public void execute(WorkFlowRequest workFlowRequest) throws WorkflowException {
        executor.execute(workFlowRequest);
    }

    public abstract String getImplementationId();

}
