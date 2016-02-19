package org.wso2.carbon.identity.analytic.authn.model;

public class AuthnStep {

    private int stepNumber;
    private boolean authnStepSuccess;
    private String idp;
    private String authenticator;

    public int getStepNumber() {

        return stepNumber;
    }

    public void setStepNumber(int stepNumber) {

        this.stepNumber = stepNumber;
    }

    public boolean isAuthnStepSuccess() {

        return authnStepSuccess;
    }

    public void setAuthnStepSuccess(boolean authnStepSuccess) {

        this.authnStepSuccess = authnStepSuccess;
    }

    public String getIdp() {

        return idp;
    }

    public void setIdp(String idp) {

        this.idp = idp;
    }

    public String getAuthenticator() {

        return authenticator;
    }

    public void setAuthenticator(String authenticator) {

        this.authenticator = authenticator;
    }
}
