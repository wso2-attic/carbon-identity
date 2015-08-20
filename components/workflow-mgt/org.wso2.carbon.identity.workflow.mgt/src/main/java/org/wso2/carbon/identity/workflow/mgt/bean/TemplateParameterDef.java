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

public class TemplateParameterDef {

    private String paramName;
    private String displayName;
    private String paramType;
    private String defaultValue;
    private boolean mandatory;

    public String getParamName() {

        return paramName;
    }

    public void setParamName(String paramName) {

        this.paramName = paramName;
    }

    public String getParamType() {

        return paramType;
    }

    public void setParamType(String paramType) {

        this.paramType = paramType;
    }

    public boolean isMandatory() {

        return mandatory;
    }

    public void setMandatory(boolean mandatory) {

        this.mandatory = mandatory;
    }

    public String getDisplayName() {

        return displayName;
    }

    public void setDisplayName(String displayName) {

        this.displayName = displayName;
    }

    public String getDefaultValue() {

        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {

        this.defaultValue = defaultValue;
    }
}
