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

import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.authorization.core.dto.Permission;
import org.wso2.carbon.identity.authorization.core.dto.PermissionGroup;
import org.wso2.carbon.identity.authorization.core.dto.PermissionModule;
import org.wso2.carbon.identity.authorization.core.dto.PermissionRequest;
import org.wso2.carbon.identity.authorization.core.dto.RolePermission;
import org.wso2.carbon.identity.authorization.core.dto.UserPermission;

/**
 * 
 */
public class AuthorizationAdminService {

	private static Log log = LogFactory.getLog(AuthorizationAdminService.class);
	private AuthorizationManagerService proxy = new AuthorizationManagerService();

	public String getVersion() {
		return "1.0";
	}

	public void createPermissions(PermissionGroup[] permissions)
	                                                            throws IdentityAuthorizationException {
		proxy.createPermissions(Arrays.asList(permissions));

	}

	public void deleteUserPermissions(UserPermission[] permissions)
	                                                               throws IdentityAuthorizationException {
		proxy.deleteUserPermissions(Arrays.asList(permissions));
	}

	public void deleteRolePermissions(RolePermission[] permissions)
	                                                               throws IdentityAuthorizationException {
		proxy.deleteRolePermissions(Arrays.asList(permissions));
	}

	public void deletePermission(PermissionGroup[] permissions)
	                                                           throws IdentityAuthorizationException {
		proxy.deletePermission(Arrays.asList(permissions));

	}

	public PermissionModule getPermissionList(PermissionRequest request)
	                                                                    throws IdentityAuthorizationException {
		return proxy.getPermissionList(request);
	}

	public Permission[] getPermission(PermissionRequest request)
	                                                            throws IdentityAuthorizationException {
		List<Permission> permissions = proxy.getPermission(request);
		if (permissions != null && !permissions.isEmpty()) {
			return permissions.toArray(new Permission[permissions.size()]);
		}
		return null;
	}

	public void clearPermissions(PermissionRequest clearRequest)
	                                                            throws IdentityAuthorizationException {
		proxy.clearPermissions(clearRequest);

	}

	public void addUserPermissions(UserPermission[] permissions)
	                                                            throws IdentityAuthorizationException {
		proxy.addUserPermissions(Arrays.asList(permissions));
	}

	public void addRolePermissions(RolePermission[] permissions)
	                                                            throws IdentityAuthorizationException {
		proxy.addRolePermissions(Arrays.asList(permissions));
	}

	public void updateUserPermissions(UserPermission[] permissions)
	                                                               throws IdentityAuthorizationException {
		proxy.updateUserPermissions(Arrays.asList(permissions));
	}

	public void updateRolePermissions(RolePermission[] permissions)
	                                                               throws IdentityAuthorizationException {
		proxy.updateRolePermissions(Arrays.asList(permissions));
	}

	public int createModule(PermissionModule module) throws IdentityAuthorizationException {
		return proxy.createModule(module);
	}

	public void updateAuthorizedActions(PermissionModule module)
	                                                            throws IdentityAuthorizationException {
		proxy.updateAuthorizedActions(module);
	}

	public PermissionModule getModule(String moduleName) throws IdentityAuthorizationException {
		return proxy.getModule(moduleName);
	}

	public boolean isAuthorized(PermissionRequest request) throws IdentityAuthorizationException {
		return proxy.isAuthorized(request);
	}

	public PermissionModule[] loadModules() throws IdentityAuthorizationException {
		List<PermissionModule> modules = proxy.loadModules();
		if (modules != null && !modules.isEmpty()) {
			return modules.toArray(new PermissionModule[modules.size()]);
		}
		return new PermissionModule[0];

	}

	public void removeModule(PermissionRequest request) throws IdentityAuthorizationException {
		proxy.removeModule(request);
	}

}
