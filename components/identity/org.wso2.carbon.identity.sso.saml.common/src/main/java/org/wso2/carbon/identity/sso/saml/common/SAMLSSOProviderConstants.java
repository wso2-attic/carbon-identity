/*
 * Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.sso.saml.common;

public class SAMLSSOProviderConstants {

    public static final String ISSUER_LIST_FILTER = "org.wso2.carbon.issuer.filter";
    public static final int DEFAULT_ITEMS_PER_PAGE = 5;
    public static final String CONFIG_CLIENT = "SAMLSSOConfigServiceClient";
    public static final String STARTING_SERVICE_PROVIDERS_DATA_PAGE = "starting_page";
    public static final String SERVICE_PROVIDERS_DATA_PAGE_COUNT = "page_count";

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

    public static final String LOGIN_PAGE = "customLoginPage";

	public class AuthnModes {
		public static final String USERNAME_PASSWORD = "usernamePasswordBasedAuthn";
		public static final String OPENID = "openIDBasedAuthn";
	}

	public class Notification {
		public static final String EXCEPTION_STATUS = "Error when processing the authentication request!";
		public static final String EXCEPTION_MESSAGE = "Please try login again.";
		public static final String NORELAY_STATUS = "RealyState is not present in the request!";
		public static final String NORELAY_MESSAGE = "This request will not be processed further.";
		public static final String INVALID_MESSAGE_STATUS = "Not a valid SAML 2.0 Request Message!";
		public static final String INVALID_MESSAGE_MESSAGE = "The message was not recognized by the SAML 2.0 SSO Provider. Please check the logs for more details";
	}
}
