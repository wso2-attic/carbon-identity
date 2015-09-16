package org.wso2.carbon.identity.sso.cas.handler;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.sso.cas.CASErrorConstants;
import org.wso2.carbon.identity.sso.cas.ticket.ServiceTicket;
import org.wso2.carbon.identity.sso.cas.ticket.TicketGrantingTicket;
import org.wso2.carbon.identity.sso.cas.util.CASResourceReader;
import org.wso2.carbon.identity.sso.cas.util.CASSSOUtil;

public class ProxyLoginHandler extends ServiceValidationHandler {

	private static Log log = LogFactory.getLog(ProxyLoginHandler.class);
	private static final String success = "<cas:proxySuccess>%s</cas:proxySuccess>";
	private static final String proxyTicketTemplate = "<cas:proxyTicket>%s</cas:proxyTicket>";
	private static final String failure = "<cas:proxyFailure code=\"%s\">%s</cas:proxyFailure>";

	@Override
	protected String buildResponse(HttpServletRequest req) {
		String responseXml;

		log.debug("CAS " + req.getRequestURI() + " query string : " + req.getQueryString());
		
		String targetServiceProviderUrl = req.getParameter(ProtocolConstants.PROXY_TARGET_SERVICE_ARGUMENT);
		String proxyGrantingTicketId = req.getParameter(ProtocolConstants.PROXY_GRANTING_TICKET_ARGUMENT);
		
		if( targetServiceProviderUrl == null 
				|| targetServiceProviderUrl.trim().length() == 0
				|| proxyGrantingTicketId == null
				|| proxyGrantingTicketId.trim().length() == 0
				) {
			responseXml = buildFailureResponse(CASErrorConstants.INVALID_REQUEST_CODE,
					CASResourceReader.getInstance().getLocalizedString(
							CASErrorConstants.INVALID_PROXY_REQUEST_MESSAGE,
							req.getLocale()
							)
						);
			return responseXml;
		}
		
		try {
			if (CASSSOUtil.isValidTicketGrantingTicket(proxyGrantingTicketId)) {
				// "service" URL argument must match a valid service provider
				// URL
		 	 	if (!CASSSOUtil.isValidServiceProvider(targetServiceProviderUrl)) {
					responseXml = buildFailureResponse(CASErrorConstants.INVALID_SERVICE_CODE,
							String.format(
									CASResourceReader.getInstance().getLocalizedString(
											CASErrorConstants.INVALID_SERVICE_MESSAGE,
											req.getLocale()
											), targetServiceProviderUrl));
				} else {
					TicketGrantingTicket proxyGrantingTicket = CASSSOUtil.getTicketGrantingTicket(proxyGrantingTicketId);
					
					ServiceProvider targetServiceProvider = CASSSOUtil.getServiceProviderByUrl(targetServiceProviderUrl, proxyGrantingTicket.getPrincipal());

					ServiceTicket proxyTicket = proxyGrantingTicket.grantServiceTicket(targetServiceProvider, targetServiceProviderUrl, false);
					
					responseXml = buildSuccessResponse(proxyTicket.getId());
				}
			} else {
				responseXml = buildFailureResponse(CASErrorConstants.INVALID_TICKET_CODE, 
						String.format(
								CASResourceReader.getInstance().getLocalizedString(
										CASErrorConstants.INVALID_TICKET_MESSAGE,
										req.getLocale()
										), proxyGrantingTicketId));
			}
		} catch (Exception ex) {
			log.error("CAS proxy login internal error", ex);
			responseXml = buildFailureResponse(CASErrorConstants.INTERNAL_ERROR_CODE,
					CASResourceReader.getInstance().getLocalizedString(
							CASErrorConstants.INTERNAL_ERROR_MESSAGE,
							req.getLocale()
							)
						);
		}

		log.debug("CAS proxy login response XML: " + responseXml);
		
		return responseXml;
	}
	private String buildSuccessResponse(String proxyTicketId) {
		return String.format(validationResponse,
				String.format(success, String.format( proxyTicketTemplate, proxyTicketId )));
	}

	private String buildFailureResponse(String errorCode, String errorMessage) {
		return String.format(validationResponse,
				String.format(failure, errorCode, errorMessage));
	}
}