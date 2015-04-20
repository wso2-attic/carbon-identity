/*
*  Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.user.account.connector.util;

public class UserAccountConnectorConstants {

    public static final String PRIMARY_USER_DOMAIN = "PRIMARY";

    public enum ErrorMessages {

        INVALID_INPUTS(8500, "Valid username and password must be provided"),
        ACCOUNT_CONNECTING_ERROR(8501, "Error occurred while associating the user account"),
        ACCOUNT_AUTHENTICATE_ERROR(8502, "Error occurred while trying to authenticate the new user account"),
        CONN_DELETE_DB_ERROR(8503, "Database error occurred while deleting the user account association"),
        CONN_CREATE_DB_ERROR(8504, "Database error occurred while creating the user account association"),
        DB_CONN_ERROR(8505, "Error occurred while getting the database connection"),
        CONN_LIST_DB_ERROR(8506, "Database error occurred while listing user account associations"),
        CONN_LIST_USER_STORE_ERROR(8507, "Error occurred while retrieving user domain while listing users"),
        CONN_LIST_ERROR(8508, "Error occurred while listing user account associations"),
        ERROR_IN_GET_TENANT_ID(8509, "Error occurred while getting the tenant id of the user"),
        INVALID_TENANT_DOMAIN(8510, "Invalid domain or inactivated tenant login"),
        ALREADY_CONNECTED(8511, "Provided user account is already associated to the logged in user"),
        USER_NOT_AUTHENTIC(8512, "The user name or password you entered is incorrect"),
        CONN_DELETE_ERROR(8513, "Error occurred while deleting the user account association"),
        INVALID_ASSOCIATION(8514, "User does not have valid association to proceed with this operation"),
        ERROR_RETRIEVE_REMOTE_ADDRESS(8515, "Error occurred while retrieving remote address from the request"),
        ACCOUNT_SWITCHING_ERROR(8516, "Error occurred while switching the user account"),
        CONN_SWITCH_DB_ERROR(8517, "Database error occurred while switching the user account"),
        SAME_ACCOUNT_CONNECTING_ERROR(85018, "Please select try to add account different to logged in user account"),;

        private final int code;
        private final String description;

        ErrorMessages(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public int getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return code + " - " + description;
        }

    }

    public static class SQLQueries {

        public static final String ADD_USER_ACCOUNT_ASSOCIATION = "INSERT INTO UM_USER_ACCOUNT_ASSOCIATIONS " +
                                                                  "(ASSOCIATION_KEY, DOMAIN_ID, TENANT_ID, " +
                                                                  "USER_NAME) SELECT ?, UM_DOMAIN_ID, ?, " +
                                                                  "? FROM UM_DOMAIN WHERE UM_DOMAIN_NAME = ? AND " +
                                                                  "UM_TENANT_ID = ?";

        public static final String GET_ASSOCIATION_KEY_OF_USER = "SELECT ASSOCIATION_KEY FROM " +
                                                                 "UM_USER_ACCOUNT_ASSOCIATIONS WHERE DOMAIN_ID = " +
                                                                 "(SELECT UM_DOMAIN_ID FROM UM_DOMAIN WHERE " +
                                                                 "UM_DOMAIN_NAME = ? AND UM_TENANT_ID= ?)  AND " +
                                                                 "TENANT_ID = ? AND USER_NAME = ?";

        public static final String LIST_USER_ACCOUNT_ASSOCIATIONS = "SELECT UM_DOMAIN_NAME, TENANT_ID, " +
                                                                    "USER_NAME FROM UM_DOMAIN JOIN " +
                                                                    "UM_USER_ACCOUNT_ASSOCIATIONS ON UM_DOMAIN" +
                                                                    ".UM_DOMAIN_ID = UM_USER_ACCOUNT_ASSOCIATIONS" +
                                                                    ".DOMAIN_ID WHERE UM_USER_ACCOUNT_ASSOCIATIONS" +
                                                                    ".ASSOCIATION_KEY = ?";

        public static final String DELETE_CONNECTION = "DELETE FROM UM_USER_ACCOUNT_ASSOCIATIONS WHERE DOMAIN_ID = " +
                                                       "(SELECT UM_DOMAIN_ID FROM UM_DOMAIN WHERE UM_DOMAIN_NAME = ? " +
                                                       "AND UM_TENANT_ID= ?) AND TENANT_ID = ? AND USER_NAME = ?";

        public static final String UPDATE_ASSOCIATION_KEY = "UPDATE UM_USER_ACCOUNT_ASSOCIATIONS SET ASSOCIATION_KEY " +
                                                            "= ? WHERE ASSOCIATION_KEY = ?";

        public static final String IS_VALID_ASSOCIATION = "SELECT COUNT(*) FROM UM_USER_ACCOUNT_ASSOCIATIONS WHERE " +
                                                          "DOMAIN_ID = (SELECT UM_DOMAIN_ID FROM UM_DOMAIN WHERE " +
                                                          "UM_DOMAIN_NAME = ? AND UM_TENANT_ID= ?) AND TENANT_ID = ? " +
                                                          "AND USER_NAME = ? AND ASSOCIATION_KEY = (SELECT  " +
                                                          "ASSOCIATION_KEY FROM UM_USER_ACCOUNT_ASSOCIATIONS WHERE " +
                                                          "DOMAIN_ID =  (SELECT UM_DOMAIN_ID FROM UM_DOMAIN WHERE " +
                                                          "UM_DOMAIN_NAME =  ? AND UM_TENANT_ID= ?) AND TENANT_ID = ?" +
                                                          " AND USER_NAME = ?)";

    }

}
