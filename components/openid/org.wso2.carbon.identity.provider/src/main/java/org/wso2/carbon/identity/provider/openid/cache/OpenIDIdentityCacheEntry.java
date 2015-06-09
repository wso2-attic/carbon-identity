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

import java.security.Key;
import java.util.Arrays;
import java.util.Date;

/**
 * Identity Cache entry which wraps the identity related cache entry values
 */
public class OpenIDIdentityCacheEntry extends OpenIDCacheEntry {

    private static final long serialVersionUID = 3746964700806693258L;
    private String cacheEntry;
    private String[] cacheEntryArray;
    private int hashEntry;
    private long cacheInterval;
    private boolean cacheClearing;
    private Key secretKey;
    private Date date;

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
