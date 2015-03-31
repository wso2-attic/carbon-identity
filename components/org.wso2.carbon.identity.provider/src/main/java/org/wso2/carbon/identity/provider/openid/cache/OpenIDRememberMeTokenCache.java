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

import java.sql.Timestamp;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.model.OpenIDRememberMeDO;
import org.wso2.carbon.identity.core.util.IdentityUtil;

/**
 * The Cache implementation for OpenID RememberMe tokens
 * @author WSO2 Inc
 *
 */
public class OpenIDRememberMeTokenCache extends OpenIDBaseCache<OpenIDIdentityCacheKey, OpenIDIdentityCacheEntry> {
	
	private static OpenIDRememberMeTokenCache rememberMeCache = null;
	private static final String OPENID_REMEMBER_ME_CACHE = "OPENID_REMEMBER_ME_CACHE";
	private static Log log = LogFactory.getLog(OpenIDRememberMeTokenCache.class);

	/**
	 * Private constructor
	 * @param cacheName
	 */
	protected OpenIDRememberMeTokenCache() {
	    super(OPENID_REMEMBER_ME_CACHE);
    }
	
	/**
	 * Returns the singleton of the <code>OpenIDRememberMeTokenCache</code>
	 * @return
	 */
	public static synchronized OpenIDRememberMeTokenCache getCacheInstance() {
		if (rememberMeCache == null) {
			rememberMeCache = new OpenIDRememberMeTokenCache();
		}
		return rememberMeCache;
	}
	
	/**
	 * Updates the OpenID RememberMe token in cache
	 * @param rememberMe
	 * @throws Exception
	 */
	public synchronized void updateTokenData(OpenIDRememberMeDO rememberMe) throws Exception {
		try {
			String username = rememberMe.getUserName();
			int tenantId = IdentityUtil.getTenantIdOFUser(rememberMe.getUserName());
			if (log.isDebugEnabled()) {
				log.debug("Updating RememberMe token in cache for " + username +
				          " with tenant ID " + tenantId);
			}
			OpenIDIdentityCacheKey key = new OpenIDIdentityCacheKey(tenantId, username);
			// if the entry exist, remove it
			if (rememberMeCache.getValueFromCache(key) != null) {
				rememberMeCache.clearCacheEntry(key);
			}
			// now create a new entry
			Date date = null;
			if (rememberMe.getTimestamp() != null) {
				date = new Date(rememberMe.getTimestamp().getTime());
			} else {
				date = new Date();
			}
			OpenIDIdentityCacheEntry entry = new OpenIDIdentityCacheEntry(rememberMe.getToken(), null, date);
			// add the entry
			rememberMeCache.addToCache(key, entry);

		} catch (IdentityException e) {
			log.error("Error while updating RememberMe cache ", e);
			throw new Exception("Error while updating RememberMe cache");
		}
	}
	
	/**
	 * Returns the RememberMe token from cache
	 * @param rememberMe
	 * @return <code>OpenIDRememberMeDO</code>
	 * @throws Exception
	 */
	public synchronized OpenIDRememberMeDO getTokenData(OpenIDRememberMeDO rememberMe) throws Exception {
		try {
			String username = rememberMe.getUserName();
			int tenantId = IdentityUtil.getTenantIdOFUser(rememberMe.getUserName());
			if (log.isDebugEnabled()) {
				log.debug("Loading RememberMe token in cache for " + username + " with tenant ID " +
				          tenantId);
			}
			OpenIDIdentityCacheKey key = new OpenIDIdentityCacheKey(tenantId, username);
			OpenIDIdentityCacheEntry entry = (OpenIDIdentityCacheEntry) rememberMeCache.getValueFromCache(key);
			if (entry == null) {
				return null;
			}
			rememberMe.setToken(entry.getCacheEntry());
			Timestamp timestamp = new Timestamp(entry.getDate().getTime());
			rememberMe.setTimestamp(timestamp);

			return rememberMe;
		} catch (IdentityException e) {
			log.error("Error while loading RememberMe token from cache ", e);
			throw new Exception("Error while loading RememberMe token from cache");
		}
	}

}
