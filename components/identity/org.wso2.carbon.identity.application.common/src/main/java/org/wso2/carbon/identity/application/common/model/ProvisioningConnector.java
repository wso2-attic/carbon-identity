package org.wso2.carbon.identity.application.common.model;

import org.wso2.carbon.identity.application.common.util.IdentityCommonUtil;

public class ProvisioningConnector {

    public final static String PROP_ENABLED = "enabled";

    protected ProvisioningProperty[] provisioningProperties;
    protected String name;

    /**
     * 
     * @return
     */
    public ProvisioningProperty[] getProvisioningProperties() {
        return provisioningProperties;
    }

    /**
     * 
     * @param provisioningProperties
     */
    public void setProvisioningProperties(ProvisioningProperty[] provisioningProperties) {
        this.provisioningProperties = provisioningProperties;
    }

    /**
     * 
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * 
     * @return
     */
    public boolean isValid() {
        return false;
    }

    /**
     * 
     * @return
     */
    public boolean isEnabled() {
        if (provisioningProperties != null) {
            for (ProvisioningProperty proProperty : provisioningProperties) {
                if (PROP_ENABLED.equals(proProperty.getName())) {
                    if ("true".equals(proProperty.getValue())) {
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 
     * @param enabled
     */
    public void setEnabled(boolean enabled) {

        ProvisioningProperty proProperty = new ProvisioningProperty();
        proProperty.setName(PROP_ENABLED);
        if (enabled) {
            proProperty.setValue("true");
        } else {
            proProperty.setValue("true");
        }

        if (provisioningProperties != null && provisioningProperties.length > 0) {
            provisioningProperties = IdentityCommonUtil.concatArrays(this.provisioningProperties,
                    new ProvisioningProperty[] { proProperty });
        } else {
            provisioningProperties = new ProvisioningProperty[] { proProperty };
        }

    }

    /**
     * 
     * @param string
     */
    public void setName(String name) {
        this.name = name;
    }
}
