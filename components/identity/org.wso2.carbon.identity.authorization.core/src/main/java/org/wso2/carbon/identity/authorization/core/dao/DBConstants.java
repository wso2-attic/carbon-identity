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

/**
 * 
 * @author venura
 * @date May 17, 2013
 */
public abstract class DBConstants {
	public static final byte INSERT = 1;

	public static final byte UPDATE = 2;

	public static final byte DELETE = 3;

	public abstract String getUserPermissionsForModuleSql(boolean userNameProvided);

	public abstract String getRolePermissionsForModuleSql(boolean roleNameProvided);

	public abstract String getUserPermissionsForResourceSql(boolean moduleProvided,
	                                                        boolean actionSpecific);

	public abstract String getRolePermissionsForResourceSql(boolean moduleProvided,
	                                                        boolean actionSpecific);

	public abstract String getClearModulePermForUsersSql();

	public abstract String getClearModulePermForRolesSql();

	public abstract String getClearUserPermSql(boolean moduleProvided);

	public abstract String getClearRolePermSql(boolean moduleProvided);
}
