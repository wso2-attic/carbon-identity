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

package org.wso2.carbon.identity.workflow.mgt.bean;

public class TemplateDeploymentDTO {
    private String workflowName;
    private String templateName;
    private String templateImplName;
    private String associatedEvent;
    private TemplateDeploymentParameter[] templateParameters;
    private TemplateDeploymentParameter[] templateImplParameters;
    private String condition;

    public String getWorkflowName() {
        return workflowName;
    }

    public void setWorkflowName(String workflowName) {
        this.workflowName = workflowName;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplateImplName() {
        return templateImplName;
    }

    public void setTemplateImplName(String templateImplName) {
        this.templateImplName = templateImplName;
    }

    public TemplateDeploymentParameter[] getTemplateParameters() {
        return templateParameters;
    }

    public void setTemplateParameters(
            TemplateDeploymentParameter[] templateParameters) {
        this.templateParameters = templateParameters;
    }

    public TemplateDeploymentParameter[] getTemplateImplParameters() {
        return templateImplParameters;
    }

    public void setTemplateImplParameters(
            TemplateDeploymentParameter[] templateImplParameters) {
        this.templateImplParameters = templateImplParameters;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getAssociatedEvent() {
        return associatedEvent;
    }

    public void setAssociatedEvent(String associatedEvent) {
        this.associatedEvent = associatedEvent;
    }
}
