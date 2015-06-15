/*
 * Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.provider;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.impl.dom.factory.OMDOMFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.rahas.RahasData;
import org.apache.ws.security.WSConstants;
import org.apache.xml.security.utils.Constants;
import org.w3c.dom.Element;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.IdentityClaimManager;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.claim.Claim;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class GenericIdentityProviderData {

    public static final String USERMAN_SERVICE = "UserManServiceURL";
    public static final String USER_CLASS = "UserClass";
    private static final Log log = LogFactory.getLog(GenericIdentityProviderData.class);
    protected String cardID = null;
    protected Map<String, RequestedClaimData> requestedClaims = new HashMap<String, RequestedClaimData>();
    protected Map<String, Claim> supportedClaims = new HashMap<String, Claim>();
    protected String displayTokenLang = null;
    protected int authMechanism = -1;
    protected X509Certificate rpCert;
    protected String userIdentifier = null;
    protected String requiredTokenType = null;

    /**
     * Populate CardSpace specific meta-data.
     *
     * @param data WS-Trust information in the issue request.
     * @throws IdentityProviderException
     * @throws ClassNotFoundException
     */
    public GenericIdentityProviderData(RahasData data) throws IdentityProviderException, ClassNotFoundException {
        OMElement rstElem = null;
        OMElement claimElem = null;

        rstElem = data.getRstElement();
        claimElem = data.getClaimElem();
        readAuthenticationMechanism(data);
        processUserIdentifier(data);
        loadClaims();
        processClaimData(data, claimElem);
        processInfoCardReference(rstElem);
        readRequestedTokenType(data);
        populateClaimValues(data);
        extracAndValidatetRPCert(data);
    }

    public String getRequiredTokenType() {
        return requiredTokenType;
    }

    public void setRequiredTokenType(String requiredTokenType) {
        if (StringUtils.isBlank(requiredTokenType)) {
            this.requiredTokenType = getDefautTokenType();
        } else {
            this.requiredTokenType = requiredTokenType;
        }
    }

    /**
     * This parameter specifies the type of the token to be requested from the STS as a URI. This
     * parameter can be omitted if the STS and the Web site front-end have a mutual understanding
     * about what token type will be provided, or if the Web site is willing to accept any token
     * type.
     *
     * @return Default Token Type
     */
    public String getDefautTokenType() {
        return IdentityConstants.SAML10_URL;
    }

    public X509Certificate getRpCert() {
        return rpCert;
    }

    public String getUserIdentifier() {
        return null;
    }

    public String getCardID() {
        return null;
    }

    public String getDisplayTokenLang() {
        return null;
    }

    public String getDisplayName(String uri) {
        return null;
    }

    public String getTenantDomain() throws IdentityProviderException {
        return null;
    }

    /**
     * @throws IdentityProviderException
     */
    protected void loadClaims() throws IdentityProviderException {
        IdentityClaimManager claimManager = null;
        Claim[] claims = null;

        if (log.isDebugEnabled()) {
            log.debug("Loading claims");
        }

        try {
            claimManager = IdentityClaimManager.getInstance();
            claims = claimManager.getAllSupportedClaims(IdentityConstants.INFOCARD_DIALECT, IdentityTenantUtil
                    .getRealm(null, userIdentifier));
            for (int i = 0; i < claims.length; i++) {
                Claim temp = claims[i];
                supportedClaims.put(temp.getClaimUri(), temp);
            }
            Claim tenant = new Claim();
            tenant.setClaimUri(IdentityConstants.CLAIM_TENANT_DOMAIN);
            tenant.setDescription("Tenant");
            tenant.setDisplayTag("Tenant");
            tenant.setSupportedByDefault(true);
            tenant.setDialectURI("http://wso2.org");
            supportedClaims.put(tenant.getClaimUri(), tenant);
        } catch (IdentityException e) {
            log.error("Error while loading claims", e);
            throw new IdentityProviderException("Error while loading claims", e);
        }
    }

    /**
     * @param rahasData
     * @param claims
     * @throws IdentityProviderException
     */
    protected void processClaimData(RahasData rahasData, OMElement claims) throws IdentityProviderException {

        if (claims == null) {
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Processing claim data");
        }

        Iterator iterator = null;
        iterator = claims.getChildrenWithName(
                new QName(IdentityConstants.NS, IdentityConstants.LocalNames.IDENTITY_CLAIM_TYPE));

        while (iterator.hasNext()) {
            OMElement omElem = null;
            RequestedClaimData claim = null;
            String uriClaim = null;
            String optional = null;

            omElem = (OMElement) iterator.next();
            claim = getRequestedClaim();
            uriClaim = omElem.getAttributeValue(new QName(null, "Uri"));

            if (uriClaim == null) {
                log.error("Empty claim uri found while procession claim data");
                throw new IdentityProviderException(
                        "Empty claim uri found while procession claim data");
            }
            claim.setUri(uriClaim);
            optional = (omElem.getAttributeValue(new QName(null, "Optional")));
            if (StringUtils.isNotBlank(optional)) {
                claim.setBOptional("true".equals(optional));
            } else {
                claim.setBOptional(true);
            }

            requestedClaims.put(claim.getUri(), claim);
        }
    }

    /**
     * @param rst
     * @throws IdentityProviderException
     */
    protected void processInfoCardReference(OMElement rst) throws IdentityProviderException {
        // In the generic case we have nothing to do here.
    }

    /**
     * @param data
     * @throws IdentityProviderException
     */
    protected void readAuthenticationMechanism(RahasData data) throws IdentityProviderException {
        // In the generic case we have nothing to do here.
    }

    /**
     * Extract the relying party certificate and validate it.
     *
     * @param data Information in the RST extracted by Rahas.
     */
    protected void extracAndValidatetRPCert(RahasData data) throws IdentityProviderException {
        // In the generic case we have nothing to do here.
    }

    /**
     * Obtain the user identifier depending on the authentication mechanism used.
     *
     * @param rahasData
     */
    protected void processUserIdentifier(RahasData rahasData) throws IdentityProviderException {
        if (log.isDebugEnabled()) {
            log.debug("Processing user identifier");
        }
        userIdentifier = rahasData.getPrincipal().getName();
    }

    /**
     * @param rahasData
     * @throws IdentityProviderException
     */
    protected void populateClaimValues(RahasData rahasData) throws IdentityProviderException {
        UserStoreManager connector = null;

        if (log.isDebugEnabled()) {
            log.debug("Populating claim values");
        }

        try {
            connector = IdentityTenantUtil.getRealm(null, userIdentifier).getUserStoreManager();
        } catch (Exception e) {
            log.error("Error while instantiating IdentityUserStore", e);
            throw new IdentityProviderException("Error while instantiating IdentityUserStore", e);
        }

        // get the column names for the URIs
        Iterator<RequestedClaimData> ite = requestedClaims.values().iterator();
        List<String> claimList = new ArrayList<String>();

        while (ite.hasNext()) {
            RequestedClaimData claim = ite.next();
            if (claim != null && !claim.getUri().equals(IdentityConstants.CLAIM_PPID) &&
                !claim.getUri().equals(IdentityConstants.CLAIM_TENANT_DOMAIN)) {
                claimList.add(claim.getUri());
            }
        }
        String[] claims = new String[claimList.size()];
        String userId = MultitenantUtils.getTenantAwareUsername(userIdentifier);
        Map<String, String> mapValues = null;

        try {
            mapValues = connector.getUserClaimValues(userId, claimList.toArray(claims), null);
        } catch (Exception e) {
            throw new IdentityProviderException(e.getMessage(), e);
        }

        ite = requestedClaims.values().iterator();
        while (ite.hasNext()) {
            RequestedClaimData claimData = ite.next();
            if (IdentityConstants.CLAIM_TENANT_DOMAIN.equals(claimData.getUri())) {
                String domainName = null;
                domainName = MultitenantUtils.getTenantDomain(userIdentifier);
                if (domainName == null) {
                    domainName = IdentityConstants.DEFAULT_SUPER_TENAT;
                }
                claimData.setValue(domainName);
            } else {
                claimData.setValue(mapValues.get(claimData.getUri()));
            }
        }
    }

    /**
     * Validate the given ds:KeyInfo element against the stored ds:KeyInfo element.
     *
     * @param issuerInfo Stored ds:KeyInfo element as a <code>java.lang.String</code>.
     * @param keyInfo    The incoming ds:KeyInfo element as a <code>org.w3c.dom.Element</code>.
     * @return true if the information matches, otherwise false.
     */
    protected boolean validateKeyInfo(String issuerInfo, Element keyInfo) throws IdentityProviderException {

        if (log.isDebugEnabled()) {
            log.debug("Validating key info");
        }

        try {
            OMElement elem = new StAXOMBuilder(new ByteArrayInputStream(issuerInfo.getBytes())).getDocumentElement();

            OMElement keyValueElem = elem.getFirstElement();
            if (keyValueElem != null &&
                keyValueElem.getQName().equals(new QName(WSConstants.SIG_NS, Constants._TAG_KEYVALUE))) {
                // KeyValue structure : expect an RSAKeyValue
                OMElement rsaKeyValueElem = keyValueElem.getFirstElement();
                if (rsaKeyValueElem != null &&
                    rsaKeyValueElem.getQName().equals(new QName(WSConstants.SIG_NS, Constants._TAG_RSAKEYVALUE))) {
                    String modulus =
                            rsaKeyValueElem.getFirstChildWithName(new QName(WSConstants.SIG_NS, Constants._TAG_MODULUS))
                                           .getText().trim();
                    String exponent = rsaKeyValueElem
                            .getFirstChildWithName(new QName(WSConstants.SIG_NS, Constants._TAG_EXPONENT)).getText()
                            .trim();

                    // Now process the incoming element to check for ds:RSAKeyValue

                    OMElement receivedKeyInfoElem = (OMElement) new OMDOMFactory().getDocument().importNode(keyInfo,
                                                                                                            true);

                    OMElement receivedKeyValueElem = receivedKeyInfoElem.getFirstElement();
                    if (receivedKeyValueElem != null && receivedKeyValueElem.getQName()
                                                                            .equals(new QName(WSConstants.SIG_NS,
                                                                                              Constants._TAG_KEYVALUE))) {
                        OMElement receivedRsaKeyValueElem = receivedKeyValueElem.getFirstChildWithName(
                                new QName(WSConstants.SIG_NS, Constants._TAG_RSAKEYVALUE));
                        if (receivedRsaKeyValueElem != null) {
                            // Obtain incoming mod and exp
                            String receivedModulus = receivedRsaKeyValueElem
                                    .getFirstChildWithName(new QName(WSConstants.SIG_NS, Constants._TAG_MODULUS))
                                    .getText().trim();
                            String receivedExponent = receivedRsaKeyValueElem.getFirstChildWithName(
                                    new QName(WSConstants.SIG_NS, Constants._TAG_EXPONENT)).getText().trim();

                            // Compare
                            return modulus.equals(receivedModulus) && exponent.equals(receivedExponent);
                        } else {
                            log.error("Unknown received KeyInfo type");
                            throw new IdentityProviderException("Unknown received KeyInfo type");
                        }

                    } else {
                        log.error("Unknown received KeyInfo type");
                        throw new IdentityProviderException("Unknown received KeyInfo type");
                    }
                } else {
                    log.error("Error while instantiating IdentityUserStore");
                    throw new IdentityProviderException("Unknown received KeyInfo type");
                }
            } else {
                log.error("Unknown stored KeyInfo type");
                throw new IdentityProviderException("Unknown stored KeyInfo type");
            }
        } catch (XMLStreamException e) {
            log.error("Error parsing stored KeyInfo", e);
            throw new IdentityProviderException("Error parsing stored KeyInfo");
        }
    }

    protected void readRequestedTokenType(RahasData data) {
        requiredTokenType = data.getTokenType();
        if (requiredTokenType == null || requiredTokenType.trim().length() == 0) {
            requiredTokenType = getDefautTokenType();
        }
    }

    public Map<String, RequestedClaimData> getRequestedClaims() {
        return requestedClaims;
    }

    /**
     * @param requestedClaims
     */
    public void setRequestedClaims(Map<String, RequestedClaimData> requestedClaims) {
        this.requestedClaims = requestedClaims;
    }

    protected RequestedClaimData getRequestedClaim() {
        return new RequestedClaimData();
    }

}
