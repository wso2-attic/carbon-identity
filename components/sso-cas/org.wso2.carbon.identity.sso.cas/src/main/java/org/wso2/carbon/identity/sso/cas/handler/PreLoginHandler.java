package org.wso2.carbon.identity.sso.cas.handler;

import java.io.IOException;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.identity.application.authentication.framework.cache.AuthenticationRequestCacheEntry;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticationRequest;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.sso.cas.CASEndpointConstants;
import org.wso2.carbon.identity.sso.cas.CASErrorConstants;
import org.wso2.carbon.identity.sso.cas.config.CASConfiguration;
import org.wso2.carbon.identity.sso.cas.ticket.LoginContext;
import org.wso2.carbon.identity.sso.cas.ticket.TicketGrantingTicket;
import org.wso2.carbon.identity.sso.cas.util.CASCookieUtil;
import org.wso2.carbon.identity.sso.cas.util.CASSSOUtil;
import org.wso2.carbon.registry.core.utils.UUIDGenerator;
import org.wso2.carbon.ui.CarbonUIUtil;

/***
 * This class preserves the original CAS login request and redirects the user through
 * common SSO authentication. There is also support for legacy SAML SSO arguments: 
 * TARGET and SAMLart.
 */
public class PreLoginHandler extends AbstractLoginHandler {

	private static Log log = LogFactory.getLog(PreLoginHandler.class);
	
	private String tenantDomain = null;
	
	public PreLoginHandler(String tenantDomain) {
		this.tenantDomain = tenantDomain;
	}

