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
import org.openid4java.message.ax.AxMessage;
import org.openid4java.message.sreg.SRegMessage;
import org.openid4java.message.sreg.SRegRequest;
import org.openid4java.message.sreg.SRegResponse;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.provider.dto.OpenIDAuthRequestDTO;
import org.wso2.carbon.identity.provider.dto.OpenIDClaimDTO;
import org.wso2.carbon.identity.provider.openid.handlers.OpenIDAuthenticationRequest;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Functionality related OpenID Simple Registration. OpenID Simple Registration is an extension to
 * the OpenID Authentication protocol that allows for very light-weight profile exchange. It is
 * designed to pass eight commonly requested pieces of information when an End User goes to register
 * a new account with a web service.
 */
public class OpenIDSimpleReg extends OpenIDExtension {

    private static final Log log = LogFactory.getLog(OpenIDSimpleReg.class);
    private OpenIDAuthenticationRequest request;

    /**
     * @param request An instance of OpenIDAuthenticationRequest
     */
    public OpenIDSimpleReg(OpenIDAuthenticationRequest request) throws IdentityException {
        if (request == null) {
            log.debug("Request cannot be null while initializing OpenIDSimpleReg");
            throw IdentityException.error("Request cannot be null while initializing OpenIDSimpleReg");
        }
        this.request = request;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addRequiredAttributes(List<String> requiredAttributes) throws IdentityException {
        AuthRequest authRequest = null;
        MessageExtension extension = null;
        try {
            authRequest = request.getAuthRequest();
            if (authRequest != null) {
                if (authRequest.hasExtension(SRegMessage.OPENID_NS_SREG)) {
                    extension = authRequest.getExtension(SRegMessage.OPENID_NS_SREG);
                } else if (authRequest.hasExtension(SRegMessage.OPENID_NS_SREG11)) {
                    extension = authRequest.getExtension(SRegMessage.OPENID_NS_SREG11);
                } else if (authRequest.hasExtension(AxMessage.OPENID_NS_AX)) {
                    extension = authRequest.getExtension(AxMessage.OPENID_NS_AX);
                }

                if (extension instanceof SRegRequest) {
                    SRegRequest sregReq = null;
                    List required = null;
                    List optional = null;

                    sregReq = (SRegRequest) extension;

                    // Get the required attributes as requested by the RP.
                    required = sregReq.getAttributes(true);
                    optional = sregReq.getAttributes();

                    if (optional != null && !optional.isEmpty()) {
                        for (Object attr : optional) {
                            if (!required.contains(attr)) {
                                required.add(attr);
                            }
                        }
                    }

                    for (Iterator<String> iterator = required.iterator(); iterator.hasNext(); ) {
                        String claimUri = iterator.next();
                        if (!requiredAttributes.contains(claimUri)) {
                            requiredAttributes.add(claimUri);
                        }
                    }
                }
            }
        } catch (MessageException e) {
            log.error("Failed to add required attributes of OpenID Simple Registration", e);
            throw IdentityException.error("Failed to add required attributes of OpenID Simple Registration", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageExtension getMessageExtension(String userId, String profileName, OpenIDAuthRequestDTO requestDTO)
            throws IdentityException {

        MessageExtension extension = null;
        AuthRequest authRequest = null;
        SRegResponse response = null;

        try {
            authRequest = request.getAuthRequest();

            if (authRequest.hasExtension(SRegRequest.OPENID_NS_SREG)) {
                extension = authRequest.getExtension(SRegRequest.OPENID_NS_SREG);
            } else if (authRequest.hasExtension(SRegMessage.OPENID_NS_SREG11)) {
                extension = authRequest.getExtension(SRegMessage.OPENID_NS_SREG11);
            } else if (authRequest.hasExtension(AxMessage.OPENID_NS_AX)) {
                extension = authRequest.getExtension(AxMessage.OPENID_NS_AX);
            }

            if (log.isDebugEnabled()) {
                if (extension == null) {
                    log.info("SReg extension is null");
                } else {
                    log.info("SReg extension: " + extension.getTypeUri());
                }
            }

            if (extension instanceof SRegRequest) {
                SRegRequest sregReq = null;
                List required = null;
                List optional = null;
                Map userDataSReg = null;
                Map<String, OpenIDClaimDTO> claimValues = null;

                sregReq = (SRegRequest) extension;

                // Get the required attributes as requested by the RP.
                required = sregReq.getAttributes(true);
                optional = sregReq.getAttributes();

                if (optional != null && !optional.isEmpty()) {
                    for (Object attr : optional) {
                        if (!required.contains(attr)) {
                            required.add(attr);
                        }
                    }
                }

                if (log.isDebugEnabled()) {
                    log.info("Required attributes for SReg request: " + required.toString());
                }

                userDataSReg = new HashMap();
                response = SRegResponse.createSRegResponse(sregReq, userDataSReg);
                claimValues = populateAttributeValues(required, userId, profileName, requestDTO);
                setSimpleAttributeRegistrationValues(response, claimValues);
            }

            return response;

        } catch (MessageException e) {
            log.error("Failed to create message extension for OpenID Simple Registration", e);
            throw IdentityException.error("Failed to create message extension for OpenID Simple Registration", e);
        }
    }

    /**
     * Populate the response with claim values. If we can't find the required values with us, we
     * simply avoid sending them. An Identity Provider MAY return any subset of the following fields
     * in response to the query.
     *
     * @param response    Simple Registration response.
     * @param claimValues Claim values.
     * @throws MessageException
     */
    protected void setSimpleAttributeRegistrationValues(SRegResponse response,
                                                        Map<String, OpenIDClaimDTO> claimValues)
            throws MessageException {

        Iterator<Entry<String, OpenIDClaimDTO>> iterator = null;
        OpenIDClaimDTO claim = null;
        Entry<String, OpenIDClaimDTO> entry = null;

        iterator = claimValues.entrySet().iterator();

        while (iterator.hasNext()) {
            entry = iterator.next();
            claim = entry.getValue();
            response.addAttribute(claim.getClaimUri(), claim.getClaimValue());
        }
    }
}