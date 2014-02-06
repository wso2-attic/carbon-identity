/*
 * Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.authorization.ui.internal;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.equinox.http.helper.ContextPathServletAdaptor;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.wso2.carbon.identity.authorization.ui.controller.ModuleManager;

/**
 * @scr.component name=
 *                "org.wso2.carbon.identity.authorization.ui.internal.AuthorizationUIServiceComponent"
 *                immediate="true"
 * @scr.reference name="osgi.httpservice"
 *                interface="org.osgi.service.http.HttpService"
 *                cardinality="1..1" policy="dynamic" bind="setHttpService"
 *                unbind="unsetHttpService"
 */
public class AuthorizationUIServiceComponent {

	private static HttpService httpService;
	private static final Log log = LogFactory.getLog(AuthorizationUIServiceComponent.class);

	protected void activate(ComponentContext context) {

		String url = "/carbon/identity-authorization/moduleManager";
		Servlet authoServlet = new ContextPathServletAdaptor(new ModuleManager(), url);
		try {
			httpService.registerServlet(url, authoServlet, null, null);
			log.info("Successfully registered an instance of Base Servelet");
		} catch (ServletException e) {
			e.printStackTrace();
		} catch (NamespaceException e) {
			e.printStackTrace();
		}

	}

	protected void deactivate(ComponentContext ctxt) {
		if (log.isDebugEnabled()) {
			log.debug("Identity Authorization bundle is deactivated");
		}
	}

	protected void setHttpService(HttpService httpService) {
		AuthorizationUIServiceComponent.httpService = httpService;
	}

	protected void unsetHttpService(HttpService httpService) {
		AuthorizationUIServiceComponent.httpService = null;
	}

}
