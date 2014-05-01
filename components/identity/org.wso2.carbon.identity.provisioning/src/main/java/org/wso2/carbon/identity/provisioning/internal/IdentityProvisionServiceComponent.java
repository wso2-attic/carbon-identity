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
package org.wso2.carbon.identity.provisioning.internal;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.wso2.carbon.identity.provisioning.IdentityProvisioningConfig;
import org.wso2.carbon.identity.provisioning.IdentityProvisioningConnectorFactory;
import org.wso2.carbon.identity.provisioning.IdentityProvisioningConstants;
import org.wso2.carbon.identity.provisioning.IdentityProvisioningEventListener;
import org.wso2.carbon.identity.provisioning.IdentityProvisioningException;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.listener.UserOperationEventListener;
import org.wso2.carbon.user.core.service.RealmService;

//TODO : Add dependency to IdP MetadataService

/**
 * @scr.component name=
 *                "org.wso2.carbon.identity.provision.internal.IdentityProvisionServiceComponent"
 *                immediate="true"
 * @scr.reference name="registry.service"
 *                interface="org.wso2.carbon.registry.core.service.RegistryService"
 *                cardinality="1..1" policy="dynamic" bind="setRegistryService"
 *                unbind="unsetRegistryService"
 * @scr.reference name="realm.service"
 *                interface="org.wso2.carbon.user.core.service.RealmService"
 *                cardinality="1..1" policy="dynamic" bind="setRealmService"
 *                unbind="unsetRealmService"
 */
public class IdentityProvisionServiceComponent {

	private static Log log = LogFactory
			.getLog(IdentityProvisionServiceComponent.class);

	private static RealmService realmService;
	private static RegistryService registryService;

	private static IdentityProvisioningEventListener listener = null;
	private static BundleContext bundleContext;
	public static List<IdentityProvisioningConnectorFactory> connectors = new ArrayList<IdentityProvisioningConnectorFactory>();

	protected void activate(ComponentContext context) {

		if (log.isDebugEnabled()) {
			log.debug("Activating IdentityProvisionServiceComponent");
		}
		try {
			bundleContext = context.getBundleContext();
			init();
			if (IdentityProvisioningConfig.getInstance().isProvisioningEnable()) {

				try {
					listener = new IdentityProvisioningEventListener();
					bundleContext.registerService(
							UserOperationEventListener.class.getName(), listener,
							null);
					if (log.isDebugEnabled()) {
						log.debug("Identity Provision Event listener registered successfully");
					}
					
					ServiceTracker<IdentityProvisioningConnectorFactory, IdentityProvisioningConnectorFactory> authServiceTracker = new ServiceTracker<IdentityProvisioningConnectorFactory, IdentityProvisioningConnectorFactory>(
							bundleContext,
							IdentityProvisioningConnectorFactory.class
									.getName(),
							new ServiceTrackerCustomizer<IdentityProvisioningConnectorFactory, IdentityProvisioningConnectorFactory>() {

								@Override
								public IdentityProvisioningConnectorFactory addingService(
										ServiceReference<IdentityProvisioningConnectorFactory> serviceReference) {
									IdentityProvisioningConnectorFactory connector = serviceReference
											.getBundle().getBundleContext()
											.getService(serviceReference);
									connectors.add(connector);
									if (log.isDebugEnabled()) {
										log.debug("Added application authenticator : "
												+ connector.getConnectorType());
									}
									return connector;
								}

								@Override
								public void modifiedService(
										ServiceReference<IdentityProvisioningConnectorFactory> serviceReference,
										IdentityProvisioningConnectorFactory service) {
									if (log.isDebugEnabled()) {
										log.debug("Modified connector : "
												+ service.getConnectorType());
									}
								}

								@Override
								public void removedService(
										ServiceReference<IdentityProvisioningConnectorFactory> serviceReference,
										IdentityProvisioningConnectorFactory service) {
									connectors.remove(service);
									serviceReference.getBundle()
											.getBundleContext()
											.ungetService(serviceReference);
									if (log.isDebugEnabled()) {
										log.debug("Removed connector : "
												+ service.getConnectorType());
									}
								}

							});
					authServiceTracker.open();

					if (log.isDebugEnabled()) {
						log.debug("IdentityProvisioningConnector service tracker started successfully");
					}
					
//					IdentityProvisioningManagementService provisioningService = new IdentityProvisioningManagementService();
//
//					bundleContext.registerService(
//							IdentityProvisioningManagementService.class.getName(), provisioningService,
//							null);
//					if (log.isDebugEnabled()) {
//						log.debug("Identity Provisioning service registered successfully");
//					}
				}
				catch (IdentityProvisioningException e) {
					log.error("Error while initiating identity provisioning connector framework", e);
				}
			} else {
				if (log.isDebugEnabled()) {
					log.debug("Identity Provisioning framework is disabled");
				}
			}
			if(log.isDebugEnabled()) {
				log.debug("Identity Provisioning framework bundle is activated");
			}
		} catch (Exception e) {
			log.error("Error while activating Identity Provision bundle", e);
		}
	}
	

    private static void init(){

        Registry registry;
        try {
            registry = IdentityProvisionServiceComponent.getRegistryService().getConfigSystemRegistry();
            if(!registry.resourceExists(IdentityProvisioningConstants.IDENTITY_PROVISIONING_REG_PATH)){
                registry.put(IdentityProvisioningConstants.IDENTITY_PROVISIONING_REG_PATH, registry.newCollection());
            }
        } catch (RegistryException e) {
            log.error("Error while creating registry collection for org.wso2.carbon.identity.mgt component");
        }                  
    }

	public static RealmService getRealmService() {
		return realmService;
	}

	public static RegistryService getRegistryService() {
		return registryService;
	}

	protected void deactivate(ComponentContext context) {
		if (log.isDebugEnabled()) {
			log.debug("Identity Provision bundle is de-activated");
		}
	}

	protected void setRegistryService(RegistryService registryService) {
		if (log.isDebugEnabled()) {
			log.debug("Setting the Registry Service");
		}
		IdentityProvisionServiceComponent.registryService = registryService;
	}

	protected void unsetRegistryService(RegistryService registryService) {
		if (log.isDebugEnabled()) {
			log.debug("UnSetting the Registry Service");
		}
		IdentityProvisionServiceComponent.registryService = null;
	}

	protected void setRealmService(RealmService realmService) {
		if (log.isDebugEnabled()) {
			log.debug("Setting the Realm Service");
		}
		IdentityProvisionServiceComponent.realmService = realmService;
	}

	protected void unsetRealmService(RealmService realmService) {
		if (log.isDebugEnabled()) {
			log.debug("UnSetting the Realm Service");
		}
		IdentityProvisionServiceComponent.realmService = null;
	}
}
