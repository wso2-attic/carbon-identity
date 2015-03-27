package org.wso2.carbon.identity.oauth.cache;

import org.wso2.carbon.identity.application.common.cache.BaseCache;
import org.wso2.carbon.identity.application.common.cache.CacheEntry;
import org.wso2.carbon.utils.CarbonUtils;

public class JWTCache extends BaseCache {
    private static final String OAUTH_CACHE_NAME = "JWTCache";
    private static final JWTCache instance = new JWTCache("JWTCache");

    private JWTCache(String cacheName) {
        super(cacheName);
    }

    public static JWTCache getInstance() {
        CarbonUtils.checkSecurity();
        return instance;
    }

    public void addToCache(String key, CacheEntry entry) {
        super.addToCache(key, entry);
    }

    public CacheEntry getValueFromCache(String key) {
        return (CacheEntry) super.getValueFromCache(key);
    }

    public void clearCacheEntry(String key) {
        super.clearCacheEntry(key);
    }

}
