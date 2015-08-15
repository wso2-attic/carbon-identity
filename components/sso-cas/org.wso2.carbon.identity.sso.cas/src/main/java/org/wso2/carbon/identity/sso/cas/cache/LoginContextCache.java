package org.wso2.carbon.identity.sso.cas.cache;

import org.wso2.carbon.identity.application.authentication.framework.store.SessionDataStore;
import org.wso2.carbon.identity.application.common.cache.BaseCache;
import org.wso2.carbon.identity.application.common.cache.CacheEntry;
import org.wso2.carbon.identity.application.common.cache.CacheKey;
import org.wso2.carbon.identity.core.util.IdentityUtil;

public class LoginContextCache extends BaseCache<CacheKey, CacheEntry> {

	    private static final String SESSION_DATA_CACHE_NAME = "CASLoginContextCache";
	    private static volatile LoginContextCache instance;
	    private boolean useCache = true;

	    private LoginContextCache(String cacheName) {
	        super(cacheName);
	    }
	    
	    private LoginContextCache(String cacheName, int timeout) {
	        super(cacheName, timeout);
	        useCache = !Boolean.parseBoolean(IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.Only"));
	    }

	    public static LoginContextCache getInstance(int timeout) {
	    	if (instance == null) {
	    		synchronized (LoginContextCache.class) {
	    			
					if (instance == null) {
						instance = new LoginContextCache(SESSION_DATA_CACHE_NAME, timeout);
					}
				}
	    	}
	        return instance;
	    }
	    
	    @Override
	    public void addToCache(CacheKey key, CacheEntry entry) {
	        if(useCache){
	            super.addToCache(key, entry);
	        }
	        String keyValue = ((LoginContextCacheKey) key).getSessionDataKey();
	        SessionDataStore.getInstance().storeSessionData(keyValue, SESSION_DATA_CACHE_NAME, entry);
	    }

	    @Override
	    public CacheEntry getValueFromCache(CacheKey key) {
	        CacheEntry cacheEntry = null;
	        if(useCache){
	            cacheEntry = super.getValueFromCache(key);
	        }
	        if(cacheEntry == null){
	            String keyValue = ((LoginContextCacheKey) key).getSessionDataKey();
	            cacheEntry = (LoginContextCacheEntry) SessionDataStore.getInstance().
	                                                    getSessionData(keyValue, SESSION_DATA_CACHE_NAME);
	        }
	        return cacheEntry;
	    }

	    @Override
	    public void clearCacheEntry(CacheKey key) {
	        if(useCache){
	            super.clearCacheEntry(key);
	        }
	        String keyValue = ((LoginContextCacheKey) key).getSessionDataKey();
	        SessionDataStore.getInstance().clearSessionData(keyValue, SESSION_DATA_CACHE_NAME);
	    }
}
