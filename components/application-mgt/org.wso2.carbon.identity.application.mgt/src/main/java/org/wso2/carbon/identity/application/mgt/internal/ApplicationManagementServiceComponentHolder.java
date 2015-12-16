/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.application.mgt.internal;

import org.wso2.carbon.identity.application.mgt.AbstractInboundAuthenticatorConfig;
import org.wso2.carbon.registry.api.RegistryService;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.ConfigurationContextService;

import java.util.HashMap;
import java.util.Map;

public class ApplicationManagementServiceComponentHolder {

    private static ApplicationManagementServiceComponentHolder instance=new
            ApplicationManagementServiceComponentHolder();
    private static Map<String, AbstractInboundAuthenticatorConfig> inboundAuthenticatorConfigs = new HashMap<String, AbstractInboundAuthenticatorConfig>();
    private static Map<String, AbstractInboundAuthenticatorConfig> inboundProvisioningConfigs = new HashMap<String, AbstractInboundAuthenticatorConfig>();


    private RegistryService registryService;

    private RealmService realmService;

    private ConfigurationContextService configContextService;

    private ApplicationManagementServiceComponentHolder(){
    }

    public static ApplicationManagementServiceComponentHolder getInstance(){return instance;}

    /**
     * Add inbound authenticator configuration
     * @param inboundAuthenticator
     */
    public static void addInboundAuthenticatorConfig(AbstractInboundAuthenticatorConfig inboundAuthenticator) {
        inboundAuthenticatorConfigs.put(inboundAuthenticator.getAuthKey(), inboundAuthenticator);
    }

    /**
     * Get inbound authenticator configuration
     * @param type
     * @return
     */
    public static AbstractInboundAuthenticatorConfig getInboundAuthenticatorConfig(String type) {
        return inboundAuthenticatorConfigs.get(type);
    }

    /**
     * Get inbound authenticator configurations
     * @return inbound authenticator configs
     */
    public static Map<String, AbstractInboundAuthenticatorConfig> getAllInboundAuthenticatorConfig() {
        return inboundAuthenticatorConfigs;
    }

    /**
     * Remove inbound authenticator configuration
     * @param type
     */
    public static void removeInboundAuthenticatorConfig(String type) {
        inboundAuthenticatorConfigs.remove(type);
    }


    /**
     * Add inbound provisioning configuration
     * @param inboundProvisioningConnector
     */
    public static void addInboundProvisioningConfig(AbstractInboundAuthenticatorConfig inboundProvisioningConnector) {
        inboundProvisioningConfigs.put(inboundProvisioningConnector.getAuthKey(), inboundProvisioningConnector);
    }

    /**
     * Get inbound provisioning configuration
     * @param type
     * @return
     */
    public static AbstractInboundAuthenticatorConfig getInboundProvisioningConfig(String type) {
        return inboundProvisioningConfigs.get(type);
    }

    /**
     * Get inbound provisioning configurations
     * @return inbound provisioning configs
     */
    public static Map<String, AbstractInboundAuthenticatorConfig> getAllInboundProvisioningConfig() {
        return inboundAuthenticatorConfigs;
    }

    /**
     * Remove inbound provisioning configuration
     * @param type
     */
    public static void removeInboundProvisioningConfig(String type) {
        inboundProvisioningConfigs.remove(type);
    }


    public RegistryService getRegistryService() {
        return registryService;
    }

    public void setRegistryService(RegistryService registryService) {
        this.registryService = registryService;
    }

    public RealmService getRealmService() {
        return realmService;
    }

    public void setRealmService(RealmService realmService) {
        this.realmService = realmService;
    }

    public ConfigurationContextService getConfigContextService() {
        return configContextService;
    }

    public void setConfigContextService(
            ConfigurationContextService configContextService) {
        this.configContextService = configContextService;
    }


}
