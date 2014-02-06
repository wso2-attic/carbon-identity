/*
*  Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.carbon.identity.sso.saml.tomcat.agent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.opensaml.Configuration;
import org.opensaml.DefaultBootstrap;
import org.opensaml.common.SAMLVersion;
import org.opensaml.saml2.core.*;
import org.opensaml.saml2.core.impl.AuthnContextClassRefBuilder;
import org.opensaml.saml2.core.impl.AuthnRequestBuilder;
import org.opensaml.saml2.core.impl.IssuerBuilder;
import org.opensaml.saml2.core.impl.LogoutRequestBuilder;
import org.opensaml.saml2.core.impl.NameIDBuilder;
import org.opensaml.saml2.core.impl.NameIDPolicyBuilder;
import org.opensaml.saml2.core.impl.RequestedAuthnContextBuilder;
import org.opensaml.saml2.core.impl.SessionIndexBuilder;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.Marshaller;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallerFactory;
import org.opensaml.xml.io.UnmarshallingException;
import org.opensaml.xml.signature.SignatureValidator;
import org.opensaml.xml.util.Base64;
import org.opensaml.xml.util.XMLHelper;
import org.opensaml.xml.validation.ValidationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class SSOManager {

	private String authReqRandomId = Integer.toHexString(new Double(Math.random()).intValue());
	private String relayState = null;

	public static final Log log = LogFactory.getLog(SSOManager.class);
	
	public SSOManager() throws ConfigurationException {
		/* Initializing the OpenSAML library, loading default configurations */
		DefaultBootstrap.bootstrap();
	}

	/**
	 * Returns the redirection URL with the appended SAML2
	 * Request message
	 * 
	 * @param request SAML 2 request
	 * 
	 * @return redirectionUrl<dependency>
	 *         <groupId>org.opensaml</groupId>
	 *         <artifactId>opensaml</artifactId>
	 *         <version>2.2.3</version>
	 *         </dependency>
	 */
	public String buildRequestMessage(HttpServletRequest request) {

		RequestAbstractType requestMessage;

		
		if (request.getParameter("logout") == null) {
			requestMessage = buildAuthnRequestObject();

		} else {
            String IdPSession = (String) request.getSession().getAttribute(SSOConstants.IDP_SESSION);
			requestMessage = buildLogoutRequest((String) request.getSession().getAttribute("Subject"), IdPSession);
		}
        if(log.isDebugEnabled()){
            log.debug("SAML REQUEST IS BUILT. REQUEST : " +  requestMessage);
        }
		String encodedRequestMessage = null;
		try {
			encodedRequestMessage = encodeRequestMessage(requestMessage);
		} catch (MarshallingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return SSOConfigs.getIdpUrl() + "?SAMLRequest=" + encodedRequestMessage +"&RelayState=" + relayState;
	}

	private LogoutRequest buildLogoutRequest(String user, String IdPSession) {

		LogoutRequest logoutReq = new LogoutRequestBuilder().buildObject();

		logoutReq.setID(Util.createID());

		DateTime issueInstant = new DateTime();
		logoutReq.setIssueInstant(issueInstant);
		logoutReq.setNotOnOrAfter(new DateTime(issueInstant.getMillis() + 5 * 60 * 1000));

		IssuerBuilder issuerBuilder = new IssuerBuilder();
		Issuer issuer = issuerBuilder.buildObject();
		issuer.setValue(SSOConfigs.getIssuerId());
		logoutReq.setIssuer(issuer);

		NameID nameId = new NameIDBuilder().buildObject();
		nameId.setFormat("urn:oasis:names:tc:SAML:2.0:nameid-format:entity");
		nameId.setValue(user);
		logoutReq.setNameID(nameId);

		SessionIndex sessionIndex = new SessionIndexBuilder().buildObject();
		sessionIndex.setSessionIndex(IdPSession);
		logoutReq.getSessionIndexes().add(sessionIndex);

		logoutReq.setReason("Single Logout");

		return logoutReq;
	}

	private AuthnRequest buildAuthnRequestObject() {

		
		IssuerBuilder issuerBuilder = new IssuerBuilder();
		Issuer issuer =
		                issuerBuilder.buildObject("urn:oasis:names:tc:SAML:2.0:assertion",
		                                          "Issuer", "samlp");
		issuer.setValue(SSOConfigs.getIssuerId());

		/* NameIDPolicy */
		NameIDPolicyBuilder nameIdPolicyBuilder = new NameIDPolicyBuilder();
		NameIDPolicy nameIdPolicy = nameIdPolicyBuilder.buildObject();
		nameIdPolicy.setFormat("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent");
		nameIdPolicy.setSPNameQualifier("Isser");
		nameIdPolicy.setAllowCreate(true);

		/* AuthnContextClass */
		AuthnContextClassRefBuilder authnContextClassRefBuilder = new AuthnContextClassRefBuilder();
		AuthnContextClassRef authnContextClassRef =
		                                            authnContextClassRefBuilder.buildObject("urn:oasis:names:tc:SAML:2.0:assertion",
		                                                                                    "AuthnContextClassRef",
		                                                                                    "saml");
		authnContextClassRef.setAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport");

		/* AuthnContex */
		RequestedAuthnContextBuilder requestedAuthnContextBuilder =
		                                                            new RequestedAuthnContextBuilder();
		RequestedAuthnContext requestedAuthnContext = requestedAuthnContextBuilder.buildObject();
		requestedAuthnContext.setComparison(AuthnContextComparisonTypeEnumeration.EXACT);
		requestedAuthnContext.getAuthnContextClassRefs().add(authnContextClassRef);

		DateTime issueInstant = new DateTime();

		/* Creation of AuthRequestObject */
		AuthnRequestBuilder authRequestBuilder = new AuthnRequestBuilder();
		AuthnRequest authRequest =
		                           authRequestBuilder.buildObject("urn:oasis:names:tc:SAML:2.0:protocol",
		                                                          "AuthnRequest", "samlp");
		authRequest.setForceAuthn(false);
		authRequest.setIsPassive(false);
		authRequest.setIssueInstant(issueInstant);
		authRequest.setProtocolBinding("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST");
		authRequest.setAssertionConsumerServiceURL(SSOConfigs.getConsumerUrl());
		authRequest.setIssuer(issuer);
		authRequest.setNameIDPolicy(nameIdPolicy);
		authRequest.setRequestedAuthnContext(requestedAuthnContext);
		authRequest.setID(authReqRandomId);
		authRequest.setVersion(SAMLVersion.VERSION_20);

		/* Requesting Attributes. This Index value is registered in the IDP */
		if (SSOConfigs.getAttributeIndex() != null && SSOConfigs.getAttributeIndex().trim().length() > 0) {
			authRequest.setAttributeConsumingServiceIndex(Integer.parseInt(SSOConfigs.getAttributeIndex()));
		}

		return authRequest;
	}

	private String encodeRequestMessage(RequestAbstractType requestMessage)
	                                                                       throws MarshallingException,
	                                                                       IOException {

		Marshaller marshaller = Configuration.getMarshallerFactory().getMarshaller(requestMessage);
		Element authDOM = marshaller.marshall(requestMessage);

		Deflater deflater = new Deflater(Deflater.DEFLATED, true);
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		DeflaterOutputStream deflaterOutputStream =
		                                            new DeflaterOutputStream(byteArrayOutputStream,
		                                                                     deflater);

		StringWriter rspWrt = new StringWriter();
		XMLHelper.writeNode(authDOM, rspWrt);
		deflaterOutputStream.write(rspWrt.toString().getBytes());
		deflaterOutputStream.close();

		/* Encoding the compressed message */
		String encodedRequestMessage =
		                               Base64.encodeBytes(byteArrayOutputStream.toByteArray(),
		                                                  Base64.DONT_BREAK_LINES);
		return URLEncoder.encode(encodedRequestMessage, "UTF-8").trim();
	}

	public XMLObject unmarshall(String responseMessage) throws SSOAgentException {

        String decodedString = decodeHTMLCharacters(responseMessage);
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
        try {
            DocumentBuilder docBuilder = documentBuilderFactory.newDocumentBuilder();

            ByteArrayInputStream is = new ByteArrayInputStream(decodedString.getBytes());

            Document document = docBuilder.parse(is);
            Element element = document.getDocumentElement();
            UnmarshallerFactory unmarshallerFactory = Configuration.getUnmarshallerFactory();
            Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(element);
            return unmarshaller.unmarshall(element);
        } catch (Exception e){
			log.error("Error in constructing AuthRequest from the encoded String", e);
			throw new SSOAgentException("Error in constructing AuthRequest from "
					+ "the encoded String ", e);
        }

	}
	
	private String decodeHTMLCharacters(String encodedStr) {                                           
	    return encodedStr.replaceAll("&amp;", "&").replaceAll("&lt;", "<").replaceAll("&gt;", ">")     
	            .replaceAll("&quot;", "\"").replaceAll("&apos;", "'");                                 
	                                                                                                   
	}                                                                                                  
	
	/*
	 * Process the response and returns the results
	 */
	public Map<String, String> getAssertionStatements(Assertion assertion) {

		Map<String, String> results = new HashMap<String, String>();

		if (assertion != null) {

			String subject = assertion.getSubject().getNameID().getValue();
			results.put(SSOConfigs.getSubjectNameAttributeId(), subject); // get the subject

			List<AttributeStatement> attributeStatementList = assertion.getAttributeStatements();

			if (attributeStatementList != null) {
                for (AttributeStatement statement : attributeStatementList) {
                    List<Attribute> attributesList = statement.getAttributes();
                    for (Attribute attribute : attributesList) {
                        Element value = attribute.getAttributeValues().get(0).getDOM();
                        String attributeValue = value.getTextContent();
                        results.put(attribute.getName(), attributeValue);
                    }
                }
			}
		}
		return results;
	}

	/**
	 * Validate the AudienceRestriction of SAML2 Response
	 *
	 * @param assertion SAML2 Assertion
	 * @return validity
	 */
	public boolean validateAudienceRestriction(Assertion assertion) {
        
		if (assertion != null) {
			Conditions conditions = assertion.getConditions();
    		if (conditions != null) {
    			List<AudienceRestriction> audienceRestrictions = conditions.getAudienceRestrictions();
    			if (audienceRestrictions != null && !audienceRestrictions.isEmpty()) {
    				for (AudienceRestriction audienceRestriction : audienceRestrictions) {
    					if (audienceRestriction.getAudiences() != null && audienceRestriction.getAudiences().size() > 0) {
    						for (Audience audience : audienceRestriction.getAudiences()) {
								if (SSOConfigs.getIssuerId().equals(audience.getAudienceURI())) {
									return true;
								}
							}
    					} else {
    						log.warn("SAML Response's AudienceRestriction doesn't contain Audiences");
    					}
    				}
    			} else {
    				log.error("SAML Response doesn't contain AudienceRestrictions");
    			}
        	} else {
        		log.error("SAML Response doesn't contain Conditions");
        	}
		}
		return false;
	}


    /**
     * Validate the signature of a SAML2 Response
     *
     * @param response   SAML2 Response
     * @return true, if signature is valid.
     */
    public boolean validateSignature(Response response) {
        boolean isSignatureValid = false;
        if(response.getSignature() == null){
            log.error("SAML Response is not signed. So authentication process will be terminated.");
        }
        else {
            try {
                SignatureValidator validator = new SignatureValidator(Util.getX509CredentialImpl());
                validator.validate(response.getSignature());
                isSignatureValid = true;
            } catch (SSOAgentException e) {
                log.error("Error while creating an X509CredentialImpl instance", e);
            } catch (ValidationException e) {
                log.warn("Signature validation failed for a SAML Response");
            }
        }
        return isSignatureValid;
    }
}
