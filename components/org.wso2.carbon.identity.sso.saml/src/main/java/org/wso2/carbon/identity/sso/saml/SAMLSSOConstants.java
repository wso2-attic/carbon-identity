/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.identity.sso.saml;

public class SAMLSSOConstants {

    public static final String NAME_ID_POLICY_ENTITY = "urn:oasis:names:tc:SAML:2.0:nameid-format:entity";
    public static final String SUBJECT_CONFIRM_BEARER = "urn:oasis:names:tc:SAML:2.0:cm:bearer";
    public static final String NAME_FORMAT_BASIC = "urn:oasis:names:tc:SAML:2.0:attrname-format:basic";

    public class StatusCodes {
        public static final String SUCCESS_CODE = "urn:oasis:names:tc:SAML:2.0:status:Success";    
        public static final String REQUESTOR_ERROR = "urn:oasis:names:tc:SAML:2.0:status:Requester";
        public static final String IDENTITY_PROVIDER_ERROR = "urn:oasis:names:tc:SAML:2.0:status:Responder";
        public static final String VERSION_MISMATCH = "urn:oasis:names:tc:SAML:2.0:status:VersionMismatch";
        public static final String AUTHN_FAILURE = "urn:oasis:names:tc:SAML:2.0:status:AuthnFailed";
        public static final String NO_PASSIVE = "urn:oasis:names:tc:SAML:2.0:status:NoPassive";
    }

    public class SingleLogoutCodes{
        public static final String LOGOUT_USER = "urn:oasis:names:tc:SAML:2.0:logout:user";
        public static final String LOGOUT_ADMIN = "urn:oasis:names:tc:SAML:2.0:logout:admin";
    }

    public class AuthnModes{
        public static final String USERNAME_PASSWORD = "usernamePasswordBasedAuthn";
        public static final String OPENID = "openIDBasedAuthn";
    }
    
    public class Attribute {
    	public static final String ISSUER_FORMAT = "urn:oasis:names:tc:SAML:2.0:nameid-format:entity";
    }

    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String FEDERATED_IDP = "federated-idp-domain";
    public static final String ISSUER = "issuer";
    public static final String ASSRTN_CONSUMER_URL = "assertnConsumerURL";
    public static final String REQ_ID = "id";
    public static final String SUBJECT = "subject";
    public static final String RP_SESSION_ID = "relyingPartySessionId";
    public static final String REQ_MSG_STR = "requestMessageString";
    public static final String DESTINATION = "destination";

    public static final String RELAY_STATE = "RelayState";
    public static final String AUTH_REQ_SAML_ASSRTN = "SAMLRequest";
    public static final String SAML_RESP = "SAMLResponse";
    public static final String SIG_ALG = "SigAlg";
    public static final String SIGNATURE = "Signature";
    public static final String HTTP_QUERY_STRING = "HttpQuerryString";
    public static final String TARGET_ASSRTN_CONSUMER_URL = "targetedAssrtnConsumerURL";
    public static final String kEEP_SESSION_ALIVE = "keepSessionAlive";

    public static final String LOGOUT_RESP = "logoutResponse";

    public static final String STATUS = "status";
    public static final String STATUS_MSG = "statusMsg";

    public static final String SSO_TOKEN_ID = "ssoTokenId";
    public static final String FE_SESSION_KEY = "authSession";

    public static final String AUTH_FAILURE = "authFailure";
    public static final String AUTH_FAILURE_MSG = "authFailureMsg";

    public static final String SAMLSSOServiceClient = "ssoServiceClient";

    public static final String SESSION_DATA_KEY = "sessionDataKey";

    public static final String AUTHENTICATION_RESULT = "AuthenticationResult";

    public static final String LOGIN_PAGE = "customLoginPage";

    public class Notification {
        public static final String EXCEPTION_STATUS = "Error when processing the authentication request!";
        public static final String EXCEPTION_MESSAGE = "Please try login again.";
        public static final String NORELAY_STATUS = "RealyState is not present in the request!";
        public static final String NORELAY_MESSAGE = "This request will not be processed further.";
        public static final String INVALID_MESSAGE_STATUS = "Not a valid SAML 2.0 Request Message!";
        public static final String INVALID_MESSAGE_MESSAGE = "The message was not recognized by the SAML 2.0 SSO Provider. Please check the logs for more details";
    }

    public static class FileBasedSPConfig {

        public static final String SERVICE_PROVIDERS = "ServiceProviders";
        public static final String SERVICE_PROVIDER = "ServiceProvider";
        public static final String ISSUER = "Issuer";
        public static final String ASSERTION_CONSUMER_URL = "AssertionConsumerService";
        public static final String CUSTOM_LOGIN_PAGE = "CustomLoginPage";
        public static final String SIGN_RESPONSE = "SignResponse";
        public static final String SIGN_ASSERTION = "SignAssertion";
        public static final String ENCRYPT_ASSERTION = "EncryptAssertion";
        public static final String SIG_VALIDATION = "ValidateSignatures";
        public static final String SINGLE_LOGOUT = "EnableSingleLogout";
        public static final String ATTRIBUTE_PROFILE = "EnableAttributeProfile";
        public static final String AUDIENCE_RESTRICTION = "EnableAudienceRestriction";
        public static final String RECIPIENT_VALIDATION = "EnableRecipients";
        public static final String IDP_INIT = "EnableIdPInitiatedSSO";
        public static final String USE_FULLY_QUALIFY_USER_NAME = "UseFullyQualifiedUsernameInNameID";

        public static final String CERT_ALIAS = "CertAlias";
        public static final String LOGOUT_URL = "LogoutURL";
        public static final String CLAIMS = "Claims";
        public static final String CLAIM = "Claim";
        public static final String INCLUDE_ATTRIBUTE = "IncludeAttributeByDefault";
        public static final String AUDIENCE_LIST = "AudiencesList";
        public static final String AUDIENCE = "Audience";
        public static final String RECIPIENT_LIST = "RecipientList";
        public static final String RECIPIENT = "Recipient";
        public static final String CONSUMING_SERVICE_INDEX= "ConsumingServiceIndex";
        public static final String USE_AUTHENTICATED_USER_DOMAIN_CRYPTO= "SSOService.UseAuthenticatedUserDomainCrypto";
    }

}

