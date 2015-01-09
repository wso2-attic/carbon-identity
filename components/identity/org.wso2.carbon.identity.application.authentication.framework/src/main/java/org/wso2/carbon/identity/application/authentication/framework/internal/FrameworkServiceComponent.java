/*
 *  Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.application.authentication.framework.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.equinox.http.helper.ContextPathServletAdaptor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticationService;
import org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.FederatedApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.LocalApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.RequestPathApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.config.ConfigurationFacade;
import org.wso2.carbon.identity.application.authentication.framework.listener.AuthenticationEndpointTenantActivityListener;
import org.wso2.carbon.identity.application.authentication.framework.servlet.CommonAuthenticationServlet;
import org.wso2.carbon.identity.application.common.ApplicationAuthenticatorService;
import org.wso2.carbon.identity.application.common.model.FederatedAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.LocalAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.application.common.model.RequestPathAuthenticatorConfig;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.stratos.common.listeners.TenantMgtListener;
import org.wso2.carbon.user.core.service.RealmService;

import javax.servlet.Servlet;

import java.util.ArrayList;
import java.util.List;

/**
 * @scr.component name="identity.application.authentication.framework.component"
 *                immediate="true"
 * @scr.reference name="osgi.httpservice"
 *                interface="org.osgi.service.http.HttpService"
 *                cardinality="1..1" policy="dynamic" bind="setHttpService"
 *                unbind="unsetHttpService"
 * @scr.reference name="user.realmservice.default"
 *                interface="org.wso2.carbon.user.core.service.RealmService"
 *                cardinality="1..1" policy="dynamic" bind="setRealmService"
 *                unbind="unsetRealmService"
 * @scr.reference name="registry.service"
 *                interface="org.wso2.carbon.registry.core.service.RegistryService"
 *                cardinality="1..1" policy="dynamic" bind="setRegistryService"
 *                unbind="unsetRegistryService"
 */
public class FrameworkServiceComponent {

	private static Log log = LogFactory
			.getLog(FrameworkServiceComponent.class);
	public static List<ApplicationAuthenticator> authenticators = new ArrayList<ApplicationAuthenticator>();

	private static BundleContext bundleContext;
	private static RealmService realmService;
	private static RegistryService registryService;

	private HttpService httpService;

	public static final String COMMON_SERVLET_URL = "/commonauth";

