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

package org.wso2.carbon.identity.authorization.core.jdbc.dao;

import org.wso2.carbon.identity.authorization.core.dao.DAOFactory;
import org.wso2.carbon.identity.authorization.core.dao.DBConstants;
import org.wso2.carbon.identity.authorization.core.dao.ModuleDAO;
import org.wso2.carbon.identity.authorization.core.dao.PermissionDAO;
import org.wso2.carbon.identity.authorization.core.dao.RolePermissionDAO;
import org.wso2.carbon.identity.authorization.core.dao.UserPermissionDAO;

public class JDBCDAOFactory extends DAOFactory {

	@Override
	public ModuleDAO createModule() {
		return new JDBCModuleDAO();
	}

	@Override
	public PermissionDAO createPermission() {
		return new JDBCPermissionDAO();
	}

	@Override
	public UserPermissionDAO createUserPermission() {
		return new JDBCUserPermissionDAO();
	}

	@Override
	public RolePermissionDAO createRolePermission() {
		return new JDBCRolePermissionDAO();
	}

	@Override
	public DBConstants createConstants() {
		return JDBCConstantsDAO.getInstance();
	}

}
