package org.wso2.carbon.identity.discovery.builders;


import org.wso2.carbon.base.ServerConfigurationException;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.discovery.OIDCDiscoveryEndPointException;
import org.wso2.carbon.identity.discovery.OIDProviderConfigResponse;
import org.wso2.carbon.identity.discovery.OIDProviderRequest;
import org.wso2.carbon.identity.oauth2.util.OAuth2Util;

public class ProviderConfigBuilder {

    public OIDProviderConfigResponse buildOIDProviderConfig(OIDProviderRequest request) throws
            OIDCDiscoveryEndPointException, ServerConfigurationException {
        OIDProviderConfigResponse providerConfig = new OIDProviderConfigResponse();
        providerConfig.setIssuer(IdentityUtil.getServerURL(""));
        providerConfig.setAuthorization_endpoint(OAuth2Util.OAuthURL.getOAuth2AuthzEPUrl());
        providerConfig.setToken_endpoint(OAuth2Util.OAuthURL.getOAuth2TokenEPUrl());
        providerConfig.setUserinfo_endpoint(OAuth2Util.OAuthURL.getOAuth2UserInfoEPUrl());
        temp_Value_Settings(providerConfig);
        return providerConfig;
    }

    /**
     * This is an temporary methods.
     * Provide additional services to get following parameters.
     */
    private void temp_Value_Settings(OIDProviderConfigResponse providerConfig) {
        String serverurl = IdentityUtil.getServerURL("");
        providerConfig.setToken_endpoint_auth_methods_supported(new String[]{"client_secret_basic", "private_key_jwt"});
        providerConfig.setToken_endpoint_auth_signing_alg_values_supported(new String[]{"RS256", "ES256"});
        providerConfig.setJwks_uri(serverurl + "/jwks.json");
        providerConfig.setRegistration_endpoint(serverurl + "/jwks.json");
        providerConfig.setScopes_supported(new String[]{"openid", "profile", "email", "address", "phone",
                "offline_access"});
        providerConfig.setResponse_types_supported(new String[]{"code", "code id_token", "id_token", "token id_token"});
        providerConfig.setAcr_values_supported(new String[]{"urn:mace:incommon:iap:silver",
                "urn:mace:incommon:iap:bronze"});
        providerConfig.setSubject_types_supported(new String[]{"public", "pairwise"});
        providerConfig.setUserinfo_signing_alg_values_supported(new String[]{"RS256", "ES256", "HS256"});
        providerConfig.setUserinfo_encryption_alg_values_supported(new String[]{"RSA1_5", "A128KW"});
        providerConfig.setUserinfo_encryption_enc_values_supported(new String[]{"A128CBC-HS256", "A128GCM"});
        providerConfig.setId_token_signing_alg_values_supported(new String[]{"RS256", "ES256", "HS256"});
        providerConfig.setId_token_encryption_alg_values_supported(new String[]{"RSA1_5", "A128KW"});
        providerConfig.setId_token_encryption_enc_values_supported(new String[]{"A128CBC-HS256", "A128GCM"});
        providerConfig.setRequest_object_signing_alg_values_supported(new String[]{"none", "RS256", "ES256"});
        providerConfig.setDisplay_values_supported(new String[]{"page", "popup"});
        providerConfig.setClaim_types_supported(new String[]{"normal", "distributed"});
        providerConfig.setClaims_supported(new String[]{"sub", "iss", "auth_time", "acr", "name", "given_name",
                "family_name", "nickname", "profile", "picture", "website", "email", "email_verified", "locale",
                "zoneinfo", "http://example.info/claims/groups"});
        providerConfig.setClaims_parameter_supported("true");
        providerConfig.setService_documentation(serverurl+"/documentation.html");
        providerConfig.setUi_locales_supported(new String[]{"en-US", "en-GB", "en-CA", "fr-FR", "fr-CA"});

    }
}
