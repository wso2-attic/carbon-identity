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

import org.wso2.carbon.identity.authorization.core.dto.UserPermission;
import org.wso2.carbon.user.core.UserStoreException;

/**
 * 
 * @author venura
 * @date May 15, 2013
 */
public abstract class UserPermissionDAO extends PermissionAssignmentDAO {
	private String userName;

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public void map(UserPermission userPerm) {
		super.map(userPerm);
		userName = userPerm.getUserName();
	}

	@Override
	protected void saveDependentModules(Connection connection, boolean commit)
	                                                                          throws UserStoreException {
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(super.toString());
		builder.append("{").append(getClass()).append(" User Name: ").append(userName).append("}");
		return builder.toString();
	}

}
