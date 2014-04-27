package org.wso2.carbon.identity.application.authenticator.basicauth;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.AbstractApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorStateInfo;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorStatus;
import org.wso2.carbon.identity.application.authentication.framework.LocalApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.RequestPathApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.config.ConfigurationFacade;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authenticator.basicauth.internal.BasicAuthenticatorServiceComponent;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

/**
 * Username Password based Authenticator
 *
 */
public class BasicAuthenticator extends AbstractApplicationAuthenticator implements LocalApplicationAuthenticator,RequestPathApplicationAuthenticator {

	private static final long serialVersionUID = 4438354156955223654L;
	
	private static Log log = LogFactory.getLog(BasicAuthenticator.class);
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.wso2.carbon.identity.application.authentication.framework.
	 * ApplicationAuthenticator#canHandle(javax.servlet.http.HttpServletRequest)
	 */
    @Override
    public boolean canHandle(HttpServletRequest request) {

    	if (log.isTraceEnabled()) {
    		log.trace("Inside canHandle()");
    	}
    	
        String userName = request.getParameter("username");
        String password = request.getParameter("password");

        if (userName != null && password != null) {
            return true;
        }
        
        return false;
    }
    
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.wso2.carbon.identity.application.authentication.framework.
	 * ApplicationAuthenticator
	 * #authenticate(javax.servlet.http.HttpServletRequest)
	 */
    @Override
    public AuthenticatorStatus authenticate(HttpServletRequest request, HttpServletResponse response, AuthenticationContext context) {
    	
    	if (log.isTraceEnabled()) {
    		log.trace("Inside authenticate()");
    	}
    	
    	String username = request.getParameter("username");
        String password = request.getParameter("password");

        boolean isAuthenticated = false;

        // Check the authentication
        try {
        	int tenantId = IdentityUtil.getTenantIdOFUser(username);
        	UserStoreManager userStoreManager = (UserStoreManager) BasicAuthenticatorServiceComponent.getRealmService().getTenantUserRealm(tenantId).getUserStoreManager();
	        isAuthenticated = userStoreManager.authenticate(MultitenantUtils.getTenantAwareUsername(username), password);
        } catch (IdentityException e) {
        	log.error("BasicAuthentication failed while trying to get the tenant ID of the use", e);
            return AuthenticatorStatus.FAIL;
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
        	log.error("BasicAuthentication failed while trying to authenticate", e);
            return AuthenticatorStatus.FAIL;
        }
        
        if (!isAuthenticated) {
            if (log.isDebugEnabled()) {
                log.debug("user authentication failed due to invalid credentials.");
            }
            
            // TODO implement retry count
            sendToLoginPage(request, response, context.getQueryParams() + "&authFailure=true");
            
            return AuthenticatorStatus.CONTINUE;
        }
        
        request.getSession().setAttribute("username", username);
        
        String rememberMe = request.getParameter("chkRemember");
        
        if (rememberMe != null && "on".equals(rememberMe)) {
        	context.setRememberMe(true);
        }
        
        return AuthenticatorStatus.PASS;
    }
    
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.wso2.carbon.identity.application.authentication.framework.
	 * ApplicationAuthenticator#getAuthenticatorName()
	 */
    @Override
    public String getAuthenticatorName() {
    	
    	if (log.isTraceEnabled()) {
    		log.trace("Inside getAuthenticatorName()");
    	}
    	
	    return BasicAuthenticatorConstants.AUTHENTICATOR_NAME;
    }

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.wso2.carbon.identity.application.authentication.framework.
	 * ApplicationAuthenticator
	 * #sendInitialRequest(javax.servlet.http.HttpServletRequest,
	 * javax.servlet.http.HttpServletResponse)
	 */
	@Override
    public void sendInitialRequest(HttpServletRequest request, HttpServletResponse response, AuthenticationContext context) {
		
		if (log.isTraceEnabled()) {
    		log.trace("Inside sendInitialRequest()");
    	}
		
		sendToLoginPage(request, response, context.getQueryParams());
    }
	
	private void sendToLoginPage(HttpServletRequest request, HttpServletResponse response, String queryParams) {

		if (log.isTraceEnabled()) {
    		log.trace("Inside sendToLoginPage()");
    	}
		
		String loginPage = ConfigurationFacade.getInstance().getAuthenticationEndpointURL();

		try {
			response.sendRedirect(response.encodeRedirectURL(loginPage + ("?" + queryParams)));
		} catch (IOException e) {
			log.error("Error when sending to the login page", e);
		}
		return;
	}

    @Override
    public AuthenticatorStatus logout(HttpServletRequest request, HttpServletResponse response, 
    		AuthenticationContext context, AuthenticatorStateInfo stateInfo) {
    	
    	if (log.isTraceEnabled()) {
    		log.trace("Inside logout()");
    	}
    	
    	// We cannot invalidate the session in case session is used by the calling servlet
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
		return null;
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
}
