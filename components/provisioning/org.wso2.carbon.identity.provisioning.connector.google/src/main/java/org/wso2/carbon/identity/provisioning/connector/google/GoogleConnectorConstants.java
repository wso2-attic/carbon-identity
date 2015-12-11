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

package org.wso2.carbon.identity.provisioning.connector.google;

public class GoogleConnectorConstants {

    public static final String ATTRIBUTE_FAMILYNAME = "familyName";
    public static final String ATTRIBUTE_GIVENNAME = "givenName";
    public static final String ATTRIBUTE_PASSWORD = "password";
    public static final String ATTRIBUTE_PRIMARYEMAIL = "primaryEmail";

    public static final String PRIVATE_KEY = "google_prov_private_key";

    public class PropertyConfig {

        public static final String IDP_NAME = "Identity.Provisioning.Connector.Google.IdP";
        public static final String DOMAIN_NAME = "Identity.Provisioning.Connector.Google.Domain.Name";
        public static final String ROLE_NAME = "Identity.Provisioning.Connector.Google.Role.Name";

        public static final String REQUIRED_FIELDS = "Identity.Provisioning.Connector.Google.Required.Fields";
        public static final String REQUIRED_CLAIM_PREFIX = "Identity.Provisioning.Connector.Google.Required.Field.Claim.";
        public static final String REQUIRED_DEFAULT_PREFIX = "Identity.Provisioning.Connector.Google.Required.Field.Default.";

        public static final String USER_ID_CLAIM = "Identity.Provisioning.Connector.Google.UserID.Claim";

        public static final String SERVICE_ACCOUNT_EMAIL = "Identity.Provisioning.Connector.Google.ServiceAccountEmail";
        public static final String ADMIN_EMAIL = "Identity.Provisioning.Connector.Google.AdminEmail";
        public static final String SERVICE_ACCOUNT_PKCS12_FILE_PATH = "Identity.Provisioning.Connector.Google.ServiceAccountPKCS12FilePath";
        public static final String APPLICATION_NAME = "Identity.Provisioning.Connector.Google.ApplicationName";

        public static final String DEFAULT_PROVISIONING_PATTERN = "{UN}";
        public static final String DEFAULT_PROVISIONING_SEPERATOR = "_";

        private PropertyConfig(){}
    }
}