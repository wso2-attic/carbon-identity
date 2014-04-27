package org.wso2.carbon.identity.application.authenticator.openid;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
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
import org.wso2.carbon.identity.application.authenticator.openid.exception.OpenIDException;
import org.wso2.carbon.identity.application.authenticator.openid.manager.DefaultOpenIDManager;
import org.wso2.carbon.identity.application.authenticator.openid.manager.OpenIDManager;

public class OpenIDAuthenticator extends AbstractApplicationAuthenticator implements FederatedApplicationAuthenticator {

	private static final long serialVersionUID = 2878862656196592256L;
	
	private static Log log = LogFactory.getLog(OpenIDAuthenticator.class);
	
	private static final String OPENID_MANAGER = "OpenIDManager";
	
    public boolean canHandle(HttpServletRequest request) {
    	
    	if (log.isTraceEnabled()) {
    		log.trace("Inside canHandle()");
    	}
    	
    	if (request.getParameter("openid.mode") != null || request.getParameter("claimed_id") != null) {
    		return true;
    	}

        return false;
    }
    
    public AuthenticatorStatus authenticate(HttpServletRequest request, HttpServletResponse response, AuthenticationContext context) {
    	
    	if (log.isTraceEnabled()) {
    		log.trace("Inside authenticate()");
    	}
    	
    	OpenIDManager manager = getNewOpenIDManagerInstance();
    	
    	//Handle the request from the login page
    	if (request.getParameter("claimed_id") != null) {
            try {
                response.sendRedirect(manager.doOpenIDLogin(request, response, context));
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
			manager.processOpenIDLoginResponse(request, response, context);
        } catch (OpenIDException e) {
        	log.error("Error when processing response from OpenID Provider", e);
	        return AuthenticatorStatus.FAIL;
        }
    	
    	return AuthenticatorStatus.PASS;
    }
    
    @Override
	public void sendInitialRequest(HttpServletRequest request, HttpServletResponse response, AuthenticationContext context) {
    	
    	if (log.isTraceEnabled()) {
    		log.trace("Inside sendInitialRequest()");
    	}
    	
    	OpenIDManager manager = getNewOpenIDManagerInstance();
    	
    	if (context.getExternalIdP() != null) { //Directed Identity
    		 try {
                 response.sendRedirect(manager.doOpenIDLogin(request, response, context));
             } catch (IOException e) {
                 log.error("Error when sending to OpenID Provider", e);
             } catch (OpenIDException e) {
             	log.error("Error when sending to OpenID Provider", e);
             }
    	} else { //Claimed Identity
    		String loginPage = getAuthenticatorConfig().getParameterMap().get("LoginPage");
    		
    		try {
    	        response.sendRedirect(loginPage + ("?" + context.getQueryParams() + "&loginType=openid"));
            } catch (IOException e) {
            	log.error("Error when sending to the login page", e);
            }
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
    public AuthenticatorStatus logout(HttpServletRequest request, HttpServletResponse response, 
    		AuthenticationContext context, AuthenticatorStateInfo stateInfo) {
    	
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
	public Map<String, String> getResponseAttributes(HttpServletRequest arg0, AuthenticationContext context) {
		return context.getSubjectAttributes();
	}

	@Override
	public String getContextIdentifier(HttpServletRequest request) {
		
		if (log.isTraceEnabled()) {
    		log.trace("Inside getContextIdentifier()");
    	}
		
		return request.getParameter("sessionDataKey");
	}

	@Override
	public AuthenticatorStateInfo getStateInfo(HttpServletRequest request) {
		return null;
	}
	
	private OpenIDManager getNewOpenIDManagerInstance() {

		OpenIDManager openIDManager = null;
		String managerClassName = getAuthenticatorConfig().getParameterMap().get(OPENID_MANAGER);
		if (managerClassName != null) {
			try {
				// Bundle class loader will cache the loaded class and returned
				// the already loaded instance, hence calling this method
				// multiple times doesn't cost.
				Class clazz = Thread.currentThread().getContextClassLoader()
						.loadClass(managerClassName);
				openIDManager = (OpenIDManager) clazz.newInstance();

			} catch (ClassNotFoundException e) {
				log.error("Error while instantiating the OpenIDManager ", e);
			} catch (InstantiationException e) {
				log.error("Error while instantiating the OpenIDManager ", e);
			} catch (IllegalAccessException e) {
				log.error("Error while instantiating the OpenIDManager ", e);
			}
		} else {
			openIDManager = new DefaultOpenIDManager();
		}
		
		return openIDManager;
	}

    public String getClaimDialectURIIfStandard(){
        return "http://axschema.org";
    }
}
