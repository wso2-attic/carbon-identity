/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.sts.passive.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.RegistryType;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * @scr.component name="identity.passive.sts.component" immediate="true"
 * @scr.reference name="registry.service"
 *                interface="org.wso2.carbon.registry.core.service.RegistryService"
 *                cardinality="1..1" policy="dynamic" bind="setRegistryService"
 *                unbind="unsetRegistryService"
 * @scr.reference name="user.realmservice.default"
 *                interface="org.wso2.carbon.user.core.service.RealmService" cardinality="1..1"
 *                policy="dynamic" bind="setRealmService" unbind="unsetRealmService"
 */
public class IdentityPassiveSTSServiceComponent {
	private static Log log = LogFactory.getLog(IdentityPassiveSTSServiceComponent.class);
	private static RealmService userRealmService = null;
	private static RegistryService registryService;

	/**
     *
     */
	public IdentityPassiveSTSServiceComponent() {
	}

	/**
	 * @param ctxt
	 */
	protected void activate(ComponentContext ctxt) {

	}

	/**
	 * @param userRealmDelegating
	 */
	protected void setRealmService(RealmService realm) {
		if (log.isDebugEnabled()) {
			log.info("DelegatingUserRealm set in Identity Provider bundle");
		}
		userRealmService = realm;
	}

	/**
	 * @param userRealmDelegating
	 */
	protected void unsetRealmService(RealmService realm) {
		if (log.isDebugEnabled()) {
			log.info("DelegatingUserRealm set in Identity Provider bundle");
		}
	}

	/**
	 * @return
	 */
	public static RealmService getRealmService() {
		return userRealmService;
	}

	public static RegistryService getRegistryervice() {
		return registryService;
	}

	protected void setRegistryService(RegistryService registryService) {
		if (log.isDebugEnabled()) {
			log.debug("RegistryService set in Passive STS bundle");
		}
		IdentityPassiveSTSServiceComponent.registryService = registryService;
	}

	protected void unsetRegistryService(RegistryService registryService) {
		if (log.isDebugEnabled()) {
			log.debug("RegistryService unset in Passive STS bundle");
		}
		registryService = null;
	}

	public static Registry getGovernanceSystemRegistry() throws RegistryException {
		return (Registry) CarbonContext.getThreadLocalCarbonContext().getRegistry(
				RegistryType.SYSTEM_GOVERNANCE);
	}

	public static Registry getConfigSystemRegistry() throws RegistryException {
		return (Registry) CarbonContext.getThreadLocalCarbonContext().getRegistry(
				RegistryType.SYSTEM_CONFIGURATION);
	}

}