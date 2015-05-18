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

import java.io.Serializable;
import java.util.List;

public class WorkFlowRequest implements Serializable {
    private String uuid;
    private String eventType;
    private int tenantId;
    private List<WorkflowParameter> workflowParameters;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public List<WorkflowParameter> getWorkflowParameters() {
        return workflowParameters;
    }

    public void setWorkflowParameters(List<WorkflowParameter> workflowParameters) {
        this.workflowParameters = workflowParameters;
    }

    public int getTenantId() {
        return tenantId;
    }

    public void setTenantId(int tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public String toString() {
        return "WorkFlowRequest{" +
                "uuid='" + uuid + "\'\n" +
                ", eventType='" + eventType + "\'\n" +
                ", tenantId=" + tenantId + '\n' +
                ", workflowParameters=" + workflowParameters + '\n' +
                '}';
    }
}
