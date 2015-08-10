package org.wso2.carbon.identity.sso.cas.handler;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.sso.cas.CASErrorConstants;
import org.wso2.carbon.identity.sso.cas.config.CASConfiguration;
import org.wso2.carbon.identity.sso.cas.ticket.ServiceTicket;
import org.wso2.carbon.identity.sso.cas.ticket.TicketConstants;
import org.wso2.carbon.identity.sso.cas.ticket.TicketGrantingTicket;
import org.wso2.carbon.identity.sso.cas.ticket.TicketIdGenerator;
import org.wso2.carbon.identity.sso.cas.util.CASResourceReader;
import org.wso2.carbon.identity.sso.cas.util.CASSSOUtil;
import org.wso2.carbon.user.core.util.UserCoreUtil;

public class ServiceValidationHandler extends AbstractValidationHandler {
	private static Log log = LogFactory
			.getLog(ServiceValidationHandler.class);
	
	protected static final String validationResponse = "<cas:serviceResponse xmlns:cas=\"http://www.yale.edu/tp/cas\">%s</cas:serviceResponse>";
	private static final String success = "<cas:authenticationSuccess>%s</cas:authenticationSuccess>";
	private static final String userTemplate = "<cas:user>%s</cas:user>";
	private static final String attributesWrapper = "<cas:attributes>%s</cas:attributes>";
	private static final String attributeTemplate = "<cas:%s>%s</cas:%s>";
	private static final String proxyGrantingTicketTemplate = "<cas:proxyGrantingTicket>%s</cas:proxyGrantingTicket>";
	private static final String proxiesWrapper = "<cas:proxies>%s</cas:proxies>";
	private static final String proxyTemplate = "<cas:proxy>%s</cas:proxy>";
	private static String failure = "<cas:authenticationFailure code=\"%s\">%s</cas:authenticationFailure>";
	@SuppressWarnings("unused")
	private boolean proxyRequest;
	
	public ServiceValidationHandler() {
		proxyRequest = false;
	}
	
	public ServiceValidationHandler(boolean proxyRequest) {
		this.proxyRequest = proxyRequest;
	}
	
	@Override
	protected String buildResponse(HttpServletRequest req) {
		String responseXml;

		try {
			log.debug("CAS " + req.getRequestURI() + " query string: " + req.getQueryString());

	        String serviceProviderUrl = req.getParameter(ProtocolConstants.SERVICE_PROVIDER_ARGUMENT);
	        String serviceTicketId = req.getParameter(ProtocolConstants.SERVICE_TICKET_ARGUMENT);
	        String proxyGrantingUrl = req.getParameter(ProtocolConstants.PROXY_GRANTING_URL_ARGUMENT);
	        String proxyGrantingIou = null;
	        List<String> proxies = new ArrayList<String>();
	        
			if( serviceProviderUrl == null 
					|| serviceProviderUrl.trim().length() == 0
					|| serviceTicketId == null
					|| serviceTicketId.trim().length() == 0
					) {
				responseXml = buildFailureResponse(CASErrorConstants.INVALID_REQUEST_CODE,
						CASResourceReader.getInstance().getLocalizedString(
								CASErrorConstants.INVALID_REQUEST_MESSAGE, req.getLocale()
								)
							);
				return responseXml;
			}
	                
			if (CASSSOUtil.isValidServiceTicket(serviceTicketId)) {
				// "service" URL argument must match a valid service provider URL
		 	 	if (!CASSSOUtil.isValidServiceProviderForServiceTicket(serviceTicketId, serviceProviderUrl)) {
					responseXml = buildFailureResponse(CASErrorConstants.INVALID_SERVICE_CODE,
							String.format(
									CASResourceReader.getInstance().getLocalizedString(
											CASErrorConstants.INVALID_SERVICE_MESSAGE,
											req.getLocale()
										), serviceProviderUrl));
// Uncomment to enforce proxy tickets are only validated against the /proxyValidate endpoint
//				} if(CASSSOUtil.isValidProxyTicket(serviceTicketId) && !proxyRequest) {
//					responseXml = buildFailureResponse(CASErrorConstants.INVALID_TICKET_CODE, 
//							String.format(
//					CASResourceReader.getInstance().getLocalizedString(
//							CASErrorConstants.INVALID_PROXY_TICKET_MESSAGE,
//							req.getLocale()
//						) + CASEndpointConstants.PROXY_VALIDATE_PATH, serviceTicketId));
				} else {

					ServiceTicket serviceTicket = CASSSOUtil
							.consumeServiceTicket(serviceTicketId);
					
					ServiceProvider serviceProvider = serviceTicket
							.getService();
					
			        if( proxyGrantingUrl != null ) {
			        	proxyGrantingIou = verifyProxyGrantingUrl(serviceTicket, proxyGrantingUrl, proxies);
			        }

					ClaimMapping[] claimMapping = serviceProvider
							.getClaimConfig().getClaimMappings();

					String principal = serviceTicket.getParentTicket().getPrincipal();
										
					String attributesXml = buildAttributesXml(principal, claimMapping);
					
					responseXml = buildSuccessResponse(principal, attributesXml, proxyGrantingIou, proxies);
				}
			} else {
				responseXml = buildFailureResponse(CASErrorConstants.INVALID_TICKET_CODE, 
						String.format(
								CASResourceReader.getInstance().getLocalizedString(
										CASErrorConstants.INVALID_TICKET_MESSAGE,
										req.getLocale()
									), serviceTicketId));
			}
		} catch (Exception ex) {
			log.error("CAS serviceValidate internal error", ex);
			responseXml = buildFailureResponse(CASErrorConstants.INTERNAL_ERROR_CODE,
					CASResourceReader.getInstance().getLocalizedString(
							CASErrorConstants.INTERNAL_ERROR_MESSAGE,
							req.getLocale()
							));
		}

		log.debug("CAS " + req.getRequestURI() + " response XML: " + responseXml);
		
		return responseXml;
	}
	
