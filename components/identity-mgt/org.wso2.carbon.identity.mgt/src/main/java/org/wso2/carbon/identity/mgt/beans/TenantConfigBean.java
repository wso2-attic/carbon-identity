/*
 *  Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.carbon.identity.mgt.beans;

import java.io.Serializable;
import java.util.HashMap;

public class TenantConfigBean implements Serializable {

    private int tenantId;

    HashMap<String, String> configurationDetails;

    public TenantConfigBean(int tenantId, HashMap<String, String> configurationDetails) {
        this.tenantId = tenantId;
        this.configurationDetails = configurationDetails;
    }

    public int getTenantId() {
        return tenantId;
    }

    public void setTenantId(int tenantId) {
        this.tenantId = tenantId;
    }

    public HashMap<String, String> getConfigurationDetails() {
        return configurationDetails;
    }

    public void setConfigurationDetails(HashMap<String, String> configurationDetails) {
        this.configurationDetails = configurationDetails;
    }
}
