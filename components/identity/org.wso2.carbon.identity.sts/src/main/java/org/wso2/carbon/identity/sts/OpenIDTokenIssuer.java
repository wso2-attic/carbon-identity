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

package org.wso2.carbon.identity.sts;

import java.io.ByteArrayInputStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.rahas.RahasConstants;
import org.apache.rahas.RahasData;
import org.apache.rahas.TrustException;
import org.apache.rahas.TrustUtil;
import org.apache.ws.security.message.WSSecEncryptedKey;
import org.apache.ws.security.util.XmlSchemaDateFormat;
import org.joda.time.DateTime;
import org.openid4java.message.Message;
import org.openid4java.message.MessageException;
import org.openid4java.message.Parameter;
import org.openid4java.message.ParameterList;
import org.opensaml.SAMLException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.wso2.carbon.core.util.AnonymousSessionUtil;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.provider.GenericIdentityProviderData;
import org.wso2.carbon.identity.provider.IdentityProviderData;
import org.wso2.carbon.identity.provider.IdentityProviderException;
import org.wso2.carbon.identity.provider.IdentityProviderUtil;
import org.wso2.carbon.identity.provider.RequestedClaimData;
import org.wso2.carbon.identity.provider.openid.OpenIDProvider;
import org.wso2.carbon.identity.provider.openid.infocard.OpenIDInfoCardHeader;
import org.wso2.carbon.identity.provider.openid.infocard.OpenIDInfoCardProviderData;
import org.wso2.carbon.identity.provider.openid.infocard.OpenIDInfoCardToken;
import org.wso2.carbon.identity.provider.openid.infocard.OpenIDInfoCardProviderData.OpenIDRequestedClaimData;

public class OpenIDTokenIssuer extends IdentityTokenIssuer {

    private String appliesTo;
    private static Log log = LogFactory.getLog(OpenIDTokenIssuer.class);

    /**
     * Overrides the base functionality to cater OpenID related functionality.
     */
    public SOAPEnvelope issue(RahasData data) throws TrustException {
        appliesTo = data.getAppliesToAddress();
        return super.issue(data);
    }
    
    /**
     * Override this method from the base class : we don't need SAML :)
     */
    protected Element createSAMLAssertionAsDOM(GenericIdentityProviderData ipData, RahasData rahasData,
            DateTime notBefore, DateTime notAfter, String assertionId)
            throws IdentityProviderException {
        return null;
    }

    /**
     * Overrides the base functionality to cater OpenID related functionality.
     */
    protected OMElement createRSTR(RahasData data, Date notBefore, Date notAfter, SOAPEnvelope env,
            Document doc, Node assertionElem, String assertionId, WSSecEncryptedKey encryptedKey)
            throws TrustException, SAMLException, IdentityProviderException {
        OMElement rstrElem = null;
        int wstVersion;
        OMElement appliesToEpr = null;
        DateFormat zulu = null;
        OMElement reqSecTokenElem = null;

        wstVersion = data.getVersion();
        rstrElem = TrustUtil.createRequestSecurityTokenResponseElement(wstVersion, env.getBody());
        TrustUtil.createTokenTypeElement(wstVersion, rstrElem).setText(data.getTokenType());
        createDisplayToken(rstrElem, ipData);

        if (log.isDebugEnabled()) {
            log.debug("Display token for OpenID Information card, created successfully");
        }

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

        // Use GMT time in milliseconds
        zulu = new XmlSchemaDateFormat();

        // Add the Lifetime element
        TrustUtil.createLifetimeElement(wstVersion, rstrElem, zulu.format(notBefore), zulu
                .format(notAfter));
        reqSecTokenElem = TrustUtil.createRequestedSecurityTokenElement(wstVersion, rstrElem);
        createOpenIdToken(reqSecTokenElem);
        createAttachedRef(rstrElem, assertionId);
        createUnattachedRef(rstrElem, assertionId);

        if (log.isDebugEnabled())
            log.debug("RSTR for OpenID Information card, created successfully");

        return rstrElem;
    }

    /**
     * Creates an OpenID token.
     * 
     * @param rstrElem RSTR token
     * @return OpenID token
     * @throws MessageException
     */
    protected OMElement createOpenIdToken(OMElement rstrElem) throws IdentityProviderException {
        OMElement rdt = null;
        OpenIDInfoCardToken token = null;
        Message message = null;
        ParameterList params = null;
        String claimID = null;
        OpenIDInfoCardHeader header = null;

        rdt = IdentityProviderUtil.createOpenIdToken(rstrElem, ipData);
        header = new OpenIDInfoCardHeader(OpenIDProvider.getInstance().getManager());
        claimID = ((RequestedClaimData) ipData.getRequestedClaims().get(
                IdentityConstants.CLAIM_OPENID)).getValue();
        params = header
                .buildHeader(claimID, OpenIDProvider.getInstance().getOpAddress(), appliesTo);
        setAttributeExchangeParams(params);

        try {
            message = Message.createMessage(params);
        } catch (MessageException e) {
            log.error(e.getMessage());
            throw new IdentityProviderException(e.getMessage(), e);
        }

        token = new OpenIDInfoCardToken(message);
        rdt.setText(token.getToken());
        if (log.isDebugEnabled())
            log.debug("OpenID token created successfully");

        return rdt;
    }

    /**
     * Set the attributes in the structure required by the Attribute Exchange.
     * 
     * @param params Parameter list
     */
    protected void setAttributeExchangeParams(ParameterList params) {
        Iterator<String> iterator = null;
        String key = null;
        OpenIDRequestedClaimData claim = null;

        params.set(new Parameter(IdentityConstants.OpenId.ExchangeAttributes.EXT,
                "http://openid.net/srv/ax/1.0-draft4"));

        params.set(new Parameter(IdentityConstants.OpenId.ExchangeAttributes.MODE,
                IdentityConstants.OpenId.ExchangeAttributes.FETCH_RESPONSE));

        iterator = ipData.getRequestedClaims().keySet().iterator();

        while (iterator.hasNext()) {
            key = iterator.next();
            claim = (OpenIDRequestedClaimData) ipData.getRequestedClaims().get(key);
            if (claim.openIDTag != null) {
                params.set(new Parameter(IdentityConstants.OpenId.ExchangeAttributes.TYPE
                        + claim.openIDTag, claim.getUri()));
                params.set(new Parameter(IdentityConstants.OpenId.ExchangeAttributes.VALUE
                        + claim.openIDTag, claim.getValue()));
            }
        }

        if (log.isDebugEnabled())
            log.debug("OpenID Ax parameters set successfully");
    }

    /**
     * Overrides the base functionality to cater OpenID related functionality.
     */
    protected IdentityProviderData getIdentityProviderData(RahasData rahasData) throws Exception {
        return new OpenIDInfoCardProviderData(rahasData);
    }

}