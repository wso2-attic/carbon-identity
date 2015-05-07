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
import org.openid4java.message.pape.PapeMessage;
import org.openid4java.message.pape.PapeRequest;
import org.openid4java.message.pape.PapeResponse;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.relyingparty.stub.dto.ClaimDTO;
import org.wso2.carbon.identity.relyingparty.ui.openid.AuthPolicyType;
import org.wso2.carbon.identity.relyingparty.ui.openid.OpenIDAuthenticationRequest;

import java.util.List;

public class OpenIDPape implements OpenIDExtension {

    private static Log log = LogFactory.getLog(OpenIDPape.class);
    private AuthSuccess authSuccess;

    /**
     * Default constructor
     */
    public OpenIDPape() {

    }

    /**
     * Constructed during building the response
     *
     * @param authSuccess An instance of AuthSuccess
     */
    public OpenIDPape(AuthSuccess authSuccess) {
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

        PapeRequest papeReq = null;

        papeReq = PapeRequest.createPapeRequest();

        for (AuthPolicyType authType : request.getAuthTypes()) {
            switch (authType) {
                case PAPE_POLICY_MULTI_FACTOR:
                    papeReq.addPreferredAuthPolicy(PapeMessage.PAPE_POLICY_MULTI_FACTOR);
                    break;
                case PAPE_POLICY_MULTI_FACTOR_PHYSICAL:
                    papeReq.addPreferredAuthPolicy(PapeMessage.PAPE_POLICY_MULTI_FACTOR_PHYSICAL);
                    break;
                case PAPE_POLICY_PHISHING_RESISTANT:
                    papeReq.addPreferredAuthPolicy(PapeMessage.PAPE_POLICY_PHISHING_RESISTANT);
                    break;
                default:
                    break;
            }
        }

        return papeReq;
    }

    /**
     * Set session attributes with the received OpenID response
     *
     * @param request HttpServletRequest
     * @throws RelyingPartyException
     */
    public void setSessionAttributes(List<ClaimDTO> claimList) throws IdentityException {
        try {

            PapeResponse papeResponse = null;

            if (authSuccess.hasExtension(PapeResponse.OPENID_NS_PAPE)) {
                papeResponse = (PapeResponse) authSuccess.getExtension(PapeResponse.OPENID_NS_PAPE);
                if (papeResponse != null) {
                    //TODO
                    /*
					 * request.setAttribute(IdentityConstants.OpenId.PapeAttributes.AUTH_POLICIES,
					 * papeResponse.getAuthPolicies());
					 * request.setAttribute(IdentityConstants.OpenId.PapeAttributes.AUTH_AGE,
					 * papeResponse.getAuthAge());
					 * request.setAttribute(IdentityConstants.OpenId.PapeAttributes.NIST_AUTH_LEVEL,
					 * papeResponse.getNistAuthLevel());
					 */
                }
            }
        } catch (MessageException e) {
            log.error("Error while adding retrieved user attributes to the session in OpenIDPape",
                    e);
            throw new IdentityException(
                    "Error while adding retrieved user attributes to the session in OpenIDPape", e);
        }
    }
}