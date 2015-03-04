/*
 * Copyright (c) 2015 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.workflow.mgt.bean;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * This is the DTO that will be used for storing workflow related contextual information.
 */
public class WorkflowPersistenceDataBean implements Serializable {

    private String workflowId;

    private long createdTime;

    private String tenantDomain;

    private Map<String,Object> workflowParamMap = new HashMap<String, Object>();

    public void addParam(String key, Object value){
        workflowParamMap.put(key, value);
    }

    public Object getParam(String key){
        return workflowParamMap.get(key);
    }

    public Map<String, Object> getWorkflowParamMap(){
        return workflowParamMap;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public String getTenantDomain() {
        return tenantDomain;
    }

    public void setTenantDomain(String tenantDomain) {
        this.tenantDomain = tenantDomain;
    }
}
