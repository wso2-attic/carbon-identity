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
import org.openid4java.message.sreg.SRegRequest;
import org.openid4java.message.sreg.SRegResponse;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.relyingparty.stub.dto.ClaimDTO;
import org.wso2.carbon.identity.relyingparty.ui.openid.OpenIDAuthenticationRequest;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class OpenIDSimpleReg implements OpenIDExtension {

    private static Log log = LogFactory.getLog(OpenIDSimpleReg.class);
    private AuthSuccess authSuccess;

    /**
     * Default constructor
     */
    public OpenIDSimpleReg() {
    }

    /**
     * Constructed during building the response
     *
     * @param authSuccess An instance of AuthSuccess
     */
    public OpenIDSimpleReg(AuthSuccess authSuccess) {
        this.authSuccess = authSuccess;
    }

    /**
     * Creates an instance of MessageExtension for the OpenID authentication request
     *
     * @param request OpenID authentication request
     * @return An instance of MessageExtension
     * @throws RelyingPartyException
     */
    public MessageExtension getMessageExtension(OpenIDAuthenticationRequest request)
            throws IdentityException {

        SRegRequest sregReq = null;

        sregReq = SRegRequest.createFetchRequest();

        if (request.getRequiredClaims() != null && request.getRequiredClaims().size() > 0) {
            for (Object requiredClaim : request.getRequiredClaims()) {
                if (requiredClaim instanceof String) {
                    sregReq.addAttribute((String) requiredClaim, true);
                }
            }
        }

        if (request.getOptionalClaims() != null && request.getOptionalClaims().size() > 0) {
            for (Object optionalClaim : request.getOptionalClaims()) {
                if (optionalClaim instanceof String) {
                    sregReq.addAttribute((String) optionalClaim, false);
                }
            }
        }

        return sregReq;
    }

    /**
     * Set request attributes for OpenID simple registration
     *
     * @param request HttpServletRequest
     */
    public void setSessionAttributes(List<ClaimDTO> claimList) throws IdentityException {

        try {

            SRegResponse sregResp = null;
            Iterator iterator = null;
            Map attributes = null;
            Entry entry = null;

            if (authSuccess.hasExtension(SRegResponse.OPENID_NS_SREG)) {
                sregResp = (SRegResponse) authSuccess.getExtension(SRegResponse.OPENID_NS_SREG);
            } else if (authSuccess
                    .hasExtension(IdentityConstants.OpenId.SimpleRegAttributes.NS_SREG)) {
                sregResp = (SRegResponse) authSuccess
                        .getExtension(IdentityConstants.OpenId.SimpleRegAttributes.NS_SREG);

            } else if (authSuccess
                    .hasExtension(IdentityConstants.OpenId.SimpleRegAttributes.NS_SREG_1)) {
                sregResp = (SRegResponse) authSuccess
                        .getExtension(IdentityConstants.OpenId.SimpleRegAttributes.NS_SREG_1);
            }

            if (sregResp != null) {
                ClaimDTO claim = null;
                attributes = sregResp.getAttributes();
                iterator = attributes.entrySet().iterator();
                while (iterator.hasNext()) {
                    entry = (Entry) iterator.next();
                    claim = new ClaimDTO();
                    claim.setClaimUri((String) entry.getKey());
                    claim.setClaimValue((String) entry.getValue());
                    claimList.add(claim);
                }
            }

        } catch (MessageException e) {
            log
                    .error(
                            "Error while adding retrieved user attributes to the session in OpenIDSimpleReg",
                            e);
            throw new IdentityException(
                    "Error while adding retrieved user attributes to the session in OpenIDSimpleReg",
                    e);
        }
    }

    /**
     * If no attribute set by the user for simple registration request, by default we set all the
     * attributes.
     *
     * @param request Simple registration request
     */
    protected void setDefaultRequestParams(SRegRequest request) {

    }

}