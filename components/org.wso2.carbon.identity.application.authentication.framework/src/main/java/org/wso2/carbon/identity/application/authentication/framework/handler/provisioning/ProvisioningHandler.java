package org.wso2.carbon.identity.application.authentication.framework.handler.provisioning;

import java.util.List;
import java.util.Map;

import org.wso2.carbon.identity.application.authentication.framework.exception.FrameworkException;

public interface ProvisioningHandler {

    /**
     * 
     * @param roles
     * @param subject
     * @param attributes
     * @param provisioningUserStoreId
     * @param tenantDomain
     * @throws FrameworkException
     */
    public void handle(List<String> roles, String subject, Map<String, String> attributes,
            String provisioningUserStoreId, String tenantDomain) throws FrameworkException;
}
