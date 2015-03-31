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

package org.wso2.carbon.identity.samples.attributeservice.internal;

import org.wso2.carbon.identity.provider.IdentityAttributeService;
import org.wso2.carbon.identity.samples.attributeservice.SampleAttributeService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;

import java.util.Properties;

/**
 * @scr.component name="org.wso2.carbon.identity.samples.attributeservice" immediate="true"
 */
public class CustomAttributeServiceComponent {
    private static Log log = LogFactory.getLog(CustomAttributeServiceComponent.class);

    public CustomAttributeServiceComponent() {
    }

    protected void activate(ComponentContext ctxt) {
        Properties props;

        try {
            props = new Properties();
            // Register the SampleAttributeService under IdentityAttributeService interface.
            ctxt.getBundleContext().registerService(IdentityAttributeService.class.getName(),
                    new SampleAttributeService(), props);

            if (log.isDebugEnabled()) {
                log.debug("Successfully registered the SampleAttributeService service");
            }
        } catch (Throwable e) {
            String message = null;
            message = "Error while activating the org.wso2.carbon.identity.samples.attributeservice bundle";
            log.error(message, e);
        }
    }

    protected void deactivate(ComponentContext ctxt) {
        if (log.isDebugEnabled()) {
            log.debug("org.wso2.carbon.identity.samples.attributeservice bundle is deactivated");
        }
    }
}