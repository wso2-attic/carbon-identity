/*
 * Copyright (c) 2005-2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.provider.openid.extensions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.MessageException;
import org.openid4java.message.MessageExtension;
import org.openid4java.message.pape.PapeMessage;
import org.openid4java.message.pape.PapeRequest;
import org.openid4java.message.pape.PapeResponse;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.provider.dto.OpenIDAuthRequestDTO;
import org.wso2.carbon.identity.provider.dto.OpenIDParameterDTO;
import org.wso2.carbon.identity.provider.openid.handlers.OpenIDAuthenticationRequest;

import java.util.List;

/**
 * Functionality related to OpenID Provider Authentication Policy Extension. This extension to the
 * OpenID Authentication protocol provides means for a Relying Party to request previously agreed
 * upon authentication policies be applied by the OpenID Provider and for an OpenID Provider to
 * inform a Relying Party what authentication policies were used. Thus a Relying Party can request
 * the End User authenticate, for example, by means which are resistant to common phishing attacks
 * or that provide for multi-factor authentication. Likewise, the OpenID Provider is able to convey
 * to the Relying Party that the End User either met or did not meet the requirements of the
 * requested policy, or policies, in the OpenID Authentication response message as well as the
 * general strength of the credential(s) being used.
 */
public class OpenIDPape extends OpenIDExtension {

    private static final Log log = LogFactory.getLog(OpenIDPape.class);
    private OpenIDAuthenticationRequest request;

    /**
     * @param request An instance of OpenIDAuthenticationRequest
     */
    public OpenIDPape(OpenIDAuthenticationRequest request) throws IdentityException {
        if (request == null) {
            log.debug("Request cannot be null while initializing OpenIDPape");
            throw IdentityException.error("Request cannot be null while initializing OpenIDPape");
        }
        this.request = request;
    }

    /**
     * @param authRequest OpenID authentication request
     * @return A set of policies requested
     * @throws IdentityException
     */
    public static String[] getAuthenticationPolicies(AuthRequest authRequest) throws IdentityException {

        MessageExtension message = null;
        PapeRequest papeRequest = null;
        List preferredPolicies = null;

        try {
            if (authRequest.hasExtension(PapeMessage.OPENID_NS_PAPE)) {
                message = authRequest.getExtension(PapeMessage.OPENID_NS_PAPE);

                if (message instanceof PapeRequest) {
                    papeRequest = (PapeRequest) message;
                    preferredPolicies = papeRequest.getPreferredAuthPoliciesList();
                    if (preferredPolicies != null && !preferredPolicies.isEmpty()) {
                        return (String[]) preferredPolicies.toArray(new String[preferredPolicies.size()]);
                    }
                }
            }
            return new String[0];
        } catch (MessageException e) {
            throw IdentityException.error("Failed retrieve authentication policies", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addRequiredAttributes(List<String> requiredAttributes) throws IdentityException {
        // Nothing to implement
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageExtension getMessageExtension(String userId, String profileName, OpenIDAuthRequestDTO requestDTO)
            throws IdentityException {

        MessageExtension message = null;
        PapeResponse papeResponse = null;
        AuthRequest authRequest = null;

        try {
            authRequest = request.getAuthRequest();

            if (authRequest != null) {
                message = authRequest.getExtension(PapeMessage.OPENID_NS_PAPE);

                if (message instanceof PapeRequest) {
                    papeResponse = PapeResponse.createPapeResponse();
                    if (request.isPhishingResistanceLogin()) {
                        papeResponse.addAuthPolicy(PapeMessage.PAPE_POLICY_PHISHING_RESISTANT);
                        //papeResponse.setNistAuthLevel(1);  TODO
                    }
                    if (request.isMultifactorLogin()) {
                        papeResponse.addAuthPolicy(PapeMessage.PAPE_POLICY_MULTI_FACTOR);
                        //papeResponse.setNistAuthLevel(2);  TODO
                    }
                }
            }
        } catch (MessageException e) {
            log.error("Failed to create message extension for PAPE", e);
            throw IdentityException.error("Failed to create message extension for PAPE", e);
        }

        return papeResponse;
    }

    public OpenIDParameterDTO[] getPapeInfoFromRequest() {

        OpenIDParameterDTO[] policySet = new OpenIDParameterDTO[4];

        for (int i = 0; i < policySet.length; i++) {
            policySet[i] = new OpenIDParameterDTO();
        }

        policySet[0].setName(IdentityConstants.OpenId.PapeAttributes.PHISHING_RESISTANCE);
        policySet[0].setValue("false");

        policySet[1].setName(IdentityConstants.OpenId.PapeAttributes.MULTI_FACTOR);
        policySet[1].setValue("false");

        policySet[2].setName(IdentityConstants.OpenId.PapeAttributes.INFOCARD_BASED_MULTIFACTOR_AUTH);
        policySet[2].setValue("false");

        policySet[3].setName(IdentityConstants.OpenId.PapeAttributes.XMPP_BASED_MULTIFACTOR_AUTH);
        policySet[3].setValue("false");

        if (request.isPhishingResistanceLogin()) {
            policySet[0].setValue("true");
        }

        if (request.isMultifactorLogin()) {
            policySet[1].setValue("true");
        }

        return policySet;
    }

}