/*
 * Copyright 2005-2008 WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.relyingparty.saml;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.util.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opensaml.DefaultBootstrap;
import org.opensaml.xml.ConfigurationException;
import org.w3c.dom.Element;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.relyingparty.RelyingPartyData;
import org.wso2.carbon.identity.relyingparty.RelyingPartyException;
import org.wso2.carbon.identity.relyingparty.TokenVerifierConstants;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.Map.Entry;

public class SAMLTokenConsumer {

    private static Log log = LogFactory.getLog(SAMLTokenConsumer.class);

    // Guaranteed to be thread safe
    private static SAMLTokenConsumer consumer = new SAMLTokenConsumer();

    static {
        try {
            DefaultBootstrap.bootstrap();
        } catch (ConfigurationException e) {
            log.error("SAMLTokenConsumerBootstrapFailure", e);
            throw new RuntimeException(e);
        }
    }

    private SAMLTokenConsumer() {
    }

    /**
     * Returns the SAMLTokenConsuer
     *
     * @return
     */
    public static SAMLTokenConsumer getInstance() {
        return consumer;
    }

    /**
     * The control flow is 1) Verify 2) Validate policies 3) Inject parameters into the
     * HttpServletRequest
     *
     * @param request
     * @param xmlToken
     * @param data
     * @throws RelyingPartyException
     */
    public void setInfocardSessionAttributes(HttpServletRequest request, String xmlToken,
                                             RelyingPartyData data) throws RelyingPartyException {

        SAMLTokenVerifier verifier = new SAMLTokenVerifier();
        Element plainTokenElem = verifier.decryptToken(xmlToken, data.getPrivateKey());
        boolean isAllSuccess = false;

        if (verifier.verifyDecryptedToken(plainTokenElem, data)) {
            if (validateIssuerInfoPolicy(verifier, data)) {
                isAllSuccess = true;
            }
        }

        if (isAllSuccess == false) {
            injectDataToRequestOnFailure(verifier, request);
        } else {
            injectDataToRequestOnSuccess(verifier, request);
        }
    }

    /**
     * Validates issuer info
     *
     * @param verifier
     * @return Whether issue validation successful or not.
     * @throws Exception
     */
    protected boolean validateIssuerInfoPolicy(SAMLTokenVerifier verifier, RelyingPartyData data)
            throws RelyingPartyException {
        String issuerName = verifier.getIssuerName();
        String issuerPolicy = data.getIssuerPolicy();

        try {
            if (IdentityConstants.SELF_ISSUED_ISSUER.equals(issuerName)) {
                if (issuerPolicy == null || issuerPolicy.equals(TokenVerifierConstants.SELF_ONLY)
                        || issuerPolicy.equals(TokenVerifierConstants.SELF_AND_MANGED)) {
                    return true;
                } else {
                    return false;
                }
            } else if (issuerPolicy.equals(TokenVerifierConstants.SELF_ONLY)) {
                // not a self issued card when self only
                return false;
            } else {
                return true;
            }
        } catch (Exception e) {
            log.error("Error in issuer policy validation", e);
            throw new RelyingPartyException("errorValidatingIssuerPolicy", e);
        }
    }

    /**
     * When the data token is invalid, this method injects invalid status message.
     *
     * @param verifier
     * @param request
     */
    protected void injectDataToRequestOnFailure(SAMLTokenVerifier verifier, ServletRequest request) {
        request.setAttribute(TokenVerifierConstants.SERVLET_ATTR_STATE,
                TokenVerifierConstants.STATE_FAILURE);
    }

    /**
     * When the token is valid this method injects valid states message
     *
     * @param verifier
     * @param request
     * @throws RelyingPartyException
     */
    protected void injectDataToRequestOnSuccess(SAMLTokenVerifier verifier, ServletRequest request)
            throws RelyingPartyException {
        String issuerInfo = getIssuerInfoString(verifier);
        Iterator propertyEntry = null;

        if (issuerInfo != null) {
            request.setAttribute(TokenVerifierConstants.ISSUER_INFO, issuerInfo);
        }

        propertyEntry = verifier.getAttributeTable().entrySet().iterator();

        while (propertyEntry.hasNext()) {
            Entry entry = (Entry) propertyEntry.next();
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            request.setAttribute(key, value);
        }

        request.setAttribute(TokenVerifierConstants.SERVLET_ATTR_STATE,
                TokenVerifierConstants.STATE_SUCCESS);
    }

    /**
     * @param verifier
     * @return
     * @throws RelyingPartyException
     */
    protected String getIssuerInfoString(SAMLTokenVerifier verifier) throws RelyingPartyException {
        String issuerInfo = null;
        OMFactory factory = null;
        OMNamespace namespace = null;
        Element keyInfo = null;
        OMElement certificates = null;
        OMElement omKeyInfo = null;
        boolean siginingSet = false;
        OMElement certElem = null;
        Iterator<X509Certificate> certIterator = null;

        try {
            factory = OMAbstractFactory.getOMFactory();
            namespace = factory.createOMNamespace(TokenVerifierConstants.NS,
                    TokenVerifierConstants.PREFIX);
            keyInfo = verifier.getKeyInfoElement();
            certIterator = verifier.getCertificates().iterator();

            while (certIterator.hasNext()) {
                X509Certificate cert = certIterator.next();
                byte[] encodedCert = cert.getEncoded();
                String base64Encoded = Base64.encode(encodedCert);

                if (certificates == null) {
                    certificates = factory.createOMElement(TokenVerifierConstants.LN_CERTIFICATES,
                            namespace);
                }

                certElem = factory
                        .createOMElement(TokenVerifierConstants.LN_CERTIFICATE, namespace);

                if (siginingSet == false) {
                    certElem.addAttribute(TokenVerifierConstants.LN_SIGNING_CERT, "true", null);
                    siginingSet = true;
                }
                certElem.setText(base64Encoded);
                certificates.addChild(certElem);
            }

            if (keyInfo != null) {
                String value = IdentityUtil.nodeToString(keyInfo);
                XMLStreamReader parser = XMLInputFactory.newInstance().createXMLStreamReader(
                        new StringReader(value));
                StAXOMBuilder builder = new StAXOMBuilder(parser);
                omKeyInfo = builder.getDocumentElement();
            }
        } catch (Exception e) {
            log.error("Error while building issuer info", e);
            throw new RelyingPartyException("errorBuildingIssuerInfo");
        }

        if (certificates != null) {
            issuerInfo = certificates.toString();
        }

        if (omKeyInfo != null) {
            if (issuerInfo != null) {
                issuerInfo = issuerInfo + omKeyInfo.toString();
            } else {
                issuerInfo = omKeyInfo.toString();
            }
        }
        return issuerInfo;
    }
}