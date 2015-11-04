/*
 *Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */
package org.wso2.carbon.identity.application.mgt;

import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.InboundAuthenticationRequestConfig;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 *
 */
public abstract class AbstractInboundAuthenticatorConfig {

    private final static String DEFAULT_TENANT_NAME = "carbon.super";

    /**
     *
     * @return
     */
    public abstract String getName();

    /**
     *
     * @return
     */
    public abstract String getType();

    /**
     *
     * @return
     */
    public abstract Property[] getConfigurationProperties();

    /**
     *
     * @param tenantDomain
     * @param spIssuer
     * @param propertyNames
     * @return
     * @throws org.wso2.carbon.identity.application.common.IdentityApplicationManagementException
     */
    protected Map<String, String> getPropertyValues(String tenantDomain, String spIssuer, List<String> propertyNames)
            throws IdentityApplicationManagementException {

        ServiceProvider serviceProvider = ApplicationMgtSystemConfig.getInstance().getApplicationDAO()
                .getApplication(spIssuer, tenantDomain);

        if (serviceProvider == null) {
            throw new IdentityApplicationManagementException(
                    "No service provider exists in the provided tenant, with the given issuer id.");
        }

        Map<String, String> propKeyValueMap = new HashMap<String, String>();

        InboundAuthenticationRequestConfig[] inboundAuthReqConfigs = serviceProvider.getInboundAuthenticationConfig()
                .getInboundAuthenticationRequestConfigs();

        if (inboundAuthReqConfigs != null && inboundAuthReqConfigs.length > 0) {
            for (InboundAuthenticationRequestConfig authConfig : inboundAuthReqConfigs) {
                if (authConfig.getInboundAuthType().equals(getType())
                        && authConfig.getInboundAuthKey().equals(getName())) {
                    Property[] properties = authConfig.getProperties();
                    for (Property prop : properties) {
                        if (propertyNames.contains(prop.getName())) {
                            propKeyValueMap.put(prop.getName(), prop.getValue());
                        }
                    }
                }
            }
        }

        return propKeyValueMap;
    }
}