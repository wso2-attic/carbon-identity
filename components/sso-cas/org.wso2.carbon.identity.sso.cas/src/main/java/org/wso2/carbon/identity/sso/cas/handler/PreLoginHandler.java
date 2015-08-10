package org.wso2.carbon.identity.sso.cas.handler;

import java.io.IOException;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.sso.cas.CASErrorConstants;
import org.wso2.carbon.identity.sso.cas.config.CASConfiguration;
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

	@Override
	public void handle(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException,
			IdentityApplicationManagementException {
		String ticketGrantingTicketId = CASCookieUtil
				.getTicketGrantingTicketId(req);
		String storedSessionDataKey = CASCookieUtil.getSessionDataKey(req);
		boolean samlLogin = false;

		String queryString = req.getQueryString();
		log.debug("CAS pre-login query string: " + queryString);

		String serviceProviderUrl = req
				.getParameter(ProtocolConstants.SERVICE_PROVIDER_ARGUMENT);
		
		// Fall back to "TARGET" argument for SAML-related login. 
		// Older CAS clients use this argument for login instead of following
		// the CAS protocol specification.
		if( serviceProviderUrl == null ) {
			log.debug("Found SAML login arguments");
			serviceProviderUrl = req
					.getParameter(ProtocolConstants.SAML_SERVICE_PROVIDER_ARGUMENT);
			samlLogin = true;
		}
		
		String sessionDataKey = req
				.getParameter(FrameworkConstants.SESSION_DATA_KEY);
		
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
				sessionDataKey = UUIDGenerator.generateUUID();
			}

			String commonAuthURL = CarbonUIUtil.getAdminConsoleURL(req);

			commonAuthURL = commonAuthURL.replace(
					CASConfiguration.buildRelativePath("/login/carbon/"),
					HandlerConstants.COMMON_AUTH_ENDPOINT);
			
			String selfPath;
			
			if( passiveLogin && FrameworkUtils.getAuthCookie(req) == null) {
				// CAS Protocol states to redirect to the service provider URL without a service ticket
				selfPath = URLEncoder.encode(serviceProviderUrl, HandlerConstants.DEFAULT_ENCODING);
			} else {
                String urlSafeEncoded = new String(Base64.encodeBase64(serviceProviderUrl.getBytes(), true)).replaceAll("+",
                        "-").replaceAll("/","_");

				selfPath = URLEncoder.encode(
							CASConfiguration.buildRelativePath(
									String.format(
											HandlerConstants.PRE_CAS_LOGIN_PATH_TEMPLATE,
												urlSafeEncoded.getBytes(),
												samlLogin
											)
									), 
									HandlerConstants.DEFAULT_ENCODING);
			}

			String queryParams = String.format(
					HandlerConstants.COMMON_AUTH_REDIRECT_URL,
					"",//serviceProvider.getApplicationName(),
					sessionDataKey, selfPath, forceLogin, passiveLogin);

			log.debug("Redirect for CAS after authentication: " + commonAuthURL + queryParams);

			resp.sendRedirect(commonAuthURL + queryParams);
		}
	}
}
