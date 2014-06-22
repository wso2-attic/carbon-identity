package org.wso2.carbon.identity.provisioning;

import java.io.Serializable;
import java.util.Map.Entry;

public class RuntimeProvisioningConfig implements Serializable{

    /**
     * 
     */
    private static final long serialVersionUID = -2629523092537958531L;
    private boolean blocking;
    private Entry<String, AbstractOutboundProvisioningConnector> provisioningConnectorEntry;

    /**
     * 
     * @return
     */
    public boolean isBlocking() {
        return blocking;
    }

    /**
     * 
     * @param blocking
     */
    public void setBlocking(boolean blocking) {
        this.blocking = blocking;
    }

    /**
     * 
     * @return
     */
    public Entry<String, AbstractOutboundProvisioningConnector> getProvisioningConnectorEntry() {
        return provisioningConnectorEntry;
    }

    /**
     * 
     * @param provisioningConnectorEntry
     */
    public void setProvisioningConnectorEntry(
            Entry<String, AbstractOutboundProvisioningConnector> provisioningConnectorEntry) {
        this.provisioningConnectorEntry = provisioningConnectorEntry;
    }

}
