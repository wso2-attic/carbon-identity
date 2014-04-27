package org.wso2.carbon.identity.application.authenticator.samlsso;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.AbstractApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorStateInfo;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorStatus;
import org.wso2.carbon.identity.application.authentication.framework.FederatedApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.config.ConfigurationFacade;
import org.wso2.carbon.identity.application.authentication.framework.config.dto.ExternalIdPConfig;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authenticator.samlsso.dto.StateInfoDTO;
import org.wso2.carbon.identity.application.authenticator.samlsso.exception.SAMLSSOException;
import org.wso2.carbon.identity.application.authenticator.samlsso.manager.DefaultSAMLSSOManager;
import org.wso2.carbon.identity.application.authenticator.samlsso.manager.SAMLSSOManager;
import org.wso2.carbon.identity.application.authenticator.samlsso.util.SSOConstants;

public class SAMLSSOAuthenticator extends AbstractApplicationAuthenticator implements FederatedApplicationAuthenticator {

	private static final long serialVersionUID = -8097512332218044859L;
	
	private static Log log = LogFactory.getLog(SAMLSSOAuthenticator.class);
	
	private static final String SAML_SSO_MANAGER = "SAMLSSOManager";

	private ExternalIdPConfig externalIdPConfig;
	
    public boolean canHandle(HttpServletRequest request) {
    	
    	if (log.isTraceEnabled()) {
			log.trace("Inside canHandle()");
		}
    	
        if (request.getParameter("SAMLResponse") != null || 
        		(request.getParameter("authenticator") != null 
        		&& getAuthenticatorName().equalsIgnoreCase(request.getParameter("authenticator"))) ) {
        	return true;
        }

        return false;
    }
    
    public AuthenticatorStatus authenticate(HttpServletRequest request, HttpServletResponse response, AuthenticationContext context) {
    	
    	if (log.isTraceEnabled()) {
			log.trace("Inside authenticate()");
		}
    	
    	if (request.getParameter("authenticator") != null 
        		&& getAuthenticatorName().equalsIgnoreCase(request.getParameter("authenticator"))) {
    		sendInitialRequest(request, response, context);
    		return AuthenticatorStatus.CONTINUE;
    	}
    	
    	ExternalIdPConfig externalIdPConfig = context.getExternalIdP();
    	
    	try {
	        getNewSAMLSSOManagerInstance(externalIdPConfig).processResponse(request, externalIdPConfig);
	        Map<String, String> receivedClaims = (Map<String, String>) request.getAttribute("samlssoAttributes");
	        context.setSubjectAttributes(receivedClaims);
	        
        } catch (SAMLSSOException e) {
        	log.error("Exception while processing SAMLSSO response", e);
	        return AuthenticatorStatus.FAIL;
        }
    	
    	return AuthenticatorStatus.PASS;
    }
    
    @Override
	public void sendInitialRequest(HttpServletRequest request, HttpServletResponse response, AuthenticationContext context) {
    	
    	if (log.isTraceEnabled()) {
			log.trace("Inside sendInitialRequest()");
		}
    	
    	ExternalIdPConfig externalIdPConfig = context.getExternalIdP();
    	
		String idpURL = externalIdPConfig.getSSOUrl();
		String loginPage = "";
		
        try {
	        loginPage = getNewSAMLSSOManagerInstance(externalIdPConfig).buildRequest(request, false, false, idpURL, externalIdPConfig, context.getContextIdentifier());
        } catch (SAMLSSOException e) {
        	log.error("Exception while building the SAMLRequest", e);
        }
        
		try {
	        response.sendRedirect(response.encodeRedirectURL(loginPage));
        } catch (IOException e) {
        	log.error("Exception while sending to the login page", e);
        }
		return;
	}
    
	@Override
    public String getAuthenticatorName() {
		
		if (log.isTraceEnabled()) {
			log.trace("Inside getAuthenticatorName()");
		}
		
	    return SSOConstants.AUTHENTICATOR_NAME;
	}

