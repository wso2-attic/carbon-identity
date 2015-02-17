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

package org.wso2.carbon.identity.sso.saml.artifact.resolution.cache;

import org.wso2.carbon.identity.sso.saml.cache.CacheKey;

public class SAMLSSOArtifactCacheKey extends CacheKey {

    private static final long serialVersionUID = -7367205961527597657L;

    private String artifactKey;

    public SAMLSSOArtifactCacheKey(String artifactKey) {
        this.artifactKey = artifactKey;
    }

    public String getArtifactKey() {
        return artifactKey;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SAMLSSOArtifactCacheKey other = (SAMLSSOArtifactCacheKey) obj;
        if (artifactKey == null) {
            if (other.artifactKey != null)
                return false;
        } else if (!artifactKey.equals(other.artifactKey))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((artifactKey == null) ? 0 : artifactKey.hashCode());
        return result;
    }
}
