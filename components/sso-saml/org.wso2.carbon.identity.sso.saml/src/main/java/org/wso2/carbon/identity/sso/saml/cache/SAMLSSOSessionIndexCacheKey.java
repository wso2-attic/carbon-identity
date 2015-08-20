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

public class SAMLSSOSessionIndexCacheKey extends CacheKey {

    private static final long serialVersionUID = 2102638240841113093L;

    private String tokenId;

    public SAMLSSOSessionIndexCacheKey(String tokenId) {
        this.tokenId = tokenId;
    }

    public String getTokenId() {
        return tokenId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((tokenId == null) ? 0 : tokenId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SAMLSSOSessionIndexCacheKey other = (SAMLSSOSessionIndexCacheKey) obj;
        if (tokenId == null) {
            if (other.tokenId != null)
                return false;
        } else if (!tokenId.equals(other.tokenId))
            return false;
        return true;
    }
}
