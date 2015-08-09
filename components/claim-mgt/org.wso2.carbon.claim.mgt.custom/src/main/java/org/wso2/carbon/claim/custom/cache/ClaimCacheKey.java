package org.wso2.carbon.claim.custom.cache;

/**
 * Created by Chanuka on 6/23/15 AD.
 */
public class ClaimCacheKey extends CacheKey {

    private String cacheKeyString;

    public ClaimCacheKey(String cacheKeyString) {
        this.cacheKeyString = cacheKeyString;
    }

    public String getClaimCacheKeyString() {
        return cacheKeyString;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ClaimCacheKey)) {
            return false;
        }
        return this.cacheKeyString.equals(((ClaimCacheKey) o).getClaimCacheKeyString());
    }

    @Override
    public int hashCode() {
        return cacheKeyString.hashCode();
    }
}
