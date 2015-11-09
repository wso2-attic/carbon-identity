package org.wso2.carbon.identity.application.authentication.framework.inbound;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class InboundAuthenticationContext implements Serializable {

    private InboundAuthenticationRequest authenticationRequest;
    private Map<String, Object> properties = new HashMap<String, Object>();

    public InboundAuthenticationRequest getAuthenticationRequest() {
        return authenticationRequest;
    }

    public void setAuthenticationRequest(InboundAuthenticationRequest authenticationRequest) {
        this.authenticationRequest = authenticationRequest;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
}
