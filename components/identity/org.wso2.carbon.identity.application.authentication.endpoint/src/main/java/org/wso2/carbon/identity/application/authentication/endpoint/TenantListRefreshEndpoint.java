/*
* Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.carbon.identity.application.authentication.endpoint;

import org.wso2.carbon.identity.application.authentication.endpoint.util.TenantDataManager;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;

/**
 * Class for receiving the list of tenants upon any modification to a tenant
 */
public class TenantListRefreshEndpoint extends HttpServlet {

    /**
     * Parameter name for tenant list
     */
    private static final String TENANT_LIST = "tenantList";

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        String tenantList = request.getParameter(TENANT_LIST);

        if (StringUtils.isNotEmpty(tenantList)) {
            TenantDataManager.setTenantDataList(tenantList);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        String tenantList = request.getParameter(TENANT_LIST);

        if (StringUtils.isNotEmpty(tenantList)) {
            TenantDataManager.setTenantDataList(tenantList);
        }
    }
}
