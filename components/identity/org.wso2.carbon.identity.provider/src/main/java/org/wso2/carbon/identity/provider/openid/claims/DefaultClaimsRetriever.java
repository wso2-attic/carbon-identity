/*
 *  Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.provider.openid.claims;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityException;

public class DefaultClaimsRetriever implements ClaimsRetriever {

	private static Log log = LogFactory.getLog(DefaultClaimsRetriever.class);

	@Override
	public void init() throws IdentityException {

	}

	@Override
	public Map<String, String> getUserClaimValues(String openId, String[] requiredClaims,
			String profile, Map<String, String> receivedClaims) throws IdentityException {

		Map<String, String> responseClaims = null;

		if (receivedClaims != null) {
			responseClaims = new HashMap<String, String>();

			for (String requiredClaim : requiredClaims) {
				String value = receivedClaims.get(requiredClaim);
				if (value != null) {
					responseClaims.put(requiredClaim, value);
					if (log.isDebugEnabled()) {
						log.debug("Returning Claim " + requiredClaim + " : " + value);
					}
				}
			}

		} else {
			log.debug("No user claims found");
		}

		return responseClaims;

	}
}
