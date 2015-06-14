/*
 * Copyright (c) 2005-2006, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.rahas.RahasData;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.handler.WSHandlerResult;
import org.opensaml.SAMLAssertion;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.user.core.claim.Claim;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.xml.namespace.QName;
import java.util.Vector;

/**
 * Meta-data collection of related to CardSpace required for token issuance.
 */
public class IdentityProviderData extends GenericIdentityProviderData {
    private static final Log log = LogFactory.getLog(IdentityProviderData.class);
    private SAMLAssertion assertion;

    public IdentityProviderData(RahasData data) throws IdentityProviderException, ClassNotFoundException {
        super(data);
    }

    /**
     * @param rst
     * @throws IdentityProviderException
     */
    @Override
    protected void processInfoCardReference(OMElement rst) throws IdentityProviderException {
        OMElement infoCardRef = null;
        OMElement omCardID = null;

        if (log.isDebugEnabled()) {
            log.debug("Processing information card reference");
        }

        infoCardRef = rst.getFirstChildWithName(new QName(IdentityConstants.NS,
                                                          IdentityConstants.LocalNames.INFO_CARD_REFERENCE));

        omCardID = infoCardRef.getFirstChildWithName(new QName(IdentityConstants.NS,
                                                               IdentityConstants.LocalNames.CARD_ID));

        this.cardID = omCardID.getText();
    }

    /**
     * @param data
     * @throws IdentityProviderException
     */
    @Override
    protected void readAuthenticationMechanism(RahasData data) throws IdentityProviderException {
        MessageContext inContext = null;
        Vector results = null;

        if (log.isDebugEnabled()) {
            log.debug("Reading authentication mechanism");
        }

        inContext = data.getInMessageContext();

        if ((results = (Vector) inContext.getProperty(WSHandlerConstants.RECV_RESULTS)) == null) {
            log.error("Missing authentication mechanism");
            throw new IdentityProviderException("Missing authentication mechanism");
        } else {
            for (int i = 0; i < results.size(); i++) {
                WSHandlerResult rResult = (WSHandlerResult) results.get(i);
                Vector wsSecEngineResults = rResult.getResults();

                for (int j = 0; j < wsSecEngineResults.size(); j++) {
                    WSSecurityEngineResult wser = (WSSecurityEngineResult) wsSecEngineResults.get(j);
                    int action = ((Integer) wser.get(WSSecurityEngineResult.TAG_ACTION)).intValue();
                    if (action == WSConstants.ST_UNSIGNED) {

                        this.authMechanism = IdentityConstants.AUTH_TYPE_SELF_ISSUED;
                        this.assertion = (SAMLAssertion) wser.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
                    } else if (action == WSConstants.UT && wser.get(WSSecurityEngineResult.TAG_PRINCIPAL) != null) {
                        this.authMechanism = IdentityConstants.AUTH_TYPE_USERNAME_TOKEN;
                    }
                }
            }
        }
    }

    @Override
    public String getUserIdentifier() {
        return userIdentifier;
    }

    public void setUserIdentifier(String userIdentifier) {
        this.userIdentifier = userIdentifier;
    }

    @Override
    public String getTenantDomain() throws IdentityProviderException {
        if (this.authMechanism == IdentityConstants.AUTH_TYPE_SELF_ISSUED) { //only for tenant 0
            return null;
        }

        if (userIdentifier == null) {
            // auth type is not self issued and still the user identifier is null. 
            // this is a invalid case
            throw new IllegalStateException("User identifier must NOT be null");
        }

        String domain = null;
        domain = MultitenantUtils.getTenantDomain(userIdentifier);
        return domain;
    }

    /**
     * @param uri
     * @return
     */
    @Override
    public String getDisplayName(String uri) {
        Claim claim = null;

        if (log.isDebugEnabled()) {
            log.debug("");
        }

        claim = supportedClaims.get(uri);
        if (claim != null) {
            if (IdentityConstants.CLAIM_PPID.equals(claim.getClaimUri())) {
                return IdentityConstants.PPID_DISPLAY_VALUE;
            }
            return claim.getDisplayTag();
        }
        return null;
    }

    @Override
    public String getCardID() {
        return cardID;
    }

    @Override
    public String getDisplayTokenLang() {
        return displayTokenLang;
    }

}