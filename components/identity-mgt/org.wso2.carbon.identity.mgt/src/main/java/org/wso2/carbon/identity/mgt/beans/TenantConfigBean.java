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
