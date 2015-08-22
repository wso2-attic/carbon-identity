package org.wso2.carbon.identity.oidcdiscovery.builders;


import org.wso2.carbon.base.ServerConfigurationException;
import org.wso2.carbon.identity.oauth2.util.OAuth2Util;
import org.wso2.carbon.identity.oidcdiscovery.OIDCDiscoveryEndPointException;
import org.wso2.carbon.identity.oidcdiscovery.OIDProviderConfigResponse;
import org.wso2.carbon.identity.oidcdiscovery.OIDProviderRequest;

public class ProviderConfigBuilder {

    public OIDProviderConfigResponse buildOIDProviderConfig(OIDProviderRequest request) throws
            OIDCDiscoveryEndPointException, ServerConfigurationException {
        OIDProviderConfigResponse providerConfig = new OIDProviderConfigResponse();
        String tenantName = request.getTenant();
        providerConfig.setAuthorization_endpoint(OAuth2Util.OAuthURL.getOAuth2AuthzEPUrl());
        providerConfig.setToken_endpoint(OAuth2Util.OAuthURL.getOAuth2TokenEPUrl());
        providerConfig.setUserinfo_endpoint(OAuth2Util.OAuthURL.getOAuth2UserInfoEPUrl());
        return providerConfig;
    }

}
