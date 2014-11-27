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

package org.wso2.carbon.identity.application.authenticator.passive.sts.manager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opensaml.Configuration;
import org.opensaml.DefaultBootstrap;
import org.opensaml.saml1.core.Attribute;
import org.opensaml.saml1.core.AttributeStatement;
import org.opensaml.saml1.core.NameIdentifier;
import org.opensaml.saml1.core.Subject;
import org.opensaml.saml1.core.SubjectStatement;

import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallerFactory;
import org.opensaml.xml.io.UnmarshallingException;
import org.opensaml.xml.security.x509.X509Credential;
import org.opensaml.xml.signature.SignableXMLObject;
import org.opensaml.xml.signature.SignatureValidator;
import org.opensaml.xml.validation.ValidationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.wso2.carbon.identity.application.authentication.framework.config.model.ExternalIdPConfig;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authenticator.passive.sts.exception.PassiveSTSException;
import org.wso2.carbon.identity.application.authenticator.passive.sts.util.CarbonEntityResolver;
import org.wso2.carbon.identity.application.authenticator.passive.sts.util.PassiveSTSConstants;
import org.wso2.carbon.ui.CarbonUIUtil;
import org.xml.sax.SAXException;

public class PassiveSTSManager {

	private static Log log = LogFactory.getLog(PassiveSTSManager.class);
	
    private X509Credential credential = null;
    private static boolean bootStrapped = false;

