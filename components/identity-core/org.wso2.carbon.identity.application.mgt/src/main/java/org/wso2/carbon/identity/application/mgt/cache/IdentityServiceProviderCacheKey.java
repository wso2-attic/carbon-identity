package org.wso2.carbon.identity.application.mgt.cache;

import org.wso2.carbon.identity.application.common.cache.CacheKey;

public class IdentityServiceProviderCacheKey extends CacheKey {

    /**
     *
     */
    private static final long serialVersionUID = 8263255365985309443L;

    private String serviceProviderKey;

    /**
     * @param serviceProviderName
     * @param tenantDomain
     */
    public IdentityServiceProviderCacheKey(String serviceProviderName, String tenantDomain) {
        this.serviceProviderKey = serviceProviderName;
        this.tenantDomain = tenantDomain.toLowerCase();
    }

    /**
     * @return
     */
    public String getServiceProviderKey() {
        return serviceProviderKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        IdentityServiceProviderCacheKey that = (IdentityServiceProviderCacheKey) o;

        if (!serviceProviderKey.equals(that.serviceProviderKey)) return false;
        if (!tenantDomain.equals(that.tenantDomain)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + serviceProviderKey.hashCode();
        result = 31 * result + tenantDomain.hashCode();
        return result;
    }
}
