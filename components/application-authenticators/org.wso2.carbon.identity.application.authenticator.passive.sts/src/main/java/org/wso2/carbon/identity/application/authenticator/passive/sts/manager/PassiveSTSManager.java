/*
 * Copyright (c) 2005, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.application.authenticator.passive.sts.manager;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xerces.impl.Constants;
import org.apache.xerces.util.SecurityManager;
import org.opensaml.Configuration;
import org.opensaml.DefaultBootstrap;
import org.opensaml.saml1.core.Attribute;
import org.opensaml.saml1.core.AttributeStatement;
import org.opensaml.saml1.core.NameIdentifier;
import org.opensaml.saml1.core.Subject;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallerFactory;
import org.opensaml.xml.io.UnmarshallingException;
import org.opensaml.xml.security.x509.X509Credential;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.wso2.carbon.identity.application.authentication.framework.config.model.ExternalIdPConfig;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedUser;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.application.authenticator.passive.sts.exception.PassiveSTSException;
import org.wso2.carbon.identity.application.authenticator.passive.sts.util.CarbonEntityResolver;
import org.wso2.carbon.identity.application.authenticator.passive.sts.util.PassiveSTSConstants;
import org.wso2.carbon.identity.application.common.model.Claim;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletRequest;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PassiveSTSManager {

    private static final String SECURITY_MANAGER_PROPERTY = Constants.XERCES_PROPERTY_PREFIX +
            Constants.SECURITY_MANAGER_PROPERTY;
    private static final int ENTITY_EXPANSION_LIMIT = 0;
    private static Log log = LogFactory.getLog(PassiveSTSManager.class);
    private static boolean bootStrapped = false;
    private X509Credential credential = null;

    public PassiveSTSManager(ExternalIdPConfig externalIdPConfig) throws PassiveSTSException {

        String credentialImplClass = "org.wso2.carbon.identity.application.authenticator.passive.sts.manager.STSAgentKeyStoreCredential";
        try {
            synchronized (this) {
                if (credential == null) {
                    synchronized (this) {
                        STSAgentCredential stsAgentCredential = (STSAgentCredential) Class.forName(credentialImplClass).newInstance();
                        stsAgentCredential.init(externalIdPConfig);
                        this.credential = new X509CredentialImpl(stsAgentCredential);
                    }
                }
            }
        } catch (ClassNotFoundException|InstantiationException|IllegalAccessException e) {
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
     * @param contextIdentifier
     * @return redirectionUrl
     * @throws PassiveSTSException
     */
    public String buildRequest(HttpServletRequest request, String loginPage,
                               String contextIdentifier, Map<String, String> authenticationProperties)
            throws PassiveSTSException {

        String replyUrl = IdentityUtil.getServerURL(FrameworkConstants.COMMONAUTH);
        String action = "wsignin1.0";
        String realm = authenticationProperties.get(PassiveSTSConstants.REALM_ID);
        String redirectUrl = loginPage + "?wa=" + action + "&wreply=" + replyUrl + "&wtrealm=" + realm;
        try {
            redirectUrl = redirectUrl + "&wctx=" + URLEncoder.encode(contextIdentifier, "UTF-8").trim();
        } catch (UnsupportedEncodingException e) {
            throw new PassiveSTSException("Error occurred while url encoding WCTX ", e);
        }
        return redirectUrl;
    }

    /**
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
        Map<String, String> attributeMap = new HashMap<String, String>();

        if (xmlObject instanceof org.opensaml.saml1.core.Assertion) {
            org.opensaml.saml1.core.Assertion assertion = (org.opensaml.saml1.core.Assertion) xmlObject;
            if (CollectionUtils.isNotEmpty(assertion.getAuthenticationStatements())) {
                Subject subjectElem = assertion.getAuthenticationStatements().get(0).getSubject();

                if (subjectElem != null) {
                    NameIdentifier nameIdentifierElem = subjectElem.getNameIdentifier();

                    if (nameIdentifierElem != null) {
                        subject = nameIdentifierElem.getNameIdentifier();
                    }
                }
            }

            if (CollectionUtils.isNotEmpty(assertion.getAttributeStatements())) {
                if (subject == null) {
                    subject = assertion.getAttributeStatements().get(0).getSubject().getNameIdentifier().getNameIdentifier();
                }

                for (AttributeStatement statement : assertion.getAttributeStatements()) {
                    List<Attribute> attributes = statement.getAttributes();
                    for (Attribute attribute : attributes) {
                        String attributeUri = attribute.getAttributeNamespace();
                        List<XMLObject> xmlObjects = attribute.getAttributeValues();
                        for (XMLObject object : xmlObjects) {
                            String attributeValue = object.getDOM().getTextContent();
                            attributeMap.put(attributeUri, attributeValue);
                        }
                    }
                }
            }
        } else if (xmlObject instanceof org.opensaml.saml2.core.Assertion) {

            org.opensaml.saml2.core.Assertion assertion = (org.opensaml.saml2.core.Assertion) xmlObject;

            if (assertion.getSubject() != null && assertion.getSubject().getNameID() != null) {
                subject = assertion.getSubject().getNameID().getValue();
            }

            for (org.opensaml.saml2.core.AttributeStatement statement : assertion.getAttributeStatements()) {
                List<org.opensaml.saml2.core.Attribute> attributes = statement.getAttributes();
                for (org.opensaml.saml2.core.Attribute attribute : attributes) {
                    String attributeUri = attribute.getName();
                    List<XMLObject> xmlObjects = attribute.getAttributeValues();
                    for (XMLObject object : xmlObjects) {
                        String attributeValue = object.getDOM().getTextContent();
                        attributeMap.put(attributeUri, attributeValue);
                    }
                }
            }
        }

        Map<ClaimMapping, String> claimMappingStringMap = getClaimMappingsMap(attributeMap);
        String isSubjectInClaimsProp = context.getAuthenticatorProperties().get(
                IdentityApplicationConstants.Authenticator.SAML2SSO.IS_USER_ID_IN_CLAIMS);
        if ("true".equalsIgnoreCase(isSubjectInClaimsProp)) {
            subject = FrameworkUtils.getFederatedSubjectFromClaims(
                    context.getExternalIdP().getIdentityProvider(), claimMappingStringMap);
            if (subject == null) {
                log.warn("Subject claim could not be found amongst attribute statements. " +
                        "Defaulting to Name Identifier.");
            }
        }
        if (subject == null) {
            throw new PassiveSTSException("Cannot find federated User Identifier");
        }

        AuthenticatedUser authenticatedUser =
                AuthenticatedUser.createFederateAuthenticatedUserFromSubjectIdentifier(subject);
        authenticatedUser.setUserAttributes(claimMappingStringMap);
        context.setSubject(authenticatedUser);
    }

    /**
     * @param samlString
     * @return
     * @throws PassiveSTSException
     */
    private XMLObject unmarshall(String samlString) throws PassiveSTSException {

        String samlStr = samlString;
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);

            documentBuilderFactory.setExpandEntityReferences(false);
            documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            SecurityManager securityManager = new SecurityManager();
            securityManager.setEntityExpansionLimit(ENTITY_EXPANSION_LIMIT);
            documentBuilderFactory.setAttribute(SECURITY_MANAGER_PROPERTY, securityManager);

            DocumentBuilder docBuilder = documentBuilderFactory.newDocumentBuilder();
            docBuilder.setEntityResolver(new CarbonEntityResolver());
            ByteArrayInputStream is = new ByteArrayInputStream(samlStr.getBytes(Charset.forName("UTF-8")));
            Document document = docBuilder.parse(is);
            Element element = document.getDocumentElement();

            NodeList nodeList = element.getElementsByTagNameNS("http://docs.oasis-open.org/ws-sx/ws-trust/200512",
                    "RequestedSecurityToken");
            if (nodeList == null || nodeList.getLength() == 0) {
                throw new PassiveSTSException("Security Token is not found in the Response");
            }

            if (nodeList.getLength() > 1) {
                log.warn("More than one Security Token is found in the Response");
            }

            Element node = (Element) nodeList.item(0).getFirstChild();
            UnmarshallerFactory unmarshallerFactory = Configuration.getUnmarshallerFactory();
            Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(node);
            return unmarshaller.unmarshall(node);
        } catch (ParserConfigurationException e) {
            throw new PassiveSTSException(PassiveSTSConstants.ERROR_IN_UNMARSHALLING_SAML_REQUEST_FROM_THE_ENCODED_STRING, e);
        } catch (UnmarshallingException e) {
            throw new PassiveSTSException(PassiveSTSConstants.ERROR_IN_UNMARSHALLING_SAML_REQUEST_FROM_THE_ENCODED_STRING, e);
        } catch (SAXException e) {
            throw new PassiveSTSException(PassiveSTSConstants.ERROR_IN_UNMARSHALLING_SAML_REQUEST_FROM_THE_ENCODED_STRING, e);
        } catch (IOException e) {
            throw new PassiveSTSException(PassiveSTSConstants.ERROR_IN_UNMARSHALLING_SAML_REQUEST_FROM_THE_ENCODED_STRING, e);
        }

    }

    /*
     * Process the response and returns the results
     */
    private Map<ClaimMapping, String> getClaimMappingsMap(Map<String, String> userAttributes) {

        Map<ClaimMapping, String> results = new HashMap<ClaimMapping, String>();
        for (Map.Entry<String, String> entry : userAttributes.entrySet()) {
            ClaimMapping claimMapping = new ClaimMapping();
            Claim claim = new Claim();
            claim.setClaimUri(entry.getKey());
            claimMapping.setRemoteClaim(claim);
            results.put(claimMapping, entry.getValue());
        }
        return results;
    }
}
