/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.relyingparty.internal;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.identity.relyingparty.util.RPDeploymentInterceptor;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.ConfigurationContextService;

/**
 * @scr.component name="identity.relyingparty.component" immediate="true"
 * @scr.reference name="registry.service"
 * interface="org.wso2.carbon.registry.core.service.RegistryService"
 * cardinality="1..1" policy="dynamic" bind="setRegistryService"
 * unbind="unsetRegistryService"
 * @scr.reference name="config.context.service"
 * interface="org.wso2.carbon.utils.ConfigurationContextService" cardinality="1..1"
 * policy="dynamic" bind="setConfigurationContextService"
 * unbind="unsetConfigurationContextService"
 * @scr.reference name="user.realmservice.default"
 * interface="org.wso2.carbon.user.core.service.RealmService" cardinality="1..1"
 * policy="dynamic" bind="setRealmService" unbind="unsetRealmService"
 * @scr.reference name="configuration.context.service"
 * interface="org.wso2.carbon.utils.ConfigurationContextService"
 * cardinality="1..1" policy="dynamic"
 * bind="setConfigurationContextService"
 * unbind="unsetConfigurationContextService"
 */
public class IdentityRPServiceComponent {

    private static Log log = LogFactory.getLog(IdentityRPServiceComponent.class);

    private static RegistryService registryService;

    private static RealmService realmService;

    private static ConfigurationContextService configurationContextService;

    /**
     *
     */
    public IdentityRPServiceComponent() {
    }

    public static RegistryService getRegistryService() {
        return registryService;
    }

    /**
     * @param registryService
     */
    protected void setRegistryService(RegistryService registryService) {
        if (log.isDebugEnabled()) {
            log.info("RegistryService set in Identity RP bundle");
        }
        IdentityRPServiceComponent.registryService = registryService;
    }

    public static RealmService getRealmService() {
        return realmService;
    }

    protected void setRealmService(RealmService realmService) {
        if (log.isDebugEnabled()) {
            log.info("Setting the Realm Service");
        }
        IdentityRPServiceComponent.realmService = realmService;
    }

    public static ConfigurationContextService getConfigurationContextService() {
        return configurationContextService;
    }

    protected void setConfigurationContextService(ConfigurationContextService configurationContextService) {
        log.debug("Receiving ConfigurationContext Service");
        IdentityRPServiceComponent.configurationContextService = configurationContextService;

    }

    /**
     * @param ctxt
     */
    protected void activate(ComponentContext ctxt) {
        if (log.isDebugEnabled()) {
            log.info("Identity RP bundle is activated");
        }
        try {
            ConfigurationContext configContext =
                    configurationContextService.getServerConfigContext();
            RPDeploymentInterceptor.populateRampartConfig(configContext.getAxisConfiguration());
        } catch (Throwable e) {
            log.error("Error occured while updating Relying Party service", e);
        }
    }

    /**
     * @param ctxt
     */
    protected void deactivate(ComponentContext ctxt) {
        if (log.isDebugEnabled()) {
            log.info("Identity RP bundle is deactivated");
        }
    }

    /**
     * @param registryService
     */
    protected void unsetRegistryService(RegistryService registryService) {
        if (log.isDebugEnabled()) {
            log.info("RegistryService unset in Identity RP bundle");
        }
        IdentityRPServiceComponent.registryService = null;
    }

    protected void unsetRealmService(RealmService realmService) {
        if (log.isDebugEnabled()) {
            log.info("Unsetting the Realm Service");
        }
        IdentityRPServiceComponent.realmService = null;
    }

    protected void unsetConfigurationContextService(ConfigurationContextService configurationContextService) {
        log.debug("Unsetting ConfigurationContext Service");
        setConfigurationContextService(null);
    }
}
