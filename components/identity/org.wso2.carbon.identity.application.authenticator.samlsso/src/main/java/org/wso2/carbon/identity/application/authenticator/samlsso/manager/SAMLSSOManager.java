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

package org.wso2.carbon.identity.application.authenticator.samlsso.manager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
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
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.opensaml.Configuration;
import org.opensaml.DefaultBootstrap;
import org.opensaml.common.SAMLVersion;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeStatement;
import org.opensaml.saml2.core.Audience;
import org.opensaml.saml2.core.AudienceRestriction;
import org.opensaml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.Conditions;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.LogoutRequest;
import org.opensaml.saml2.core.LogoutResponse;
import org.opensaml.saml2.core.NameID;
import org.opensaml.saml2.core.NameIDPolicy;
import org.opensaml.saml2.core.RequestAbstractType;
import org.opensaml.saml2.core.RequestedAuthnContext;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.SessionIndex;
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
import org.opensaml.xml.security.x509.X509Credential;
import org.opensaml.xml.signature.SignatureValidator;
import org.opensaml.xml.util.Base64;
import org.opensaml.xml.util.XMLHelper;
import org.opensaml.xml.validation.ValidationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.wso2.carbon.identity.application.authentication.framework.config.ExternalIdPConfig;
import org.wso2.carbon.identity.application.authenticator.samlsso.exception.SAMLSSOException;
import org.wso2.carbon.identity.application.authenticator.samlsso.util.SSOConstants;
import org.wso2.carbon.identity.application.authenticator.samlsso.util.SSOUtils;
import org.wso2.carbon.registry.core.utils.UUIDGenerator;
import org.wso2.carbon.ui.CarbonUIUtil;
import org.xml.sax.SAXException;

public class SAMLSSOManager {

	private static Log log = LogFactory.getLog(SAMLSSOManager.class);
	
	//private String authReqRandomId = Integer.toHexString(new Double(Math.random()).intValue());
	private String relayState = null;
    private X509Credential credential = null;
    private static boolean bootStrapped = false;

