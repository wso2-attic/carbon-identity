/*
 * Copyright (c) 2015 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.fido.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.identity.fido.u2f.U2FService;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @scr.component name="identity.fido" immediate="true"
 * @scr.reference name="user.realmservice.default" interface="org.wso2.carbon.user.core.service.RealmService"
 * cardinality="1..1" policy="dynamic" bind="setRealmService"
 * unbind="unsetRealmService"
 */
public class FIDOServiceComponent {
	private static final Log LOG = LogFactory.getLog(FIDOServiceComponent.class);
	public static U2FService u2FService;
	private static RealmService realmService;
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	public static RealmService getRealmService() {
		return realmService;
	}

	public static void setRealmService(RealmService realmService) {
		FIDOServiceComponent.realmService = realmService;
	}

	protected void unsetRealmService(RealmService realmService) {
		setRealmService(null);
	}

	public static U2FService getU2FService() {
		return u2FService;
	}

	protected void activate(ComponentContext context) {
		BundleContext bundleContext = context.getBundleContext();

		//initialize services
		u2FService = U2FService.getInstance();

		//register OSGI services
		bundleContext.registerService(U2FService.class, u2FService, null);

		LOG.info("Activated U2fService bundle");
	}

	protected void deactivate(ComponentContext ctxt) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("CA component is deactivating ...");
		}
	}
}
