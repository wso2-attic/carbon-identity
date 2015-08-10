package org.wso2.carbon.identity.sso.cas.handler;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.cache.AuthenticationRequestCacheEntry;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticationRequest;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.sso.cas.config.CASConfiguration;
import org.wso2.carbon.identity.sso.cas.ticket.TicketGrantingTicket;
import org.wso2.carbon.identity.sso.cas.util.CASCookieUtil;
import org.wso2.carbon.identity.sso.cas.util.CASLogoutSender;
import org.wso2.carbon.identity.sso.cas.util.CASPageTemplates;
import org.wso2.carbon.identity.sso.cas.util.CASSSOUtil;
import org.wso2.carbon.ui.CarbonUIUtil;

public class LogoutHandler {
	private static Log log = LogFactory.getLog(LogoutHandler.class);

	@SuppressWarnings("rawtypes")
	public void handle(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		log.debug("CAS " + req.getRequestURI() + " query string: "
				+ req.getQueryString());

		String ticketGrantingTicketId = null;
		Cookie ticketGrantingCookie = CASCookieUtil
				.getTicketGrantingCookie(req);

		if (ticketGrantingCookie != null) {
			ticketGrantingTicketId = ticketGrantingCookie.getValue();
		}

		boolean legacyLogoutDetected = false;
		boolean logoutErrors = false;
		
		String returnUrl = req
				.getParameter(ProtocolConstants.LOGOUT_URL_ARGUMENT);
		String sessionDataKey = req
				.getParameter(FrameworkConstants.SESSION_DATA_KEY);
		String sessionLogoutComplete = req
				.getParameter(HandlerConstants.LOGOUT_COMPLETE_ARGUMENT);

		// Fall back to CAS 1.0/2.0 return URL argument
		if (returnUrl == null) {
			returnUrl = req
					.getParameter(ProtocolConstants.LEGACY_LOGOUT_URL_ARGUMENT);
			legacyLogoutDetected = (returnUrl != null);
			logoutErrors = (returnUrl == null);
		}

		String logoutHtml = null;

		// Single Logout must occur
		if (sessionLogoutComplete == null && ticketGrantingTicketId != null) {
			String commonAuthURL = CarbonUIUtil.getAdminConsoleURL(req);

			commonAuthURL = commonAuthURL.replace(
					CASConfiguration.buildRelativePath("/logout/carbon/"),
					HandlerConstants.COMMON_AUTH_ENDPOINT);
			
			String selfPath = URLEncoder.encode(
					CASConfiguration.buildRelativePath("/logout?"
							+ buildLogoutReturnUrl(returnUrl, legacyLogoutDetected)
							+ HandlerConstants.LOGOUT_COMPLETE_NAME_VALUE),
							HandlerConstants.DEFAULT_ENCODING);

			// Retrieve the associated session key
			if (sessionDataKey == null) {
				sessionDataKey = CASCookieUtil.getSessionDataKey(req);
			}

			String serviceProviderName = getFirstServiceProvider(ticketGrantingTicketId);

			if (serviceProviderName == null) {
				try {
					// Clean up CAS artifacts for invalid session
					logoutCASSession(req, resp, ticketGrantingTicketId);

					// remove the SessionContext from the cache
					FrameworkUtils
							.removeSessionContextFromCache(sessionDataKey);

					// Remove the WSO2 session cookie
					FrameworkUtils.removeAuthCookie(req, resp);

					String forcedCASLogoutUrl = CarbonUIUtil
							.getAdminConsoleURL(req);

					forcedCASLogoutUrl = forcedCASLogoutUrl.replace(
							CASConfiguration.buildRelativePath("/logout/carbon/"), URLDecoder.decode(selfPath,
									HandlerConstants.DEFAULT_ENCODING));

					resp.sendRedirect(forcedCASLogoutUrl);
				} finally {
					log.debug("CAS ticket granting ticket for logout is invalid: "
							+ ticketGrantingTicketId);
				}
			} else {
				// Use AuthenticationRequest in addition to URL arguments to mimic 
				// logout handling in org.wso2.carbon.identity.sso.saml.servlet.SAMLSSOProviderServlet
				AuthenticationRequest authenticationRequest = new AuthenticationRequest();
				authenticationRequest.addRequestQueryParam(
						FrameworkConstants.RequestParams.LOGOUT,
						new String[] { "true" });
				authenticationRequest.setRequestQueryParams(req
						.getParameterMap());
				authenticationRequest.setCommonAuthCallerPath(selfPath);

				authenticationRequest.setRelyingParty(serviceProviderName);

				authenticationRequest.appendRequestQueryParams(req
						.getParameterMap());
				
				// Add headers to AuthenticationRequestContext
				for (Enumeration e = req.getHeaderNames(); e.hasMoreElements();) {
					String headerName = e.nextElement().toString();
					authenticationRequest.addHeader(headerName,
							req.getHeader(headerName));
				}

				AuthenticationRequestCacheEntry authRequest = new AuthenticationRequestCacheEntry(
						authenticationRequest);
				FrameworkUtils.addAuthenticationRequestToCache(sessionDataKey,
						authRequest, req.getSession().getMaxInactiveInterval());

				String queryParams = "?" 
						+ buildLogoutReturnUrl(returnUrl, legacyLogoutDetected)
						+ "&"
						+ FrameworkConstants.SESSION_DATA_KEY + "="
						+ sessionDataKey + "&type=samlsso"
						+ "&commonAuthCallerPath=" + selfPath
						+ "&commonAuthLogout=true";
				resp.sendRedirect(commonAuthURL + queryParams);
			}
		} else { // Single Logout has completed, now cleanup the CAS artifacts
			try {
				if (ticketGrantingTicketId == null) {
					log.debug("CAS logout proceeded without ticket granting ticket");
					logoutHtml = CASPageTemplates.getInstance()
							.showLogoutSuccess(returnUrl, req.getLocale());
					logoutErrors = true;
				} else {
					if (legacyLogoutDetected || logoutErrors) {
						logoutHtml = CASPageTemplates.getInstance()
								.showLogoutSuccess(returnUrl, req.getLocale());
					}

					logoutCASSession(req, resp, ticketGrantingTicketId);

				}
			} catch (Exception ex) {
				// Log at info level to avoid flooding logs from attacks
				log.debug("CAS logout failed for ticket "
						+ ticketGrantingTicketId);
				logoutHtml = CASPageTemplates.getInstance().showLogoutError(
						ex.getMessage(), req.getLocale());
				logoutErrors = true;
			}

			sendLogoutResponse(resp, legacyLogoutDetected, logoutErrors,
					logoutHtml, returnUrl);
		}
	}
	
