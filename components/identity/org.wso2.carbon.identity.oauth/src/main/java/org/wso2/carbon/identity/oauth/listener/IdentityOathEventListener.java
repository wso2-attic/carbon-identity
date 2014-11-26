/**
 *  Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.carbon.identity.oauth.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.core.model.OAuthAppDO;
import org.wso2.carbon.identity.oauth.OAuthUtil;
import org.wso2.carbon.identity.oauth.common.exception.InvalidOAuthClientException;
import org.wso2.carbon.identity.oauth.dao.OAuthAppDAO;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.dao.TokenMgtDAO;
import org.wso2.carbon.identity.oauth2.model.AccessTokenDO;
import org.wso2.carbon.identity.oauth2.util.OAuth2Util;
import org.wso2.carbon.user.core.common.AbstractUserOperationEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * This is an implementation of UserOperationEventListener. This defines
 * additional operations
 * for some of the core user management operations
 *
 */
public class IdentityOathEventListener extends AbstractUserOperationEventListener {
    private static final Log log = LogFactory.getLog(IdentityOathEventListener.class);

    /**
     * Bundle execution order id.
     */
    @Override
    public int getExecutionOrderId() {
        return 1501;
    }

    /**
     * Deleting user from the identity database prerequisites.
     */
    @Override
    public boolean doPreDeleteUser(java.lang.String userName, org.wso2.carbon.user.core.UserStoreManager userStoreManager)
            throws org.wso2.carbon.user.core.UserStoreException {
        TokenMgtDAO tokenMgtDAO = new TokenMgtDAO();
        OAuthAppDAO appDAO = new OAuthAppDAO();

        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        String domainUsername = userName + "@" + tenantDomain;
        domainUsername = domainUsername.toLowerCase();

        try {
            OAuthAppDO[] appDOs = tokenMgtDAO.getAppsAuthorizedByUser(domainUsername);
            String appNames[] = new String[appDOs.length];
            for (int i = 0; i < appDOs.length; i++) {
                OAuthAppDO appDO = appDAO.getAppInformation(appDOs[i].getOauthConsumerKey());
                appNames[i] = appDO.getApplicationName();
            }

            //Retrieving the AccessTokenDO array before revoking
            List<AccessTokenDO> accessTokenDOs = new ArrayList<AccessTokenDO>();
            String userStoreDomain = null;
            if (OAuth2Util.checkAccessTokenPartitioningEnabled() &&
                    OAuth2Util.checkUserNameAssertionEnabled()) {
                userStoreDomain = OAuth2Util.getUserStoreDomainFromUserId(domainUsername);
            }
            for (String appName : appNames) {
                for (OAuthAppDO appDO : appDOs) {
                    appDO = appDAO.getAppInformation(appDO.getOauthConsumerKey());
                    if (appDO.getApplicationName().equals(appName)) {
                        AccessTokenDO accessTokenDO = tokenMgtDAO.getValidAccessTokenIfExist(appDO.getOauthConsumerKey(),
                                domainUsername, userStoreDomain, true);
                        accessTokenDOs.add(accessTokenDO);
                    }
                }
            }

            //Revoking the tokens
            tokenMgtDAO.revokeTokensByResourceOwner(appNames, domainUsername);

            //Clear cache with AccessTokenDO
            for (AccessTokenDO accessTokenDO : accessTokenDOs) {
                OAuthUtil.clearOAuthCache(accessTokenDO.getConsumerKey(), accessTokenDO.getAuthzUser(),
                        OAuth2Util.buildScopeString(accessTokenDO.getScope()));
            }
            log.debug("Remove apps authorized by user[" + domainUsername + "].");
        } catch (IdentityOAuth2Exception e) {
            log.error("Error while retrieving OAuth application information", e);
            return false;
        } catch (InvalidOAuthClientException e) {
            log.error("Error while retrieving OAuth application information", e);
            return false;
        }
        return true;
    }
}
