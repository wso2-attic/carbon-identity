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

package org.wso2.carbon.identity.authorization.ui.tag;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.identity.authorization.ui.IdentityAuthorizationClient;
import org.wso2.carbon.ui.CarbonUIUtil;
import org.wso2.carbon.utils.ServerConstants;

public abstract class BaseTag extends BodyTagSupport {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(BaseTag.class);

	protected static final byte LOAD = 1;
	protected static final byte INSERT = 2;
	protected static final byte UPDATE = 3;
	protected static final byte DELETE = 4;

	private ServletConfig config;
	private HttpServletRequest request;
	private String locale;

	public ServletConfig getConfig() {
		return config;
	}

	public void setConfig(ServletConfig config) {
		this.config = config;
	}

	public HttpServletRequest getRequest() {
		return request;
	}

	public void setRequest(HttpServletRequest request) {
		this.request = request;
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	@Override
	public int doStartTag() throws JspException {

		try {

			HttpSession session = request.getSession();
			String backendServerURL =
			                          CarbonUIUtil.getServerURL(config.getServletContext(), session);
			ConfigurationContext configContext =
			                                     (ConfigurationContext) config.getServletContext()
			                                                                  .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
			String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);

			IdentityAuthorizationClient client =
			                                     new IdentityAuthorizationClient(cookie,
			                                                                     backendServerURL,
			                                                                     configContext);
			process(client, session, request);
		} catch (AxisFault e) {
			log.error(e.getMessage());
			setErrorCodes(e);
		} catch (Exception e) {
			log.error(e.getMessage());
			setErrorCodes(e);
		}

		return BodyTagSupport.EVAL_BODY_INCLUDE;
	}

	protected abstract void process(IdentityAuthorizationClient client, HttpSession session,
	                                HttpServletRequest req) throws Exception;

	protected abstract void setErrorCodes(Exception e);

}
