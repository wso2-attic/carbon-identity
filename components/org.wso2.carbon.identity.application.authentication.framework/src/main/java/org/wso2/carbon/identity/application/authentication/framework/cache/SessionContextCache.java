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

import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.context.RegistryType;
import org.wso2.carbon.identity.application.authentication.framework.config.model.ApplicationConfig;
import org.wso2.carbon.identity.application.authentication.framework.config.model.SequenceConfig;
import org.wso2.carbon.identity.application.authentication.framework.model.SessionInfo;
import org.wso2.carbon.identity.application.authentication.framework.model.UserSessionInfo;
import org.wso2.carbon.identity.application.authentication.framework.store.SessionDataStore;
import org.wso2.carbon.identity.application.common.cache.BaseCache;
import org.wso2.carbon.identity.application.common.cache.CacheEntry;
import org.wso2.carbon.identity.application.common.cache.CacheKey;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.common.model.User;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.registry.api.Registry;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.tenant.mgt.core.internal.TenantMgtCoreServiceComponent;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreConfigConstants;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.tenant.TenantManager;
import org.wso2.carbon.user.core.util.UserCoreUtil;
import org.wso2.carbon.user.mgt.UserMgtConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

public class SessionContextCache extends BaseCache<CacheKey, CacheEntry> {

    private static final String SESSION_CONTEXT_CACHE_NAME = "AppAuthFrameworkSessionContextCache";
    public static final String SESSION_MANAGEMENT_PERMISSION ="/manage/session_management";
    public static final String VIEW_SESSION_PERMISSION = "view_sessions";
    public static final String KILL_SESSION_PERMISSION = "kill_sessions";
    private static volatile SessionContextCache instance;
    private boolean useCache = true;

    private SessionContextCache(String cacheName) {
        super(cacheName);
    }

    private SessionContextCache(String cacheName, int timeout) {
        super(cacheName, timeout);
        useCache = !Boolean.parseBoolean(IdentityUtil.getProperty(
                "JDBCPersistenceManager.SessionDataPersist.Only"));
        if(IdentityUtil.getProperty("SessionContextCache.Enable") != null){
            useCache = Boolean.parseBoolean(
                    IdentityUtil.getProperty("SessionContextCache.Enable"));
        }
    }

    private SessionContextCache(String cacheName, int timeout, int capacity) {
        super(cacheName, timeout, capacity);
        useCache = !Boolean.parseBoolean(IdentityUtil.getProperty(
                "JDBCPersistenceManager.SessionDataPersist.Only"));
        if(IdentityUtil.getProperty("SessionContextCache.Enable") != null){
            useCache = Boolean.parseBoolean(IdentityUtil.getProperty("SessionContextCache.Enable"));
        }
    }

