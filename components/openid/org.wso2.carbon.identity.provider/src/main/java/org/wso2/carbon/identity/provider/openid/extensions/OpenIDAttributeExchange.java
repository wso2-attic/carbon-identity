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

import org.apache.commons.collections.MapUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.MessageException;
import org.openid4java.message.MessageExtension;
import org.openid4java.message.ax.FetchRequest;
import org.openid4java.message.ax.FetchResponse;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.base.IdentityConstants.OpenId.ExchangeAttributes;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.provider.dto.OpenIDAuthRequestDTO;
import org.wso2.carbon.identity.provider.dto.OpenIDClaimDTO;
import org.wso2.carbon.identity.provider.openid.handlers.OpenIDAuthenticationRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Functionality related to OpenID Attribute Exchange. OpenID Attribute Exchange is an OpenID
 * service extension for exchanging identity information between end-points.
 */
public class OpenIDAttributeExchange extends OpenIDExtension {

    private static final Log log = LogFactory.getLog(OpenIDAttributeExchange.class);
    private OpenIDAuthenticationRequest openidAuthnRequest;

    /**
     * @param request An instance of OpenIDAuthenticationRequest
     */
    public OpenIDAttributeExchange(OpenIDAuthenticationRequest request) throws IdentityException {
        if (request == null) {
            log.debug("Request cannot be null while initializing OpenIDAttributeExchange");
            throw IdentityException.error("Request cannot be null while initializing OpenIDAttributeExchange");
        }
        this.openidAuthnRequest = request;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addRequiredAttributes(List<String> requiredAttributes) throws IdentityException {
        MessageExtension extensions = null;
        AuthRequest authRequest = null;

        try {
            authRequest = openidAuthnRequest.getAuthRequest();

            if (authRequest != null) {
                if (authRequest.hasExtension(FetchRequest.OPENID_NS_AX)) {
                    extensions = authRequest.getExtension(FetchRequest.OPENID_NS_AX);
                } else if (authRequest.hasExtension(ExchangeAttributes.NS_AX)) {
                    extensions = authRequest.getExtension(ExchangeAttributes.NS_AX);
                }

                if (extensions instanceof FetchRequest) {
                    Map required = null;
                    Map optional = null;
                    FetchRequest fetchRequest = null;

                    fetchRequest = (FetchRequest) extensions;

                    // Get the required attributes as requested by the RP.
                    required = fetchRequest.getAttributes(true);
                    optional = fetchRequest.getAttributes();

                    if (optional != null && !optional.isEmpty()) {
                        Iterator iterator = optional.entrySet().iterator();
                        Entry entry = null;
                        while (iterator.hasNext()) {
                            entry = (Entry) iterator.next();
                            if (!required.containsKey(entry.getKey())) {
                                required.put(entry.getKey(), entry.getValue());
                            }
                        }
                    }

                    Iterator<Entry<String, String>> iterator = null;
                    Entry<String, String> entry = null;
                    iterator = required.entrySet().iterator();

                    while (iterator.hasNext()) {
                        entry = iterator.next();
                        if (!requiredAttributes.contains((String) entry.getValue())) {
                            requiredAttributes.add((String) entry.getValue());
                        }
                    }
                }
            }
        } catch (MessageException e) {
            log.error("Failed to add required attributes of Attribute Exchange", e);
            throw IdentityException.error("Failed to add required attributes of Attribute Exchange", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageExtension getMessageExtension(String userId, String profileName, OpenIDAuthRequestDTO requestDTO)
            throws IdentityException {
        MessageExtension extensions = null;
        AuthRequest authRequest = null;
        FetchResponse fetchResponse = null;

        try {
            authRequest = openidAuthnRequest.getAuthRequest();
            if (authRequest.hasExtension(FetchRequest.OPENID_NS_AX)) {
                extensions = authRequest.getExtension(FetchRequest.OPENID_NS_AX);
            } else if (authRequest.hasExtension(IdentityConstants.OpenId.ExchangeAttributes.NS_AX)) {
                extensions = authRequest.getExtension(IdentityConstants.OpenId.ExchangeAttributes.NS_AX);
            }

            if (extensions instanceof FetchRequest) {
                Map required = null;
                Map optional = null;
                FetchRequest fetchRequest = null;
                Map<String, OpenIDClaimDTO> claimValues = null;

                fetchRequest = (FetchRequest) extensions;

                // Get the required attributes as requested by the RP.
                required = fetchRequest.getAttributes(true);
                optional = fetchRequest.getAttributes();

                if (optional != null && !optional.isEmpty()) {
                    Iterator iterator = optional.entrySet().iterator();
                    Entry entry = null;
                    while (iterator.hasNext()) {
                        entry = (Entry) iterator.next();
                        if (!required.containsKey(entry.getKey())) {
                            required.put(entry.getKey(), entry.getValue());
                        }
                    }
                }

                Iterator<Entry<String, String>> iterator = null;
                Entry<String, String> entry = null;
                iterator = required.entrySet().iterator();
                List<String> requiredAttributes = null;
                requiredAttributes = new ArrayList<String>();

                while (iterator.hasNext()) {
                    entry = iterator.next();
                    if (!requiredAttributes.contains((String) entry.getValue())) {
                        requiredAttributes.add((String) entry.getValue());
                    }
                }

                fetchResponse = FetchResponse.createFetchResponse(fetchRequest, new HashMap());
                claimValues = populateAttributeValues(requiredAttributes, userId, profileName, requestDTO);
                if (MapUtils.isNotEmpty(claimValues)) {
                    setAttributeExchangeValues(fetchResponse, claimValues);
                }
            }

            return fetchResponse;
        } catch (MessageException e) {
            log.error("Failed to create message extension for Attribute Exchange", e);
            throw IdentityException.error("Failed to create message extension for Attribute Exchange", e);
        }
    }

    /**
     * Populate the response with claim values. If we can't find the required values with us, we
     * simply avoid sending them. An Identity Provider MAY return any subset of the following fields
     * in response to the query.
     *
     * @param claimValues Claim values.
     * @throws MessageException
     */
    protected void setAttributeExchangeValues(FetchResponse response,
                                              Map<String, OpenIDClaimDTO> claimValues) throws MessageException {

        Iterator<Entry<String, OpenIDClaimDTO>> iterator = null;
        Entry<String, OpenIDClaimDTO> entry = null;
        OpenIDClaimDTO claim = null;

        iterator = claimValues.entrySet().iterator();

        while (iterator.hasNext()) {
            entry = iterator.next();
            claim = entry.getValue();
            response.addAttribute(claim.getClaimUri(), claim.getClaimValue());
        }
    }
}