	@SuppressWarnings("unchecked")
    protected void activate(ComponentContext ctxt) {
		bundleContext = ctxt.getBundleContext();
        bundleContext.registerService(ApplicationAuthenticationService.class.getName(), new ApplicationAuthenticationService(), null);

		boolean tenantDropdownEnabled = ConfigurationFacade.getInstance().getTenantDropdownEnabled();

		if (tenantDropdownEnabled) {
			// Register the tenant management listener for tracking changes to tenants
			bundleContext.registerService(TenantMgtListener.class.getName(),
			        new AuthenticationEndpointTenantActivityListener(), null);

			if (log.isDebugEnabled()) {
				log.debug("AuthenticationEndpointTenantActivityListener is registered. Tenant Domains Dropdown is " +
				        "enabled.");
			}
		}

		ServiceTracker authServiceTracker = new ServiceTracker(
				bundleContext,
				ApplicationAuthenticator.class.getName(),
				new ServiceTrackerCustomizer<ApplicationAuthenticator, ApplicationAuthenticator>() {

					@Override
					public ApplicationAuthenticator addingService(
							ServiceReference<ApplicationAuthenticator> serviceReference) {
						ApplicationAuthenticator authenticator = serviceReference
								.getBundle().getBundleContext()
								.getService(serviceReference);
						authenticators.add(authenticator);
						
						Property[] configProperties = null;
						
						if (authenticator.getConfigurationProperties() != null
                                && authenticator.getConfigurationProperties().size() > 0) {
						    configProperties = authenticator.getConfigurationProperties().toArray(new Property[0]);
                        }
						
						if (authenticator instanceof LocalApplicationAuthenticator) {
							LocalAuthenticatorConfig localAuthenticatorConfig = new LocalAuthenticatorConfig();
							localAuthenticatorConfig.setName(authenticator.getName());                            
							localAuthenticatorConfig.setProperties(configProperties);
							localAuthenticatorConfig.setDisplayName(authenticator.getFriendlyName());
							ApplicationAuthenticatorService.getInstance().addLocalAuthenticator(localAuthenticatorConfig);
						} else if (authenticator instanceof FederatedApplicationAuthenticator) {
							FederatedAuthenticatorConfig federatedAuthenticatorConfig = new FederatedAuthenticatorConfig();
							federatedAuthenticatorConfig.setName(authenticator.getName());
							federatedAuthenticatorConfig.setProperties(configProperties);
							federatedAuthenticatorConfig.setDisplayName(authenticator.getFriendlyName());
							ApplicationAuthenticatorService.getInstance().addFederatedAuthenticator(federatedAuthenticatorConfig);
						} else if (authenticator instanceof RequestPathApplicationAuthenticator) {
							RequestPathAuthenticatorConfig reqPathAuthenticatorConfig = new RequestPathAuthenticatorConfig();
							reqPathAuthenticatorConfig.setName(authenticator.getName());
							reqPathAuthenticatorConfig.setProperties(configProperties);
							reqPathAuthenticatorConfig.setDisplayName(authenticator.getFriendlyName());
							ApplicationAuthenticatorService.getInstance().addRequestPathAuthenticator(reqPathAuthenticatorConfig);
						}
						
						if (log.isDebugEnabled()) {
							log.debug("Added application authenticator : "
									+ authenticator.getName());
						}
						return authenticator;
					}

					@Override
					public void modifiedService(
							ServiceReference<ApplicationAuthenticator> serviceReference,
							ApplicationAuthenticator service) {
					}

					@Override
					public void removedService(
							ServiceReference<ApplicationAuthenticator> serviceReference,
							ApplicationAuthenticator authenticator) {
						authenticators.remove(authenticator);
						String authenticatorName = authenticator.getName();
						ApplicationAuthenticatorService appAuthenticatorService = ApplicationAuthenticatorService.getInstance();
						
						if (authenticator instanceof LocalApplicationAuthenticator) {
							LocalAuthenticatorConfig localAuthenticatorConfig = appAuthenticatorService.getLocalAuthenticatorByName(authenticatorName);
							appAuthenticatorService.removeLocalAuthenticator(localAuthenticatorConfig);
						} else if (authenticator instanceof FederatedApplicationAuthenticator) {
							FederatedAuthenticatorConfig federatedAuthenticatorConfig = appAuthenticatorService.getFederatedAuthenticatorByName(authenticatorName);
							appAuthenticatorService.removeFederatedAuthenticator(federatedAuthenticatorConfig);
						} else if (authenticator instanceof RequestPathApplicationAuthenticator) {
							RequestPathAuthenticatorConfig reqPathAuthenticatorConfig = appAuthenticatorService.getRequestPathAuthenticatorByName(authenticatorName);
							appAuthenticatorService.removeRequestPathAuthenticator(reqPathAuthenticatorConfig);
						}
						
						serviceReference.getBundle().getBundleContext()
								.ungetService(serviceReference);
						if (log.isDebugEnabled()) {
							log.debug("Removed application authenticator : "
									+ authenticator.getName());
						}
					}

				});
		authServiceTracker.open();

		// Register Common servlet
		Servlet commonServlet = new ContextPathServletAdaptor(
				new CommonAuthenticationServlet(),
				COMMON_SERVLET_URL);
		try {
			httpService.registerServlet(COMMON_SERVLET_URL, commonServlet,
					null, null);
		} catch (Exception e) {
			String errMsg = "Error when registering Common Servlet via the HttpService.";
			log.error(errMsg, e);
			throw new RuntimeException(errMsg, e);
		}

		if (log.isDebugEnabled()) {
			log.info("Application Authentication Framework bundle is activated");
		}
	}

	protected void deactivate(ComponentContext ctxt) {
		if (log.isDebugEnabled()) {
			log.info("Application Authentication Framework bundle is deactivated");
		}

		bundleContext = null;
	}

	protected void setHttpService(HttpService httpService) {
		if (log.isDebugEnabled()) {
			log.debug("HTTP Service is set in the Application Authentication Framework bundle");
		}

		this.httpService = httpService;
	}

	protected void unsetHttpService(HttpService httpService) {
		if (log.isDebugEnabled()) {
			log.debug("HTTP Service is unset in the Application Authentication Framework bundle");
		}

		this.httpService = null;
	}

    protected void setRealmService(RealmService realmService) {
		if (log.isDebugEnabled()) {
			log.debug("RealmService is set in the Application Authentication Framework bundle");
		}
		FrameworkServiceComponent.realmService = realmService;
    }

    protected void unsetRealmService(RealmService realmService) {
		if (log.isDebugEnabled()) {
			log.debug("RealmService is unset in the Application Authentication Framework bundle");
		}
		FrameworkServiceComponent.realmService = null;
    }

    public static RealmService getRealmService() {
        return FrameworkServiceComponent.realmService;
    }

    protected void setApplicationAuthenticatorService(ApplicationAuthenticatorService service) {
    }

    protected void unsetApplicationAuthenticatorService(ApplicationAuthenticatorService service) {
    }
    
    protected void setRegistryService(RegistryService registryService) {
		if (log.isDebugEnabled()) {
			log.debug("RegistryService is set in the Application Authentication Framework bundle");
		}
		FrameworkServiceComponent.registryService = registryService;
    }

    protected void unsetRegistryService(RegistryService registryService) {
		if (log.isDebugEnabled()) {
			log.debug("RegistryService is unset in the Application Authentication Framework bundle");
		}
		FrameworkServiceComponent.registryService = null;
    }

    public static RegistryService getRegistryService() {
        return FrameworkServiceComponent.registryService;
    }
    
	public static BundleContext getBundleContext() throws Exception {
		if (bundleContext == null) {
			String msg = "System has not been started properly. Bundle Context is null.";
			log.error(msg);
			throw new Exception(msg);
		}

		return bundleContext;
	}
}
