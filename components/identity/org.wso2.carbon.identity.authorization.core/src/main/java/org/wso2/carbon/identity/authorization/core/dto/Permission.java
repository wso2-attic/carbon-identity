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
 * 
 * @author venura
 * @date May 16, 2013
 */
public class Permission {
	/**
	 * Id of the UM_PERMISSION table
	 */
	private int permissionId;

	/**
	 * Id of the UM_USER_PERMISSION or UM_ROLE_PERMISSION table
	 */
	private int subjectPermissionId;

	/**
	 * User name or role name
	 */
	private String subject;

	/**
	 * User id or role id
	 */
	private int subjectId;

	private String resourceId;
	private String action;
	private boolean authorized;

	private boolean rolePermission;

	public int getPermissionId() {
		return permissionId;
	}

	public void setPermissionId(int permissionId) {
		this.permissionId = permissionId;
	}

	public int getSubjectPermissionId() {
		return subjectPermissionId;
	}

	public void setSubjectPermissionId(int subjectPermissionId) {
		this.subjectPermissionId = subjectPermissionId;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public int getSubjectId() {
		return subjectId;
	}

	public void setSubjectId(int subjectId) {
		this.subjectId = subjectId;
	}

	public String getResourceId() {
		return resourceId;
	}

	public void setResourceId(String resourceId) {
		this.resourceId = resourceId;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public boolean isAuthorized() {
		return authorized;
	}

	public void setAuthorized(boolean authorized) {
		this.authorized = authorized;
	}

	public boolean isRolePermission() {
		return rolePermission;
	}

	public void setRolePermission(boolean rolePermission) {
		this.rolePermission = rolePermission;
	}

}
