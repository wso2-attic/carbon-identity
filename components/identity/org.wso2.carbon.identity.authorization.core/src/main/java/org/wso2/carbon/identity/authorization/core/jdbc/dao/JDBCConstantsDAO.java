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

import java.text.MessageFormat;

import org.wso2.carbon.identity.authorization.core.dao.DBConstants;

/**
 * 
 */
public class JDBCConstantsDAO extends DBConstants {

	private static JDBCConstantsDAO instance;

	private JDBCConstantsDAO() {
	}

	public static JDBCConstantsDAO getInstance() {
		if (instance == null) {
			instance = new JDBCConstantsDAO();
		}
		return instance;
	}

	private static final String GET_USER_PERMISSIONS_FOR_MODULE =
	                                                              "SELECT "
	                                                                      + "UM_PERMISSION.UM_ID, UM_PERMISSION.UM_RESOURCE_ID, UM_PERMISSION.UM_ACTION, "
	                                                                      + "UM_USER_PERMISSION.UM_ID, UM_USER_PERMISSION.UM_IS_ALLOWED, "
	                                                                      + "UM_USER_PERMISSION.UM_USER_NAME FROM "
	                                                                      + "UM_USER_PERMISSION INNER JOIN UM_PERMISSION ON UM_PERMISSION.UM_ID = UM_USER_PERMISSION.UM_PERMISSION_ID "
	                                                                      + "WHERE "
	                                                                      + " {0} UM_PERMISSION.UM_TENANT_ID = ? "
	                                                                      + "AND UM_PERMISSION.UM_MODULE_ID IN (SELECT UM_MODULE.UM_ID FROM UM_MODULE WHERE UM_MODULE.UM_MODULE_NAME = ?)";

	private static final String GET_ROLE_PERMISSIONS_FOR_MODULE =
	                                                              "SELECT "
	                                                                      + "UM_PERMISSION.UM_ID, UM_PERMISSION.UM_RESOURCE_ID, UM_PERMISSION.UM_ACTION, "
	                                                                      + "UM_ROLE_PERMISSION.UM_ID, UM_ROLE_PERMISSION.UM_IS_ALLOWED, "
	                                                                      + "UM_ROLE_PERMISSION.UM_ROLE_NAME FROM "
	                                                                      + "UM_ROLE_PERMISSION INNER JOIN UM_PERMISSION ON UM_PERMISSION.UM_ID = UM_ROLE_PERMISSION.UM_PERMISSION_ID "
	                                                                      + "WHERE "
	                                                                      + " {0} UM_PERMISSION.UM_TENANT_ID = ? "
	                                                                      + "AND UM_PERMISSION.UM_MODULE_ID IN (SELECT UM_MODULE.UM_ID FROM UM_MODULE WHERE UM_MODULE.UM_MODULE_NAME = ?)";

	private static final String GET_USER_PERMISSIONS_FOR_RESOURCE =
	                                                                "SELECT "
	                                                                        + "UM_PERMISSION.UM_ID, UM_PERMISSION.UM_RESOURCE_ID, UM_PERMISSION.UM_ACTION, "
	                                                                        + "UM_USER_PERMISSION.UM_ID, UM_USER_PERMISSION.UM_IS_ALLOWED, "
	                                                                        + "UM_USER_PERMISSION.UM_USER_NAME FROM "
	                                                                        + "UM_USER_PERMISSION INNER JOIN UM_PERMISSION ON UM_PERMISSION.UM_ID = UM_USER_PERMISSION.UM_PERMISSION_ID "
	                                                                        + "WHERE "
	                                                                        + "UM_USER_PERMISSION.UM_USER_NAME = ? AND UM_PERMISSION.UM_TENANT_ID = ? AND UM_PERMISSION.UM_RESOURCE_ID = ?"
	                                                                        + "{0} {1}";

