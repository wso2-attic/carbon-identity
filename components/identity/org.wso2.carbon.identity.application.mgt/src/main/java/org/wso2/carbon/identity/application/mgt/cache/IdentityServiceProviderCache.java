package org.wso2.carbon.identity.application.mgt.cache;

import org.wso2.carbon.identity.application.common.cache.BaseCache;
import org.wso2.carbon.identity.application.common.cache.CacheEntry;
import org.wso2.carbon.identity.application.common.cache.CacheKey;

public class IdentityServiceProviderCache extends BaseCache<CacheKey, CacheEntry> {

    public static final String SP_CACHE_NAME = "ServiceProviderCache";

    private static volatile IdentityServiceProviderCache instance;

    public IdentityServiceProviderCache(String cacheName) {
        super(cacheName);
    }

    /**
     *
     * @return
     */
    public static IdentityServiceProviderCache getInstance() {
        if (instance == null) {
            synchronized (IdentityServiceProviderCache.class) {
                if (instance == null) {
                    instance = new IdentityServiceProviderCache(SP_CACHE_NAME);
                }
            }
        }
        return instance;
    }

    @Override
    public void addToCache(CacheKey key, CacheEntry entry) {
        super.addToCache(key, entry);
    }

    @Override
    public CacheEntry getValueFromCache(CacheKey key) {
        return super.getValueFromCache(key);
    }

    @Override
    public void clearCacheEntry(CacheKey key) {
        super.clearCacheEntry(key);
    }
}