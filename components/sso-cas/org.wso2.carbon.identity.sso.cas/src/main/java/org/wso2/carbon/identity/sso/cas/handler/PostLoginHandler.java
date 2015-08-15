package org.wso2.carbon.identity.sso.cas.handler;

import java.io.IOException;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.cache.AuthenticationResultCache;
import org.wso2.carbon.identity.application.authentication.framework.cache.AuthenticationResultCacheKey;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticationResult;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.sso.cas.CASErrorConstants;
import org.wso2.carbon.identity.sso.cas.config.CASConfiguration;
import org.wso2.carbon.identity.sso.cas.exception.ServiceProviderNotFoundException;
import org.wso2.carbon.identity.sso.cas.ticket.LoginContext;
import org.wso2.carbon.identity.sso.cas.ticket.ServiceTicket;
import org.wso2.carbon.identity.sso.cas.ticket.TicketGrantingTicket;
import org.wso2.carbon.identity.sso.cas.util.CASCookieUtil;
import org.wso2.carbon.identity.sso.cas.util.CASSSOUtil;

/***
 * This class processes the original CAS login request after common SSO authentication.
 * There is also support for legacy SAML SSO arguments: TARGET and SAMLart.
 */
public class PostLoginHandler extends AbstractLoginHandler {

	private static Log log = LogFactory.getLog(PostLoginHandler.class);
	
	private String tenantDomain = null;
	
	public PostLoginHandler(String tenantDomain) {
		this.tenantDomain = tenantDomain;
	}

