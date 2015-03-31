/*
 * Copyright 2005-2008 WSO2, Inc. (http://wso2.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.provider.openid;

import java.util.Date;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openid4java.association.Association;
import org.openid4java.association.AssociationException;
import org.openid4java.server.InMemoryServerAssociationStore;
import org.wso2.carbon.identity.provider.openid.cache.OpenIDAssociationCache;
import org.wso2.carbon.identity.provider.openid.dao.OpenIDAssociationDAO;

/**
 * This is the custom AssociationStore. Uses super's methods to generate
 * associations. However this class persist the associations in the identity
 * database. In the case of loading an association it will first look in the
 * super and if fails, it will look in the database. The database may be shared
 * in a clustered environment.
 * 
 * @author WSO2 Inc.
 * 
 */
public class OpenIDServerAssociationStore extends
		InMemoryServerAssociationStore {

	private int storeId = 0;
	private String timestamp;
	private int counter;
	private OpenIDAssociationCache cache;
	private OpenIDAssociationDAO dao;

	private static Log log = LogFactory
			.getLog(OpenIDServerAssociationStore.class);

	/**
	 * Here we instantiate a DAO to access the identity database.
	 * 
	 * @param dbConnection
	 * @param privateAssociations
	 *            if this association store stores private associations
	 */
	public OpenIDServerAssociationStore(String associationsType) {
		storeId = new Random().nextInt(9999);
		timestamp = Long.toString(new Date().getTime());
		counter = 0;
		cache = OpenIDAssociationCache.getCacheInstance();
		dao = new OpenIDAssociationDAO(associationsType);
	}

	/**
	 * Super will generate the association and it will be persisted by the DAO.
	 * 
	 * @param type
	 *            association type defined in the OpenID 2.0
	 * @param expiryIn
	 *            date
	 * @return <code>Association</code>           
	 */
	public synchronized Association generate(String type, int expiryIn)
			throws AssociationException {
		String handle = storeId + timestamp + "-" + counter++;
		final Association association = Association.generate(type, handle, expiryIn);
		cache.addToCache(association);
		// Asynchronous write to database
		Thread thread = new Thread() {
			public void run() {
				log.debug("Stroing association " + association.getHandle()
						+ " in the database.");
				dao.storeAssociation(association);
			}
		};
		thread.start();
		return association;
	}

	/**
	 * First try to load from the memory, in case of failure look in the db.
	 * @param handle
	 * @return <code>Association<code>
	 */
	public synchronized Association load(String handle) {

		boolean chacheMiss = false;

		// looking in the cache
		Association association  = cache.getFromCache(handle);

		// if failed, look in the database
		if (association == null) {
			log.debug("Association " + handle
					+ " not found in cache. Loading from the database.");
			association = dao.loadAssociation(handle);
			chacheMiss = true;
		}

		// no association found for the given handle
		if (association == null) {
			log.debug("Association " + handle + " not found in the database.");
			return null;
		}

		// if the association is expired
		if (association.hasExpired()) {
			log.warn("Association is expired for handle " + handle);
			remove(handle); // remove only from db
			return null;

		} else if (chacheMiss) {
			// add the missing entry to the cache
			cache.addToCache(association);
		}

		return association;
	}

	/**
	 * Removes the association from the memory and db.
	 */
	public synchronized void remove(final String handle) {

		// we are not removing from cache
		// because it will cost a database call
		// for a cache miss. Associations are self validating tokens
		// cache.removeCacheEntry(handle);

		// removing from the database
		Thread thread = new Thread() {
			public void run() {
				log.debug("Removing the association" + handle + " from the database");
				dao.removeAssociation(handle);
			}
		};
		thread.start();
	}
}
