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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.identity.application.authentication.framework.config.ConfigurationFacade;
import org.wso2.carbon.identity.application.authentication.framework.config.dto.AuthenticatorConfig;
import org.wso2.carbon.identity.application.authentication.framework.config.dto.ExternalIdPConfig;
import org.wso2.carbon.identity.application.authentication.framework.config.dto.SequenceConfig;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.context.SessionContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.FrameworkException;
import org.wso2.carbon.identity.application.authentication.framework.handler.request.AuthenticationRequestHandler;
import org.wso2.carbon.identity.application.authentication.framework.handler.sequence.impl.RequestPathBasedSequenceHandler;
import org.wso2.carbon.identity.application.authentication.framework.handler.sequence.impl.StepBasedSequenceHandler;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticationResult;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.registry.core.utils.UUIDGenerator;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class DefaultAuthenticationRequestHandler implements AuthenticationRequestHandler {
	
	private static Log log = LogFactory.getLog(DefaultAuthenticationRequestHandler.class);
	private static volatile DefaultAuthenticationRequestHandler instance;
	
	public static DefaultAuthenticationRequestHandler getInstance() {
		
		if (log.isTraceEnabled()) {
			log.trace("Inside getInstance()");
		}
		
		if (instance == null) {
			synchronized(DefaultAuthenticationRequestHandler.class) {
				
				if (instance == null) {
					instance = new DefaultAuthenticationRequestHandler();
				}
			}
		}
		
		return instance;
	}
	
	/**
	 * Executes the authentication flow
	 * @param request
	 * @param response
	 * @throws FrameworkException 
	 * @throws Exception
	 */
	public void handle(HttpServletRequest request, HttpServletResponse response, 
												AuthenticationContext context) 
												throws ServletException, IOException, FrameworkException {
		
		if (log.isTraceEnabled()) {
			log.trace("Inside handle()");
		}
		
		//if "Deny" or "Cancel" pressed on the login page.
        if (request.getParameter(FrameworkConstants.RequestParams.DENY) != null) { 
        	context.setSequenceComplete(true);
			context.setRequestAuthenticated(Boolean.FALSE);
            sendResponse(request, response, context);
            return;
        }
		
		int currentStep = context.getCurrentStep();
		
		//if this is the start of the authentication flow
		if (currentStep == 0) {
			if (handleSequenceStart(request, response, context)) {
				//we return if user is previously authenticated
				return;
			}
		}
		
        SequenceConfig seqConfig = context.getSequenceConfig();
        List<AuthenticatorConfig> reqPathAuthenticators = seqConfig.getReqPathAuthenticators();
        
  		//if SP has request path authenticators configured and this is start of the flow
        if (reqPathAuthenticators != null && !reqPathAuthenticators.isEmpty() && currentStep == 0) {
        	//call request path sequence handler
        	RequestPathBasedSequenceHandler.getInstance().handle(request, response, context);
        }
        
        //if no request path authenticators or handler returned cannot handle 
        if (!context.isSequenceComplete() || (reqPathAuthenticators == null || reqPathAuthenticators.isEmpty())) {
        	//call step based sequence handler
        	StepBasedSequenceHandler.getInstance().handle(request, response, context);
        }
		
        //if flow completed send response back
        if (context.isSequenceComplete()) {
        	sendResponse(request, response, context);
        }
	}
	
	/**
	 * Handle the start of a Sequence
	 * 
	 * @param request
	 * @param response
	 * @param context
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws FrameworkException
	 */
	private boolean handleSequenceStart(HttpServletRequest request,
			HttpServletResponse response,
			AuthenticationContext context) throws ServletException,
			IOException, FrameworkException {

		if (log.isTraceEnabled()) {
			log.trace("Inside handleSequenceStart()");
		}

		String tenantDomain = request.getParameter("tenantDomain");
		
		if (tenantDomain == null || tenantDomain.isEmpty()) {
			tenantDomain = MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
		}
		
		//Get service provider chains
		SequenceConfig sequenceConfig = ConfigurationFacade.getInstance().
				getSequenceConfig(context.getRequestType(), request.getParameter("relyingParty"), tenantDomain);

		// "forceAuthenticate" - go in the full authentication flow even if user
		// is already logged in.
		boolean forceAuthenticate = request.getParameter(FrameworkConstants.RequestParams.FORCE_AUTHENTICATE) != null 
				? Boolean.valueOf(request.getParameter(FrameworkConstants.RequestParams.FORCE_AUTHENTICATE))
				: false;

		// override force authentication from the config
		if (sequenceConfig.isForceAuthn()) {
			forceAuthenticate = true;
		}

		// "checkAuthentication" - passive mode. just send back whether user is
		// *already* authenticated or not.
		boolean checkAuthentication = request.getParameter(FrameworkConstants.RequestParams.CHECK_AUTHENTICATION) != null 
				? Boolean.valueOf(request.getParameter(FrameworkConstants.RequestParams.CHECK_AUTHENTICATION))
				: false;

		// override check authentication from the config
		if (sequenceConfig.isCheckAuthn()) {
			checkAuthentication = true;
		}

		String authenticatedUser = null;
		Cookie cookie = FrameworkUtils.getAuthCookie(request);
		
		// if cookie exists user has previously authenticated
		if (cookie != null) {
			// get the authentication details from the cache
			SessionContext sessionContext = FrameworkUtils.getSessionContextFromCache(cookie.getValue());
			
			if (sessionContext != null) {
				authenticatedUser = sessionContext.getAuthenticatedUser();
				context.setSubject(authenticatedUser);
				context.setSequenceConfig(sessionContext.getAuthenticatedSequence());
			}
		}

		if (log.isDebugEnabled()) {
			if (authenticatedUser != null) {
				log.debug("Already authenticated by username: " + authenticatedUser);
			} else {
				log.debug("An already authenticated user doesn't exist");
			}
		}

		// if passive mode
		if (checkAuthentication) {

			if (log.isDebugEnabled()) {
				log.debug("Executing in passive mode.");
			}

			boolean isAuthenticated = false;

			if (authenticatedUser != null) {
				isAuthenticated = true;
			}

			context.setSequenceComplete(true);
			context.setRequestAuthenticated(isAuthenticated);
			sendResponse(request, response, context);
			return true;
		}

		//TODO remove session usage
		String prevAuthenticatedIdP = (String)request.getSession().getAttribute("externalIdP");
		String fidp = request.getParameter(FrameworkConstants.RequestParams.FEDERATED_IDP);
		boolean isSameDomain = false;
		
		if (prevAuthenticatedIdP != null && fidp != null && !fidp.isEmpty()) {
			ExternalIdPConfig externalIdPConfig = ConfigurationFacade.getInstance().getIdPConfigByName(prevAuthenticatedIdP);
			
			if (fidp.equalsIgnoreCase(externalIdPConfig.getDomain())) {
				isSameDomain = true;
			}
		}
		
		// skip authentication flow if already logged in and caller hasn't asked to force authenticate
		if (authenticatedUser != null && !forceAuthenticate 
				&& ((fidp == null || fidp.isEmpty()) || isSameDomain)) {

			if (log.isDebugEnabled()) {
				log.debug("Skipping authentication flow since user is already logged in.");
			}

			context.setSequenceComplete(true);
			context.setRequestAuthenticated(Boolean.TRUE);
			sendResponse(request, response, context);
			return true;
		}
		
		// let's start a fresh authentication flow
		FrameworkUtils.removeAuthCookie(request, response);
		context.setSequenceConfig(sequenceConfig);
		context.setSubject(null);

		return false;
	}
	
	/**
	 * Sends the response to the servlet that initiated the authentication flow
	 * @param request
	 * @param response
	 * @param isAuthenticated
	 * @throws ServletException
	 * @throws IOException
	 */
	private void sendResponse(HttpServletRequest request, 
                              HttpServletResponse response,
                              AuthenticationContext context) 
                            		  throws ServletException, IOException {
		
		if (log.isTraceEnabled()) {
			log.trace("Inside sendResponse()");
		}
		
		boolean isAuthenticated = context.isRequestAuthenticated();
		
		if (isAuthenticated && context.getExternalIdP() != null) {
			request.getSession().setAttribute("externalIdP", context.getExternalIdP());
		} else {
			request.getSession().removeAttribute("externalIdP");
		}
        
        String authenticatedAuthenticators = StringUtils.join(context.getAuthenticatedAuthenticators().iterator(), ",");
        
        AuthenticationResult authenticationResult = new AuthenticationResult();
        authenticationResult.setAuthenticated(isAuthenticated);
        
        if (isAuthenticated) {
        	authenticationResult.setSubject(context.getSubject());
            authenticationResult.setAuthenticatedAuthenticators(authenticatedAuthenticators);
            
            if (context.getSubjectAttributes() != null && !context.getSubjectAttributes().isEmpty()) {
            	authenticationResult.setUserAttributes(context.getSubjectAttributes());
            }
            
            SequenceConfig sequenceConfig = context.getSequenceConfig();
    		
    		//SessionContext is retained across different SP requests in the same browser session.
    		//it is tracked by a cookie
    		SessionContext sessionContext = new SessionContext();
    		sessionContext.setAuthenticatedUser(sequenceConfig.getAuthenticatedUser());
    		sessionContext.setAuthenticatedSequence(sequenceConfig);
    		
    		String sessionKey = UUIDGenerator.generateUUID();
            FrameworkUtils.addSessionContextToCache(sessionKey, sessionContext, request.getSession().getMaxInactiveInterval());
            
            FrameworkUtils.storeAuthCookie(request, response, sessionKey);
        }
        //Put the result in the cache using calling servlet's sessionDataKey as the cache key
        //Once the redirect is done to that servlet, it will retrieve the result from the cache
        //using that key.
        FrameworkUtils.addAuthenticationResultToCache(context.getCallerSessionKey(), authenticationResult, 
        		request.getSession().getMaxInactiveInterval());
        
        if (log.isDebugEnabled()) {
            log.debug("Sending response back to: " + context.getCallerPath() + "...\n" +
	              FrameworkConstants.ResponseParams.AUTHENTICATED + ": " +
	                String.valueOf(isAuthenticated) + "\n" +
	              FrameworkConstants.ResponseParams.AUTHENTICATED_USER + ": " + context.getSubject() + "\n" +
	              FrameworkConstants.AUTHENTICATED_AUTHENTICATORS + ": " + authenticatedAuthenticators + "\n" +
	              FrameworkConstants.SESSION_DATA_KEY + ": " + context.getCallerSessionKey());
        }
        
        /*TODO rememberMe should be handled by a cookie authenticator.
        	   For now rememberMe flag that was set in the login page will
        	   be sent as a query param to the calling servlet so it will
        	   handle rememberMe as usual. */
        String rememberMeParam = "";
        
        if (isAuthenticated && context.isRememberMe()) {
        	rememberMeParam = rememberMeParam + "&chkRemember=on";  
        }
        
        //redirect to the caller
		String redirectURL = context.getCallerPath() + "?sessionDataKey=" + context.getCallerSessionKey() + rememberMeParam;
		response.sendRedirect(redirectURL); 
    }
}
