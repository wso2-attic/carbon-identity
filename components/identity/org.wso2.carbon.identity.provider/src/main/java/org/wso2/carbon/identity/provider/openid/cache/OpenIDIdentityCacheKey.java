package org.wso2.carbon.identity.provider.openid.cache;


import org.wso2.carbon.base.MultitenantConstants;

/**
 * Identity Cache key which wraps the identity related cache key values
 */
public class OpenIDIdentityCacheKey extends OpenIDCacheKey {

    private int tenantId;
    private String key;
    private static final long serialVersionUID = -7700438046096986522L;

    public OpenIDIdentityCacheKey(int tenantId, String key) {
        this.tenantId = tenantId;
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public int getTenantId() {
        return tenantId;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof OpenIDIdentityCacheKey)) {
            return false;
        }
        OpenIDIdentityCacheKey cacheKey = (OpenIDIdentityCacheKey) obj;

        return cacheKey.getKey() != null &&  cacheKey.getKey().equals(key) &&
               cacheKey.getTenantId() == tenantId;

    }

    @Override
    public int hashCode() {
        int hash = Integer.valueOf( (tenantId == MultitenantConstants.SUPER_TENANT_ID) 
        		? 0 : tenantId).hashCode();
        hash += hash + key.hashCode();
        return hash;
    }
}
