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

package org.wso2.carbon.identity.sso.agent.saml;

import org.joda.time.DateTime;
import org.opensaml.Configuration;
import org.opensaml.DefaultBootstrap;
import org.opensaml.common.SAMLVersion;
import org.opensaml.saml2.core.*;
import org.opensaml.saml2.core.impl.*;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.*;
import org.opensaml.xml.security.x509.X509Credential;
import org.opensaml.xml.signature.SignatureValidator;
import org.opensaml.xml.util.Base64;
import org.opensaml.xml.util.XMLHelper;
import org.opensaml.xml.validation.ValidationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.wso2.carbon.identity.sso.agent.bean.SSOAgentSessionBean;
import org.wso2.carbon.identity.sso.agent.exception.SSOAgentException;
import org.wso2.carbon.identity.sso.agent.util.SSOAgentConfigs;
import org.wso2.carbon.identity.sso.agent.util.SSOAgentConstants;
import org.wso2.carbon.identity.sso.agent.util.SSOAgentUtils;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public class SAML2SSOManager {

	private String authReqRandomId = Integer.toHexString(new Double(Math.random()).intValue());
	private String relayState = null;
    private X509Credential credential = null;

	public SAML2SSOManager() throws SSOAgentException {
		/* Initializing the OpenSAML library, loading default configurations */
		try {
            DefaultBootstrap.bootstrap();
            synchronized (this){
                if(credential == null){
                    synchronized (this){
                        SSOAgentCredential credential = (SSOAgentCredential) Class.forName(SSOAgentConfigs.getSSOAgentCredentialImplClass()).newInstance();
                        credential.init();
                        this.credential = new X509CredentialImpl(credential);
                    }
                }
            }
        } catch (ConfigurationException e) {
            throw new SSOAgentException("Error while bootstrapping OpenSAML library", e);
        } catch (ClassNotFoundException e) {
            throw new SSOAgentException("Error while instantiating SSOAgentCredentialImplClass: " +
                    SSOAgentConfigs.getSSOAgentCredentialImplClass(), e);
        } catch (InstantiationException e) {
            throw new SSOAgentException("Error while instantiating SSOAgentCredentialImplClass: " +
                    SSOAgentConfigs.getSSOAgentCredentialImplClass(), e);
        } catch (IllegalAccessException e) {
            throw new SSOAgentException("Error while instantiating SSOAgentCredentialImplClass: " +
                    SSOAgentConfigs.getSSOAgentCredentialImplClass(), e);
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
	public String buildRequest(HttpServletRequest request, boolean isLogout, boolean isPassive) throws SSOAgentException {

		RequestAbstractType requestMessage;
		if (!isLogout) {
			requestMessage = buildAuthnRequest(isPassive);
		} else { 
			requestMessage = buildLogoutRequest(((SSOAgentSessionBean) request.getSession().getAttribute(
                    SSOAgentConfigs.getSessionBeanName())).getSAMLSSOSessionBean().getSubjectId(),
                    ((SSOAgentSessionBean) request.getSession().getAttribute(
                            SSOAgentConfigs.getSessionBeanName())).getSAMLSSOSessionBean().getIdPSessionIndex());
		}
        String idpUrl = null;
        
        String encodedRequestMessage = encodeRequestMessage(requestMessage);
        StringBuilder httpQueryString = new StringBuilder("SAMLRequest=" + encodedRequestMessage);
                
        if(relayState != null && !relayState.isEmpty()){
            try {
                httpQueryString.append("&RelayState=" + URLEncoder.encode(relayState, "UTF-8").trim());
            } catch (UnsupportedEncodingException e) {
                throw new SSOAgentException("Error occurred while url encoding RelayState", e);
            }
        }
        
        if(SSOAgentConfigs.isRequestSigned()){
            SSOAgentUtils.addDeflateSignatureToHTTPQueryString(httpQueryString, credential);
        }
        
        if(SSOAgentConfigs.getIdPUrl().indexOf("?") > -1){
            idpUrl = SSOAgentConfigs.getIdPUrl().concat("&").concat(httpQueryString.toString());
        } else {
            idpUrl = SSOAgentConfigs.getIdPUrl().concat("?").concat(httpQueryString.toString());
        }
        return idpUrl;
	}
	
    public void processResponse(HttpServletRequest request) throws SSOAgentException {

            String decodedResponse = new String(Base64.decode(request.getParameter(SSOAgentConstants.HTTP_POST_PARAM_SAML2_RESP)));
            XMLObject samlObject = unmarshall(decodedResponse);
            if (samlObject instanceof LogoutResponse) {
                //This is a SAML response for a single logout request from the SP
                doSLO(request);
            } else {
                processSSOResponse(request);
            }
    }

    /**
     * This method handles the logout requests from the IdP
     * Any request for the defined logout URL is handled here
     * @param request
     * @throws javax.servlet.ServletException
     * @throws IOException
     */
    public void doSLO (HttpServletRequest request) throws SSOAgentException {
        XMLObject samlObject = null;
        if(request.getParameter(SSOAgentConstants.HTTP_POST_PARAM_SAML2_AUTH_REQ) != null){
            samlObject = unmarshall(new String(Base64.decode(request.getParameter(SSOAgentConstants.HTTP_POST_PARAM_SAML2_AUTH_REQ))));
        }
        if(samlObject == null){
            samlObject = unmarshall(new String(Base64.decode(request.getParameter(SSOAgentConstants.HTTP_POST_PARAM_SAML2_RESP))));
        }
        if (samlObject instanceof LogoutRequest) {
            LogoutRequest logoutRequest = (LogoutRequest) samlObject;
            String sessionIndex = logoutRequest.getSessionIndexes().get(0).getSessionIndex();
            SSOAgentSessionManager.invalidateSession(sessionIndex);
        } else if (samlObject instanceof LogoutResponse){
            if(request.getSession(false) != null){
                request.getSession().invalidate();
            }
        } else {
            throw new SSOAgentException("Invalid Single Logout SAML Request");
        }
    }

    private void processSSOResponse(HttpServletRequest request) throws SSOAgentException {

        SSOAgentSessionBean sessionBean = new SSOAgentSessionBean();
        sessionBean.setSAMLSSOSessionBean(sessionBean.new SAMLSSOSessionBean());
        request.getSession().setAttribute(SSOAgentConfigs.getSessionBeanName(), sessionBean);

        String samlResponseString =
                new String(Base64.decode(request.getParameter(SSOAgentConstants.HTTP_POST_PARAM_SAML2_RESP)));
        Response samlResponse = (Response) unmarshall(samlResponseString);
        sessionBean.getSAMLSSOSessionBean().setSAMLResponseString(samlResponseString);
        sessionBean.getSAMLSSOSessionBean().setSAMLResponse(samlResponse);

        List<Assertion> assertions = samlResponse.getAssertions();
        Assertion assertion = null;
        if (assertions != null && assertions.size() > 0) {
            assertion = assertions.get(0);
        }
        if (assertion == null) {
            if (samlResponse.getStatus() != null &&
                    samlResponse.getStatus().getStatusCode() != null &&
                    samlResponse.getStatus().getStatusCode().getValue().equals(SSOAgentConstants.StatusCodes.IDENTITY_PROVIDER_ERROR) &&
                    samlResponse.getStatus().getStatusCode().getStatusCode() != null &&
                    samlResponse.getStatus().getStatusCode().getStatusCode().getValue().equals(SSOAgentConstants.StatusCodes.NO_PASSIVE)) {
                    request.getSession().removeAttribute(SSOAgentConfigs.getSessionBeanName());
                return;
            }
            throw new SSOAgentException("SAML Assertion not found in the Response");
        }

        sessionBean.getSAMLSSOSessionBean().setSAMLAssertion(assertion);
        // Cannot marshall SAML assertion here, before signature validation due to a weird issue in OpenSAML

        // Get the subject name from the Response Object and forward it to login_action.jsp
        String subject = null;
        if(assertion.getSubject() != null && assertion.getSubject().getNameID() != null){
            subject = assertion.getSubject().getNameID().getValue();
        }

        if(subject == null){
            throw new SSOAgentException("SAML Response does not contain the name of the subject");
        }


        sessionBean.getSAMLSSOSessionBean().setSubjectId(subject); // set the subject
        request.getSession().setAttribute(SSOAgentConfigs.getSessionBeanName(), sessionBean);

        // validate audience restriction
        validateAudienceRestriction(assertion);

        // validate signature
        validateSignature(samlResponse);

        // Marshalling SAML assertion after signature validation due to a weird issue in OpenSAML
        sessionBean.getSAMLSSOSessionBean().setSAMLAssertionString(marshall(assertion));

        ((SSOAgentSessionBean)request.getSession().getAttribute(
                SSOAgentConfigs.getSessionBeanName())).getSAMLSSOSessionBean().
                setSAMLSSOAttributes(getAssertionStatements(assertion));

        //For removing the session when the single sign out request made by the SP itself
        if(SSOAgentConfigs.isSLOEnabled()){
            String sessionId = assertion.getAuthnStatements().get(0).getSessionIndex();
            if(sessionId == null){
                throw new SSOAgentException("Single Logout is enabled but IdP Session ID not found in SAML Assertion");
            }
            ((SSOAgentSessionBean)request.getSession().getAttribute(
                    SSOAgentConfigs.getSessionBeanName())).getSAMLSSOSessionBean().setIdPSessionIndex(sessionId);
            SSOAgentSessionManager.addAuthenticatedSession(sessionId, request.getSession());
        }

    }

	private LogoutRequest buildLogoutRequest(String user, String sessionIdx) throws SSOAgentException{

		LogoutRequest logoutReq = new LogoutRequestBuilder().buildObject();

		logoutReq.setID(SSOAgentUtils.createID());
		logoutReq.setDestination(SSOAgentConfigs.getIdPUrl());

		DateTime issueInstant = new DateTime();
		logoutReq.setIssueInstant(issueInstant);
		logoutReq.setNotOnOrAfter(new DateTime(issueInstant.getMillis() + 5 * 60 * 1000));

		IssuerBuilder issuerBuilder = new IssuerBuilder();
		Issuer issuer = issuerBuilder.buildObject();
		issuer.setValue(SSOAgentConfigs.getIssuerId());
		logoutReq.setIssuer(issuer);

		NameID nameId = new NameIDBuilder().buildObject();
		nameId.setFormat("urn:oasis:names:tc:SAML:2.0:nameid-format:entity");
		nameId.setValue(user);
		logoutReq.setNameID(nameId);

		SessionIndex sessionIndex = new SessionIndexBuilder().buildObject();
		sessionIndex.setSessionIndex(sessionIdx);
		logoutReq.getSessionIndexes().add(sessionIndex);

		logoutReq.setReason("Single Logout");

		return logoutReq;
	}

	private AuthnRequest buildAuthnRequest(boolean isPassive) throws SSOAgentException{

		
		IssuerBuilder issuerBuilder = new IssuerBuilder();
		Issuer issuer =
		                issuerBuilder.buildObject("urn:oasis:names:tc:SAML:2.0:assertion",
		                                          "Issuer", "samlp");
		issuer.setValue(SSOAgentConfigs.getIssuerId());

		/* NameIDPolicy */
		NameIDPolicyBuilder nameIdPolicyBuilder = new NameIDPolicyBuilder();
		NameIDPolicy nameIdPolicy = nameIdPolicyBuilder.buildObject();
		nameIdPolicy.setFormat("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent");
		nameIdPolicy.setSPNameQualifier("Issuer");
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
		authRequest.setIsPassive(isPassive);
		authRequest.setIssueInstant(issueInstant);
		authRequest.setProtocolBinding("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST");
		authRequest.setAssertionConsumerServiceURL(SSOAgentConfigs.getConsumerUrl());
		authRequest.setIssuer(issuer);
		authRequest.setNameIDPolicy(nameIdPolicy);
		authRequest.setRequestedAuthnContext(requestedAuthnContext);
		authRequest.setID(authReqRandomId);
		authRequest.setVersion(SAMLVersion.VERSION_20);
		authRequest.setDestination(SSOAgentConfigs.getIdPUrl());

		/* Requesting Attributes. This Index value is registered in the IDP */
		if (SSOAgentConfigs.getAttributeConsumingServiceIndex() != null && SSOAgentConfigs.getAttributeConsumingServiceIndex().trim().length() > 0) {
			authRequest.setAttributeConsumingServiceIndex(Integer.parseInt(SSOAgentConfigs.getAttributeConsumingServiceIndex()));
		}

		return authRequest;
	}

	private String encodeRequestMessage(RequestAbstractType requestMessage) throws SSOAgentException{

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
            throw new SSOAgentException("Error occurred while encoding SAML request",e);
        } catch (UnsupportedEncodingException e) {
            throw new SSOAgentException("Error occurred while encoding SAML request",e);
        } catch (IOException e) {
            throw new SSOAgentException("Error occurred while encoding SAML request",e);
        }
    }

	private XMLObject unmarshall(String samlString) throws SSOAgentException {

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
            throw new SSOAgentException("Error in unmarshalling SAML Request from the encoded String", e);
        } catch (UnmarshallingException e) {
            throw new SSOAgentException("Error in unmarshalling SAML Request from the encoded String", e);
        } catch (SAXException e) {
            throw new SSOAgentException("Error in unmarshalling SAML Request from the encoded String", e);
        } catch (IOException e) {
            throw new SSOAgentException("Error in unmarshalling SAML Request from the encoded String", e);
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
	private void validateAudienceRestriction(Assertion assertion) throws SSOAgentException{
        
		if (assertion != null) {
			Conditions conditions = assertion.getConditions();
    		if (conditions != null) {
    			List<AudienceRestriction> audienceRestrictions = conditions.getAudienceRestrictions();
    			if (audienceRestrictions != null && !audienceRestrictions.isEmpty()) {
                    boolean audienceFound = false;
    				for (AudienceRestriction audienceRestriction : audienceRestrictions) {
    					if (audienceRestriction.getAudiences() != null && audienceRestriction.getAudiences().size() > 0) {
    						for (Audience audience : audienceRestriction.getAudiences()) {
								if (SSOAgentConfigs.getIssuerId().equals(audience.getAudienceURI())) {
									audienceFound = true;
                                    break;
								}
							}
    					}
                        if(audienceFound){
                            break;
                        }
    				}
                    if(!audienceFound){
                        throw new SSOAgentException("SAML Assertion Audience Restriction validation failed");
                    }
    			} else {
                    throw new SSOAgentException("SAML Response doesn't contain AudienceRestrictions");
    			}
        	} else {
                throw new SSOAgentException("SAML Response doesn't contain Conditions");
        	}
		}
	}


    /**
     * Validate the signature of a SAML2 Response and Assertion
     *
     * @param response   SAML2 Response
     * @return true, if signature is valid.
     */
    private void validateSignature(Response response) throws SSOAgentException{

        List<Assertion> assertions = response.getAssertions();
        Assertion assertion = null;
        if (assertions != null && assertions.size() > 0) {
            assertion = assertions.get(0);
        }
        if(SSOAgentConfigs.isResponseSigned()){
            if(response.getSignature() == null){
                throw new SSOAgentException("SAMLResponse signing is enabled, but signature element not found in SAML Response element.");
            } else {
                try {
                    SignatureValidator validator = new SignatureValidator(credential);
                    validator.validate(response.getSignature());
                }  catch (ValidationException e) {
                    throw new SSOAgentException("Signature validation failed for SAML Response");
                }
            }
        }
        if(SSOAgentConfigs.isAssertionSigned()){
            if(assertion.getSignature() == null){
                throw new SSOAgentException("SAMLAssertion signing is enabled, but signature element not found in SAML Assertion element.");
            } else {
                try {
                    SignatureValidator validator = new SignatureValidator(credential);
                    validator.validate(assertion.getSignature());
                }  catch (ValidationException e) {
                    throw new SSOAgentException("Signature validation failed for SAML Assertion");
                }
            }
        }
    }

    /**
     * Serialize the Auth. Request
     *
     * @param xmlObject
     * @return serialized auth. req
     */
     public static String marshall(XMLObject xmlObject) throws SSOAgentException {
     try {
        System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
                "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
            MarshallerFactory marshallerFactory =
                    org.opensaml.xml.Configuration.getMarshallerFactory();
            Marshaller marshaller = marshallerFactory.getMarshaller(xmlObject);
            Element element = marshaller.marshall(xmlObject);
            ByteArrayOutputStream byteArrayOutputStrm = new ByteArrayOutputStream();
            DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
            DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation("LS");
            LSSerializer writer = impl.createLSSerializer();
            LSOutput output = impl.createLSOutput();
            output.setByteStream(byteArrayOutputStrm);
            writer.write(element, output);
            return byteArrayOutputStrm.toString();
        } catch (ClassNotFoundException e) {
            throw new SSOAgentException("Error in marshalling SAML Assertion", e);
        } catch (InstantiationException e) {
            throw new SSOAgentException("Error in marshalling SAML Assertion", e);
        } catch (MarshallingException e) {
            throw new SSOAgentException("Error in marshalling SAML Assertion", e);
        } catch (IllegalAccessException e) {
            throw new SSOAgentException("Error in marshalling SAML Assertion", e);
        }
    }

}
