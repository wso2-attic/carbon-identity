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


public class TenantIdentityMgtClient {

    protected UserIdentityManagementAdminServiceStub stub = null;

    protected static Log log = LogFactory.getLog(TenantIdentityMgtClient.class);

    public TenantIdentityMgtClient(String cookie, String backendServerURL,
                                   ConfigurationContext configContext)
            throws Exception {
        try {
            stub = new UserIdentityManagementAdminServiceStub(configContext, backendServerURL + "UserIdentityManagementAdminService");
            ServiceClient client = stub._getServiceClient();
            Options option = client.getOptions();
            option.setManageSession(true);
            option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);
        } catch (Exception e) {
        }
    }


    public void setAllConfigurations(HashMap<String, String> configMap) {

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

            stub.setAllConfigurations(tenantConfigDTOs);
        } catch (java.rmi.RemoteException e) {
        }
    }

    public HashMap<String, String> getAllConfigurations() {
        HashMap<String, String> configMap = new HashMap<String, String>();

        try {
            TenantConfigDTO[] tenantConfigDTOs = stub.getAllConfigurations();

            for (int i = 0; i < tenantConfigDTOs.length; i++) {
                TenantConfigDTO tenantConfigDTO = tenantConfigDTOs[i];
                configMap.put(tenantConfigDTO.getProperty(),
                        tenantConfigDTO.getPropertyValue());
            }

        } catch (java.rmi.RemoteException e) {
        }

        return configMap;

    }
}


