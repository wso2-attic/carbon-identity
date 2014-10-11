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

package org.wso2.carbon.identity.authorization.core.permission;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.authorization.core.AuthorizationKey;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.authorization.core.AuthorizationManagerService;
import org.wso2.carbon.identity.authorization.core.CustomAuthorizationCache;
import org.wso2.carbon.identity.authorization.core.dao.GenericDAO;
import org.wso2.carbon.identity.authorization.core.dao.ModuleDAO;
import org.wso2.carbon.identity.authorization.core.dao.ModuleResourceDAO;
import org.wso2.carbon.identity.authorization.core.dao.PermissionDAO;
import org.wso2.carbon.identity.authorization.core.dto.Permission;
import org.wso2.carbon.identity.authorization.core.dto.PermissionGroup;
import org.wso2.carbon.identity.authorization.core.dto.PermissionModule;
import org.wso2.carbon.identity.authorization.core.dto.PermissionRequest;
import org.wso2.carbon.identity.authorization.core.jdbc.SQLExecutor;
import org.wso2.carbon.identity.authorization.core.jdbc.dao.JDBCConstantsDAO;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.utils.xml.StringUtils;

/**
 * This class is used to process permission requests coming to and from the
 * {@link AuthorizationManagerService}
 * 
 * @author venura
 * @date May 14, 2013
 */
public class PermissionProcessor {

	private static Log log = LogFactory.getLog(PermissionProcessor.class);

	private SQLExecutor executor;
	private static Map<String, ModuleDAO> moduleCache;

	private static PermissionProcessor instance;
	private static CustomAuthorizationCache cache;

	private PermissionProcessor() {
		log.info("Loading modules relevent for permissions");
		moduleCache = new HashMap<String, ModuleDAO>();
		executor = SQLExecutor.getInstance();
		cache = CustomAuthorizationCache.getInstance();
	}

	public static PermissionProcessor getInstance() {
		if (instance == null) {
			instance = new PermissionProcessor();
		}
		return instance;
	}

	public int isExistingPermission(PermissionDAO permDAO) {
		List<PermissionDAO> permissionGrps = null;
		try {
			permissionGrps = (List<PermissionDAO>) executor.load(permDAO);
		} catch (SQLException e) {
			log.error("Error while loading the permission for the provided details");
		} catch (UserStoreException e) {
			log.error("Error while loading the permission for the provided details");
		}
		return (permissionGrps != null && !permissionGrps.isEmpty())
		                                                            ? permissionGrps.get(0)
		                                                                            .getIdentifier()
		                                                            : -1;
	}

	/**
	 * 
	 * @param dao
	 * @throws UserStoreException
	 */
	public int save(GenericDAO dao) throws UserStoreException {
		if (dao.getStatus() != JDBCConstantsDAO.INSERT) {
			Map<AuthorizationKey, Boolean> cacheMap = null;
			try {
				cacheMap = executor.createCacheEntry(dao);
			} catch (SQLException e) {
				log.error("Error while creating the cache entry for the DAO object ");
			}
			if (cacheMap != null) {
				cache.removeCacheEntry(cacheMap.keySet().iterator().next());
			}
		} else {
			Map<AuthorizationKey, Boolean> cacheMap = null;
			try {
				cacheMap = executor.createCacheEntry(dao);
			} catch (SQLException e) {
				log.error("Error while creating the cache entry for the DAO object ");
			}
			if (cacheMap != null) {
				Entry<AuthorizationKey, Boolean> entry = cacheMap.entrySet().iterator().next();

				cache.addCacheEntry(entry.getKey(), entry.getValue());
			}

		}
		return executor.save(dao);
	}

	/**
	 * Validates the permission against actions and resources permitted on a
	 * module.
	 * 
	 * @param moduleName
	 * @param permission
	 * @return
	 * @throws UserStoreException
	 */
	public boolean validatePermission(final String moduleName, final PermissionGroup permission)
	                                                                                            throws UserStoreException {
		boolean validAction = false;
		ModuleDAO module = null;
		if (moduleCache == null || moduleCache.isEmpty()) {
			log.debug("Modules applicable for permissions ha not been loaded");
			module = loadModule(moduleName);
		} else {
			module = moduleCache.get(moduleName);
		}
		for (String action : module.getAllowedActions()) {
			if (action.equals(permission.getAction())) {
				validAction = true;
				break;
			}
		}

		if (!validAction) {
			return validAction;
		}

		boolean validResource = false;

		if (module.getResources() == null || module.getResources().isEmpty()) {
			log.debug("Resources are not assigned");
		} else {
			for (ModuleResourceDAO resource : module.getResources()) {
				if (resource.getResource().equalsIgnoreCase(permission.getResource())) {
					validResource = true;
					break;
				}
			}
		}

		return validAction && validResource;
	}

