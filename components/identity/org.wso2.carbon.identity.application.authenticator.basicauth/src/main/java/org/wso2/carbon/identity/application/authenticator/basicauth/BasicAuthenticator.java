package org.wso2.carbon.identity.application.authenticator.basicauth;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.AbstractApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorFlowStatus;
import org.wso2.carbon.identity.application.authentication.framework.LocalApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.config.ConfigurationFacade;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;
import org.wso2.carbon.identity.application.authentication.framework.exception.InvalidCredentialsException;
import org.wso2.carbon.identity.application.authentication.framework.exception.LogoutFailedException;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.application.authenticator.basicauth.internal.BasicAuthenticatorServiceComponent;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

/**
 * Username Password based Authenticator
 * 
 */
public class BasicAuthenticator extends AbstractApplicationAuthenticator
		implements LocalApplicationAuthenticator {

	private static final long serialVersionUID = 4438354156955223654L;

	private static Log log = LogFactory.getLog(BasicAuthenticator.class);

	@Override
	public boolean canHandle(HttpServletRequest request) {

		String userName = request.getParameter("username");
		String password = request.getParameter("password");

		if (userName != null && password != null) {
			return true;
		}

		return false;
	}

    @Override
    public AuthenticatorFlowStatus process(HttpServletRequest request,
                                           HttpServletResponse response, AuthenticationContext context)
            throws AuthenticationFailedException, LogoutFailedException {

        if (context.isLogoutRequest()) {
            return AuthenticatorFlowStatus.SUCCESS_COMPLETED;
        } else {
            return super.process(request, response, context);
        }
    }

	@Override
	protected void initiateAuthenticationRequest(HttpServletRequest request,
			HttpServletResponse response, AuthenticationContext context)
			throws AuthenticationFailedException {
		
		String loginPage = ConfigurationFacade.getInstance().getAuthenticationEndpointURL();
		String queryParams = FrameworkUtils
				.getQueryStringWithFrameworkContextId(context.getQueryParams(),
						context.getCallerSessionKey(),
						context.getContextIdentifier());
		
		try {
		    String retryParam = "";
            
            if (context.isRetrying()) {
                retryParam = "&authFailure=true&authFailureMsg=login.fail.message";
            }
		    
            response.sendRedirect(response.encodeRedirectURL(loginPage + ("?" + queryParams))
                    + "&authenticators=" + getName() + ":" + "LOCAL" + retryParam);
		} catch (IOException e) {
			throw new AuthenticationFailedException(e.getMessage(), e);
		}
	}

	@Override
	protected void processAuthenticationResponse(HttpServletRequest request,
			HttpServletResponse response, AuthenticationContext context)
			throws AuthenticationFailedException {
		
		String username = request.getParameter("username");
		String password = request.getParameter("password");

		boolean isAuthenticated = false;

		// Check the authentication
		try {
			int tenantId = IdentityUtil.getTenantIdOFUser(username);
            UserRealm userRealm = BasicAuthenticatorServiceComponent.getRealmService()
                    .getTenantUserRealm(tenantId);
            
            if (userRealm != null) {
                UserStoreManager userStoreManager = (UserStoreManager)userRealm.getUserStoreManager();
                isAuthenticated = userStoreManager.authenticate(MultitenantUtils.getTenantAwareUsername(username),password);
            } else {
                throw new AuthenticationFailedException("Cannot find the user realm for the given tenant: " + tenantId);
            }
		} catch (IdentityException e) {
			log.error("BasicAuthentication failed while trying to get the tenant ID of the use", e);
			throw new AuthenticationFailedException(e.getMessage(), e);
		} catch (org.wso2.carbon.user.api.UserStoreException e) {
			log.error("BasicAuthentication failed while trying to authenticate", e);
			throw new AuthenticationFailedException(e.getMessage(), e);
		}

		if (!isAuthenticated) {
			if (log.isDebugEnabled()) {
				log.debug("user authentication failed due to invalid credentials.");
            }

            throw new InvalidCredentialsException();
		}

		context.setSubject(username);
		String rememberMe = request.getParameter("chkRemember");

		if (rememberMe != null && "on".equals(rememberMe)) {
			context.setRememberMe(true);
		}
	}
	
	@Override
	protected boolean retryAuthenticationEnabled() {
		return true;
	}
	
	@Override
	public String getContextIdentifier(HttpServletRequest request) {
		return request.getParameter("sessionDataKey");
	}

	@Override
	public String getFriendlyName() {
		return BasicAuthenticatorConstants.AUTHENTICATOR_FRIENDLY_NAME;
	}

	@Override
	public String getName() {
		return BasicAuthenticatorConstants.AUTHENTICATOR_NAME;
	}
}
