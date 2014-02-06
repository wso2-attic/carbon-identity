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

package org.wso2.carbon.identity.authorization.core.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.authorization.core.AuthorizationKey;
import org.wso2.carbon.identity.authorization.core.dao.DAOFactory;
import org.wso2.carbon.identity.authorization.core.dao.DBConstants;
import org.wso2.carbon.identity.authorization.core.dao.GenericDAO;
import org.wso2.carbon.identity.authorization.core.dao.ModuleDAO;
import org.wso2.carbon.identity.authorization.core.dto.Permission;
import org.wso2.carbon.identity.authorization.core.dto.PermissionModule;
import org.wso2.carbon.identity.authorization.core.internal.AuthorizationServiceComponent;
import org.wso2.carbon.identity.authorization.core.jdbc.dao.JDBCConstantsDAO;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.util.DatabaseUtil;
import org.wso2.carbon.utils.xml.StringUtils;

/**
 *
 */
public class SQLExecutor {

	private static Log log = LogFactory.getLog(SQLExecutor.class);

	private DataSource dataSource;
	private DAOFactory factory;
	private DBConstants dbConstants;

	private static SQLExecutor instance;

	private SQLExecutor() {

		dataSource =
		             DatabaseUtil.getRealmDataSource(AuthorizationServiceComponent.getRealmService()
		                                                                          .getBootstrapRealmConfiguration());
		factory = DAOFactory.createFactory();

		dbConstants = factory.createConstants();
	}

	public static SQLExecutor getInstance() {
		if (instance == null) {
			instance = new SQLExecutor();
		}
		return instance;
	}

	/**
	 * Retrieve the module for the provided module name
	 * 
	 * @param moduleName
	 * @return
	 * @throws UserStoreException
	 * @throws SQLException
	 */
	public ModuleDAO populateModule(String moduleName) throws UserStoreException {
		ModuleDAO module = factory.createModule();
		module.setModuleName(moduleName);
		String error = null;
		try {
			List<? extends GenericDAO> modules = module.load(dataSource.getConnection());
			if (modules != null && !modules.isEmpty()) {
				module = (ModuleDAO) modules.get(0);
			}
		} catch (SQLException e) {
			error = "Error while loading module: " + moduleName;
			log.error(error);
			throw new UserStoreException(error, e);
		}
		if (module == null) {
			error = "Modules not found for the bgiven module name: " + moduleName;
			log.error(error);
			throw new UserStoreException(error);
		}
		return module;
	}

	public List<ModuleDAO> loadModules() throws UserStoreException {
		ModuleDAO module = factory.createModule();
		String error = null;
		try {
			List<? extends GenericDAO> modules = module.load(dataSource.getConnection());
			if (modules != null && !modules.isEmpty()) {
				return (List<ModuleDAO>) modules;
			}
		} catch (SQLException e) {
			error = "Error while loading module ";
			log.error(error);
			throw new UserStoreException(error, e);
		}
		return null;

	}

	public PermissionModule loadModulePermissions(final String subject, final boolean isUserPerm,
	                                              final String moduleName, int tenantId)
	                                                                                    throws UserStoreException {
		String sql = null;
		PreparedStatement stmt = null;
		ResultSet res = null;
		PermissionModule module = null;
		List<Permission> permissions = null;
		Connection connection = null;
		boolean subjectProvided = !StringUtils.isEmpty(subject);
		try {
			connection = dataSource.getConnection();
			if (isUserPerm) {
				sql = dbConstants.getUserPermissionsForModuleSql(subjectProvided);

				stmt = connection.prepareStatement(sql);
				byte count = 0;
				if (subjectProvided) {
					stmt.setString(++count, subject);
				}
				stmt.setInt(++count, tenantId);
				stmt.setString(++count, moduleName);

				res = stmt.executeQuery();
				module = new PermissionModule();
				permissions = new ArrayList<Permission>();
				Permission permission = null;
				while (res.next()) {
					permission = new Permission();
					permissions.add(permission);
					fillUserPermission(res, permission);
				}
				module.setPermissions(permissions.toArray(new Permission[permissions.size()]));
			} else {
				sql = dbConstants.getRolePermissionsForModuleSql(subjectProvided);

				stmt = connection.prepareStatement(sql);
				byte count = 0;
				if (subjectProvided) {
					stmt.setString(++count, subject);
				}
				stmt.setInt(++count, tenantId);
				stmt.setString(++count, moduleName);

				res = stmt.executeQuery();
				module = new PermissionModule();
				permissions = new ArrayList<Permission>();
				Permission permission = null;
				while (res.next()) {
					permission = new Permission();
					permissions.add(permission);
					fillRolePermission(res, permission);
				}
				module.setPermissions(permissions.toArray(new Permission[permissions.size()]));

			}
		} catch (SQLException e) {
			String error = "Error loading permissions for the provided module " + e.getMessage();
			log.error(error);
			throw new UserStoreException(error, e);
		} finally {
			DatabaseUtil.closeAllConnections(connection, res, stmt);
		}
		return module;
	}

