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

package org.wso2.carbon.identity.authorization.core.dto;

/**
 * Encapsulate the instance of permission
 */
public class PermissionGroup {

	/**
	 * unique identifier to identify the permission instance.
	 */
	private int permissionId;

	/**
	 * To which resource the permission is attached
	 */
	private String resource;

	/**
	 * This will be the module (application domain), to which the permission is
	 * applicable
	 */
	private String moduleName;
	private int moduleId;

	/**
	 * Action which will be restricted based on the permission
	 */
	private String action;

	private UserPermission[] userPermissions;
	private RolePermission[] rolePermissions;

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public int getPermissionId() {
		return permissionId;
	}

	public void setPermissionId(int permissionId) {
		this.permissionId = permissionId;
	}

	public String getResource() {
		return resource;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}

	public String getModuleName() {
		return moduleName;
	}

	public void setModuleName(String moduleName) {
		this.moduleName = moduleName;
	}

	public UserPermission[] getUserPermissions() {
		return userPermissions;
	}

	public void setUserPermissions(UserPermission[] userPermissions) {
		this.userPermissions = userPermissions;
	}

	public RolePermission[] getRolePermissions() {
		return rolePermissions;
	}

	public void setRolePermissions(RolePermission[] rolePermissions) {
		this.rolePermissions = rolePermissions;
	}

	public int getModuleId() {
		return moduleId;
	}

	public void setModuleId(int moduleId) {
		this.moduleId = moduleId;
	}

}
