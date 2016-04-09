/*
 *  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */

package org.wso2.carbon.identity.uma.dto;

import org.wso2.carbon.identity.uma.beans.authz.ClaimTokenBean;
import org.wso2.carbon.identity.uma.beans.authz.UmaRptRequestPayloadBean;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public class UmaRptRequest extends UmaRequest{

    private UmaRptRequestPayloadBean requestPayloadBean;

    private String resourceId;

    private String[] requestedScopes;


    public UmaRptRequest(HttpServletRequest request, UmaRptRequestPayloadBean payloadBean){
        super(request);
        requestPayloadBean = payloadBean;

    }

    public UmaRptRequestPayloadBean getRequestPayloadBean() {
        return requestPayloadBean;
    }

    public void setRequestPayloadBean(UmaRptRequestPayloadBean requestPayloadBean) {
        this.requestPayloadBean = requestPayloadBean;
    }

    public String getPermissionTicket() {
        return (requestPayloadBean != null) ? requestPayloadBean.getTicket() : null;
    }


    public String getRpt() {
       return (requestPayloadBean != null) ? requestPayloadBean.getRpt() : null;
    }

    public List<ClaimTokenBean> getClaimTokens() {
        return (requestPayloadBean != null) ? requestPayloadBean.getClaim_tokens() : null;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String[] getRequestedScopes() {
        return requestedScopes;
    }

    public void setRequestedScopes(String[] requestedScopes) {
        this.requestedScopes = requestedScopes;
    }
}
