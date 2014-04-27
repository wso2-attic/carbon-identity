package org.wso2.carbon.identity.application.common.model;


public class InboundAuthenticationConfig {

    private InboundAuthenticationRequest[] inboundAuthenticationRequests;

    /**
     * 
     * @return
     */
    public InboundAuthenticationRequest[] getInboundAuthenticationRequests() {
        return inboundAuthenticationRequests;
    }

    /**
     * 
     * @param inboundAuthenticationRequest
     */
    public void setInboundAuthenticationRequests(
            InboundAuthenticationRequest[] inboundAuthenticationRequests) {
        this.inboundAuthenticationRequests = inboundAuthenticationRequests;
    }
}
