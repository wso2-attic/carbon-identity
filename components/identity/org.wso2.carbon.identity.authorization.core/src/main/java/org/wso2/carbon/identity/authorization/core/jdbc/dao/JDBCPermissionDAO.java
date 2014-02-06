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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.authorization.core.dao.GenericDAO;
import org.wso2.carbon.identity.authorization.core.dao.PermissionDAO;
import org.wso2.carbon.identity.authorization.core.dao.RolePermissionDAO;
import org.wso2.carbon.identity.authorization.core.dao.UserPermissionDAO;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.util.DatabaseUtil;

public class JDBCPermissionDAO extends PermissionDAO {

	private static Log log = LogFactory.getLog(JDBCPermissionDAO.class);

	@Override
	protected void update(Connection connection, boolean commit) throws UserStoreException {
	}

	@Override
	protected void delete(Connection connection, boolean commit) throws UserStoreException {
		StringBuilder sql = new StringBuilder("DELETE FROM UM_PERMISSION ");
		if (getPermissionId() > 0) {
			// If permission id is provided then we canremove the permission
			// using this.
			sql.append(" WHERE UM_ID = ? ");
			DatabaseUtil.updateDatabase(connection, sql.toString(), getPermissionId());
		} else {
			// If permission id is not provided, all the other parameters which
			// participate as composite key should be privided. In this case:
			// resource, module, teant and action
			sql.append(" WHERE UM_RESOURCE_ID = ? AND UM_MODULE_ID = ? AND UM_TENANT_ID = ? AND UM_ACTION = ? ");
			DatabaseUtil.updateDatabase(connection, sql.toString(), getResourceId(), getModuleId(),
			                            getTenantId(), getAction());
		}

	}

	@Override
	protected void insert(PreparedStatement stmt, ResultSet res, Connection connection)
	                                                                                   throws SQLException,
	                                                                                   UserStoreException {
		String sql =
		             "INSERT INTO UM_PERMISSION (UM_RESOURCE_ID, UM_MODULE_ID, UM_TENANT_ID, UM_ACTION) VALUES(?,?,?,?) ";

		stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		byte count = 0;
		stmt.setString(++count, getResourceId());
		stmt.setInt(++count, getModuleId());
		stmt.setInt(++count, getTenantId());
		stmt.setString(++count, getAction());

		int resCount = stmt.executeUpdate();
		if (resCount == 0) {
			String error = "Insertion faild for the permission";
			log.error(error);
			throw new UserStoreException(error);
		}
		res = stmt.getGeneratedKeys();
		if (res.next()) {
			setPermissionId(res.getInt(1));
		}

	}

	@Override
	protected UserPermissionDAO createUserPermissionDAO() {
		return new JDBCUserPermissionDAO();
	}

	@Override
	protected RolePermissionDAO createRolePermissionDAO() {
		return new JDBCRolePermissionDAO();
	}

	@Override
	public List<? extends GenericDAO> load(Connection connection) throws UserStoreException {
		PreparedStatement stmt = null;
		ResultSet res = null;
		resetAppendTxt();
		try {
			StringBuilder sql =
			                    new StringBuilder(
			                                      "SELECT UM_ID, UM_RESOURCE_ID, UM_MODULE_ID, UM_TENANT_ID, UM_ACTION FROM UM_PERMISSION");
			if (getPermissionId() > 0) {
				sql.append(" WHERE UM_ID = ? ");
				appendTxt = AND;
			}
			if (getResourceId() != null && !getResourceId().isEmpty()) {
				sql.append(appendTxt).append(" UM_RESOURCE_ID = ?");
				appendTxt = AND;
			}
			if (getModuleId() > 0) {
				sql.append(appendTxt).append(" UM_MODULE_ID = ? ");
				appendTxt = AND;
			} else {
				sql.append(appendTxt).append(" UM_MODULE_ID IS NULL ");
				appendTxt = AND;
			}
			if (getTenantId() != 0) {
				sql.append(appendTxt).append(" UM_TENANT_ID = ? ");
				appendTxt = AND;
			}
			if (getAction() != null && !getAction().isEmpty()) {
				sql.append(appendTxt).append(" UM_ACTION = ? ");
			}

			stmt = connection.prepareStatement(sql.toString());
			byte count = 0;
			if (getPermissionId() > 0) {
				stmt.setInt(++count, getPermissionId());
			}
			if (getResourceId() != null && !getResourceId().isEmpty()) {
				stmt.setString(++count, getResourceId());
			}
			if (getModuleId() > 0) {
				stmt.setInt(++count, getModuleId());
			}
			if (getTenantId() != 0) {
				stmt.setInt(++count, getTenantId());
			}
			if (getAction() != null && !getAction().isEmpty()) {
				stmt.setString(++count, getAction());
			}
			res = stmt.executeQuery();
			List<PermissionDAO> dataList = new ArrayList<PermissionDAO>();
			PermissionDAO permission = null;
			while (res.next()) {
				permission = new JDBCPermissionDAO();
				dataList.add(permission);
				permission.setPermissionId(res.getInt("UM_ID"));
				permission.setResourceId(res.getString("UM_RESOURCE_ID"));
				permission.setModuleId(res.getInt("UM_MODULE_ID"));
				permission.setTenantId(res.getInt("UM_TENANT_ID"));
				permission.setAction(res.getString("UM_ACTION"));
			}
			return dataList;

		} catch (SQLException e) {
			log.error("Error while loading permissions " + e.getMessage());
			throw new UserStoreException("Error while loading permissions " + e.getMessage());
		} finally {
			DatabaseUtil.closeAllConnections(connection, res, stmt);
		}

	}
}
