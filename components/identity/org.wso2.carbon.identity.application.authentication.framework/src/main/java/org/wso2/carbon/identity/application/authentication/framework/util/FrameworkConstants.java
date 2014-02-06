/*
*  Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.application.authentication.framework.util;

/**
 * Constants used in Application Authenticators Framework
 *
 */
public abstract class FrameworkConstants {
	
	public static class Config {
    	public static final String AUTHENTICATORS_FILE_NAME = "application-authenticators.xml";
    
        //Constant definitions for Elements
        public static final String ELEM_SEQUENCE = "Sequence";	
        public static final String ELEM_STEP = "Step";
        public static final String ELEM_AUTHENTICATOR = "Authenticator";
        public static final String ELEM_AUTHENTICATOR_CONFIG = "AuthenticatorConfig";
        public static final String ELEM_IDP_CONFIG = "IdPConfig";
        public static final String ELEM_PARAMETER = "Parameter";
    
        //Constant definitions for attributes
        public static final String ATTR_AUTHENTICATOR_NAME = "name";
        public static final String ATTR_AUTHENTICATOR_IDPS = "idpList";
        public static final String ATTR_AUTHENTICATOR_ENABLED = "enabled";
        public static final String ATTR_PARAMETER_NAME = "name";
        public static final String ATTR_STEP_LOGIN_PAGE = "loginPage";
        public static final String ATTR_STEP_ORDER = "order";
        public static final String ATTR_APPLICATION_NAME = "name";
        public static final String ATTR_AUTHENTICATOR_CONFIG_NAME = "name";
        public static final String ATTR_FORCE_AUTHENTICATE = "forceAuthn";
        public static final String ATTR_CHECK_AUTHENTICATE = "checkAuthn";
        public static final String ATTR_RELYING_PARTY = "relyingParty";
        
        //Constant definitions for other QNames
        public static final String QNAME_AUTHENTICATION_ENDPOINT_URL = "AuthenticationEndpointURL";
        public static final String QNAME_AUTHENTICATOR_CONFIGS = "AuthenticatorConfigs";
        public static final String QNAME_IDP_CONFIGS = "IdPConfigs";
        public static final String QNAME_SEQUENCES = "Sequences";
	}      
	
	public static class RequestParams {
		public static final String TYPE = "type";
		public static final String DENY = "deny";
		public static final String FORCE_AUTHENTICATE = "forceAuthenticate";
		public static final String CHECK_AUTHENTICATION = "checkAuthentication";
		public static final String CALLER_PATH = "commonAuthCallerPath";
		public static final String FEDERATED_IDP = "fidp";
	}
	
	public static class ResponseParams {
		public static final String AUTHENTICATED = "commonAuthAuthenticated";
		public static final String AUTHENTICATED_USER = "authenticatedUser";
		public static final String LOGGED_OUT = "commonAuthLoggedOut";
	}
	
	public static class SessionAttributes {
		public static final String SEQUENCE_CONFIG = "sequenceConfig";
		public static final String CURRENT_STEP = "currentStep";
	}
	
	public static class AuthenticatorStatus {
		public static final String CONTINUE = "continue";
	}
	
	public static final String SESSION_DATA_KEY = "sessionDataKey";
	public static final String QUERY_PARAMS = "commonAuthQueryParams";
	public static final String SUBJECT = "subject";
	public static final String DEFAULT_SEQUENCE = "default";
	public static final String AUTHENTICATED_AUTHENTICATORS = "authenticatedAuthenticators";
}
