package org.wso2.carbon.identity.sso.cas.util;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.sso.cas.config.CASConfiguration;
import org.wso2.carbon.registry.core.utils.UUIDGenerator;

public class CASSAMLUtil {
	private static String soapEnvelope;
	private static String successResponse;
	private static String failureResponse;
	private static String samlAttribute;

	private static Log log = LogFactory.getLog(CASSAMLUtil.class);

	
	static {
		try {
			soapEnvelope = CASResourceReader.getInstance()
					.readSecurityResource("cas_soap_envelope.xml");
			successResponse = CASResourceReader.getInstance()
					.readSecurityResource("cas_saml_success.xml");
			failureResponse = CASResourceReader.getInstance()
					.readSecurityResource("cas_saml_failure.xml");
			samlAttribute = CASResourceReader.getInstance()
					.readSecurityResource("cas_saml_attribute.xml");
		} catch (Exception ex) {
			log.error("SAML response templates cannot be loaded", ex);
		}
	}
	
	public static String getSOAPEnvelope() {
		return soapEnvelope;
	}
		
	public static String getSAMLSuccessResponse(String serviceProviderUrl, String username, String requestId, String attributes) {
		String randomId = UUIDGenerator.generateUUID().replaceAll("-", "");
		Date baseDate = new Date();
		Date dateBefore = new Date();
		long samlResponseValidityPeriod = CASConfiguration.getSAMLResponseValidityPeriod();
		dateBefore.setTime(baseDate.getTime() - samlResponseValidityPeriod);
		Date dateAfter = new Date();
		dateAfter.setTime(baseDate.getTime() + samlResponseValidityPeriod);

		String attributeFilteredUrl = serviceProviderUrl.replaceAll("&", "&amp;");
		
		return soapEnvelope
				.replace("$samlResponse", successResponse)
				.replaceAll("\\$issuer", "localhost")
				.replaceAll("\\$recipient", attributeFilteredUrl)
				.replaceAll("\\$audience", attributeFilteredUrl)
				.replaceAll("\\$timestamp", CASSSOUtil.formatSoapDate(baseDate))
				.replaceAll("\\$notBefore",
						CASSSOUtil.formatSoapDate(dateBefore))
				.replaceAll("\\$notAfter", CASSSOUtil.formatSoapDate(dateAfter))
				.replaceAll("\\$assertionId", "_assertion" + randomId)
				.replaceAll("\\$inResponseTo", "_" + requestId)
				.replaceAll("\\$responseId", "_response" + randomId)
				.replace("$attributes", attributes)
				.replaceAll("\\$username", username);
	}
	
	public static String getSAMLFailureResponse(String serviceProviderUrl, String requestId, String errorMessage) {
		Date baseDate = new Date();
		String randomId = "_"
				+ UUIDGenerator.generateUUID().replaceAll("-", "");

		return soapEnvelope
				.replace("$samlResponse", failureResponse)
				.replaceAll("\\$issuer", "localhost")
				.replaceAll("\\$recipient", serviceProviderUrl)
				.replaceAll("\\$errorMessage", errorMessage)
				.replaceAll("\\$responseId", randomId)
				.replaceAll("\\$inResponseTo", "_" + requestId)
				.replaceAll("\\$timestamp", CASSSOUtil.formatSoapDate(baseDate));
	}
	
	public static String getSAMLAttribute(String key, String value) {
		return samlAttribute
		.replace("$attributeName", key)
		.replace("$attributeValue", value);
	}
}