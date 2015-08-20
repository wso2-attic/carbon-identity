/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.provisioning.connector.google.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.identity.provisioning.AbstractProvisioningConnectorFactory;
import org.wso2.carbon.identity.provisioning.connector.google.GoogleProvisioningConnectorFactory;

//TODO : Add dependency to IdP MetadataService

/**
 * @scr.component name=
 * "org.wso2.carbon.identity.provisioning.google.internal.GoogleConnectorServiceComponent"
 * immediate="true"
 */
public class GoogleConnectorServiceComponent {

    private static Log log = LogFactory
            .getLog(GoogleConnectorServiceComponent.class);

    protected void activate(ComponentContext context) {

        if (log.isDebugEnabled()) {
            log.debug("Activating GoogleConnectorServiceComponent");
        }

        try {
            GoogleProvisioningConnectorFactory googleProvisioningConnectorFactory = new GoogleProvisioningConnectorFactory();

            context.getBundleContext().registerService(AbstractProvisioningConnectorFactory.class.getName(), googleProvisioningConnectorFactory, null);
            if (log.isDebugEnabled()) {
                log.debug("Google Identity Provisioning Connector bundle is activated");
            }
        } catch (Throwable e) {
            log.error("Error while activating Google Identity Provisioning Connector", e);
        }
    }

}
