/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.oauth.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.listener.AbstractCacheListener;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.oauth.cache.CacheEntry;
import org.wso2.carbon.identity.oauth.cache.OAuthCache;
import org.wso2.carbon.identity.oauth.cache.OAuthCacheKey;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.identity.oauth2.model.AccessTokenDO;
import org.wso2.carbon.identity.oauth2.util.OAuth2Util;

import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryRemovedListener;

public class OAuthCacheRemoveListener extends AbstractCacheListener<String, CacheEntry>
        implements CacheEntryRemovedListener<String, CacheEntry> {

    private static Log log = LogFactory.getLog(OAuthCacheRemoveListener.class);

    @Override
    public void entryRemoved(CacheEntryEvent<? extends String, ? extends CacheEntry> cacheEntryEvent)
            throws CacheEntryListenerException {

        AccessTokenDO accessTokenDO = (AccessTokenDO) cacheEntryEvent.getValue();

        if (accessTokenDO != null) {

            if (log.isDebugEnabled()) {
                log.debug("OAuth cache removed for consumer id : " + accessTokenDO.getConsumerKey());
            }

            boolean isUsernameCaseSensitive = IdentityUtil
                    .isUserStoreInUsernameCaseSensitive(accessTokenDO.getAuthzUser().getUserName());
            String cacheKeyString;
            if (isUsernameCaseSensitive){
                cacheKeyString = accessTokenDO.getConsumerKey() + ":" + accessTokenDO.getAuthzUser().getUserName() + ":"
                        + OAuth2Util.buildScopeString(accessTokenDO.getScope());
            }else {
                cacheKeyString =
                        accessTokenDO.getConsumerKey() + ":" + accessTokenDO.getAuthzUser().getUserName().toLowerCase()
                                + ":" + OAuth2Util.buildScopeString(accessTokenDO.getScope());
            }

            OAuthCacheKey oauthcacheKey = new OAuthCacheKey(cacheKeyString);
            OAuthCache oauthCache = OAuthCache
                    .getInstance(OAuthServerConfiguration.getInstance().getOAuthCacheTimeout());

            oauthCache.clearCacheEntry(oauthcacheKey);
            oauthcacheKey = new OAuthCacheKey(accessTokenDO.getAccessToken());

            oauthCache.clearCacheEntry(oauthcacheKey);

        }
    }
}
