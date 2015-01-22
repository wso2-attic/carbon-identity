/*
 *Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.application.common.util;

public class IdentityApplicationConstants {

    public static final String APPLICATION_AUTHENTICATION_CONGIG = "application-authentication.xml";
    public static final String APPLICATION_AUTHENTICATION_DEFAULT_NAMESPACE =
            "http://wso2.org/projects/carbon/application-authentication.xml";
    public static final String RESIDENT_IDP_RESERVED_NAME = "LOCAL";
    public static final String DEFAULT_SP_CONFIG = "default";
    public static final String DEFAULT_IDP_CONFIG = "default";

    public static final String WSO2CARBON_CLAIM_DIALECT = "http://wso2.org/claims";
    public static final String SF_OAUTH2_TOKEN_ENDPOINT = "https://login.salesforce.com/services/oauth2/token";


    public static class ConfigElements {
        public static final String PROPERTIES = "Properties";
        public static final String PROPERTY = "Property";
        public static final String ATTR_NAME = "name";
        public static final String ATTR_ENABLED = "enabled";
        public static final String PROPERTY_TYPE_STRING = "STRING";
        public static final String PROPERTY_TYPE_BLOB = "BLOB";
    }

    public static class Authenticator {

        public static class OpenID {

            public static final String NAME = "openid";
            public static final String REALM_ID = "RealmId";
            public static final String OPEN_ID_URL = "OpenIdUrl";
            public static final String IS_USER_ID_IN_CLAIMS = "IsUserIdInClaims";
        }

        public static class SAML2SSO {

            public static final String NAME = "samlsso";
            public static final String FED_AUTH_NAME = "SAMLSSOAuthenticator";
            public static final String IDP_ENTITY_ID = "IdPEntityId";
            public static final String SP_ENTITY_ID = "SPEntityId";
            public static final String SSO_URL = "SSOUrl";
            public static final String IS_AUTHN_REQ_SIGNED = "ISAuthnReqSigned";
            public static final String IS_ENABLE_ASSERTION_ENCRYPTION = "IsAssertionEncrypted";
            public static final String IS_ENABLE_ASSERTION_SIGNING = "isAssertionSigned";
            public static final String IS_LOGOUT_ENABLED = "IsLogoutEnabled";
            public static final String LOGOUT_REQ_URL = "LogoutReqUrl";
            public static final String IS_LOGOUT_REQ_SIGNED = "IsLogoutReqSigned";
            public static final String IS_AUTHN_RESP_SIGNED = "IsAuthnRespSigned";
            public static final String IS_USER_ID_IN_CLAIMS = "IsUserIdInClaims";
        }

        public static class OIDC extends OAuth2 {

            public static final String NAME = "openidconnect";
            public static final String USER_INFO_URL = "UserInfoUrl";
            public static final String IS_USER_ID_IN_CLAIMS = "IsUserIdInClaims";
        }

        public static class PassiveSTS {

            public static final String NAME = "passivests";
            public static final String REALM_ID = "RealmId";
            public static final String PASSIVE_STS_URL = "PassiveSTSUrl";
            public static final String IS_USER_ID_IN_CLAIMS = "IsUserIdInClaims";
        }

        public static class Facebook {

            public static final String NAME = "facebook";
            public static final String CLIENT_ID = "ClientId";
            public static final String CLIENT_SECRET = "ClientSecret";
        }

        public static class WSTrust {
            public static final String NAME = "wstrust";
        }

    }

    public static class OAuth10A {

        public static final String CONSUMER_KEY = "ConsumerKey";
        public static final String CONSUMER_SECRET = "ConsumerSecret";
        public static final String OAUTH10A_REQ_URL = "OAuth10AReqTokenUrl";
        public static final String OAUTH10A_AUTHZ_URL = "OAuth10AAuthzTokenUrl";
        public static final String OAUTH10A_ACCESS_URL = "OAuth10AAccessTokenUrl";
    }

    public static class OAuth2 {

        public static final String NAME = "oauth2";
        public static final String CLIENT_ID = "ClientId";
        public static final String CLIENT_SECRET = "ClientSecret";
        public static final String OAUTH2_AUTHZ_URL = "OAuth2AuthzUrl";
        public static final String OAUTH2_TOKEN_URL = "OAUTH2TokenUrl";
    }

}
