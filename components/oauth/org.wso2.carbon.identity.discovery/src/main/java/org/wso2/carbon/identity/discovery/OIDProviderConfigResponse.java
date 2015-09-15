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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.discovery;


import java.util.HashMap;
import java.util.Map;

public class OIDProviderConfigResponse {

    private String issuer;
    private String authorization_endpoint;
    private String token_endpoint;
    private String userinfo_endpoint;
    private String jwks_uri;
    private String registration_endpoint;
    private String[] scopes_supported;
    private String[] response_types_supported;
    private String[] response_modes_supported;
    private String[] grant_types_supported;
    private String[] acr_values_supported;
    private String[] subject_types_supported;
    private String[] id_token_signing_alg_values_supported;
    private String[] id_token_encryption_alg_values_supported;
    private String[] id_token_encryption_enc_values_supported;
    private String[] userinfo_signing_alg_values_supported;
    private String[] userinfo_encryption_alg_values_supported;
    private String[] userinfo_encryption_enc_values_supported;
    private String[] request_object_signing_alg_values_supported;
    private String[] request_object_encryption_alg_values_supported;
    private String[] request_object_encryption_enc_values_supported;
    private String[] token_endpoint_auth_methods_supported;
    private String[] token_endpoint_auth_signing_alg_values_supported;
    private String[] display_values_supported;
    private String[] claim_types_supported;
    private String[] claims_supported;
    private String service_documentation;
    private String[] claims_locales_supported;
    private String[] ui_locales_supported;
    private String claims_parameter_supported;
    private String request_parameter_supported;
    private String request_uri_parameter_supported;
    private String require_request_uri_registration;
    private String op_policy_uri;
    private String op_tos_uri;


    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAuthorization_endpoint() {
        return authorization_endpoint;
    }

    public void setAuthorization_endpoint(String authorization_endpoint) {
        this.authorization_endpoint = authorization_endpoint;
    }

    public String getToken_endpoint() {
        return token_endpoint;
    }

    public void setToken_endpoint(String token_endpoint) {
        this.token_endpoint = token_endpoint;
    }

    public String getUserinfo_endpoint() {
        return userinfo_endpoint;
    }

    public void setUserinfo_endpoint(String userinfo_endpoint) {
        this.userinfo_endpoint = userinfo_endpoint;
    }

    public String getJwks_uri() {
        return jwks_uri;
    }

    public void setJwks_uri(String jwks_uri) {
        this.jwks_uri = jwks_uri;
    }

    public String getRegistration_endpoint() {
        return registration_endpoint;
    }

    public void setRegistration_endpoint(String registration_endpoint) {
        this.registration_endpoint = registration_endpoint;
    }

    public String[] getScopes_supported() {
        return scopes_supported;
    }

    public void setScopes_supported(String[] scopes_supported) {
        this.scopes_supported = scopes_supported;
    }

    public String[] getResponse_types_supported() {
        return response_types_supported;
    }

    public void setResponse_types_supported(String[] response_types_supported) {
        this.response_types_supported = response_types_supported;
    }

    public String[] getResponse_modes_supported() {
        return response_modes_supported;
    }

    public void setResponse_modes_supported(String[] response_modes_supported) {
        this.response_modes_supported = response_modes_supported;
    }

    public String[] getGrant_types_supported() {
        return grant_types_supported;
    }

    public void setGrant_types_supported(String[] grant_types_supported) {
        this.grant_types_supported = grant_types_supported;
    }

    public String[] getAcr_values_supported() {
        return acr_values_supported;
    }

    public void setAcr_values_supported(String[] acr_values_supported) {
        this.acr_values_supported = acr_values_supported;
    }

    public String[] getSubject_types_supported() {
        return subject_types_supported;
    }

    public void setSubject_types_supported(String[] subject_types_supported) {
        this.subject_types_supported = subject_types_supported;
    }

