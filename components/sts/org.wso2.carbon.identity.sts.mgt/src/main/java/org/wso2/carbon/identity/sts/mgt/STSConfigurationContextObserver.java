/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.sts.mgt;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.provider.IdentityProviderException;
import org.wso2.carbon.identity.sts.mgt.admin.STSConfigAdmin;
import org.wso2.carbon.utils.AbstractAxis2ConfigurationContextObserver;

/**
 * Observer for tenant ConfigurationContext creations. Configures the STS service of each tenant, mainly by setting
 * the attribute call back handler.
 */
public class STSConfigurationContextObserver extends AbstractAxis2ConfigurationContextObserver {

    private static final Log log = LogFactory.getLog(STSConfigurationContextObserver.class);

    @Override
    public void createdConfigurationContext(ConfigurationContext configurationContext) {
        int tenantID = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        try {
            if (log.isDebugEnabled()) {
                log.debug("Configuring the STS service for tenant: " + tenantDomain + "[" + tenantID + "]");
            }
            STSConfigAdmin.configureGenericSTS(configurationContext.getAxisConfiguration());
            configurationContext.getAxisConfiguration().addObservers(new TenantSTSObserver());
        } catch (IdentityProviderException e) {
            String msg = "Failed to configure STS service for tenant: " + tenantDomain + "[" + tenantID + "]";
            log.error(msg, e);
        }
    }
}
