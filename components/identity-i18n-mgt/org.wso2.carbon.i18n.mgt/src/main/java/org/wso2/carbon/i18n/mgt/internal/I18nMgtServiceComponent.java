/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
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

package org.wso2.carbon.i18n.mgt.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.i18n.mgt.config.ConfigBuilder;
import org.wso2.carbon.i18n.mgt.config.ConfigType;
import org.wso2.carbon.i18n.mgt.config.StorageType;
import org.wso2.carbon.i18n.mgt.exceptions.I18nMgtEmailConfigException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.stratos.common.listeners.TenantMgtListener;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.ConfigurationContextService;


/**
 * @scr.component name="I18nMgtServiceComponent"
 * immediate="true"
 * @scr.reference name="registry.service"
 * interface="org.wso2.carbon.registry.core.service.RegistryService" cardinality="1..1"
 * policy="dynamic" bind="setRegistryService" unbind="unsetRegistryService"
 * @scr.reference name="realm.service"
 * interface="org.wso2.carbon.user.core.service.RealmService"cardinality="1..1"
 * policy="dynamic" bind="setRealmService" unbind="unsetRealmService"
 */

public class I18nMgtServiceComponent {

    private static Log log = LogFactory.getLog(I18nMgtServiceComponent.class);

    private static RealmService realmService;

    private static RegistryService registryService;

    private static ConfigurationContextService configurationContextService;
    private ServiceRegistration serviceRegistration = null;

    public static RealmService getRealmService() {
        return realmService;
    }

    protected void setRealmService(RealmService realmService) {
        log.debug("Setting the Realm Service");
        I18nMgtServiceComponent.realmService = realmService;
    }

    public static RegistryService getRegistryService() {
        return registryService;
    }

    protected void setRegistryService(RegistryService registryService) {
        log.debug("Setting the Registry Service");
        I18nMgtServiceComponent.registryService = registryService;
    }

    public static ConfigurationContextService getConfigurationContextService() {
        return configurationContextService;
    }

    protected void setConfigurationContextService(ConfigurationContextService configurationContextService) {
        log.debug("Setting the ConfigurationContext Service");
        I18nMgtServiceComponent.configurationContextService = configurationContextService;

    }

    protected void activate(ComponentContext context) {
        try {
            BundleContext bundleCtx = context.getBundleContext();
            TenantManagementListener idPMgtTenantMgtListener = new TenantManagementListener();
            ServiceRegistration tenantMgtListenerSR = bundleCtx.registerService(
                    TenantMgtListener.class.getName(), idPMgtTenantMgtListener, null);
            if (tenantMgtListenerSR != null) {
                log.debug("I18n Management - TenantMgtListener registered");
            } else {
                log.error("I18n Management - TenantMgtListener could not be registered");
            }
            loadEmailConfigurations();
            log.debug("I18n Management is activated");
        } catch (Throwable e) {
            log.error("Error while activating I18n Management bundle", e);
        }
    }

    private void loadEmailConfigurations() {
        //Load email template configuration on server startup.
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
        ConfigBuilder configBuilder = ConfigBuilder.getInstance();
        try {
            configBuilder.loadDefaultConfiguration(ConfigType.EMAIL, StorageType.REGISTRY, tenantId);
        } catch (I18nMgtEmailConfigException e) {
            log.error("Error occurred while loading default email templates", e);
        }
    }

    protected void deactivate(ComponentContext context) {
        log.debug("I18n Management bundle is de-activated");
    }

    protected void unsetRegistryService(RegistryService registryService) {
        log.debug("UnSetting the Registry Service");
        I18nMgtServiceComponent.registryService = null;
    }

    protected void unsetRealmService(RealmService realmService) {
        log.debug("UnSetting the Realm Service");
        I18nMgtServiceComponent.realmService = null;
    }

    protected void unsetConfigurationContextService(ConfigurationContextService configurationContextService) {
        log.debug("UnSetting the  ConfigurationContext Service");
        I18nMgtServiceComponent.configurationContextService = null;
    }

}