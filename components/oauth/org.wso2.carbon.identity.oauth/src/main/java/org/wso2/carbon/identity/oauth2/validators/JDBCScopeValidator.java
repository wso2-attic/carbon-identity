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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.common.model.User;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.oauth.cache.CacheEntry;
import org.wso2.carbon.identity.oauth.cache.OAuthCache;
import org.wso2.carbon.identity.oauth.cache.OAuthCacheKey;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.identity.oauth.internal.OAuthComponentServiceHolder;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.dao.TokenMgtDAO;
import org.wso2.carbon.identity.oauth2.model.AccessTokenDO;
import org.wso2.carbon.identity.oauth2.model.ResourceScopeCacheEntry;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;


/**
 * The JDBC Scope Validation implementation. This validates the Resource's scope (stored in IDN_OAUTH2_RESOURCE_SCOPE)
 * against the Access Token's scopes.
 */
public class JDBCScopeValidator extends OAuth2ScopeValidator {

    Log log = LogFactory.getLog(JDBCScopeValidator.class);

    @Override
    public boolean validateScope(AccessTokenDO accessTokenDO, String resource) throws IdentityOAuth2Exception {

        //Get the list of scopes associated with the access token
        String[] scopes = accessTokenDO.getScope();

        //If no scopes are associated with the token
        if (scopes == null || scopes.length == 0) {
            return true;
        }

        String resourceScope = null;
        TokenMgtDAO tokenMgtDAO = new TokenMgtDAO();

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
            if(log.isDebugEnabled()){
                log.debug("Resource '" + resource + "' is not protected with a scope");
            }
            return true;
        }

        List<String> scopeList = new ArrayList<>(Arrays.asList(scopes));

        //If the access token does not bear the scope required for accessing the Resource.
        if(!scopeList.contains(resourceScope)){
            if(log.isDebugEnabled()){
                log.debug("Access token '" + accessTokenDO.getAccessToken() + "' does not bear the scope '" +
                            resourceScope + "'");
            }
            return false;
        }

        try {
            //Get the roles associated with the scope, if any
            Set<String> rolesOfScope = tokenMgtDAO.getRolesOfScopeByScopeKey(resourceScope);

            //If the scope doesn't have any roles associated with it.
            if(rolesOfScope == null || rolesOfScope.isEmpty()){
                if(log.isDebugEnabled()){
                    log.debug("Did not find any roles associated to the scope " + resourceScope);
                }
                return true;
            }

            if(log.isDebugEnabled()){
                StringBuilder logMessage = new StringBuilder("Found roles of scope '" + resourceScope + "' ");
                for(String role : rolesOfScope){
                    logMessage.append(role);
                    logMessage.append(", ");
                }
                log.debug(logMessage.toString());
            }

            User authzUser = accessTokenDO.getAuthzUser();
            RealmService realmService = OAuthComponentServiceHolder.getRealmService();

            int tenantId = realmService.getTenantManager().
                    getTenantId(authzUser.getTenantDomain());

            if (tenantId == 0 || tenantId == -1) {
                tenantId = IdentityTenantUtil.getTenantIdOfUser(authzUser.getUserName());
            }

            UserStoreManager userStoreManager;
            String[] userRoles;
            boolean tenantFlowStarted = false;

            try{
                //If this is a tenant user
                if(tenantId != MultitenantConstants.SUPER_TENANT_ID){
                    PrivilegedCarbonContext.startTenantFlow();
                    PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(
                                                            realmService.getTenantManager().getDomain(tenantId),true);
                    tenantFlowStarted = true;
                }

                userStoreManager = realmService.getTenantUserRealm(tenantId).getUserStoreManager();
                userRoles = userStoreManager.getRoleListOfUser(
                        MultitenantUtils.getTenantAwareUsername(authzUser.getUserName()));
            } finally {
                if (tenantFlowStarted) {
                    PrivilegedCarbonContext.endTenantFlow();
                }
            }

            if(userRoles != null && userRoles.length > 0){
                if(log.isDebugEnabled()){
                    StringBuilder logMessage = new StringBuilder("Found roles of user ");
                    logMessage.append(authzUser.getUserName());
                    logMessage.append(" ");
                    for(String role : userRoles){
                        logMessage.append(role);
                        logMessage.append(", ");
                    }
                    log.debug(logMessage.toString());
                }
                //Check if the user still has a valid role for this scope.
                rolesOfScope.retainAll(Arrays.asList(userRoles));
                return !rolesOfScope.isEmpty();
            }
            else{
                if(log.isDebugEnabled()){
                    log.debug("No roles associated for the user " + authzUser.getUserName());
                }
                return false;
            }

        } catch (UserStoreException e) {
            //Log and return since we do not want to stop issuing the token in case of scope validation failures.
            log.error("Error when getting the tenant's UserStoreManager or when getting roles of user ", e);
            return false;
        }
    }
}