	private void fillRolePermission(ResultSet res, Permission permission) throws SQLException {
		permission.setPermissionId(res.getInt("UM_PERMISSION.UM_ID"));
		permission.setSubjectPermissionId(res.getInt("UM_ROLE_PERMISSION.UM_ID"));
		permission.setResourceId(res.getString("UM_PERMISSION.UM_RESOURCE_ID"));
		permission.setAction(res.getString("UM_PERMISSION.UM_ACTION"));
		permission.setAuthorized(res.getBoolean("UM_ROLE_PERMISSION.UM_IS_ALLOWED"));
		permission.setSubject(res.getString("UM_ROLE_PERMISSION.UM_ROLE_NAME"));
		permission.setRolePermission(true);
	}

	private void fillUserPermission(ResultSet res, Permission permission) throws SQLException {
		permission.setPermissionId(res.getInt("UM_PERMISSION.UM_ID"));
		permission.setSubjectPermissionId(res.getInt("UM_USER_PERMISSION.UM_ID"));
		permission.setResourceId(res.getString("UM_PERMISSION.UM_RESOURCE_ID"));
		permission.setAction(res.getString("UM_PERMISSION.UM_ACTION"));
		permission.setAuthorized(res.getBoolean("UM_USER_PERMISSION.UM_IS_ALLOWED"));
		permission.setSubject(res.getString("UM_USER_PERMISSION.UM_USER_NAME"));
		permission.setRolePermission(false);
	}

	public List<Permission> loadPermission(final String subject, final boolean isUserPerm,
	                                       final String moduleName, final String resource,
	                                       final String action, int tenantId)
	                                                                         throws UserStoreException {
		String sql = null;
		PreparedStatement stmt = null;
		ResultSet res = null;
		List<Permission> permissionList = new ArrayList<Permission>();
		Connection connection = null;
		Permission permission = null;

		boolean isModuleSpecific = !StringUtils.isEmpty(moduleName);
		boolean isActionSpecific = !StringUtils.isEmpty(action);

		try {
			connection = dataSource.getConnection();
			if (isUserPerm) {
				sql =
				      dbConstants.getUserPermissionsForResourceSql(isModuleSpecific,
				                                                   isActionSpecific);

			} else {
				sql =
				      dbConstants.getRolePermissionsForResourceSql(isModuleSpecific,
				                                                   isActionSpecific);
			}

			stmt = connection.prepareStatement(sql);
			byte count = 0;
			stmt.setString(++count, subject);
			stmt.setInt(++count, tenantId);
			stmt.setString(++count, resource);
			if (isModuleSpecific) {
				stmt.setString(++count, moduleName);
			}
			if (isActionSpecific) {
				stmt.setString(++count, action);
			}
			res = stmt.executeQuery();
			while (res.next()) {
				permission = new Permission();
				permissionList.add(permission);
				if (isUserPerm) {
					fillUserPermission(res, permission);
				} else {
					fillRolePermission(res, permission);
				}
			}

		} catch (SQLException e) {
			String error = "Error loading permission for the provided resource" + e.getMessage();
			log.error(error);
			throw new UserStoreException(error, e);
		} finally {
			DatabaseUtil.closeAllConnections(connection, res, stmt);
		}
		return permissionList;
	}

	public List<? extends GenericDAO> load(GenericDAO dao) throws UserStoreException, SQLException {
		List<? extends GenericDAO> data = dao.load(dataSource.getConnection());
		return data;
	}

