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
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.core.IdentityClaimManager;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.provider.dto.OpenIDClaimDTO;
import org.wso2.carbon.identity.provider.openid.OpenIDUtil;
import org.wso2.carbon.identity.provider.openid.claims.ClaimsRetriever;
import org.wso2.carbon.user.core.claim.Claim;
import org.wso2.carbon.utils.TenantUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for all OpenID extensions. Any OpenID extension added should
 * extend this class.
 */

public abstract class OpenIDExtension {

    private static ClaimsRetriever claimsRetriever = null;

	/**
	 * Creates an instance of MessageExtension for the OpenID authentication
	 * response
	 * 
	 * @param request
	 *            OpenID authentication request
	 * @return An instance of MessageExtension
	 * @throws RelyingPartyException
	 */
	public abstract MessageExtension getMessageExtension(String userId, String profileName)
	                                                                                       throws IdentityException;

	/**
	 * Add required attributes.
	 * 
	 * @param requiredAttributes
	 *            Required attributes as per the OpenID authentication request.
	 * @throws IdentityProviderException
	 */
	public abstract void addRequiredAttributes(List<String> requiredAttributes)
	                                                                           throws IdentityException;

	/**
	 * Populate the required claims with claim values.
	 * 
	 * @param requiredClaims
	 *            Required claims as requested by the RP.
	 * @param userId
	 *            User ID.
	 * @return A map, populated with ClaimDO objects which have OpenIDTag, that
	 *         is OpenID supported
	 *         claims.
	 * @throws IdentityProviderException
	 */
	protected Map<String, OpenIDClaimDTO> populateAttributeValues(List<String> requiredClaims,
	                                                              String openId, String profileName)
	                                                                                                throws IdentityException {
		Map<String, OpenIDClaimDTO> map = null;
		map = new HashMap<String, OpenIDClaimDTO>();
		OpenIDClaimDTO[] claims = null;

		try {
			claims = getClaimValues(openId, profileName, requiredClaims);

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

	private OpenIDClaimDTO[] getClaimValues(String openId, String profileId,
	                                        List<String> requiredClaims) throws Exception {
		Map<String, String> claimValues = null;
		OpenIDClaimDTO[] claims = null;
		OpenIDClaimDTO dto = null;
		IdentityClaimManager claimManager = null;
		Claim[] claimData = null;
		String[] claimArray = new String[requiredClaims.size()];

        synchronized (this){
            if(claimsRetriever == null){
                synchronized (this){
                    claimsRetriever = (ClaimsRetriever)Class.forName(
                            IdentityUtil.getProperty("OpenID.ClaimsRetrieverImplClass").trim()).newInstance();
                    claimsRetriever.init();
                }
            }
        }

        String userName = null;
        userName = OpenIDUtil.getUserName(openId);
        String domainName = TenantUtils.getDomainNameFromOpenId(openId);

		claimValues =
		              claimsRetriever.getUserClaimValues(openId, requiredClaims.toArray(claimArray),
		                                           profileId);

		claims = new OpenIDClaimDTO[claimValues.size()];
		int i = 0;
		claimManager = IdentityClaimManager.getInstance();

		if (!requiredClaims.isEmpty() && requiredClaims.get(0).startsWith("http://axschema.org")) {
			claimData =
			            claimManager.getAllSupportedClaims(IdentityConstants.OPENID_AX_DIALECT,
			                                               IdentityTenantUtil.getRealm(domainName,
			                                                                           userName));
		} else {
			claimData =
			            claimManager.getAllSupportedClaims(IdentityConstants.OPENID_SREG_DIALECT,
			                                               IdentityTenantUtil.getRealm(domainName,
			                                                                           userName));
		}

		for (int j = 0; j < claimData.length; j++) {
			if (claimValues.containsKey(claimData[j].getClaimUri())) {
				dto = new OpenIDClaimDTO();
				dto.setClaimUri(claimData[j].getClaimUri());
				dto.setClaimValue(claimValues.get(claimData[j].getClaimUri()));
				dto.setDisplayTag(claimData[j].getDisplayTag());
				dto.setDescription(claimData[j].getDescription());
				claims[i++] = dto;
			}
		}

		return claims;
	}
}