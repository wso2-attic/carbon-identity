package org.wso2.carbon.identity.application.authentication.framework.handler.sequence.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorStatus;
import org.wso2.carbon.identity.application.authentication.framework.FederatedApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.config.ConfigurationFacade;
import org.wso2.carbon.identity.application.authentication.framework.config.dto.AuthenticatorConfig;
import org.wso2.carbon.identity.application.authentication.framework.config.dto.ExternalIdPConfig;
import org.wso2.carbon.identity.application.authentication.framework.config.dto.SequenceConfig;
import org.wso2.carbon.identity.application.authentication.framework.config.dto.StepConfig;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.FrameworkException;
import org.wso2.carbon.identity.application.authentication.framework.handler.sequence.SequenceHandler;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StepBasedSequenceHandler implements SequenceHandler {
	
	private static Log log = LogFactory.getLog(StepBasedSequenceHandler.class);
	private static volatile StepBasedSequenceHandler instance;
	
	public static StepBasedSequenceHandler getInstance() {
		
		if (log.isTraceEnabled()) {
			log.trace("Inside getInstance()");
		}
		
		if (instance == null) {
			synchronized(StepBasedSequenceHandler.class) {
				
				if (instance == null) {
					instance = new StepBasedSequenceHandler();
				}
			}
		}
		
		return instance;
	}
	
	/**
	 * Executes the steps
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
		
		//if this is the start of the authentication flow
		if (context.getCurrentStep() == 0) {
			//start the first step
			handleStepStart(request, response, context, context.getSequenceConfig().getStepMap().get(1));
			return;
		} 
		//if request is coming from the domain page
		else if (request.getParameter(FrameworkConstants.RequestParams.FEDERATED_IDP) != null) {
			handleHomeRealmDiscovery(request, response, context);
		}
		//if this is a response from the login page or from external parties (e.g. federated IdPs)
		else {
			handleResponse(request, response, context);
		}
	}
	
	/**
	 * Handles a new step
	 * @param request
	 * @param response
	 * @param stepConfig
	 * @return
	 * @throws IOException
	 */
	private void handleStepStart(HttpServletRequest request, HttpServletResponse response, 
									AuthenticationContext context, StepConfig stepConfig) 
																					throws IOException {

		if (log.isTraceEnabled()) {
			log.trace("Inside handleStepStart()");
			log.trace("Starting Step " + String.valueOf(stepConfig.getOrder()));
		}
		
		context.setCurrentStep(stepConfig.getOrder());
		context.setExternalIdP(null);
		List<AuthenticatorConfig> authConfigList = stepConfig.getAuthenticatorList();
		String authenticatorNames = getCommaSeperatedAuthenticatorNamesList(authConfigList);
		String redirectURL = ConfigurationFacade.getInstance().getAuthenticationEndpointURL();
		String fidp = request.getParameter(FrameworkConstants.RequestParams.FEDERATED_IDP);
		
		//if Request has fidp param
		if (fidp != null && !fidp.isEmpty()) {
			handleHomeRealmDiscovery(request, response, context);
		//if dumbMode
		} else if (ConfigurationFacade.getInstance().isDumbMode()) { 
			response.sendRedirect(redirectURL + ("?" + context.getQueryParams()) + "&authenticators=" + authenticatorNames + "&hrd=true");
		} else { 
			//Find if step contains only a single authenticator with a single IdP
			//If yes, don't send to the multi-option page. Call directly.
			boolean sendToPage = false;
			//Are there multiple authenticators?
			if (authConfigList.size() > 1) {
				sendToPage = true;
			}
			
			AuthenticatorConfig authenticatorConfig = null;
			//TODO re-think the logic here
			for (AuthenticatorConfig authConfig : authConfigList) {
				authenticatorConfig = authConfig;
				
				if (!sendToPage && (authConfig.getIdps().size() > 1)) {
					sendToPage = true; 
					break;
				}
			}
			//call directly
			if (!sendToPage && authenticatorConfig != null && authenticatorConfig.getIdps().size() > 0) {
				context.setExternalIdP(ConfigurationFacade.getInstance().getIdPConfigByName(authenticatorConfig.getIdps().get(0)));
				authenticatorConfig.getApplicationAuthenticator().sendInitialRequest(request, response, context);
				return;
			}
			
			//else send to the multi option page.
			response.sendRedirect(redirectURL + ("?" + context.getQueryParams()) + "&authenticators=" + authenticatorNames);
		}
	}
	
	private void handleHomeRealmDiscovery(HttpServletRequest request, HttpServletResponse response, 
			AuthenticationContext context) throws IOException {
		
		String domain = request.getParameter("fidp");
		SequenceConfig sequenceConfig = context.getSequenceConfig();
		StepConfig stepConfig = sequenceConfig.getStepMap().get(context.getCurrentStep());
		List<AuthenticatorConfig> authConfigList = stepConfig.getAuthenticatorList();
		
		try {
			//call home realm discovery handler to retrieve the realm
			String homeRealm = FrameworkUtils.getHomeRealmDiscoverer().discover(domain);
			//try to find an IdP with the retrieved realm
			ExternalIdPConfig externalIdPConfig = ConfigurationFacade.getInstance().getIdPConfigByRealm(homeRealm);
			//if an IdP exists
			if (externalIdPConfig != null) {
				//try to find an authenticator of the current step, that is mapped to the IdP
				for (AuthenticatorConfig authConfig : authConfigList) {
					//if found
					if (authConfig.getIdps().contains(externalIdPConfig.getIdPName())) {
						context.setExternalIdP(externalIdPConfig);
						//call the authenticator to send the request to IdP
						authConfig.getApplicationAuthenticator().sendInitialRequest(request, response, context);
						return;
					}
				}
			}
		} catch (FrameworkException e) {
			log.error("Home Realm Discovery failed", e);
		}
		
		String authenticatorNames = getCommaSeperatedAuthenticatorNamesList(authConfigList);
		String redirectURL = ConfigurationFacade.getInstance().getAuthenticationEndpointURL();
		String errorMsg = "domain.unknown";
		
		response.sendRedirect(redirectURL + ("?" + context.getQueryParams()) 
				+ "&authenticators=" + authenticatorNames + "&authFailure=true" + "&authFailureMsg=" + errorMsg + "&hrd=true");
	}
	
	private void handleResponse(HttpServletRequest request, HttpServletResponse response, 
			AuthenticationContext context) throws IOException, FrameworkException {
		
		SequenceConfig sequenceConfig = context.getSequenceConfig();
		int currentStep = context.getCurrentStep();
		StepConfig stepConfig = sequenceConfig.getStepMap().get(currentStep);
		
		//if request from the login page with a selected IdP
		if (request.getParameter("idp") != null) {
			context.setExternalIdP(ConfigurationFacade.getInstance().getIdPConfigByName(request.getParameter("idp")));
		}
		
		for (AuthenticatorConfig authenticatorConfig : stepConfig.getAuthenticatorList()) {
			ApplicationAuthenticator authenticator = authenticatorConfig.getApplicationAuthenticator();
			
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
					//set the authenticated username in the step
					String authenticatedUser = authenticator.getAuthenticatedSubject(request);
					stepConfig.setAuthenticatedUser(authenticatedUser);
					authenticatorConfig.setAuthenticatorStateInfo(authenticator.getStateInfo(request));
					stepConfig.setAuthenticatedAutenticator(authenticatorConfig);
					
					if (context.getExternalIdP() != null) {
						stepConfig.setAuthenticatedIdP(context.getExternalIdP().getIdPName());
					} else {
						stepConfig.setAuthenticatedIdP("Local");
					}
					
					//following will be overwritten in each authenticated step.
					//last authenticated user is considered as the subject.
					sequenceConfig.setAuthenticatedUser(authenticatedUser);
					context.setSubject(authenticatedUser);
					context.getAuthenticatedAuthenticators().add(authenticator.getAuthenticatorName());
					
					currentStep++;
					stepConfig = sequenceConfig.getStepMap().get(currentStep);
					
					//if there are more steps in the sequence
					if (stepConfig != null) {
						handleStepStart(request, response, context, stepConfig);
						return;
					//if all the steps are successfully authenticated
					} else {
						sequenceConfig.setAuthenticated(true);
						handlePostAuthentication(request, response, context);
					}
				} else {
					//TODO handle retry
				}
				
				//finish authentication flow if all the steps are authenticated or if current step has failed.
				context.setRequestAuthenticated(authenticated);
				context.setSequenceComplete(true);
				return;
			}
		}
		//TODO: What if all the authenticators are disabled or none canHandle?
	}
	
	private void handlePostAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationContext context) 
													throws FrameworkException {
		
		SequenceConfig sequenceConfig = context.getSequenceConfig();
		
		for (Map.Entry<Integer, StepConfig> entry : sequenceConfig.getStepMap().entrySet()) {
			StepConfig stepConfig = entry.getValue();
			AuthenticatorConfig authenticatorConfig = stepConfig.getAuthenticatedAutenticator();
			ApplicationAuthenticator authenticator = authenticatorConfig.getApplicationAuthenticator();
			
			if (authenticator instanceof FederatedApplicationAuthenticator) {
				
				ExternalIdPConfig externalIdPConfig = ConfigurationFacade.getInstance().getIdPConfigByName(stepConfig.getAuthenticatedIdP());
				context.setExternalIdP(externalIdPConfig);
				Map<String, String> extAttrs = authenticator.getResponseAttributes(request, context);
				Map<String, String> mappedAttrs = null;
				
				if (externalIdPConfig == null) {
					String errorMsg = "An External IdP cannot be null for a FederatedApplicationAuthenticator";
					log.error(errorMsg);
					throw new FrameworkException(errorMsg);
				}
				
				if (extAttrs != null && !extAttrs.isEmpty()) {
					//do claim handling
					mappedAttrs = handleClaimMappings(context, externalIdPConfig, extAttrs);
				}
				//do user provisioning
				if (externalIdPConfig.isProvisioningEnabled()) {
					handleProvisioning(context, externalIdPConfig, mappedAttrs);
				}
				//only one step can contain FederatedApplicationAuthenticators
				break;
			} else {
				// local authentications
				handleClaimMappings(context);
			}
		}
	}
	
	private Map<String, String> handleClaimMappings(AuthenticationContext context, 
			ExternalIdPConfig externalIdPConfig,  Map<String, String> extAttrs) {
		
		Map<String, String> mappedAttrs = null;
		
		try {
			mappedAttrs = FrameworkUtils.getClaimHandler().handle(context, externalIdPConfig, extAttrs);
			context.setSubjectAttributes(mappedAttrs);
		} catch (FrameworkException e) {
			log.error("Claim handling failed!", e);
			//TODO check config file to see whether to continue or not
		}
		
		return mappedAttrs;
	}
	
	private Map<String, String> handleClaimMappings(AuthenticationContext context) {
		
		Map<String, String> mappedAttrs = null;
		
		try {
			mappedAttrs = FrameworkUtils.getClaimHandler().handle(context);
			context.setSubjectAttributes(mappedAttrs);
		} catch (FrameworkException e) {
			log.error("Claim handling failed!", e);
		}
		
		return mappedAttrs;
	}
	
	private void handleProvisioning(AuthenticationContext context, 
			ExternalIdPConfig externalIdPConfig,  Map<String, String> mappedAttrs) {
		
		//get mapped roles, if empty get the default roles from the IdP
		List<String> roles = new ArrayList<String>();
		
		if (mappedAttrs != null) {
			//TODO get the claim without hardcoding
			String roleStr= mappedAttrs.get("http://wso2.org/claims/role");
			
			if (roleStr != null) {
				String[] rolesArr = roleStr.split(",");
				
				if (rolesArr != null) {
					
					for (String role : rolesArr) {
						roles.add(role);
					}
				}
			}
		}
		
		if (roles.isEmpty()) {
			//TODO read default roles from IdP
		}
		
		try {
			FrameworkUtils.getProvisioningHandler().handle(context, externalIdPConfig, roles, context.getSubject(), mappedAttrs);
		} catch (FrameworkException e) {
			log.error("User provisioning failed!", e);
			//TODO check config file to see whether to continue or not
		}
	}
	
	private String getCommaSeperatedAuthenticatorNamesList(List<AuthenticatorConfig> authConfigList) {
		String authenticatorIdPList = "";
		
		for (AuthenticatorConfig authConfig : authConfigList) {
			String idpCSV = "";
			
			for (String idpName: authConfig.getIdps()) {
				
				if (idpName != null) {
					
					if (idpCSV != "") {
						idpCSV = idpCSV + ":";
					}
					
					idpCSV = idpCSV + idpName;
				}
			}
			//TODO use StringBuffer or StringBuilder
			if (authenticatorIdPList != "") {
				authenticatorIdPList = authenticatorIdPList + ";";
			}
			
			authenticatorIdPList = authenticatorIdPList + authConfig.getName() + ":" + idpCSV;
		}
		
		return authenticatorIdPList;
	}
}
