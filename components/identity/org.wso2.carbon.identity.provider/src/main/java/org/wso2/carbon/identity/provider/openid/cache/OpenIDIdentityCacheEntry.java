package org.wso2.carbon.identity.provider.openid.cache;


import java.security.Key;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;

/**
 * Identity Cache entry which wraps the identity related cache entry values
 */
public class OpenIDIdentityCacheEntry extends OpenIDCacheEntry {

    private String cacheEntry;
//    private Set<String> cacheEntrySet;
    private String[] cacheEntryArray;
    private int hashEntry;
    private long cacheInterval;
    private boolean cacheClearing;
    private Key secretKey;
    private Date date;    
    private static final long serialVersionUID = 3746964700806693258L;

    public OpenIDIdentityCacheEntry(String cacheEntry) {
        this.cacheEntry = cacheEntry;
    }

    public OpenIDIdentityCacheEntry(int hashEntry) {
        this.hashEntry = hashEntry;
    }

    public OpenIDIdentityCacheEntry(boolean cacheClearing) {
        this.cacheClearing = cacheClearing;
    }

    public OpenIDIdentityCacheEntry(String cacheEntry, long cacheInterval) {
        this.cacheEntry = cacheEntry;
        this.cacheInterval = cacheInterval;
    }

    public OpenIDIdentityCacheEntry(String[] cacheEntryArray) {
        this.cacheEntryArray = Arrays.copyOf(cacheEntryArray, cacheEntryArray.length);
    }

//    public OpenIDIdentityCacheEntry(Set<String> cacheEntrySet) {
//        this.cacheEntrySet = cacheEntrySet;
//    }

    public OpenIDIdentityCacheEntry(String cacheEntry, Key secretKey, Date date) {
        this.cacheEntry = cacheEntry;
        this.secretKey = secretKey;
        this.date = date;
    }

    public String getCacheEntry() {
        return cacheEntry;
    }

    public int getHashEntry() {
        return hashEntry;
    }

    public long getCacheInterval() {
        return cacheInterval;
    }

//    public Set<String> getCacheEntrySet() {
//        return cacheEntrySet;
//    }

    public boolean isCacheClearing() {
        return cacheClearing;
    }

    public String[] getCacheEntryArray() {
        return cacheEntryArray;
    }

    public Key getSecretKey() {
        return secretKey;
    }

    public Date getDate() {
        return date;
    }
}