	private String buildLogoutReturnUrl(String returnUrl, boolean legacyLogoutDetected) throws UnsupportedEncodingException {
		String logoutReturnUrl = ( returnUrl != null ) ?
			CASSSOUtil.buildUrlArgument(
					(legacyLogoutDetected) ? ProtocolConstants.LEGACY_LOGOUT_URL_ARGUMENT : ProtocolConstants.LOGOUT_URL_ARGUMENT,
					URLEncoder.encode(returnUrl, HandlerConstants.DEFAULT_ENCODING)
			) : "";
		
		return logoutReturnUrl;
	}

	private void logoutCASSession(HttpServletRequest req,
			HttpServletResponse resp, String ticketGrantingTicketId) {
		// Proceed with session cleanup
		CASLogoutSender.getInstance().logoutSession(ticketGrantingTicketId);

		CASCookieUtil.removeTicketGrantingCookie(req, resp);
	}

	private void sendLogoutResponse(HttpServletResponse resp,
			boolean legacyLogoutDetected, boolean logoutErrors,
			String logoutHtml, String returnUrl) throws IOException {
		// CAS 1.0/2.0 displays a logout page in all cases
		// CAS 3.0 displays a logout page when "service" argument is missing
		if (legacyLogoutDetected || logoutErrors) {
			resp.getWriter().write(logoutHtml);
		} else { // CAS 3.0 redirects to the "service" argument
			resp.sendRedirect(returnUrl);
		}
	}

	private String getFirstServiceProvider(String ticketGrantingTicketId) {
		String serviceProvider = null;

		try {
			TicketGrantingTicket ticketGrantingTicket = CASSSOUtil
					.getTicketGrantingTicket(ticketGrantingTicketId);
	
			if (ticketGrantingTicket != null) {
				serviceProvider = ticketGrantingTicket.getLastServiceProvider();
			}
		} catch(Exception ex) {
			// Leave serviceProvider null
		}

		return serviceProvider;
	}
}