	@Override
    public AuthenticatorStatus logout(HttpServletRequest request, HttpServletResponse response, 
    		AuthenticationContext context, AuthenticatorStateInfo stateInfo) {
		
		if (log.isTraceEnabled()) {
			log.trace("Inside logout()");
		}
		
		if (request.getParameter("SAMLResponse") == null) {
			//send logout request to external idp
			ExternalIdPConfig externalIdPConfig = context.getExternalIdP();
			String idpLogoutURL = externalIdPConfig.getLogoutRequestUrl();
			String logoutURL = "";
			
			if (stateInfo instanceof StateInfoDTO) {
				request.getSession().setAttribute("logoutSessionIndex", ((StateInfoDTO)stateInfo).getSessionIndex());
				request.getSession().setAttribute("logoutUsername", ((StateInfoDTO)stateInfo).getSubject());
			}
			
	        try {
		        logoutURL = getNewSAMLSSOManagerInstance(externalIdPConfig).buildRequest(request, true, false, idpLogoutURL, externalIdPConfig, context.getContextIdentifier());
	        } catch (SAMLSSOException e) {
	        	log.error("Exception while building the SAMLRequest", e);
	        }
	        
			try {
		        response.sendRedirect(logoutURL);
	        } catch (IOException e) {
	        	log.error("Exception while sending to the login page", e);
	        }
			
		    return AuthenticatorStatus.CONTINUE;
		} else {
			return AuthenticatorStatus.PASS;
		}
    }
	
	@Override
	public String getAuthenticatedSubject(HttpServletRequest request) {
		
		if (log.isTraceEnabled()) {
			log.trace("Inside getAuthenticatedSubject()");
		}
		
		return (String)request.getSession().getAttribute("username");
	}
	
	@Override
	public Map<String, String> getResponseAttributes(HttpServletRequest request, AuthenticationContext context) {
		return context.getSubjectAttributes(); 
	}
	
	@Override
	public String getContextIdentifier(HttpServletRequest request) {
		
		if (log.isTraceEnabled()) {
			log.trace("Inside getContextIdentifier()");
		}
		
		String identifier = request.getParameter("sessionDataKey");
		
		if (identifier == null) {
			identifier = request.getParameter("RelayState");
			
			if (identifier != null) {
				// TODO SHOULD ensure that the value has not been tampered with by using a checksum, a pseudo-random value, or similar means.
				try {
					return URLDecoder.decode(identifier, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					log.error("Exception while URL decoding the Relay State", e);
				}
			}
		}
		
		return identifier;
	}

	@Override
	public AuthenticatorStateInfo getStateInfo(HttpServletRequest request) {
		
		Object sessionIndexObj = request.getSession().getAttribute(SSOConstants.IDP_SESSION);
		String sessionIndex = null;
		
		if (sessionIndexObj != null) {
			sessionIndex = (String)sessionIndexObj;
		}
		
		StateInfoDTO stateInfoDTO = new StateInfoDTO();
		stateInfoDTO.setSessionIndex(sessionIndex);
		stateInfoDTO.setSubject(getAuthenticatedSubject(request));
		
		return stateInfoDTO;
	}
	
	private SAMLSSOManager getNewSAMLSSOManagerInstance(ExternalIdPConfig pExternalIdPConfig) throws SAMLSSOException {

		String managerClassName = getAuthenticatorConfig().getParameterMap().get(SAML_SSO_MANAGER);
		if (managerClassName != null) {
			try {
				// Bundle class loader will cache the loaded class and returned
				// the already loaded instance, hence calling this method
				// multiple times doesn't cost.
				Class clazz = Thread.currentThread().getContextClassLoader()
						.loadClass(managerClassName);
				return (SAMLSSOManager) clazz.newInstance();

			} catch (ClassNotFoundException e) {
				log.error("Error while instantiating the SAMLSSOManager ", e);
			} catch (InstantiationException e) {
				log.error("Error while instantiating the SAMLSSOManager ", e);
			} catch (IllegalAccessException e) {
				log.error("Error while instantiating the SAMLSSOManager ", e);
			}
		} else {
			return new DefaultSAMLSSOManager(pExternalIdPConfig);
		}
		return null;
	}
}
