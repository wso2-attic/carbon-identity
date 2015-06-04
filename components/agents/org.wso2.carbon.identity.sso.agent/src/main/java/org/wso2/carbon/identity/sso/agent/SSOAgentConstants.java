/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 *
 */

package org.wso2.carbon.identity.sso.agent;

public class SSOAgentConstants {

    public static final String LOGGER_NAME = "org.wso2.carbon.identity.sso.agent";



    public static final String SESSION_BEAN_NAME =

            "org.wso2.carbon.identity.sso.agent.LoggedInSessionBean";
    public static final String CONFIG_BEAN_NAME = "org.wso2.carbon.identity.sso.agent.SSOAgentConfig";


    public static class SAML2SSO {

        private SAML2SSO() {
        }

        public static final String HTTP_POST_PARAM_SAML2_AUTH_REQ = "SAMLRequest";
        public static final String HTTP_POST_PARAM_SAML2_RESP = "SAMLResponse";
    }

    public static class OpenID {

        public static final String OPENID_MODE = "openid.mode";
        private OpenID() {
        }

    }

    public static class OAuth2 {
        public static final String SAML2_BEARER_GRANT_TYPE =
                "urn:ietf:params:oauth:grant-type:saml2-bearer";

        private OAuth2() {
        }
    }

    private SSOAgentConstants() {
    }

    public static class SSOAgentConfig {

        public static final String ENABLE_SAML2_SSO_LOGIN = "EnableSAML2SSOLogin";
        public static final String ENABLE_OPENID_SSO_LOGIN = "EnableOpenIDLogin";
        public static final String ENABLE_OAUTH2_SAML2_OAUTH2_GRANT = "EnableOAuth2SAML2Grant";
        public static final String SAML2_SSO_URL = "SAML2SSOURL";
        public static final String OPENID_URL = "OpenIdURL";
        public static final String OAUTH2_SAML2_GRANT_URL = "OAuth2SAML2GrantURL";
        public static final String SKIP_URIS = "SkipURIs";
        public static final String QUERY_PARAMS = "QueryParams";

        private SSOAgentConfig() {
        }

        public static class SAML2 {

            public static final String HTTP_BINDING = "SAML2.HTTPBinding";
            public static final String SP_ENTITY_ID = "SAML2.SPEntityId";
            public static final String ACS_URL = "SAML2.AssertionConsumerURL";
            public static final String IDP_ENTITY_ID = "SAML2.IdPEntityId";
            public static final String IDP_URL = "SAML2.IdPURL";
            public static final String ATTRIBUTE_CONSUMING_SERVICE_INDEX =
                    "SAML2.AttributeConsumingServiceIndex";
            public static final String ENABLE_SLO = "SAML2.EnableSLO";
            public static final String SLO_URL = "SAML2.SLOURL";
            public static final String ENABLE_ASSERTION_SIGNING =
                    "SAML2.EnableAssertionSigning";
            public static final String ENABLE_ASSERTION_ENCRYPTION =
                    "SAML2.EnableAssertionEncryption";
            public static final String ENABLE_RESPONSE_SIGNING =
                    "SAML2.EnableResponseSigning";
            public static final String ENABLE_REQUEST_SIGNING = "SAML2.EnableRequestSigning";
            public static final String IS_PASSIVE_AUTHN = "SAML2.IsPassiveAuthn";
            public static final String IS_FORCE_AUTHN = "SAML2.IsForceAuthn";
            public static final String RELAY_STATE = "SAML2.RelayState";
            public static final String POST_BINDING_REQUEST_HTML_PAYLOAD =
                    "SAML2.PostBindingRequestHTMLPayload";
            public static final String POST_BINDING_REQUEST_HTML_FILE_PATH =
                    "SAML2.PostBindingRequestHTMLFilePath";
            public static final String SIGNATURE_VALIDATOR = "SAML2.SignatureValidatorImplClass";

            private SAML2() {
            }
        }

        public static class OpenID {

            public static final String PROVIDER_URL = "OpenId.ProviderURL";
            public static final String RETURN_TO_URL = "OpenId.ReturnToURL";
            public static final String CLAIMED_ID = "OpenId.ClaimedId";
            public static final String ENABLE_ATTRIBUTE_EXCHANGE = "OpenId.EnableAttributeExchange";
            public static final String ENABLE_DUMB_MODE = "OpenId.EnableDumbMode";

            private OpenID() {
            }
        }

        public static class OAuth2 {

            public static final String CLIENT_ID = "OAuth2.ClientId";
            public static final String CLIENT_SECRET = "OAuth2.ClientSecret";
            public static final String TOKEN_URL = "OAuth2.TokenURL";

            private OAuth2() {
            }
        }

    }

}
