package org.wso2.carbon.identity.application.authentication.framework.handlers;

import java.io.IOException;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorStatus;
import org.wso2.carbon.identity.application.authentication.framework.config.AuthenticatorConfig;
import org.wso2.carbon.identity.application.authentication.framework.config.ConfigurationFacade;
import org.wso2.carbon.identity.application.authentication.framework.config.SequenceConfig;
import org.wso2.carbon.identity.application.authentication.framework.config.StepConfig;
import org.wso2.carbon.identity.application.authentication.framework.context.ApplicationAuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;

public class AuthenticationHandler {
	
	private static Log log = LogFactory.getLog(AuthenticationHandler.class);
	private static volatile AuthenticationHandler instance;
	
	public static AuthenticationHandler getInstance() {
		
		if (log.isTraceEnabled()) {
			log.trace("Inside getInstance()");
		}
		
		if (instance == null) {
			synchronized(AuthenticationHandler.class) {
				
				if (instance == null) {
					instance = new AuthenticationHandler();
				}
			}
		}
		
		return instance;
	}
	
	public boolean handleInitialRequest(HttpServletRequest request,
			HttpServletResponse response,
			ApplicationAuthenticationContext context) throws ServletException,
			IOException {

		if (log.isTraceEnabled()) {
			log.trace("Inside handleAuthenticationRequest()");
		}

		// TODO Get service provider chains
		SequenceConfig sequenceConfig = ConfigurationFacade.getInstance().getSequenceConfig(context.getRequestType(), request.getParameter("relyingParty"));

		if (sequenceConfig == null) {

			if (log.isDebugEnabled()) {
				log.debug("An application specific configuration doesn't exist for the request type. "
						+ "Taking the default config");
			}
			
			//TODO sequence cannot be null. exit from here
		}

		// "forceAuthenticate" - go in the full authentication flow even if user
		// is already logged in.
		boolean forceAuthenticate = request.getParameter(FrameworkConstants.RequestParams.FORCE_AUTHENTICATE) != null 
				? Boolean.valueOf(request.getParameter(FrameworkConstants.RequestParams.FORCE_AUTHENTICATE))
				: false;

		if (sequenceConfig.isForceAuthn()) {
			forceAuthenticate = true;
		}

		// "checkAuthentication" - passive mode. just send back whether user is
		// *already* authenticated or not.
		boolean checkAuthentication = request.getParameter(FrameworkConstants.RequestParams.CHECK_AUTHENTICATION) != null 
				? Boolean.valueOf(request.getParameter(FrameworkConstants.RequestParams.CHECK_AUTHENTICATION))
				: false;

		if (sequenceConfig.isCheckAuthn()) {
			checkAuthentication = true;
		}

		String authenticatedUser = (String) request.getSession().getAttribute(FrameworkConstants.SUBJECT);
		context.setSubject(authenticatedUser);

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

			sendResponse(request, response, context, isAuthenticated);
			return true;
		}

		// skip authentication flow if already logged in
		if (authenticatedUser != null && !forceAuthenticate) {

			if (log.isDebugEnabled()) {
				log.debug("Skipping authentication flow since user is already logged in.");
			}

			sendResponse(request, response, context, Boolean.TRUE);
			return true;
		}

		context.setSequenceConfig(sequenceConfig);
		context.setCurrentStep(1);

		if (handleStepStart(request, response, context, sequenceConfig.getStepMap().get(1))) {
			return true;
		}

