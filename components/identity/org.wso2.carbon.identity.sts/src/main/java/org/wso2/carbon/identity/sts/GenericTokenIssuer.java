/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.sts;

import java.io.ByteArrayInputStream;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.util.Date;

import javax.crypto.SecretKey;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.util.UUIDGenerator;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.rahas.RahasConstants;
import org.apache.rahas.RahasData;
import org.apache.rahas.Token;
import org.apache.rahas.TokenIssuer;
import org.apache.rahas.TrustException;
import org.apache.rahas.TrustUtil;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.message.WSSecEncryptedKey;
import org.apache.ws.security.message.token.SecurityTokenReference;
import org.apache.ws.security.util.WSSecurityUtil;
import org.apache.ws.security.util.XmlSchemaDateFormat;
import org.apache.xml.security.encryption.EncryptedData;
import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.keys.KeyInfo;
import org.joda.time.DateTime;
import org.opensaml.SAMLException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.persistence.IdentityPersistenceManager;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.provider.GenericIdentityProviderData;
import org.wso2.carbon.identity.provider.IdentityProviderException;
import org.wso2.carbon.identity.provider.IdentityProviderUtil;
import org.wso2.carbon.identity.provider.saml.SAML1TokenBuilder;
import org.wso2.carbon.identity.provider.saml.SAML2TokenBuilder;
import org.wso2.carbon.identity.provider.saml.SAMLTokenBuilder;
import org.wso2.carbon.identity.provider.saml.SAMLTokenDirector;

public class GenericTokenIssuer implements TokenIssuer {
    private static Log log = LogFactory.getLog(IdentityTokenIssuer.class);
    private final static String WSS_SAML_NS = "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.0#";
    protected GenericIdentityProviderData ipData = null;
    public final static String ISSUER_SELF = IdentityConstants.NS + "/issuer/self";
    private static Log tokenIssuerLog = LogFactory.getLog(IdentityConstants.TOKEN_ISSUSER_LOG);
    private boolean isTokenLogDebug = false;

    public GenericTokenIssuer() {
        isTokenLogDebug = tokenIssuerLog.isDebugEnabled();
    }

    /**
     * {@inheritDoc}
     */
    public String getResponseAction(RahasData data) throws TrustException {
        return RahasConstants.WST_NS_05_02 + RahasConstants.RSTR_ACTION_ISSUE;
    }

    /**
     * {@inheritDoc}
     */
    public void setConfigurationElement(OMElement configElement) {
        // Nothing to do
    }

    /**
     * {@inheritDoc}
     */
    public void setConfigurationFile(String configFile) {
        // Nothing to do
    }

    /**
     * {@inheritDoc}
     */
    public void setConfigurationParamName(String configParamName) {
        // Nothing to do
    }

    /**
     * {@inheritDoc}
     */
    public SOAPEnvelope issue(RahasData data) throws TrustException {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Request: \n" + data.getRstElement().toString() + "\n\n");
            }
            ipData = getIdentityProviderData(data);

