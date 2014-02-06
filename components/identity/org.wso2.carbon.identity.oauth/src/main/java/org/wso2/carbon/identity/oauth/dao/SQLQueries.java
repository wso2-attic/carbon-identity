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

package org.wso2.carbon.identity.oauth.dao;

/**
 * SQL queries related to OAuth data access layer.
 */
public class SQLQueries {

    public static class OAuthAppDAOSQLQueries {

        public static final String ADD_OAUTH_APP = "INSERT INTO IDN_OAUTH_CONSUMER_APPS " +
                "(CONSUMER_KEY, CONSUMER_SECRET, USERNAME, TENANT_ID, APP_NAME, OAUTH_VERSION, CALLBACK_URL, GRANT_TYPES) VALUES (?,?,?,?,?,?,?,?) ";

        public static final String ADD_OAUTH_CONSUMER = "INSERT INTO IDN_OAUTH_CONSUMER_APPS " +
                "(CONSUMER_KEY, CONSUMER_SECRET, USERNAME, TENANT_ID, OAUTH_VERSION) VALUES (?,?,?,?,?) ";

        public static final String UPDATE_OAUTH_CONSUMER = "UPDATE IDN_OAUTH_CONSUMER_APPS " +
                "SET CONSUMER_SECRET=? WHERE CONSUMER_KEY=? AND USERNAME=? AND TENANT_ID=?";
        
        public static final String GET_APPS_OF_USER = "SELECT CONSUMER_KEY, CONSUMER_SECRET, APP_NAME, OAUTH_VERSION, CALLBACK_URL, GRANT_TYPES FROM " +
                "IDN_OAUTH_CONSUMER_APPS WHERE USERNAME=? AND TENANT_ID=?";

        public static final String GET_APP_INFO = "SELECT CONSUMER_SECRET,USERNAME,APP_NAME, OAUTH_VERSION, CALLBACK_URL,TENANT_ID, GRANT_TYPES FROM " +
        "IDN_OAUTH_CONSUMER_APPS WHERE CONSUMER_KEY=? ";

        public static final String UPDATE_CONSUMER_APP = "UPDATE IDN_OAUTH_CONSUMER_APPS SET APP_NAME=?, CALLBACK_URL=?, GRANT_TYPES=? " +
                "WHERE CONSUMER_KEY=? AND CONSUMER_SECRET=?";

        public static final String CHECK_EXISTING_APPLICATION = "SELECT * FROM IDN_OAUTH_CONSUMER_APPS " +
                "WHERE USERNAME=? AND TENANT_ID=? AND APP_NAME=?";

        public static final String CHECK_EXISTING_CONSUMER = "SELECT * FROM IDN_OAUTH_CONSUMER_APPS " +
                "WHERE CONSUMER_KEY=?";

        public static final String GET_APP_NAME = "SELECT APP_NAME FROM IDN_OAUTH_CONSUMER_APPS WHERE CONSUMER_KEY=?";

        public static final String REMOVE_APPLICATION = "DELETE FROM IDN_OAUTH_CONSUMER_APPS WHERE CONSUMER_KEY=?";
    }

    public static class OAuthConsumerDAOSQLQueries {
        public static final String GET_CONSUMER_SECRET = "SELECT CONSUMER_SECRET FROM IDN_OAUTH_CONSUMER_APPS " +
                "WHERE CONSUMER_KEY=?";

        public static final String GET_REGISTERED_CALLBACK_URL = "SELECT CALLBACK_URL FROM IDN_OAUTH_CONSUMER_APPS WHERE CONSUMER_KEY=?";

        public static final String ADD_OAUTH_REQ_TOKEN = "INSERT INTO IDN_OAUTH1A_REQUEST_TOKEN (REQUEST_TOKEN, REQUEST_TOKEN_SECRET, CONSUMER_KEY, " +
                "CALLBACK_URL, SCOPE, AUTHORIZED) VALUES (?,?,?,?,?,?)";

        public static final String GET_CALLBACK_URL_OF_REQ_TOKEN = "SELECT CALLBACK_URL FROM IDN_OAUTH1A_REQUEST_TOKEN WHERE REQUEST_TOKEN=?";

        public static final String AUTHORIZE_REQ_TOKEN = "UPDATE IDN_OAUTH1A_REQUEST_TOKEN SET AUTHORIZED=?, OAUTH_VERIFIER=?, AUTHZ_USER=? " +
                "WHERE REQUEST_TOKEN=?";

        public static final String GET_REQ_TOKEN = "SELECT * FROM IDN_OAUTH1A_REQUEST_TOKEN WHERE REQUEST_TOKEN=?";

        public static final String GET_REQ_TOKEN_SECRET = "SELECT REQUEST_TOKEN_SECRET FROM IDN_OAUTH1A_REQUEST_TOKEN WHERE REQUEST_TOKEN=?";

        public static final String REMOVE_REQUEST_TOKEN = "DELETE FROM IDN_OAUTH1A_REQUEST_TOKEN WHERE REQUEST_TOKEN=?";

        public static final String ADD_ACCESS_TOKEN = "INSERT INTO IDN_OAUTH1A_ACCESS_TOKEN VALUES (?,?,?,?,?)";
        
        public static final String GET_ACCESS_TOKEN = "SELECT SCOPE, AUTHZ_USER FROM IDN_OAUTH1A_ACCESS_TOKEN " +
                "WHERE ACCESS_TOKEN=?";

        public static final String GET_ACCESS_TOKEN_SECRET = "SELECT ACCESS_TOKEN_SECRET FROM IDN_OAUTH1A_ACCESS_TOKEN " +
                "WHERE ACCESS_TOKEN=?";

        public static final String GET_CONSUMER_KEY_FOR_TOKEN = "SELECT CONSUMER_KEY, SCOPE FROM IDN_OAUTH1A_REQUEST_TOKEN WHERE REQUEST_TOKEN=?";

        // Get the username corresponding to the given consumer key and secret
        public static final String GET_USERNAME_FOR_KEY_AND_SECRET = "SELECT USERNAME FROM IDN_OAUTH_CONSUMER_APPS WHERE CONSUMER_KEY=? AND CONSUMER_SECRET=?";
        
    }
}
