package org.wso2.carbon.identity.discovery;


class MessageContext {

    private OIDProviderConfigResponse configurations;

    private OIDProviderRequest request;

    public MessageContext() {
        this.request = new OIDProviderRequest();
        this.configurations = new OIDProviderConfigResponse();
    }

    public MessageContext(OIDProviderRequest request) {
        this.request = request;
        this.configurations = new OIDProviderConfigResponse();
    }

    public void setConfigurations(OIDProviderConfigResponse configurations) {
        this.configurations = configurations;
    }

    public OIDProviderConfigResponse getConfigurations() {
        return configurations;
    }

    public OIDProviderRequest getRequest() {
        return request;
    }

    public void setRequest(OIDProviderRequest request) {
        this.request = request;
    }

}
