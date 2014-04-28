/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.sso.saml.builders.claims;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.sso.saml.dto.SAMLSSOAuthnReqDTO;

import java.util.HashMap;
import java.util.Map;

public class DefaultClaimsRetriever implements ClaimsRetriever {

    private static Log log = LogFactory.getLog(DefaultClaimsRetriever.class);

    @Override
    public void init() {

    }

	@Override
	public Map<String, String> getUserClaimValues(SAMLSSOAuthnReqDTO authnReqDTO,
			String[] requestedClaims, String profile) throws IdentityException {
		
		Map<String, String> userAttributes = authnReqDTO.getUserAttributes();
		Map<String, String> returnAttibutes = null;
		
		if(requestedClaims != null) {
			
			if(userAttributes == null) {
				log.debug("No attributes found.");
				return null;
			}
			
			returnAttibutes = new HashMap<String, String>();
			
			for(String reqClaim : requestedClaims) {
				String value = userAttributes.get(reqClaim);
				returnAttibutes.put(reqClaim, value);
				if(log.isDebugEnabled()) {
					log.debug("Setting up claim " + reqClaim + " : " + value);
				}
			}
		}
		
		return returnAttibutes;
	}


}