    public String[] getId_token_signing_alg_values_supported() {
        return id_token_signing_alg_values_supported;
    }

    public void setId_token_signing_alg_values_supported(String[] id_token_signing_alg_values_supported) {
        this.id_token_signing_alg_values_supported = id_token_signing_alg_values_supported;
    }

    public String[] getId_token_encryption_alg_values_supported() {
        return id_token_encryption_alg_values_supported;
    }

    public void setId_token_encryption_alg_values_supported(String[] id_token_encryption_alg_values_supported) {
        this.id_token_encryption_alg_values_supported = id_token_encryption_alg_values_supported;
    }

    public String[] getId_token_encryption_enc_values_supported() {
        return id_token_encryption_enc_values_supported;
    }

    public void setId_token_encryption_enc_values_supported(String[] id_token_encryption_enc_values_supported) {
        this.id_token_encryption_enc_values_supported = id_token_encryption_enc_values_supported;
    }

    public String[] getUserinfo_signing_alg_values_supported() {
        return userinfo_signing_alg_values_supported;
    }

    public void setUserinfo_signing_alg_values_supported(String[] userinfo_signing_alg_values_supported) {
        this.userinfo_signing_alg_values_supported = userinfo_signing_alg_values_supported;
    }

    public String[] getUserinfo_encryption_alg_values_supported() {
        return userinfo_encryption_alg_values_supported;
    }

    public void setUserinfo_encryption_alg_values_supported(String[] userinfo_encryption_alg_values_supported) {
        this.userinfo_encryption_alg_values_supported = userinfo_encryption_alg_values_supported;
    }

    public String[] getUserinfo_encryption_enc_values_supported() {
        return userinfo_encryption_enc_values_supported;
    }

    public void setUserinfo_encryption_enc_values_supported(String[] userinfo_encryption_enc_values_supported) {
        this.userinfo_encryption_enc_values_supported = userinfo_encryption_enc_values_supported;
    }

    public String[] getRequest_object_signing_alg_values_supported() {
        return request_object_signing_alg_values_supported;
    }

    public void setRequest_object_signing_alg_values_supported(String[] request_object_signing_alg_values_supported) {
        this.request_object_signing_alg_values_supported = request_object_signing_alg_values_supported;
    }

    public String[] getRequest_object_encryption_alg_values_supported() {
        return request_object_encryption_alg_values_supported;
    }

    public void setRequest_object_encryption_alg_values_supported(String[]
                                                                          request_object_encryption_alg_values_supported) {
        this.request_object_encryption_alg_values_supported = request_object_encryption_alg_values_supported;
    }

    public String[] getRequest_object_encryption_enc_values_supported() {
        return request_object_encryption_enc_values_supported;
    }

    public void setRequest_object_encryption_enc_values_supported(String[]
                                                                          request_object_encryption_enc_values_supported) {
        this.request_object_encryption_enc_values_supported = request_object_encryption_enc_values_supported;
    }

    public String[] getToken_endpoint_auth_methods_supported() {
        return token_endpoint_auth_methods_supported;
    }

    public void setToken_endpoint_auth_methods_supported(String[] token_endpoint_auth_methods_supported) {
        this.token_endpoint_auth_methods_supported = token_endpoint_auth_methods_supported;
    }

    public String[] getToken_endpoint_auth_signing_alg_values_supported() {
        return token_endpoint_auth_signing_alg_values_supported;
    }

    public void setToken_endpoint_auth_signing_alg_values_supported(String[]
                                                                            token_endpoint_auth_signing_alg_values_supported) {
        this.token_endpoint_auth_signing_alg_values_supported = token_endpoint_auth_signing_alg_values_supported;
    }

    public String[] getDisplay_values_supported() {
        return display_values_supported;
    }

    public void setDisplay_values_supported(String[] display_values_supported) {
        this.display_values_supported = display_values_supported;
    }

