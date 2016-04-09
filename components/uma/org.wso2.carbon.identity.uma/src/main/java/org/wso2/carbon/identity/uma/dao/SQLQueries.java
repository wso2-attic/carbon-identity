/*
 *  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */

package org.wso2.carbon.identity.uma.dao;

public class SQLQueries {

    public static final String RESOURCE_SET_TABLE = "IDN_UMA_RESOURCE_SET";
    public static final String RESOURCE_SET_METADATA_TABLE = "IDN_UMA_RESOURCE_SET_METADATA";
    public static final String RESOURCE_SET_SCOPE_TABLE = "IDN_UMA_RESOURCE_SCOPE_ASSOCIATION";


    public static final String INSERT_RESOURCE_SET =
            "INSERT INTO IDN_UMA_RESOURCE_SET(RESOURCE_SET_ID, TIME_CREATED,TOKEN_ID) " +
                    "VALUES (?,?,?)";
    ;

    public static final String INSERT_RESOURCE_SET_METADATA =
            "INSERT INTO IDN_UMA_RESOURCE_SET_METADATA(RESOURCE_SET_ID, PROPERTY_KEY, PROPERTY_VALUE) " +
                    "VALUES (?,?,?)";

    public static final String INSERT_RESOURCE_SET_SCOPE =
            "INSERT INTO IDN_UMA_RESOURCE_SCOPE_ASSOCIATION(RESOURCE_SET_ID,SCOPE) VALUES (?,?)";


    public static final String UPDATE_RESOURCE_SET_METADATA =
            "UPDATE $resourceSetRegMetaDataTable SET PROPERTY_VALUE=?" +
                    " WHERE RESOURCE_SET_ID=? AND PROPERTY_KEY=?";

//    public static final String DELETE_RESOURCE_SET_FROM_ID =
//            "DELETE FROM $resourceSetRegTable WHERE RESOURCE_SET_ID =?";

    public static final String DELETE_RESOURCE_SET_BY_ID =
            "DELETE FROM IDN_UMA_RESOURCE_SET WHERE RESOURCE_SET_ID=? AND TOKEN_ID IN (SELECT T4.TOKEN_ID FROM " +
                    "(SELECT * FROM IDN_OAUTH2_ACCESS_TOKEN WHERE TOKEN_ID=?) AS T3 JOIN IDN_OAUTH2_ACCESS_TOKEN AS T4 " +
                    "ON T3.AUTHZ_USER=T4.AUTHZ_USER AND T3.TENANT_ID=T4.TENANT_ID AND T3.USER_DOMAIN=T4.USER_DOMAIN) ";

