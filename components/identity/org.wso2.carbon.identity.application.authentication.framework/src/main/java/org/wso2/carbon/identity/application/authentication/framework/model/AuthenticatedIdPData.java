package org.wso2.carbon.identity.application.authentication.framework.model;

import java.io.Serializable;
import java.util.Map;

import org.wso2.carbon.identity.application.authentication.framework.config.model.AuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;

public class AuthenticatedIdPData implements Serializable {
    
    private static final long serialVersionUID = -1778751155155790874L;
        
    private String idpName;
    private AuthenticatorConfig authenticator;
    private String username;
    private Map<ClaimMapping, String> userAttributes;
    
    public String getIdpName() {
        return idpName;
    }
    
    public void setIdpName(String idpName) {
        this.idpName = idpName;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }

    public Map<ClaimMapping, String> getUserAttributes() {
        return userAttributes;
    }

    public void setUserAttributes(Map<ClaimMapping, String> userAttributes) {
        this.userAttributes = userAttributes;
    }

    public AuthenticatorConfig getAuthenticator() {
        return authenticator;
    }

    public void setAuthenticator(AuthenticatorConfig authenticator) {
        this.authenticator = authenticator;
    }
}
