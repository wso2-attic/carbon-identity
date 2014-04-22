package org.wso2.carbon.identity.application.common.model;

public class SPMLProvisioningConnector extends ProvisioningConnector{

    @Override
    public String getName() {
        return "spml";
    }

    @Override
    public boolean isValid() {
        return false;
    }

}