    public static final String GET_RESOURCE_SET_FROM_ID =
            "SELECT * FROM (SELECT RESOURCE_METADATA.RESOURCE_SET_ID,TIME_CREATED,TOKEN_ID,PROPERTY_KEY,PROPERTY_VALUE," +
                    "SCOPE FROM (SELECT A.RESOURCE_SET_ID,TIME_CREATED,TOKEN_ID,PROPERTY_KEY,PROPERTY_VALUE FROM " +
                    "IDN_UMA_RESOURCE_SET AS A JOIN IDN_UMA_RESOURCE_SET_METADATA AS B ON A.RESOURCE_SET_ID = " +
                    "B.RESOURCE_SET_ID) AS RESOURCE_METADATA JOIN IDN_UMA_RESOURCE_SCOPE_ASSOCIATION " +
                    "ON RESOURCE_METADATA.RESOURCE_SET_ID=IDN_UMA_RESOURCE_SCOPE_ASSOCIATION.RESOURCE_SET_ID) " +
                    "AS RESOURCE_SET WHERE RESOURCE_SET_ID=? AND TOKEN_ID IN (SELECT TOKEN_ID FROM (SELECT AUTHZ_USER," +
                    "TENANT_ID,USER_DOMAIN FROM IDN_OAUTH2_ACCESS_TOKEN WHERE TOKEN_ID=?) AS T3 JOIN " +
                    "IDN_OAUTH2_ACCESS_TOKEN AS T4 ON T3.AUTHZ_USER=T4.AUTHZ_USER AND T3.TENANT_ID=T4.TENANT_ID AND " +
                    "T3.USER_DOMAIN=T4.USER_DOMAIN)";

//    public static final String GET_RESOURCE_SET_FROM_ID =
//            "SELECT USER_RES_METADATA.RESOURCE_SET_ID,PROPERTY_KEY,PROPERTY_VALUE,SCOPE FROM " +
//                    "(SELECT USER_RESOURCES.RESOURCE_SET_ID,PROPERTY_KEY,PROPERTY_VALUE FROM " +
//                    "(SELECT RESOURCE_SET_ID FROM (SELECT TOKEN_ID FROM (SELECT AUTHZ_USER,TENANT_ID,USER_DOMAIN " +
//                    "FROM IDN_OAUTH2_ACCESS_TOKEN WHERE TOKEN_ID =?) AS T3 JOIN IDN_OAUTH2_ACCESS_TOKEN AS T4 ON " +
//                    "T3.AUTHZ_USER=T4.AUTHZ_USER AND T3.TENANT_ID=T4.TENANT_ID AND T3.USER_DOMAIN=T4.USER_DOMAIN) AS T1 " +
//                    "JOIN IDN_UMA_RESOURCE_SET AS T2 ON  T2.TOKEN_ID=T1.TOKEN_ID) AS USER_RESOURCES JOIN " +
//                    "IDN_UMA_RESOURCE_SET_METADATA AS METADATA ON USER_RESOURCES.RESOURCE_SET_ID=METADATA.RESOURCE_SET_ID) " +
//                    "AS USER_RES_METADATA JOIN IDN_UMA_RESOURCE_SCOPE_ASSOCIATION ON " +
//                    "IDN_UMA_RESOURCE_SCOPE_ASSOCIATION.RESOURCE_SET_ID = USER_RES_METADATA.RESOURCE_SET_ID WHERE " +
//                    "USER_RES_METADATA.RESOURCE_SET_ID = ?";


    public static final String GET_ALL_RESOURCE_SETS =
            "SELECT * FROM $resourceSetRegTable WHERE CONSUMER_KEY=?";

//    public static final String GET_RESOURCE_SET_IDS = "SELECT RESOURCE_SET_ID FROM " +
//            "(SELECT TOKEN_ID,T2.AUTHZ_USER,T2.TENANT_ID,T2.USER_DOMAIN  FROM (SELECT AUTHZ_USER,TENANT_ID,USER_DOMAIN " +
//            "FROM IDN_OAUTH2_ACCESS_TOKEN WHERE TOKEN_ID = ?) as T1 JOIN IDN_OAUTH2_ACCESS_TOKEN AS T2 ON " +
//            "T1.AUTHZ_USER=T2.AUTHZ_USER AND T1.TENANT_ID=T2.TENANT_ID AND T1.USER_DOMAIN=T2.USER_DOMAIN) AS T3 JOIN " +
//            "IDN_UMA_RESOURCE_SET as T4 ON T4.TOKEN_ID = T3.TOKEN_ID";

    public static final String GET_RESOURCE_SET_IDS =
            "SELECT RESOURCE_SET_ID FROM IDN_UMA_RESOURCE_SET WHERE TOKEN_ID IN (SELECT T4.TOKEN_ID FROM " +
                    "(SELECT * FROM IDN_OAUTH2_ACCESS_TOKEN WHERE TOKEN_ID=?) AS T3 JOIN IDN_OAUTH2_ACCESS_TOKEN AS T4 " +
                    "ON T3.AUTHZ_USER=T4.AUTHZ_USER AND T3.TENANT_ID=T4.TENANT_ID AND T3.USER_DOMAIN=T4.USER_DOMAIN)";


    private SQLQueries() {
    }
}
