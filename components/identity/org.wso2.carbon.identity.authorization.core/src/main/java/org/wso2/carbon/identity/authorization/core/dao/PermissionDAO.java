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

package org.wso2.carbon.identity.authorization.core.dao;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.authorization.core.dto.PermissionGroup;
import org.wso2.carbon.identity.authorization.core.dto.RolePermission;
import org.wso2.carbon.identity.authorization.core.dto.UserPermission;
import org.wso2.carbon.user.core.UserStoreException;

/**
 * 
 * @author venura
 * @date May 14, 2013
 */
public abstract class PermissionDAO extends GenericDAO {

	private static Log log = LogFactory.getLog(PermissionDAO.class);

	private int permissionId;
	private String resourceId;
	private int moduleId;
	private String action;

	private List<UserPermissionDAO> assignedUsers;
	private List<RolePermissionDAO> assignedRoles;

	public int getPermissionId() {
		return permissionId;
	}

	public void setPermissionId(int permissionId) {
		this.permissionId = permissionId;
	}

	public String getResourceId() {
		return resourceId;
	}

	public void setResourceId(String resourceId) {
		this.resourceId = resourceId;
	}

	public int getModuleId() {
		return moduleId;
	}

	public void setModuleId(int moduleId) {
		this.moduleId = moduleId;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public List<UserPermissionDAO> getAssignedUsers() {
		if (assignedUsers == null) {
			assignedUsers = new ArrayList<UserPermissionDAO>();
		}
		return assignedUsers;
	}

	public void setAssignedUsers(List<UserPermissionDAO> assignedUsers) {
		this.assignedUsers = assignedUsers;
	}

	public List<RolePermissionDAO> getAssignedRoles() {
		if (assignedRoles == null) {
			assignedRoles = new ArrayList<RolePermissionDAO>();
		}
		return assignedRoles;
	}

	public void setAssignedRoles(List<RolePermissionDAO> assignedRoles) {
		this.assignedRoles = assignedRoles;
	}

	@Override
	protected void saveDependentModules(Connection connection, boolean commit)
	                                                                          throws UserStoreException {
		if (assignedRoles != null && !assignedRoles.isEmpty()) {
			if (log.isDebugEnabled()) {
				log.debug("Automattically adding permissions for the roles defined at the permission insert time");
			}
			for (RolePermissionDAO rolePerm : assignedRoles) {
				rolePerm.setPermissionId(getPermissionId());
				rolePerm.setTenantId(getTenantId());
				rolePerm.delete(connection, false);
				rolePerm.insert(connection, false);
			}
		}

		if (assignedUsers != null && !assignedRoles.isEmpty()) {
			if (log.isDebugEnabled()) {
				log.debug("Automattically adding permissions for the users defined at the permission insert time");
			}
			for (UserPermissionDAO userPerm : assignedUsers) {
				userPerm.setPermissionId(getPermissionId());
				userPerm.setTenantId(getTenantId());
				userPerm.delete(connection, false);
				userPerm.insert(connection, false);
			}
		}

	}

	public void map(PermissionGroup permission) {
		permissionId = permission.getPermissionId();
		resourceId = permission.getResource();
		moduleId = permission.getModuleId();
		action = permission.getAction();

		if (permission.getRolePermissions() != null && permission.getRolePermissions().length > 0) {
			for (RolePermission rolePerm : permission.getRolePermissions()) {
				RolePermissionDAO role = createRolePermissionDAO();
				getAssignedRoles().add(role);
				role.map(rolePerm);
			}
		}

		if (permission.getUserPermissions() != null && permission.getUserPermissions().length > 0) {
			for (UserPermission userPerm : permission.getUserPermissions()) {
				UserPermissionDAO user = createUserPermissionDAO();
				getAssignedUsers().add(user);
				user.map(userPerm);
			}
		}
	}

	protected abstract UserPermissionDAO createUserPermissionDAO();

	protected abstract RolePermissionDAO createRolePermissionDAO();

	@Override
	public int getIdentifier() {
		return permissionId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(super.toString());
		builder.append("{").append(getClass()).append(" Permission Id: ").append(permissionId)
		       .append(" ResourceId: ").append(resourceId).append(" Module Id: ").append(moduleId)
		       .append(" Action: ").append(action);

		if (assignedUsers != null) {
			builder.append(" Assigned Users: ");
			for (UserPermissionDAO dao : assignedUsers) {
				builder.append(dao.toString());
			}
		}

		if (assignedRoles != null) {
			builder.append(" Assigned Roles: ");
			for (RolePermissionDAO dao : assignedRoles) {
				builder.append(dao.toString());
			}
		}
		builder.append("}");
		return builder.toString();
	}

}
