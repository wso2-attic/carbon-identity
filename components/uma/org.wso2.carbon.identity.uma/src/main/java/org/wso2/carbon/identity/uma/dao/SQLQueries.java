/*
 *
 *  *
 *  * Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *  *
 *  * WSO2 Inc. licenses this file to you under the Apache License,
 *  * Version 2.0 (the "License"); you may not use this file except
 *  * in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *  * /
 *
 */

package org.wso2.carbon.identity.uma.dao;

public class SQLQueries {

    public static final String INSERT_RESOURCE_SET =
            "INSERT INTO $resourceSetRegTable(RESOURCE_SET_ID, NAME, URI, TYPE, ICON_URI," +
                    "AUTHZ_USER, CONSUMER_KEY, RESOURCE_SET_SCOPE_HASH, TIME_CREATED) " +
                    "VALUES (?,?,?,?,?,?,?,?,?)";

    public static final String INSERT_RESOURCE_SET_SCOPE = "INSERT INTO $resourceSetScopeTable (RESOURCE_SET_ID, " +
            "RESOURCE_SET_SCOPE) VALUES (?,?)";

    public static final String UPDATE_RESOURCE_SET =
            "UPDATE $resourceSetRegTable SET NAME=?, URI=?, TYPE=?, ICON_URI=?," +
                    "RESOURCE_SET_SCOPE=? WHERE RESOURCE_SET_ID=?";

    public static final String DELETE_RESOURCE_SET_FROM_ID =
            "DELETE FROM $resourceSetRegTable WHERE RESOURCE_SET_ID = ? ";

    public static final String GET_RESOURCE_SET_FROM_ID =
            "SELECT * FROM $resourceSetRegTable WHERE RESOURCE_SET_ID=?";

    public static final String GET_ALL_RESOURCE_SETS=
            "SELECT * FROM $resourceSetRegTable WHERE CONSUMER_KEY=?";

    public static final String GET_ALL_RESOURCE_SET_IDS =
            "SELECT RESOURCE_SET_ID FROM $resourceSetRegTable WHERE CONSUMER_KEY=?";



    private SQLQueries(){}
}
