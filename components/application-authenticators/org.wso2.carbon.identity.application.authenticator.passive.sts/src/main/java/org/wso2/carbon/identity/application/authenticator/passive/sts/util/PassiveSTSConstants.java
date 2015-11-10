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

package org.wso2.carbon.identity.application.authenticator.passive.sts.util;

public class PassiveSTSConstants {

    public static final String AUTHENTICATOR_NAME = "PassiveSTSAuthenticator";
    public static final String AUTHENTICATOR_FRIENDLY_NAME = "Passive STS";
    public static final String HTTP_PARAM_PASSIVE_STS_RESULT = "wresult";
    public static final String REALM_ID = "RealmId";
    public static final String ERROR_WHILE_INSTANTIATING_SSOAGENT_CREDENTIAL_IMPL_CLASS = "Error while instantiating SSOAgentCredentialImplClass: ";
    public static final String ERROR_IN_UNMARSHALLING_SAML_REQUEST_FROM_THE_ENCODED_STRING = "Error in unmarshalling SAML Request from the encoded String";
    public static final String EXTERNAL_GENERAL_ENTITIES_URI= "http://xml.org/sax/features/external-general-entities";

    private PassiveSTSConstants() {
    }
}