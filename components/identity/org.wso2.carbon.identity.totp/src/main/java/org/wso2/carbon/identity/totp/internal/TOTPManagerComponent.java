/*
 * Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.totp.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
//import org.wso2.carbon.identity.notification.mgt.NotificationSender;
import org.wso2.carbon.identity.totp.TOTPManager;
import org.wso2.carbon.identity.totp.TOTPManagerImpl;
import org.wso2.carbon.user.core.service.RealmService;
import java.util.Hashtable;

/**
 * @scr.component name="identity.totp.component" immediate="true"
 * @scr.reference name="realm.service"
 * interface="org.wso2.carbon.user.core.service.RealmService"cardinality="1..1"
 * policy="dynamic" bind="setRealmService" unbind="unsetRealmService"
 */


public class TOTPManagerComponent {

    private static Log log = LogFactory.getLog(TOTPManagerComponent.class);
    private static RealmService realmService;
    //private static NotificationSender notificationSender;

    protected void activate(ComponentContext ctxt) {

        TOTPManagerImpl totpService = new TOTPManagerImpl();
        Hashtable<String, String> props = new Hashtable<String, String>();

        ctxt.getBundleContext().registerService(TOTPManager.class.getName(), totpService, props);

        if (log.isDebugEnabled()) {
            log.info("TOTPServiceComponent bundle is activated");
        }
    }

    protected void deactivate(ComponentContext ctxt) {
        if (log.isDebugEnabled()) {
            log.info("TOTPServiceComponent bundle is deactivated");
        }
    }

    protected void setRealmService(RealmService realmService) {
        log.debug("Setting the Realm Service");
        TOTPManagerComponent.realmService = realmService;
    }

    protected void unsetRealmService(RealmService realmService) {
        log.debug("UnSetting the Realm Service");
        TOTPManagerComponent.realmService = null;
    }

//    protected void setNotificationSender(NotificationSender notificationSender) {
//        if (log.isDebugEnabled()) {
//            log.debug("Un-setting notification sender in Entitlement bundle");
//        }
//        this.notificationSender = notificationSender;
//    }
//
//    protected void unsetNotificationSender(NotificationSender notificationSender) {
//        if (log.isDebugEnabled()) {
//            log.debug("Setting notification sender in Entitlement bundle");
//        }
//        this.notificationSender = null;
//    }

    public static RealmService getRealmService() {
        return realmService;
    }
    
//    public static NotificationSender getNotificationSender(){
//        return notificationSender;
//    }
    
}
