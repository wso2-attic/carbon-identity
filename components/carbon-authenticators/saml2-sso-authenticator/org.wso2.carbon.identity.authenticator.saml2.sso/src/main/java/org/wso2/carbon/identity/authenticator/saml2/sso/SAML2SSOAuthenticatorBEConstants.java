/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.authenticator.saml2.sso;

public class SAML2SSOAuthenticatorBEConstants {

    public static final String SAML2_SSO_AUTHENTICATOR_NAME = "SAML2SSOAuthenticator";

    public static final String ROLE_ATTRIBUTE_NAME = "http://wso2.org/claims/role";
    public static final String ATTRIBUTE_VALUE_SEPERATER = ",";


    public class PropertyConfig {
        private PropertyConfig(){

        }
        public static final String AUTH_CONFIG_PARAM_IDP_CERT_ALIAS = "IdPCertAlias";
        public static final String RESPONSE_SIGNATURE_VALIDATION_ENABLED = "ResponseSignatureValidationEnabled";
        public static final String VALIDATE_SIGNATURE_WITH_USER_DOMAIN = "VerifySignatureWithUserDomain";
        public static final String ROLE_CLAIM_ATTRIBUTE = "RoleClaimAttribute";
        public static final String ATTRIBUTE_VALUE_SEPARATOR = "AttributeValueSeparator";

        public static final String JIT_USER_PROVISIONING_ENABLED = "JITUserProvisioningEnabled";
        public static final String PROVISIONING_DEFAULT_USERSTORE = "ProvisioningDefaultUserstore";
        public static final String PROVISIONING_DEFAULT_ROLE = "ProvisioningDefaultRole";
        public static final String IS_SUPER_ADMIN_ROLE_REQUIRED = "IsSuperAdminRoleRequired";
    }

}
