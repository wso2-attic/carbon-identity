/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.oauth.cache;

/**
 * Authorization code is the cache key attribute here. Authenticated user attributes and OpenID Connect specific
 * attributes are stored against cache key.
 */
public class AuthorizationGrantCacheKey extends CacheKey {
    private static final long serialVersionUID = -2846349295093760488L;
    //todo:variable name userAttributesId should be change later because userAttributesId = authorizationCode
    private String userAttributesId;

    public AuthorizationGrantCacheKey(String userAttributesId) {
        this.userAttributesId = userAttributesId;
    }

    public String getUserAttributesId() {
        return userAttributesId;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AuthorizationGrantCacheKey)) {
            return false;
        }
        return this.userAttributesId.equals(((AuthorizationGrantCacheKey) o).getUserAttributesId());
    }

    @Override
    public int hashCode() {
        return userAttributesId.hashCode();
    }

}
