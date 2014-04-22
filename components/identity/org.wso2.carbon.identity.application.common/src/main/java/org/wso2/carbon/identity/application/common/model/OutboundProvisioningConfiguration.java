package org.wso2.carbon.identity.application.common.model;

public class OutboundProvisioningConfiguration {

    private FederatedIdentityProvider[] provisioningIdentityProviders;

    /**
     * 
     * @return
     */
    public FederatedIdentityProvider[] getProvisioningIdentityProviders() {
        return provisioningIdentityProviders;
    }

    /**
     * 
     * @param provisioningIdentityProviders
     */
    public void setProvisioningIdentityProviders(
            FederatedIdentityProvider[] provisioningIdentityProviders) {
        this.provisioningIdentityProviders = provisioningIdentityProviders;
    }
}
