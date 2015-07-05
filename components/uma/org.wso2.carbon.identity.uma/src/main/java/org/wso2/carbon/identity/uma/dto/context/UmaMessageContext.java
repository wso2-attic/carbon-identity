/*
 *
 *  *
 *  * Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *  *
 *  * WSO2 Inc. licenses this file to you under the Apache License,
 *  * Version 2.0 (the "License"); you may not use this file except
 *  * in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *  * /
 *
 */

package org.wso2.carbon.identity.uma.dto.context;

import org.wso2.carbon.identity.uma.dto.UmaRequest;
import org.wso2.carbon.identity.uma.dto.UmaResponse;

public class UmaMessageContext {

    protected UmaRequest umaRequest;

    protected UmaResponse umaResponse;

    protected String tenantDomain;

    protected int tenantID;


    protected UmaMessageContext(UmaRequest umaRequest){
        this.umaRequest = umaRequest;

        if (umaRequest != null){
            tenantDomain = umaRequest.getTenantDomain();
            tenantID = umaRequest.getTenantID();
        }
    }


    public UmaRequest getUmaRequest() {
        return umaRequest;
    }

    public void setUmaRequest(UmaRequest umaRequest) {
        this.umaRequest = umaRequest;
    }

    public UmaResponse getUmaResponse() {
        return umaResponse;
    }

    public void setUmaResponse(UmaResponse umaResponse) {
        this.umaResponse = umaResponse;
    }

    public String getTenantDomain() {
        return tenantDomain;
    }

    public void setTenantDomain(String tenantDomain) {
        this.tenantDomain = tenantDomain;
    }

    public int getTenantID() {
        return tenantID;
    }

    public void setTenantID(int tenantID) {
        this.tenantID = tenantID;
    }
}
