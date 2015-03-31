/*
 * Copyright 2005-2007 WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.relyingparty;

/**
 * Constants of the SAML token verifier
 */
public class TokenVerifierConstants {

	public final static String SAML_ATTR_GIVENNAME = "givenname";
	public final static String SAML_ATTR_SURNAME = "surname";
	public final static String SAML_ATTR_EMALADDR = "emailaddress";
	public final static String SAML_ATTR_PPID = "privatepersonalidentifier";
	public final static String SERVICE_CONFIG_SMTP_HOST = "mail.smtp.host";
	public final static String SERVICE_CONFIG_SMTP_PORT = "mail.smtp.port";
	public final static String SERVICE_CONFIG_FROM_ADDRESS = "mail.from.address";
	public final static String SERVICE_CONFIG_FROM_SUBJECT = "mail.from.subject";

	/**
	 * Namespace of the response
	 */
	public final static String NS = "http://ns.wso2.org/relyingparty";

	/**
	 * Namespace prefix
	 */
	public final static String PREFIX = "rp";

	/**
	 * Response element local name
	 */
	public final static String LN_RESP = "VerifierResponse";

	/**
	 * Request element local name
	 */
	public final static String LN_REQ = "VerifierRequest";

	/**
	 * Token element local name
	 */
	public final static String LN_TOKEN = "Token";

	public final static String LN_ISSUER = "Issuer";

	/**
	 * Value of self issuer
	 */
	public final static String SELF_ISSUER_VALUE = "self";

	/**
	 * Property element local name
	 */
	public final static String LN_PROPERTY = "Property";
	public final static String LN_CERTIFICATES = "Certificates";
	public final static String LN_CERTIFICATE = "Certificate";
	public final static String LN_SIGNING_CERT = "SigningCert";
	public final static String LN_ISSUER_INFO = "IssuerInfo";
	public final static String LN_PROPERTIES = "Properties";

	/**
	 * Name attribute of the <code>Property</code> element
	 */
	public final static String ATTR_NAME = "Name";
	public final static String ATTR_STATE = "State";
	/**
	 * Context attribute of reason for the failure
	 */
	public final static String FAILURE_REASON = "failure-reason";
	public final static String SERVLET_ATTR_STATE = "org.wso2.solutions.identity.rp.State";
	public final static String ISSUER_SELF = "self";
	public final static String ISSUER_MANAGED = "managed";
	public final static String ISSUER_INFO = "issuerInfo";

	// Response states

	public final static String STATE_SUCCESS = "success";
	public final static String STATE_FAILURE = "failure";

	public final static String REASON_TOKEN_MISSING = "missingToken";
	public static final String RESOURCES = "org.wso2.solutions.identity.relyingparty.resources";
	public static final String CACERTS_STORE_UNIX = "/jre/lib/security/cacerts";
	public static final String CACERTS_STORE_WIN = "\\jre\\lib\\security\\cacerts";

	/**
	 * Constants in the config file.
	 */
	public static final String KEY_STORE = "Keystore";
	public static final String STORE_TYPE = "StoreType";
	public static final String STORE_PASS = "StorePass";
	public static final String KEY_ALIAS = "KeyAlias";
	public static final String KEY_PASS = "KeyPass";
	public static final String MAPPING_HOST = "MappingHost";
	public static final String MAPPING_PORT = "MappingPort";
	public static final String MAPPED_HOST = "MappedHost";
	public static final String MAPPED_PORT = "MappedPort";

	public static final String TRUSTED_KEY_STORE = "TrustedIdP.KeyStore";
	public static final String TRUSTED_STORE_PASS = "TrustedIdP.StorePass";
	public static final String TRUSTED_STORE_TYPE = "TrustedIdP.StoreType";

	public static final String MULTIVALUE_CLAIMS_POLICY = "MultiValueClaimsPolicy";
	public static final String ISSUER_POLICY = "IssuerPolicy";
	public static final String TOKEN_VALIDATE_POLICY = "TokenValidationPolicy";

	public static final String SYSTEM_KEY_STORE_PASS = "System.StorePass";
	public static final String SYSTEM_KEY_STORE = "System.KeyStore";

	public static final String OP_KEY_STORE = "OP.KeyStore";
	public static final String OP_STORE_PASS = "OP.StorePass";
	public static final String OP_STORE_TYPE = "OP.StoreType";
	public static final String OP_VALIDATION_POLICY = "OPValidationPolicy";

	/**
	 * IssuerPolicy
	 */
	public static final String SELF_ONLY = "SelfOnly";
	public static final String MANGED_ONLY = "ManagedOnly";
	public static final String SELF_AND_MANGED = "SelfAndManaged";
	public static final String MULTIVALUED_CLAIMS_ALLOWED = "MultiValueClaimsAllowed";
	public static final String MULTIVALUED_CLAIMS_NOT_ALLOWED = "MultiValueClaimsNotAllowed";

	public static final String PROMISCUOUS = "Promiscuous";
	public static final String BLACK_LIST = "BlackList";
	public static final String WHITE_LIST = "WhiteList";
	public static final String CERT_VALIDATE = "CertValidate";

}