/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 */

package org.wso2.carbon.identity.application.authentication.endpoint.util;

public class Constants {

    public static final String SESSION_DATA_KEY = "sessionDataKey";
    public static final String SESSION_DATA_KEY_CONSENT = "sessionDataKeyConsent";
    public static final String AUTH_FAILURE = "authFailure";
    public static final String AUTH_FAILURE_MSG = "authFailureMsg";
    public static final String STATUS = "status";
    public static final String STATUS_MSG = "statusMsg";
    public static final String IDP_AUTHENTICATOR_MAP = "idpAuthenticatorMap";
    public static final String RESIDENT_IDP_RESERVED_NAME = "LOCAL";
    public static final String WEB_CONTEXT_ROOT = "WebContextRoot";

    private Constants() {
    }

    public static class SAML2SSO {
        public static final String ASSERTION_CONSUMER_URL = "assertnConsumerURL";
        public static final String RELAY_STATE = "RelayState";
        public static final String SAML_RESP = "SAMLResponse";

        private SAML2SSO() {
        }
    }
}