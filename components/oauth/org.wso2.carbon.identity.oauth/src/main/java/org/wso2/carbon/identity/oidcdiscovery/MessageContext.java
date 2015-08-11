package org.wso2.carbon.identity.oidcdiscovery;


class MessageContext {

    private OIDProviderConfig configurations;

    private OIDProviderRequest request;

    public MessageContext(){
        this.request = new OIDProviderRequest();
        this.configurations = new OIDProviderConfig();
    }
    public MessageContext(OIDProviderRequest request){
        this.request = request;
        this.configurations = new OIDProviderConfig();
    }
    public void setConfigurations(OIDProviderConfig configurations) {
        this.configurations = configurations;
    }

    public OIDProviderConfig getConfigurations() {
        return configurations;
    }

    public OIDProviderRequest getRequest() {
        return request;
    }

    public void setRequest(OIDProviderRequest request) {
        this.request = request;
    }

}
