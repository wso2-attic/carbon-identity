/*
 * Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.mgt.dao;

/**
 * This class contains default SQL queries
 */
public class IdentityMgtDBQueries {

    public static final String CHECK_TENANT_ID_EXISTS =
            "SELECT TENANT_ID FROM IDN_TENANT_CONFIG WHERE TENANT_ID=?";

    // STORE Queries
    public static final String STORE_CONFIG_DATA =
            "INSERT "
                    + "INTO IDN_TENANT_CONFIG"
                    + "(TENANT_ID, CONFIG_OBJECT)"
                    + "VALUES (?,?)";

    // LOAD Queries
    public static final String LOAD_CONFIG_DATA =
            "SELECT " + "CONFIG_OBJECT "
                    + "FROM IDN_TENANT_CONFIG "
                    + "WHERE TENANT_ID = ? ";

    // DELETE queries
    public static final String DELETE_CONFIG_DATA =
            "DELETE " + "CONFIG_OBJECT "
                    + "FROM IDN_TENANT_CONFIG "
                    + "WHERE TENANT_ID = ? ";

    // UPDATE queries
    public static final String UPDATE_CONFIG_DATA =
            "UPDATE " + "IDN_TENANT_CONFIG "
                    + "SET CONFIG_OBJECT= ? "
                    + "WHERE TENANT_ID = ? ";
}
