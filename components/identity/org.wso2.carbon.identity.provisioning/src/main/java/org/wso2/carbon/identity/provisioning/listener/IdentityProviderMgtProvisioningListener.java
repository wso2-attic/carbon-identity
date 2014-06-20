/*
 *  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.identity.provisioning.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.identity.provisioning.IdentityProvisioningException;
import org.wso2.carbon.identity.provisioning.cache.ProvisioningConnectorCache;
import org.wso2.carbon.identity.provisioning.cache.ProvisioningConnectorCacheEntry;
import org.wso2.carbon.identity.provisioning.cache.ProvisioningConnectorCacheKey;
import org.wso2.carbon.idp.mgt.listener.IdentityProviderMgtLister;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

public class IdentityProviderMgtProvisioningListener implements IdentityProviderMgtLister {

    private static final Log log = LogFactory.getLog(IdentityProviderMgtProvisioningListener.class);

    @Override
    public void updateResidentIdP(IdentityProvider identityProvider) {
        log.debug("update Resident Identity Provider event received");
    }

    @Override
    public void addIdP(IdentityProvider identityProvider) {
        log.debug("add new Identity Provider event received");
    }

    @Override
    public void deleteIdP(String idPName) {
        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext()
                .getTenantDomain();
        try {
            destroyConnector(idPName, tenantDomain);
        } catch (IdentityProvisioningException e) {
            log.error(e.getMessage(), e);
        }

    }

    @Override
    public void updateIdP(String oldIdPName, IdentityProvider identityProvider) {
        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext()
                .getTenantDomain();
        try {
            destroyConnector(oldIdPName, tenantDomain);
        } catch (IdentityProvisioningException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 
     * @param identityProviderName
     * @param tenantDomain
     * @throws IdentityProvisioningException
     */
    public void destroyConnector(String identityProviderName, String tenantDomain)
            throws IdentityProvisioningException {

        try {

            String tenantDomainName = null;
            int tenantId;

            if (CarbonContext.getThreadLocalCarbonContext() != null) {

            }

            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext
                    .getThreadLocalCarbonContext();
            carbonContext.setTenantId(MultitenantConstants.SUPER_TENANT_ID);
            carbonContext.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);

            ProvisioningConnectorCacheKey cacheKey = new ProvisioningConnectorCacheKey(identityProviderName, tenantDomain);
            ProvisioningConnectorCacheEntry entry = ((ProvisioningConnectorCacheEntry) ProvisioningConnectorCache
                    .getInstance().getValueFromCache(cacheKey));

            if (entry != null) {
                ProvisioningConnectorCache.getInstance().clearCacheEntry(cacheKey);

                if (log.isDebugEnabled()) {
                    log.debug("Provisioning cached entry removed for idp " + identityProviderName);
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Provisioning cached entry not found for idp " + identityProviderName);
                }
            }
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

}
