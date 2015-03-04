/*
*  Copyright (c)  WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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


package org.wso2.carbon.identity.entitlement.cache;

import org.wso2.carbon.caching.impl.CachingConstants;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.entitlement.PDPConstants;


/**
 * Decision cache
 */
public class DecisionCache extends EntitlementBaseCache<IdentityCacheKey, String> {

    public DecisionCache(int timeOut) {
        super(CachingConstants.LOCAL_CACHE_PREFIX + PDPConstants.PDP_DECISION_CACHE, timeOut);
    }

    public void addToCache(String key, String decision) {

        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        IdentityCacheKey cacheKey = new IdentityCacheKey(tenantId, key);
        addToCache(cacheKey, decision);
    }

    public String getFromCache(String key) {

        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        IdentityCacheKey cacheKey = new IdentityCacheKey(tenantId, key);


        Object entry = getValueFromCache(cacheKey);
        if (entry != null) {
            return (String) entry;
        }

        return null;
    }

    public void clearCache() {
        clear();
    }

}
