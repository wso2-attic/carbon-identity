/*
 * Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License
 */

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
