/*
*  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.carbon.identity.user.store.configuration.randompasswordcache;

import org.wso2.carbon.identity.user.store.configuration.randompasswordcache.randompasswordobject.RandomPasswordContainer;

import javax.cache.Cache;
import javax.cache.CacheConfiguration;
import javax.cache.CacheManager;
import javax.cache.Caching;
import java.util.concurrent.TimeUnit;

/**
 * Class to hold the reference for distributed cache object
 */
public class RandomPasswordContainerCache {

    //hashMap implementation
    private static boolean secondaryStorageIDIncrementalCounter;

    /**
     * Get the cache which holds the incremental unique id counter
     * @return Cache object for incremental unique ID
     */
    public static Cache<Integer, String> getIdIncrementalCounterCache() {
        if (secondaryStorageIDIncrementalCounter) {
            return Caching.getCacheManagerFactory().getCacheManager(
                    RandomPasswordContainerCacheConstants.SECONDARY_STORAGE_CACHE_MANAGER).
                    getCache(RandomPasswordContainerCacheConstants.ID_INCREMENTAL_COUNTER_CACHE);
        } else {
            secondaryStorageIDIncrementalCounter = true;

            CacheManager cacheManager = Caching.getCacheManagerFactory().
                    getCacheManager(RandomPasswordContainerCacheConstants.SECONDARY_STORAGE_CACHE_MANAGER);

            return cacheManager.<Integer, String>createCacheBuilder(
                    RandomPasswordContainerCacheConstants.ID_INCREMENTAL_COUNTER_CACHE).
                   setExpiry(CacheConfiguration.ExpiryType.MODIFIED, new CacheConfiguration.Duration(TimeUnit.SECONDS,
                            RandomPasswordContainerCacheConstants.CACHE_INVALIDATION_TIME)).
                    setExpiry(CacheConfiguration.ExpiryType.ACCESSED, new CacheConfiguration.Duration(TimeUnit.SECONDS,
                            RandomPasswordContainerCacheConstants.CACHE_INVALIDATION_TIME)).
                    setStoreByValue(false).build();

        }
    }

    /**
     * Get the cache which holds the RandomPasswordContainer cache
     * @return Cache object of RandomPasswordContainerCache
     */
    public static Cache<Long, RandomPasswordContainer> getRandomPasswordContainerCache() {
            return Caching.getCacheManagerFactory().getCacheManager(
                    RandomPasswordContainerCacheConstants.SECONDARY_STORAGE_CACHE_MANAGER).
                    getCache(RandomPasswordContainerCacheConstants.RANDOM_PASSWORD_CONTAINER_CACHE);
    }


}
