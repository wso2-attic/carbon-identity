/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.oauth.cache;

import org.wso2.carbon.identity.application.authentication.framework.store.SessionDataStore;
import org.wso2.carbon.identity.application.common.cache.BaseCache;
import org.wso2.carbon.identity.core.model.OAuthAppDO;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.utils.CarbonUtils;

/**
 * AppInfoCache is used to cache oauth application information.
 */
public class AppInfoCache extends BaseCache<String, OAuthAppDO> {

    private static final String OAUTH_APP_INFO_CACHE_NAME = "AppInfoCache";

    private static volatile AppInfoCache instance;
    private boolean enableRequestScopeCache = false;

    private AppInfoCache(String cacheName, int timeout) {
        super(cacheName, timeout);
        if (IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.Temporary") != null) {
            enableRequestScopeCache = Boolean.parseBoolean(IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.Temporary"));
        }
    }

    /**
     * Returns AppInfoCache instance
     *
     * @return instance of OAuthAppInfoCache
     */
    public static AppInfoCache getInstance(int timeout) {
        CarbonUtils.checkSecurity();
        if (instance == null) {
            synchronized (SessionDataCache.class) {
                if (instance == null) {
                    instance = new AppInfoCache(OAUTH_APP_INFO_CACHE_NAME, timeout);
                }
            }
        }
        return instance;
    }

    @Override
    public void addToCache(String key, OAuthAppDO entry) {
        super.addToCache(key, entry);
        SessionDataStore.getInstance().storeSessionData(key, OAUTH_APP_INFO_CACHE_NAME, entry);
        if(enableRequestScopeCache){
            SessionDataStore.getInstance().storeSessionData(key,OAUTH_APP_INFO_CACHE_NAME,entry);
        }
    }

    @Override
    public OAuthAppDO getValueFromCache(String key) {
        OAuthAppDO oAuthAppDO = super.getValueFromCache(key);
        if (oAuthAppDO == null) {
            oAuthAppDO = (OAuthAppDO) SessionDataStore.getInstance().getSessionData(key, OAUTH_APP_INFO_CACHE_NAME);
        }
        return oAuthAppDO;
    }

    @Override
    public void clearCacheEntry(String key) {
        super.clearCacheEntry(key);
        SessionDataStore.getInstance().clearSessionData(key, OAUTH_APP_INFO_CACHE_NAME);
        if(enableRequestScopeCache){
            SessionDataStore.getInstance().clearSessionData(key,OAUTH_APP_INFO_CACHE_NAME);
        }
    }
}
