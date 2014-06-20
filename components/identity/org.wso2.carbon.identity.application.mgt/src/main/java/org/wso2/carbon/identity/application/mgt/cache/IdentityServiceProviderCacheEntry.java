package org.wso2.carbon.identity.application.mgt.cache;

import org.wso2.carbon.identity.application.common.cache.CacheEntry;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;

import java.io.Serializable;

public class IdentityServiceProviderCacheEntry extends CacheEntry {

    /**
     * 
     */
    private static final long serialVersionUID = 3112605038259278777L;

    private ServiceProvider serviceProvider;

    public ServiceProvider getServiceProvider() {
        return serviceProvider;
    }

    public void setServiceProvider(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

}