	protected String verifyProxyGrantingUrl(ServiceTicket serviceTicket, String proxyGrantingUrl, List<String> proxies) throws URISyntaxException {
		String proxyIou = TicketIdGenerator.generate(TicketConstants.PROXY_GRANTING_TICKET_IOU_PREFIX);

		TicketGrantingTicket ticketGrantingTicket = CASSSOUtil.createTicketGrantingTicket(
			serviceTicket.getParentTicket().getSessionDataKey(), 
			serviceTicket.getParentTicket().getPrincipal(), 
			true
		);
			
		// URIBuilder was hanging, so traditional string concatenation is sufficient
		String remoteProxyUrl = proxyGrantingUrl;
		
		if( remoteProxyUrl.indexOf("?" ) != -1 ) {
			remoteProxyUrl += "&";
		} else {
			remoteProxyUrl += "?";
		}
		
		remoteProxyUrl += ProtocolConstants.PROXY_GRANTING_IOU_ARGUMENT + "=" + proxyIou + "&" + ProtocolConstants.PROXY_GRANTING_TICKET_ID_ARGUMENT + "=" + ticketGrantingTicket.getId();
		
        HttpClient client = new HttpClient();
        HttpMethod method = new GetMethod(remoteProxyUrl);

        // Set retries to configuration value
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, 
        		new DefaultHttpMethodRetryHandler( CASConfiguration.getProxyRetryLimit(), false));
        try {

            int statusCode = client.executeMethod(method);
            
            if (statusCode != HttpStatus.SC_OK) {
              log.error("Method failed: " + method.getStatusLine());
            } else {
            	proxies.add(proxyGrantingUrl);
            }

            // Dump the response in debug
            if( log.isDebugEnabled() ) {
                byte[] responseBody = method.getResponseBody();
            	log.debug(new String(responseBody));            
            }
            
          } catch (Exception ex) {
        	  log.error("Fatal proxy error: ",ex);
            proxyIou = null;
          } finally {
            // Release the connection.
            method.releaseConnection();
          }
        
        return proxyIou;
	}
	
	private String buildAttributesXml(String username, ClaimMapping[] claimMapping) throws IdentityException {
		StringBuilder attributesXml = new StringBuilder();
		
		Map<String, String> claims = CASSSOUtil
				.getUserClaimValues(
						username,
						claimMapping,
						null);

		
		for (Map.Entry<String, String> entry : claims.entrySet()) {
			String scrubbedKey = entry.getKey().replaceAll(" ", "_");
			attributesXml.append(
					String.format( 
							attributeTemplate,
							scrubbedKey,
							entry.getValue(),
							scrubbedKey)
			);
		}
		log.debug("attributesXml:\n" + attributesXml);
		return attributesXml.toString();
	}
	
	private String buildProxyXml(List<String> proxies) {
		StringBuilder proxyXml = new StringBuilder();
		
		for( String proxy : proxies ) {
			proxyXml.append(String.format(proxyTemplate, proxy));
		}
		
		return proxyXml.toString();
	}

	/**
	 * Build success response XML with user ID
	 * 
	 * @param userId
	 *            user id
	 * @return success response XML
	 */
	private String buildSuccessResponse(String userId,
			String userAttributesXml, String proxyGrantingTicketId, List<String> proxies) {
		StringBuilder responseAttributes = new StringBuilder();
		
		// Strip the domain prefix from the username for applications
		// that rely on the raw uid
		String rawUserId = UserCoreUtil.removeDomainFromName(userId);
		
		// user ID is always included
		responseAttributes.append(String.format(userTemplate, rawUserId));
		
		if( proxyGrantingTicketId != null ) {
			responseAttributes.append( String.format( proxyGrantingTicketTemplate, proxyGrantingTicketId ));
			responseAttributes.append( String.format( proxiesWrapper, buildProxyXml(proxies) ) );
		}

		if (userAttributesXml != null) {
			responseAttributes.append( String.format( attributesWrapper, userAttributesXml ) );
		}

		return String.format(validationResponse,
				String.format(success, responseAttributes.toString()));
	}

	/**
	 * Build error response XML with specific code and message
	 * 
	 * @param errorCode
	 *            error code
	 * @param errorMessage
	 *            error message
	 * @return error response XML
	 */
	private String buildFailureResponse(String errorCode, String errorMessage) {
		return String.format(validationResponse,
				String.format(failure, errorCode, errorMessage));
	}
}
