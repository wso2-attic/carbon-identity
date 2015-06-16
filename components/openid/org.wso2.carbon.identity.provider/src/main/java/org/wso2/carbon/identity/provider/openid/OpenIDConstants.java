/*
 * Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.provider.openid;

public class OpenIDConstants {

    public static final String NS = "http://schema.openid.net";
    public static final String OPENID_URL = "http://specs.openid.net/auth/2.0";
    public static final String ATTR_MODE = "openid.mode";
    public static final String ATTR_IDENTITY = "openid.identity";
    public static final String ATTR_RESPONSE_NONCE = "openid.response_nonce";
    public static final String ATTR_OP_ENDPOINT = "openid.op_endpoint";
    public static final String ATTR_NS = "openid.ns";
    public static final String ATTR_CLAIM_ID = "openid.claimed_id";
    public static final String ATTR_RETURN_TO = "openid.return_to";
    public static final String ATTR_ASSOC_HANDLE = "openid.assoc_handle";
    public static final String ATTR_SIGNED = "openid.signed";
    public static final String ATTR_SIG = "openid.sig";
    public static final String OPENID_IDENTIFIER = "openid_identifier";
    public static final String ASSOCIATE = "associate";
    public static final String CHECKID_SETUP = "checkid_setup";
    public static final String CHECKID_IMMEDIATE = "checkid_immediate";
    public static final String CHECK_AUTHENTICATION = "check_authentication";
    public static final String DISC = "openid-disc";
    public static final String PREFIX = "openid";
    public static final String ASSERTION = "openidAssertion";
    public static final String COMPLETE = "complete";
    public static final String ONLY_ONCE = "Only Once";
    public static final String ONCE = "once";
    public static final String ALWAYS = "always";
    public static final String DENY = "Deny";
    public static final String ACTION = "_action";
    public static final String OPENID_RESPONSE = "id_res";
    public static final String AUTHENTICATED_AND_APPROVED = "authenticatedAndApproved";
    public static final String CANCEL = "cancel";
    public static final String FALSE = "false";
    public static final String PARAM_LIST = "parameterlist";
    public static final String PASSWORD = "password";
    public static final String SERVICE_NAME_STS_OPENID = "sts-openid-ut";
    public static final String SERVICE_NAME_MEX_OPENID = "mex-openid-ut";
    public static final String SERVICE_NAME_MEX_IC_OPENID = "mex-openid-ic";
    public static final String SERVICE_NAME_STS_IC_OPENID = "sts-openid-ic";
    public static final String AUTHENTICATION_RESULT = "AuthenticationResult";

    public static final String UTF_8 = "UTF-8";

    // session attributes
    public static class SessionAttribute {
        public static final String OPENID = "openId";
        public static final String USERNAME = "userName";
        public static final String OPENID_ADMIN_CLIENT = "openid_admin_client";
        public static final String PROFILE = "profile";
        public static final String SELECTED_PROFILE = "selectedProfile";
        public static final String DEFAULT_PROFILE = "default";
        public static final String ACTION = "_action";
        public static final String USER_APPROVED = "userApproved";
        public static final String USER_APPROVED_ALWAYS = "userApprovedAlways";
        public static final String HAS_APPROVED_ALWAYS = "hasApprovedAlways";
        public static final String AUTHENTICATED_OPENID = "authenticatedOpenID";
        public static final String IS_OPENID_AUTHENTICATED = "isOpenIDAuthenticated";

        private SessionAttribute() {
        }
    }

    // request parameters
    public static class RequestParameter {
        public static final String LOGOUT_URL = "logoutUrl";
        public static final String OPENID = "openId";
        public static final String USERNAME = "userName";
        public static final String REMEMBER = "remember";
        public static final String PASSWORD = "password";
        public static final String HAS_APPROVED_ALWAYS = "hasApprovedAlways";
        public static final String SESSION_DATA_KEY = "sessionDataKey";
        public static final String NON_LOGIN = "nonlogin";

        private RequestParameter() {
        }
    }

    // cookies
    public static class Cookie {
        public static final String OPENID_TOKEN = "openidtoken";
        public static final String OPENID_REMEMBER_ME = "openidrememberme";

        private Cookie() {
        }
    }

    public static class PapeAttributes {
        public static final String AUTH_POLICIES = "auth_policies";
        public static final String NIST_AUTH_LEVEL = "nist_auth_level";
        public static final String AUTH_AGE = "auth_age";
        public static final String PHISHING_RESISTANCE =
                "http://schemas.openid.net/pape/policies/2007/06/phishing-resistant";
        public static final String MULTI_FACTOR = "http://schemas.openid.net/pape/policies/2007/06/multi-factor";
        public static final String MULTI_FACTOR_PHYSICAL =
                "http://schemas.openid.net/pape/policies/2007/06/multi-factor-physical";

        private PapeAttributes() {
        }
    }

    public static class SimpleRegAttributes {

        // As per the OpenID Simple Registration Extension 1.0 spec fields
        // below should be included in the Identity Provider's
        // response when "openid.mode" is "id_res"

        public static final String NS_SREG = "http://openid.net/sreg/1.0";
        public static final String NS_SREG_1 = "http://openid.net/extensions/sreg/1.1";
        public static final String SREG = "openid.sreg.";
        public static final String OP_SREG = "openid.ns.sreg";

        public static final String NICK_NAME = "nickname";
        public static final String EMAIL = "email";
        public static final String FULL_NAME = "fullname";
        public static final String DOB = "dob";
        public static final String GENDER = "gender";
        public static final String POSTAL_CODE = "postcode";
        public static final String COUNTRY = "country";
        public static final String LANGUAGE = "language";
        public static final String TIMEZONE = "timezone";

        public static final String FULL_NAME_NS = "http://schema.openid.net/2007/05/claims/fullname";
        public static final String DOB_NS = "http://schema.openid.net/2007/05/claims/dob";
        public static final String GENDER_NS = "http://schema.openid.net/2007/05/claims/gender";
        public static final String LANGUAGE_NS = "http://schema.openid.net/2007/05/claims/language";
        public static final String TIMEZONE_NS = "http://schema.openid.net/2007/05/claims/timezone";

        private SimpleRegAttributes() {
        }
    }

    public static class ExchangeAttributes extends SimpleRegAttributes {

        public static final String NS = "http://axschema.org";
        public static final String NS_AX = "http://openid.net/srv/ax/1.0";
        public static final String EXT = "openid.ns.ext1";
        public static final String MODE = "openid.ext1.mode";
        public static final String TYPE = "openid.ext1.type.";
        public static final String VALUE = "openid.ext1.value.";
        public static final String FETCH_RESPONSE = "fetch_response";

        public static final String NICK_NAME_NS = NS + "/namePerson/friendly";
        public static final String EMAIL_NS = NS + "/contact/email";
        public static final String FULL_NAME_NS = NS + "/namePerson";
        public static final String DOB_NS = NS + "/birthDate";
        public static final String GENDER_NS = NS + "/person/gender";
        public static final String POSTAL_CODE_NS = NS
                                                    + "/contact/postalCode/home";
        public static final String COUNTRY_NS = NS + "/contact/country/home";
        public static final String LANGUAGE_NS = NS + "/pref/language";
        public static final String TIMEZONE_NS = NS + "/pref/timezone";

        private ExchangeAttributes() {
        }
    }

    // OpenID request parameters
    public static class OpenIDRequestParameters {

        public static final String OPENID_REALM = "openid.realm";
        public static final String OPENID_RETURN_TO = "openid.return_to";
        public static final String OPENID_CLAIMED_ID = "openid.claimed_id";
        public static final String OPENID_IDENTITY = "openid.identity";

        private OpenIDRequestParameters() {
        }
    }

    private OpenIDConstants() {
    }
}
