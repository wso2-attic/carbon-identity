/*
 * Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.sso.saml.artifact.resolution.storage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.sso.saml.artifact.resolution.SAMLSSOArtifactResolutionConstants;
import org.wso2.carbon.identity.sso.saml.artifact.resolution.cache.SAMLSSOArtifactCache;
import org.wso2.carbon.identity.sso.saml.artifact.resolution.cache.SAMLSSOArtifactCacheEntry;
import org.wso2.carbon.identity.sso.saml.artifact.resolution.cache.SAMLSSOArtifactCacheKey;
import org.wso2.carbon.identity.sso.saml.cache.CacheEntry;
import org.wso2.carbon.identity.sso.saml.persistence.ArtifactStorage;
import org.wso2.carbon.identity.sso.saml.persistence.model.SAMLSSOArtifactResponse;

public class CacheBasedArtifactStorage implements ArtifactStorage {

    private final int DEFAULT_PRIORITY = 5;

    private int priority;

    private static Log log = LogFactory.getLog(CacheBasedArtifactStorage.class);

    public void setDefaultPriority() {
        priority = DEFAULT_PRIORITY;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public void store(SAMLSSOArtifactResponse artifactResponse) {
        SAMLSSOArtifactCacheKey cacheKey = new SAMLSSOArtifactCacheKey(artifactResponse.getArtifact());
        SAMLSSOArtifactCacheEntry cacheEntry = new SAMLSSOArtifactCacheEntry();
        cacheEntry.setArtifactResponse(artifactResponse);
        SAMLSSOArtifactCache.getInstance(SAMLSSOArtifactResolutionConstants.DEFAULT_ARTIFACT_LIFETIME).addToCache(cacheKey, cacheEntry);
    }

    @Override
    public SAMLSSOArtifactResponse retrieve(String artifact) {

        SAMLSSOArtifactResponse artifactResponse = null;
        SAMLSSOArtifactCacheKey key = new SAMLSSOArtifactCacheKey(artifact);
        CacheEntry cacheEntry = SAMLSSOArtifactCache.getInstance(0).getValueFromCache(key);
        if(cacheEntry != null) {
            artifactResponse = ((SAMLSSOArtifactCacheEntry)cacheEntry).getArtifactResponse();
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Cache Entry Not Found for the Received Artifact");
            }
        }
        return artifactResponse;
    }

    @Override
    public void clearEntry(String artifact) {

        SAMLSSOArtifactCacheKey key = new SAMLSSOArtifactCacheKey(artifact);
        SAMLSSOArtifactCache.getInstance(0).clearCacheEntry(key);
        if (log.isDebugEnabled()) {
            log.debug("Cache Entry Cleared for the Received Artifact");
        }
    }
}
