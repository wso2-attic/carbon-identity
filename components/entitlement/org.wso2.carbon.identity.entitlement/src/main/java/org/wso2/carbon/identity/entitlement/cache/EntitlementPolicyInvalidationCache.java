/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.entitlement.cache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.entitlement.PDPConstants;

/**
 *
 */
public class EntitlementPolicyInvalidationCache extends EntitlementBaseCache<IdentityCacheKey, IdentityCacheEntry> {

    private static EntitlementPolicyInvalidationCache entitlementPolicyCache = new EntitlementPolicyInvalidationCache();
    private static final Log log = LogFactory.getLog(EntitlementPolicyInvalidationCache.class);
    private int myHashCode;

    private EntitlementPolicyInvalidationCache() {
        super(PDPConstants.ENTITLEMENT_POLICY_INVALIDATION_CACHE);
    }

    /**
     * Gets a new instance of EntitlementPolicyInvalidationCache.
     *
     * @return A new instance of EntitlementPolicyInvalidationCache.
     */
    public static EntitlementPolicyInvalidationCache getInstance() {
        return entitlementPolicyCache;
    }

    public void invalidateCache() {

        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();

        IdentityCacheKey cacheKey = new IdentityCacheKey(tenantId, "");
        int valueToCache = myHashCode + 1;
        IdentityCacheEntry cacheEntry = new IdentityCacheEntry(valueToCache);
        addToCache(cacheKey, cacheEntry);
        if (log.isDebugEnabled()) {
            log.debug("My Hash code of Policy cache is : " + myHashCode);
            log.debug("Adding Shared Hash of Policy cache : " + valueToCache);
        }
    }

    public boolean isInvalidate() {

        int hashCode;
        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        IdentityCacheKey cacheKey = new IdentityCacheKey(tenantId, "");
        Object entry = getValueFromCache(cacheKey);
        if (entry != null) {
            IdentityCacheEntry cacheEntry = (IdentityCacheEntry) entry;
            hashCode = cacheEntry.getHashEntry();
            if (log.isDebugEnabled()) {
                log.debug("My Hash code of Policy cache is : " + myHashCode);
                log.debug("Shared Hash code of Policy cache is : " + hashCode);
            }
            if (hashCode > myHashCode) {
                myHashCode = hashCode;
                if (Integer.MAX_VALUE == myHashCode) {
                    myHashCode = 0;
                }
                return true;
            }
        }

        return false;
    }
}
