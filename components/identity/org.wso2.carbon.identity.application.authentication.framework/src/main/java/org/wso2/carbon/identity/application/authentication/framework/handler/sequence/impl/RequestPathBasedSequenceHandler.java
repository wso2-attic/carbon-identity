package org.wso2.carbon.identity.application.authentication.framework.handler.sequence.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorStatus;
import org.wso2.carbon.identity.application.authentication.framework.RequestPathApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.FrameworkException;
import org.wso2.carbon.identity.application.authentication.framework.handler.sequence.SequenceHandler;
import org.wso2.carbon.identity.application.authentication.framework.internal.FrameworkServiceComponent;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RequestPathBasedSequenceHandler implements SequenceHandler {
	
	private static Log log = LogFactory.getLog(RequestPathBasedSequenceHandler.class);
	private static volatile RequestPathBasedSequenceHandler instance;
	
	public static RequestPathBasedSequenceHandler getInstance() {
		
		if (log.isTraceEnabled()) {
			log.trace("Inside getInstance()");
		}
		
		if (instance == null) {
			synchronized(RequestPathBasedSequenceHandler.class) {
				
				if (instance == null) {
					instance = new RequestPathBasedSequenceHandler();
				}
			}
		}
		
		return instance;
	}
	
	public void handle(HttpServletRequest request, HttpServletResponse response, 
			AuthenticationContext context) 
			throws ServletException, IOException, FrameworkException {
		
		 for (ApplicationAuthenticator authenticator : FrameworkServiceComponent.authenticators) {
			 
			 if (authenticator instanceof RequestPathApplicationAuthenticator) {
				 
				 if (authenticator.canHandle(request)) {
					 AuthenticatorStatus status = authenticator.authenticate(request, response, context);
					 
					 if (log.isDebugEnabled()) {
							log.debug(authenticator.getAuthenticatorName() +
							          ".authenticate() returned: " + status.toString());
						}
						
						boolean authenticated = (status == AuthenticatorStatus.PASS) ? Boolean.TRUE : Boolean.FALSE;
						
						if (authenticated) { 
							String authenticatedUser = authenticator.getAuthenticatedSubject(request);
							context.setSubject(authenticatedUser);
							context.getAuthenticatedAuthenticators().add(authenticator.getAuthenticatorName());
							request.getSession().setAttribute(FrameworkConstants.SUBJECT, authenticatedUser);
						}
						
						context.setRequestAuthenticated(authenticated);
						context.setSequenceComplete(true);
						return;
				 }
			 }
		 }
	}
}
