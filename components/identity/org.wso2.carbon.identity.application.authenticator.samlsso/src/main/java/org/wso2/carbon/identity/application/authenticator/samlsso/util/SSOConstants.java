/*
*  Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.application.authenticator.samlsso.util;

public class SSOConstants {
	
	public static final int CUSTOM_STATUS_SEND_TO_LOGIN = 10;
	public static final int CUSTOM_STATUS_AUTHENTICATE = 11;
	
	public static final String AUTHENTICATOR_NAME = "SAMLSSOAuthenticator";
	public static final String AUTHENTICATOR_STATUS = "SAMLSSOAuthenticatorStatus";

	public static final String HTTP_POST_PARAM_SAML2_AUTH_REQ = "SAMLRequest";
	public static final String HTTP_POST_PARAM_SAML2_RESP = "SAMLResponse";
	public static final String IDP_SESSION = "IdPSession";

    public class StatusCodes {
        public static final String IDENTITY_PROVIDER_ERROR = "urn:oasis:names:tc:SAML:2.0:status:Responder";
        public static final String NO_PASSIVE = "urn:oasis:names:tc:SAML:2.0:status:NoPassive";
    }
    
    public class ConfParams {
    	public static final String SERVICE_PROVIDER_ID = "ServiceProviderID";
    	public static final String IDP_CERT_ALIAS = "IdPCertAlias";
    	public static final String ENABLE_REQUEST_SIGNING = "EnableRequestSigning";
    	public static final String ENABLE_RESPONSE_SIGNATION_VALIDATION = "EnableResponseSignatureValidation";
    	public static final String ENABLE_ASSERTION_SIGNATION_VALIDATION = "EnableAssertionSignatureValidation";
    	public static final String ENABLE_SLO = "EnableSLO";
    	public static final String CREDENTIAL_IMPL_CLASS = "CredentialImplClass";
    }
}