/*
 * Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.authorization.core.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * @scr.component name="identity.authorization.component" immediate="true"
 * @scr.reference name="registry.service"
 *                interface=
 *                "org.wso2.carbon.registry.core.service.RegistryService"
 *                cardinality="1..1" policy="dynamic" bind="setRegistryService"
 *                unbind="unsetRegistryService"
 * @scr.reference name="user.realmservice.default"
 *                interface="org.wso2.carbon.user.core.service.RealmService"
 *                cardinality="1..1"
 *                policy="dynamic" bind="setRealmService"
 *                unbind="unsetRealmService"
 */
public class AuthorizationServiceComponent {

	private static RegistryService registryService = null;

	private static RealmService realmService;

	private static Log log = LogFactory.getLog(AuthorizationServiceComponent.class);

	private static AuthorizationConfigHolder configHolder;

	/**
	 * @param ctxt
	 */
	protected void activate(ComponentContext ctxt) {

		if (log.isDebugEnabled()) {
			log.info("Identity Authorization bundle is activated");
		}

		try {
			configHolder = new AuthorizationConfigHolder();
			ExtensionBuilder builder = new ExtensionBuilder();
			builder.buildAuthorizationConfig(configHolder);
		} catch (Exception e) {
			log.error("Failed to initialize Entitlement Service", e);
		}
	}

	/**
	 * @param ctxt
	 */
	protected void deactivate(ComponentContext ctxt) {
		if (log.isDebugEnabled()) {
			log.debug("Identity Authorization bundle is deactivated");
		}
	}

	/**
	 * sets registry service
	 * 
	 * @param registryService
	 *            <code>RegistryService</code>
	 */
	protected void setRegistryService(RegistryService registryService) {
		if (log.isDebugEnabled()) {
			log.debug("RegistryService set in Authorization bundle");
		}
		AuthorizationServiceComponent.registryService = registryService;
	}

	/**
	 * un-sets registry service
	 * 
	 * @param registryService
	 *            <code>RegistryService</code>
	 */
	protected void unsetRegistryService(RegistryService registryService) {
		if (log.isDebugEnabled()) {
			log.debug("RegistryService unset in Authorization bundle");
		}
		AuthorizationServiceComponent.registryService = null;
	}

	/**
	 * sets realm service
	 * 
	 * @param realmService
	 *            <code>RealmService</code>
	 */
	protected void setRealmService(RealmService realmService) {
		if (log.isDebugEnabled()) {
			log.debug("DefaultUserRealm set in Authorization bundle");
		}
		AuthorizationServiceComponent.realmService = realmService;
	}

	/**
	 * unsets realm service
	 * 
	 * @param realmService
	 *            <code>RealmService</code>
	 */
	protected void unsetRealmService(RealmService realmService) {
		if (log.isDebugEnabled()) {
			log.debug("DefaultUserRealm set in Authorization bundle");
		}
		AuthorizationServiceComponent.realmService = realmService;
	}

	public static RegistryService getRegistryService() {
		return registryService;
	}

	public static RealmService getRealmService() {
		return realmService;
	}

	public static AuthorizationConfigHolder getConfigHolder() {
		return configHolder;
	}
//
//	public static CacheInvalidator getCacheInvalidator() {
//		return cacheInvalidator;
//	}
//
//	public static void setCacheInvalidator(CacheInvalidator cacheInvalidator) {
//		AuthorizationServiceComponent.cacheInvalidator = cacheInvalidator;
//	}
//
//	public static void unSetCacheInvalidator(CacheInvalidator cacheInvalidator) {
//		AuthorizationServiceComponent.cacheInvalidator = null;
//	}

}
