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

package org.wso2.carbon.identity.relyingparty.ui.openid.extensions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openid4java.message.AuthSuccess;
import org.openid4java.message.MessageException;
import org.openid4java.message.MessageExtension;
import org.openid4java.message.ax.FetchRequest;
import org.openid4java.message.ax.FetchResponse;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.relyingparty.stub.dto.ClaimDTO;
import org.wso2.carbon.identity.relyingparty.ui.openid.OpenIDAuthenticationRequest;
import org.wso2.carbon.identity.relyingparty.ui.openid.OpenIDAxAttribute;

import java.util.List;
import java.util.Map;

public class OpenIDAttributeExchange implements OpenIDExtension {

    private static Map<String, String> axMapping;
    private static Log log = LogFactory.getLog(OpenIDAttributeExchange.class);
    private AuthSuccess authSuccess;

    /**
     * Default constructor
     */
    public OpenIDAttributeExchange() {
    }

    /**
     * Constructed during building the response
     *
     * @param authSuccess An instance of AuthSuccess
     */
    public OpenIDAttributeExchange(AuthSuccess authSuccess) {
        this.authSuccess = authSuccess;
    }

    /**
     * Creates an instance of MessageExtension for the OpenID authentication request
     *
     * @param request OpenID authentication request
     * @return An instance of MessageExtension
     * @throws IdentityException
     */
    public MessageExtension getMessageExtension(OpenIDAuthenticationRequest request)
            throws IdentityException {

        FetchRequest fetchReq = null;
        OpenIDAxAttribute attr = null;

        try {
            fetchReq = FetchRequest.createFetchRequest();
            if (request != null && request.getRequiredClaims() != null
                    && request.getRequiredClaims().size() > 0) {
                for (Object requiredClaim : request.getRequiredClaims()) {
                    if (requiredClaim instanceof OpenIDAxAttribute) {
                        attr = (OpenIDAxAttribute) requiredClaim;
                        fetchReq.addAttribute(attr.getAttributeName(), attr.getNamespace(), true);
                    }
                }
            }
            if (request != null && request.getOptionalClaims() != null
                    && request.getOptionalClaims().size() > 0) {
                for (Object optionalClaim : request.getOptionalClaims()) {
                    if (optionalClaim instanceof OpenIDAxAttribute) {
                        attr = (OpenIDAxAttribute) optionalClaim;
                        fetchReq.addAttribute(attr.getAttributeName(), attr.getNamespace(), false);
                    }
                }
            }
        } catch (MessageException e) {
            log.error("Error while creating the message extension for OpenIDAttributeExchange", e);
            throw new IdentityException(
                    "Error while creating the message extension for OpenIDAttributeExchange", e);
        }

        return fetchReq;
    }

    /**
     * Set request attributes for OpenID attribute exchange
     *
     * @param request HttpServletRequest
     */
    public void setSessionAttributes(List<ClaimDTO> claimList) throws IdentityException {

        List aliases = null;
        List values = null;
        FetchResponse fetchReponse = null;
        Map attributeTypes = null;
        ClaimDTO claim = null;

        try {

            if (authSuccess.hasExtension(FetchResponse.OPENID_NS_AX)) {
                fetchReponse = (FetchResponse) authSuccess.getExtension(FetchResponse.OPENID_NS_AX);
                aliases = fetchReponse.getAttributeAliases();
                attributeTypes = fetchReponse.getAttributeTypes();

                for (Object alias : aliases) {
                    values = fetchReponse.getAttributeValues((String) alias);
                    if (values != null && !values.isEmpty()) {
                        claim = new ClaimDTO();
                        claim.setClaimUri((String) attributeTypes.get(alias));
                        claim.setClaimValue((String) values.get(0));
                        claimList.add(claim);
                    }
                }
            } else if (authSuccess.hasExtension(IdentityConstants.OpenId.ExchangeAttributes.NS_AX)) {
                fetchReponse = (FetchResponse) authSuccess
                        .getExtension(IdentityConstants.OpenId.ExchangeAttributes.NS_AX);
                aliases = fetchReponse.getAttributeAliases();
                attributeTypes = fetchReponse.getAttributeTypes();

                for (Object alias : aliases) {
                    values = fetchReponse.getAttributeValues((String) alias);
                    if (values != null && !values.isEmpty()) {
                        claim = new ClaimDTO();
                        claim.setClaimUri((String) attributeTypes.get(alias));
                        claim.setClaimValue((String) values.get(0));
                        claimList.add(claim);
                    }
                }
            }
        } catch (MessageException e) {
            log
                    .error(
                            "Error while adding retrieved user attributes to the session in OpenIDAttributeExchange",
                            e);
            throw new IdentityException(
                    "Error while adding retrieved user attributes to the session in OpenIDAttributeExchange",
                    e);
        }
    }
}