            if (isTokenLogDebug) {
                tokenIssuerLog.debug("validInfoCard");
            }
            return createResponse(data);
        } catch (Exception e) {
            throw new TrustException(TrustException.REQUEST_FAILED, e);
        } finally {
            log.info("Issued token");
        }
    }

    /**
     * Create the response SOAP envelope.
     * 
     * @param data WS-Trust information in the issue request.
     * @return response SOAP envelope.
     * @throws TrustException
     */
    protected SOAPEnvelope createResponse(RahasData rahasData) throws TrustException {
        MessageContext inMsgCtx = null;
        SOAPEnvelope envelope = null;
        Document doc = null;
        WSSecEncryptedKey encryptedKey = null;
        X509Certificate serviceCert = null;
        try {

            inMsgCtx = rahasData.getInMessageContext();
            envelope = TrustUtil.createSOAPEnvelope(inMsgCtx.getEnvelope().getNamespace()
                    .getNamespaceURI());
            doc = ((Element) envelope).getOwnerDocument();

            // Create EncryptedKey
            serviceCert = ipData.getRpCert();
            if (serviceCert != null) {
                Element encrKeyElem = null;
                Element keyInfoElem = null;

                encryptedKey = new WSSecEncryptedKey();
                encryptedKey.setUseThisCert(serviceCert);
                encryptedKey.setKeySize(256);
                encryptedKey.setKeyEncAlgo(WSConstants.KEYTRANSPORT_RSAOEP);
                encryptedKey.setKeyIdentifierType(WSConstants.THUMBPRINT_IDENTIFIER);
                encryptedKey.prepare(doc, null);

                encrKeyElem = encryptedKey.getEncryptedKeyElement();

                // Create KeyInfo structure - START
                keyInfoElem = doc.createElementNS(WSConstants.SIG_NS, "KeyInfo");
                ((OMElement) encrKeyElem).declareNamespace(WSConstants.SIG_NS,
                        WSConstants.SIG_PREFIX);
                ((OMElement) encrKeyElem).declareNamespace(WSConstants.ENC_NS,
                        WSConstants.ENC_PREFIX);
                keyInfoElem.appendChild(encrKeyElem);
                // Create KeyInfo structure - END

            }

            if (!checkIsValidTokenType(ipData)) {
                throw new IdentityProviderException("invalidTokenType");
            }

            DateTime notBefore = null;
            DateTime notAfter = null;
            String assertionId = null;
            Element assertionNode = null;
            OMElement rstrElem = null;

            notBefore = new DateTime();
            notAfter = new DateTime(notBefore.getMillis() + (300 * 1000));
            assertionId = UUIDGenerator.getUUID();

            if (isTokenLogDebug) {
                tokenIssuerLog.debug("startSAMLTokenCreation");
            }

            assertionNode = createSAMLAssertionAsDOM(ipData, rahasData, notBefore, notAfter,
                    assertionId);

            if (isTokenLogDebug) {
                tokenIssuerLog.debug("finishSAMLTokenCreation");
            }

            rstrElem = createRSTR(rahasData, notBefore.toDate(), notAfter.toDate(), envelope, doc,
                    assertionNode, assertionId, encryptedKey);

            if (isTokenLogDebug) {
                tokenIssuerLog.debug("RSTRCreationDone");
            }

            if (log.isDebugEnabled()) {
                log.debug("Response created");
                log.debug("Response body : \n" + rstrElem.toString() + "\n\n");
            }

            return envelope;
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new TrustException(TrustException.REQUEST_FAILED, e);
        }
    }

    /**
     * Create the <code>wst:RequstedSecurityTokenRespoonse</code> element.
     * 
     * @param data WS-Trust information in the issue request
     * @param notBefore Created time
     * @param notAfter Expiration time
     * @param env Response SOAP envelope
     * @param doc <code>org.w3.dom.Document</code> instance of the response SOAP envelope
     * @param assertion SAML Assertion to be sent in the response.
     * @param encryptedKey Key used to encrypt the SAML assertion.
     * @return <code>wst:RequstedSecurityTokenRespoonse</code> element.
     * @throws TrustException
     * @throws SAMLException
     */
    protected OMElement createRSTR(RahasData data, Date notBefore, Date notAfter, SOAPEnvelope env,
            Document doc, Node assertionElem, String assertionId, WSSecEncryptedKey encryptedKey)
            throws TrustException, SAMLException, IdentityProviderException {
        if (log.isDebugEnabled()) {
            log.debug("Begin RSTR Element creation.");
        }

        int wstVersion;
        MessageContext inMsgCtx = null;
        OMElement rstrElem = null;
        OMElement appliesToEpr = null;

        wstVersion = data.getVersion();
        inMsgCtx = data.getInMessageContext();

        rstrElem = TrustUtil.createRequestSecurityTokenResponseElement(wstVersion, env.getBody());
        TrustUtil.createTokenTypeElement(wstVersion, rstrElem).setText(data.getTokenType());

        createDisplayToken(rstrElem, ipData);

        if (encryptedKey != null) {
            OMElement incomingAppliesToEpr = null;
            OMElement appliesToElem = null;
            int keysize = data.getKeysize();
            if (keysize == -1) {
                keysize = encryptedKey.getEphemeralKey().length * 8;
            }

            TrustUtil.createKeySizeElement(wstVersion, rstrElem, keysize);
            incomingAppliesToEpr = data.getAppliesToEpr();
            try {
                Document eprDoc = null;
                eprDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                        new ByteArrayInputStream(incomingAppliesToEpr.toString().getBytes()));
                appliesToEpr = (OMElement) doc.importNode(eprDoc.getDocumentElement(), true);
            } catch (Exception e) {
                throw new TrustException(TrustException.REQUEST_FAILED, e);
            }

            appliesToElem = rstrElem.getOMFactory().createOMElement(
                    new QName(RahasConstants.WSP_NS,
                            RahasConstants.IssuanceBindingLocalNames.APPLIES_TO,
                            RahasConstants.WSP_PREFIX), rstrElem);
            appliesToElem.addChild(appliesToEpr);
        }

        DateFormat zulu = null;
        OMElement reqSecTokenElem = null;
        Node assertionElement = null;
        Token assertionToken = null;

        // Use GMT time in milliseconds
        zulu = new XmlSchemaDateFormat();

        // Add the Lifetime element
        TrustUtil.createLifetimeElement(wstVersion, rstrElem, zulu.format(notBefore), zulu
                .format(notAfter));

        reqSecTokenElem = TrustUtil.createRequestedSecurityTokenElement(wstVersion, rstrElem);
        assertionElement = doc.importNode(assertionElem, true);
        reqSecTokenElem.addChild((OMNode) assertionElement);

        if (log.isDebugEnabled()) {
            log.debug(assertionElement.toString());
        }

        if (encryptedKey != null) {
            encryptSAMLAssertion(doc, (Element) assertionElement, encryptedKey);
        }

        createAttachedRef(rstrElem, assertionId);
        createUnattachedRef(rstrElem, assertionId);

        // Store the Token
        assertionToken = new Token(assertionId, (OMElement) doc.importNode(assertionElem, true),
                notBefore, notAfter);

        // At this point we definitely have the secret
        // Otherwise it should fail with an exception earlier
        assertionToken.setSecret(data.getEphmeralKey());
        TrustUtil.getTokenStore(inMsgCtx).add(assertionToken);

        // Creating the ReqProoftoken - END
        if (log.isDebugEnabled()) {
            log.debug("RSTR Elem created.");
        }

        return rstrElem;
    }

    /**
     * Create and add wst:AttachedReference element
     * 
     * @param rstrElem wst:RequestSecurityToken element
     * @param id Token identifier
     */
    protected void createAttachedRef(OMElement rstrElem, String id) {
        OMFactory fac = null;
        OMElement rar = null;
        OMElement str = null;
        OMElement ki = null;

        fac = rstrElem.getOMFactory();
        rar = fac.createOMElement(new QName(RahasConstants.WST_NS_05_02,
                RahasConstants.IssuanceBindingLocalNames.REQUESTED_ATTACHED_REFERENCE,
                RahasConstants.WST_PREFIX), rstrElem);
        str = fac.createOMElement(new QName(WSConstants.WSSE_NS,
                SecurityTokenReference.SECURITY_TOKEN_REFERENCE, WSConstants.WSSE_PREFIX), rar);
        ki = fac.createOMElement(new QName(WSConstants.WSSE_NS, "KeyIdentifier",
                WSConstants.WSSE_PREFIX), str);
        ki.addAttribute("ValueType", WSS_SAML_NS + WSConstants.SAML_ASSERTION_ID, null);
        ki.setText(id);
    }

    /**
     * Create and add wst:UnattachedReference element
     * 
     * @param rstrElem wst:RequestSecurityToken element
     * @param id Token identifier
     */
    protected void createUnattachedRef(OMElement rstrElem, String id) {
        OMFactory fac = null;
        OMElement rar = null;
        OMElement str = null;
        OMElement ki = null;

        fac = rstrElem.getOMFactory();
        rar = fac.createOMElement(new QName(RahasConstants.WST_NS_05_02,
                RahasConstants.IssuanceBindingLocalNames.REQUESTED_UNATTACHED_REFERENCE,
                RahasConstants.WST_PREFIX), rstrElem);
        str = fac.createOMElement(new QName(WSConstants.WSSE_NS,
                SecurityTokenReference.SECURITY_TOKEN_REFERENCE, WSConstants.WSSE_PREFIX), rar);
        ki = fac.createOMElement(new QName(WSConstants.WSSE_NS, "KeyIdentifier",
                WSConstants.WSSE_PREFIX), str);

        ki.addAttribute("ValueType", WSS_SAML_NS + WSConstants.SAML_ASSERTION_ID, null);
        ki.setText(id);
    }

    /**
     * Create the DisplayToken element according to CardSpace specifications.
     * 
     * @param rstrElem Information from the WS-Trust request.
     * @param ipData CardSpace specific meta-data for this issuance.
     * @return The DisplayToken element.
     */
    protected OMElement createDisplayToken(OMElement rstrElem, GenericIdentityProviderData ipData)
            throws IdentityProviderException {
        return null;
    }

    /**
     * 
     * @param rahasData
     * @return
     * @throws Exception
     */
    protected GenericIdentityProviderData getIdentityProviderData(RahasData rahasData)
            throws Exception {
        return new GenericIdentityProviderData(rahasData);
    }

    /**
     * 
     * @param ipData
     * @param rahasData
     * @param notBefore
     * @param notAfter
     * @param assertionId
     * @return
     * @throws IdentityProviderException
     */
    protected Element createSAMLAssertionAsDOM(GenericIdentityProviderData ipData,
            RahasData rahasData, DateTime notBefore, DateTime notAfter, String assertionId)
            throws IdentityProviderException {

        Element elem = null;
        SAMLTokenBuilder builder = null;
        final String requiredTokenType = ipData.getRequiredTokenType();
        if (requiredTokenType.equals(IdentityConstants.SAML10_URL)
                || requiredTokenType.equals(IdentityConstants.SAML11_URL)) {
            builder = new SAML1TokenBuilder();
        } else if (requiredTokenType.equals(IdentityConstants.SAML20_URL)) {
            builder = new SAML2TokenBuilder();
        }

        SAMLTokenDirector director = new SAMLTokenDirector(builder, rahasData, ipData);
        elem = director.createSAMLToken(notBefore, notAfter, assertionId);

        return elem;
    }

    /**
     * 
     * @param data
     * @return
     * @throws IdentityProviderException
     */
    protected boolean checkIsValidTokenType(GenericIdentityProviderData data)
            throws IdentityProviderException {
        boolean isValid = false;
        String type = data.getRequiredTokenType();
        IdentityPersistenceManager admin = null;
        String types = null;
        String[] arrTypes = null;

        try {
            admin = IdentityPersistenceManager.getPersistanceManager();
            types = admin.getParameterValue(IdentityTenantUtil.getRegistry(null, data
                    .getUserIdentifier()), IdentityConstants.PARAM_SUPPORTED_TOKEN_TYPES);
        } catch (IdentityException e) {
            throw new IdentityProviderException(e.getMessage(), e);
        }

        arrTypes = types.split(",");

        for (int i = 0; i < arrTypes.length; i++) {
            if (arrTypes[i].equals(type)) {
                isValid = true;
                break;
            }
        }
        return isValid;
    }

    /**
     * Encrypt the given SAML Assertion element with the given key information.
     * 
     * @param doc
     * @param assertionElement
     * @param encryptedKey
     */
    private void encryptSAMLAssertion(Document doc, Element assertionElement,
            WSSecEncryptedKey encryptedKey) throws TrustException {
        XMLCipher xmlCipher = null;
        SecretKey secretKey = null;
        String xencEncryptedDataId = null;
        KeyInfo keyInfo = null;
        EncryptedData encData = null;
        try {
            xmlCipher = XMLCipher.getInstance(WSConstants.AES_256);
            secretKey = WSSecurityUtil.prepareSecretKey(WSConstants.AES_256, encryptedKey
                    .getEphemeralKey());
            xmlCipher.init(XMLCipher.ENCRYPT_MODE, secretKey);
            xencEncryptedDataId = "EncDataId-" + assertionElement.hashCode();

            keyInfo = new KeyInfo(doc);
            keyInfo.addUnknownElement(encryptedKey.getEncryptedKeyElement());

            encData = xmlCipher.getEncryptedData();
            encData.setId(xencEncryptedDataId);
            encData.setKeyInfo(keyInfo);
            xmlCipher.doFinal(doc, assertionElement, false);
        } catch (Exception e) {
            throw new TrustException(TrustException.REQUEST_FAILED, e);
        }
    }
}
