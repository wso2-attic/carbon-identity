package org.wso2.carbon.identity.application.common.model;

public class SalesforceProvisioningConnector extends ProvisioningConnector {

    @Override
    public String getName() {
        return "salesforce";
    }

    @Override
    public boolean isValid() {
        return true;
    }

}
