package org.wso2.carbon.identity.application.authentication.framework.handlers;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorStatus;
import org.wso2.carbon.identity.application.authentication.framework.config.ConfigurationFacade;
import org.wso2.carbon.identity.application.authentication.framework.context.ApplicationAuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.internal.ApplicationAuthenticationFrameworkServiceComponent;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;

public class LogoutHandler {

	private static Log log = LogFactory.getLog(LogoutHandler.class);
	private static volatile LogoutHandler instance;
	
	public static LogoutHandler getInstance() {
		
		if (log.isTraceEnabled()) {
			log.trace("Inside getInstance()");
		}
		
		if (instance == null) {
			synchronized(LogoutHandler.class) {
				
				if (instance == null) {
					instance = new LogoutHandler();
				}
			}
		}
		
		return instance;
	}
	
	public boolean handle(HttpServletRequest request,
								HttpServletResponse response,
								ApplicationAuthenticationContext context) throws ServletException,
								IOException {

		if (log.isTraceEnabled()) {
			log.trace("Inside handleLogout()");
		}
		
		for (String name : context.getAuthenticatedAuthenticators()) {

			for (ApplicationAuthenticator authenticator : ApplicationAuthenticationFrameworkServiceComponent.authenticators) {

				if (name.equals(authenticator.getAuthenticatorName())) {
					AuthenticatorStatus status = authenticator.logout(request, response, context);
					
					//TODO following should be properly handled. This causes concurrent modification exception
					//context.getAuthenticatedAuthenticators().remove(name);

					if (status == AuthenticatorStatus.CONTINUE) {
						return true;
					} else {
						// TODO what if logout fails. this is an edge case
						// TODO send response only if Logout is done by all the authenticated authenticators
						request.getSession().removeAttribute(FrameworkConstants.SUBJECT);
						sendResponse(request, response, context, true);
						return true;
					}
				}
			}
		}

		return false;
	}
	
	private void sendResponse(HttpServletRequest request,
								HttpServletResponse response,
								ApplicationAuthenticationContext context, boolean isLoggedOut)
								throws ServletException, IOException {
		
		if (log.isTraceEnabled()) {
			log.trace("Inside sendLogoutResponseToCaller()");
		}

		// Set values to be returned to the calling servlet as request
		// attributes
		request.setAttribute(FrameworkConstants.ResponseParams.LOGGED_OUT, isLoggedOut);
		request.setAttribute(FrameworkConstants.SESSION_DATA_KEY, context.getCallerSessionKey());

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
		} else {
			// Use normal forwarding for others
			RequestDispatcher dispatcher = request.getRequestDispatcher(context.getCallerPath());
			dispatcher.forward(request, response);
		}
	}
}