	/**
	 * Populates modules (application domains) applicable for authorization
	 * 
	 * @throws SQLException
	 * @throws UserStoreException
	 */
	public List<ModuleDAO> populateModules() {
		List<ModuleDAO> modules = null;
		try {
			modules = (List<ModuleDAO>) factory.createModule().load(dataSource.getConnection());
		} catch (UserStoreException e) {
			log.fatal("Error while loading modules for the first time " + e.getMessage());
		} catch (SQLException e) {
			log.fatal("Error while loading modules for the first time " + e.getMessage());
		}
		return modules;
	}

	/**
	 * Saves the provided DAO object
	 * 
	 * @param dao
	 * @return Unique identifier generated while inserting the object to DB. If
	 *         the operations is not INSERT, then the identifier will be the
	 *         unique identifier which was in the object previously.
	 * @throws UserStoreException
	 */
	public int save(GenericDAO dao) throws UserStoreException {
		try {
			dao.save(dataSource.getConnection());
			return dao.getIdentifier();
		} catch (SQLException e) {
			String error = "Error geting the connection from dta source " + e.getMessage();
			log.error(error);
			throw new UserStoreException(error, e);
		}
	}

	public void clearPermissions(final String subject, final boolean isUserPermissions,
	                             final String moduleName, int tenantId) throws UserStoreException {

		Connection connection = null;
		try {
			connection = dataSource.getConnection();
			boolean moduleProvided = !StringUtils.isEmpty(moduleName);
			String sql =
			             isUserPermissions ? dbConstants.getClearUserPermSql(moduleProvided)
			                              : dbConstants.getClearRolePermSql(moduleProvided);
			if (moduleProvided) {
				// If module is provided, permissions which will be deleted are
				// the ones with module id as specified.
				DatabaseUtil.updateDatabase(connection, sql, subject, tenantId, moduleName);
			} else {
				// If module is no provided, permissions which will be deleted
				// are the ones with module id as zero.
				DatabaseUtil.updateDatabase(connection, sql, subject, tenantId);
			}
		} catch (SQLException e) {
			String error = "Error geting the connection from dta source " + e.getMessage();
			log.error(error);
			throw new UserStoreException(error, e);
		} finally {
			DatabaseUtil.closeConnection(connection);
		}

	}

	/**
	 * Delete all the permissions relevant for a module.
	 * 
	 * @param moduleId
	 * @param connection
	 *            TODO
	 * @param commit
	 *            TODO
	 * @throws UserStoreException
	 */
	public void clearModulePermissions(final int moduleId, int tenantId, Connection connection,
	                                   boolean commit) throws UserStoreException {
		boolean connectionProvided = connection != null;
		try {
			connection = connectionProvided ? connection : dataSource.getConnection();
			connection.setAutoCommit(false);
			String sql = dbConstants.getClearModulePermForRolesSql();

			// If permissions should be removed for a given module, both user
			// and role permissions should be removed.
			DatabaseUtil.updateDatabase(connection, sql, moduleId, tenantId);
			sql = dbConstants.getClearModulePermForUsersSql();
			DatabaseUtil.updateDatabase(connection, sql, moduleId, tenantId);

			if (commit) {
				connection.commit();
			}

		} catch (SQLException e) {
			String error =
			               "Error geting the connection from data source or commiting the connection " +
			                       e.getMessage();
			log.error(error);
			try {
				connection.rollback();
			} catch (SQLException e1) {
				log.error(e1.getMessage());
			}

			throw new UserStoreException(error, e);
		} finally {
			if (!connectionProvided) {
				DatabaseUtil.closeConnection(connection);
			}
		}

	}

	public void removeModule(final int moduleId, int tenantId) throws UserStoreException {
		Connection connection;
		try {
			connection = dataSource.getConnection();
			connection.setAutoCommit(false);
			clearModulePermissions(moduleId, tenantId, connection, false);
			ModuleDAO dao = factory.createModule();
			dao.setModuleId(moduleId);
			dao.setTenantId(tenantId);
			dao.setStatus(JDBCConstantsDAO.DELETE);
			dao.save(connection);

		} catch (SQLException e) {
			log.error(e.getMessage());
			throw new UserStoreException(e);
		}

	}

	public Map<AuthorizationKey, Boolean> createCacheEntry(GenericDAO dao) throws SQLException,
	                                                                      UserStoreException {
		return dao.createCacheEntry(dataSource.getConnection());
	}
}
