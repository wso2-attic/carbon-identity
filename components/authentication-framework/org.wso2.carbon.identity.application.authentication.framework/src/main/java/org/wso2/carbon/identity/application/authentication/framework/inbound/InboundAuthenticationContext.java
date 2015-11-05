package org.wso2.carbon.identity.application.authentication.framework.inbound;

import java.io.Serializable;

public class InboundAuthenticationContext implements Serializable {

    private InboundAuthenticationRequest authenticationRequest;

    public InboundAuthenticationRequest getAuthenticationRequest() {
        return authenticationRequest;
    }

    public void setAuthenticationRequest(InboundAuthenticationRequest authenticationRequest) {
        this.authenticationRequest = authenticationRequest;
    }
}
