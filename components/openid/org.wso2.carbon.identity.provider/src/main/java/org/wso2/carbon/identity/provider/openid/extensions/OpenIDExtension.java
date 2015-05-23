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

package org.wso2.carbon.identity.provider.openid.extensions;

import org.openid4java.message.MessageExtension;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.provider.IdentityProviderException;
import org.wso2.carbon.identity.provider.dto.OpenIDAuthRequestDTO;
import org.wso2.carbon.identity.provider.dto.OpenIDClaimDTO;
import org.wso2.carbon.identity.relyingparty.RelyingPartyException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for all OpenID extensions. Any OpenID extension added should
 * extend this class.
 */

public abstract class OpenIDExtension {

    private static final String MULTI_ATTRIBUTE_SEPARATOR = "MultiAttributeSeparator";

    /**
     * Creates an instance of MessageExtension for the OpenID authentication
     * response
     *
     * @param requestDTO
     * @param requestDTO OpenID authentication request
     * @return An instance of MessageExtension
     * @throws RelyingPartyException
     */
    public abstract MessageExtension getMessageExtension(String userId, String profileName, OpenIDAuthRequestDTO requestDTO)
            throws IdentityException;

    /**
     * Add required attributes.
     *
     * @param requiredAttributes Required attributes as per the OpenID authentication request.
     * @throws IdentityProviderException
     */
    public abstract void addRequiredAttributes(List<String> requiredAttributes)
            throws IdentityException;

    /**
     * Populate the required claims with claim values.
     *
     * @param requiredClaims Required claims as requested by the RP.
     * @param requestDTO
     * @return A map, populated with ClaimDO objects which have OpenIDTag, that
     * is OpenID supported
     * claims.
     * @throws IdentityProviderException
     */
    protected Map<String, OpenIDClaimDTO> populateAttributeValues(List<String> requiredClaims,
                                                                  String openId, String profileName, OpenIDAuthRequestDTO requestDTO)
            throws IdentityException {
        Map<String, OpenIDClaimDTO> map = null;
        map = new HashMap<String, OpenIDClaimDTO>();
        OpenIDClaimDTO[] claims = null;

        try {
            if (requestDTO.getResponseClaims() == null || requestDTO.getResponseClaims().size() == 0) {
                return map;
            }

            claims = getClaimValues(openId, profileName, requiredClaims, requestDTO.getResponseClaims());

            if (claims != null) {
                for (int i = 0; i < claims.length; i++) {
                    if (claims[i] != null) {
                        map.put(claims[i].getClaimUri(), claims[i]);
                    }
                }
            }
            return map;

        } catch (Exception e) {
            throw new IdentityException(e.getLocalizedMessage(), e);
        }
    }

    private OpenIDClaimDTO[] getClaimValues(String openId, String profileId, List<String> requiredClaims,
                                            Map<ClaimMapping, String> receivedClaims) throws Exception {

        List<OpenIDClaimDTO> claims = new ArrayList<OpenIDClaimDTO>();

        if (requiredClaims.isEmpty()) {
            for (Map.Entry<ClaimMapping, String> entry : receivedClaims.entrySet()) {
                if (MULTI_ATTRIBUTE_SEPARATOR.equals(entry.getKey().getRemoteClaim().getClaimUri())) {
                    continue;
                }
                OpenIDClaimDTO openIDClaimDTO = new OpenIDClaimDTO();
                openIDClaimDTO.setClaimUri(entry.getKey().getRemoteClaim().getClaimUri());
                openIDClaimDTO.setClaimValue(entry.getValue());
                claims.add(openIDClaimDTO);
            }
        } else {
            for (String requiredClaim : requiredClaims) {
                ClaimMapping mapping = getClaimMappingFromMap(receivedClaims, requiredClaim);
                if (mapping != null) {
                    OpenIDClaimDTO openIDClaimDTO = new OpenIDClaimDTO();
                    openIDClaimDTO.setClaimUri(mapping.getRemoteClaim().getClaimUri());
                    openIDClaimDTO.setClaimValue(receivedClaims.get(mapping));
                    claims.add(openIDClaimDTO);
                }
            }
        }
        return claims.toArray(new OpenIDClaimDTO[claims.size()]);
    }

    private ClaimMapping getClaimMappingFromMap(Map<ClaimMapping, String> claimMappings, String requiredClaimURI) {

        ClaimMapping mapping = null;

        for (ClaimMapping claimMapping : claimMappings.keySet()) {
            if (claimMapping.getRemoteClaim().getClaimUri().equals(requiredClaimURI)) {
                mapping = claimMapping;
                break;
            }
        }
        return mapping;
    }
}