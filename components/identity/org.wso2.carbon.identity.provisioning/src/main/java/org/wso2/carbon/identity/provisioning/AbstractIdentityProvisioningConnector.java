/*
 *  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.identity.provisioning;

import java.util.Map;
import java.util.Properties;

import org.wso2.carbon.user.api.Permission;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;

public abstract class AbstractIdentityProvisioningConnector {
	private String name;
	private boolean isEnabled;
	private Properties configs;

	public AbstractIdentityProvisioningConnector(String name,
			boolean isEnabled, Properties configs) {
		this.name = name;
		this.isEnabled = isEnabled;
		this.configs = configs;
	}

	public boolean getIsEnabled() {
		return this.isEnabled;
	}

	public String getName() {
		return this.name;
	}

	public String getProperty(String key) {
		return configs.getProperty(key);
	}

	public abstract String createUser(String userName, Object credential,
			String[] roleList, Map<String, String> claims, String profile,
			UserStoreManager userStoreManager)
			throws UserStoreException;

	public abstract String deleteUser(String userName,
			UserStoreManager userStoreManager)
			throws UserStoreException;

	public abstract boolean updateUserListOfRole(String roleName,
			String[] deletedUsers, String[] newUsers,
			UserStoreManager userStoreManager)
			throws UserStoreException;
    

	public abstract boolean updateRoleListOfUser(String userName,
			String[] deletedRoles, String[] newRoles,
			UserStoreManager userStoreManager)
			throws UserStoreException;

	public abstract boolean addRole(String roleName, String[] userList,
			Permission[] permissions, UserStoreManager userStoreManager)
			throws UserStoreException;

	public abstract boolean deleteRole(String roleName,
			UserStoreManager userStoreManager)
			throws UserStoreException;
	

}
