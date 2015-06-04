/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.provisioning.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.mgt.listener.ApplicationMgtListener;
import org.wso2.carbon.identity.provisioning.cache.ServiceProviderProvisioningConnectorCache;
import org.wso2.carbon.identity.provisioning.cache.ServiceProviderProvisioningConnectorCacheEntry;
import org.wso2.carbon.identity.provisioning.cache.ServiceProviderProvisioningConnectorCacheKey;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

public class ApplicationMgtProvisioningListener implements ApplicationMgtListener {

    private static Log log = LogFactory.getLog(ApplicationMgtProvisioningListener.class);

    @Override
    public void createApplication(ServiceProvider serviceProvider) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateApplication(ServiceProvider serviceProvider) {
        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        log.debug("Clearing cache entry for " + serviceProvider.getApplicationName());
        destroySpProvConnectors(serviceProvider.getApplicationName(), tenantDomain);
    }

    @Override
    public void deleteApplication(String applicationName) {
        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        log.debug("Clearing cache entry for " + applicationName);
        destroySpProvConnectors(applicationName, tenantDomain);
    }

    private void destroySpProvConnectors(String applicationName, String tenantDomain) {

        try {

            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext
                    .getThreadLocalCarbonContext();
            carbonContext.setTenantId(MultitenantConstants.SUPER_TENANT_ID);
            carbonContext.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);

            // reading from the cache
            ServiceProviderProvisioningConnectorCacheKey key =
                    new ServiceProviderProvisioningConnectorCacheKey(applicationName, tenantDomain);

            ServiceProviderProvisioningConnectorCacheEntry entry = (ServiceProviderProvisioningConnectorCacheEntry) ServiceProviderProvisioningConnectorCache
                    .getInstance().getValueFromCache(key);

            // cache hit
            if (entry != null) {
                ServiceProviderProvisioningConnectorCache.getInstance().clearCacheEntry(key);
                if (log.isDebugEnabled()) {
                    log.debug("Provisioning cached entry removed for sp " + applicationName);
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Provisioning cached entry not found for sp " + applicationName);
                }
            }

        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }

    }

}
