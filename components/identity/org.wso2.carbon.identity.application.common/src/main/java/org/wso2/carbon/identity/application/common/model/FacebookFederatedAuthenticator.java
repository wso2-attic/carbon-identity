package org.wso2.carbon.identity.application.common.model;

public class FacebookFederatedAuthenticator extends FederatedAuthenticator {

    /**
     * Facebook Client Id
     */
    private String clientId;

    /**
     * Facebook Client Secret
     */
    private String clientSecret;

    /**
     * 
     * @return
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * 
     * @param clientId
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * 
     * @return
     */
    public String getClientSecret() {
        return clientSecret;
    }

    /**
     * 
     * @param clientSecret
     */
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    /**
     * 
     * @return
     */
    public boolean isValid() {
        return clientId != null && clientId != null;
    }

    @Override
    public String getName() {
        return "facebook";
    }

}
