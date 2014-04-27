/*
*Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.wso2.carbon.identity.application.authentication.framework.handler.request.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorStatus;
import org.wso2.carbon.identity.application.authentication.framework.config.ConfigurationFacade;
import org.wso2.carbon.identity.application.authentication.framework.config.dto.AuthenticatorConfig;
import org.wso2.carbon.identity.application.authentication.framework.config.dto.SequenceConfig;
import org.wso2.carbon.identity.application.authentication.framework.config.dto.StepConfig;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.handler.request.LogoutRequestHandler;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticationResult;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DefaultLogoutRequestHandler implements LogoutRequestHandler {

	private static Log log = LogFactory.getLog(DefaultLogoutRequestHandler.class);
	private static volatile DefaultLogoutRequestHandler instance;
	
	public static DefaultLogoutRequestHandler getInstance() {
		
		if (log.isTraceEnabled()) {
			log.trace("Inside getInstance()");
		}
		
		if (instance == null) {
			synchronized(DefaultLogoutRequestHandler.class) {
				
				if (instance == null) {
					instance = new DefaultLogoutRequestHandler();
				}
			}
		}
		
		return instance;
	}
	
	public void handle(HttpServletRequest request,
								HttpServletResponse response,
								AuthenticationContext context) throws ServletException,
								IOException {

		if (log.isTraceEnabled()) {
			log.trace("Inside handle()");
		}
		
		//if this is the start of the logout sequence
		if (context.getCurrentStep() == 0) {
			context.setCurrentStep(1);
		}
		
		SequenceConfig sequenceConfig = context.getSequenceConfig();
		int stepCount = sequenceConfig.getStepMap().size();
		
		while (context.getCurrentStep() <= stepCount) {
			int currentStep = context.getCurrentStep();
			StepConfig stepConfig = sequenceConfig.getStepMap().get(currentStep);
			AuthenticatorConfig authenticatorConfig = stepConfig.getAuthenticatedAutenticator();
			ApplicationAuthenticator authenticator = authenticatorConfig.getApplicationAuthenticator();
			context.setExternalIdP(ConfigurationFacade.getInstance().getIdPConfigByName(stepConfig.getAuthenticatedIdP()));
			
			AuthenticatorStatus status = authenticator.logout(request, response, context, 
					authenticatorConfig.getAuthenticatorStateInfo());
			
			if (status != AuthenticatorStatus.CONTINUE) {
				// TODO what if logout fails. this is an edge case
				currentStep++;
				context.setCurrentStep(currentStep);
				continue;
			}
			//sends the logout request to the external IdP
			return;
		}
		
		// if there are no more steps
		// remove the SessionContext from the cache
		FrameworkUtils.removeSessionContextFromCache(context.getSessionIdentifier());
		// remove the cookie
		FrameworkUtils.removeAuthCookie(request, response);
		
		sendResponse(request, response, context, true);
	}
	
	private void sendResponse(HttpServletRequest request,
								HttpServletResponse response,
								AuthenticationContext context, boolean isLoggedOut)
								throws ServletException, IOException {
		
		if (log.isTraceEnabled()) {
			log.trace("Inside sendLogoutResponseToCaller()");
		}

		// Set values to be returned to the calling servlet as request
		// attributes
		request.setAttribute(FrameworkConstants.ResponseParams.LOGGED_OUT, isLoggedOut);
		request.setAttribute(FrameworkConstants.SESSION_DATA_KEY, context.getCallerSessionKey());

		AuthenticationResult authenticationResult = new AuthenticationResult();
		authenticationResult.setLoggedOut(true);
		
		//Put the result in the 
		FrameworkUtils.addAuthenticationResultToCache(context.getCallerSessionKey(), authenticationResult, request.getSession().getMaxInactiveInterval());
		
		if (log.isDebugEnabled()) {
			log.debug("Sending response back to: " + context.getCallerPath() + "...\n" + 
				FrameworkConstants.ResponseParams.LOGGED_OUT + ": " + String.valueOf(isLoggedOut) + "\n" +
				FrameworkConstants.SESSION_DATA_KEY + ": " + context.getCallerSessionKey());
		}

		// TODO: POST using HTTP Client rather than forwarding?
		// Forward the request to the caller.
		if (context.getRequestType().equals("oauth2")) {
			// Since OAuth servlet is a separate web app forward cross-context.
			request.getServletContext().getContext("/oauth2").getRequestDispatcher("/authorize/").forward(request, response);
		} else if (context.getRequestType().equals("samlsso")) { 
			//TODO: redirect to samlsso servlet. This should be done to all the servlets
			String redirectURL = context.getCallerPath() + "?sessionDataKey=" + context.getCallerSessionKey();
			response.sendRedirect(redirectURL); 
		} else {
			// Use normal forwarding for others
			RequestDispatcher dispatcher = request.getRequestDispatcher(context.getCallerPath());
			dispatcher.forward(request, response);
		}
	}
}
