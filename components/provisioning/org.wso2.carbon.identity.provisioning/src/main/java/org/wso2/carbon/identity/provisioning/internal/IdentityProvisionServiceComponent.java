/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.provisioning.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.identity.application.common.ProvisioningConnectorService;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.application.common.model.ProvisioningConnectorConfig;
import org.wso2.carbon.identity.application.mgt.listener.ApplicationMgtListener;
import org.wso2.carbon.identity.provisioning.AbstractProvisioningConnectorFactory;
import org.wso2.carbon.identity.provisioning.IdentityProvisioningException;
import org.wso2.carbon.identity.provisioning.listener.ApplicationMgtProvisioningListener;
import org.wso2.carbon.identity.provisioning.listener.DefaultInboundUserProvisioningListener;
import org.wso2.carbon.identity.provisioning.listener.IdentityProviderMgtProvisioningListener;
import org.wso2.carbon.idp.mgt.listener.IdentityProviderMgtLister;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.listener.UserOperationEventListener;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.HashMap;
import java.util.Map;

/**
 * @scr.component name=
 * "org.wso2.carbon.identity.provision.internal.IdentityProvisionServiceComponent"
 * immediate="true"
 * @scr.reference name="registry.service"
 * interface="org.wso2.carbon.registry.core.service.RegistryService"
 * cardinality="1..1" policy="dynamic" bind="setRegistryService"
 * unbind="unsetRegistryService"
 * @scr.reference name="realm.service" interface="org.wso2.carbon.user.core.service.RealmService"
 * cardinality="1..1" policy="dynamic" bind="setRealmService"
 * unbind="unsetRealmService"
 * @scr.reference name="provisioning.connector.factory"
 * interface="org.wso2.carbon.identity.provisioning.AbstractProvisioningConnectorFactory"
 * cardinality="1..n" policy="dynamic" bind="setProvisioningConnectorFactory"
 * unbind="unsetProvisioningConnectorFactory"
 */
public class IdentityProvisionServiceComponent {

    private static Log log = LogFactory.getLog(IdentityProvisionServiceComponent.class);

    private static RealmService realmService;
    private static RegistryService registryService;
    private static BundleContext bundleContext;
    private static Map<String, AbstractProvisioningConnectorFactory> connectorFactories = new HashMap<String, AbstractProvisioningConnectorFactory>();

    /**
     * @return
     */
    public static RealmService getRealmService() {
        return realmService;
    }

    /**
     * @param realmService
     */
    protected void setRealmService(RealmService realmService) {
        if (log.isDebugEnabled()) {
            log.debug("Setting the Realm Service");
        }
        IdentityProvisionServiceComponent.realmService = realmService;
    }

    /**
     * @return
     */
    public static RegistryService getRegistryService() {
        return registryService;
    }

    /**
     * @param registryService
     */
    protected void setRegistryService(RegistryService registryService) {
        if (log.isDebugEnabled()) {
            log.debug("Setting the Registry Service");
        }
        IdentityProvisionServiceComponent.registryService = registryService;
    }

    /**
     * @return
     */
    public static Map<String, AbstractProvisioningConnectorFactory> getConnectorFactories() {
        return connectorFactories;
    }

    /**
     * @param context
     */
    protected void activate(ComponentContext context) {

        try {
            bundleContext = context.getBundleContext();

            bundleContext.registerService(UserOperationEventListener.class.getName(), new DefaultInboundUserProvisioningListener(), null);
            if (log.isDebugEnabled()) {
                log.debug("Identity Provision Event listener registered successfully");
            }
            bundleContext.registerService(ApplicationMgtListener.class.getName(), new ApplicationMgtProvisioningListener(), null);
            if (log.isDebugEnabled()) {
                log.debug("Application Management Event listener registered successfully");
            }
            bundleContext.registerService(IdentityProviderMgtLister.class.getName(), new IdentityProviderMgtProvisioningListener(), null);
            if (log.isDebugEnabled()) {
                log.debug("Identity Provider Management Event listener registered successfully");
            }

            if (log.isDebugEnabled()) {
                log.debug("Identity Provisioning framework bundle is activated");
            }
        } catch (IdentityProvisioningException e) {
            log.error("Error while initiating identity provisioning connector framework", e);
            log.error("Error while activating Identity Provision bundle", e);
        }
    }

    protected void deactivate() {
        if (log.isDebugEnabled()) {
            log.debug("Identity Provision bundle is de-activated");
        }
    }

    protected void unsetRegistryService() {
        if (log.isDebugEnabled()) {
            log.debug("UnSetting the Registry Service");
        }
        IdentityProvisionServiceComponent.registryService = null;
    }

    protected void unsetRealmService() {
        if (log.isDebugEnabled()) {
            log.debug("UnSetting the Realm Service");
        }
        IdentityProvisionServiceComponent.realmService = null;
    }


    protected void setProvisioningConnectorFactory(AbstractProvisioningConnectorFactory connectorFactory) {

        connectorFactories.put(connectorFactory.getConnectorType(), connectorFactory);
        if (log.isDebugEnabled()) {
            log.debug("Added provisioning connector : " + connectorFactory.getConnectorType());
        }

        ProvisioningConnectorConfig provisioningConnectorConfig = new ProvisioningConnectorConfig();
        provisioningConnectorConfig.setName(connectorFactory.getConnectorType());
        Property[] property = new Property[connectorFactory.getConfigurationProperties().size()];
        provisioningConnectorConfig.setProvisioningProperties(connectorFactory.getConfigurationProperties().toArray(property));
        ProvisioningConnectorService.getInstance().addProvisioningConnectorConfigs(provisioningConnectorConfig);
    }


    protected void unsetProvisioningConnectorFactory(AbstractProvisioningConnectorFactory connectorFactory) {

        connectorFactories.remove(connectorFactory);
        ProvisioningConnectorConfig provisioningConnectorConfig = ProvisioningConnectorService.getInstance().
                getProvisioningConnectorByName(connectorFactory.getConnectorType());
        ProvisioningConnectorService.getInstance().removeProvisioningConnectorConfigs(provisioningConnectorConfig);

        if (log.isDebugEnabled()) {
            log.debug("Removed provisioning connector : " + connectorFactory.getConnectorType());
        }
    }
}
