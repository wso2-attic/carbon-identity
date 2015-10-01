/*
 * Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.provider.openid.dao;

/**
 * SQL Queries required for the AssociationDAO.
 *
 * @author WSO2 Inc.
 */
public class OpenIDSQLQueries {

    /**
     * {@link OpenIDAssociationDAO}
     */
    public static final String CHECK_ASSOCIATION_ENTRY_EXIST = "SELECT 1 FROM IDN_OPENID_ASSOCIATIONS " +
                                                               "WHERE HANDLE = ?";

    public static final String LOAD_ASSOCIATION = "SELECT HANDLE, ASSOC_TYPE, EXPIRE_IN, MAC_KEY, ASSOC_STORE " +
                                                  "FROM IDN_OPENID_ASSOCIATIONS " + "WHERE HANDLE = ?";

    public static final String STORE_ASSOCIATION = "INSERT INTO " + "IDN_OPENID_ASSOCIATIONS " +
                                                   "(HANDLE, ASSOC_TYPE, EXPIRE_IN, MAC_KEY, ASSOC_STORE) " +
                                                   "VALUES (?,?,?,?,?)";

    public static final String REMOVE_ASSOCIATION = "DELETE " + "FROM IDN_OPENID_ASSOCIATIONS " +
                                                    "WHERE HANDLE = ?";

    /**
     * {@link OpenIDRememberMeTokenDAO}
     */
    public static final String CHECK_REMEMBER_ME_TOKEN_EXIST = "SELECT * " + "FROM IDN_OPENID_REMEMBER_ME " +
                                                               "WHERE USER_NAME = ? AND TENANT_ID = ?";

    public static final String STORE_REMEMBER_ME_TOKEN = "INSERT INTO " + "IDN_OPENID_REMEMBER_ME " +
                                                         "(USER_NAME, TENANT_ID, COOKIE_VALUE) " + "VALUES (?,?,?)";

    public static final String UPDATE_REMEMBER_ME_TOKEN = "UPDATE " + "IDN_OPENID_REMEMBER_ME " +
                                                          "SET COOKIE_VALUE = ?" +
                                                          " WHERE USER_NAME = ? AND TENANT_ID = ?";

    public static final String LOAD_REMEMBER_ME_TOKEN =
            "SELECT " + "USER_NAME, TENANT_ID, COOKIE_VALUE, CREATED_TIME " +
            "FROM IDN_OPENID_REMEMBER_ME " + "WHERE  USER_NAME = ? AND TENANT_ID = ?";

    private OpenIDSQLQueries() {
    }
}
