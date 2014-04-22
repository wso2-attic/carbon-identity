package org.wso2.carbon.identity.application.common.model;

public class OpenIDConnectFederatedAuthenticator extends FederatedAuthenticator {

    /**
     * OAuth2 Authorize End-point
     */
    private String authzEndpointUrl;

    /**
     * OAuth2 Token End-point
     */
    private String tokenEndpointUrl;

    /**
     * OAuth2 Client Identifier
     */
    private String clientId;

    /**
     * OAuth2 Client Secret
     */
    private String clientSecret;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getAuthzEndpointUrl() {
        return authzEndpointUrl;
    }

    public void setAuthzEndpointUrl(String authzEndpointUrl) {
        this.authzEndpointUrl = authzEndpointUrl;
    }

    public String getTokenEndpointUrl() {
        return tokenEndpointUrl;
    }

    public void setTokenEndpointUrl(String tokenEndpointUrl) {
        this.tokenEndpointUrl = tokenEndpointUrl;
    }

    @Override
    public boolean isValid() {
        return tokenEndpointUrl != null && authzEndpointUrl != null && clientId != null
                && clientSecret != null;
    }

    @Override
    public String getName() {
        return "openidconnect";
    }

}
