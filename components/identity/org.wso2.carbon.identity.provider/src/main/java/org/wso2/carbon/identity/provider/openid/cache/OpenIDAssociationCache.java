/*
 *  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openid4java.association.Association;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;

/**
 * Cache implementation for OpenID Associations
 */
public class OpenIDAssociationCache extends OpenIDBaseCache<OpenIDIdentityCacheKey, OpenIDIdentityCacheEntry> {

    private static OpenIDAssociationCache associationCache = null;
	private final static String OPENID_ASSOCIATION_CACHE = "OPENID_ASSOCIATION_CACHE";
	private static Log log = LogFactory.getLog(OpenIDAssociationCache.class);

	/**
	 * Private constructor
	 * @param cacheName
	 */
	private OpenIDAssociationCache() {
		super(OPENID_ASSOCIATION_CACHE);
	}

	/**
	 * Returns the singleton of the <code>AssociationCache</code>
	 * 
	 * @return
	 */
	public synchronized static OpenIDAssociationCache getCacheInstance() {
		if (associationCache == null) {
			associationCache = new OpenIDAssociationCache();
		}
		return associationCache;
	}
	


	/**
	 * Add the entry to the cache.
	 * 
	 * @param association
	 */
	public void addToCache(Association association) {
		if (log.isDebugEnabled()) {
			log.debug("Trying to add to cache.");
		}
		if (association != null && association.getHandle() != null) {
			OpenIDIdentityCacheKey cacheKey = new OpenIDIdentityCacheKey(0, association.getHandle());
			OpenIDIdentityCacheEntry cacheEntry =
			                                new OpenIDIdentityCacheEntry(association.getType(),
			                                                       association.getMacKey(),
			                                                       association.getExpiry());
			associationCache.addToCache(cacheKey, cacheEntry);
			if (log.isDebugEnabled()) {
				log.debug("New entry is added to cache  : " + association.getHandle());
			}
		}
	}

	/**
	 * Read entries from the cache. If no value found then returns null.
	 * If the association is expired then returns null.
	 * Else returns the <code>Association</code>
	 * 
	 * @param handle
	 * @return <code>Association<code>
	 */
	public Association getFromCache(String handle) {
		if (log.isDebugEnabled()) {
			log.debug("Trying to get from cache.");
		}
		if (handle != null) {
			OpenIDIdentityCacheKey cacheKey = new OpenIDIdentityCacheKey(0, handle);
			OpenIDIdentityCacheEntry cacheEntry =
			                                (OpenIDIdentityCacheEntry) associationCache.getValueFromCache(cacheKey);
			if (cacheEntry != null) {
				if (log.isDebugEnabled()) {
					log.debug("Cache hit for handle : " + handle);
				}
				Date expiry = cacheEntry.getDate();
				String type = cacheEntry.getCacheEntry();
				Key secretKey = cacheEntry.getSecretKey();

				if (expiry != null && type != null && secretKey != null) {
					return new Association(type, handle, (SecretKey) secretKey, expiry);
					/*
					 * We are not removing expired handles from the cache. If we
					 * do, then at a lookup for a expired search, it will fall
					 * back to a database lookup which costs a lot. JCache
					 * should remove an entry if an entry was never called.
					 * 
					 * if(association.hasExpired()){
					 * associationCache.removeCacheEntry(handle);
					 * if(log.isDebugEnabled()){
					 * log.debug("Expired entry in cache for handle : " +
					 * handle); } } else { return association; }
					 */
				}
			} else {
				if (log.isDebugEnabled()) {
					log.debug("Cache miss for handle : " + handle);
				}
			}
		}
		return null;
	}

	/**
	 * Remove the cache entry from the cache
	 * @param handle
	 */
	public void removeCacheEntry(String handle) {
		if (handle != null) {
			OpenIDIdentityCacheKey cacheKey = new OpenIDIdentityCacheKey(0, handle);
			associationCache.clearCacheEntry(cacheKey);
		}
	}
}
