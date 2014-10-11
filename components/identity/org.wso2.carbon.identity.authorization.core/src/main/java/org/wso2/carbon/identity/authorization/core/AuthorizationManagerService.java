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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.authorization.core.dao.ModuleDAO;
import org.wso2.carbon.identity.authorization.core.dao.PermissionAssignmentDAO;
import org.wso2.carbon.identity.authorization.core.dao.PermissionDAO;
import org.wso2.carbon.identity.authorization.core.dto.Permission;
import org.wso2.carbon.identity.authorization.core.dto.PermissionAssignment;
import org.wso2.carbon.identity.authorization.core.dto.PermissionGroup;
import org.wso2.carbon.identity.authorization.core.dto.PermissionModule;
import org.wso2.carbon.identity.authorization.core.dto.PermissionRequest;
import org.wso2.carbon.identity.authorization.core.dto.RolePermission;
import org.wso2.carbon.identity.authorization.core.dto.UserPermission;
import org.wso2.carbon.identity.authorization.core.jdbc.dao.JDBCConstantsDAO;
import org.wso2.carbon.identity.authorization.core.permission.PermissionMapper;
import org.wso2.carbon.identity.authorization.core.permission.PermissionProcessor;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.utils.xml.StringUtils;

/**
 * @author venura
 * @date May 10, 2013
 */
public class AuthorizationManagerService implements AuthorizationManager {

	private static Log log = LogFactory.getLog(AuthorizationManagerService.class);

	private PermissionProcessor permissionProcessor;
	private PermissionMapper permissionMapper = PermissionMapper.getInstance();

	public AuthorizationManagerService() {
		super();
		permissionProcessor = PermissionProcessor.getInstance();
	}

	@Override
	public void clearPermissions(PermissionRequest request) throws IdentityAuthorizationException {
		if (StringUtils.isEmpty(request.getSubject()) && StringUtils.isEmpty(request.getModule())) {
			log.error("Cannot clear all the permissions for the current teant id. Module or subject parameters should be provided");
			throw new IdentityAuthorizationException(
			                                         "Cannot clear all the permissions for the current teant id. Module or subject parameters should be provided");
		}
		try {
			permissionProcessor.clearPermissions(request);
		} catch (UserStoreException e) {
			log.error(e.getMessage());
			throw new IdentityAuthorizationException(e.getMessage(), e);
		}
	}

	@Override
	public void createPermissions(List<PermissionGroup> permissions)
	                                                                throws IdentityAuthorizationException {
		if (permissions == null || permissions.isEmpty()) {
			log.error("Permissions are not provided");
			throw new IdentityAuthorizationException("Permissions are not provided");
		}
		for (PermissionGroup permission : permissions) {
			try {
				ModuleDAO module =
				                   permissionProcessor.loadPermissionDependency(permission.getModuleName());
				if (module != null) {
					if (!permissionProcessor.validatePermission(module.getModuleName(),
					                                                  permission)) {
						log.error("Required actions are not supported by the module");
						throw new IdentityAuthorizationException(
						                                         "Required actions are not supported by the module");
					} else {
						permission.setModuleId(module.getModuleId());
					}

				}
				PermissionDAO permDAO = permissionMapper.mapPermission(permission);
				if (log.isDebugEnabled()) {
					log.debug("Adding new permission to the database  " + permission.toString());
				}
				int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();

				permDAO.setTenantId(tenantId);

				int existingPermissionId = permissionProcessor.isExistingPermission(permDAO);
				if (existingPermissionId > 0) {
					// Permissions for the same resource, module, tenant and
					// action is already exists within the DB
					permDAO.setPermissionId(existingPermissionId);
					permDAO.setStatus(JDBCConstantsDAO.UPDATE);
				} else {
					permDAO.setStatus(JDBCConstantsDAO.INSERT);
				}
				permissionProcessor.save(permDAO);

			} catch (UserStoreException e) {
				throw new IdentityAuthorizationException(e.getMessage(), e);
			}
		}

	}

