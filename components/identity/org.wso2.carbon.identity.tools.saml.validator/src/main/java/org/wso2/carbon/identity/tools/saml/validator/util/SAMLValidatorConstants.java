package org.wso2.carbon.identity.tools.saml.validator.util;

public class SAMLValidatorConstants {

	public class Attribute {
		public static final String ISSUER_FORMAT =
		                                           "urn:oasis:names:tc:SAML:2.0:nameid-format:entity";
	}

	public class ValidationType {
		public static final String VAL_DECODE = "Decoding the Request";
		public static final String VAL_UNMARSHAL = "Unmarshalling the Request";
		public static final String VAL_VERSION = "Validating the Version";
		public static final String VAL_ISSUER = "Checking for Issuer";
		public static final String VAL_ISSUER_FORMAT = "Validating Issuer Format";
		public static final String VAL_IDP_CONFIGS = "Loading IdP Configurations";
		public static final String VAL_CONSUM_URL = "Validating Assertion Consumer URL";
		public static final String VAL_SUB_NAMEID_FMT = "Validating Suject NameID Format";
		public static final String VAL_SUB_CONF_MTHD = "Validating Suject Confirmation Method";
		public static final String VAL_DESTINATION = "Validating Destination";
		public static final String VAL_SIGNATURE = "Validating Signature";
		public static final String VAL_AUTHN_REQUEST = "Validating for AuthnRequest";
		public static final String VAL_WRONG_BINDING = "Invalid Binding for AuthnRequest";
	}

	public class ValidationMessage {
		public static final String VAL_DECODE_SUCCESS = "Request decoded succesfully.";
		public static final String VAL_DECODE_FAIL = " Unable to decode the request.";
		public static final String VAL_UNMARSHAL_SUCCESS = "Request unmarshalled succesfully.";
		public static final String VAL_UNMARSHAL_FAIL = " Unable to unmarshal the request.";
		public static final String VAL_VERSION_SUCCESS = "SAML version is valid.";
		public static final String VAL_VERSION_FAIL = "Invalied SAML version %s, expected version is 2.0.";
		public static final String VAL_ISSUER_SUCCESS = "Issuer/ProviderName is available in the AuthnRequest.";
		public static final String VAL_ISSUER_FAIL = "Issuer/ProviderName is not available in the AuthnRequest.";
		public static final String VAL_ISSUER_FMT_SUCCESS = "Issuer Format attribute value is valid.";
		public static final String VAL_ISSUER_FMT_FAIL = "Issuer Format attribute value is invalid.";
		public static final String VAL_IDP_CONFIGS_SUCCESS = "A Service Provider with the Issuer '%s' is registered.";
		public static final String VAL_IDP_CONFIGS_FAIL = "A Service Provider with the Issuer '%s' is not registered.";
		public static final String EXIT_WITH_ERROR = "Validation service error exit.";
		public static final String ERROR_LOADING_SP_CONF = "Error while reading Service Provider configurations.";
		public static final String VAL_CONSUM_URL_SUCCESS = "Assertion Consumer URL value '%s' is matched.'";
		public static final String VAL_CONSUM_URL_FAIL = "Invalid Assertion Consumer URL value '%s', expected value '%s'.";
		public static final String VAL_SUB_NAMEID_SUCCESS = "Subject NameID fromat is matched.";
		public static final String VAL_SUB_CONF_MTHD_FAIL = "Subject Confirmation methods should NOT be in the request.";
		public static final String VAL_DESTINATION_SUCCESS = "Destination value '%s' is matched.";
		public static final String VAL_DESTINATION_FAIL = "Invalid destination value '%s', expected value '%s'.";
		public static final String VAL_SIGNATURE_SUCCESS = "Signature validation for AuthnRequest is succeeded.";
		public static final String VAL_SIGNATURE_FAIL = "Signature validation for Authentication Request failed.";
		public static final String VAL_SIGNATURE_ERROR = "Signature validation for Authentication Request failed with error : %s";
		public static final String VAL_AUTHN_REQUEST_FAIL = "Request is not an AuthnRequest";
		public static final String VAL_EXTRACT_SAML_REQ_FAIL = "Extracting SAML request failed.";
		public static final String VAL_DECODE_QUERY_STRING_FAIL = "Decoding query string failed.";
		public static final String VAL_WRONG_BINDING_MSG = "Request can unmarshall using 'Redirect Binding', please check Binding in Service Provider side.";
	}
	
	public class ErrorMessage{
		public static final String ERROR_INCOMPLETE_DATA = "Provided data(Issuer/User Name) is incomplete.";
		public static final String ERROR_CONFIG_NOT_AVAIL = "A Service Provider with the Issuer '%s' is not registered.";
		public static final String ERROR_BUILD_FAIL = "Response generation failed with error : '%s'.";
	}
}