	/**
	 * Loads dependencies for {@link PermissionGroup}
	 * 
	 * @param moduleName
	 * @return
	 * @throws UserStoreException
	 */
	public ModuleDAO loadPermissionDependency(final String moduleName) throws UserStoreException {
		return loadModule(moduleName);
	}

	public ModuleDAO loadModule(final String moduleName) throws UserStoreException {
		if (moduleCache.containsKey(moduleName)) {
			return moduleCache.get(moduleName);
		}
		ModuleDAO module = executor.populateModule(moduleName);
		if (module == null) {
			log.info("Module not found for the provided module name");
		} else {
			moduleCache.put(module.getModuleName(), module);
		}
		return module;

	}

	public List<ModuleDAO> loadModules() throws UserStoreException {
		List<ModuleDAO> modules = executor.loadModules();
		if (modules == null) {
			log.info("Modules not found ");
		} else {
			for (ModuleDAO dao : modules) {
				moduleCache.put(dao.getModuleName(), dao);
			}
		}
		return modules;
	}

	public PermissionModule loadModulePermissions(final String subject, final boolean isUserPerm,
	                                              final String moduleName)
	                                                                      throws UserStoreException {
		int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
		PermissionModule permissionModule =
		                                    executor.loadModulePermissions(subject, isUserPerm,
		                                                                   moduleName, tenantId);
		if (permissionModule == null) {
			String error = "Permissions are not defined for the provided user, role or module";
			log.info(error);
			throw new UserStoreException(error);
		}
		ModuleDAO dao = loadModule(moduleName);
		permissionModule.setModuleId(dao.getModuleId());
		permissionModule.setModuleName(moduleName);

		cache.addCacheEntry(permissionModule, tenantId);

		return permissionModule;
	}

	public List<Permission> loadPermission(final String subject, final boolean isUserPerm,
	                                       final String moduleName, final String resource,
	                                       final String action) throws UserStoreException {
		int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
		ModuleDAO module = loadModule(moduleName);
		if (module == null) {
			log.error("Module code incorrect");
			throw new UserStoreException("Module code incorrect");
		}
		List<Permission> permissionList = null;
		if (!StringUtils.isEmpty(action)) {
			Permission permission =
			                        cache.loadPermission(subject, isUserPerm, module.getModuleId(),
			                                             resource, action, tenantId);
			if (permission != null) {
				permissionList = new ArrayList<Permission>();
				permissionList.add(permission);
				return permissionList;
			}
		}
		permissionList =
		                 executor.loadPermission(subject, isUserPerm, moduleName, resource, action,
		                                         tenantId);
		if (permissionList != null && !permissionList.isEmpty()) {
			for (Permission permission : permissionList) {
				cache.addCacheEntry(permission, tenantId, module.getModuleId());
			}
		}

		return permissionList;
	}

	/**
	 * 
	 * @param request
	 * @throws UserStoreException
	 */
	public void clearPermissions(PermissionRequest request) throws UserStoreException {
		boolean isWholeModule = StringUtils.isEmpty(request.getSubject());
		int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
		if (isWholeModule) {
			// clears the permissions for the whole module
			executor.clearModulePermissions(request.getModuleId(), tenantId, null, true);
			cache.removeCacheEntries(request.getModuleId(), tenantId);
		} else {
			// clears permissions allocated for a given user or role
			executor.clearPermissions(request.getSubject(), request.isUserPermissions(),
			                          request.getModule(), tenantId);
			cache.removeCacheEntries(request.getModuleId(), request.getSubject(), tenantId,
			                         !request.isUserPermissions());
		}
	}

	/**
	 * Removes a module specified by a the {@link PermissionRequest}
	 * 
	 * @param request
	 * @throws UserStoreException
	 */
	public void removeModule(PermissionRequest request) throws UserStoreException {
		int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
		// removes the module

		if (request.getModuleId() <= 0) {
			ModuleDAO dao = loadModule(request.getModule());
			if (dao == null) {
				throw new UserStoreException("Mentioned module cannot be found");
			}
			request.setModuleId(dao.getModuleId());
		}
		executor.removeModule(request.getModuleId(), tenantId);
		cache.removeCacheEntries(request.getModuleId(), tenantId);
	}
}
