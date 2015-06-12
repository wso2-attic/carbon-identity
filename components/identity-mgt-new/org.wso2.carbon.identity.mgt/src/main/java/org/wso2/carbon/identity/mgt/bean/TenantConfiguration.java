/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.mgt.bean;

import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class TenantConfiguration implements Serializable {

    private int tenantId = MultitenantConstants.SUPER_TENANT_ID;

    Map<String, String> configurationDetails = new HashMap<>();

    public TenantConfiguration(int tenantId, Map<String, String> configurationDetails) {
        this.tenantId = tenantId;
        this.configurationDetails = configurationDetails;
    }

    public int getTenantId() {
        return tenantId;
    }

    public void setTenantId(int tenantId) {
        this.tenantId = tenantId;
    }

    public Map<String, String> getConfigurationDetails() {
        return configurationDetails;
    }

    public void setConfigurationDetails(Map<String, String> configurationDetails) {
        this.configurationDetails = configurationDetails;
    }
}