    public static SessionContextCache getInstance(int timeout) {
    	if (instance == null) {
    		synchronized (SessionContextCache.class) {
				if (instance == null) {
                    int capacity = 2000;
                    try{
                        capacity = Integer.parseInt(
                                IdentityUtil.getProperty("SessionContextCache.Capacity"));
                    } catch (Exception e){
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
        if(useCache){
            super.addToCache(key, entry);
        }
        String keyValue = ((SessionContextCacheKey) key).getContextId();
        SessionDataStore.getInstance().storeSessionData(keyValue, SESSION_CONTEXT_CACHE_NAME, entry);
    }

    @Override
    public CacheEntry getValueFromCache(CacheKey key) {
        CacheEntry cacheEntry = null;
        if(useCache){
            cacheEntry = super.getValueFromCache(key);
        }
        if(cacheEntry == null){
            String keyValue = ((SessionContextCacheKey) key).getContextId();
            SessionContextCacheEntry sessionEntry = (SessionContextCacheEntry) SessionDataStore.getInstance().
                    getSessionData(keyValue, SESSION_CONTEXT_CACHE_NAME);
            if(sessionEntry!=null && sessionEntry.getContext().isRememberMe()){
                cacheEntry = sessionEntry;
            }
        }
        return cacheEntry;

    }

    @Override
    public void clearCacheEntry(CacheKey key) {
        if(useCache){
            super.clearCacheEntry(key);
        }
        String keyValue = ((SessionContextCacheKey) key).getContextId();
        SessionDataStore.getInstance().clearSessionData(keyValue, SESSION_CONTEXT_CACHE_NAME);
    }


    /**
     * Get logged in users session details.
     *
     * @return sessionInfo list.
     */
    public ArrayList<UserSessionInfo> getSessionDetails() throws RegistryException {
        HashMap<String, UserSessionInfo> userSessionInfoMap;
        ArrayList<UserSessionInfo> userSessionInfoList = null;
        String viewPermissionResourcePath = getPermissionPath(VIEW_SESSION_PERMISSION);
        String killPermissionResourcePath = getPermissionPath(KILL_SESSION_PERMISSION);
        String loggedInUserName = CarbonContext.getThreadLocalCarbonContext().getUsername();
        loggedInUserName = MultitenantUtils.getTenantAwareUsername(loggedInUserName);
        String userStoreDomainName = UserCoreUtil.getDomainFromThreadLocal();
        UserRealm realm = (UserRealm) CarbonContext.getThreadLocalCarbonContext().getUserRealm();

        boolean hasViewPermission;
        try {
            if (realm.getAuthorizationManager().isUserAuthorized(
                    loggedInUserName, viewPermissionResourcePath, UserMgtConstants.EXECUTE_ACTION)) {
                hasViewPermission = true;

            } else {
                hasViewPermission = false;
            }
            if(userStoreDomainName == null) {
                userStoreDomainName = UserStoreConfigConstants.PRIMARY;
            }
            boolean hasKillPermission = realm.getAuthorizationManager().isUserAuthorized(
                    loggedInUserName, killPermissionResourcePath, UserMgtConstants.EXECUTE_ACTION);
            String tenantDomainName = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
            if (Boolean.parseBoolean(IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.Enable"))) {
                userSessionInfoMap = getSessionDetailsFromDb(loggedInUserName, hasViewPermission, hasKillPermission,
                        tenantDomainName, userStoreDomainName);
            } else {
                userSessionInfoMap = getSessionDetailsFromCache(loggedInUserName, hasViewPermission, hasKillPermission,
                        tenantDomainName, userStoreDomainName);
            }
            if (userSessionInfoMap.size() > 0) {
                userSessionInfoList = new ArrayList<UserSessionInfo>(userSessionInfoMap.values());
            }
        } catch (UserStoreException e) {
            String msg = "Error occurred while getting session details.";
            throw new RegistryException(msg, e);
        }
        return userSessionInfoList;
    }

    /**
     * Get Session details when session data persist enable.
     *
     * @return HashMap of sessionInformation.
     */
    private HashMap<String, UserSessionInfo> getSessionDetailsFromDb(String loggedInUser, boolean hasViewPermission,
                                                                     boolean hasKillPermission, String currentUserTenantDomain, String currentUserStoreDomain) {
        Object sessionDetails;
        HashMap<String, UserSessionInfo> userSessionInfoMap = new HashMap<String, UserSessionInfo>();
        List<String> dbSessionList;
        if (hasViewPermission) {
            dbSessionList = SessionDataStore.getInstance().getLoggedInSessionIdsFromDb(SESSION_CONTEXT_CACHE_NAME,
                    currentUserTenantDomain);
        } else {
            dbSessionList = SessionDataStore.getInstance().getSessionIdsFromDbByUserName(SESSION_CONTEXT_CACHE_NAME,
                    currentUserTenantDomain, loggedInUser, currentUserStoreDomain);
        }
        if (dbSessionList != null) {
            for (String cacheId : dbSessionList) {
                sessionDetails = SessionDataStore.getInstance().getSessionData(cacheId, SESSION_CONTEXT_CACHE_NAME);
                Timestamp sessionCreatedTime = SessionDataStore.getInstance().getSessionCreatedTime(cacheId,
                        SESSION_CONTEXT_CACHE_NAME);
                if (sessionDetails instanceof SessionContextCacheEntry) {
                    Set<Map.Entry<String, SequenceConfig>> sessions = ((SessionContextCacheEntry) sessionDetails)
                            .getContext().getAuthenticatedSequences().entrySet();
                    for (Map.Entry<String, SequenceConfig> session : sessions) {
                        readSessionAndPopulateMap(session, userSessionInfoMap, loggedInUser, currentUserTenantDomain,
                                currentUserStoreDomain, hasViewPermission, hasKillPermission, sessionCreatedTime);
                    }
                }
            }
        }
        return userSessionInfoMap;
    }

    /**
     * Get Session details when session data persist disable
     *
     * @return HashMap of sessionInformation.
     */
    private HashMap<String, UserSessionInfo> getSessionDetailsFromCache(String loggedInUserName, boolean hasPermission,
                                                                        boolean hasKillPermission, String currentUserTenantDomain, String loggedInUserStoreDomain) {
        Timestamp sessionCreatedTime;
        HashMap<String, UserSessionInfo> userSessionInfoMap = new HashMap<String, UserSessionInfo>();
        List<Object> cacheSessionIdList = SessionContextCache.getInstance(0).getCacheKeyList();
        if (cacheSessionIdList != null) {
            ///    String loggedInUser = loggedInUserStoreDomain.concat("/").concat(loggedInUserName).concat("/")
            //         .concat(currentUserTenantDomain);
            for (Object cacheObject : cacheSessionIdList) {
                CacheKey cacheId = ((SessionContextCacheKey) cacheObject);
                if (cacheId != null) {
                    CacheEntry cacheEntry = getValueFromCache(cacheId);
                    if (cacheEntry != null) {
                        if (cacheEntry instanceof SessionContextCacheEntry) {
                            sessionCreatedTime = ((SessionContextCacheEntry) cacheEntry).getLoggedInTime();
                            Set<Map.Entry<String, SequenceConfig>> sessions = ((SessionContextCacheEntry) cacheEntry)
                                    .getContext().getAuthenticatedSequences().entrySet();
                            for (Map.Entry<String, SequenceConfig> session : sessions) {
                                readSessionAndPopulateMap(session, userSessionInfoMap, loggedInUserName,
                                        currentUserTenantDomain, loggedInUserStoreDomain, hasPermission, hasKillPermission, sessionCreatedTime);
                            }
                        }
                    }
                }
            }
        }
        return userSessionInfoMap;
    }

    private void readSessionAndPopulateMap(Map.Entry<String, SequenceConfig> session,
                                           HashMap<String, UserSessionInfo> userSessionInfoMap, String loggedInUser,
                                           String currentUserTenantDomain, String loggedInUserStoreDomain,
                                           boolean hasPermission, boolean hasKillPermission, Timestamp sessionCreatedTime) {
        SessionInfo sessionInfo;
        UserSessionInfo userSessionInfo;
        ArrayList<SessionInfo> sessionInfoList;
        if (session != null && session.getValue() != null) {
            SequenceConfig sessionValue = session.getValue();

            String applicationId = sessionValue.getApplicationId();
            String userFullName = sessionValue.getAuthenticatedUser();
            String userName = null;
            String userStoreDomain = null;
            if((userFullName != null) && (userFullName.contains("/"))) {
                String[] nameParts = userFullName.split("/");
                userName = nameParts[0];
                userStoreDomain = nameParts[1];
            } else {
                userName = userFullName;
                userStoreDomain = UserStoreConfigConstants.PRIMARY;;
            }
            String tenantDomainName = sessionValue.getAuthenticatedUserTenantDomain();
            String loggedInUserFullName = loggedInUserStoreDomain.concat("/").concat(loggedInUser).concat("/")
                    .concat(currentUserTenantDomain);
            String authenticatedUserName = userStoreDomain.concat("/").concat(userName).concat("/").concat(tenantDomainName);
            String applicationTenantDomain;

            ApplicationConfig applicationConfigValue = sessionValue.getApplicationConfig();
            if (applicationConfigValue != null) {
                ServiceProvider serviceProvider = applicationConfigValue.getServiceProvider();
                if (serviceProvider != null) {
                    User owner = serviceProvider.getOwner();
                    if (owner != null) {
                        applicationTenantDomain = String.valueOf(owner.getTenantId());
                    } else {
                        applicationTenantDomain = MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
                    }
                    if (hasPermission || loggedInUserFullName.equals(authenticatedUserName)) {
                        sessionInfo = new SessionInfo();
                        sessionInfo.setApplicationId(applicationId);
                        sessionInfo.setLoggedInTimeStamp(timeStampIntoString(sessionCreatedTime));
                        sessionInfo.setLoggedInDuration(calculateTimeDuration(sessionCreatedTime));
                        sessionInfo.setApplicationTenantDomain(applicationTenantDomain.toLowerCase());
                        if (userSessionInfoMap.containsKey(authenticatedUserName)) {
                            userSessionInfo = userSessionInfoMap.get(authenticatedUserName);
                            sessionInfoList = userSessionInfo.getSessionsList();
                            sessionInfoList.add(sessionInfo);
                        } else {
                            userSessionInfo = new UserSessionInfo();
                            userSessionInfo.setUserName(userName);
                            userSessionInfo.setUserStoreDomain(userStoreDomain.toLowerCase());
                            userSessionInfo.setTenantDomain(tenantDomainName);
                            userSessionInfo.setHasKillPermission(hasKillPermission);
                            sessionInfoList = new ArrayList<SessionInfo>();
                            sessionInfoList.add(sessionInfo);
                            userSessionInfo.setSessionsList(sessionInfoList);
                            userSessionInfoMap.put(authenticatedUserName, userSessionInfo);
                        }
                    }
                }
            }

        }
    }

    /**
     * Remove Session details from db and cache.
     *
     * @param userName String.
     * @return boolean value.
     */
    public void removeSessionDetailsFromDbAndCache(String userName, String userStoreDomain, String tenantDomainName)
            throws RegistryException {
        String killPermissionResourcePath = getPermissionPath(KILL_SESSION_PERMISSION);
        String loggedInUser = CarbonContext.getThreadLocalCarbonContext().getUsername();
        loggedInUser = MultitenantUtils.getTenantAwareUsername(loggedInUser);
        String userStoreDomainName = UserCoreUtil.getDomainFromThreadLocal();
        UserRealm realm = (UserRealm) CarbonContext.getThreadLocalCarbonContext().getUserRealm();
        try {
            boolean hasKillPermission = realm.getAuthorizationManager().isUserAuthorized(
                    loggedInUser, killPermissionResourcePath, UserMgtConstants.EXECUTE_ACTION);
            if (Boolean.parseBoolean(IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.Enable"))) {
                if (hasKillPermission) {
                    List<String> sessionIdList = SessionDataStore.getInstance().getSessionIdsForUserName(
                            userName, tenantDomainName, userStoreDomain);
                    for (String cacheId : sessionIdList) {
                        SessionDataStore.getInstance().removeSessionData(cacheId, SESSION_CONTEXT_CACHE_NAME);
                        clearCacheEntry(cacheId);
                    }
                }
            } else {
                List<Object> cacheSessionList = SessionContextCache.getInstance(0).getCacheKeyList();
                Object sessionDetails;
                if (hasKillPermission) {
                    //get details from cache
                    if (cacheSessionList != null) {
                        for (Object cacheObject : cacheSessionList) {
                            String cacheId = ((SessionContextCacheKey) cacheObject).getContextId();
                            sessionDetails = SessionContextCache.getInstance(0).getValueFromCache(cacheId);
                            if (sessionDetails instanceof SessionContextCacheEntry) {
                                Set<Map.Entry<String, SequenceConfig>> sessions = ((SessionContextCacheEntry) sessionDetails)
                                        .getContext().getAuthenticatedSequences().entrySet();
                                for (Map.Entry<String, SequenceConfig> session : sessions) {
                                    if (userName.equals(session.getValue().getAuthenticatedUser())) {
                                        clearCacheEntry(cacheId);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (UserStoreException e) {
            String msg = "Error occurred while getting session details.";
            throw new RegistryException(msg, e);
        }
    }

    private long calculateTimeDuration(Timestamp sessionCreatedTime) {
        long duration;
        Date date = new Date();
        Timestamp currentTimeStamp = new Timestamp(date.getTime());
        long lCreatedTime = sessionCreatedTime.getTime();
        long lCurrentTime = currentTimeStamp.getTime();
        duration = (lCurrentTime - lCreatedTime) / 1000;
        return duration;
    }

    private String timeStampIntoString(Timestamp timeStamp) {
        String timeString = "";
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        StringBuilder sb = new StringBuilder(fmt.format(timeStamp));
        timeString = sb.toString();
        return timeString;
    }

    private static String getPermissionPath(String permission) {
        return CarbonConstants.UI_ADMIN_PERMISSION_COLLECTION + SESSION_MANAGEMENT_PERMISSION
                + RegistryConstants.PATH_SEPARATOR + permission;
    }
}
