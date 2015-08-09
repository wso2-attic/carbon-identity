package org.wso2.carbon.claim.custom.cache;

import org.wso2.carbon.utils.CarbonUtils;

/**
 * Created by Chanuka on 6/23/15 AD.
 */
public class ClaimCache  extends BaseCache<CacheKey, CacheEntry> {

private static final String Claim_CACHE_NAME = "ClaimCache";

private static final ClaimCache instance = new ClaimCache(Claim_CACHE_NAME);

private ClaimCache(String cacheName) {
        super(cacheName);
        }

public static ClaimCache getInstance() {
        CarbonUtils.checkSecurity();
        return instance;
        }

@Override
public void addToCache(CacheKey key, CacheEntry entry) {
        super.addToCache(key, entry);
        }

@Override
public CacheEntry getValueFromCache(CacheKey key) {
        return super.getValueFromCache(key);
        }

@Override
public void clearCacheEntry(CacheKey key) {
        super.clearCacheEntry(key);
        }
        }
