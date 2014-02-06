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
package org.wso2.carbon.identity.provider.openid;

public class OpenIDConstants {

	public final static String NS = "http://schema.openid.net";
	public final static String OPENID_URL = "http://specs.openid.net/auth/2.0";
	public final static String ATTR_MODE = "openid.mode";
	public final static String ATTR_IDENTITY = "openid.identity";
	public final static String ATTR_RESPONSE_NONCE = "openid.response_nonce";
	public final static String ATTR_OP_ENDPOINT = "openid.op_endpoint";
	public final static String ATTR_NS = "openid.ns";
	public final static String ATTR_CLAIM_ID = "openid.claimed_id";
	public final static String ATTR_RETURN_TO = "openid.return_to";
	public final static String ATTR_ASSOC_HANDLE = "openid.assoc_handle";
	public final static String ATTR_SIGNED = "openid.signed";
	public final static String ATTR_SIG = "openid.sig";
	public final static String OPENID_IDENTIFIER = "openid_identifier";
	public final static String ASSOCIATE = "associate";
	public final static String CHECKID_SETUP = "checkid_setup";
	public final static String CHECKID_IMMEDIATE = "checkid_immediate";
	public final static String CHECK_AUTHENTICATION = "check_authentication";
	public final static String DISC = "openid-disc";
	public static final String PREFIX = "openid";
	public final static String ASSERTION = "openidAssertion";
	public final static String COMPLETE = "complete";
	public final static String ONLY_ONCE = "Only Once";
	public final static String ONCE = "once";
	public final static String ALWAYS = "always";
	public final static String DENY = "Deny";
	public final static String ACTION = "_action";
	public final static String OPENID_RESPONSE = "id_res";
	public static final String AUTHENTICATED_AND_APPROVED = "authenticatedAndApproved";
	public final static String CANCEL = "cancel";
	public final static String FALSE = "false";
	public final static String PARAM_LIST = "parameterlist";
	public final static String PASSWORD = "password";
	public static final String SERVICE_NAME_STS_OPENID = "sts-openid-ut";
	public static final String SERVICE_NAME_MEX_OPENID = "mex-openid-ut";
	public static final String SERVICE_NAME_MEX_IC_OPENID = "mex-openid-ic";
	public static final String SERVICE_NAME_STS_IC_OPENID = "sts-openid-ic";
	
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
	}
	
	// request parameters
	public static class RequestParameter {
		public static final String LOGOU_URL = "logoutUrl";
		public static final String OPENID = "openId";
		public static final String USERNAME = "userName";
		public static final String REMEMBER = "remember";
		public static final String PASSWORD = "password";
		public static final String HAS_APPROVED_ALWAYS = "hasApprovedAlways";
	}

	// cookies 
	public static class Cookie {
		public static final String OPENID_TOKEN = "openidtoken";
		public static final String OPENID_REMEMBER_ME = "openidrememberme";
	}
	
	public static class PapeAttributes {
		public final static String AUTH_POLICIES = "auth_policies";
		public final static String NIST_AUTH_LEVEL = "nist_auth_level";
		public final static String AUTH_AGE = "auth_age";
		public final static String PHISHING_RESISTANCE = "http://schemas.openid.net/pape/policies/2007/06/phishing-resistant";
		public final static String MULTI_FACTOR = "http://schemas.openid.net/pape/policies/2007/06/multi-factor";
		public final static String MULTI_FACTOR_PHYSICAL = "http://schemas.openid.net/pape/policies/2007/06/multi-factor-physical";
	}

	public static class SimpleRegAttributes {

		// As per the OpenID Simple Registration Extension 1.0 spec fields
		// below should be included in the Identity Provider's
		// response when "openid.mode" is "id_res"

		public final static String NS_SREG = "http://openid.net/sreg/1.0";
		public final static String NS_SREG_1 = "http://openid.net/extensions/sreg/1.1";
		public final static String SREG = "openid.sreg.";
		public final static String OP_SREG = "openid.ns.sreg";

		public final static String NICK_NAME = "nickname";
		public final static String EMAIL = "email";
		public final static String FULL_NAME = "fullname";
		public final static String DOB = "dob";
		public final static String GENDER = "gender";
		public final static String POSTAL_CODE = "postcode";
		public final static String COUNTRY = "country";
		public final static String LANGUAGE = "language";
		public final static String TIMEZONE = "timezone";

		public final static String FULL_NAME_NS = "http://schema.openid.net/2007/05/claims/fullname";
		public final static String DOB_NS = "http://schema.openid.net/2007/05/claims/dob";
		public final static String GENDER_NS = "http://schema.openid.net/2007/05/claims/gender";
		public final static String LANGUAGE_NS = "http://schema.openid.net/2007/05/claims/language";
		public final static String TIMEZONE_NS = "http://schema.openid.net/2007/05/claims/timezone";

	}

	public static class ExchangeAttributes extends SimpleRegAttributes {

		public final static String NS = "http://axschema.org";
		public final static String NS_AX = "http://openid.net/srv/ax/1.0";
		public final static String EXT = "openid.ns.ext1";
		public final static String MODE = "openid.ext1.mode";
		public final static String TYPE = "openid.ext1.type.";
		public final static String VALUE = "openid.ext1.value.";
		public final static String FETCH_RESPONSE = "fetch_response";

		public final static String NICK_NAME_NS = NS + "/namePerson/friendly";
		public final static String EMAIL_NS = NS + "/contact/email";
		public final static String FULL_NAME_NS = NS + "/namePerson";
		public final static String DOB_NS = NS + "/birthDate";
		public final static String GENDER_NS = NS + "/person/gender";
		public final static String POSTAL_CODE_NS = NS
				+ "/contact/postalCode/home";
		public final static String COUNTRY_NS = NS + "/contact/country/home";
		public final static String LANGUAGE_NS = NS + "/pref/language";
		public final static String TIMEZONE_NS = NS + "/pref/timezone";

	}
}
