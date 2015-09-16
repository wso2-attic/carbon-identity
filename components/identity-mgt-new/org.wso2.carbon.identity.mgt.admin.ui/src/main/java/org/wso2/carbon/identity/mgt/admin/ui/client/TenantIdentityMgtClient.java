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

package org.wso2.carbon.identity.mgt.admin.ui.client;

import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.mgt.stub.UserIdentityManagementAdminServiceStub;
import org.wso2.carbon.identity.mgt.stub.dto.TenantConfigDTO;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.rmi.RemoteException;


public class TenantIdentityMgtClient {

    protected UserIdentityManagementAdminServiceStub stub = null;

    protected static Log log = LogFactory.getLog(TenantIdentityMgtClient.class);

    public TenantIdentityMgtClient(String cookie, String backendServerURL,
                                   ConfigurationContext configContext)
            throws Exception {
        try {
            stub = new UserIdentityManagementAdminServiceStub(configContext, backendServerURL +
                    IdentityMgtAdminUIConstants.IDENTITY_MGT_ADMIN_SERVICE_URL);
            ServiceClient client = stub._getServiceClient();
            Options option = client.getOptions();
            option.setManageSession(true);
            option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);
        } catch (Exception e) {
            throw new Exception("Error occurred while creating TenantIdentityMgtClient Object", e);
        }
    }


    public void updateConfiguration(Map<String, String> configMap) {

        try {
            TenantConfigDTO[] tenantConfigDTOs = new TenantConfigDTO[configMap.size()];
            Iterator<Map.Entry<String, String>> iterator = configMap.entrySet().iterator();
            int count = 0;
            while (iterator.hasNext()) {
                Map.Entry<String, String> pair = iterator.next();
                TenantConfigDTO tenantConfigDTO = new TenantConfigDTO();
                tenantConfigDTO.setProperty(pair.getKey());
                tenantConfigDTO.setPropertyValue(pair.getValue());
                tenantConfigDTOs[count] = tenantConfigDTO;
                iterator.remove();
                ++count;
            }

            stub.updateConfiguration(tenantConfigDTOs);
        } catch (RemoteException e) {
            log.error("Error occurred when updating configuration details", e);
        }
    }

    public Map<String, String> getConfiguration() {
        Map<String, String> configMap = new HashMap<>();

        try {
            TenantConfigDTO[] tenantConfigDTOs = stub.getConfiguration();

            for (int i = 0; i < tenantConfigDTOs.length; i++) {
                TenantConfigDTO tenantConfigDTO = tenantConfigDTOs[i];
                configMap.put(tenantConfigDTO.getProperty(),
                        tenantConfigDTO.getPropertyValue());
            }

        } catch (RemoteException e) {
            log.error("Error occurred when retrieving configuration details", e);
        }

        return configMap;

    }
}


