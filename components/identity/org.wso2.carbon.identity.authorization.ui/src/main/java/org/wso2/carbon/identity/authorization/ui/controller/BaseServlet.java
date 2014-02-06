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

package org.wso2.carbon.identity.authorization.ui.controller;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.axis2.context.ConfigurationContext;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.identity.authorization.ui.IdentityAuthorizationClient;
import org.wso2.carbon.ui.CarbonUIUtil;
import org.wso2.carbon.utils.ServerConstants;

/**
 * 
 * @author venura
 * 
 */
public abstract class BaseServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
	                                                                      IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
	                                                                       throws ServletException,
	                                                                       IOException {
		HttpSession session = req.getSession();

		String backendServerURL =
		                          CarbonUIUtil.getServerURL(getServletConfig().getServletContext(),
		                                                    session);
		ConfigurationContext configContext =
		                                     (ConfigurationContext) getServletConfig().getServletContext()
		                                                                              .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);

		// String backendServerURL =
		// CarbonUIUtil.getServerURL(getServletContext(), session);
		// ConfigurationContext configContext =
		// (ConfigurationContext)
		// getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
		String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);

		IdentityAuthorizationClient client =
		                                     new IdentityAuthorizationClient(cookie,
		                                                                     backendServerURL,
		                                                                     configContext);

		doProcess(req, resp, client);
	}

	protected abstract void doProcess(HttpServletRequest req, HttpServletResponse resp,
	                                  IdentityAuthorizationClient client);
}
