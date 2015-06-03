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
    private FIDOAuthenticatorConstants(){}

	public static final String AUTHENTICATOR_NAME = "FIDOAuthenticator";
	public static final String AUTHENTICATOR_FRIENDLY_NAME = "fido";
	public static final String UNUSED = "unused";
    public static final String AUTHENTICATION_STATUS = "Authentication Failed !";
    public static final String AUTHENTICATION_ERROR_MESSAGE = "No registered device found, Please register your device before sign in.";
    public static final String PRIMARY_USER_DOMAIN = "PRIMARY";

    public static final String U2F_KEY_HANDLE = "KEY_HANDLE";
    public static final String U2F_DEVICE_DATA = "DEVICE_DATA";
    public static final String UTF_8 = "UTF-8";

    public static class SQLQueries {
        private SQLQueries(){}
        public static final String ADD_DEVICE_REGISTRATION_QUERY = "INSERT INTO FIDO_DEVICE_STORE (TENANT_ID, DOMAIN_ID," +
                " USER_NAME, KEY_HANDLE, DEVICE_DATA ) SELECT ?, UM_DOMAIN_ID, ?,?,? FROM UM_DOMAIN WHERE UM_DOMAIN_NAME = ? " +
                "AND UM_TENANT_ID = ?";
        public static final String GET_DEVICE_REGISTRATION_QUERY = "SELECT * FROM FIDO_DEVICE_STORE WHERE DOMAIN_ID = (SELECT " +
                "UM_DOMAIN_ID FROM UM_DOMAIN WHERE UM_DOMAIN_NAME = ? AND UM_TENANT_ID= ?)  AND TENANT_ID = ? AND USER_NAME = ?";
    }
}

