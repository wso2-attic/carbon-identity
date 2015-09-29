/*
 * Copyright (c) 2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
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

package org.wso2.carbon.identity.sso.saml.ui;

public class SAMLSSOUIConstants {

    public static final String ISSUER_LIST_FILTER = "org.wso2.carbon.issuer.filter";
    public static final int DEFAULT_ITEMS_PER_PAGE = 5;
    public static final String CONFIG_CLIENT = "SAMLSSOConfigServiceClient";
    public static final String STARTING_SERVICE_PROVIDERS_DATA_PAGE = "starting_page";
    public static final String SERVICE_PROVIDERS_DATA_PAGE_COUNT = "page_count";
    public static final String ENABLE_ATTRIBUTE_PROFILE = "enableAttributeProfile";
    public static final String ENABLE_SINGLE_LOGOUT = "enableSingleLogout";
    public static final String SLO_RESPONSE_URL = "sloResponseURL";
    public static final String SLO_REQUEST_URL = "sloRequestURL";
    public static final String ENABLE_RESPONSE_SIGNATURE = "enableResponseSignature";
    public static final String ENABLE_ASSERTION_SIGNATURE = "enableAssertionSignature";
    public static final String NAME_ID_FORMAT = "nameIdFormat";
    public static final String ENABLE_DEFAULT_ATTRIBUTE_PROFILE_HIDDEN = "enableDefaultAttributeProfileHidden";
    public static final String ENABLE_NAME_ID_CLAIM_URI_HIDDEN = "enableNameIdClaimUriHidden";
    public static final String NAME_ID_CLAIM = "nameIdClaim";
    public static final String ENABLE_AUDIENCE_RESTRICTION = "enableAudienceRestriction";
    public static final String ENABLE_RECIPIENTS = "enableRecipients";
    public static final String LOGIN_PAGE_URL = "loginPageURL";
    public static final String ENABLE_IDP_INIT_SSO = "enableIdPInitSSO";
    public static final String ENABLE_IDP_INIT_SLO = "enableIdPInitSLO";
    public static final String ENABLE_ENC_ASSERTION = "enableEncAssertion";
    public static final String ENABLE_SIG_VALIDATION = "enableSigValidation";
    public static final String CLAIM_PROPERTY_COUNTER = "claimPropertyCounter";
    public static final String CLAIM_PROPERTY_NAME = "claimPropertyName";
    public static final String AUDIENCE_PROPERTY_COUNTER = "audiencePropertyCounter";
    public static final String AUDIENCE_PROPERTY_NAME = "audiencePropertyName";
    public static final String RECIPIENT_PROPERTY_COUNTER = "recipientPropertyCounter";
    public static final String RECIPIENT_PROPERTY_NAME = "recipientPropertyName";

    public static final String SAML_SSO_SIGNING_ALGORITHM = "signingAlgorithm";
    public static final String SAML_SSO_DIGEST_ALGORITHM = "digestAlgorithm";

    private SAMLSSOUIConstants() {
    }
}
