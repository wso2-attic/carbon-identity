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
import org.wso2.carbon.identity.workflow.mgt.exception.RuntimeWorkflowException;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;

import java.util.HashMap;
import java.util.Map;

public abstract class WorkflowTemplate {

    private Map<String, AbstractWorkflowTemplateImpl> implementations;

    public WorkflowTemplate() {
        implementations = new HashMap<>();
    }

    public void addImplementation(AbstractWorkflowTemplateImpl impl) throws WorkflowException {
        //todo: check impl's template
        if (impl != null) {
            if (implementations.containsKey(impl.getImplementationId())) {
                throw new RuntimeWorkflowException(
                        "Implementation already exists with id:" + impl.getImplementationId());
            }
            implementations.put(impl.getImplementationId(), impl);
        }
    }

    public AbstractWorkflowTemplateImpl getImplementation(String id) {
        return implementations.get(id);
    }

    public abstract TemplateParameterDef[] getParamDefinitions();

    public abstract String getTemplateId();

    public abstract String getFriendlyName();
}
