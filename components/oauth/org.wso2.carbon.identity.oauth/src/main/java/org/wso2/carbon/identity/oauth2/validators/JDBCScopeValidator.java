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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.oauth2.validators;

import org.wso2.carbon.identity.oauth.cache.CacheEntry;
import org.wso2.carbon.identity.oauth.cache.OAuthCache;
import org.wso2.carbon.identity.oauth.cache.OAuthCacheKey;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.dao.TokenMgtDAO;
import org.wso2.carbon.identity.oauth2.model.AccessTokenDO;
import org.wso2.carbon.identity.oauth2.model.ResourceScopeCacheEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * The JDBC Scope Validation implementation. This validates the Resource's scope (stored in IDN_OAUTH2_RESOURCE_SCOPE)
 * against the Access Token's scopes.
 */
public class JDBCScopeValidator extends OAuth2ScopeValidator {

    @Override
    public boolean validateScope(AccessTokenDO accessTokenDO, String resource) throws IdentityOAuth2Exception {

        //Get the list of scopes associated with the access token
        String[] scopes = accessTokenDO.getScope();

        //If no scopes are associated with the token
        if (scopes == null || scopes.length == 0) {
            return true;
        }

        String resourceScope = null;

        boolean cacheHit = false;
        // Check the cache, if caching is enabled.
        if (OAuthServerConfiguration.getInstance().isCacheEnabled()) {
            OAuthCache oauthCache = OAuthCache.getInstance();
            OAuthCacheKey cacheKey = new OAuthCacheKey(resource);
            CacheEntry result = oauthCache.getValueFromCache(cacheKey);

            //Cache hit
            if (result instanceof ResourceScopeCacheEntry) {
                resourceScope = ((ResourceScopeCacheEntry) result).getScope();
                cacheHit = true;
            }
        }

        if (!cacheHit) {
            TokenMgtDAO tokenMgtDAO = new TokenMgtDAO();
            resourceScope = tokenMgtDAO.findScopeOfResource(resource);

            if (OAuthServerConfiguration.getInstance().isCacheEnabled()) {
                OAuthCache oauthCache = OAuthCache.getInstance();
                OAuthCacheKey cacheKey = new OAuthCacheKey(resource);
                ResourceScopeCacheEntry cacheEntry = new ResourceScopeCacheEntry(resourceScope);
                //Store resourceScope in cache even if it is null (to avoid database calls when accessing resources for
                //which scopes haven't been defined).
                oauthCache.addToCache(cacheKey, cacheEntry);
            }
        }

        //Return TRUE if - There does not exist a scope definition for the resource
        if (resourceScope == null) {
            return true;
        }

        List<String> scopeList = new ArrayList<String>(Arrays.asList(scopes));

        //Return true if the resource scope is one of the scopes associated with the access token.
        return scopeList.contains(resourceScope);
    }
}
