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

package org.wso2.carbon.identity.application.authenticator.samlsso.util;

public class SSOConstants {

    public static final String AUTHENTICATOR_NAME = "SAMLSSOAuthenticator";
    public static final String AUTHENTICATOR_FRIENDLY_NAME = "samlsso";

    public static final String HTTP_POST_PARAM_SAML2_AUTH_REQ = "SAMLRequest";
    public static final String HTTP_POST_PARAM_SAML2_RESP = "SAMLResponse";
    public static final String IDP_SESSION = "IdPSession";

    public static final String HTTP_POST = "POST";
    public static final String POST = "POST";
    public static final String REDIRECT = "REDIRECT";

	public static final String SERVLET_REQ_ATTR_AUTHENTICATION_CONTEXT = "authenticationContext";

    public static final String SP_NAME_QUALIFIER = "spNameQualifier";
    public static final String NAME_QUALIFIER = "nameQualifier";
    public static final String LOGOUT_USERNAME = "logoutUsername";
    public static final String LOGOUT_SESSION_INDEX = "logoutSessionIndex";

    public class StatusCodes {
        private StatusCodes() {

        }

        public static final String IDENTITY_PROVIDER_ERROR = "urn:oasis:names:tc:SAML:2.0:status:Responder";
        public static final String NO_PASSIVE = "urn:oasis:names:tc:SAML:2.0:status:NoPassive";
    }

    public class ServerConfig {
        private ServerConfig() {

        }

        public static final String KEY_ALIAS = "Security.KeyStore.KeyAlias";
        public static final String KEY_PASSWORD = "Security.KeyStore.KeyPassword";
        public static final String SAML2_SSO_MANAGER = "SAML2SSOManager";
        public static final String SAML_SSO_ACS_URL = "SAMLSSOAssertionConsumerUrl";
    }
}
