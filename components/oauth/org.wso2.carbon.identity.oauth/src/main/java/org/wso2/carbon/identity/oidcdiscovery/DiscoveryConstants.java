/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.oidcdiscovery;


public class DiscoveryConstants {
    /*
issuer
    REQUIRED. URL using the https scheme with no query or fragment component that the OP asserts as its
    Issuer Identifier. If Issuer discovery is supported (see Section 2), this value MUST be identical to
    the issuer value returned by WebFinger. This also MUST be identical to the iss Claim value in ID
    Tokens issued from this Issuer.
authorization_endpoint
    REQUIRED. URL of the OP's OAuth 2.0 Authorization Endpoint [OpenID.Core].
token_endpoint
    URL of the OP's OAuth 2.0 Token Endpoint [OpenID.Core]. This is REQUIRED unless only the Implicit
    Flow is used.
userinfo_endpoint
    RECOMMENDED. URL of the OP's UserInfo Endpoint [OpenID.Core]. This URL MUST use the https scheme and
    MAY contain port, path, and query parameter components.
jwks_uri
    REQUIRED. URL of the OP's JSON Web Key Set [JWK] document. This contains the signing key(s) the RP
    uses to validate signatures from the OP. The JWK Set MAY also contain the Server's encryption key(s),
     which are used by RPs to encrypt requests to the Server. When both signing and encryption keys are
     made available, a use (Key Use) parameter value is REQUIRED for all keys in the referenced JWK Set
     to indicate each key's intended usage. Although some algorithms allow the same key to be used for
     both signatures and encryption, doing so is NOT RECOMMENDED, as it is less secure. The JWK x5c
     parameter MAY be used to provide X.509 representations of keys provided. When used, the bare key
     values MUST still be present and MUST match those in the certificate.
registration_endpoint
    RECOMMENDED. URL of the OP's Dynamic Client Registration Endpoint [OpenID.Registration].
scopes_supported
    RECOMMENDED. JSON array containing a list of the OAuth 2.0 [RFC6749] scope values that this server
    supports. The server MUST support the openid scope value. Servers MAY choose not to advertise some
    supported scope values even when this parameter is used, although those defined in [OpenID.Core]
    SHOULD be listed, if supported.
response_types_supported
    REQUIRED. JSON array containing a list of the OAuth 2.0 response_type values that this OP supports.
    Dynamic OpenID Providers MUST support the code, id_token, and the token id_token Response Type values.
response_modes_supported
    OPTIONAL. JSON array containing a list of the OAuth 2.0 response_mode values that this OP supports,
    as specified in OAuth 2.0 Multiple Response Type Encoding Practices [OAuth.Responses]. If omitted,
    the default for Dynamic OpenID Providers is ["query", "fragment"].
grant_types_supported
    OPTIONAL. JSON array containing a list of the OAuth 2.0 Grant Type values that this OP supports.
    Dynamic OpenID Providers MUST support the authorization_code and implicit Grant Type values and MAY
    support other Grant Types. If omitted, the default value is ["authorization_code", "implicit"].
acr_values_supported
    OPTIONAL. JSON array containing a list of the Authentication Context Class References that this OP
    supports.
subject_types_supported
    REQUIRED. JSON array containing a list of the Subject Identifier types that this OP supports. Valid
    types include pairwise and public.
id_token_signing_alg_values_supported
    REQUIRED. JSON array containing a list of the JWS signing algorithms (alg values) supported by the OP
     for the ID Token to encode the Claims in a JWT [JWT]. The algorithm RS256 MUST be included. The
     value none MAY be supported, but MUST NOT be used unless the Response Type used returns no ID Token
     from the Authorization Endpoint (such as when using the Authorization Code Flow).
id_token_encryption_alg_values_supported
    OPTIONAL. JSON array containing a list of the JWE encryption algorithms (alg values) supported by the
     OP for the ID Token to encode the Claims in a JWT [JWT].
id_token_encryption_enc_values_supported
    OPTIONAL. JSON array containing a list of the JWE encryption algorithms (enc values) supported by the
     OP for the ID Token to encode the Claims in a JWT [JWT].
userinfo_signing_alg_values_supported
    OPTIONAL. JSON array containing a list of the JWS [JWS] signing algorithms (alg values) [JWA]
    supported by the UserInfo Endpoint to encode the Claims in a JWT [JWT]. The value none MAY be included.
userinfo_encryption_alg_values_supported
    OPTIONAL. JSON array containing a list of the JWE [JWE] encryption algorithms (alg values) [JWA]
    supported by the UserInfo Endpoint to encode the Claims in a JWT [JWT].
userinfo_encryption_enc_values_supported
    OPTIONAL. JSON array containing a list of the JWE encryption algorithms (enc values) [JWA] supported
    by the UserInfo Endpoint to encode the Claims in a JWT [JWT].
request_object_signing_alg_values_supported
    OPTIONAL. JSON array containing a list of the JWS signing algorithms (alg values) supported by the OP
     for Request Objects, which are described in Section 6.1 of OpenID Connect Core 1.0 [OpenID.Core].
     These algorithms are used both when the Request Object is passed by value (using the request
     parameter) and when it is passed by reference (using the request_uri parameter). Servers SHOULD
     support none and RS256.
request_object_encryption_alg_values_supported
    OPTIONAL. JSON array containing a list of the JWE encryption algorithms (alg values) supported by the
     OP for Request Objects. These algorithms are used both when the Request Object is passed by value
     and when it is passed by reference.
request_object_encryption_enc_values_supported
    OPTIONAL. JSON array containing a list of the JWE encryption algorithms (enc values) supported by the
     OP for Request Objects. These algorithms are used both when the Request Object is passed by value
     and when it is passed by reference.
token_endpoint_auth_methods_supported
    OPTIONAL. JSON array containing a list of Client Authentication methods supported by this Token
    Endpoint. The options are client_secret_post, client_secret_basic, client_secret_jwt, and
    private_key_jwt, as described in Section 9 of OpenID Connect Core 1.0 [OpenID.Core]. Other
    authentication methods MAY be defined by extensions. If omitted, the default is client_secret_basic
    -- the HTTP Basic Authentication Scheme specified in Section 2.3.1 of OAuth 2.0 [RFC6749].
token_endpoint_auth_signing_alg_values_supported
    OPTIONAL. JSON array containing a list of the JWS signing algorithms (alg values) supported by the
    Token Endpoint for the signature on the JWT [JWT] used to authenticate the Client at the Token
    Endpoint for the private_key_jwt and client_secret_jwt authentication methods. Servers SHOULD support
     RS256. The value none MUST NOT be used.
display_values_supported
    OPTIONAL. JSON array containing a list of the display parameter values that the OpenID Provider
    supports. These values are described in Section 3.1.2.1 of OpenID Connect Core 1.0 [OpenID.Core].
claim_types_supported
    OPTIONAL. JSON array containing a list of the Claim Types that the OpenID Provider supports. These
    Claim Types are described in Section 5.6 of OpenID Connect Core 1.0 [OpenID.Core]. Values defined by
    this specification are normal, aggregated, and distributed. If omitted, the implementation supports
    only normal Claims.
claims_supported
    RECOMMENDED. JSON array containing a list of the Claim Names of the Claims that the OpenID Provider
    MAY be able to supply values for. Note that for privacy or other reasons, this might not be an
    exhaustive list.
service_documentation
    OPTIONAL. URL of a page containing human-readable information that developers might want or need to
    know when using the OpenID Provider. In particular, if the OpenID Provider does not support Dynamic
    Client Registration, then information on how to register Clients needs to be provided in this
    documentation.
claims_locales_supported
    OPTIONAL. Languages and scripts supported for values in Claims being returned, represented as a JSON
    array of BCP47 [RFC5646] language tag values. Not all languages and scripts are necessarily supported
     for all Claim values.
ui_locales_supported
    OPTIONAL. Languages and scripts supported for the user interface, represented as a JSON array of
    BCP47 [RFC5646] language tag values.
claims_parameter_supported
    OPTIONAL. Boolean value specifying whether the OP supports use of the claims parameter, with true
    indicating support. If omitted, the default value is false.
request_parameter_supported
    OPTIONAL. Boolean value specifying whether the OP supports use of the request parameter, with true
    indicating support. If omitted, the default value is false.
request_uri_parameter_supported
    OPTIONAL. Boolean value specifying whether the OP supports use of the request_uri parameter, with
    true indicating support. If omitted, the default value is true.
require_request_uri_registration
    OPTIONAL. Boolean value specifying whether the OP requires any request_uri values used to be
    pre-registered using the request_uris registration parameter. Pre-registration is REQUIRED when the
    value is true. If omitted, the default value is false.
op_policy_uri
    OPTIONAL. URL that the OpenID Provider provides to the person registering the Client to read about
    the OP's requirements on how the Relying Party can use the data provided by the OP. The registration
    process SHOULD display this URL to the person registering the Client if it is given.
op_tos_uri
    OPTIONAL. URL that the OpenID Provider provides to the person registering the Client to read about
    OpenID Provider's terms of service. The registration process SHOULD display this URL to the person
    registering the Client if it is given.
*/
    public static final String ISSUER="Issuer";
    public static final String AUTHORIZATION_ENDPOINT="Authorization_endpoint";
    public static final String TOKEN_ENDPOINT="Token_endpoint";
    public static final String USERINFO_ENDPOINT="Userinfo_endpoint";
    public static final String JWKS_URI="Jwks_uri";
    public static final String REGISTRATION_ENDPOINT="Registration_endpoint";
    public static final String SCOPES_SUPPORTED="Scopes_supported";
    public static final String RESPONSE_TYPES_SUPPORTED="Response_types_supported";
    public static final String RESPONSE_MODES_SUPPORTED="Response_modes_supported";
    public static final String GRANT_TYPES_SUPPORTED="Grant_types_supported";
    public static final String ACR_VALUES_SUPPORTED="Acr_values_supported";
    public static final String SUBJECT_TYPES_SUPPORTED="Subject_types_supported";
    public static final String ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED="ID_token_signing_alg_values_supported";
    public static final String ID_TOKEN_ENCRYPTION_ALG_VALUES_SUPPORTED="ID_token_encryption_alg_values_supported";
    public static final String ID_TOKEN_ENCRYPTION_ENC_VALUES_SUPPORTED="ID_token_encryption_enc_values_supported";
    public static final String USERINFO_SIGNING_ALG_VALUES_SUPPORTED="Userinfo_signing_alg_values_supported";
    public static final String USERINFO_ENCRYPTION_ALG_VALUES_SUPPORTED="Userinfo_encryption_alg_values_supported";
    public static final String USERINFO_ENCRYPTION_ENC_VALUES_SUPPORTED="Userinfo_encryption_enc_values_supported";
    public static final String REQUEST_OBJECT_SIGNING_ALG_VALUES_SUPPORTED="Request_object_signing_alg_values_supported";
    public static final String REQUEST_OBJECT_ENCRYPTION_ALG_VALUES_SUPPORTED="Request_object_encryption_alg_values_supported";
    public static final String REQUEST_OBJECT_ENCRYPTION_ENC_VALUES_SUPPORTED="Request_object_encryption_enc_values_supported";
    public static final String TOKEN_ENDPOINT_AUTH_METHODS_SUPPORTED="Token_endpoint_auth_methods_supported";
    public static final String TOKEN_ENDPOINT_AUTH_SIGNING_ALG_VALUES_SUPPORTED="Token_endpoint_auth_signing_alg_values_supported";
    public static final String DISPLAY_VALUES_SUPPORTED="Display_values_supported";
    public static final String CLAIM_TYPES_SUPPORTED="Claim_types_supported";
    public static final String CLAIMS_SUPPORTED="Claims_supported";
    public static final String SERVICE_DOCUMENTATION="Service_documentation";
    public static final String CLAIMS_LOCALES_SUPPORTED="Claims_locales_supported";
    public static final String UI_LOCALES_SUPPORTED="UI_locales_supported";
    public static final String CLAIMS_PARAMETER_SUPPORTED="Claims_parameter_supported";
    public static final String REQUEST_PARAMETER_SUPPORTED="Request_parameter_supported";
    public static final String REQUEST_URI_PARAMETER_SUPPORTED="Request_uri_parameter_supported";
    public static final String REQUIRE_REQUEST_URI_REGISTRATION="Require_request_uri_registration";
    public static final String OP_POLICY_URI="OP_policy_uri";
    public static final String OP_TOS_URI="OP_tos_uri";
}