	@Override
	public void deletePermission(List<PermissionGroup> permissions)
	                                                               throws IdentityAuthorizationException {
		if (permissions == null || permissions.isEmpty()) {
			log.error("Permissions are not provided for delete operation");
			throw new IdentityAuthorizationException(
			                                         "Permissions are not provided for delete operation");
		}

		for (PermissionGroup permission : permissions) {
			ModuleDAO module = null;
			try {
				module = permissionProcessor.loadModule(permission.getModuleName());
			} catch (UserStoreException e) {
				log.error(e.getMessage());
				throw new IdentityAuthorizationException(e.getMessage());
			}
			permission.setModuleId(module.getModuleId());
			PermissionDAO dao = permissionMapper.mapPermission(permission);
			dao.setTenantId(CarbonContext.getThreadLocalCarbonContext().getTenantId());
			dao.setStatus(JDBCConstantsDAO.DELETE);
			try {
				permissionProcessor.save(dao);
			} catch (UserStoreException e) {
				throw new IdentityAuthorizationException(e.getMessage(), e);
			}

		}
	}

	@Override
	public void addUserPermissions(List<UserPermission> permissions)
	                                                                throws IdentityAuthorizationException {
		if (permissions == null || permissions.isEmpty()) {
			log.error("Permissions are not provided for insert operation");
			throw new IdentityAuthorizationException(
			                                         "Permissions are not provided for insert operation");
		}
		updatePermissions(permissions, JDBCConstantsDAO.INSERT);
	}

	@Override
	public void addRolePermissions(List<RolePermission> permissions)
	                                                                throws IdentityAuthorizationException {
		if (permissions == null || permissions.isEmpty()) {
			log.error("Role Permissions are not provided for insert operation");
			throw new IdentityAuthorizationException(
			                                         "Role Permissions are not provided for insert operation");
		}
		updatePermissions(permissions, JDBCConstantsDAO.INSERT);
	}

	@Override
	public void deleteUserPermissions(List<UserPermission> permissions)
	                                                                   throws IdentityAuthorizationException {
		if (permissions == null || permissions.isEmpty()) {
			log.error("Permissions are not provided for delete operation");
			throw new IdentityAuthorizationException(
			                                         "Permissions are not provided for delete operation");
		}
		updatePermissions(permissions, JDBCConstantsDAO.DELETE);
	}

	@Override
	public void deleteRolePermissions(List<RolePermission> permissions)
	                                                                   throws IdentityAuthorizationException {
		if (permissions == null || permissions.isEmpty()) {
			log.error("Role Permissions are not provided for delete operation");
			throw new IdentityAuthorizationException(
			                                         "Role Permissions are not provided for delete operation");
		}
		updatePermissions(permissions, JDBCConstantsDAO.DELETE);
	}

	private void updatePermissions(final List<? extends PermissionAssignment> permAssignments,
	                               final byte operation) throws IdentityAuthorizationException {
		for (PermissionAssignment assignment : permAssignments) {
			PermissionAssignmentDAO dao = permissionMapper.mapPermission(assignment);
			dao.setStatus(operation);
			try {
				permissionProcessor.save(dao);
			} catch (UserStoreException e) {
				throw new IdentityAuthorizationException(e.getMessage(), e);
			}

		}
	}

	@Override
	public PermissionModule getPermissionList(PermissionRequest request)
	                                                                    throws IdentityAuthorizationException {

		if (request.getModule() == null || request.getModule().trim().length() == 0) {
			log.error("Module not specified in order to check permissions");
			throw new IdentityAuthorizationException(
			                                         "Module was not specified in order to check permissions");
		}

		try {
			return permissionProcessor.loadModulePermissions(request.getSubject(),
			                                                 request.isUserPermissions(),
			                                                 request.getModule());
		} catch (UserStoreException e) {
			log.error(e.getMessage());
			throw new IdentityAuthorizationException(e.getMessage(), e);
		}
	}

	@Override
	public List<Permission> getPermission(PermissionRequest request)
	                                                                throws IdentityAuthorizationException {
		try {
			return permissionProcessor.loadPermission(request.getSubject(),
			                                          request.isUserPermissions(),
			                                          request.getModule(), request.getAction(),
			                                          request.getResource());
		} catch (UserStoreException e) {
			log.error(e.getMessage());
			throw new IdentityAuthorizationException(e.getMessage(), e);
		}
	}

	@Override
	public void updateUserPermissions(List<UserPermission> permissions)
	                                                                   throws IdentityAuthorizationException {
		if (permissions == null || permissions.isEmpty()) {
			log.error("Permissions are not provided for update operation");
			throw new IdentityAuthorizationException(
			                                         "Permissions are not provided for update operation");
		}
		updatePermissions(permissions, JDBCConstantsDAO.UPDATE);
	}

