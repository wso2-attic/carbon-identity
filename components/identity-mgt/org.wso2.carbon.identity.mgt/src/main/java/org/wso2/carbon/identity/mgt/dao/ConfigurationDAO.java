package org.wso2.carbon.identity.mgt.dao;

import org.wso2.carbon.identity.mgt.beans.TenantConfigBean;

public interface ConfigurationDAO {

    TenantConfigBean getConfigurations(int tenantId);

    void addConfigurations(TenantConfigBean tenantConfigBean);

    void updateConfigurations(TenantConfigBean tenantConfigBean);

}
