/*
 *Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */
package org.wso2.carbon.identity.application.mgt;

public class DBQueries {

    public static String STORE_BASIC_APPINFO = "INSERT INTO IDN_APPMGT_APP "
            + "(APP_ID, TENANT_ID, USERNAME) VALUES (?,?,?) ";

    public static String STORE_CLIENT_INFO = "INSERT INTO IDN_APPMGT_CLIENT "
            + "(APP_ID, CLIENT_ID, CLIENT_SECRETE, CALLBACK_URL, TYPE) VALUES (?,?,?,?,?)";

    public static String STORE_STEP_INFO = "INSERT INTO IDN_APPMGT_STEP "
            + "(APP_ID, STEP_ID, AUTHN_ID, IDP_ID, ENDPOINT, TYPE) VALUES (?,?,?,?,?,?)";

    public static String GAT_BASIC_APP_INFO = "SELECT USERNAME " +
            "FROM IDN_APPMGT_APP " +
            "WHERE APP_ID = ? ADN TENANT_ID = ?";

    public static String GET_CLIENT_INFO = "SELECT CLIENT_ID, CLIENT_SECRETE, CALLBACK_URL, TYPE " +
            "FROM IDN_APPMGT_CLIENT " +
            "WHERE APP_ID = ? ";

    public static String GET_STEP_INFO = "SELECT STEP_ID, AUTHN_ID, IDP_ID, ENDPOINT, TYPE " +
            "FROM IDN_APPMGT_STEP " +
            "WHERE APP_ID = ? ";

    public static String GET_ALL_APP = "SELECT APP_ID FROM IDN_APPMGT_APP WHERE TENANT_ID = ?";

    public static String GET_APP_ID = "SELECT APP_ID FROM IDN_APPMGT_CLIENT WHERE CLIENT_ID = ? AND TYPE = ?";

    public static String REMOVE_APP_FROM_APPMGT_APP = "DELETE FROM IDN_APPMGT_APP WHERE APP_ID = ? ";
    public static String REMOVE_APP_FROM_APPMGT_CLIENT = "DELETE FROM IDN_APPMGT_CLIENT WHERE APP_ID = ? ";
    public static String REMOVE_APP_FROM_APPMGT_STEP = "DELETE FROM IDN_APPMGT_STEP WHERE APP_ID = ? ";



}


// "DELETE FROM IDN_OAUTH2_ACCESS_TOKEN WHERE ACCESS_TOKEN = ? ";