    public String[] getClaim_types_supported() {
        return claim_types_supported;
    }

    public void setClaim_types_supported(String[] claim_types_supported) {
        this.claim_types_supported = claim_types_supported;
    }

    public String[] getClaims_supported() {
        return claims_supported;
    }

    public void setClaims_supported(String[] claims_supported) {
        this.claims_supported = claims_supported;
    }

    public String getService_documentation() {
        return service_documentation;
    }

    public void setService_documentation(String service_documentation) {
        this.service_documentation = service_documentation;
    }

    public String[] getClaims_locales_supported() {
        return claims_locales_supported;
    }

    public void setClaims_locales_supported(String[] claims_locales_supported) {
        this.claims_locales_supported = claims_locales_supported;
    }

    public String[] getUi_locales_supported() {
        return ui_locales_supported;
    }

    public void setUi_locales_supported(String[] ui_locales_supported) {
        this.ui_locales_supported = ui_locales_supported;
    }

    public String getClaims_parameter_supported() {
        return claims_parameter_supported;
    }

    public void setClaims_parameter_supported(String claims_parameter_supported) {
        this.claims_parameter_supported = claims_parameter_supported;
    }

    public String getRequest_parameter_supported() {
        return request_parameter_supported;
    }

    public void setRequest_parameter_supported(String request_parameter_supported) {
        this.request_parameter_supported = request_parameter_supported;
    }

    public String getRequest_uri_parameter_supported() {
        return request_uri_parameter_supported;
    }

    public void setRequest_uri_parameter_supported(String request_uri_parameter_supported) {
        this.request_uri_parameter_supported = request_uri_parameter_supported;
    }

    public String getRequire_request_uri_registration() {
        return require_request_uri_registration;
    }

    public void setRequire_request_uri_registration(String require_request_uri_registration) {
        this.require_request_uri_registration = require_request_uri_registration;
    }

    public String getOp_policy_uri() {
        return op_policy_uri;
    }

    public void setOp_policy_uri(String op_policy_uri) {
        this.op_policy_uri = op_policy_uri;
    }

    public String getOp_tos_uri() {
        return op_tos_uri;
    }

    public void setOp_tos_uri(String op_tos_uri) {
        this.op_tos_uri = op_tos_uri;
    }

