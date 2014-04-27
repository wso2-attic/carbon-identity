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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.config.ConfigurationFacade;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.context.SessionContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.ApplicationAuthenticatorException;
import org.wso2.carbon.identity.application.authentication.framework.exception.FrameworkException;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.registry.core.utils.UUIDGenerator;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;

/**
 * Core component of Application Authentication Framework. All the authentication
 * requests are handled by this and delegated to relevant registered authenticators.
 */
public class CommonAuthenticationServlet extends HttpServlet {
	
    private static final long serialVersionUID = -7182121722709941646L;
	private static Log log = LogFactory.getLog(CommonAuthenticationServlet.class);
	
	@Override
	public void init() {
	    
		if (log.isTraceEnabled()) {
			log.trace("Inside init()");
		}
		ConfigurationFacade.getInstance();
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) 
														throws ServletException, IOException {
		if (log.isTraceEnabled()) {
			log.trace("Inside doGet()");
		}
		
		doPost(request, response);
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) 
														throws ServletException, IOException {
		
		if (log.isTraceEnabled()) {
			log.trace("Inside doPost()");
		}
		
		try {
			AuthenticationContext context = null;
			/* Check whether this is the start of the authentication flow. 'type' parameter should 
			 * be present if so. This parameter contains the request type (e.g. samlsso) set by the 
			 * calling servlet.
			 */
			//TODO: use a different mechanism to determine the flow start.
			if (request.getParameter(FrameworkConstants.RequestParams.TYPE) != null) {
				log.debug("Initializing new authentication context");
				context = initializeFlow(request, response);
			} else {	
				context = FrameworkUtils.getContextData(request);
			} 
			
			if (context != null) {
		        //If Context have the logoutrequest flag on then this should be a LogoutReponse
		        if (context.isLogoutRequest()) {
		        	FrameworkUtils.getLogoutRequestHandler().handle(request, response, context);
		        } 
		        else {
		        	FrameworkUtils.getAuthenticationRequestHandler().handle(request, response, context);
		        }
			} else {
	            log.error("Context does not exist. Probably due to invalidated cache");
	            FrameworkUtils.sendToRetryPage(request, response);
			}
		} catch (Exception e) {
			log.error("Exception in Authentication Framework", e);
			FrameworkUtils.sendToRetryPage(request, response);
		}
	}
	
	/**
	 * Handles the initial request (from the calling servlet)
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 * @throws ApplicationAuthenticatorException 
	 */
	private AuthenticationContext initializeFlow(HttpServletRequest request, HttpServletResponse response) 
														throws ServletException, IOException, FrameworkException {
		
		if (log.isTraceEnabled()) {	
			log.trace("Inside initializeFlow()");
		}
		
		String queryParams = request.getQueryString();
		
		if (log.isDebugEnabled()) {
			log.debug("The query-string sent by the calling servlet is: " + queryParams);
		}
			
		//"sessionDataKey" - calling servlet maintains its state information using this  
		String callerSessionDataKey = request.getParameter(FrameworkConstants.SESSION_DATA_KEY);
		
		//"commonAuthCallerPath" - path of the calling servlet. This is the url response should be sent to
		String callerPath = URLDecoder.decode(request.getParameter(FrameworkConstants.RequestParams.CALLER_PATH), "UTF-8");
		
		//"type" - type of the request. e.g. samlsso, openid, oauth, passivests
		String requestType = request.getParameter(FrameworkConstants.RequestParams.TYPE);
		
		//Store the request data sent by the caller
		AuthenticationContext context = new AuthenticationContext();
		context.setCallerSessionKey(callerSessionDataKey);
		context.setCallerPath(callerPath);
		context.setRequestType(requestType);
		
		//generate a new key to hold the context data object
		String contextId = UUIDGenerator.generateUUID();
		context.setContextIdentifier(contextId);
		
		if (log.isDebugEnabled()) {
			log.debug("CommonApplicationAuthenticationServlet contextId: " + contextId);
		}
		
		//Upto now, query-string contained a 'sessionDataKey' of the calling servlet.
		//At here we replace it with the key generated for this commonauth servlet. 
		//TODO use a different param name to pass the context ID
		queryParams = queryParams.replace(callerSessionDataKey, contextId);
		context.setOrignalRequestQueryParams(queryParams);
		
		//if this a logout request from the calling servlet
		if (request.getParameter("commonAuthLogout") != null) {
			context.setLogoutRequest(true);
			Cookie cookie = FrameworkUtils.getAuthCookie(request);
			
			//retrieve the authentication session
			if (cookie != null) {
				SessionContext sessionContext = FrameworkUtils.getSessionContextFromCache(cookie.getValue());
				
				if (sessionContext != null) {
					context.setSessionIdentifier(cookie.getValue());
					context.setSubject(sessionContext.getAuthenticatedUser());
					context.setSequenceConfig(sessionContext.getAuthenticatedSequence());
				}
			}
		}
		
		// TODO extension point to store in session, cache, database etc?
		FrameworkUtils.addAuthenticationContextToCache(contextId, context, request.getSession().getMaxInactiveInterval());
		
		return context;
	}
}