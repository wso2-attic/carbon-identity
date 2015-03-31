/*
*Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.wso2.carbon.identity.application.authentication.framework.cache;

import org.wso2.carbon.identity.application.authentication.framework.store.SessionDataStore;
import org.wso2.carbon.identity.application.common.cache.BaseCache;
import org.wso2.carbon.identity.application.common.cache.CacheEntry;
import org.wso2.carbon.identity.application.common.cache.CacheKey;
import org.wso2.carbon.identity.core.util.IdentityUtil;

public class AuthenticationContextCache extends BaseCache<CacheKey, CacheEntry> {

    private static final String AUTHENTICATION_CONTEXT_CACHE_NAME = "AuthenticationContextCache";
    private static volatile AuthenticationContextCache instance ;
    private boolean useCache = true;
    private boolean enableTemporaryCaches = true;

    private AuthenticationContextCache(String cacheName) {
        super(cacheName);
    }
    
    private AuthenticationContextCache(String cacheName, int timeout) {
        super(cacheName, timeout);
        useCache = !Boolean.parseBoolean(IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.Only"));
        if(IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.Temporary") != null){
            enableTemporaryCaches = Boolean.parseBoolean(IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.Temporary"));
        }
    }

    public static AuthenticationContextCache getInstance(int timeout) {
    	if (instance == null) {
    		synchronized (AuthenticationContextCache.class) {
				if (instance == null) {
					instance = new AuthenticationContextCache(AUTHENTICATION_CONTEXT_CACHE_NAME, timeout);
				}
			}
    	}
        return instance;
    }

    @Override
    public void addToCache(CacheKey key, CacheEntry entry) {
    	String keyValue = ((AuthenticationContextCacheKey) key).getContextId();
        if(useCache){
            super.addToCache(keyValue, entry);
        }
        if(enableTemporaryCaches){            
            SessionDataStore.getInstance().storeSessionData(keyValue, AUTHENTICATION_CONTEXT_CACHE_NAME ,entry);
        }
    }

    @Override
    public CacheEntry getValueFromCache(CacheKey key) {
        String keyValue = ((AuthenticationContextCacheKey) key).getContextId();
        CacheEntry cacheEntry = null;
        if(useCache){
            cacheEntry = super.getValueFromCache(keyValue);
        }
        if(cacheEntry == null){
            cacheEntry = (AuthenticationContextCacheEntry) SessionDataStore.getInstance().
                                                        getSessionData(keyValue, AUTHENTICATION_CONTEXT_CACHE_NAME);
        }
        return cacheEntry;
    }

    @Override
    public void clearCacheEntry(CacheKey key) {
        String keyValue = ((AuthenticationContextCacheKey) key).getContextId();
        if(useCache){
            super.clearCacheEntry(keyValue);
        }
        if(enableTemporaryCaches){
                SessionDataStore.getInstance().clearSessionData(keyValue, AUTHENTICATION_CONTEXT_CACHE_NAME);
        }
    }
}
