package org.wso2.carbon.identity.application.authenticator.basicauth;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

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
import org.wso2.carbon.identity.core.model.IdentityErrorMsgContext;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.util.UserCoreUtil;
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

        Map<String, String> parameterMap = getAuthenticatorConfig().getParameterMap();
        String showAuthFailureReason = null;
        if(parameterMap != null) {
                showAuthFailureReason = parameterMap.get("showAuthFailureReason");
            if(log.isDebugEnabled()) {
                log.debug("showAuthFailureReason has been set as : " + showAuthFailureReason);
            }
        }
		
		String loginPage = ConfigurationFacade.getInstance().getAuthenticationEndpointURL();
		String queryParams = context.getContextIdIncludedQueryParams();
		
		try {
		    String retryParam = "";
            
            if (context.isRetrying()) {
                retryParam = "&authFailure=true&authFailureMsg=login.fail.message";
            }

            IdentityErrorMsgContext errorContext = IdentityUtil.getIdentityErrorMsg();
            IdentityUtil.clearIdentityErrorMsg();

            if(showAuthFailureReason != null && showAuthFailureReason.equals("true")) {
                if (errorContext != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Identity error message context is not null");
                    }

                    String errorCode = errorContext.getErrorCode();
                    int remainingAttempts = errorContext.getMaximumLoginAttempts() - errorContext.getFailedLoginAttempts();

                    if (log.isDebugEnabled()) {
                        log.debug("errorCode : " + errorCode);
                        log.debug("username : " + request.getParameter("username"));
                        log.debug("remainingAttempts : " + remainingAttempts);
                    }

                    if (errorCode.equals(UserCoreConstants.ErrorCode.INVALID_CREDENTIAL)) {
                        retryParam = retryParam + "&errorCode=" + errorCode
                                + "&failedUsername=" + URLEncoder.encode(request.getParameter("username"),"UTF-8")
                                + "&remainingAttempts=" + remainingAttempts;
                        response.sendRedirect(response.encodeRedirectURL(loginPage + ("?" + queryParams))
                                + "&authenticators=" + getName() + ":" + "LOCAL" + retryParam);
                    } else if (errorCode.equals(UserCoreConstants.ErrorCode.USER_IS_LOCKED)) {
                        String redirectURL = loginPage.replace("login.do", "retry.do");
                        redirectURL =  response.encodeRedirectURL( redirectURL + ("?" + queryParams))  + "&errorCode=" + errorCode
                                + "&failedUsername=" + URLEncoder.encode(request.getParameter("username"),"UTF-8");
                        response.sendRedirect(redirectURL);

                    } else if (errorCode.equals(UserCoreConstants.ErrorCode.USER_DOES_NOT_EXIST)) {
                        retryParam = retryParam + "&errorCode=" + errorCode
                                + "&failedUsername=" + URLEncoder.encode(request.getParameter("username"),"UTF-8");
                        response.sendRedirect(response.encodeRedirectURL(loginPage + ("?" + queryParams))
                                + "&authenticators=" + getName() + ":" + "LOCAL" + retryParam);
                    }
                } else {
                    response.sendRedirect(response.encodeRedirectURL(loginPage + ("?" + queryParams))
                            + "&authenticators=" + getName() + ":" + "LOCAL" + retryParam);
                }
            } else {
                String errorCode = errorContext != null ? errorContext.getErrorCode() : null;
                if (errorCode!= null && errorCode.equals(UserCoreConstants.ErrorCode.USER_IS_LOCKED)) {
                    String redirectURL = loginPage.replace("login.do", "retry.do");
                    redirectURL =  response.encodeRedirectURL( redirectURL + ("?" + queryParams)) + "&failedUsername=" + URLEncoder.encode(request.getParameter("username"), "UTF-8");
                    response.sendRedirect(redirectURL);

                } else {
                    response.sendRedirect(response.encodeRedirectURL(loginPage + ("?" + queryParams))
                            + "&authenticators=" + getName() + ":" + "LOCAL" + retryParam);
                }
            }


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
		UserStoreManager userStoreManager;
		// Check the authentication
		try {
			int tenantId = IdentityUtil.getTenantIdOFUser(username);
            UserRealm userRealm = BasicAuthenticatorServiceComponent.getRealmService()
                    .getTenantUserRealm(tenantId);
            
            if (userRealm != null) {
				userStoreManager = (UserStoreManager) userRealm.getUserStoreManager();
				isAuthenticated = userStoreManager.authenticate(MultitenantUtils.getTenantAwareUsername(username),password);
            } else {
                throw new AuthenticationFailedException("Cannot find the user realm for the given tenant: " + tenantId);
            }
		} catch (IdentityException e) {
			log.debug("BasicAuthentication failed while trying to get the tenant ID of the use", e);
			throw new AuthenticationFailedException(e.getMessage(), e);
		} catch (org.wso2.carbon.user.api.UserStoreException e) {
			log.debug("BasicAuthentication failed while trying to authenticate", e);
			throw new AuthenticationFailedException(e.getMessage(), e);
		}

		if (!isAuthenticated) {
			if (log.isDebugEnabled()) {
				log.debug("user authentication failed due to invalid credentials.");
            }

            throw new InvalidCredentialsException();
		}
		
        Map<String, Object> authProperties = context.getProperties();
		String tenantDomain = MultitenantUtils.getTenantDomain(username);

        if (authProperties==null){
        	authProperties = new HashMap<String, Object>();
        	context.setProperties(authProperties);
        }
        
        //TODO: user tenant domain has to be an attribute in the AuthenticationContext
        authProperties.put("user-tenant-domain", tenantDomain);
        
		username = FrameworkUtils.prependUserStoreDomainToName(username);

		if (getAuthenticatorConfig().getParameterMap() != null) {
			String userNameUri = getAuthenticatorConfig().getParameterMap().get("UserNameAttributeClaimUri");
			if (userNameUri != null && userNameUri.trim().length() > 0) {
				boolean multipleAttributeEnable;
				String domain = UserCoreUtil.getDomainFromThreadLocal();
				if (domain != null && domain.trim().length() > 0) {
					multipleAttributeEnable = Boolean.parseBoolean(userStoreManager.getSecondaryUserStoreManager(domain).
							getRealmConfiguration().getUserStoreProperty("MultipleAttributeEnable"));
				} else {
					multipleAttributeEnable = Boolean.parseBoolean(userStoreManager.
							getRealmConfiguration().getUserStoreProperty("MultipleAttributeEnable"));
				}
				if (multipleAttributeEnable) {
					try {
						if (log.isDebugEnabled()) {
							log.debug("Searching for UserNameAttribute value for user " + username +
									" for claim uri : " + userNameUri);
						}
						String usernameValue = userStoreManager.
								getUserClaimValue(MultitenantUtils.getTenantAwareUsername(username), userNameUri, null);
						if (usernameValue != null && usernameValue.trim().length() > 0) {
							tenantDomain = MultitenantUtils.getTenantDomain(username);
							usernameValue = FrameworkUtils.prependUserStoreDomainToName(usernameValue);
							username = usernameValue + "@" + tenantDomain;
							if (log.isDebugEnabled()) {
								log.debug("UserNameAttribute is found for user. Value is :  " + username);
							}
						}
					} catch (UserStoreException e) {
						//ignore  but log in debug
						log.debug("Error while retrieving UserNameAttribute for user : " + username, e);
					}
				} else {
					if (log.isDebugEnabled()) {
						log.debug("MultipleAttribute is not enabled for user store domain : " + domain + " " +
								"Therefore UserNameAttribute is not retrieved");
					}
				}
			}
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
