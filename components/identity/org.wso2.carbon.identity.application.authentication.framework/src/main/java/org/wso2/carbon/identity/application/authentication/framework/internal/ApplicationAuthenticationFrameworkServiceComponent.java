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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.Servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.equinox.http.helper.ContextPathServletAdaptor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.servlet.CommonApplicationAuthenticationServlet;

/**
 * @scr.component name="identity.application.authentication.framework.component"
 *                immediate="true"
 * @scr.reference name="osgi.httpservice"
 *                interface="org.osgi.service.http.HttpService"
 *                cardinality="1..1" policy="dynamic" bind="setHttpService"
 *                unbind="unsetHttpService"
 */
public class ApplicationAuthenticationFrameworkServiceComponent {

	private static Log log = LogFactory
			.getLog(ApplicationAuthenticationFrameworkServiceComponent.class);
	public static List<ApplicationAuthenticator> authenticators = new ArrayList<ApplicationAuthenticator>();

	private static BundleContext bundleContext;

	private HttpService httpService;

	public static final String COMMON_SERVLET_URL = "/commonauth";

	protected void activate(ComponentContext ctxt) {
		bundleContext = ctxt.getBundleContext();

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
						if (log.isDebugEnabled()) {
							log.debug("Added application authenticator : "
									+ authenticator.getAuthenticatorName());
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
							ApplicationAuthenticator service) {
						authenticators.remove(service);
						serviceReference.getBundle().getBundleContext()
								.ungetService(serviceReference);
						if (log.isDebugEnabled()) {
							log.debug("Removed application authenticator : "
									+ service.getAuthenticatorName());
						}
					}

				});
		authServiceTracker.open();

		// Register Common servlet
		Servlet commonServlet = new ContextPathServletAdaptor(
				new CommonApplicationAuthenticationServlet(),
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

	public static BundleContext getBundleContext() throws Exception {
		if (bundleContext == null) {
			String msg = "System has not been started properly. Bundle Context is null.";
			log.error(msg);
			throw new Exception(msg);
		}

		return bundleContext;
	}
}