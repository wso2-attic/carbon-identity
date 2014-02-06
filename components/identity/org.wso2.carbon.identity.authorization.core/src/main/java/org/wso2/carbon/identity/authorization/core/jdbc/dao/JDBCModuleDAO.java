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
import org.wso2.carbon.identity.authorization.core.dao.ModuleDAO;
import org.wso2.carbon.identity.authorization.core.dao.ModuleResourceDAO;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.util.DatabaseUtil;

public class JDBCModuleDAO extends ModuleDAO {

	@Override
	protected void deleteObjects(Connection connection) throws SQLException, UserStoreException {
		if (getDeletedActions() != null && !getDeletedActions().isEmpty()) {
			for (String s : getDeletedActions()) {
				String sql =
				             "DELETE FROM UM_MODULE_ACTIONS WHERE UM_MODULE_ID = ? AND UM_ACTION = ?";
				DatabaseUtil.updateDatabase(connection, sql, getModuleId(), s);
			}
		}
	}

	private Log log = LogFactory.getLog(JDBCModuleDAO.class);

	@Override
	public List<? extends GenericDAO> load(Connection connection) throws UserStoreException {

		PreparedStatement stmt = null;
		ResultSet res = null;
		resetAppendTxt();
		try {
			StringBuilder sql = new StringBuilder("SELECT UM_ID, UM_MODULE_NAME FROM UM_MODULE");
			if (getModuleId() > 0) {
				sql.append(" WHERE UM_ID = ? ");
				appendTxt = AND;
			}
			if (getModuleName() != null && getModuleName().trim().length() > 0) {
				sql.append(appendTxt).append(" UM_MODULE_NAME = ?");
			}

			stmt = connection.prepareStatement(sql.toString());
			byte count = 0;
			if (getModuleId() > 0) {
				stmt.setInt(++count, getModuleId());
			}
			if (getModuleName() != null && getModuleName().trim().length() > 0) {

				stmt.setString(++count, getModuleName());
			}
			res = stmt.executeQuery();
			List<ModuleDAO> dataList = new ArrayList<ModuleDAO>();
			ModuleDAO moduel = null;
			while (res.next()) {
				moduel = new JDBCModuleDAO();
				dataList.add(moduel);
				moduel.setModuleId(res.getInt("UM_ID"));
				moduel.setModuleName(res.getString("UM_MODULE_NAME"));
				loadDependancies(moduel, connection);
			}
			return dataList;

		} catch (SQLException e) {
			log.error("Error while loading modules for the id: " + getModuleId(), e);
			throw new UserStoreException("Error while loading modules ", e);
		} finally {
			DatabaseUtil.closeAllConnections(connection, res, stmt);
		}
	}

	@Override
	public void update(Connection connection, boolean commit) throws UserStoreException {
	}

	@Override
	public void delete(Connection connection, boolean commit) throws UserStoreException {
		String sql = "DELETE FROM UM_PERMISSION WHERE UM_MODULE_ID = ? AND UM_TENANT_ID = ? ";
		DatabaseUtil.updateDatabase(connection, sql, getModuleId(), getTenantId());
		sql = "DELETE FROM UM_MODULE WHERE UM_ID = ?";
		DatabaseUtil.updateDatabase(connection, sql, getModuleId());
	}

	@Override
	protected void loadDependancies(ModuleDAO module, Connection connection)
	                                                                        throws UserStoreException {
		PreparedStatement stmt = null;
		ResultSet res = null;
		try {
			StringBuilder sql =
			                    new StringBuilder(
			                                      "SELECT UM_ACTION FROM UM_MODULE_ACTIONS WHERE UM_MODULE_ID = ?");
			stmt = connection.prepareStatement(sql.toString());
			byte count = 0;
			stmt.setInt(++count, module.getModuleId());
			res = stmt.executeQuery();

			while (res.next()) {
				module.getAllowedActions().add(res.getString("UM_ACTION"));
			}

			JDBCModuleResourceDAO resource = new JDBCModuleResourceDAO();
			resource.setModuleId(module.getModuleId());
			List<ModuleResourceDAO> resources =
			                                    (List<ModuleResourceDAO>) resource.load(connection,
			                                                                            false);
			module.setResources(resources);

		} catch (SQLException e) {
			log.error("Error while loading module actions for the id: " + module.getModuleName() +
			          " " + e.getMessage());
			throw new UserStoreException("Error while loading module actions: " + e.getMessage());
		} finally {

			DatabaseUtil.closeAllConnections(null, res, stmt);
		}
	}

	@Override
	protected void insert(PreparedStatement stmt, ResultSet res, Connection connection)
	                                                                                   throws SQLException,
	                                                                                   UserStoreException {
		String sql = "INSERT INTO UM_MODULE (UM_MODULE_NAME) VALUES(?) ";

		stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		byte count = 0;
		stmt.setString(++count, getModuleName());

		int resCount = stmt.executeUpdate();
		if (resCount == 0) {
			String error = "Insertion faild for the module " + getModuleName();
			log.error(error);
			throw new UserStoreException(error);
		}
		res = stmt.getGeneratedKeys();
		if (res.next()) {
			setModuleId(res.getInt(1));
		}

	}

	@Override
	protected void saveDependentModules(Connection connection, boolean commit)
	                                                                          throws UserStoreException {
		if (getAllowedActions() != null && !getAllowedActions().isEmpty()) {
			for (String action : getAllowedActions()) {
				String sql =
				             " INSERT INTO UM_MODULE_ACTIONS (UM_ACTION, UM_MODULE_ID) VALUES (?, ?)";
				DatabaseUtil.updateDatabase(connection, sql, action, getModuleId());
			}
		}

		if (getResources() != null && !getResources().isEmpty()) {
			for (ModuleResourceDAO dao : getResources()) {
				dao.setModuleId(getIdentifier());
				dao.save(connection, false);
			}
		}
	}

	@Override
	protected ModuleResourceDAO createResource() {
		return new JDBCModuleResourceDAO();
	}

}
