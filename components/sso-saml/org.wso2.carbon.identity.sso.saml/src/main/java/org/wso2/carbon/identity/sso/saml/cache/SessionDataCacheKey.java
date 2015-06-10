/*
 * Copyright (c) 2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

public class SessionDataCacheKey extends CacheKey {

    private static final long serialVersionUID = -1414485745666304223L;

    private String sessionDataKey;

    public SessionDataCacheKey(String sessionDataKey) {
        this.sessionDataKey = sessionDataKey;
    }

    public String getSessionDataKey() {
        return sessionDataKey;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((sessionDataKey == null) ? 0 : sessionDataKey.hashCode());
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
        SessionDataCacheKey other = (SessionDataCacheKey) obj;
        if (sessionDataKey == null) {
            if (other.sessionDataKey != null)
                return false;
        } else if (!sessionDataKey.equals(other.sessionDataKey))
            return false;
        return true;
    }

}
