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

package org.wso2.carbon.identity.uma.dto;

import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

public class UmaRequest {

    protected HttpServletRequest httpServletRequest;

    protected RequestParameter[] requestParameters;

    protected String tenantDomain;

    protected int tenantID;


    public UmaRequest(HttpServletRequest httpServletRequest){
        this.httpServletRequest = httpServletRequest;

        // set the tenant domain from the request parameters
        tenantDomain = MultitenantUtils.getTenantDomain(httpServletRequest);

        if (tenantDomain != null){
            // get tenantId from tenant domain
        }

        // Store all request parameters
        if (httpServletRequest.getParameterNames() != null) {
            List<RequestParameter> requestParameterList = new ArrayList<RequestParameter>();
            while (httpServletRequest.getParameterNames().hasMoreElements()) {
                String key = httpServletRequest.getParameterNames().nextElement();
                String value = httpServletRequest.getParameter(key);
                requestParameterList.add(new RequestParameter(key, value));
            }
            requestParameters =
                    requestParameterList.toArray(new RequestParameter[requestParameterList.size()]);
        }
    }

    public HttpServletRequest getHttpServletRequest() {
        return httpServletRequest;
    }

    public RequestParameter[] getRequestParameters() {
        return requestParameters;
    }

    public String getTenantDomain() {
        return tenantDomain;
    }

    public int getTenantID() {
        return tenantID;
    }
}