	@Override
	public void updateRolePermissions(List<RolePermission> permissions)
	                                                                   throws IdentityAuthorizationException {
		if (permissions == null || permissions.isEmpty()) {
			log.error("Role Permissions are not provided for update operation");
			throw new IdentityAuthorizationException(
			                                         "Role Permissions are not provided for update operation");
		}
		updatePermissions(permissions, JDBCConstantsDAO.UPDATE);
	}

	@Override
	public int createModule(PermissionModule module) throws IdentityAuthorizationException {
		if (module == null || StringUtils.isEmpty(module.getModuleName())) {
			log.error("Module/ Application registration cannot be done if module name is not provided");
			throw new IdentityAuthorizationException(
			                                         "Module/ Application registration cannot be done if module name is not provided");
		}
		ModuleDAO dao = permissionMapper.mapModule(module);
		dao.setStatus(JDBCConstantsDAO.INSERT);
		int id = 0;
		try {
			id = permissionProcessor.save(dao);
		} catch (UserStoreException e) {
			log.error(e.getMessage());
			throw new IdentityAuthorizationException(e.getMessage(), e);
		}
		return id;
	}

	@Override
	public void updateAuthorizedActions(PermissionModule module)
	                                                            throws IdentityAuthorizationException {
		if (module == null ||
		    (module.getModuleId() <= 0 && StringUtils.isEmpty(module.getModuleName()))) {
			log.error("Adding actions to the module cannot be done since module cannot be identified ");
			throw new IdentityAuthorizationException(
			                                         "Adding actions to the module cannot be done since module cannot be identified ");
		}
		ModuleDAO dao = permissionMapper.mapModule(module);
		dao.setStatus(JDBCConstantsDAO.UPDATE);
		try {
			if (dao.getModuleId() <= 0) {
				ModuleDAO loadedDao = permissionProcessor.loadModule(dao.getModuleName());
				if (loadedDao == null) {
					log.error("Adding actions to the module cannot be done since module cannot be identified ");
					throw new IdentityAuthorizationException(
					                                         "Adding actions to the module cannot be done since module cannot be identified ");
				}
				dao.setModuleId(loadedDao.getModuleId());
			}
			permissionProcessor.save(dao);
		} catch (UserStoreException e) {
			log.error(e.getMessage());
			throw new IdentityAuthorizationException(e.getMessage(), e);
		}
	}

	@Override
	public PermissionModule getModule(String moduleName) throws IdentityAuthorizationException {
		try {
			ModuleDAO dao = permissionProcessor.loadModule(moduleName);

			if (dao == null) {
				log.error("Module cannot be identified ");
				throw new IdentityAuthorizationException("Module cannot be identified ");
			}
			return permissionMapper.mapModule(dao);

		} catch (UserStoreException e) {
			log.error(e.getMessage());
			throw new IdentityAuthorizationException(e.getMessage(), e);
		}
	}

	@Override
	public boolean isAuthorized(PermissionRequest request) throws IdentityAuthorizationException {
		List<Permission> permissions = getPermission(request);
		if (permissions == null || permissions.isEmpty()) {
			return false;
		}
		return permissions.get(0).isAuthorized();
	}

	@Override
	public List<PermissionModule> loadModules() throws IdentityAuthorizationException {

		List<PermissionModule> moduleList = new ArrayList<PermissionModule>();
		try {
			List<ModuleDAO> modules = permissionProcessor.loadModules();
			if (modules != null) {
				for (ModuleDAO module : modules) {
					moduleList.add(permissionMapper.mapModule(module));
				}
			}

		} catch (UserStoreException e) {
			log.error(e.getMessage());
			throw new IdentityAuthorizationException(e.getMessage(), e);
		}
		return moduleList;

	}

	@Override
	public void removeModule(PermissionRequest request) throws IdentityAuthorizationException {
		if (StringUtils.isEmpty(request.getModule()) && request.getModuleId() <= 0) {
			log.error("Cannot clear all the permissions for the current teant id. Module parameter should be provided");
			throw new IdentityAuthorizationException(
			                                         "Cannot clear all the permissions for the current teant id. Module or subject parameters should be provided");
		}
		request.setSubject(null);
		// Removes the role and user permissions.
		try {
			permissionProcessor.removeModule(request);
		} catch (UserStoreException e) {
			log.error(e.getMessage());
			throw new IdentityAuthorizationException(e.getMessage(), e);
		}

	}
}
