/* ***************************************************************************
 * Copyright 2014 Ellucian Company L.P. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *****************************************************************************/
package org.wso2.carbon.identity.sso.cas.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.sso.cas.exception.TicketNotFoundException;
import org.wso2.carbon.identity.sso.cas.handler.PostLoginHandler;
import org.wso2.carbon.identity.sso.cas.handler.PreLoginHandler;
import org.wso2.carbon.identity.sso.cas.util.CASCookieUtil;
import org.wso2.carbon.identity.sso.cas.util.CASPageTemplates;

public class CASLoginServlet extends HttpServlet {

	private static final long serialVersionUID = -5182312441482721905L;
	private static Log log = LogFactory.getLog(CASLoginServlet.class);

	private String tenantDomain = null;
	
	public CASLoginServlet(String tenantDomain) {
		this.tenantDomain = tenantDomain;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse) throws ServletException,
			IOException {
		doPost(httpServletRequest, httpServletResponse);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		handleRequest(req, resp);
	}

	private void handleRequest(HttpServletRequest req,
			HttpServletResponse resp) throws ServletException,
			IOException {
		try {
			if( req.getParameter(FrameworkConstants.SESSION_DATA_KEY) == null ) {
				PreLoginHandler preLoginHandler = new PreLoginHandler(tenantDomain);
				preLoginHandler.handle(req, resp);
			} else {
				PostLoginHandler postLoginHandler = new PostLoginHandler(tenantDomain);
				postLoginHandler.handle(req, resp);
			}
		} catch(TicketNotFoundException ex) {
			// Remove the invalid cookie
			CASCookieUtil.removeTicketGrantingCookie(req, resp, tenantDomain);
			
			log.debug("Removed CAS cookie and redirecting to: "+req.getRequestURL().append('?').append(req.getQueryString()));
			
			resp.sendRedirect(req.getRequestURL().append('?').append(req.getQueryString()).toString());
		} catch(Exception ex) {	
			log.debug(ex);
			// Remove the invalid cookie
			CASCookieUtil.removeTicketGrantingCookie(req, resp, tenantDomain);
			
			resp.getWriter().write(CASPageTemplates.getInstance().showLoginError("CAS login has failed due to an internal error", req.getLocale()));
			
			log.debug("Original requested URL: "+req.getRequestURL().append('?').append(req.getQueryString()), ex);

		}
	}
}