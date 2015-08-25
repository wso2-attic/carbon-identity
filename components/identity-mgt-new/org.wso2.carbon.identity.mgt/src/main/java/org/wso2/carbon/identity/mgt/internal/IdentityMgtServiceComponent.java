/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.mgt.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.identity.mgt.IdentityMgtConfig;
import org.wso2.carbon.identity.mgt.IdentityMgtConfigGlobal;
import org.wso2.carbon.identity.mgt.IdentityMgtConstants;
import org.wso2.carbon.identity.mgt.IdentityMgtException;
import org.wso2.carbon.identity.mgt.listener.IdentityMgtEventListener;
import org.wso2.carbon.identity.mgt.handler.EventHandler;
import org.wso2.carbon.identity.mgt.handler.internal.AccountLockEventHandler;
import org.wso2.carbon.identity.mgt.listener.TenantCreationEventListener;
import org.wso2.carbon.identity.mgt.policy.PolicyEnforcer;
import org.wso2.carbon.identity.mgt.store.UserIdentityDataStore;
import org.wso2.carbon.stratos.common.listeners.TenantMgtListener;
import org.wso2.carbon.user.core.listener.UserOperationEventListener;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * @scr.component name="org.wso2.carbon.identity.mgt.internal.IdentityMgtServiceComponent"
 * immediate="true
 * @scr.reference name="realm.service"
 * interface="org.wso2.carbon.user.core.service.RealmService"cardinality="1..1"
 * policy="dynamic" bind="setRealmService" unbind="unsetRealmService"
 * @scr.reference name="event.handler"
 * interface="org.wso2.carbon.identity.mgt.handler.EventHandler"
 * cardinality="0..n" policy="dynamic"
 * bind="registerEventHandler"
 * unbind="unRegisterEventHandler"
 * @scr.reference name="listener.TenantMgtListener"
 * interface="org.wso2.carbon.stratos.common.listeners.TenantMgtListener"
 * cardinality="0..n" policy="dynamic"
 * bind="registerTenantMgtListener" unbind="unRegisterTenantMgtListener"
 */

public class IdentityMgtServiceComponent {

    private static Log log = LogFactory.getLog(IdentityMgtServiceComponent.class);

    private static RealmService realmService;

    private ServiceRegistration serviceRegistration = null;

    private static IdentityMgtEventListener listener = null;

    // list of all registered event handlers
    public static List<EventHandler> eventHandlerList = new ArrayList<>();

    protected void activate(ComponentContext context) {

        context.getBundleContext().registerService(EventHandler.class.getName(),
                new AccountLockEventHandler(), null);
        context.getBundleContext().registerService(TenantMgtListener.class.getName(),
                new TenantCreationEventListener(), null);
        init();
        listener = new IdentityMgtEventListener();
        serviceRegistration =
                context.getBundleContext().registerService(UserOperationEventListener.class.getName(),
                        listener, null);
        if (log.isDebugEnabled()) {
            log.debug("Identity Management Listener is enabled");
        }
    }


    protected void deactivate(ComponentContext context) {
        if (log.isDebugEnabled()) {
            log.debug("Identity Management bundle is de-activated");
        }
    }

    protected void registerEventHandler(EventHandler eventHandler) throws IdentityMgtException {
        eventHandler.init();
        eventHandlerList.add(eventHandler);
    }

    protected void unRegisterEventHandler(EventHandler eventHandler) {

    }

    protected void registerTenantMgtListener(TenantMgtListener tenantMgtListener) {
    }

    protected void unRegisterTenantMgtListener(TenantMgtListener tenantMgtListener) {
    }

    protected void setRealmService(RealmService realmService) {
        if (log.isDebugEnabled()) {
            log.debug("Setting the Realm Service");
        }
        IdentityMgtServiceComponent.realmService = realmService;
    }

    protected void unsetRealmService(RealmService realmService) {
        if (log.isDebugEnabled()) {
            log.debug("UnSetting the Realm Service");
        }
        IdentityMgtServiceComponent.realmService = null;
    }

    public static RealmService getRealmService() {
        return realmService;
    }

    private void init() {
        try {
            IdentityMgtConfig identityMgtConfig = new IdentityMgtConfig();
            Properties properties = identityMgtConfig.addConfiguration(MultitenantConstants.SUPER_TENANT_ID);
            IdentityMgtConfigGlobal.getInstance().setGlobalConfiguration(properties);
        } catch (IdentityMgtException ex) {
            log.error("Error when storing super tenant configurations.");
        }
    }
}
