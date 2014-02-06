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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.provider.openid.OpenIDUtil;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.utils.TenantUtils;

import java.util.Map;

public class DefaultClaimsRetriever implements ClaimsRetriever {

    private static Log log = LogFactory.getLog(DefaultClaimsRetriever.class);

    @Override
    public void init() throws IdentityException {

    }

    @Override
    public Map<String, String> getUserClaimValues(String openId, String[] requiredClaims, String profile) throws IdentityException {

        try {
            String userName = null;
            userName = OpenIDUtil.getUserName(openId);
            String domainName = TenantUtils.getDomainNameFromOpenId(openId);
            String tenatUser = userName;

            if (userName.contains("@")) {
                tenatUser = userName.substring(0, userName.indexOf("@"));
            }
            UserStoreManager userStore = IdentityTenantUtil.getRealm(domainName, userName).getUserStoreManager();
            return userStore.getUserClaimValues(tenatUser, requiredClaims, profile);
        } catch (Exception e) {
            log.error("Error while retrieving claims values", e);
            throw new IdentityException("Error while retrieving claims values", e);
        }
    }
}
