package org.wso2.carbon.identity.application.common.model;

public class GoogleAppsProvisioningConnector extends ProvisioningConnector{

    @Override
    public String getName() {
        return "googleapps";
    }

    @Override
    public boolean isValid() {
        return true;
    }

}
