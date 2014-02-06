/*
 * Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.authorization.core;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.authorization.core.dto.Permission;
import org.wso2.carbon.identity.authorization.core.dto.PermissionModule;

/**
 * Cache wrapper for the custom permissions
 * 
 * @author venura
 * 
 */
public class CustomAuthorizationCache {

	private static Log log = LogFactory.getLog(CustomAuthorizationCache.class);
	
	private static final String CUSTOM_AUTH_CACHE_MANAGER = "CUSTOM_AUTH_CACHE_MANAGER";
	private static final String CUSTOM_AUTH_CACHE = "CUSTOM_AUTH_CACHE";
	
	private static String cacheIdentifier;
	
	private static CustomAuthorizationCache instance = new CustomAuthorizationCache();

	private CustomAuthorizationCache() {
		cacheIdentifier = null;
	}

	public static CustomAuthorizationCache getInstance() {
//		if (instance == null) {
//			instance = new CustomAuthorizationCache();
//		}
		return instance;
	}
	
	/**
	 * Getting existing cache if the cache available, else returns a newly created cache.
	 * This logic handles by javax.cache implementation
	 */
	private Cache<AuthorizationKey, AuthorizeCacheEntry> getCustomAuthorizationCache() {
		Cache<AuthorizationKey, AuthorizeCacheEntry> cache = null;
		CacheManager cacheManager = Caching.getCacheManagerFactory().getCacheManager(CUSTOM_AUTH_CACHE_MANAGER);
		cache = cacheManager.getCache(CUSTOM_AUTH_CACHE);
		return cache;
	}
	
	public void addCacheEntry(AuthorizationKey key, Boolean value) {
		Cache<AuthorizationKey, AuthorizeCacheEntry> cache = getCustomAuthorizationCache();
		if (cache != null) {
			key.setServerId(cacheIdentifier);
			if (cache.containsKey(key)) {
				removeCacheEnrty(key);
			}
			cache.put(key, new AuthorizeCacheEntry(value));
		}
	}
	
	public void addCacheEntry(boolean authorized, int tenantId, String userName, String resourceId,
	                          String action, int moduleId, String roleName) {
		Cache<AuthorizationKey, AuthorizeCacheEntry> cache = getCustomAuthorizationCache();
		if(cache != null) {
			AuthorizeCacheEntry entry = new AuthorizeCacheEntry(authorized);
			AuthorizationKey key = new AuthorizationKey(cacheIdentifier, tenantId, userName, resourceId, action, moduleId, roleName);
			
			// remove cache entry from the cache and from the whole cluster if
			// the environment is a cluster environment. This has to be done,
			// since replacing a cache entry means something have been changed
			// in the already existing entry. So it's safe to remove the cache
			// entry from the cache
			if (cache.containsKey(key)) {
				removeCacheEnrty(key);
			}
	
			cache.put(key, entry);
		}
	}

	public void addCacheEntry(PermissionModule permissions, int tenantId) {
		int moduleId = permissions.getModuleId();
		for (Permission permission : permissions.getPermissions()) {
			addCacheEntry(permission, tenantId, moduleId);
		}
	}

	public void addCacheEntry(Permission permission, int tenantId, int moduleId) {
		addCacheEntry(permission.isAuthorized(), tenantId,
		              !permission.isRolePermission() ? permission.getSubject() : "",
		              permission.getResourceId(), permission.getAction(), moduleId,
		              permission.isRolePermission() ? permission.getSubject() : "");

	}


	private void removeCacheEnrty(AuthorizationKey key) {
		Cache<AuthorizationKey, AuthorizeCacheEntry> cache = getCustomAuthorizationCache();
		if(cache != null) {
			if (cache.containsKey(key)) {
				cache.remove(key);
			}
		}
	}
	
	public void removeCacheEntry(AuthorizationKey key) {
		key.setServerId(cacheIdentifier);
		removeCacheEnrty(key);
	}

	public void removeCacheEntries(int moduleId, int tenantId) {
		Cache<AuthorizationKey, AuthorizeCacheEntry> cache = getCustomAuthorizationCache();
		if(cache != null) {
			for (Cache.Entry<AuthorizationKey, AuthorizeCacheEntry> entry : cache) {
				AuthorizationKey authorizationKey = entry.getKey();
				if (tenantId == (authorizationKey.getTenantId())
						&& authorizationKey.getModuleId() == moduleId) {
					removeCacheEnrty(authorizationKey);
				}
			}
		}
	}

	public void removeCacheEntries(int moduleId, String subjectName, int tenantId,
	                               boolean isRolePermissions) {
		Cache<AuthorizationKey, AuthorizeCacheEntry> cache = getCustomAuthorizationCache();
		if(cache != null) {
			for (Cache.Entry<AuthorizationKey, AuthorizeCacheEntry> entry : cache) {
				AuthorizationKey authorizationKey = entry.getKey();
				if (authorizationKey.getTenantId() == tenantId && authorizationKey.getModuleId() == moduleId &&
					    (isRolePermissions ? authorizationKey.getRoleName().equals(subjectName)
					                      : authorizationKey.getUserName().equals(subjectName))) {
					removeCacheEnrty(authorizationKey);
				}
			}
		}
	}

	public void clearCache() {
		Cache<AuthorizationKey, AuthorizeCacheEntry> cache = this.getCustomAuthorizationCache();
		// check for null
		if (cache != null) {
			cache.removeAll();
		}
	}

	/**
	 * Load the permission from cache.
	 * 
	 * @param subject
	 * @param isUserPerm
	 * @param moduleName
	 * @param resource
	 * @return <code>null</code> if permission not found for the provided
	 *         criteria or cache is not initialized or not enabled
	 */
	public Permission loadPermission(final String subject, final boolean isUserPerm,
	                                 final int moduleId, final String resource,
	                                 final String action, int tenantId) {

		Permission permission = null;
		
		Cache<AuthorizationKey, AuthorizeCacheEntry> cache = getCustomAuthorizationCache();
		if (cache != null) {
			AuthorizationKey key = new AuthorizationKey(cacheIdentifier,
					tenantId, isUserPerm ? subject : null, resource, action,
					moduleId, !isUserPerm ? subject : null);

			if (cache.containsKey(key)) {
				AuthorizeCacheEntry entry = (AuthorizeCacheEntry) cache
						.get(key);

				permission = new Permission();
				permission.setAction(key.getAction());
				permission.setResourceId(key.getResourceId());
				permission.setSubject(isUserPerm ? key.getUserName() : key
						.getRoleName());
				permission.setAuthorized(entry.isUserAuthorized());
			}
		}
		return permission;
	}





}
