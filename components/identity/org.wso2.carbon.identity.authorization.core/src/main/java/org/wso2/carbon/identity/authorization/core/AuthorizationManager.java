/*
 * Copyright 2004,2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.authorization.core;

import java.util.List;

import org.wso2.carbon.identity.authorization.core.dto.Permission;
import org.wso2.carbon.identity.authorization.core.dto.PermissionGroup;
import org.wso2.carbon.identity.authorization.core.dto.PermissionModule;
import org.wso2.carbon.identity.authorization.core.dto.PermissionRequest;
import org.wso2.carbon.identity.authorization.core.dto.RolePermission;
import org.wso2.carbon.identity.authorization.core.dto.UserPermission;

/**
 * 
 * @author venura
 * @date May 10, 2013
 */
public interface AuthorizationManager {

	/**
	 * Add permissions to a role or to a user.
	 * 
	 * @param permissions
	 *            List of permissions. If any of the parameter has a different
	 *            value then a new permission DTO should be created. For
	 *            example, if the action is 'edit' and one DTO should be created
	 *            for edit enabled permission and another one for edit disabled
	 *            permission. In this way object list might grow.
	 * @throws IdentityAuthorizationException
	 */
	public void createPermissions(final List<PermissionGroup> permissions)
	                                                                      throws IdentityAuthorizationException;

	/**
	 * Delete a given permission from a user
	 * 
	 * @param permissions
	 * @throws IdentityAuthorizationException
	 */
	public void deleteUserPermissions(final List<UserPermission> permissions)
	                                                                         throws IdentityAuthorizationException;

	/**
	 * Delete permissions for the given roles.
	 * 
	 * @param permissions
	 * @throws IdentityAuthorizationException
	 */
	public void deleteRolePermissions(final List<RolePermission> permissions)
	                                                                         throws IdentityAuthorizationException;

	/**
	 * Deletes permissions from users or roles
	 * 
	 * @param permissions
	 * @throws IdentityAuthorizationException
	 */
	public void deletePermission(final List<PermissionGroup> permissions)
	                                                                     throws IdentityAuthorizationException;

	/**
	 * Returns the set of permissions for requested user or role (subject) for a
	 * complete module. Only supported actions are as below. Get permissions for
	 * a subject for a complete module.
	 * 
	 * @param permissionRequest
	 * @return List of {@link PermissionGroup}
	 */
	public PermissionModule getPermissionList(final PermissionRequest request)
	                                                                          throws IdentityAuthorizationException;

	/**
	 * Get permission for a subject for a specific resource. For this resource
	 * should be provided. Multiple permissions can be provided if the action is
	 * not provided.
	 * 
	 * @param request
	 * @return
	 * @throws IdentityAuthorizationException
	 */
	public List<Permission> getPermission(final PermissionRequest request)
	                                                                      throws IdentityAuthorizationException;

	/**
	 * Permissions will be removed from the user/ role or the complete module.
	 * 
	 * Module parameter in the {@link PermissionRequest} has to be specified.
	 * Based on the subject parameter in {@link PermissionRequest} is provided
	 * or not, functionality will be changed. If subject is not specified,
	 * permissions will be removed form the mentioned module for the current
	 * tenant id. If subject parameter is specified, permissions which are
	 * allocated to the subject will be removed from the module for the current
	 * tenant id.
	 * 
	 * If the module is not specified, permissions which are marked with zero
	 * for module will be removed
	 * 
	 * @param clearRequest
	 */
	public void clearPermissions(final PermissionRequest clearRequest)
	                                                                  throws IdentityAuthorizationException;

	/**
	 * Add permissions to users. In the same method call, different permissions
	 * can be added to different users.
	 * 
	 * @param permissions
	 * @throws IdentityAuthorizationException
	 */
	public void addUserPermissions(List<UserPermission> permissions)
	                                                                throws IdentityAuthorizationException;

	/**
	 * Add permissions to roles. In the same method call, different permissions
	 * can be added to different roles.
	 * 
	 * @param permissions
	 * @throws IdentityAuthorizationException
	 */
	public void addRolePermissions(List<RolePermission> permissions)
	                                                                throws IdentityAuthorizationException;

	/**
	 * Update user permissions. In the same method call, different permissions
	 * can be added to different users.
	 * 
	 * @param permissions
	 * @throws IdentityAuthorizationException
	 */
	public void updateUserPermissions(List<UserPermission> permissions)
	                                                                   throws IdentityAuthorizationException;

	/**
	 * Update role permissions. In the same method call, different permissions
	 * can be added to different roles.
	 * 
	 * @param permissions
	 * @throws IdentityAuthorizationException
	 */
	public void updateRolePermissions(List<RolePermission> permissions)
	                                                                   throws IdentityAuthorizationException;

	/**
	 * Creates a module which will be used to categorized permissions.
	 * 
	 * @param module
	 * @return
	 * @throws IdentityAuthorizationException
	 */
	public int createModule(final PermissionModule module) throws IdentityAuthorizationException;

	/**
	 * Assign authorized actions to the module. These are the actions that are
	 * permitted by the module.
	 * 
	 * @param module
	 * @throws IdentityAuthorizationException
	 */
	public void updateAuthorizedActions(final PermissionModule module)
	                                                                  throws IdentityAuthorizationException;

	/**
	 * Returns the module for the module name
	 * 
	 * @param moduleName
	 * @return
	 * @throws IdentityAuthorizationException
	 */
	public PermissionModule getModule(final String moduleName)
	                                                          throws IdentityAuthorizationException;

	/**
	 * Checks whether the user or role is authorized to do the operation
	 * 
	 * @param request
	 * @return
	 * @throws IdentityAuthorizationException
	 */
	public boolean isAuthorized(final PermissionRequest request)
	                                                            throws IdentityAuthorizationException;

	/**
	 * Returns all the modules that are stored in the database.
	 * 
	 * @return
	 * @throws IdentityAuthorizationException
	 */
	public List<PermissionModule> loadModules() throws IdentityAuthorizationException;

	/**
	 * Removes the module permissions and removes the module references as well.
	 * 
	 * @param request
	 */
	public void removeModule(PermissionRequest request) throws IdentityAuthorizationException;
}
