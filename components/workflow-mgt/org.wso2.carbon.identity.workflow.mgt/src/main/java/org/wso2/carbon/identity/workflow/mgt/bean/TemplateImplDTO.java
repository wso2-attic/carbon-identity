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

public class TemplateImplDTO {
    private String templateId;
    private String implementationId;
    private String implementationName;
    private TemplateParameterDef[] implementationParams;

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getImplementationId() {
        return implementationId;
    }

    public void setImplementationId(String implementationId) {
        this.implementationId = implementationId;
    }

    public String getImplementationName() {
        return implementationName;
    }

    public void setImplementationName(String implementationName) {
        this.implementationName = implementationName;
    }

    public TemplateParameterDef[] getImplementationParams() {
        return implementationParams;
    }

    public void setImplementationParams(
            TemplateParameterDef[] implementationParams) {
        this.implementationParams = implementationParams;
    }
}
