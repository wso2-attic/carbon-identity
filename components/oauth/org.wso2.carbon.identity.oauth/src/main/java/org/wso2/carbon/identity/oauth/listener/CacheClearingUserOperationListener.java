/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.oauth.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedUser;
import org.wso2.carbon.identity.oauth.cache.AuthorizationGrantCache;
import org.wso2.carbon.identity.oauth.cache.AuthorizationGrantCacheEntry;
import org.wso2.carbon.identity.oauth.cache.AuthorizationGrantCacheKey;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.dao.TokenMgtDAO;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.common.AbstractUserOperationEventListener;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.util.Map;
import java.util.Set;

/**
 * This listener will remove the  existing AuthorizationGrantCache session from OAUthCache
 * This will help in getting updated profile values in the claims
 */
public class CacheClearingUserOperationListener extends AbstractUserOperationEventListener {
    private static final Log log = LogFactory.getLog(CacheClearingUserOperationListener.class);

    @Override
    public boolean doPostSetUserClaimValues(String userName, Map<String, String> claims, String profileName,
                                            UserStoreManager userStoreManager) throws UserStoreException {
        removeAccessTokenFromCache(userName, userStoreManager);
        return super.doPostSetUserClaimValues(userName, claims, profileName, userStoreManager);
    }

    private void removeAccessTokenFromCache(String userName, UserStoreManager userStoreManager) throws
            UserStoreException {
        String userStoreDomain = UserCoreUtil.getDomainName(userStoreManager.getRealmConfiguration());
        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        TokenMgtDAO tokenMgtDAO = new TokenMgtDAO();
        Set<String> accessTokenAndAuthorizationCodeList;
        AuthenticatedUser authenticatedUser = new AuthenticatedUser();
        authenticatedUser.setUserStoreDomain(userStoreDomain);
        authenticatedUser.setTenantDomain(tenantDomain);
        authenticatedUser.setUserName(userName);
        try {
            accessTokenAndAuthorizationCodeList = tokenMgtDAO.getAccessTokenListForUserByTenant(Integer.toString
                    (userStoreManager.getTenantId()), userName, authenticatedUser.toString());
            accessTokenAndAuthorizationCodeList.addAll(tokenMgtDAO.getAuthorizationCodeListForUserByTenant(Integer
                    .toString(userStoreManager.getTenantId()), userName, authenticatedUser.toString()));
            if (accessTokenAndAuthorizationCodeList != null && accessTokenAndAuthorizationCodeList.size() > 0) {
                for (String accessToken : accessTokenAndAuthorizationCodeList) {
                    AuthorizationGrantCacheKey cacheKey = new AuthorizationGrantCacheKey(accessToken);
                    AuthorizationGrantCacheEntry cacheEntry = (AuthorizationGrantCacheEntry) AuthorizationGrantCache
                            .getInstance().getValueFromCacheByToken(cacheKey);
                    if (cacheEntry != null) {
                        AuthorizationGrantCache.getInstance().clearCacheEntryByToken(cacheKey);
                    }
                }
            }
        } catch (IdentityOAuth2Exception e) {
            String errorMsg = "Error occurred while retrieving access tokens issued for user : " + userName;
            log.error(errorMsg, e);
        }

    }
}
