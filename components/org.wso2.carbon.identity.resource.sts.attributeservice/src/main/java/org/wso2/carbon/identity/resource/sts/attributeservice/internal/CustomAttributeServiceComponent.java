/*
 * Copyright (c) 2006, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.resource.sts.attributeservice.internal;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.wso2.carbon.identity.provider.IdentityAttributeService;
import org.wso2.carbon.identity.resource.sts.attributeservice.ResourceAttributeService;

public class CustomAttributeServiceComponent implements BundleActivator {
	private static Log log = LogFactory.getLog(CustomAttributeServiceComponent.class);

	@Override
	public void start(BundleContext ctxt) throws Exception {
		Properties props;

		try {
			props = new Properties();
			// Register the SampleAttributeService under
			// IdentityAttributeService interface.
			ctxt.registerService(IdentityAttributeService.class.getName(),
					new ResourceAttributeService(), props);

			if (log.isDebugEnabled()) {
				log.debug("Successfully registered the SampleAttributeService service");
			}
		} catch (Throwable e) {
			String message = null;
			message = "Error while activating the org.wso2.carbon.identity.samples.attributeservice bundle";
			log.error(message, e);
		}
	}

	@Override
	public void stop(BundleContext arg0) throws Exception {
		if (log.isDebugEnabled()) {
			log.debug("org.wso2.carbon.identity.samples.attributeservice bundle is deactivated");
		}
	}
}