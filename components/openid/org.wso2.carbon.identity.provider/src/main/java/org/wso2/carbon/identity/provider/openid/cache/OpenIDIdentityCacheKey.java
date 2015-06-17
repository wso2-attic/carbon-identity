/*
 * Copyright (c) 2004-2005, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.provider.openid.cache;

import org.wso2.carbon.base.MultitenantConstants;

/**
 * Identity Cache key which wraps the identity related cache key values
 */
public class OpenIDIdentityCacheKey extends OpenIDCacheKey {

    private static final long serialVersionUID = -7700438046096986522L;
    private int tenantId;
    private String key;

    public OpenIDIdentityCacheKey(int tenantId, String key) {
        this.tenantId = tenantId;
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public int getTenantId() {
        return tenantId;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof OpenIDIdentityCacheKey)) {
            return false;
        }
        OpenIDIdentityCacheKey cacheKey = (OpenIDIdentityCacheKey) obj;

        return cacheKey.getKey() != null && cacheKey.getKey().equals(key) && cacheKey.getTenantId() == tenantId;

    }

    @Override
    public int hashCode() {
        int hash = Integer.valueOf((tenantId == MultitenantConstants.SUPER_TENANT_ID) ? 0 : tenantId).hashCode();
        hash += hash + key.hashCode();
        return hash;
    }
}
