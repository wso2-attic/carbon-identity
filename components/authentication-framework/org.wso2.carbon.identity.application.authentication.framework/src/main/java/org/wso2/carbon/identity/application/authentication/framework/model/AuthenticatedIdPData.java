package org.wso2.carbon.identity.application.authentication.framework.model;

import org.wso2.carbon.identity.application.authentication.framework.config.model.AuthenticatorConfig;

import java.io.Serializable;

public class AuthenticatedIdPData implements Serializable {

    private static final long serialVersionUID = -1778751155155790874L;

    private String idpName;
    private AuthenticatorConfig authenticator;
    private AuthenticatedUser user;

    public String getIdpName() {
        return idpName;
    }

    public void setIdpName(String idpName) {
        this.idpName = idpName;
    }

    public AuthenticatedUser getUser() {
        return user;
    }

    public void setUser(AuthenticatedUser user) {
        this.user = user;
    }

    public AuthenticatorConfig getAuthenticator() {
        return authenticator;
    }

    public void setAuthenticator(AuthenticatorConfig authenticator) {
        this.authenticator = authenticator;
    }
}
