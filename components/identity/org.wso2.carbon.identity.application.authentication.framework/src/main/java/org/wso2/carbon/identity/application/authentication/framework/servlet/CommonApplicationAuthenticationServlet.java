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

package org.wso2.carbon.identity.application.authentication.framework.servlet;

import java.io.IOException;
import java.net.URLDecoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.config.ConfigurationFacade;
import org.wso2.carbon.identity.application.authentication.framework.config.FileBasedConfigurationBuilder;
import org.wso2.carbon.identity.application.authentication.framework.context.ApplicationAuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.handlers.AuthenticationHandler;
import org.wso2.carbon.identity.application.authentication.framework.handlers.LogoutHandler;
import org.wso2.carbon.identity.application.authentication.framework.internal.ApplicationAuthenticationFrameworkServiceComponent;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.registry.core.utils.UUIDGenerator;
import org.wso2.carbon.ui.CarbonUIUtil;

/**
 * Core component of Application Authentication Framework. All the authentication
 * requests are handled by this and delegated to relevant registered authenticators.
 */
public class CommonApplicationAuthenticationServlet extends HttpServlet {
	
    private static final long serialVersionUID = -7182121722709941646L;
	private static Log log = LogFactory.getLog(CommonApplicationAuthenticationServlet.class);
	
	/* (non-Javadoc)
	 * @see javax.servlet.GenericServlet#init()
	 */
	@Override
	public void init() {
	    
		if (log.isTraceEnabled()) {
			log.trace("Inside init()");
		}
		ConfigurationFacade.getInstance();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest
	 * , javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) 
														throws ServletException, IOException {
		if (log.isTraceEnabled()) {
			log.trace("Inside doGet()");
		}
		
		doPost(request, response);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest
	 * , javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) 
														throws ServletException, IOException {
		
		if (log.isTraceEnabled()) {
			log.trace("Inside doPost()");
		}
		
		try {
			/* Check whether this is the start of the authentication flow. 'type' parameter should 
			 * be present if so. This parameter contains the request type (e.g. samlsso) set by the 
			 * calling servlet.
			 */
			//TODO: use a different mechanism to determine the flow start.
			if (request.getParameter(FrameworkConstants.RequestParams.TYPE) != null) {
				if (handleInitialRequest(request, response)) {
					//return if passive-mode or user alredy logged in or step 1 has a login page
					//or sending a logout request to an IdP
					return;
				}
			} 
			
			ApplicationAuthenticationContext context = getContextData(request);
			
			if (context != null) {
		        //If Context have the logoutrequest flag on then this should be a LogoutReponse
		        if (context.isLogoutRequest()) {
		            LogoutHandler.getInstance().handle(request, response, context);
		        } 
		        else {
		            AuthenticationHandler.getInstance().handle(request, response, context);
		        }
			} else {
	            log.error("Context does not exist. Probably due to invalid session");
	            String redirectURL = CarbonUIUtil.getAdminConsoleURL(request);
	            redirectURL = redirectURL.replace("commonauth/carbon/", "authenticationendpoint/retry.do");
	            response.sendRedirect(redirectURL);
			}
		} catch (Exception e) {
			log.error("Exception in Authentication Framework", e);
            String redirectURL = CarbonUIUtil.getAdminConsoleURL(request);
            redirectURL = redirectURL.replace("commonauth/carbon/", "authenticationendpoint/retry.do");
            response.sendRedirect(redirectURL);
		}
	}
	
	/**
	 * Handles the initial request (from the calling servlet)
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	private boolean handleInitialRequest(HttpServletRequest request, HttpServletResponse response) 
														throws ServletException, IOException {
		
		if (log.isTraceEnabled()) {	
			log.trace("Inside handleInitialRequest()");
		}
		
		String queryParams = request.getQueryString();
		
		if (log.isDebugEnabled()) {
			log.debug("The query-string sent by the calling servlet is: " + queryParams);
		}
		
		//remove the session variables left from a previous authentication flow
		cleanUpSession(request);
			
		//"sessionDataKey" - calling servlet maintains its state information using this  
		String callerSessionDataKey = request.getParameter(FrameworkConstants.SESSION_DATA_KEY);
		
		//"commonAuthCallerPath" - path of the calling servlet. This is the url response should be sent to
		String callerPath = URLDecoder.decode(request.getParameter(FrameworkConstants.RequestParams.CALLER_PATH), "UTF-8");
		
		//"type" - type of the request. e.g. samlsso, openid, oauth, passivests
		String requestType = request.getParameter(FrameworkConstants.RequestParams.TYPE);
		
		//Store the request data sent by the caller in a session DTO
		ApplicationAuthenticationContext context = new ApplicationAuthenticationContext();
		context.setCallerSessionKey(callerSessionDataKey);
		context.setCallerPath(callerPath);
		context.setRequestType(requestType);
		
		//generate a new key to hold the session DTO
		String sessionDataKey = UUIDGenerator.generateUUID();
		context.setContextIdentifier(sessionDataKey);
		
		if (log.isDebugEnabled()) {
			log.debug("CommonApplicationAuthenticationServlet sessionDataKey: " + sessionDataKey);
		}
		
		//Upto now, query-string contained a 'sessionDataKey' of the calling servlet.
		//At here. we replace it with the key generated for this commonauth servlet. 
		queryParams = queryParams.replace(callerSessionDataKey, sessionDataKey);
		context.setQueryParams(queryParams);
		
		// TODO extension point to store in session, cache, database etc?
		request.getSession().setAttribute(sessionDataKey, context);
		
		if (request.getParameter("commonAuthLogout") != null) {
			context.setLogoutRequest(true);
			String authenticatedAuthenticators = request.getParameter("authenticatedAuthenticators");
	        String[] authenticatorNames = authenticatedAuthenticators.split(",");
	        
	        for (String authenticatorName : authenticatorNames) {
	        	context.getAuthenticatedAuthenticators().add(authenticatorName);
	        }
	        
			return LogoutHandler.getInstance().handle(request, response, context);
		} else {
			return AuthenticationHandler.getInstance().handleInitialRequest(request, response, context);
		}
	}
	
	private ApplicationAuthenticationContext getContextData(HttpServletRequest request) {
	    
	    if (log.isTraceEnabled()) {
            log.trace("Inside getContextData()");
        }
	    
	    ApplicationAuthenticationContext context = null;
	    
	    for (ApplicationAuthenticator authenticator : ApplicationAuthenticationFrameworkServiceComponent.authenticators) {
	    	try {
	    		String contextIdentifier = authenticator.getContextIdentifier(request);
	            
	            if (contextIdentifier != null && !contextIdentifier.isEmpty()) {
	                // TODO extension point here? to get the request data from the session, cache, database or etc.
	                context = (ApplicationAuthenticationContext)request.getSession().getAttribute(contextIdentifier);
	                break;
	            }
	    	} catch (UnsupportedOperationException e) {
	    		continue;
	    	}
	    }
	    
	    return context;
	}
	
	/**
	 * Removes the session attributes used by an authentication flow
	 * @param request
	 */
	private void cleanUpSession(HttpServletRequest request) {
		
		if (log.isTraceEnabled()) {
			log.trace("Inside cleanUpSession()");
		}
		
		request.getSession().removeAttribute(FrameworkConstants.SESSION_DATA_KEY);
	}
}