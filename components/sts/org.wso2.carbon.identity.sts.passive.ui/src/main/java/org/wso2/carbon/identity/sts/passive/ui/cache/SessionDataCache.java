/*
 * Copyright (c) 2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.sts.passive.ui.cache;

import org.wso2.carbon.identity.application.authentication.framework.store.SessionDataStore;
import org.wso2.carbon.identity.application.common.cache.BaseCache;
import org.wso2.carbon.identity.core.util.IdentityUtil;

public class SessionDataCache extends BaseCache<SessionDataCacheKey, SessionDataCacheEntry> {

    private static final String SESSION_DATA_CACHE_NAME = "PassiveSTSSessionDataCache";
    private static volatile SessionDataCache instance;
    private boolean isTemporarySessionDataPersistEnabled = false;

    private SessionDataCache() {
        super(SESSION_DATA_CACHE_NAME);
        if (IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.Temporary") != null) {
            isTemporarySessionDataPersistEnabled = Boolean.parseBoolean(
                    IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.Temporary"));
        }
    }

    public static SessionDataCache getInstance() {
        if (instance == null) {
            synchronized (SessionDataCache.class) {
                if (instance == null) {
                    instance = new SessionDataCache();
                }
            }
        }
        return instance;
    }

    public void addToCache(SessionDataCacheKey key, SessionDataCacheEntry entry){
        super.addToCache(key, entry);
        if (isTemporarySessionDataPersistEnabled) {
            SessionDataStore.getInstance().storeSessionData(key.getSessionDataKey(), SESSION_DATA_CACHE_NAME, entry);
        }
    }

    public void clearCacheEntry(SessionDataCacheKey key){
        super.clearCacheEntry(key);
        if (isTemporarySessionDataPersistEnabled) {
            SessionDataStore.getInstance().clearSessionData(key.getSessionDataKey(), SESSION_DATA_CACHE_NAME);
        }
    }

    public SessionDataCacheEntry getValueFromCache(SessionDataCacheKey key) {
        SessionDataCacheEntry cacheEntry = super.getValueFromCache(key);
        if(cacheEntry == null && isTemporarySessionDataPersistEnabled){
            cacheEntry = (SessionDataCacheEntry) SessionDataStore.getInstance().getSessionData(key.getSessionDataKey(),
                    SESSION_DATA_CACHE_NAME);
        }
        return cacheEntry;
    }
}
