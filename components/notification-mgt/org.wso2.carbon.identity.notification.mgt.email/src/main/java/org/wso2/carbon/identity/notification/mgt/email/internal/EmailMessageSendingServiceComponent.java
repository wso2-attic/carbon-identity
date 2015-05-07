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

package org.wso2.carbon.identity.notification.mgt.email.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.identity.notification.mgt.NotificationSendingModule;
import org.wso2.carbon.identity.notification.mgt.email.EmailSendingModule;

/**
 * @scr.component name="carbon.identity.notification.mgt.email" immediate="true"
 */

@SuppressWarnings("unused")
public class EmailMessageSendingServiceComponent {

    private static Log log = LogFactory.getLog(EmailMessageSendingServiceComponent.class);

    protected void activate(ComponentContext ctxt) {

        try {
            // Registering email message sending module on user operation for entitlement component
            ctxt.getBundleContext().registerService(NotificationSendingModule.class.getName(),
                    new EmailSendingModule(), null);
            if (log.isDebugEnabled()) {
                log.debug("Email notification sending module is activated");
            }
        } catch (Throwable t) {
            // Catching throwable, since there may be throwable throwing while initiating
            log.error("Error while registering email notification sending module", t);
        }
    }

    protected void deactivate(ComponentContext ctxt) {
        if (log.isDebugEnabled()) {
            log.debug("Email notification sending module is deactivated");
        }
    }
}