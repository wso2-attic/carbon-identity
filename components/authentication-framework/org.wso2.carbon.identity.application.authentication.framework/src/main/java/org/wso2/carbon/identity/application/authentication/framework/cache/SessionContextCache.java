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

import org.wso2.carbon.identity.application.authentication.framework.config.model.SequenceConfig;
import org.wso2.carbon.identity.application.authentication.framework.model.SessionInfo;
import
        org.wso2.carbon.identity.application.authentication.framework.store.SessionDataStore;
import org.wso2.carbon.identity.application.common.cache.BaseCache;
import org.wso2.carbon.identity.application.common.cache.CacheEntry;
import org.wso2.carbon.identity.application.common.cache.CacheKey;
import org.wso2.carbon.identity.core.util.IdentityUtil;

import java.sql.Timestamp;
import java.util.*;

public class SessionContextCache extends BaseCache<CacheKey, CacheEntry> {

    private static final String SESSION_CONTEXT_CACHE_NAME = "AppAuthFrameworkSessionContextCache";
    private static volatile SessionContextCache instance;
    private boolean useCache = true;

    private SessionContextCache(String cacheName) {
        super(cacheName);
    }

    private SessionContextCache(String cacheName, int timeout) {
        super(cacheName, timeout);
        useCache = !Boolean.parseBoolean(IdentityUtil.getProperty(
                "JDBCPersistenceManager.SessionDataPersist.Only"));
        if (IdentityUtil.getProperty("SessionContextCache.Enable") != null) {
            useCache = Boolean.parseBoolean(
                    IdentityUtil.getProperty("SessionContextCache.Enable"));
        }
    }

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
                        capacity = Integer.parseInt(
                                IdentityUtil.getProperty("SessionContextCache.Capacity"));
                    } catch (Exception e) {
                        //ignore
                    }
                    instance = new SessionContextCache(SESSION_CONTEXT_CACHE_NAME, timeout, capacity);
                }
            }
        }
        return instance;
    }

    @Override
    public void addToCache(CacheKey key, CacheEntry entry) {
        if (useCache) {
            super.addToCache(key, entry);
        }
        String keyValue = ((SessionContextCacheKey) key).getContextId();
        SessionDataStore.getInstance().storeSessionData(keyValue, SESSION_CONTEXT_CACHE_NAME, entry);
    }

    @Override
    public CacheEntry getValueFromCache(CacheKey key) {
        CacheEntry cacheEntry = null;
        if (useCache) {
            cacheEntry = super.getValueFromCache(key);
        }
        if (cacheEntry == null) {
            String keyValue = ((SessionContextCacheKey) key).getContextId();
            SessionContextCacheEntry sessionEntry = (SessionContextCacheEntry) SessionDataStore.getInstance().
                    getSessionData(keyValue, SESSION_CONTEXT_CACHE_NAME);
            if (sessionEntry != null && sessionEntry.getContext().isRememberMe()) {
                cacheEntry = sessionEntry;
            }
        }
        return cacheEntry;

    }

    @Override
    public void clearCacheEntry(CacheKey key) {
        if (useCache) {
            super.clearCacheEntry(key);
        }
        String keyValue = ((SessionContextCacheKey) key).getContextId();
        SessionDataStore.getInstance().clearSessionData(keyValue, SESSION_CONTEXT_CACHE_NAME);
    }

    /**
     * Get Session details from database and cache.
     * @return sessionInfo list.
     */
    public ArrayList<SessionInfo> getSessionDetailsFromDbAndCache() {
        Object sessionDetails = null;
        String[] userDetailsArray;
        Timestamp sessionCreatedTime = null;
        ArrayList<SessionInfo> sessionInfoList = new ArrayList<SessionInfo>();
        Set<String> cacheIdSet = new HashSet<String>();
        if (Boolean.parseBoolean(IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.Enable"))) {
            List<String> dbSessionList = SessionDataStore.getInstance().getSessionIdsFromDb(SESSION_CONTEXT_CACHE_NAME);
            if (dbSessionList != null) {
                for(String cacheId : dbSessionList){
                    sessionDetails = SessionDataStore.getInstance().getSessionData(cacheId,SESSION_CONTEXT_CACHE_NAME);
                    sessionCreatedTime = SessionDataStore.getInstance().getSessionCreatedTime(cacheId,SESSION_CONTEXT_CACHE_NAME);
                    if(sessionDetails instanceof SessionContextCacheEntry){
                        Set<Map.Entry<String, SequenceConfig>> sessions = ((SessionContextCacheEntry) sessionDetails).getContext().getAuthenticatedSequences().entrySet();
                        for (Map.Entry<String, SequenceConfig> session : sessions) {
                            String applicationId = session.getValue().getApplicationId();
                            String userName = session.getValue().getAuthenticatedUser().getUserName();

                            SessionInfo sessionInfo = new SessionInfo();
                            sessionInfo.setUserName(userName);
                            sessionInfo.setApplicationId(applicationId);
                            sessionInfo.setLoggedInTimeStamp(sessionCreatedTime);

                            if(!cacheIdSet.contains(cacheId)) {
                                cacheIdSet.add(cacheId);
                                sessionInfoList.add(sessionInfo);
                            }

                        }

                    }
                }
            }
        } else {
            List<String> cacheSessionList = SessionContextCache.getInstance(0).getCacheKeyList();
            //get details from cache
            if (cacheSessionList != null) {
                for(String cacheId : cacheSessionList){
                    sessionDetails = SessionContextCache.getInstance(0).getValueFromCache(cacheId);
                    if(sessionDetails instanceof SessionContextCacheEntry) {
                        Set<Map.Entry<String, SequenceConfig>> sessions = ((SessionContextCacheEntry) sessionDetails).getContext().getAuthenticatedSequences().entrySet();
                        for (Map.Entry<String, SequenceConfig> session : sessions) {
                            String applicationId = session.getValue().getApplicationId();
                            String userName = session.getValue().getAuthenticatedUser().getUserName();

                            SessionInfo sessionInfo = new SessionInfo();
                            sessionInfo.setUserName(userName);
                            if(!cacheIdSet.contains(cacheId)) {
                                cacheIdSet.add(cacheId);
                                sessionInfoList.add(sessionInfo);
                            }

                        }
                    }
                }
            }
        }

        return  sessionInfoList;
    }

    /**
     * Remove Session details from db and cache.
     * @param userName String.
     * @return boolean value.
     */
    public void removeSessionDetailsFromDbAndCache(String userName) {
        if (Boolean.parseBoolean(IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.Enable"))) {
            List<String> sessionIdList = SessionDataStore.getInstance().getSessionIdsForUserName(userName);
            for(String cacheId: sessionIdList) {
                SessionDataStore.getInstance().removeSessionData(cacheId, SESSION_CONTEXT_CACHE_NAME);
            }
        } else {
            List<String> cacheSessionList = SessionContextCache.getInstance(0).getCacheKeyList();
            Object sessionDetails = null;
            //get details from cache
            if (cacheSessionList != null) {
                for(String cacheId : cacheSessionList){
                    sessionDetails = SessionContextCache.getInstance(0).getValueFromCache(cacheId);
                    if(sessionDetails instanceof SessionContextCacheEntry) {
                        Set<Map.Entry<String, SequenceConfig>> sessions = ((SessionContextCacheEntry) sessionDetails).getContext().getAuthenticatedSequences().entrySet();
                        for (Map.Entry<String, SequenceConfig> session : sessions) {
                            if(userName.equals(session.getValue().getAuthenticatedUser().getUserName())) {
                                clearCacheEntry(cacheId);
                            }
                        }
                    }
                }
            }
        }

    }

}
