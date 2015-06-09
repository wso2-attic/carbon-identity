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

package org.wso2.carbon.identity.provisioning.connector.scim.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.identity.provisioning.AbstractProvisioningConnectorFactory;
import org.wso2.carbon.identity.provisioning.connector.scim.SCIMProvisioningConnectorFactory;

/**
 * @scr.component name=
 * "org.wso2.carbon.identity.provisioning.connector.scim.internal.SCIMConnectorServiceComponent"
 * immediate="true"
 */
public class SCIMConnectorServiceComponent {
    private static Log log = LogFactory.getLog(SCIMConnectorServiceComponent.class);

    protected void activate(ComponentContext context) {

        if (log.isDebugEnabled()) {
            log.debug("Activating SCIMConnectorServiceComponent");
        }

        try {
            SCIMProvisioningConnectorFactory scimProvisioningConnectorFactory = new SCIMProvisioningConnectorFactory();
            context.getBundleContext().registerService(AbstractProvisioningConnectorFactory.class.getName(), scimProvisioningConnectorFactory, null);
            if (log.isDebugEnabled()) {
                log.debug("SCIM Provisioning Connector bundle is activated");
            }
        } catch (Throwable e) {
            log.error(" Error while activating SCIM Provisioning Connector ", e);
        }
    }
}
