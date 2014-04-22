package org.wso2.carbon.identity.application.common.model;

public class JustInTimeProvisioningConfiguration extends InboundProvisioningConfiguration {

    private String userStoreClaimUri;

    /**
     * 
     * @return
     */
    public String getUserStoreClaimUri() {
        return userStoreClaimUri;
    }

    /**
     * 
     * @param userStoreClaimUri
     */
    public void setUserStoreClaimUri(String userStoreClaimUri) {
        this.userStoreClaimUri = userStoreClaimUri;
    }

}