	@Override
	public void handle(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException,
			IdentityApplicationManagementException {
		String ticketGrantingTicketId = CASCookieUtil
				.getTicketGrantingTicketId(req);
		String storedSessionDataKey = CASCookieUtil.getSessionDataKey(req);

		String queryString = req.getQueryString();
		log.debug("CAS post-login query string: " + queryString);
        log.debug("storedSessionDataKey= " + storedSessionDataKey);

		String serviceProviderUrl = req
				.getParameter(ProtocolConstants.SERVICE_PROVIDER_ARGUMENT);
		String sessionDataKey = req
				.getParameter(FrameworkConstants.SESSION_DATA_KEY);

        // Check for a sessionDataKey in the session attributes
        if( sessionDataKey == null ) {
            sessionDataKey = (String)req.getAttribute(FrameworkConstants.SESSION_DATA_KEY);
        }

        log.debug("sessionDataKey= " + sessionDataKey);
		log.debug("ticketGrantingTicketId= " + ticketGrantingTicketId);

		String redirectUrl = null;

        LoginContext loginContext = CASSSOUtil.getLoginContextFromCache(sessionDataKey);

        if( loginContext == null ) {
            log.debug("LoginContext for stored session data key");
            loginContext = CASSSOUtil.getLoginContextFromCache(storedSessionDataKey);
		}

        redirectUrl = loginContext.getRedirectUrl();

		// Ticket granting ticket is required to generate service tickets for
		// service providers
		TicketGrantingTicket ticketGrantingTicket;

        if (sessionDataKey != null ) {
            // After authentication completes, before final CAS session
            if(!loginContext.isLoginComplete()) {
                log.debug("login not complete");

                // Move the AuthenticationResult to the new sessionDataKey
                // for future requests and remove the old entry
                AuthenticationResult authResult = CASSSOUtil.getAuthenticationResultFromCache(sessionDataKey);

                FrameworkUtils.addAuthenticationResultToCache(storedSessionDataKey,
                        authResult, CASConfiguration.getCacheTimeout());
                removeAuthenticationResultFromCache(sessionDataKey);

                loginContext.setLoginComplete(true);

                // Remove and add the login context for guaranteed serialization to the database
                CASSSOUtil.removeLoginContextFromCache(sessionDataKey);
                CASSSOUtil.addLoginContextToCache(storedSessionDataKey, loginContext);

                ServiceProvider serviceProvider = CASSSOUtil.getServiceProviderByUrl(redirectUrl,
                        authResult.getSubject().toString());

                // Allow login and let them know that the service provider not authorized
                if( serviceProvider == null ) {
                    showLoginError(resp, CASErrorConstants.SERVICE_PROVIDER_NOT_AUTHORIZED, req.getLocale());
                    return;
                }

			}
			


			try {
				
				AuthenticationResult authResult = CASSSOUtil
						.getAuthenticationResultFromCache(storedSessionDataKey);
				
				ServiceProvider serviceProvider = (serviceProviderUrl != null) ? CASSSOUtil
						.getServiceProviderByUrl(serviceProviderUrl, authResult.getSubject().toString())
						: CASSSOUtil.getServiceProviderByUrl(redirectUrl, authResult.getSubject().toString());

				// Generate ticket granting ticket for new CAS session
				if (ticketGrantingTicketId == null && redirectUrl != null) {

					ticketGrantingTicket = CASSSOUtil.createTicketGrantingTicket(
							storedSessionDataKey, authResult.getSubject().toString(), false);
                    log.debug("Creating ticket granting ticket " + ticketGrantingTicket.getId());
				} else { // Existing TGT found
					ticketGrantingTicket = CASSSOUtil
							.getTicketGrantingTicket(ticketGrantingTicketId);
					if( serviceProviderUrl != null ) {
						redirectUrl = serviceProviderUrl;
					}
                    log.debug("Reusing ticket granting ticket " + ticketGrantingTicket.getId());
				}

				CASCookieUtil.storeTicketGrantingCookie(ticketGrantingTicket.getId(), req, resp, tenantDomain);

				
				String baseUrl = CASSSOUtil.getBaseUrl((serviceProviderUrl != null) ? serviceProviderUrl : redirectUrl, false); 

				ServiceTicket serviceTicket = ticketGrantingTicket
						.grantServiceTicket(
								serviceProvider, 
								baseUrl, 
								loginContext.isSAMLLogin());

				String serviceTicketId = serviceTicket.getId();
				
				log.debug("Service ticket created: " + serviceTicketId);
				
				CASSSOUtil.removeLoginContextFromCache(storedSessionDataKey);

				// Remove "sessionDataKey" from CAS service provider
				// redirect; consuming client does not need to understand
				// WSO2 SSO in order to use CAS protocol.
				int sessionDataKeyPosition = redirectUrl
						.indexOf(FrameworkConstants.SESSION_DATA_KEY);

				if (sessionDataKeyPosition > -1) {
					redirectUrl = redirectUrl.substring(0,
							sessionDataKeyPosition);
				}

				String redirectArgument = (loginContext.isSAMLLogin()) ? 
						CASSSOUtil.buildUrlArgument(ProtocolConstants.SAML_SERVICE_PROVIDER_ARGUMENT, 
								URLEncoder.encode(redirectUrl, HandlerConstants.DEFAULT_ENCODING)
								) +
						CASSSOUtil.buildUrlArgument(ProtocolConstants.SAML_SERVICE_TICKET_ARGUMENT, 
								URLEncoder.encode(serviceTicketId, HandlerConstants.DEFAULT_ENCODING)) : 
							CASSSOUtil.buildUrlArgument(ProtocolConstants.SERVICE_TICKET_ARGUMENT, serviceTicketId);
				
				log.debug("redirectArgument="+redirectArgument);
						
				if (redirectUrl.indexOf('?') < 0) {
					redirectUrl += "?";
				}
						
				// Append the service ticket to the CAS service provider URL
				redirectUrl = redirectUrl + redirectArgument;

				log.debug("redirecting back to service provider: "+redirectUrl);
				
				resp.sendRedirect(redirectUrl);
			} catch (ServiceProviderNotFoundException ex) {
				showLoginError(resp, "cas.service.provider.not.authorized", req.getLocale());
			}
		}
	}

	private void removeAuthenticationResultFromCache(String sessionDataKey) {
		if (sessionDataKey != null) {
			AuthenticationResultCacheKey cacheKey = new AuthenticationResultCacheKey(
					sessionDataKey);
			AuthenticationResultCache.getInstance(0).clearCacheEntry(cacheKey);
		}
	}
}
