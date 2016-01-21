/*
 * Copyright (c) 2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.oauth.util;

import org.wso2.carbon.identity.oauth.cache.CacheKey;

import java.util.ArrayList;
import java.util.Arrays;

public class ClaimCacheKey extends CacheKey {

    private static final long serialVersionUID = -1695934146647205833L;
    private String endUserName;
    private String[] requiredClaims;

    public ClaimCacheKey(String endUserName, String[] requiredClaims) {
        this.endUserName = endUserName;
        this.requiredClaims = requiredClaims;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ClaimCacheKey)) {
            return false;
        }
        if (!((ClaimCacheKey) o).getEndUserName().equals(getEndUserName())) {
            return false;
        }
        for (String requiredClaim : ((ClaimCacheKey) o).getRequiredClaims()) {
            if (!new ArrayList<String>(Arrays.asList(getRequiredClaims())).contains(requiredClaim)) {
                return false;
            }
        }
        for (String requiredClaim : getRequiredClaims()) {
            if (!new ArrayList<String>(Arrays.asList(((ClaimCacheKey) o).getRequiredClaims())).contains(requiredClaim)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = endUserName != null ? endUserName.hashCode() : 0;
        result = 31 * result + (requiredClaims != null ? Arrays.hashCode(requiredClaims) : 0);
        return result;
    }

    public String getEndUserName() {
        return endUserName;
    }

    public String[] getRequiredClaims() {
        return requiredClaims;
    }

    @Override
    public String toString() {
        return "ClaimCacheKey{" +
                "endUserName='" + endUserName + '\'' +
                ", requiredClaims=" + Arrays.toString(requiredClaims) +
                '}';
    }
}
