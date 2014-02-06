package org.wso2.carbon.identity.application.authenticator.openid;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.AbstractApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorStatus;
import org.wso2.carbon.identity.application.authentication.framework.context.ApplicationAuthenticationContext;
import org.wso2.carbon.identity.application.authenticator.openid.exception.OpenIDException;
import org.wso2.carbon.identity.application.authenticator.openid.manager.OpenIDManager;

public class OpenIDAuthenticator extends AbstractApplicationAuthenticator {

	private static Log log = LogFactory.getLog(OpenIDAuthenticator.class);
	
    public boolean canHandle(HttpServletRequest request) {
    	
    	if (log.isTraceEnabled()) {
    		log.trace("Inside canHandle()");
    	}
    	
    	if (request.getParameter("openid.mode") != null || request.getParameter("claimed_id") != null) {
    		return true;
    	}

        return false;
    }
    
    public AuthenticatorStatus authenticate(HttpServletRequest request, HttpServletResponse response, ApplicationAuthenticationContext context) {
    	
    	if (log.isTraceEnabled()) {
    		log.trace("Inside authenticate()");
    	}
    	
    	OpenIDManager manager = new OpenIDManager();
    	
    	//Handle the request from the login page
    	if (request.getParameter("claimed_id") != null) {
            try {
                response.sendRedirect(manager.doOpenIDLogin(request, response, context.getContextIdentifier()));
            } catch (IOException e) {
                log.error("Error when sending to OpenID Provider", e);
                return AuthenticatorStatus.FAIL;
            } catch (OpenIDException e) {
            	log.error("Error when sending to OpenID Provider", e);
            	return AuthenticatorStatus.FAIL;
            }
            return AuthenticatorStatus.CONTINUE;
    	}
    	
    	try {
			manager.processOpenIDLoginResponse(request, response, context.getContextIdentifier());
        } catch (OpenIDException e) {
        	log.error("Error when processing response from OpenID Provider", e);
	        return AuthenticatorStatus.FAIL;
        }
    	
    	return AuthenticatorStatus.PASS;
    }
    
    @Override
	public void sendInitialRequest(HttpServletRequest request, HttpServletResponse response, ApplicationAuthenticationContext context) {
    	
    	if (log.isTraceEnabled()) {
    		log.trace("Inside sendInitialRequest()");
    	}
    	
    	String loginPage = getAuthenticatorConfig().getParameterMap().get("LoginPage");
		
		try {
	        response.sendRedirect(response.encodeRedirectURL(loginPage + ("?" + context.getQueryParams())));
        } catch (IOException e) {
        	log.error("Error when sending to the login page", e);
        }
		
		return;	
	}
    
	@Override
    public String getAuthenticatorName() {
		
		if (log.isTraceEnabled()) {
    		log.trace("Inside getAuthenticatorName()");
    	}
		
	    return "OpenIDAuthenticator";
	}

    @Override
    public AuthenticatorStatus logout(HttpServletRequest request, HttpServletResponse response, ApplicationAuthenticationContext context) {
    	
    	if (log.isTraceEnabled()) {
    		log.trace("Inside logout()");
    	}
    	// There is not logout facility in openid
        return AuthenticatorStatus.PASS;
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
		
		return request.getParameter("sessionDataKey");
	}
}