	private static final String GET_ROLE_PERMISSIONS_FOR_RESOURCE =
	                                                                "SELECT "
	                                                                        + "UM_PERMISSION.UM_ID, UM_PERMISSION.UM_RESOURCE_ID, UM_PERMISSION.UM_ACTION, "
	                                                                        + "UM_ROLE_PERMISSION.UM_ID, UM_ROLE_PERMISSION.UM_IS_ALLOWED, "
	                                                                        + "UM_ROLE_PERMISSION.UM_ROLE_NAME FROM "
	                                                                        + "UM_ROLE_PERMISSION INNER JOIN UM_PERMISSION ON UM_PERMISSION.UM_ID = UM_ROLE_PERMISSION.UM_PERMISSION_ID "
	                                                                        + "WHERE "
	                                                                        + "UM_ROLE_PERMISSION.UM_ROLE_NAME = ? AND UM_PERMISSION.UM_TENANT_ID = ? AND UM_PERMISSION.UM_RESOURCE_ID = ?"
	                                                                        + "{0} {1}";

	/**
	 * Clears role permissions for the provided role. If the module
	 * parameter is not passed, deleted permissions are the permissions with
	 * MODULE_ID IS NULL.
	 */
	private static final String CLEAR_ROLE_PERMISSIONS =
	                                                     "DELETE FROM UM_ROLE_PERMISSION WHERE UM_ROLE_PERMISSION.UM_ROLE_NAME = ? AND "
	                                                             + "UM_ROLE_PERMISSION.UM_PERMISSION_ID IN "
	                                                             + "(SELECT UM_PERMISSION.UM_ID FROM UM_PERMISSION WHERE "
	                                                             + "UM_TENANT_ID = ? " + "{0}) ";
	/**
	 * Clears user permissions for the provided user. If the module
	 * parameter is not passed, deleted permissions are the permissions with
	 * MODULE_ID IS NULL.
	 */
	private static final String CLEAR_USER_PERMISSIONS =
	                                                     "DELETE FROM UM_USER_PERMISSION WHERE UM_USER_PERMISSION.UM_USER_NAME = ? AND "
	                                                             + "UM_USER_PERMISSION.UM_PERMISSION_ID IN "
	                                                             + "(SELECT UM_PERMISSION.UM_ID FROM UM_PERMISSION WHERE "
	                                                             + " UM_TENANT_ID = ? " + "{0}) ";

	/**
	 * Clear permissions from the user table for a specific module
	 */
	private static final String CLEAR_MODULE_PERMISSIONS_FOR_USERS =
	                                                                 "DELETE FROM UM_USER_PERMISSION WHERE UM_USER_PERMISSION.UM_PERMISSION_ID IN "
	                                                                         + "(SELECT UM_PERMISSION.UM_ID FROM UM_PERMISSION WHERE "
	                                                                         + "UM_PERMISSION.UM_MODULE_ID = ? AND UM_PERMISSION.UM_TENANT_ID = ?) ";
	/**
	 * Clear permissions from the role table for a specific module
	 */
	private static final String CLEAR_MODULE_PERMISSIONS_FOR_ROLES =
	                                                                 "DELETE FROM UM_ROLE_PERMISSION WHERE UM_ROLE_PERMISSION.UM_PERMISSION_ID IN "
	                                                                         + "(SELECT UM_PERMISSION.UM_ID FROM UM_PERMISSION WHERE "
	                                                                         + "UM_PERMISSION.UM_MODULE_ID = ? AND UM_TENANT_ID = ?) ";
	private static final String UM_MODULE_ID_CHECK_WITH_MODULE_NAME =
	                                                                  "AND UM_PERMISSION.UM_MODULE_ID IN (SELECT UM_MODULE.UM_ID FROM UM_MODULE WHERE UM_MODULE.UM_MODULE_NAME = ?)";

	private static final String UM_MODULE_ID_CHECK_WITH_NULL =
	                                                           "AND UM_PERMISSION.UM_MODULE_ID = 0 ";

