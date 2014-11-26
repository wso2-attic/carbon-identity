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

/**
 *  Constants to hold password cache array
 */
public final class RandomPasswordContainerCacheConstants {

    private RandomPasswordContainerCacheConstants(){

    }

    //name constant of the cache manager
    public static final String SECONDARY_STORAGE_CACHE_MANAGER = "secondaryStorageCacheManager";

    //incremental counter cache
    public static final String ID_INCREMENTAL_COUNTER_CACHE = "idIncrementalCounterCache";

    //random password container cache
    public static final String RANDOM_PASSWORD_CONTAINER_CACHE = "randomPasswordContainerCache";


    //cache invalidation time
    public static final Integer CACHE_INVALIDATION_TIME = 1000 * 24 * 3600;

    //unique ID to identify the incremental ID counter
    public static final Integer INCREMENTAL_ID_COUNTER = 9999;
}