	@Override
	public void handle(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException,
			IdentityApplicationManagementException {
		String ticketGrantingTicketId = CASCookieUtil
				.getTicketGrantingTicketId(req);
		String storedSessionDataKey = CASCookieUtil.getSessionDataKey(req);

		String sessionDataKey = req
				.getParameter(FrameworkConstants.SESSION_DATA_KEY);
		
		if( sessionDataKey == null ) {
			sessionDataKey = (String)req.getAttribute(FrameworkConstants.SESSION_DATA_KEY);
		}
		
		String queryString = req.getQueryString();
		log.debug("CAS pre-login query string: " + queryString);
		
		String serviceProviderUrl = req
				.getParameter(ProtocolConstants.SERVICE_PROVIDER_ARGUMENT);

		LoginContext loginContext = CASSSOUtil.getLoginContextFromCache(sessionDataKey != null ? sessionDataKey : storedSessionDataKey);

		if( loginContext == null ) {
			loginContext = new LoginContext();
		}
		
		// Fall back to "TARGET" argument for SAML-related login. 
		// Older CAS clients use this argument for login instead of following
		// the CAS protocol specification.
		if( serviceProviderUrl == null ) {
			serviceProviderUrl = req
					.getParameter(ProtocolConstants.SAML_SERVICE_PROVIDER_ARGUMENT);
			
			if( serviceProviderUrl != null ) {
				log.debug("Found SAML login arguments");
				loginContext.setRedirectUrl(serviceProviderUrl);
				loginContext.setSAMLLogin(true);
			}
			
		} else {
			log.debug("Setting redirect URL: "+serviceProviderUrl);
			loginContext.setRedirectUrl(serviceProviderUrl);
		}
		
		if( serviceProviderUrl == null ) {
			serviceProviderUrl = loginContext.getRedirectUrl();
			log.debug("Getting redirect URL from login context: " + serviceProviderUrl);
		}
		
		String forceLoginString = req.getParameter(ProtocolConstants.RENEW_ARGUMENT);
		String passiveLoginString = req.getParameter(ProtocolConstants.GATEWAY_ARGUMENT);
		
		boolean forceLogin = (forceLoginString != null && forceLoginString.equals(HandlerConstants.TRUE_FLAG_STRING));
		boolean passiveLogin = (passiveLoginString != null && passiveLoginString.equals(HandlerConstants.TRUE_FLAG_STRING));  
		
		log.debug("ticketGrantingTicketId= " + ticketGrantingTicketId);

		if( ticketGrantingTicketId != null ) {
			// Generates an exception for a missing TGT early in the SSO process
			TicketGrantingTicket ticketGrantingTicket = CASSSOUtil.getTicketGrantingTicket(ticketGrantingTicketId);

			log.debug("Ticket granting ticket found for "+ ticketGrantingTicket.getPrincipal());
		}
		
		
		if( forceLogin && passiveLogin ) {
			showLoginError(resp, CASErrorConstants.INVALID_ARGUMENTS_RENEW_GATEWAY, req.getLocale());
		} else if ((ticketGrantingTicketId != null && serviceProviderUrl == null)
				|| (ticketGrantingTicketId == null
						&& serviceProviderUrl == null && storedSessionDataKey == null)) {
			showLoginError(resp, CASErrorConstants.SERVICE_PROVIDER_MISSING, req.getLocale());
		} // Allow login and check service provider authorization afterwards
//		else if (serviceProvider == null) {
//			showLoginError(resp, CASErrorConstants.SERVICE_PROVIDER_NOT_AUTHORIZED, req.getLocale());
		else {// if (ticketGrantingTicketId == null) {
			// Guarantee that a sessionDataKey is generated for existing SSO
			// infrastructure
			if (sessionDataKey == null) {
				log.debug("Generating new session data key");
				sessionDataKey = UUIDGenerator.generateUUID();
			}

			String commonAuthURL = CarbonUIUtil.getAdminConsoleURL(req);

			commonAuthURL = commonAuthURL.replace(
					CASConfiguration.buildTenantRelativePath(tenantDomain, "/login/carbon/"),
					HandlerConstants.COMMON_AUTH_ENDPOINT);
			
			String selfPath;
			
			if( passiveLogin && FrameworkUtils.getAuthCookie(req) == null) {
				// CAS Protocol states to redirect to the service provider URL without a service ticket
				selfPath = URLEncoder.encode(serviceProviderUrl, HandlerConstants.DEFAULT_ENCODING);
			} else {
				selfPath = URLEncoder.encode(
                        CASConfiguration.buildTenantRelativePath(tenantDomain, CASEndpointConstants.LOGIN_PATH),
                        HandlerConstants.DEFAULT_ENCODING);
			}

            if (tenantDomain == null || "null".equalsIgnoreCase(tenantDomain) || "".equals(tenantDomain)) {
                tenantDomain = MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
            }

            ServiceProvider serviceProvider = CASSSOUtil.getServiceProviderByUrlAndTenant(serviceProviderUrl, tenantDomain);

            // Let user know that the service provider is not authorized before login
            if( serviceProvider == null ) {
                showLoginError(resp, CASErrorConstants.SERVICE_PROVIDER_NOT_AUTHORIZED, req.getLocale());
                return;
            }

            // Populate arguments for CAS login
            loginContext.setRedirectUrl(serviceProviderUrl);
            loginContext.setForcedLogin(forceLogin);
            loginContext.setPassiveLogin(passiveLogin);
            loginContext.setLoginComplete(false);

            // Store arguments for after authentication
            CASSSOUtil.addLoginContextToCache(sessionDataKey, loginContext);

            // Create authentication request object instead of URL arguments
            AuthenticationRequest authenticationRequest = new AuthenticationRequest();
            authenticationRequest.setRelyingParty(serviceProvider.getApplicationName());
            authenticationRequest.setCommonAuthCallerPath(selfPath);
            authenticationRequest.setForceAuth(forceLogin);
            authenticationRequest.setPassiveAuth(passiveLogin);
            authenticationRequest.setTenantDomain(tenantDomain);

            AuthenticationRequestCacheEntry authRequest = new AuthenticationRequestCacheEntry(authenticationRequest);
            FrameworkUtils.addAuthenticationRequestToCache(sessionDataKey, authRequest, req.getSession().getMaxInactiveInterval());

			String queryParams = String.format(
					HandlerConstants.COMMON_AUTH_REDIRECT_URL,
                    serviceProvider.getApplicationName(),
					sessionDataKey, selfPath, forceLogin, passiveLogin);

            queryParams = "?" + FrameworkConstants.SESSION_DATA_KEY + "=" + sessionDataKey + "&type=cassso";

			log.debug("Redirect for CAS after authentication: " + commonAuthURL + queryParams);

			resp.sendRedirect(commonAuthURL + queryParams);
		}
	}
}
