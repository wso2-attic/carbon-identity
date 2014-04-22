package org.wso2.carbon.identity.application.common.model;


public class LocalAndOutboundAuthenticationConfig {

    private AuthenticationStep[] authenticationSteps;
    private String authenticationType;
    

    /**
     * 
     * @return
     */
    public AuthenticationStep[] getAuthenticationSteps() {
        return authenticationSteps;
    }

    /**
     * 
     * @param authSteps
     */
    public void setAuthenticationSteps(AuthenticationStep[] authenticationSteps) {
        this.authenticationSteps = authenticationSteps;
    }

    /**
     * 
     * @return
     */
    public String getAuthenticationType() {
        return authenticationType;
    }

    /**
     * 
     * @param authenticationType
     */
    public void setAuthenticationType(String authenticationType) {
        this.authenticationType = authenticationType;
    }

}
