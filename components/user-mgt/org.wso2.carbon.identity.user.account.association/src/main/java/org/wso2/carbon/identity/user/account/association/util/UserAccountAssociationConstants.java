/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.user.account.association.util;

public class UserAccountAssociationConstants {


    public static final String PRIMARY_USER_DOMAIN = "PRIMARY";
    public static final String LOGIN_PERMISSION = "/permission/admin/login";

    private UserAccountAssociationConstants(){

    }

    public enum ErrorMessages {

        INVALID_INPUTS(8500, "Valid username and password must be provided"),
        ACCOUNT_CONNECTING_ERROR(8501, "Error occurred while associating the user account"),
        ACCOUNT_AUTHENTICATE_ERROR(8502, "Error occurred while trying to authenticate the new user account"),
        CONN_DELETE_DB_ERROR(8503, "Database error occurred while deleting user account association"),
        CONN_CREATE_DB_ERROR(8504, "Database error occurred while creating user account association"),
        CONN_UPDATE_DB_ERROR(8505, "Database error occurred while updating user account association"),
        CHECK_ASSOCIATION_DB_ERROR(8506, "Database error occurred while validating user association"),
        ASSOCIATIONS_DELETE_DB_ERROR(8507, "Database error occurred while deleting user account associations"),
        ERROR_WHILE_LOADING_REALM_SERVICE(8508, "Error occurred while getting the RealmService"),
        ERROR_WHILE_ACCESSING_REALM_SERVICE(8509, "Error occurred while accessing the RealmService"),
        ERROR_WHILE_RETRIEVING_ASSOC_KEY(8510, "Error occurred while getting the RealmService"),
        ERROR_WHILE_GETTING_TENANT_NAME(8511, "Error occurred while getting tenant name from tenant id"),
        ERROR_WHILE_GETTING_TENANT_ID(8512, "Error occurred while getting tenant id from tenant name"),
        ERROR_WHILE_AUTHENTICATING_USER(8513, "Error occurred while authenticating user"),
        ERROR_WHILE_DELETING_USER_ASSOC(8514, "Error occurred while deleting user account association for user %s"),
        ERROR_WHILE_RETRIEVING_REMOTE_ADDRESS(8515, "Error occurred while retrieving remote address"),
        ERROR_WHILE_UPDATING_SESSION(8516, "Error occurred while updating session parameters"),
        ERROR_WHILE_EXECUTING_AUTHENTICATORS(8517, "Error occurred while executing pre/post user authenticators"),
        DB_CONN_ERROR(8518, "Error occurred while getting the database connection"),
        CONN_LIST_DB_ERROR(8519, "Database error occurred while listing user account associations"),
        CONN_LIST_USER_STORE_ERROR(8520, "Error occurred while retrieving user domain while listing users"),
        CONN_LIST_ERROR(8521, "Error occurred while listing user account associations"),
        ERROR_IN_GET_TENANT_ID(8522, "Error occurred while getting the tenant id of the user"),
        DEBUG_INVALID_TENANT_DOMAIN(8523, "Invalid or inactivated tenant domain '%s'"),
        INVALID_TENANT_DOMAIN(8524, "Invalid or inactivated tenant domain"),
        ALREADY_CONNECTED(8525, "Provided user account is already associated to the logged in user"),
        USER_NOT_AUTHENTIC(8526, "The user name or password you entered is incorrect"),
        CONN_DELETE_ERROR(8527, "Error occurred while deleting the user account association"),
        CONN_DELETE_FROM_TENANT_ID_ERROR(8528, "Error occurred while deleting the user account associations for " +
                                               "tenant id %s"),
        INVALID_ASSOCIATION(8529, "User does not have valid association to proceed with this operation"),
        ERROR_RETRIEVE_REMOTE_ADDRESS(8530, "Error occurred while retrieving remote address from the request"),
        ACCOUNT_SWITCHING_ERROR(8531, "Error occurred while switching the user account"),
        CONN_SWITCH_DB_ERROR(8532, "Database error occurred while switching the user account"),
        SAME_ACCOUNT_CONNECTING_ERROR(8533, "User can not associate logged in user account to itself"),
        ERROR_UPDATE_DOMAIN_NAME(8534, "Database error occurred while updating user domain '%s' in the tenant " +
                                             "'%s'"),
        ERROR_DELETE_ASSOC_FROM_DOMAIN_NAME(8535, "Database error occurred while deleting user association from " +
                                                  "domain '%s' in the tenant '%s'"),
        ERROR_WHILE_UPDATING_ASSOC_DOMAIN(8536, "Error occurred while updating user domain of account associations" +
                                                " with domain '%s'"),
        ERROR_WHILE_DELETING_ASSOC_FROM_DOMAIN(8537, "Error occurred while deleting user account associations with " +
                                                     "domain '%s'");

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

        public static final String ADD_USER_ACCOUNT_ASSOCIATION = "INSERT INTO IDN_USER_ACCOUNT_ASSOCIATION " +
                                                                  "(ASSOCIATION_KEY, TENANT_ID, DOMAIN_NAME, " +
                                                                  "USER_NAME) VALUES (?, ?, ?, ?)";

        public static final String GET_ASSOCIATION_KEY_OF_USER = "SELECT ASSOCIATION_KEY FROM " +
                                                                 "IDN_USER_ACCOUNT_ASSOCIATION WHERE TENANT_ID = ? " +
                                                                 "AND DOMAIN_NAME = ? AND USER_NAME = ?";

        public static final String LIST_USER_ACCOUNT_ASSOCIATIONS = "SELECT TENANT_ID, DOMAIN_NAME, " +
                                                                    "USER_NAME FROM IDN_USER_ACCOUNT_ASSOCIATION " +
                                                                    "WHERE ASSOCIATION_KEY = ?";

        public static final String DELETE_CONNECTION = "DELETE FROM IDN_USER_ACCOUNT_ASSOCIATION WHERE TENANT_ID = ? " +
                                                       "AND DOMAIN_NAME = ? AND USER_NAME = ?";

        public static final String DELETE_CONNECTION_FROM_TENANT_ID = "DELETE FROM IDN_USER_ACCOUNT_ASSOCIATION WHERE" +
                                                                      " TENANT_ID = ?";

        public static final String UPDATE_ASSOCIATION_KEY = "UPDATE IDN_USER_ACCOUNT_ASSOCIATION SET ASSOCIATION_KEY " +
                                                            "= ? WHERE ASSOCIATION_KEY = ?";

        public static final String IS_VALID_ASSOCIATION = "SELECT COUNT(*) FROM IDN_USER_ACCOUNT_ASSOCIATION WHERE " +
                                                          "TENANT_ID = ? AND DOMAIN_NAME = ? AND USER_NAME = ? AND " +
                                                          "ASSOCIATION_KEY = (SELECT ASSOCIATION_KEY FROM " +
                                                          "IDN_USER_ACCOUNT_ASSOCIATION WHERE TENANT_ID = ? AND " +
                                                          "DOMAIN_NAME = ? AND USER_NAME = ?)";

        public static final String UPDATE_USER_DOMAIN_NAME = "UPDATE IDN_USER_ACCOUNT_ASSOCIATION SET DOMAIN_NAME = ?" +
                                                             " WHERE DOMAIN_NAME = ? AND TENANT_ID = ?";

        public static final String DELETE_USER_ASSOCIATION_FROM_DOMAIN = "DELETE FROM IDN_USER_ACCOUNT_ASSOCIATION " +
                                                                         "WHERE TENANT_ID = ? AND DOMAIN_NAME = ?";

    }

}
