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

package org.wso2.carbon.identity.oauth.endpoint.user.impl;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.oauth.cache.AuthorizationGrantCache;
import org.wso2.carbon.identity.oauth.cache.AuthorizationGrantCacheEntry;
import org.wso2.carbon.identity.oauth.cache.AuthorizationGrantCacheKey;
import org.wso2.carbon.identity.oauth.endpoint.util.ClaimUtil;
import org.wso2.carbon.identity.oauth.user.UserInfoClaimRetriever;
import org.wso2.carbon.identity.oauth.user.UserInfoEndpointException;
import org.wso2.carbon.identity.oauth.user.UserInfoResponseBuilder;
import org.wso2.carbon.identity.oauth2.dto.OAuth2TokenValidationResponseDTO;

import java.util.HashMap;
import java.util.Map;

public class UserInfoJWTResponse implements UserInfoResponseBuilder {

    private static final Log log = LogFactory.getLog(UserInfoJWTResponse.class);

    @Override
    public String getResponseString(OAuth2TokenValidationResponseDTO tokenResponse)
            throws UserInfoEndpointException, OAuthSystemException {

        Map<ClaimMapping, String> userAttributes = getUserAttributesFromCache(tokenResponse);

        Map<String, Object> claims = null;

        if (userAttributes.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("User attributes not found in cache. Trying to retrieve from user store.");
            }
            claims = ClaimUtil.getClaimsFromUserStore(tokenResponse);
        } else {
            UserInfoClaimRetriever retriever = UserInfoEndpointConfig.getInstance().getUserInfoClaimRetriever();
            claims = retriever.getClaimsMap(userAttributes);
        }
        if(claims == null){
            claims = new HashMap<String,Object>();
        }
        claims.put("sub", tokenResponse.getAuthorizedUser());

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet();
        jwtClaimsSet.setAllClaims(claims);
        return new PlainJWT(jwtClaimsSet).serialize();
    }

    private Map<ClaimMapping, String> getUserAttributesFromCache(OAuth2TokenValidationResponseDTO tokenResponse) {

        Map<ClaimMapping,String> claims = new HashMap<ClaimMapping,String>();
        AuthorizationGrantCacheKey cacheKey =
                new AuthorizationGrantCacheKey(tokenResponse.getAuthorizationContextToken().getTokenString());
        AuthorizationGrantCacheEntry cacheEntry =
                (AuthorizationGrantCacheEntry) AuthorizationGrantCache.getInstance(0).getValueFromCache(cacheKey);
        if (cacheEntry != null) {
            claims = cacheEntry.getUserAttributes();
        }
        return claims;
    }

}
