/*
 * Copyright (c) 2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.oauth.endpoint.util;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.claim.mgt.ClaimManagerHandler;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.oauth.user.UserInfoEndpointException;
import org.wso2.carbon.identity.oauth2.dto.OAuth2TokenValidationResponseDTO;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClaimUtil {
    static final String SP_DIALECT = "http://wso2.org/oidc/claim";
    private static final Log log = LogFactory.getLog(ClaimUtil.class);

    private ClaimUtil() {

    }

    public static Map<String, Object> getClaimsFromUserStore(OAuth2TokenValidationResponseDTO tokenResponse) throws
            UserInfoEndpointException {
        String username = tokenResponse.getAuthorizedUser();
        String tenantDomain = MultitenantUtils.getTenantDomain(tokenResponse.getAuthorizedUser());
        UserRealm realm;
        List<String> claimURIList = new ArrayList<>();
        Map<String, Object> mappedAppClaims = new HashMap<>();

        try {
            realm = IdentityTenantUtil.getRealm(tenantDomain, username);

            if (realm == null) {
                log.warn("No valid tenant domain provider. Empty claim returned back");
                return new HashMap<>();
            }

            Map<String, String> spToLocalClaimMappings;

            UserStoreManager userstore = realm.getUserStoreManager();

            // need to get all the requested claims
            Map<String, String> requestedLocalClaimMap = ClaimManagerHandler.getInstance()
                    .getMappingsMapFromOtherDialectToCarbon(SP_DIALECT, null, tenantDomain, true);
            if (MapUtils.isNotEmpty(requestedLocalClaimMap)) {
                for (String s : requestedLocalClaimMap.keySet()) {
                    claimURIList.add(s);

                }
                if (log.isDebugEnabled()) {
                    log.debug("Requested number of local claims: " + claimURIList.size());
                }

                spToLocalClaimMappings = ClaimManagerHandler.getInstance().getMappingsMapFromOtherDialectToCarbon
                        (SP_DIALECT, null, tenantDomain, false);

                Map<String, String> userClaims = userstore.getUserClaimValues(MultitenantUtils.getTenantAwareUsername
                        (username), claimURIList.toArray(new String[claimURIList.size()]), null);
                if (log.isDebugEnabled()) {
                    log.debug("User claims retrieved from user store: " + userClaims.size());
                }

                if (MapUtils.isEmpty(userClaims)) {
                    return new HashMap<>();
                }

                for (Map.Entry<String, String> entry : spToLocalClaimMappings.entrySet()) {
                    String value = userClaims.get(entry.getValue());
                    if (value != null) {
                        mappedAppClaims.put(entry.getKey(), value);
                        if (log.isDebugEnabled() &&
                                IdentityUtil.isTokenLoggable(IdentityConstants.IdentityTokens.USER_CLAIMS)) {
                            log.debug("Mapped claim: key -  " + entry.getKey() + " value -" + value);
                        }
                    }
                }
            }

        } catch (Exception e) {
            if(e instanceof UserStoreException){
                if (e.getMessage().contains("UserNotFound")) {
                    if (log.isDebugEnabled()) {
                        log.debug("User " + username + " not found in user store");
                    }
                }
            } else {
                log.error("Error while retrieving the claims from user store for " + username, e);
                throw new UserInfoEndpointException("Error while retrieving the claims from user store for " + username);
            }
        }
        return mappedAppClaims;
    }
}