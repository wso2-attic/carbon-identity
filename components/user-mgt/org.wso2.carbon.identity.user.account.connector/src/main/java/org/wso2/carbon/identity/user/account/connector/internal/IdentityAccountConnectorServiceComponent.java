/*
*  Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
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

package org.wso2.carbon.identity.user.account.connector.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.identity.user.account.connector.UserAccountConnector;
import org.wso2.carbon.identity.user.account.connector.UserAccountConnectorImpl;
import org.wso2.carbon.user.core.listener.UserOperationEventListener;
import org.wso2.carbon.user.core.listener.UserStoreManagerListener;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * @scr.component name="identity.user.account.connector.component" immediate=true
 * @scr.reference name="user.realmservice.default"
 *                interface="org.wso2.carbon.user.core.service.RealmService"
 *                cardinality="1..1" policy="dynamic" bind="setRealmService"
 *                unbind="unsetRealmService"
 * @scr.reference name="user.store.manager.listener.service"
 *                interface="org.wso2.carbon.user.core.listener.UserStoreManagerListener"
 *                cardinality="0..n" policy="dynamic"
 *                bind="setUserStoreManagerListenerService"
 *                unbind="unsetUserStoreManagerListenerService"
 * @scr.reference name="user.operation.event.listener.service"
 *                interface="org.wso2.carbon.user.core.listener.UserOperationEventListener"
 *                cardinality="0..n" policy="dynamic"
 *                bind="setUserOperationEventListenerService"
 *                unbind="unsetUserOperationEventListenerService" *
 */
public class IdentityAccountConnectorServiceComponent {

    private static Log log = LogFactory.getLog(IdentityAccountConnectorServiceComponent.class);

    private static BundleContext bundleContext;
    private static RealmService realmService;
    private static Collection<UserStoreManagerListener> userStoreManagerListenerCollection;
    private static Collection<UserOperationEventListener> userOperationEventListenerCollection;
    private static Map<Integer, UserStoreManagerListener> userStoreManagerListeners;
    private static Map<Integer, UserOperationEventListener> userOperationEventListeners;

    protected void activate(ComponentContext context) {
        try {
            bundleContext = context.getBundleContext();

            ServiceRegistration userAccountConnectorSR = bundleContext.registerService(
                    UserAccountConnector.class.getName(), UserAccountConnectorImpl.getInstance(), null);
            if (userAccountConnectorSR != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Identity account connector service component activated successfully.");
                }
            } else {
                log.error("Identity account connector service component activation failed.");
            }

            ServiceRegistration UserOptEventListenerSR = bundleContext.registerService(
                    UserOperationEventListener.class.getName(), new UserOptEventListener(), null);
            if (UserOptEventListenerSR != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Identity account association - UserOptEventListener registered.");
                }
            } else {
                log.error("Identity account association - UserOptEventListener could not be registered.");
            }

        } catch (Exception e) {
            log.error("Failed to activate identity account connector service component ", e);
        }
    }

    protected void deactivate(ComponentContext context) {
        if (log.isDebugEnabled()) {
            log.debug("Identity account connector service component is deactivated ");
        }
    }

    protected void setRealmService(RealmService realmService) {
        IdentityAccountConnectorServiceComponent.realmService = realmService;
    }

    protected void unsetRealmService(RealmService realmService) {
        IdentityAccountConnectorServiceComponent.realmService = null;
    }

    protected void setUserStoreManagerListenerService(
            UserStoreManagerListener userStoreManagerListenerService) {
        userStoreManagerListenerCollection = null;
        if (userStoreManagerListeners == null) {
            userStoreManagerListeners =
                    new TreeMap<Integer, UserStoreManagerListener>();
        }
        userStoreManagerListeners.put(userStoreManagerListenerService.getExecutionOrderId(),
                                      userStoreManagerListenerService);
    }

    protected void unsetUserStoreManagerListenerService(
            UserStoreManagerListener userStoreManagerListenerService) {
        if (userStoreManagerListenerService != null &&
            userStoreManagerListeners != null) {
            userStoreManagerListeners.remove(userStoreManagerListenerService.getExecutionOrderId());
            userStoreManagerListenerCollection = null;
        }
    }

    protected void setUserOperationEventListenerService(
            UserOperationEventListener userOperationEventListenerService) {
        userOperationEventListenerCollection = null;
        if (userOperationEventListeners == null) {
            userOperationEventListeners = new TreeMap<Integer, UserOperationEventListener>();
        }
        userOperationEventListeners.put(userOperationEventListenerService.getExecutionOrderId(),
                                        userOperationEventListenerService);
    }

    protected void unsetUserOperationEventListenerService(
            UserOperationEventListener userOperationEventListenerService) {
        if (userOperationEventListenerService != null &&
            userOperationEventListeners != null) {
            userOperationEventListeners.remove(userOperationEventListenerService.getExecutionOrderId());
            userOperationEventListenerCollection = null;
        }
    }

    public static RealmService getRealmService() throws Exception {
        if (realmService == null) {
            String msg = "System has not been started properly. Realm Service is null.";
            log.error(msg);
            throw new Exception(msg);
        }
        return realmService;
    }

    public static Collection<UserStoreManagerListener> getUserStoreManagerListeners() {
        if (userStoreManagerListeners == null) {
            userStoreManagerListeners = new TreeMap<Integer, UserStoreManagerListener>();
        }
        if (userStoreManagerListenerCollection == null) {
            userStoreManagerListenerCollection =
                    userStoreManagerListeners.values();
        }
        return userStoreManagerListenerCollection;
    }

    public static Collection<UserOperationEventListener> getUserOperationEventListeners() {
        if (userOperationEventListeners == null) {
            userOperationEventListeners = new TreeMap<Integer, UserOperationEventListener>();
        }
        if (userOperationEventListenerCollection == null) {
            userOperationEventListenerCollection =
                    userOperationEventListeners.values();
        }
        return userOperationEventListenerCollection;
    }

}
