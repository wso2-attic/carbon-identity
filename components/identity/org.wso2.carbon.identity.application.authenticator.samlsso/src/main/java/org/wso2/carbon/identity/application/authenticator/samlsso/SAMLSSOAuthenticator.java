package org.wso2.carbon.identity.application.authenticator.samlsso;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.AbstractApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorStatus;
import org.wso2.carbon.identity.application.authentication.framework.config.ConfigurationFacade;
import org.wso2.carbon.identity.application.authentication.framework.config.ExternalIdPConfig;
import org.wso2.carbon.identity.application.authentication.framework.context.ApplicationAuthenticationContext;
import org.wso2.carbon.identity.application.authenticator.samlsso.exception.SAMLSSOException;
import org.wso2.carbon.identity.application.authenticator.samlsso.manager.SAMLSSOManager;
import org.wso2.carbon.identity.application.authenticator.samlsso.util.SSOConstants;

public class SAMLSSOAuthenticator extends AbstractApplicationAuthenticator {

	private static Log log = LogFactory.getLog(SAMLSSOAuthenticator.class);
	
    public boolean canHandle(HttpServletRequest request) {
    	
    	if (log.isTraceEnabled()) {
			log.trace("Inside canHandle()");
		}
    	
        if (request.getParameter("SAMLResponse") != null || 
        		(request.getParameter("loginType") != null && "samlsso".equals(request.getParameter("loginType")))) {
        	return true;
        }

        return false;
    }
    
    public AuthenticatorStatus authenticate(HttpServletRequest request, HttpServletResponse response, ApplicationAuthenticationContext context) {
    	
    	if (log.isTraceEnabled()) {
			log.trace("Inside authenticate()");
		}
    	
    	if (request.getParameter("loginType") != null && "samlsso".equals(request.getParameter("loginType"))) {
    		sendInitialRequest(request, response, context);
    		return AuthenticatorStatus.CONTINUE;
    	}
    	
    	ExternalIdPConfig externalIdPConfig = getIdPConfigs(request, context);
    	
    	try {
	        new SAMLSSOManager(externalIdPConfig).processResponse(request, externalIdPConfig);
        } catch (SAMLSSOException e) {
        	log.error("Exception while processing SAMLSSO response", e);
	        return AuthenticatorStatus.FAIL;
        }
    	
    	return AuthenticatorStatus.PASS;
    }
    
    @Override
	public void sendInitialRequest(HttpServletRequest request, HttpServletResponse response, ApplicationAuthenticationContext context) {
    	
    	if (log.isTraceEnabled()) {
			log.trace("Inside sendInitialRequest()");
		}
    	
    	ExternalIdPConfig externalIdPConfig = getIdPConfigs(request, context);
    	
		String idpURL = externalIdPConfig.getSSOUrl();
		String loginPage = "";
		
        try {
	        loginPage = new SAMLSSOManager(externalIdPConfig).buildRequest(request, false, false, idpURL, externalIdPConfig, context.getContextIdentifier());
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
    
    public ExternalIdPConfig getIdPConfigs(HttpServletRequest request, ApplicationAuthenticationContext context) {
    	
    	if (log.isTraceEnabled()) {
			log.trace("Inside getIdPConfigs()");
		}
    	
//    	String hrdIdP = null;
//    	
//    	if (request.getSession().getAttribute("federated-idp-domain") != null) {
//    		hrdIdP = (String)request.getSession().getAttribute("federated-idp-domain");
//    	} else {
//    		hrdIdP = request.getParameter("fidp");
//    	}
//    	
//    	if (hrdIdP != null && !hrdIdP.equals("null") && !hrdIdP.isEmpty()) {
//    		request.getSession().setAttribute("federated-idp-domain", hrdIdP);
//    		return ConfigurationFacade.getInstance().getIdPConfig(context.getCurrentStep(), getAuthenticatorName(), hrdIdP);
//    	} else {
//    		String defaultIdP = getAuthenticatorConfig().getParameterMap().get("DefaultIdPConfig");
//    		return ConfigurationFacade.getInstance().getIdPConfig(context.getCurrentStep(), getAuthenticatorName(), defaultIdP);
//    	}
    	
    	return ConfigurationFacade.getInstance().getIdPConfig(context.getCurrentStep(), getAuthenticatorName(), context.getExternalIdP());
    }
    
	@Override
    public String getAuthenticatorName() {
		
		if (log.isTraceEnabled()) {
			log.trace("Inside getAuthenticatorName()");
		}
		
	    return SSOConstants.AUTHENTICATOR_NAME;
	}

	@Override
    public AuthenticatorStatus logout(HttpServletRequest request, HttpServletResponse response, ApplicationAuthenticationContext context) {
		
		if (log.isTraceEnabled()) {
			log.trace("Inside logout()");
		}
		
		String authenticatedIdP = (String)request.getSession().getAttribute(getAuthenticatorName() + "AuthenticatedIdP");
		
		// TODO get the idp url from the config
		
		ExternalIdPConfig externalIdPConfig = getIdPConfigs(request, context);
		String idpLogoutURL = externalIdPConfig.getLogoutRequestUrl();
		String loginPage = "";
		
        try {
	        loginPage = new SAMLSSOManager(externalIdPConfig).buildRequest(request, true, false, idpLogoutURL, externalIdPConfig, context.getContextIdentifier());
        } catch (SAMLSSOException e) {
        	log.error("Exception while building the SAMLRequest", e);
        }
        
		try {
	        response.sendRedirect(response.encodeRedirectURL(loginPage));
        } catch (IOException e) {
        	log.error("Exception while sending to the login page", e);
        }
		
	    return AuthenticatorStatus.CONTINUE;
    }
	
	@Override
	public String getAuthenticatedSubject(HttpServletRequest request) {
		
		if (log.isTraceEnabled()) {
			log.trace("Inside getAuthenticatedSubject()");
		}
		
		return (String)request.getSession().getAttribute("username");
	}
	
	@Override
	public String getResponseAttributes(HttpServletRequest arg0) {
		return null;
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
}