		return false;
	}
	
	/**
	 * Handles a new factor; sends to a login page
	 * @param request
	 * @param response
	 * @param stepConfig
	 * @return
	 * @throws IOException
	 */
	public boolean handleStepStart(HttpServletRequest request, HttpServletResponse response, 
									ApplicationAuthenticationContext context, StepConfig stepConfig) 
																					throws IOException {

		if (log.isTraceEnabled()) {
			log.trace("Inside handleFactorStart()");
			log.trace("Starting Step " + String.valueOf(stepConfig.getOrder()));
		}
		
		context.setCurrentStep(stepConfig.getOrder());
		String redirectURL = null;
		
		List<AuthenticatorConfig> authConfigList = stepConfig.getAuthenticatorList();
		String fidp = request.getParameter(FrameworkConstants.RequestParams.FEDERATED_IDP);
		
		if (fidp != null && !fidp.isEmpty()) {
			//TODO call authenticator
			
			for (AuthenticatorConfig authConfig : authConfigList) {
				List<String> idpList =  authConfig.getIdpList();
				
				if (idpList != null && idpList.contains(fidp)) {
					context.setExternalIdP(fidp);
					authConfig.getApplicationAuthenticator().sendInitialRequest(request, response, context);
					break;
				}
			}
			return true;
		}
		
		boolean sendToPhonePage = false;
		
		if (authConfigList.size() > 1) {
			sendToPhonePage = true;
		}
		
		AuthenticatorConfig authenticatorConfig = null;
		
		for (AuthenticatorConfig authConfig : authConfigList) {
			authenticatorConfig = authConfig;
			
			if (!sendToPhonePage && (authConfig.getIdpList().size() > 1)) {
				sendToPhonePage = true;
			}
		}
		
		if (!sendToPhonePage && authenticatorConfig != null) {
			context.setExternalIdP(authenticatorConfig.getIdpList().get(0));
			authenticatorConfig.getApplicationAuthenticator().sendInitialRequest(request, response, context);
			return true;
		}
		
		if (ConfigurationFacade.getInstance().getAuthenticationEndpointURL() != null 
				&& !ConfigurationFacade.getInstance().getAuthenticationEndpointURL().isEmpty()) {
			
			String authenticatorNames = null;
			
			for (AuthenticatorConfig authConfig : authConfigList) {
				if (authenticatorNames == null) {
					authenticatorNames = authConfig.getName();
				} else {
					authenticatorNames = authenticatorNames + "," + authConfig.getName();
				}
			}
			
			redirectURL = ConfigurationFacade.getInstance().getAuthenticationEndpointURL();
			response.sendRedirect(redirectURL + ("?" + context.getQueryParams()) + "&authenticators=" + authenticatorNames);
			
			if (log.isDebugEnabled()) {
				log.debug("Redirecting default login page: " + (redirectURL + context.getQueryParams()));
			}
			
			return true;
		} 
		
		//If factor contains a login page. redirect to it
		/*if (stepConfig.getLoginPage() != null) {
			redirectURL = stepConfig.getLoginPage();
			response.sendRedirect(redirectURL + ("?" + context.getQueryParams()));
			
			if (log.isDebugEnabled()) {
				log.debug("Redirecting to factor's login page: " + (redirectURL + context.getQueryParams()));
			}
			
			return true;
		} */
		
		//Else send to an authenticator's login page
		/*else {
			for (AuthenticatorConfig authenticatorConfig : stepConfig.getAuthenticatorList()) {
				ApplicationAuthenticator authenticator = authenticatorConfig.getApplicationAuthenticator();
				//TODO Null check authenticator
				authenticator.sendInitialRequest(request, response, context);
				
				if (log.isDebugEnabled()) {
					log.debug("Factor doesn't have a login page. Sending to " + authenticator.getAuthenticatorName() + "'s authentication URL");
				}
				//TODO: what if an authenticator doesn't have a login page?
				return true;
			}
		} */
		
		//TODO set idp cookie
		
		return false;
	}
	
	/**
	 * Executes the authentication flow
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	public void handle(HttpServletRequest request, HttpServletResponse response, 
												ApplicationAuthenticationContext context) 
												throws ServletException, IOException {
		
		if (log.isTraceEnabled()) {
			log.trace("Inside doAuthenticationFlow()");
		}
		
		 //if "Deny" or "Cancel" pressed on the login page.
        if (request.getParameter(FrameworkConstants.RequestParams.DENY) != null) { 
            sendResponse(request, response, context, Boolean.FALSE);
            return;
        }
		
		int currentStep = context.getCurrentStep();
		SequenceConfig sequenceConfig = context.getSequenceConfig();
		StepConfig stepConfig = sequenceConfig.getStepMap().get(currentStep);
		
		String phoneNo = request.getParameter("phone_number");
		
		if (phoneNo != null) {
			
			String idpID = null;
			
			if (phoneNo.startsWith("00")) {
				idpID = phoneNo.substring(2, 6);
			} else if (phoneNo.startsWith("+")) {
				idpID = phoneNo.substring(1, 5);
			} else {
				idpID = phoneNo.substring(0, 2);
			}
			
			List<AuthenticatorConfig> authConfigList = sequenceConfig.getStepMap().get(1).getAuthenticatorList(); 
			
			for (AuthenticatorConfig authConfig : authConfigList) {
				List<String> idpList =  authConfig.getIdpList();
				
				if (idpList != null && idpList.contains(idpID)) {
					context.setExternalIdP(idpID);
					authConfig.getApplicationAuthenticator().sendInitialRequest(request, response, context);
					break;
				}
			}
			
			return;
		}
		
		for (AuthenticatorConfig authenticatorConfig : stepConfig.getAuthenticatorList()) {
			
			ApplicationAuthenticator authenticator = authenticatorConfig.getApplicationAuthenticator();
				
			//if (authenticator.isEnabled()) {
				//Call authenticate if canHandle;
				if (authenticator.canHandle(request)) {
					
					if (log.isDebugEnabled()) {
						log.debug(authenticator.getAuthenticatorName() + " can handle the request.");
					}
					
					AuthenticatorStatus status = authenticator.authenticate(request, response, context);
					
					if (log.isDebugEnabled()) {
						log.debug(authenticator.getAuthenticatorName() +
						          ".authenticate() returned: " + status.toString());
					}
					
					//Send to the next authentication page if the authenticator contains several 
					//steps of authentication (e.g. OTP),
					if (status == AuthenticatorStatus.CONTINUE) {
						
						if (log.isDebugEnabled()) {
							log.debug("Sending to the next authentication URL of " +
							          authenticator.getAuthenticatorName());
						}
						
						return;
					}
					
					boolean authenticated = (status == AuthenticatorStatus.PASS) ? Boolean.TRUE : Boolean.FALSE;
					
					if (authenticated) {   
						context.getAuthenticatedAuthenticators().add(authenticator.getAuthenticatorName());
						String authenticatedUser = authenticator.getAuthenticatedSubject(request);
						context.setSubject(authenticatedUser);
						request.getSession().setAttribute(FrameworkConstants.SUBJECT, authenticatedUser);
						
						currentStep++;
						stepConfig = sequenceConfig.getStepMap().get(currentStep);
						
						if (stepConfig != null && handleStepStart(request, response, context, stepConfig)) {
							return;
						}
					}
					
					sendResponse(request, response, context, authenticated);
					return;
				}
			//}
		}
		//TODO: What if all the authenticators are disabled or none canHandle?
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
	                                          ApplicationAuthenticationContext context,
	                                          Boolean isAuthenticated) 
	                                          throws ServletException, IOException {
		
		if (log.isTraceEnabled()) {
			log.trace("Inside sendAuthenticationResponseToCaller()");
		}
        
        String authenticatedAuthenticators = StringUtils.join(context.getAuthenticatedAuthenticators().iterator(), ",");
        // Set values to be returned to the calling servlet as request attributes
        request.setAttribute(FrameworkConstants.ResponseParams.AUTHENTICATED, isAuthenticated);
        request.setAttribute(FrameworkConstants.ResponseParams.AUTHENTICATED_USER, context.getSubject());
        request.setAttribute(FrameworkConstants.AUTHENTICATED_AUTHENTICATORS, authenticatedAuthenticators);
        request.setAttribute(FrameworkConstants.SESSION_DATA_KEY, context.getCallerSessionKey());
        
        if (log.isDebugEnabled()) {
            log.debug("Sending response back to: " + context.getCallerPath() + "...\n" +
	              FrameworkConstants.ResponseParams.AUTHENTICATED + ": " +
	                String.valueOf(isAuthenticated) + "\n" +
	              FrameworkConstants.ResponseParams.AUTHENTICATED_USER + ": " + context.getSubject() + "\n" +
	              FrameworkConstants.AUTHENTICATED_AUTHENTICATORS + ": " + authenticatedAuthenticators + "\n" +
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