	public PassiveSTSManager(ExternalIdPConfig externalIdPConfig) throws PassiveSTSException {
		String credentialImplClass = "org.wso2.carbon.identity.application.authenticator.passive.sts.manager.STSAgentKeyStoreCredential";
		try {
            synchronized (this){
                if(credential == null){
                    synchronized (this){
                        STSAgentCredential credential = (STSAgentCredential) Class.forName(credentialImplClass).newInstance();
                        credential.init(externalIdPConfig);
                        this.credential = new X509CredentialImpl(credential);
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            throw new PassiveSTSException("Error while instantiating SSOAgentCredentialImplClass: " + credentialImplClass, e);
        } catch (InstantiationException e) {
            throw new PassiveSTSException("Error while instantiating SSOAgentCredentialImplClass: " + credentialImplClass, e);
        } catch (IllegalAccessException e) {
            throw new PassiveSTSException("Error while instantiating SSOAgentCredentialImplClass: " + credentialImplClass, e);
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
	 * @param request
	 * @param loginPage
     * @param externalIdPConfig
     * @param contextIdentifier
     * @return redirectionUrl
     * @throws PassiveSTSException
	 */
	public String buildRequest(HttpServletRequest request, String loginPage, ExternalIdPConfig externalIdPConfig, String contextIdentifier, Map<String,String> authenticationProperties)
	                                                                              throws PassiveSTSException {

        String replyUrl = CarbonUIUtil.getAdminConsoleURL(request);
        replyUrl = replyUrl.replace("commonauth/carbon/", "commonauth");
        String action = "wsignin1.0";
        String realm = authenticationProperties.get(PassiveSTSConstants.REALM_ID);
        String redirectUrl =  loginPage + "?wa=" + action + "&wreply=" + replyUrl + "&wtrealm=" + realm ;
        try {
            redirectUrl = redirectUrl + "&wctx=" + URLEncoder.encode(contextIdentifier, "UTF-8").trim();
        } catch (UnsupportedEncodingException e) {
            throw new PassiveSTSException("Error occurred while url encoding WCTX ", e);
        }
        return redirectUrl;
	}

    /**
     * 
     * @param request
     * @param externalIdPConfig
     * @throws PassiveSTSException
     */
    public void processResponse(HttpServletRequest request, AuthenticationContext context) throws PassiveSTSException {

        doBootstrap();

        String response = request.getParameter(PassiveSTSConstants.HTTP_PARAM_PASSIVE_STS_RESULT).replaceAll("(\\r|\\n)", "");

        // there is no unmarshaller to unmarshall "RequestSecurityTokenResponseCollection". Therefore retrieve Assertion
        XMLObject xmlObject = unmarshall(response);

        if (xmlObject == null) {
            throw new PassiveSTSException("SAML Assertion not found in the Response");
        }

        String subject = null;
        Map<String, String> attributeMap =  new HashMap<String, String>();
        
        if(xmlObject instanceof org.opensaml.saml1.core.Assertion){
            
            org.opensaml.saml1.core.Assertion assertion = (org.opensaml.saml1.core.Assertion) xmlObject;
            /*List<SubjectStatement> subjectStatements = assertion.getSubjectStatements();
            
            if(subjectStatements != null && subjectStatements.size() > 0 && subjectStatements.get(0).getSubject() != null){
                subject = subjectStatements.get(0).getSubject().getNameIdentifier().getNameIdentifier();
            }*/
            
            if (assertion.getAuthenticationStatements() != null && assertion.getAuthenticationStatements().size() > 0) {
            	Subject subjectElem = assertion.getAuthenticationStatements().get(0).getSubject();
            	
            	if (subjectElem != null) {
            		NameIdentifier nameIdentifierElem = subjectElem.getNameIdentifier();
            		
            		if (nameIdentifierElem != null) {
            			subject = nameIdentifierElem.getNameIdentifier();
            		}
            	}
            }

            if(assertion.getAttributeStatements() != null && assertion.getAttributeStatements().size() > 0){
                if(subject == null){
                    subject =  assertion.getAttributeStatements().get(0).getSubject().getNameIdentifier().getNameIdentifier();
                }
                
                for(AttributeStatement statement : assertion.getAttributeStatements()){
                    List<Attribute> attributes = statement.getAttributes();  
                    for(Attribute attribute : attributes){
                        String attributeUri = attribute.getAttributeNamespace();
                        String attributeName =  attribute.getAttributeName();
                        List<XMLObject> xmlObjects = attribute.getAttributeValues();
                        for(XMLObject object : xmlObjects){
                            String attributeValue = object.getDOM().getTextContent();
                            attributeMap.put(attributeUri, attributeValue);
                        }
                    }
                }                               
            }
                       
            // validate signature this SP only looking for assertion signature
            //validateSignature(assertion);

        } else if(xmlObject instanceof org.opensaml.saml2.core.Assertion){
            
            org.opensaml.saml2.core.Assertion assertion = (org.opensaml.saml2.core.Assertion) xmlObject;

            if(assertion.getSubject() != null && assertion.getSubject().getNameID() != null){
                subject = assertion.getSubject().getNameID().getValue();
            }
            // validate signature this SP only looking for assertion signature
            //validateSignature(assertion);

            for(org.opensaml.saml2.core.AttributeStatement statement : assertion.getAttributeStatements()){
                List<org.opensaml.saml2.core.Attribute> attributes = statement.getAttributes();
                for(org.opensaml.saml2.core.Attribute attribute : attributes){
                    String attributeUri = attribute.getName();
                    String attributeName =  attribute.getFriendlyName();
                    List<XMLObject> xmlObjects = attribute.getAttributeValues();
                    for(XMLObject object : xmlObjects){
                        String attributeValue = object.getDOM().getTextContent();
                        attributeMap.put(attributeUri, attributeValue);
                    }
                }
            }
        }

        if(subject == null){
            throw new PassiveSTSException("SAML Response does not contain the name of the subject");
        }
        
        context.setSubject(subject);
        request.getSession().setAttribute("userAttributes", attributeMap);
    }

    /**
     *
     * @param samlString
     * @return
     * @throws PassiveSTSException
     */
	private XMLObject unmarshall(String samlString) throws PassiveSTSException {
        
        samlString = decodeHTMLCharacters(samlString);
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setExpandEntityReferences(false);
		documentBuilderFactory.setNamespaceAware(true);

        try {
            DocumentBuilder docBuilder = documentBuilderFactory.newDocumentBuilder();
            docBuilder.setEntityResolver(new CarbonEntityResolver());
            ByteArrayInputStream is = new ByteArrayInputStream(samlString.getBytes(Charset.forName("UTF-8")));
            Document document = docBuilder.parse(is);
            Element element = document.getDocumentElement();
                                              
            NodeList nodeList =  element.getElementsByTagNameNS("http://docs.oasis-open.org/ws-sx/ws-trust/200512", 
                                                                                            "RequestedSecurityToken");
            if(nodeList == null || nodeList.getLength() == 0){
                throw new PassiveSTSException("Security Token is not found in the Response");        
            }

            if(nodeList.getLength() > 1){
                log.warn("More than one Security Token is found in the Response");
            }  

            Element node = (Element) nodeList.item(0).getFirstChild();
            UnmarshallerFactory unmarshallerFactory = Configuration.getUnmarshallerFactory();
            Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(node);
            return unmarshaller.unmarshall(node);
        } catch (ParserConfigurationException e) {
            throw new PassiveSTSException("Error in unmarshalling SAML Request from the encoded String", e);
        } catch (UnmarshallingException e) {
            throw new PassiveSTSException("Error in unmarshalling SAML Request from the encoded String", e);
        } catch (SAXException e) {
            throw new PassiveSTSException("Error in unmarshalling SAML Request from the encoded String", e);
        } catch (IOException e) {
            throw new PassiveSTSException("Error in unmarshalling SAML Request from the encoded String", e);
        }

    }

    /**
     *
     * @param encodedStr
     * @return
     */
	private String decodeHTMLCharacters(String encodedStr) {
	    return encodedStr.replaceAll("&amp;", "&").replaceAll("&lt;", "<").replaceAll("&gt;", ">")
	            .replaceAll("&quot;", "\"").replaceAll("&apos;", "'");

	}

    /**
     * Validate the signature of a SAML2 Response and Assertion
     *
     * @param assertion   SAML2 Response
     * @throws PassiveSTSException
     */
    private void validateSignature(SignableXMLObject assertion) throws PassiveSTSException{

        if(assertion.getSignature() == null){
            throw new PassiveSTSException("SAMLAssertion signing is enabled, but signature element not found in SAML Assertion element.");
        } else {
            try {
                SignatureValidator validator = new SignatureValidator(credential);
                validator.validate(assertion.getSignature());
            }  catch (ValidationException e) {
                throw new PassiveSTSException("Signature validation failed for SAML Assertion");
            }
        }
    }
}
