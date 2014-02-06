package org.wso2.carbon.user.cassandra;

/**
 *   Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

public class CFConstants {
    public static final String KEYSPACE = "TN_KS";
    public static final String ROLES = "ROLES";
    public static final String USERS = "USERS";
    public static final String USERNAME_INDEX = "USERNAME_INDEX";
    public static final String PASSWORD_INDEX = "PASSWORD_INDEX";
    public static final String USER_ID = "userId";
    static final String USERNAME_ROLES_INDEX = "USERNAME_TO_ROLES_INDEX";
    public static final String KEYSPACE_NAME_XML_ATTRIB = "Keyspace";
    public static final String HOST_XML_ATTRIB = "Host";
    public static final String PORT_XML_ATTRIB = "Port";
    public static final String IS_ACTIVE = "isActive";
    public static final String DEFAULT_TYPE = "Default";
    public static final String DEVICE_TYPE = "Device";
    public static final String SALT_VALUE = "SaltValue";
    public static final String SECRET = "secret";
    public static String AUTH_WITH_ANY_CREDENTIAL = "AuthenticateWithAnyCredential";
    public static final String CLAIMS = "CLAIMS";
}