    public Map<String, Object> getConfigMap() {
        Map<String, Object> configMap = new HashMap<String, Object>();
        configMap.put(DiscoveryConstants.ISSUER.toLowerCase(), this.issuer);
        configMap.put(DiscoveryConstants.ACR_VALUES_SUPPORTED.toLowerCase(), this.acr_values_supported);
        configMap.put(DiscoveryConstants.AUTHORIZATION_ENDPOINT.toLowerCase(), this.authorization_endpoint);
        configMap.put(DiscoveryConstants.CLAIM_TYPES_SUPPORTED.toLowerCase(), this.claim_types_supported);
        configMap.put(DiscoveryConstants.CLAIMS_LOCALES_SUPPORTED.toLowerCase(), this.claims_locales_supported);
        configMap.put(DiscoveryConstants.CLAIMS_PARAMETER_SUPPORTED.toLowerCase(), this.claims_parameter_supported);
        configMap.put(DiscoveryConstants.CLAIMS_SUPPORTED.toLowerCase(), this.claims_supported);
        configMap.put(DiscoveryConstants.DISPLAY_VALUES_SUPPORTED.toLowerCase(), this.display_values_supported);
        configMap.put(DiscoveryConstants.GRANT_TYPES_SUPPORTED.toLowerCase(), this.grant_types_supported);
        configMap.put(DiscoveryConstants.ID_TOKEN_ENCRYPTION_ALG_VALUES_SUPPORTED.toLowerCase(), this
                .id_token_encryption_alg_values_supported);
        configMap.put(DiscoveryConstants.ID_TOKEN_ENCRYPTION_ENC_VALUES_SUPPORTED.toLowerCase(), this
                .id_token_encryption_enc_values_supported);
        configMap.put(DiscoveryConstants.ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED.toLowerCase(), this
                .id_token_signing_alg_values_supported);
        configMap.put(DiscoveryConstants.JWKS_URI.toLowerCase(), this.jwks_uri);
        configMap.put(DiscoveryConstants.OP_POLICY_URI.toLowerCase(), this.op_policy_uri);
        configMap.put(DiscoveryConstants.OP_TOS_URI.toLowerCase(), this.op_tos_uri);
        configMap.put(DiscoveryConstants.REGISTRATION_ENDPOINT.toLowerCase(), this.registration_endpoint);
        configMap.put(DiscoveryConstants.REQUEST_OBJECT_ENCRYPTION_ALG_VALUES_SUPPORTED.toLowerCase(), this
                .request_object_encryption_alg_values_supported);
        configMap.put(DiscoveryConstants.REQUEST_OBJECT_ENCRYPTION_ENC_VALUES_SUPPORTED.toLowerCase(), this
                .request_object_encryption_enc_values_supported);
        configMap.put(DiscoveryConstants.REQUEST_OBJECT_SIGNING_ALG_VALUES_SUPPORTED.toLowerCase(), this
                .request_object_signing_alg_values_supported);
        configMap.put(DiscoveryConstants.REQUEST_PARAMETER_SUPPORTED.toLowerCase(), this.request_parameter_supported);
        configMap.put(DiscoveryConstants.REQUEST_URI_PARAMETER_SUPPORTED.toLowerCase(), this
                .request_uri_parameter_supported);
        configMap.put(DiscoveryConstants.REQUIRE_REQUEST_URI_REGISTRATION.toLowerCase(), this
                .require_request_uri_registration);
        configMap.put(DiscoveryConstants.RESPONSE_MODES_SUPPORTED.toLowerCase(), this.response_modes_supported);
        configMap.put(DiscoveryConstants.RESPONSE_TYPES_SUPPORTED.toLowerCase(), this.response_types_supported);
        configMap.put(DiscoveryConstants.SCOPES_SUPPORTED.toLowerCase(), this.scopes_supported);
        configMap.put(DiscoveryConstants.SERVICE_DOCUMENTATION.toLowerCase(), this.service_documentation);
        configMap.put(DiscoveryConstants.SUBJECT_TYPES_SUPPORTED.toLowerCase(), this.subject_types_supported);
        configMap.put(DiscoveryConstants.TOKEN_ENDPOINT.toLowerCase(), this.token_endpoint);
        configMap.put(DiscoveryConstants.TOKEN_ENDPOINT_AUTH_METHODS_SUPPORTED.toLowerCase(), this
                .token_endpoint_auth_methods_supported);
        configMap.put(DiscoveryConstants.TOKEN_ENDPOINT_AUTH_SIGNING_ALG_VALUES_SUPPORTED.toLowerCase(), this
                .token_endpoint_auth_signing_alg_values_supported);
        configMap.put(DiscoveryConstants.UI_LOCALES_SUPPORTED.toLowerCase(), this.ui_locales_supported);
        configMap.put(DiscoveryConstants.USERINFO_ENCRYPTION_ALG_VALUES_SUPPORTED.toLowerCase(), this
                .userinfo_encryption_alg_values_supported);
        configMap.put(DiscoveryConstants.USERINFO_ENCRYPTION_ENC_VALUES_SUPPORTED.toLowerCase(), this
                .userinfo_encryption_enc_values_supported);
        configMap.put(DiscoveryConstants.USERINFO_ENDPOINT.toLowerCase(), this.userinfo_endpoint);
        configMap.put(DiscoveryConstants.USERINFO_SIGNING_ALG_VALUES_SUPPORTED.toLowerCase(), this
                .userinfo_signing_alg_values_supported);
        return configMap;
    }


}
