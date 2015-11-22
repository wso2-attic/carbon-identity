/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.provisioning.connector.salesforce;

public class SalesforceConnectorConstants {

    public static final String Salesforce_Connector_IdP = "Salesforce.Connector.IdP";

    public static final String OAUTH2_TOKEN_ENDPOINT = "https://login.salesforce.com/services/oauth2/token";
    public static final String AUTHORIZATION_HEADER_NAME = "Authorization";
    public static final String AUTHORIZATION_HEADER_OAUTH = "OAuth";

    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_SECRET = "client_secret";
    public static final String GRANT_TYPE = "grant_type";
    public static final String GRANT_TYPE_PASSWORD = "password";
    public static final String USERNAME = "username";
    public static final String USERNAME_ATTRIBUTE = "Username";
    public static final String PASSWORD = "password";


    public static final String IS_ACTIVE = "IsActive";
    public static final String PROFILE_ID = "ProfileId";
    public static final String USE_ROLES = "roles";
    public static final String USE_USERNAME = "USE_USERNAME";
    public static final String USE_PASSWORD = "USE_PASSWORD";


    public static final String CONTEXT_SERVICES_DATA = "/services/data/";
    public static final String CONTEXT_SOOBJECTS_USER = "/sobjects/user/";
    // Shouldnt contain slash at the end
    public static final String CONTEXT_QUERY = "/query";

    public static final String CONTENT_TYPE_APPLICATION_JSON = "application/json";

    public static final String SALESFORCE_SERVICES_DATA = "/services/data/";
    public static final String SALESFORCE_ENDPOINT_QUERY = "/query";
    public static final String SALESFORCE_OLD_USERNAME_PREFIX = "old";

    public class PropertyConfig {

        public static final String IDP_NAME = "Identity.Provisioning.Connector.Salesforce.IdP.Name";
        public static final String DOMAIN_NAME = "sf-domain-name";
        public static final String API_VERSION = "sf-api-version";
        public static final String USER_ID_CLAIM = "Identity.Provisioning.Connector.Salesforce.UserID.Claim";

        public static final String FORCE_PROVISIONING_AT_USER_CREATION_ENABLED = "Identity.Provisioning.Connector.Salesforce.ForceProvisioningAtUserCreationEnabled";
        public static final String IDENTITY_PROVISIONING_CONNECTOR = "Identity.Provisioning.Connector.Salesforce.Domain.Name";

        public static final String REQUIRED_FIELDS = "Identity.Provisioning.Connector.Salesforce.Required.Fields";
        public static final String REQUIRED_CLAIM_PREFIX = "Identity.Provisioning.Connector.Salesforce.Required.Field.Claim.";
        public static final String REQUIRED_DEFAULT_PREFIX = "Identity.Provisioning.Connector.Salesforce.Required.Field.Default.";

        public static final String CLIENT_ID = "sf-clientid";
        public static final String CLIENT_SECRET = "sf-client-secret";
        public static final String GRANT_TYPE = "grant_type";
        public static final String USERNAME = "sf-username";
        public static final String PASSWORD = "sf-password";
        public static final String OAUTH2_TOKEN_ENDPOINT = "sf-token-endpoint";

        private PropertyConfig(){}

    }

}