	private static final String UM_PERMISSION_ACTION_CHECK = " AND UM_PERMISSION.UM_ACTION = ? ";

	private static final String UM_PERMISSIONS_ROLE_CHECK =
	                                                        " UM_ROLE_PERMISSION.UM_ROLE_NAME = ? AND";

	private static final String UM_PERMISSIONS_USER_CHECK =
	                                                        " UM_USER_PERMISSION.UM_USER_NAME = ? AND";

	@Override
	public String getUserPermissionsForResourceSql(boolean moduleProvided, boolean actionSpecific) {
		if (moduleProvided) {
			return MessageFormat.format(GET_USER_PERMISSIONS_FOR_RESOURCE,
			                            UM_MODULE_ID_CHECK_WITH_MODULE_NAME,
			                            actionSpecific ? UM_PERMISSION_ACTION_CHECK : "");
		} else {
			return MessageFormat.format(GET_USER_PERMISSIONS_FOR_RESOURCE,
			                            UM_MODULE_ID_CHECK_WITH_NULL,
			                            actionSpecific ? UM_PERMISSION_ACTION_CHECK : "");
		}
	}

	@Override
	public String getRolePermissionsForResourceSql(boolean moduleProvided, boolean actionSpecific) {
		if (moduleProvided) {
			return MessageFormat.format(GET_ROLE_PERMISSIONS_FOR_RESOURCE,
			                            UM_MODULE_ID_CHECK_WITH_MODULE_NAME,
			                            actionSpecific ? UM_PERMISSION_ACTION_CHECK : "");
		} else {
			return MessageFormat.format(GET_ROLE_PERMISSIONS_FOR_RESOURCE,
			                            UM_MODULE_ID_CHECK_WITH_NULL,
			                            actionSpecific ? UM_PERMISSION_ACTION_CHECK : "");
		}

	}

	@Override
	public String getClearModulePermForUsersSql() {
		return CLEAR_MODULE_PERMISSIONS_FOR_USERS;
	}

	@Override
	public String getClearModulePermForRolesSql() {
		return CLEAR_MODULE_PERMISSIONS_FOR_ROLES;
	}

	@Override
	public String getClearUserPermSql(boolean moduleProvided) {
		if (moduleProvided) {
			return MessageFormat.format(CLEAR_USER_PERMISSIONS, UM_MODULE_ID_CHECK_WITH_MODULE_NAME);
		} else {
			return MessageFormat.format(CLEAR_USER_PERMISSIONS, UM_MODULE_ID_CHECK_WITH_NULL);
		}

	}

	@Override
	public String getClearRolePermSql(boolean moduleProvided) {
		if (moduleProvided) {
			return MessageFormat.format(CLEAR_ROLE_PERMISSIONS, UM_MODULE_ID_CHECK_WITH_MODULE_NAME);
		} else {
			return MessageFormat.format(CLEAR_ROLE_PERMISSIONS, UM_MODULE_ID_CHECK_WITH_NULL);
		}
	}

	@Override
	public String getUserPermissionsForModuleSql(boolean userNameProvided) {
		String sql =
		             userNameProvided ? MessageFormat.format(GET_USER_PERMISSIONS_FOR_MODULE,
		                                                     UM_PERMISSIONS_USER_CHECK)
		                             : MessageFormat.format(GET_USER_PERMISSIONS_FOR_MODULE, "");
		return sql;
	}

	@Override
	public String getRolePermissionsForModuleSql(boolean roleNameProvided) {
		String sql =
		             roleNameProvided ? MessageFormat.format(GET_ROLE_PERMISSIONS_FOR_MODULE,
		                                                     UM_PERMISSIONS_ROLE_CHECK)
		                             : MessageFormat.format(GET_ROLE_PERMISSIONS_FOR_MODULE, "");
		return sql;

	}

}
