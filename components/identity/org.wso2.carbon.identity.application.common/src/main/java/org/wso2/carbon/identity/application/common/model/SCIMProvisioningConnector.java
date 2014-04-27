package org.wso2.carbon.identity.application.common.model;

public class SCIMProvisioningConnector extends ProvisioningConnector{

    @Override
    public String getName() {
        return "scim";
    }

    @Override
    public boolean isValid() {
        return true;
    }

}
