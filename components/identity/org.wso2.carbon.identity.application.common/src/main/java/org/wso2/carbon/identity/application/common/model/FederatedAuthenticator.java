package org.wso2.carbon.identity.application.common.model;

public class FederatedAuthenticator extends LocalAuthenticator {

    protected boolean usetIdInClaim;
    protected String name;

    public void setName(String name) {
        this.name = name;
    }

    /**
     * 
     * @return
     */
    public boolean isUsetIdInClaim() {
        return usetIdInClaim;
    }

    /**
     * 
     * @param usetIdInClaim
     */
    public void setUsetIdInClaim(boolean usetIdInClaim) {
        this.usetIdInClaim = usetIdInClaim;
    }

    /**
     * 
     * @return
     */
    public boolean isValid() {
        return true;
    }

    /**
     * 
     */
    public String getName() {
        return name;
    }

}
