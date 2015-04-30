/*
 * Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.identity.mgt.store;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.mgt.dto.UserIdentityClaimsDO;
import org.wso2.carbon.user.api.UserStoreManager;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;

/**
 *
 */
public class InMemoryIdentityDataStore extends UserIdentityDataStore {

    private static final String IDENTITY_LOGIN_DATA_CACHE_MANAGER = "IDENTITY_LOGIN_DATA_CACHE_MANAGER";
    private static final String IDENTITY_LOGIN_DATA_CACHE = "IDENTITY_LOGIN_DATA_CACHE";

    //protected Cache<String, UserIdentityClaimsDO> cache = getCache();//CarbonUtils.getLocalCache("IDENTITY_LOGIN_DATA_CACHE");

    private static Log log = LogFactory.getLog(InMemoryIdentityDataStore.class);

    protected Cache<String, UserIdentityClaimsDO> getCache() {
        CacheManager manager = Caching.getCacheManagerFactory().getCacheManager(InMemoryIdentityDataStore.IDENTITY_LOGIN_DATA_CACHE_MANAGER);
        Cache<String, UserIdentityClaimsDO> cache = manager.getCache(InMemoryIdentityDataStore.IDENTITY_LOGIN_DATA_CACHE);
        return cache;
    }

    @Override
    public void store(UserIdentityClaimsDO userIdentityDTO, UserStoreManager userStoreManager)
            throws IdentityException {
        if (userIdentityDTO != null && userIdentityDTO.getUserName() != null) {

            String userStoreSalt = userStoreManager.hashCode()+"";

            String key =
                    userStoreSalt+ CarbonContext.getThreadLocalCarbonContext().getTenantId() +
                            userIdentityDTO.getUserName();
//			if (cache.containsKey(key)) {
//				invalidateCache(userIdentityDTO.getUserName());
//			}

            Cache<String, UserIdentityClaimsDO> cache = getCache();
            if (cache != null) {
                cache.put(key, userIdentityDTO);
            }
        }
    }

    @Override
    public UserIdentityClaimsDO load(String userName, UserStoreManager userStoreManager) {

        Cache<String, UserIdentityClaimsDO> cache = getCache();
        if (userName != null && cache != null) {
            return (UserIdentityClaimsDO) cache.get(userStoreManager.hashCode() + "" +CarbonContext.getThreadLocalCarbonContext().getTenantId() +
                    userName);
        }
        return null;
    }

    public void remove(String userName, UserStoreManager userStoreManager) throws IdentityException {
        Cache<String, UserIdentityClaimsDO> cache = getCache();
        if (userName == null) {
            return;
        }

        cache.remove(userStoreManager.hashCode() + "" +CarbonContext.getThreadLocalCarbonContext().getTenantId() + userName);

//		invalidateCache(userName);
    }

//	public void invalidateCache(String userName){
//
//		if (log.isDebugEnabled()) {
//			log.debug("Init invalidation caching process");
//		}
    // sending cluster message
//		CacheInvalidator invalidator = IdentityMgtServiceComponent.getCacheInvalidator();
//		try {
//			if (invalidator != null) {
//				invalidator.invalidateCache("IDENTITY_LOGIN_DATA_CACHE",
//				                            CarbonContext.getCurrentContext().getTenantId() +
//				                                    userName);
//				if (log.isDebugEnabled()) {
//					log.debug("Calling invalidation cache");
//				}
//			} else {
//				if (log.isDebugEnabled()) {
//					log.debug("Not calling invalidation cache");
//				}
//			}
//		} catch (CacheException e) {
//			log.error("Error while invalidating cache", e);
//		}
//	}
}
