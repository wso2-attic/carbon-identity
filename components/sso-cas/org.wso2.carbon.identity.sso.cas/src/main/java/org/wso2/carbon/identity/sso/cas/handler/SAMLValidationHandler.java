package org.wso2.carbon.identity.sso.cas.handler;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.sso.cas.CASErrorConstants;
import org.wso2.carbon.identity.sso.cas.ticket.ServiceTicket;
import org.wso2.carbon.identity.sso.cas.util.CASResourceReader;
import org.wso2.carbon.identity.sso.cas.util.CASSAMLUtil;
import org.wso2.carbon.identity.sso.cas.util.CASSSOUtil;
import org.wso2.carbon.user.core.util.UserCoreUtil;

public class SAMLValidationHandler extends AbstractValidationHandler {
	private static Log log = LogFactory
			.getLog(SAMLValidationHandler.class);

	private static final String CONST_START_ARTIFACT_XML_TAG_NO_NAMESPACE = "<AssertionArtifact>";
	private static final String CONST_END_ARTIFACT_XML_TAG_NO_NAMESPACE = "</AssertionArtifact>";
	private static final String CONST_START_ARTIFACT_XML_TAG = "<samlp:AssertionArtifact>";
	private static final String CONST_END_ARTIFACT_XML_TAG = "</samlp:AssertionArtifact>";
	
	@Override
	protected String buildResponse(HttpServletRequest req) {
		String responseXml;
		String serviceProviderUrl = "";

		try {
            log.debug("CAS " + req.getRequestURI() + " query string: " + req.getQueryString());

			serviceProviderUrl = req.getParameter(ProtocolConstants.SAML_SERVICE_PROVIDER_ARGUMENT);

			String samlRequest = readSamlRequest(req);

			log.debug("SAML Request: " + samlRequest);
			if (StringUtils.isNotEmpty(samlRequest)) {

				String serviceTicketId = extractServiceTicketId(samlRequest);

				String requestId = extractRequestId(samlRequest);

				if (CASSSOUtil.isValidServiceTicket(serviceTicketId)) {
					if (!CASSSOUtil.isValidServiceProviderForServiceTicket(serviceTicketId, serviceProviderUrl)) {
						responseXml = buildFailureResponse(
								CASResourceReader.getInstance().getLocalizedString(
										CASErrorConstants.INVALID_SERVICE_MESSAGE,
										req.getLocale()
									), requestId,
								serviceProviderUrl);
					} else {
						ServiceTicket serviceTicket = CASSSOUtil
								.consumeServiceTicket(serviceTicketId);
						ServiceProvider serviceProvider = serviceTicket.getService();
						String principal = serviceTicket.getParentTicket().getPrincipal();
						
						ClaimMapping[] claimMapping = serviceProvider.getClaimConfig().getClaimMappings();

						responseXml = buildSuccessResponse(
								principal, requestId,
								serviceProviderUrl, claimMapping);
					}
				} else {
					responseXml = buildFailureResponse(
							CASResourceReader.getInstance().getLocalizedString(
									CASErrorConstants.INVALID_TICKET_MESSAGE,
									req.getLocale()
								),
							requestId, serviceProviderUrl);
				}

			} else {
				responseXml = buildFailureResponse(
						String.format(
								CASResourceReader.getInstance().getLocalizedString(
										CASErrorConstants.INVALID_REQUEST_MESSAGE,
										req.getLocale()
									),
								serviceProviderUrl
								), null,
						serviceProviderUrl);
			}
		} catch (Exception ex) {
			log.error("CAS samlValidate internal error", ex);
			responseXml = buildFailureResponse(
					CASResourceReader.getInstance().getLocalizedString(
							CASErrorConstants.INTERNAL_ERROR_MESSAGE,
							req.getLocale()
						), null, serviceProviderUrl);
		}

		log.debug("CAS " + req.getRequestURI() + " response XML: " + responseXml);
		
		return responseXml;
	}

	private String readSamlRequest(HttpServletRequest req) {
		try {
			// Read from request
			StringBuilder buffer = new StringBuilder();
			BufferedReader reader = req.getReader();
			String line;
			while ((line = reader.readLine()) != null) {
				buffer.append(line);
			}
			return buffer.toString();
		} catch (IOException ioex) {
			return "";
		}
	}
	
	private String buildAttributeXml(ClaimMapping[] claimMapping, String username) throws IdentityException {
		StringBuffer claimsXml = new StringBuffer();
		
		Map<String, String> claims = CASSSOUtil
				.getUserClaimValues(
						username,
						claimMapping,
						null);

		for (Map.Entry<String, String> entry : claims.entrySet()) {
			claimsXml.append(
					CASSAMLUtil.getSAMLAttribute( entry.getKey(), entry.getValue() )
			);
		}
		
		return claimsXml.toString();
	}

	private String extractServiceTicketId(final String samlRequest) {
		final String tagStart;
		final String tagEnd;

		if (samlRequest.indexOf(CONST_START_ARTIFACT_XML_TAG) >= -1) {
			tagStart = CONST_START_ARTIFACT_XML_TAG;
			tagEnd = CONST_END_ARTIFACT_XML_TAG;
		} else {
			tagStart = CONST_START_ARTIFACT_XML_TAG_NO_NAMESPACE;
			tagEnd = CONST_END_ARTIFACT_XML_TAG_NO_NAMESPACE;
		}
		final int startTagLocation = samlRequest.indexOf(tagStart);
		final int artifactStartLocation = startTagLocation + tagStart.length();
		final int endTagLocation = samlRequest.indexOf(tagEnd);

		return samlRequest.substring(artifactStartLocation, endTagLocation)
				.trim();
	}

	private String extractRequestId(final String samlRequest) {
		if (!samlRequest.contains("RequestID")) {
			return null;
		}

		try {
			final int position = samlRequest.indexOf("RequestID=\"") + 11;
			final int nextPosition = samlRequest.indexOf("\"", position);

			return samlRequest.substring(position, nextPosition);
		} catch (final Exception e) {
			log.debug("Exception parsing RequestID from request.", e);
			return null;
		}
	}

	/**
	 * Build success response XML with user ID
	 * 
	 * @param userId
	 *            user id
	 * @return success response XML
	 */
	private String buildSuccessResponse(String userId, String requestId,
			String serviceProviderUrl, ClaimMapping[] claimMapping) throws IdentityException {

		String attributes = buildAttributeXml(claimMapping, userId);
		
		// Strip the domain prefix from the username for applications
		// that rely on the raw uid
		String rawUserId = UserCoreUtil.removeDomainFromName(userId);
		
		return CASSAMLUtil.getSAMLSuccessResponse(serviceProviderUrl, rawUserId, requestId, attributes);
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
	private String buildFailureResponse(String errorMessage,
			String requestId, String serviceProviderUrl) {
		return CASSAMLUtil.getSAMLFailureResponse(serviceProviderUrl, requestId, errorMessage);
	}
}
