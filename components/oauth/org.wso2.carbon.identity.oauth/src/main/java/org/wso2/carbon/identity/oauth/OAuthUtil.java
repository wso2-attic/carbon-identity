/*
*Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.wso2.carbon.identity.oauth;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.utils.Base64;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.oauth.cache.CacheKey;
import org.wso2.carbon.identity.oauth.cache.OAuthCache;
import org.wso2.carbon.identity.oauth.cache.OAuthCacheKey;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.registry.core.utils.UUIDGenerator;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class OAuthUtil {

    public static final Log log = LogFactory.getLog(OAuthUtil.class);

    /**
     * Generates a random number using two UUIDs and HMAC-SHA1
     *
     * @return generated secure random number
     * @throws IdentityOAuthAdminException Invalid Algorithm or Invalid Key
     */
    public static String getRandomNumber() throws IdentityOAuthAdminException {
        try {
            String secretKey = UUIDGenerator.generateUUID();
            String baseString = UUIDGenerator.generateUUID();

            SecretKeySpec key = new SecretKeySpec(secretKey.getBytes(), "HmacSHA1");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(key);
            byte[] rawHmac = mac.doFinal(baseString.getBytes());
            String random = Base64.encode(rawHmac);
            // Registry doesn't have support for these character.
            random = random.replace("/", "_");
            random = random.replace("=", "a");
            random = random.replace("+", "f");
            return random;
        } catch (Exception e) {
            log.error("Error when generating a random number.", e);
            throw new IdentityOAuthAdminException("Error when generating a random number.", e);
        }
    }

    public static void clearOAuthCache(String consumerKey, String authorizedUser) {
        clearOAuthCache(consumerKey + ":" + authorizedUser);
    }

    public static void clearOAuthCache(String consumerKey, String authorizedUser, String scope) {
        clearOAuthCache(consumerKey + ":" + authorizedUser + ":" + scope);
    }

    public static void clearOAuthCache(String oauthCacheKey) {
        OAuthCache oauthCache;
        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext
                    .getThreadLocalCarbonContext();
            carbonContext.setTenantId(MultitenantConstants.SUPER_TENANT_ID);
            carbonContext.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);

            CacheKey cacheKey = new OAuthCacheKey(oauthCacheKey);
            if (OAuthServerConfiguration.getInstance().isCacheEnabled()) {
                oauthCache = OAuthCache.getInstance(0);
                oauthCache.clearCacheEntry(cacheKey);
            }
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

}
