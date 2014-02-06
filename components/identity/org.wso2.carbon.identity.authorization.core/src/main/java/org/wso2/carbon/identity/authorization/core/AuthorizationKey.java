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

import org.wso2.carbon.base.MultitenantConstants;

/**
 * Date: Oct 7, 2010 Time: 11:13:54 AM
 */

/**
 * A key class which wraps a cache key used by Authorization manager.
 */
public class AuthorizationKey {

	private static final long serialVersionUID = 926710669453381695L;

	private String userName;

	private String resourceId;

	private String action;

	private int tenantId;

	private String serverId;

	private int moduleId;

	// Role name can be null since this was introduced later stage. Role
	// name was implemented as a effect of the custom permissions
	private String roleName;

	public AuthorizationKey(String serverId, int tenantId, String userName, String resourceId,
	                        String action) {
		this.userName = userName;
		this.resourceId = resourceId;
		this.action = action;
		this.tenantId = tenantId;
		this.serverId = serverId;
	}

	public AuthorizationKey(String serverId, int tenantId, String userName, String resourceId,
	                        String action, int moduleId, String roleName) {
		super();
		this.userName = userName;
		this.resourceId = resourceId;
		this.action = action;
		this.tenantId = tenantId;
		this.serverId = serverId;
		this.moduleId = moduleId;
		this.roleName = roleName;
	}

	@Override
	public boolean equals(Object otherObject) {

		if (!(otherObject instanceof AuthorizationKey)) {
			return false;
		}

		AuthorizationKey secondObject = (AuthorizationKey) otherObject;

		// serverId can be null. We assume other parameters are not null.
		return checkAttributesAreEqual(serverId, tenantId, userName, resourceId, action, roleName,
		                               moduleId, secondObject);
	}

	@Override
	public int hashCode() {

		return getHashCodeForAttributes(serverId, tenantId, userName, resourceId, action, moduleId,
		                                roleName);
	}

	public String getUserName() {
		return userName;
	}

	public String getResourceId() {
		return resourceId;
	}

	public String getAction() {
		return action;
	}

	public int getTenantId() {
		return tenantId;
	}

	public void setTenantId(int tenantId) {
		this.tenantId = tenantId;
	}

	public String getServerId() {
		return serverId;
	}

	public int getModuleId() {
		return moduleId;
	}

	public void setModuleId(int moduleId) {
		this.moduleId = moduleId;
	}

	public String getRoleName() {
		if (roleName == null) {
			roleName = "";
		}
		return roleName;
	}

	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}

	public void setServerId(String serverId) {
		this.serverId = serverId;
	}

	private int getHashCodeForAttributes(String severId, int tenantId, String userName,
	                                     String resourceId, String action, int moduleId,
	                                     String roleName) {
		if (roleName == null) {
			roleName = "";
		}
		if ((tenantId != MultitenantConstants.INVALID_TENANT_ID) && userName != null &&
		    severId != null) {
			if (tenantId == MultitenantConstants.SUPER_TENANT_ID) {
				tenantId = 0;
			}
			return tenantId + moduleId + userName.hashCode() * 5 + severId.hashCode() * 7 +
			       resourceId.hashCode() * 11 + action.hashCode() * 13 + roleName.hashCode() * 17;
		} else if (severId != null) {
			return moduleId + severId.hashCode() * 7 + resourceId.hashCode() * 11 +
			       action.hashCode() * 13 + roleName.hashCode() * 17;
		} else {
			return moduleId + resourceId.hashCode() * 11 + action.hashCode() * 13 +
			       roleName.hashCode() * 17;
		}
	}

	private boolean checkAttributesAreEqual(String serverId, int tenantId, String userName,
	                                        String resourceIdentifier, String actionName,
	                                        String roleName, int moduleId,
	                                        AuthorizationKey authorizationKey) {
		boolean equality =
		                   tenantId == authorizationKey.getTenantId() &&
		                           userName.equals(authorizationKey.getUserName()) &&
		                           resourceIdentifier.equals(authorizationKey.getResourceId()) &&
		                           actionName.equals(authorizationKey.getAction()) &&
		                           authorizationKey.getRoleName().equals(roleName) &&
		                           moduleId == authorizationKey.getModuleId();
		// as server id can be null, then we skip the equality of it.
		if (serverId != null) {
			equality = equality && serverId.equals(authorizationKey.getServerId());
		}

		return equality;
	}
}