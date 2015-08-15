package org.wso2.carbon.identity.sso.cas.handler;

import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.sso.cas.CASEndpointConstants;

public class HandlerConstants {
	public static final String COMMON_AUTH_ENDPOINT = "/commonauth";
	public static final String CARBON_APP = "/carbon/";
	
	public static final String TRUE_FLAG_STRING = "true";
	public static final String DEFAULT_ENCODING = "UTF-8";
	
	public static final String LOGOUT_COMPLETE_ARGUMENT = "sessionLogoutComplete";
	public static final String LOGOUT_COMPLETE_NAME_VALUE = "&" + LOGOUT_COMPLETE_ARGUMENT + "=" + TRUE_FLAG_STRING + "#";
	public static final String PATH_DELIMITER = ";";
	public static final String POST_AUTH_REDIRECT_ARGUMENT = "redirect";
	public static final String POST_AUTH_REDIRECT_NAME_VALUE = PATH_DELIMITER + POST_AUTH_REDIRECT_ARGUMENT + "=";
	public static final String POST_AUTH_SUCCESS_ARGUMENT = "loginComplete";
	public static final String POST_AUTH_SUCCESS_NAME_VALUE = PATH_DELIMITER + POST_AUTH_SUCCESS_ARGUMENT + "=" + TRUE_FLAG_STRING;

	public static final String POST_AUTH_SAML_LOGIN_ARGUMENT = "samlLogin";
	public static final String POST_AUTH_SAML_LOGIN_NAME_VALUE = PATH_DELIMITER + POST_AUTH_SAML_LOGIN_ARGUMENT + "=%s";
	
	public static final String PRE_CAS_LOGIN_PATH = CASEndpointConstants.LOGIN_PATH + POST_AUTH_REDIRECT_NAME_VALUE;
	public static final String PRE_CAS_LOGIN_PATH_TEMPLATE = PRE_CAS_LOGIN_PATH + "%s" + POST_AUTH_SAML_LOGIN_NAME_VALUE;
	public static final String POST_CAS_LOGIN_PATH_TEMPLATE = PRE_CAS_LOGIN_PATH_TEMPLATE + "%s";
	public static final String COMMON_AUTH_REDIRECT_URL = "?relyingParty=%s&"+FrameworkConstants.SESSION_DATA_KEY+"=%s&type=cassso&commonAuthCallerPath=%s&forceAuth=%s&passiveAuth=%s";

	public static final String VALIDATION_RESPONSE_TEMPLATE = "<cas:serviceResponse xmlns:cas=\"http://www.yale.edu/tp/cas\">%s</cas:serviceResponse>";
	
	public static final String CUSTOM_RESOURCE_BUNDLE = "org.wso2.carbon.identity.sso.cas.i18n.custom.Resources";
	public static final String RESOURCE_BUNDLE = "org.wso2.carbon.identity.sso.cas.i18n.Resources";
}
