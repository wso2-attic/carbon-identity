/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
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

package org.wso2.carbon.identity.notification.mgt.json.internal;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.identity.notification.mgt.NotificationSendingModule;
import org.wso2.carbon.identity.notification.mgt.json.JsonMessageModule;

/**
 * @scr.component name="carbon.identity.notification.mgt.json" immediate="true"
 */

@SuppressWarnings("unused")
public class JsonMessageSendingServiceComponent {

    private static Log log = LogFactory.getLog(JsonMessageSendingServiceComponent.class);

    protected void activate(ComponentContext ctxt) {
        // Using try catch to whole activator. Unless if something goes wrong (configuration failure) module will keep
        // trying to start
        try {
            // Registering json message sending module on user operation for entitlement component
            ctxt.getBundleContext().registerService(NotificationSendingModule.class.getName(),
                    new JsonMessageModule(), null);
            if (log.isDebugEnabled()) {
                log.debug("REST JSON notification sending component is activated ");
            }
        } catch (Throwable t) {
            // Catching throwable, since there may be throwable throwing while initiating
            log.error("Error while initializing  REST JSON  notification sending module", t);
        }
    }

    protected void deactivate(ComponentContext ctxt) {
        if (log.isDebugEnabled()) {
            log.debug("REST JSON notification sending module is deactivated");
        }
    }
}