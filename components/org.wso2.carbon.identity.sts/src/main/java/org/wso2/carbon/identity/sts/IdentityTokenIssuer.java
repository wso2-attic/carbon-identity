/*                                                                             
 * Copyright 2005,2006 WSO2, Inc. http://www.wso2.org
 *                                                                             
 * Licensed under the Apache License, Version 2.0 (the "License");             
 * you may not use this file except in compliance with the License.            
 * You may obtain a copy of the License at                                     
 *                                                                             
 *      http://www.apache.org/licenses/LICENSE-2.0                             
 *                                                                             
 * Unless required by applicable law or agreed to in writing, software         
 * distributed under the License is distributed on an "AS IS" BASIS,           
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.    
 * See the License for the specific language governing permissions and         
 * limitations under the License.                                              
 */

package org.wso2.carbon.identity.sts;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.rahas.RahasData;
import org.apache.rahas.TrustException;
import org.wso2.carbon.core.util.AnonymousSessionUtil;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.model.InfoCardDO;
import org.wso2.carbon.identity.core.persistence.IdentityPersistenceManager;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.provider.GenericIdentityProviderData;
import org.wso2.carbon.identity.provider.IdentityProviderData;
import org.wso2.carbon.identity.provider.IdentityProviderException;
import org.wso2.carbon.identity.provider.IdentityProviderUtil;
import org.wso2.carbon.identity.provider.RequestedClaimData;
import org.wso2.carbon.identity.sts.internal.IdentitySTSServiceComponent;
import org.wso2.carbon.registry.core.Registry;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;

/**
 * This is the main token issuer implementation identity provider. Two types of credential
 * mechanisms are supported: User name and Password Credential / Self-issued Token Credential
 * 
 * Supported list of claims can be configured and the values of those supported claims will be
 * obtained from the user store setup to work with the identity provider.
 */
public class IdentityTokenIssuer extends GenericTokenIssuer {
    public final static String ISSUER_SELF = IdentityConstants.NS + "/issuer/self";
    private static Log log = LogFactory.getLog(IdentityTokenIssuer.class);
    private static Log tokenIssuerLog = LogFactory.getLog(IdentityConstants.TOKEN_ISSUSER_LOG);
    private boolean isTokenLogDebug = false;

    public IdentityTokenIssuer() {
        isTokenLogDebug = tokenIssuerLog.isDebugEnabled();
    }

    /**
     * {@inheritDoc}
     */
    public SOAPEnvelope issue(RahasData data) throws TrustException {
        boolean isValidCard = false;
        try {
            if (isTokenLogDebug) {
                log.debug("Request: \n" + data.getRstElement().toString() + "\n\n");
            }

            ipData = getIdentityProviderData(data);
            if(data.getPrincipal() == null){
                log.error("Principle is null");
            }
            
            String username = data.getPrincipal().getName();
            if(log.isDebugEnabled()){
                log.debug("Recieved " + username);
            }
            Registry registry = AnonymousSessionUtil.getSystemRegistryByUserName(
                    IdentitySTSServiceComponent.getRegistryService(), IdentitySTSServiceComponent
                            .getRealmService(), username);
            isValidCard = isValidCard(ipData.getCardID(), registry);

            if (log.isDebugEnabled()) {
                log.debug("Card is validated");
            }
            if (isValidCard == false) {
                String msg = "Invalid information card";
                log.error(msg);
                throw new TrustException(TrustException.REQUEST_FAILED);
            }
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
     * {@inheritDoc}
     */
    protected SOAPEnvelope createResponse(RahasData rahasData, Registry registry) throws TrustException {
        SOAPEnvelope envelope = null;
        try {
            envelope = super.createResponse(rahasData);
            log.info("Token issued for info card");            
            return envelope;
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new TrustException(TrustException.REQUEST_FAILED, e);
        } finally {
            log.info("Response ready for : " + ipData.getCardID());
        }
    }

    /**
     * Create the DisplayToken element according to CardSpace specifications.
     * 
     * @param rahasData Information from the WS-Trust request.
     * @param ipData CardSpace specific meta-data for this issuance.
     * @return The DisplayToken element.
     */
    protected OMElement createDisplayToken(OMElement rstrElem, GenericIdentityProviderData ipData)
            throws IdentityProviderException {
        if (log.isDebugEnabled()) {
            log.debug("Begin Display token creation.");
        }

        Map<String, RequestedClaimData> requestedClaims = null;
        OMElement rdt = null;
        OMElement displayToken = null;
        Iterator<RequestedClaimData> iterator = null;
        requestedClaims = ipData.getRequestedClaims();

        if (requestedClaims.isEmpty())
            return null;

        rdt = IdentityProviderUtil.createRequestedDisplayToken(rstrElem, ipData);
        displayToken = IdentityProviderUtil.createDisplayToken(rdt, ipData);

        try {
            iterator = requestedClaims.values().iterator();
            while (iterator.hasNext()) {
                RequestedClaimData claim = null;
                claim = iterator.next();
                if (claim.getUri().equals(IdentityConstants.CLAIM_PPID)) {
                    // PPID display token
                    IdentityProviderUtil.createDisplayClaim(displayToken, ipData
                            .getDisplayName(claim.getUri()), IdentityUtil.getPPIDDisplayValue(claim
                            .getValue()), claim.getUri());
                } else {
                    IdentityProviderUtil.createDisplayClaim(displayToken, ipData
                            .getDisplayName(claim.getUri()), claim.getValue(), claim.getUri());
                }
            }
        } catch (Exception e) {
            log.error("Error occured while creating the display token", e);
            throw new IdentityProviderException("Error occured while creating the display token", e);
        }
        if (log.isDebugEnabled()) {
            log.debug("createDisplayToken");
        }
        return rdt;
    }

    /**
     * {@inheritDoc}
     */
    protected IdentityProviderData getIdentityProviderData(RahasData rahasData) throws Exception {
        return new IdentityProviderData(rahasData);
    }

    /**
     * Check whether the information card referenced in the token request is a valid managed
     * information card issued by the identity provider.
     * 
     * @param cardId Identifier of the information card.
     * @return Whether the card is valid or not.
     * @throws IdentityProviderException
     */
    private boolean isValidCard(String cardId, Registry registry) throws IdentityProviderException {
        if (log.isDebugEnabled()) {
            log.debug("Begin Validating card.");
        }
        boolean retval = false;
        IdentityPersistenceManager dbman = null;
        InfoCardDO card = null;

        try {
            dbman = IdentityPersistenceManager.getPersistanceManager();
            card = dbman.getInfoCard(registry, cardId);
        } catch (IdentityException e) {
            throw new IdentityProviderException(e.getMessage(), e);
        }

        if (card != null) {
            Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
            Date now = cal.getTime();
            if (now.before(card.getDateExpires()) && now.after(card.getDateIssued())) {
                retval = true;
            }
        }

        return retval;
    }

}
