/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.application.authenticator.oidc;

public class OIDCAuthenticatorConstants {
    private OIDCAuthenticatorConstants() {
    }

    public static final String AUTHENTICATOR_NAME = "OpenIDConnectAuthenticator";
    public static final String LOGIN_TYPE = "OIDC";

    public static final String OAUTH_OIDC_SCOPE = "openid";
    public static final String OAUTH2_GRANT_TYPE_CODE = "code";
    public static final String OAUTH2_PARAM_STATE = "state";
    public static final String OAUTH2_ERROR = "error";

    public static final String ACCESS_TOKEN = "access_token";
    public static final String REFRESH_TOKEN = "refresh_token";
    public static final String ID_TOKEN = "id_token";

    public static final String CLIENT_ID = "ClientId";
    public static final String CLIENT_SECRET = "ClientSecret";
    public static final String OAUTH2_AUTHZ_URL = "OAuth2AuthzEPUrl";
    public static final String OAUTH2_TOKEN_URL = "OAuth2TokenEPUrl";

    public class AuthenticatorConfParams {
        private AuthenticatorConfParams() {
        }

        public static final String DEFAULT_IDP_CONFIG = "DefaultIdPConfig";
    }

    public class IdPConfParams {
        private IdPConfParams() {
        }

        public static final String CLIENT_ID = "ClientId";
        public static final String CLIENT_SECRET = "ClientSecret";
        public static final String AUTHORIZATION_EP = "AuthorizationEndPoint";
        public static final String TOKEN_EP = "TokenEndPoint";
        public static final String USER_INFO_EP = "UserInfoEndPoint";
    }

    public class Claim {
        private Claim() {
        }

        public static final String SUB = "sub";
        public static final String NAME = "name";
        public static final String GIVEN_NAME = "given_name";
        public static final String FAMILY_NAME = "family_name";
        public static final String MIDDLE_NAME = "middle_name";
        public static final String NICK_NAME = "nickname";
        public static final String PREFERED_USERNAME = "preferred_username";
        public static final String PROFILE = "profile";
        public static final String PICTURE = "picture";
        public static final String WEBSITE = "website";
        public static final String EMAIL = "email";
        public static final String EMAIL_VERIFIED = "email_verified";
        public static final String GENDER = "gender";
        public static final String BIRTH_DATE = "birthdate";
        public static final String ZONE_INFO = "zoneinfo";
        public static final String LOCALE = "locale";
        public static final String PHONE_NUMBER = "phone_number";
        public static final String PHONE_NUMBER_VERIFIED = "phone_number_verified";
        public static final String ADDRESS = "address";
        public static final String UPDATED_AT = "updated_at";
    }
}