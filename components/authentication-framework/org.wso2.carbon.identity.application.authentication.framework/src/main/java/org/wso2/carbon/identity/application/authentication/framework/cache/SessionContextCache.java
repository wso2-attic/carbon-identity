/*
 * Copyright (c) 2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * under the License.
 */

package org.wso2.carbon.identity.application.authentication.framework.cache;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.application.authentication.framework.store.SessionDataStore;
import org.wso2.carbon.identity.application.common.cache.BaseCache;
import org.wso2.carbon.identity.application.common.cache.CacheEntry;
import org.wso2.carbon.identity.application.common.cache.CacheKey;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.idp.mgt.util.IdPManagementUtil;

import java.sql.Timestamp;

public class SessionContextCache extends BaseCache<String, CacheEntry> {

    private static final String SESSION_CONTEXT_CACHE_NAME = "AppAuthFrameworkSessionContextCache";
    private static final Log log = LogFactory.getLog(SessionContextCache.class);

    private static volatile SessionContextCache instance;
    private boolean useCache = true;

    private SessionContextCache(String cacheName, int timeout, int capacity) {
        super(cacheName, timeout, capacity);
        useCache = !Boolean.parseBoolean(IdentityUtil.getProperty(
                "JDBCPersistenceManager.SessionDataPersist.Only"));
        if (IdentityUtil.getProperty("SessionContextCache.Enable") != null) {
            useCache = Boolean.parseBoolean(IdentityUtil.getProperty("SessionContextCache.Enable"));
        }
    }

    public static SessionContextCache getInstance(int timeout) {
        if (instance == null) {
            synchronized (SessionContextCache.class) {
                if (instance == null) {
                    int capacity = 2000;
                    try {
                        String capacityConfigValue = IdentityUtil.getProperty("SessionContextCache.Capacity");
                        if (StringUtils.isNotBlank(capacityConfigValue)) {
                            capacity = Integer.parseInt(capacityConfigValue);
                        }
                    } catch (NumberFormatException e) {
                        if (log.isDebugEnabled()) {
                            log.debug("Ignoring Exception.", e);
                        }
                        log.warn("Session context cache capacity size is not configured. Using default value.");
                    }
                    instance = new SessionContextCache(SESSION_CONTEXT_CACHE_NAME, timeout, capacity);
                }
            }
        }
        return instance;
    }

    public void addToCache(CacheKey key, CacheEntry entry) {
        if (useCache) {
            super.addToCache(((SessionContextCacheKey) key).getContextId(), entry);
        }
        String keyValue = ((SessionContextCacheKey) key).getContextId();
        SessionDataStore.getInstance().storeSessionData(keyValue, SESSION_CONTEXT_CACHE_NAME, entry);
    }

    public CacheEntry getValueFromCache(CacheKey key) {
        CacheEntry cacheEntry = null;
        if (useCache) {
            cacheEntry = super.getValueFromCache(((SessionContextCacheKey) key).getContextId());
        }
        if (cacheEntry == null) {
            String keyValue = ((SessionContextCacheKey) key).getContextId();
            SessionContextCacheEntry sessionEntry = (SessionContextCacheEntry) SessionDataStore.getInstance().
                    getSessionData(keyValue, SESSION_CONTEXT_CACHE_NAME);
            Timestamp currentTimestamp = new java.sql.Timestamp(new java.util.Date().getTime());
            if (sessionEntry != null && sessionEntry.getContext().isRememberMe() &&
                    (currentTimestamp.getTime() - SessionDataStore.getInstance().getTimeStamp(keyValue,
                            SESSION_CONTEXT_CACHE_NAME)
                            .getTime() <=
                            IdPManagementUtil.getRememberMeTimeout(CarbonContext.getThreadLocalCarbonContext()
                                    .getTenantDomain())*60*1000)) {
                cacheEntry = sessionEntry;
            }
        }
        return cacheEntry;

    }

    public void clearCacheEntry(CacheKey key) {
        if (useCache) {
            super.clearCacheEntry(((SessionContextCacheKey) key).getContextId());
        }
        String keyValue = ((SessionContextCacheKey) key).getContextId();
        SessionDataStore.getInstance().clearSessionData(keyValue, SESSION_CONTEXT_CACHE_NAME);
    }
}
