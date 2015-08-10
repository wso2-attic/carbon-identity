package org.wso2.carbon.identity.sso.cas.handler;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
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
import org.wso2.carbon.identity.sso.cas.ticket.ServiceTicket;
import org.wso2.carbon.identity.sso.cas.ticket.TicketGrantingTicket;
import org.wso2.carbon.identity.sso.cas.util.CASCookieUtil;
import org.wso2.carbon.identity.sso.cas.util.CASSSOUtil;
import org.wso2.carbon.ui.CarbonUIUtil;

/***
 * This class processes the original CAS login request after common SSO authentication.
 * There is also support for legacy SAML SSO arguments: TARGET and SAMLart.
 */
public class PostLoginHandler extends AbstractLoginHandler {

	private static Log log = LogFactory.getLog(PostLoginHandler.class);

	@Override
	public void handle(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException,
			IdentityApplicationManagementException {
		String ticketGrantingTicketId = CASCookieUtil
				.getTicketGrantingTicketId(req);
		String storedSessionDataKey = CASCookieUtil.getSessionDataKey(req);

		String queryString = req.getQueryString();
		log.debug("CAS post-login query string: " + queryString);

		String serviceProviderUrl = req
				.getParameter(ProtocolConstants.SERVICE_PROVIDER_ARGUMENT);
		String sessionDataKey = req
				.getParameter(FrameworkConstants.SESSION_DATA_KEY);
		boolean loginComplete = false;
		boolean samlLogin = false;

		log.debug("ticketGrantingTicketId= " + ticketGrantingTicketId);

		String redirectUrl = null;
		String pathInfo = req.getRequestURI();
		
		log.debug("pathInfo= "+pathInfo);
		
		// Capture redirect after WSO2 authentication and check for CAS login
		// completion
		StringTokenizer st = new StringTokenizer(pathInfo, HandlerConstants.PATH_DELIMITER);
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			String[] tokenParts = token.split("=");
			if( tokenParts.length == 2 ) {
				if( tokenParts[0].equals(HandlerConstants.POST_AUTH_REDIRECT_ARGUMENT) ) {
					redirectUrl = new String(Base64.decodeBase64(tokenParts[1].getBytes()));
					log.debug(HandlerConstants.POST_AUTH_REDIRECT_ARGUMENT + "= " + redirectUrl);
				} else if( tokenParts[0].equals(HandlerConstants.POST_AUTH_SUCCESS_ARGUMENT)) {
					loginComplete = HandlerConstants.TRUE_FLAG_STRING.equals(tokenParts[1]);
					log.debug(HandlerConstants.POST_AUTH_SUCCESS_ARGUMENT + "= " + loginComplete);
				} else if( tokenParts[0].equals(HandlerConstants.POST_AUTH_SAML_LOGIN_ARGUMENT)) {
					samlLogin = HandlerConstants.TRUE_FLAG_STRING.equals(tokenParts[1]);
					log.debug(HandlerConstants.POST_AUTH_SAML_LOGIN_ARGUMENT + "= " + samlLogin);
				}
			}
		}
		
		// Ticket granting ticket is required to generate service tickets for
		// service providers
		TicketGrantingTicket ticketGrantingTicket;

		// After authentication completes, before final CAS session
		if (sessionDataKey != null && !loginComplete) {

			String commonAuthURL = CarbonUIUtil.getAdminConsoleURL(req);

			// Move the AuthenticationResult to the new sessionDataKey
			// for future requests and remove the old entry
			AuthenticationResult authResult = CASSSOUtil
					.getAuthenticationResultFromCache(sessionDataKey);
			FrameworkUtils.addAuthenticationResultToCache(storedSessionDataKey,
					authResult, CASConfiguration.getCacheTimeout());
			removeAuthenticationResultFromCache(sessionDataKey);

			ServiceProvider serviceProvider = CASSSOUtil
					.getServiceProviderByUrl(redirectUrl, authResult.getSubject().toString());
			
			// Allow login and let them know that the service provider not authorized
			if( serviceProvider == null ) {
				showLoginError(resp, CASErrorConstants.SERVICE_PROVIDER_NOT_AUTHORIZED, req.getLocale());
				return;
			}
			
			commonAuthURL = commonAuthURL.replace(
					CASConfiguration.buildRelativePath("/login/carbon/"),
					HandlerConstants.COMMON_AUTH_ENDPOINT);

            String urlSafeEncoded = new String(Base64.encodeBase64(redirectUrl.getBytes(), true)).replaceAll("+",
                    "-").replaceAll("/","_");

            String selfPath = URLEncoder.encode(
					CASConfiguration.buildRelativePath(
					String.format(
							HandlerConstants.POST_CAS_LOGIN_PATH_TEMPLATE,
					urlSafeEncoded,
					samlLogin,
					HandlerConstants.POST_AUTH_SUCCESS_NAME_VALUE)),
					HandlerConstants.DEFAULT_ENCODING);

			String queryParams = String.format(
					HandlerConstants.COMMON_AUTH_REDIRECT_URL,
					serviceProvider.getApplicationName(),
					sessionDataKey, selfPath, false, false);

			resp.sendRedirect(commonAuthURL + queryParams);
		} else {

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

				} else { // Existing TGT found
					ticketGrantingTicket = CASSSOUtil
							.getTicketGrantingTicket(ticketGrantingTicketId);
					if( serviceProviderUrl != null ) {
						redirectUrl = serviceProviderUrl;
					}
				}

				CASCookieUtil.storeTicketGrantingCookie(
						ticketGrantingTicket.getId(), req, resp, 0);
				
				String baseUrl = CASSSOUtil.getBaseUrl((serviceProviderUrl != null) ? serviceProviderUrl : redirectUrl, false); 

				ServiceTicket serviceTicket = ticketGrantingTicket
						.grantServiceTicket(
								serviceProvider, 
								baseUrl, 
								samlLogin);

				String serviceTicketId = serviceTicket.getId();
				
				log.debug("Service ticket created: " + serviceTicketId);

				// Remove "sessionDataKey" from CAS service provider
				// redirect; consuming client does not need to understand
				// WSO2 SSO in order to use CAS protocol.
				int sessionDataKeyPosition = redirectUrl
						.indexOf("sessionDataKey");

				if (sessionDataKeyPosition > -1) {
					redirectUrl = redirectUrl.substring(0,
							sessionDataKeyPosition);
				}

				String redirectArgument = (samlLogin) ? 
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
