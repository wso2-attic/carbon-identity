package org.wso2.carbon.identity.application.common.model;

public class InboundProvisioningConfiguration {

    private String provisioningUserStore;
    private boolean provisioningEnabled;

    /**
     * 
     * @return
     */
    public String getProvisioningUserStore() {
        return provisioningUserStore;
    }

    /**
     * 
     * @param provisioningUserStore
     */
    public void setProvisioningUserStore(String provisioningUserStore) {
        this.provisioningUserStore = provisioningUserStore;
    }

	public boolean isProvisioningEnabled() {
		return provisioningEnabled;
	}

	public void setProvisioningEnabled(boolean provisioningEnabled) {
		this.provisioningEnabled = provisioningEnabled;
	}

}
