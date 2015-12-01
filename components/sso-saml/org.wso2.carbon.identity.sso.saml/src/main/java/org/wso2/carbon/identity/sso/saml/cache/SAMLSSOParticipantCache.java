/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
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

package org.wso2.carbon.identity.sso.saml.cache;

import org.wso2.carbon.identity.application.authentication.framework.store.SessionDataStore;
import org.wso2.carbon.identity.application.common.cache.BaseCache;

public class SAMLSSOParticipantCache extends BaseCache<SAMLSSOParticipantCacheKey, SAMLSSOParticipantCacheEntry> {

    private static final String CACHE_NAME = "SAMLSSOParticipantCache";
    private static volatile SAMLSSOParticipantCache instance;

    private SAMLSSOParticipantCache() {
        super(CACHE_NAME);
    }

    public static SAMLSSOParticipantCache getInstance() {
        if (instance == null) {
            synchronized (SAMLSSOParticipantCache.class) {
                if (instance == null) {
                    instance = new SAMLSSOParticipantCache();
                }
            }
        }
        return instance;
    }

    public void addToCache(SAMLSSOParticipantCacheKey key, SAMLSSOParticipantCacheEntry entry) {
        super.addToCache(key, entry);
        SessionDataStore.getInstance().storeSessionData(key.getSessionIndex(), CACHE_NAME, entry);
    }

    public SAMLSSOParticipantCacheEntry getValueFromCache(SAMLSSOParticipantCacheKey key) {
        SAMLSSOParticipantCacheEntry cacheEntry = super.getValueFromCache(key);
        if (cacheEntry == null) {
            cacheEntry = (SAMLSSOParticipantCacheEntry) SessionDataStore.getInstance().
                    getSessionData(key.getSessionIndex(), CACHE_NAME);
        }
        return cacheEntry;
    }

    public void clearCacheEntry(SAMLSSOParticipantCacheKey key) {
        super.clearCacheEntry(key);
        SessionDataStore.getInstance().clearSessionData(key.getSessionIndex(), CACHE_NAME);
    }

}
