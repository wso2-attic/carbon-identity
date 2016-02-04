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
package org.wso2.carbon.identity.discovery.builders;


import org.wso2.carbon.base.ServerConfigurationException;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.discovery.OIDCDiscoveryEndPointException;
import org.wso2.carbon.identity.discovery.OIDProviderConfigResponse;
import org.wso2.carbon.identity.discovery.OIDProviderRequest;
import org.wso2.carbon.identity.oauth2.util.OAuth2Util;

/**
 * ProviderConfigBuilder builds the OIDProviderConfigResponse
 * giving the correct OprnIDConnect settings.
 * This should handle all the services to get the required data.
 */
public class ProviderConfigBuilder {

    public OIDProviderConfigResponse buildOIDProviderConfig(OIDProviderRequest request) throws
            OIDCDiscoveryEndPointException, ServerConfigurationException {
        OIDProviderConfigResponse providerConfig = new OIDProviderConfigResponse();
        providerConfig.setIssuer(IdentityUtil.getServerURL("",false,false));
        providerConfig.setAuthorizationEndpoint(OAuth2Util.OAuthURL.getOAuth2AuthzEPUrl());
        providerConfig.setTokenEndpoint(OAuth2Util.OAuthURL.getOAuth2TokenEPUrl());
        providerConfig.setUserinfoEndpoint(OAuth2Util.OAuthURL.getOAuth2UserInfoEPUrl());
        temp_Value_Settings(providerConfig);
        return providerConfig;
    }

    /**
     * This is an temporary method.
     * Provide additional services to get following parameters.
     */
    private void temp_Value_Settings(OIDProviderConfigResponse providerConfig) {
        String serverurl = IdentityUtil.getServerURL("",false,false);
        providerConfig.setTokenEndpointAuthMethodsSupported(new String[]{"client_secret_basic", "private_key_jwt"});
        providerConfig.setTokenEndpointAuthSigningAlgValuesSupported(new String[]{"RS256", "ES256"});
        providerConfig.setJwksUri(serverurl + "/jwks.json");
        providerConfig.setRegistrationEndpoint(serverurl + "/jwks.json");
        providerConfig.setScopesSupported(new String[]{"openid", "profile", "email", "address", "phone",
                "offline_access"});
        providerConfig.setResponseTypesSupported(new String[]{"code", "code id_token", "id_token", "token id_token"});
        providerConfig.setAcrValuesSupported(new String[]{"urn:mace:incommon:iap:silver",
                "urn:mace:incommon:iap:bronze"});
        providerConfig.setSubjectTypesSupported(new String[]{"public", "pairwise"});
        providerConfig.setUserinfoSigningAlgValuesSupported(new String[]{"RS256", "ES256", "HS256"});
        providerConfig.setUserinfoEncryptionAlgValuesSupported(new String[]{"RSA1_5", "A128KW"});
        providerConfig.setUserinfoEncryptionEncValuesSupported(new String[]{"A128CBC-HS256", "A128GCM"});
        providerConfig.setIdTokenSigningAlgValuesSupported(new String[]{"RS256", "ES256", "HS256"});
        providerConfig.setIdTokenEncryptionAlgValuesSupported(new String[]{"RSA1_5", "A128KW"});
        providerConfig.setIdTokenEncryptionEncValuesSupported(new String[]{"A128CBC-HS256", "A128GCM"});
        providerConfig.setRequestObjectSigningAlgValuesSupported(new String[]{"none", "RS256", "ES256"});
        providerConfig.setDisplayValuesSupported(new String[]{"page", "popup"});
        providerConfig.setClaimTypesSupported(new String[]{"normal", "distributed"});
        providerConfig.setClaimsSupported(new String[]{"sub", "iss", "auth_time", "acr", "name", "given_name",
                "family_name", "nickname", "profile", "picture", "website", "email", "email_verified", "locale",
                "zoneinfo", "http://example.info/claims/groups"});
        providerConfig.setClaimsParameterSupported("true");
        providerConfig.setServiceDocumentation(serverurl + "/documentation.html");
        providerConfig.setUiLocalesSupported(new String[]{"en-US", "en-GB", "en-CA", "fr-FR", "fr-CA"});

    }
}