	public SAMLSSOManager(ExternalIdPConfig externalIdPConfig) throws SAMLSSOException {
		String credentialImplClass = "org.wso2.carbon.identity.application.authenticator.samlsso.manager.SSOAgentKeyStoreCredential";
		try {
            synchronized (this){
                if(credential == null){
                    synchronized (this){
                        SSOAgentCredential credential = (SSOAgentCredential) Class.forName(credentialImplClass).newInstance();
                        credential.init(externalIdPConfig);
                        this.credential = new X509CredentialImpl(credential);
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            throw new SAMLSSOException("Error while instantiating SSOAgentCredentialImplClass: " + credentialImplClass, e);
        } catch (InstantiationException e) {
            throw new SAMLSSOException("Error while instantiating SSOAgentCredentialImplClass: " + credentialImplClass, e);
        } catch (IllegalAccessException e) {
            throw new SAMLSSOException("Error while instantiating SSOAgentCredentialImplClass: " + credentialImplClass, e);
        }
    }
	
	public static void doBootstrap() {
		/* Initializing the OpenSAML library */
        if (!bootStrapped) {
            try {
                DefaultBootstrap.bootstrap();
                bootStrapped = true;
            } catch (ConfigurationException e) {
                log.error("Error in bootstrapping the OpenSAML2 library", e);
            }
        }
    }

	/**
	 * Returns the redirection URL with the appended SAML2
	 * Request message
	 * 
	 * @param request SAML 2 request
	 * 
	 * @return redirectionUrl
	 */
	public String buildRequest(HttpServletRequest request, boolean isLogout, boolean isPassive,
	                           String loginPage, ExternalIdPConfig externalIdPConfig, String contextIdentifier)
	                                                                              throws SAMLSSOException {
		doBootstrap();
		RequestAbstractType requestMessage;
		if (!isLogout) {
			requestMessage = buildAuthnRequest(request, isPassive, loginPage, externalIdPConfig);
		} else { 
			requestMessage = buildLogoutRequest((String) request.getSession().getAttribute("username"), loginPage, externalIdPConfig);
		}
        String idpUrl = null;
        
        String encodedRequestMessage = encodeRequestMessage(requestMessage);
        StringBuilder httpQueryString = new StringBuilder("SAMLRequest=" + encodedRequestMessage);
                
        try {
            httpQueryString.append("&RelayState=" + URLEncoder.encode(contextIdentifier, "UTF-8").trim());
        } catch (UnsupportedEncodingException e) {
            throw new SAMLSSOException("Error occurred while url encoding RelayState", e);
        }
        
        if(Boolean.valueOf(externalIdPConfig.isAuthnRequestSigned())){
            SSOUtils.addDeflateSignatureToHTTPQueryString(httpQueryString);
        }
        
        if(loginPage.indexOf("?") > -1){
            idpUrl = loginPage.concat("&").concat(httpQueryString.toString());
        } else {
            idpUrl = loginPage.concat("?").concat(httpQueryString.toString());
        }
        return idpUrl;
	}
	
    public void processResponse(HttpServletRequest request, ExternalIdPConfig externalIdPConfig) throws SAMLSSOException {
    	doBootstrap();
        String decodedResponse = new String(Base64.decode(request.getParameter(SSOConstants.HTTP_POST_PARAM_SAML2_RESP)));
        XMLObject samlObject = unmarshall(decodedResponse);
        if (samlObject instanceof LogoutResponse) {
            //This is a SAML response for a single logout request from the SP
            doSLO(request);
        } else {
            processSSOResponse(request, externalIdPConfig);
        }
    }

    /**
     * This method handles the logout requests from the IdP
     * Any request for the defined logout URL is handled here
     * @param request
     * @throws javax.servlet.ServletException
     * @throws IOException
     */
    public void doSLO (HttpServletRequest request) throws SAMLSSOException {
    	doBootstrap();
        XMLObject samlObject = null;
        if(request.getParameter(SSOConstants.HTTP_POST_PARAM_SAML2_AUTH_REQ) != null){
            samlObject = unmarshall(new String(Base64.decode(request.getParameter(SSOConstants.HTTP_POST_PARAM_SAML2_AUTH_REQ))));
        }
        if(samlObject == null){
            samlObject = unmarshall(new String(Base64.decode(request.getParameter(SSOConstants.HTTP_POST_PARAM_SAML2_RESP))));
        }
        if (samlObject instanceof LogoutRequest) {
            LogoutRequest logoutRequest = (LogoutRequest) samlObject;
            String sessionIndex = logoutRequest.getSessionIndexes().get(0).getSessionIndex();
            SSOAgentSessionManager.invalidateSessionByIdPSId(sessionIndex);
        } else if (samlObject instanceof LogoutResponse){
            request.getSession().invalidate();
        } else {
            throw new SAMLSSOException("Invalid Single Logout SAML Request");
        }
    }

    private void processSSOResponse(HttpServletRequest request, ExternalIdPConfig externalIdPConfig) throws SAMLSSOException {

        Response samlResponse = (Response) unmarshall(new String(Base64.decode(request.getParameter(SSOConstants.HTTP_POST_PARAM_SAML2_RESP))));
        List<Assertion> assertions = samlResponse.getAssertions();
        Assertion assertion = null;
        if (assertions != null && assertions.size() > 0) {
            assertion = assertions.get(0);
        }
        if (assertion == null) {
            if (samlResponse.getStatus() != null &&
                    samlResponse.getStatus().getStatusCode() != null &&
                    samlResponse.getStatus().getStatusCode().getValue().equals(SSOConstants.StatusCodes.IDENTITY_PROVIDER_ERROR) &&
                    samlResponse.getStatus().getStatusCode().getStatusCode() != null &&
                    samlResponse.getStatus().getStatusCode().getStatusCode().getValue().equals(SSOConstants.StatusCodes.NO_PASSIVE)) {
                return;
            }
            throw new SAMLSSOException("SAML Assertion not found in the Response");
        }

        // Get the subject name from the Response Object and forward it to login_action.jsp
        String subject = null;
        if(assertion.getSubject() != null && assertion.getSubject().getNameID() != null){
            subject = assertion.getSubject().getNameID().getValue();
        }

        if(subject == null){
            throw new SAMLSSOException("SAML Response does not contain the name of the subject");
        }

        request.getSession().setAttribute("username", subject); // get the subject

        // validate audience restriction
        validateAudienceRestriction(assertion, externalIdPConfig);

        // validate signature this SP only looking for assertion signature
        validateSignature(samlResponse, externalIdPConfig);

//        request.getSession(false).setAttribute(SSOConfigs.getSAMLSSOAttributesMapName(), getAssertionStatements(assertion));

        //For removing the session when the single sign out request made by the SP itself
        if(externalIdPConfig.isLogoutEnabled()){
            String sessionId = assertion.getAuthnStatements().get(0).getSessionIndex();
            if(sessionId == null){
                throw new SAMLSSOException("Single Logout is enabled but IdP Session ID not found in SAML Assertion");
            }
            request.getSession().setAttribute(SSOConstants.IDP_SESSION, sessionId);
            SSOAgentSessionManager.addAuthenticatedSession(sessionId, request.getSession());
        }

    }

	private LogoutRequest buildLogoutRequest(String user, String idpUrl, ExternalIdPConfig externalIdPConfig) throws SAMLSSOException{

		LogoutRequest logoutReq = new LogoutRequestBuilder().buildObject();

		logoutReq.setID(SSOUtils.createID());
		logoutReq.setDestination(idpUrl);

		DateTime issueInstant = new DateTime();
		logoutReq.setIssueInstant(issueInstant);
		logoutReq.setNotOnOrAfter(new DateTime(issueInstant.getMillis() + 5 * 60 * 1000));

		IssuerBuilder issuerBuilder = new IssuerBuilder();
		Issuer issuer = issuerBuilder.buildObject();
		issuer.setValue("carbonServer");
		logoutReq.setIssuer(issuer);

		NameID nameId = new NameIDBuilder().buildObject();
		nameId.setFormat("urn:oasis:names:tc:SAML:2.0:nameid-format:entity");
		nameId.setValue(user);
		logoutReq.setNameID(nameId);

		SessionIndex sessionIndex = new SessionIndexBuilder().buildObject();
		sessionIndex.setSessionIndex(UUID.randomUUID().toString());
		logoutReq.getSessionIndexes().add(sessionIndex);

		logoutReq.setReason("Single Logout");

		return logoutReq;
	}

	private AuthnRequest buildAuthnRequest(HttpServletRequest request, boolean isPassive,
	                                       String idpUrl, ExternalIdPConfig externalIdPConfig)
	                                                                                       throws SAMLSSOException {
		
		IssuerBuilder issuerBuilder = new IssuerBuilder();
		Issuer issuer = issuerBuilder.buildObject("urn:oasis:names:tc:SAML:2.0:assertion", "Issuer", "samlp");
		issuer.setValue("carbonServer");

		/* NameIDPolicy */
		NameIDPolicyBuilder nameIdPolicyBuilder = new NameIDPolicyBuilder();
		NameIDPolicy nameIdPolicy = nameIdPolicyBuilder.buildObject();
		nameIdPolicy.setFormat("urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified");
		//nameIdPolicy.setSPNameQualifier("Issuer");
		nameIdPolicy.setAllowCreate(true);

		/* AuthnContextClass */
		AuthnContextClassRefBuilder authnContextClassRefBuilder = new AuthnContextClassRefBuilder();
		AuthnContextClassRef authnContextClassRef = authnContextClassRefBuilder.buildObject("urn:oasis:names:tc:SAML:2.0:assertion",
		                                                                                    "AuthnContextClassRef",
		                                                                                    "saml");
		authnContextClassRef.setAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport");

		/* AuthnContex */
		RequestedAuthnContextBuilder requestedAuthnContextBuilder = new RequestedAuthnContextBuilder();
		RequestedAuthnContext requestedAuthnContext = requestedAuthnContextBuilder.buildObject();
		requestedAuthnContext.setComparison(AuthnContextComparisonTypeEnumeration.EXACT);
		requestedAuthnContext.getAuthnContextClassRefs().add(authnContextClassRef);

		DateTime issueInstant = new DateTime();

		/* Creation of AuthRequestObject */
		AuthnRequestBuilder authRequestBuilder = new AuthnRequestBuilder();
		AuthnRequest authRequest = authRequestBuilder.buildObject("urn:oasis:names:tc:SAML:2.0:protocol",
		                                                          "AuthnRequest", "samlp");
		authRequest.setForceAuthn(false);
		authRequest.setIsPassive(isPassive);
		authRequest.setIssueInstant(issueInstant);
		authRequest.setProtocolBinding("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST");
		
		String acsUrl = CarbonUIUtil.getAdminConsoleURL(request);
		acsUrl = acsUrl.replace("commonauth/carbon/", "commonauth");
		
		authRequest.setAssertionConsumerServiceURL(acsUrl);
		authRequest.setIssuer(issuer);
		authRequest.setNameIDPolicy(nameIdPolicy);
		authRequest.setRequestedAuthnContext(requestedAuthnContext);
		authRequest.setID(UUIDGenerator.generateUUID());
		authRequest.setVersion(SAMLVersion.VERSION_20);
		authRequest.setDestination(idpUrl);

		/* Requesting Attributes. This Index value is registered in the IDP */
//		if (SSOConfigs.getAttributeConsumingServiceIndex() != null && SSOConfigs.getAttributeConsumingServiceIndex().trim().length() > 0) {
//			authRequest.setAttributeConsumingServiceIndex(Integer.parseInt(SSOConfigs.getAttributeConsumingServiceIndex()));
//		}

		return authRequest;
	}

	private String encodeRequestMessage(RequestAbstractType requestMessage) throws SAMLSSOException{

		Marshaller marshaller = Configuration.getMarshallerFactory().getMarshaller(requestMessage);
        Element authDOM = null;
        try {
            authDOM = marshaller.marshall(requestMessage);

            /* Compress the message */
            Deflater deflater = new Deflater(Deflater.DEFLATED, true);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(byteArrayOutputStream, deflater);
            StringWriter rspWrt = new StringWriter();
            XMLHelper.writeNode(authDOM, rspWrt);
            deflaterOutputStream.write(rspWrt.toString().getBytes());
            deflaterOutputStream.close();

            /* Encoding the compressed message */
            String encodedRequestMessage = Base64.encodeBytes(byteArrayOutputStream.toByteArray(), Base64.DONT_BREAK_LINES);
            return URLEncoder.encode(encodedRequestMessage, "UTF-8").trim();

        } catch (MarshallingException e) {
            throw new SAMLSSOException("Error occurred while encoding SAML request",e);
        } catch (UnsupportedEncodingException e) {
            throw new SAMLSSOException("Error occurred while encoding SAML request",e);
        } catch (IOException e) {
            throw new SAMLSSOException("Error occurred while encoding SAML request",e);
        }
    }

	private XMLObject unmarshall(String samlString) throws SAMLSSOException {

        String decodedString = decodeHTMLCharacters(samlString);
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
        } catch (ParserConfigurationException e) {
            throw new SAMLSSOException("Error in unmarshalling SAML Request from the encoded String", e);
        } catch (UnmarshallingException e) {
            throw new SAMLSSOException("Error in unmarshalling SAML Request from the encoded String", e);
        } catch (SAXException e) {
            throw new SAMLSSOException("Error in unmarshalling SAML Request from the encoded String", e);
        } catch (IOException e) {
            throw new SAMLSSOException("Error in unmarshalling SAML Request from the encoded String", e);
        }

    }
	
	private String decodeHTMLCharacters(String encodedStr) {                                           
	    return encodedStr.replaceAll("&amp;", "&").replaceAll("&lt;", "<").replaceAll("&gt;", ">")     
	            .replaceAll("&quot;", "\"").replaceAll("&apos;", "'");                                 
	                                                                                                   
	}                                                                                                  
	
	/*
	 * Process the response and returns the results
	 */
	private Map<String, String> getAssertionStatements(Assertion assertion) {

		Map<String, String> results = new HashMap<String, String>();

		if (assertion != null) {

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
	private void validateAudienceRestriction(Assertion assertion, ExternalIdPConfig externalIdPConfig) throws SAMLSSOException{
        
		if (assertion != null) {
			Conditions conditions = assertion.getConditions();
    		if (conditions != null) {
    			List<AudienceRestriction> audienceRestrictions = conditions.getAudienceRestrictions();
    			if (audienceRestrictions != null && !audienceRestrictions.isEmpty()) {
    				for (AudienceRestriction audienceRestriction : audienceRestrictions) {
    					if (audienceRestriction.getAudiences() != null && audienceRestriction.getAudiences().size() > 0) {
    						for (Audience audience : audienceRestriction.getAudiences()) {
								if (!"carbonServer".equals(audience.getAudienceURI())) {
									throw new SAMLSSOException("SAML Assertion Audience Restriction validation failed");
								}
							}
    					} else {
    						throw new SAMLSSOException("SAML Response's AudienceRestriction doesn't contain Audiences");
    					}
    				}
    			} else {
                    throw new SAMLSSOException("SAML Response doesn't contain AudienceRestrictions");
    			}
        	} else {
                throw new SAMLSSOException("SAML Response doesn't contain Conditions");
        	}
		}
	}


    /**
     * Validate the signature of a SAML2 Response and Assertion
     *
     * @param response   SAML2 Response
     * @return true, if signature is valid.
     */
    private void validateSignature(Response response, ExternalIdPConfig externalIdPConfig) throws SAMLSSOException{

        List<Assertion> assertions = response.getAssertions();
        Assertion assertion = null;
        if (assertions != null && assertions.size() > 0) {
            assertion = assertions.get(0);
        }
        if(externalIdPConfig.isAuthnResponseSigned()){
            if(response.getSignature() == null){
                throw new SAMLSSOException("SAMLResponse signing is enabled, but signature element not found in SAML Response element.");
            } else {
                try {
                    SignatureValidator validator = new SignatureValidator(credential);
                    validator.validate(response.getSignature());
                }  catch (ValidationException e) {
                    throw new SAMLSSOException("Signature validation failed for SAML Response");
                }
            }
        }
        if(true){
            if(assertion.getSignature() == null){
                throw new SAMLSSOException("SAMLAssertion signing is enabled, but signature element not found in SAML Assertion element.");
            } else {
                try {
                    SignatureValidator validator = new SignatureValidator(credential);
                    validator.validate(assertion.getSignature());
                }  catch (ValidationException e) {
                    throw new SAMLSSOException("Signature validation failed for SAML Assertion");
                }
            }
        }
    }

}
