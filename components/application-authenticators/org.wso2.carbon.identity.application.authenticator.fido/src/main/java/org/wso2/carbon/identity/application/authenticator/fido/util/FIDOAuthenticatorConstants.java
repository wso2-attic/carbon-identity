/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.application.authenticator.fido.util;

/**
 * Utils class for FIDO Authenticator Constants.
 */
public class FIDOAuthenticatorConstants {
    private FIDOAuthenticatorConstants() {
    }

    public static final String AUTHENTICATOR_NAME = "FIDOAuthenticator";
    public static final String AUTHENTICATOR_FRIENDLY_NAME = "fido";
    public static final String UNUSED = "unused";
    public static final String AUTHENTICATION_STATUS = "Authentication Failed !";
    public static final String AUTHENTICATION_ERROR_MESSAGE = "No registered device found, Please register your device before sign in.";

    public static final String U2F_KEY_HANDLE = "KEY_HANDLE";
    public static final String U2F_DEVICE_DATA = "DEVICE_DATA";
    public static final String U2F_DEVICE_METADATA = "TIME_REGISTERED";

    public static class SQLQueries {
        private SQLQueries() {
        }

        public static final String ADD_DEVICE_REGISTRATION_QUERY = "INSERT INTO FIDO_DEVICE_STORE (TENANT_ID, DOMAIN_NAME," +
                                                                   " USER_NAME, TIME_REGISTERED, KEY_HANDLE, DEVICE_DATA ) VALUES (?, ?, ?, ?, ?, ?)";
        public static final String GET_DEVICE_REGISTRATION_QUERY = "SELECT * FROM FIDO_DEVICE_STORE WHERE TENANT_ID = ? " +
                                                                   "AND DOMAIN_NAME = ? AND USER_NAME = ?";
        public static final String REMOVE_ALL_REGISTRATION_QUERY = "DELETE FROM FIDO_DEVICE_STORE WHERE TENANT_ID = ? " +
                                                               "AND DOMAIN_NAME = ? AND USER_NAME = ?";
        public static final String REMOVE_REGISTRATION_QUERY = "DELETE FROM FIDO_DEVICE_STORE WHERE TENANT_ID = ? " +
                                                                   "AND DOMAIN_NAME = ? AND USER_NAME = ? AND TIME_REGISTERED = ?";
        public static final String UPDATE_USER_DOMAIN_NAME = "UPDATE FIDO_DEVICE_STORE SET DOMAIN_NAME = ?" +
                                                             " WHERE DOMAIN_NAME = ? AND TENANT_ID = ?";

        public static final String DELETE_DEVICE_REGISTRATION_FROM_DOMAIN = "DELETE FROM FIDO_DEVICE_STORE " +
                                                                         "WHERE TENANT_ID = ? AND DOMAIN_NAME = ?";
    }
}

