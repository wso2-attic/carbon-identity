/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.scim.common.config;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.scim.common.utils.IdentitySCIMException;
import org.wso2.charon.core.config.SCIMConfig;
import org.wso2.charon.core.config.SCIMConfigConstants;
import org.wso2.charon.core.config.SCIMConsumer;
import org.wso2.charon.core.config.SCIMProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SCIMProvisioningConfigManager {

    private static Log logger = LogFactory.getLog(SCIMProvisioningConfigManager.class.getName());
    /*In this cache, we maintain a map of scimConsumerId to enable true/false*/

    /*Changing to in - memory map*/
    private static Map<String, Boolean> scimCache = new HashMap<>();

    private static SCIMProvisioningConfigManager configManager = null;
    private static SCIMConfig scimConfig;
    //keep the extracted connector list in an array to avoid perf bottleneck in string manipulation
    private static String[] provisioningConnectorClasses;

    private SCIMProvisioningConfigManager(){}

    public static SCIMProvisioningConfigManager getInstance() {
        if (configManager == null) {
            synchronized (SCIMProvisioningConfigManager.class) {
                if (configManager == null) {
                    configManager = new SCIMProvisioningConfigManager();
                    return configManager;
                }
                return configManager;
            }
        }
        return configManager;
    }

    public static SCIMConfig getSCIMConfig() {
        return scimConfig;
    }

    public static void setSCIMConfig(SCIMConfig scimConfiguration) {
        scimConfig = scimConfiguration;
    }

    public static boolean isConsumerRegistered(String consumerName) {

        //From SCIMProviderDAO and check if exists.
        boolean isConsumerExisting = false;
        try {
            if (MapUtils.isNotEmpty(scimCache) && scimCache.containsKey(consumerName)) {
                isConsumerExisting = scimCache.get(consumerName);
                return isConsumerExisting;
            }
            SCIMProviderDAO scimDAO = new SCIMProviderDAO();
            isConsumerExisting = scimDAO.isExistingConsumer(consumerName);
            scimCache.put(consumerName, isConsumerExisting);
            return isConsumerExisting;
        } catch (IdentitySCIMException e) {
            logger.error("Error when checking whether user exists or not.", e);
            return false;
        }

    }

    public static SCIMConsumer getSCIMConsumerConfig(String consumerName)
            throws IdentitySCIMException {
        //TODO:for perf improvements, we can think of using cache with out retrieving from cache every time
        SCIMProviderDAO scimProviderDAO = new SCIMProviderDAO();
        List<SCIMProviderDTO> scimProviders = scimProviderDAO.getAllProviders(consumerName);
        //scim consumer to be returned
        SCIMConsumer scimConsumer = new SCIMConsumer();
        Map<String, SCIMProvider> scimProviderMap = new HashMap<>();

        if (CollectionUtils.isNotEmpty(scimProviders)) {
            for (SCIMProviderDTO scimProvider : scimProviders) {
                SCIMProvider currentProvider = new SCIMProvider();
                currentProvider.setId(scimProvider.getProviderId());
                currentProvider.setProperty(SCIMConfigConstants.ELEMENT_NAME_USERNAME,
                        scimProvider.getUserName());
                currentProvider.setProperty(SCIMConfigConstants.ELEMENT_NAME_PASSWORD,
                        scimProvider.getPassword());
                currentProvider.setProperty(SCIMConfigConstants.ELEMENT_NAME_USER_ENDPOINT,
                        scimProvider.getUserEPURL());
                currentProvider.setProperty(SCIMConfigConstants.ELEMENT_NAME_GROUP_ENDPOINT,
                        scimProvider.getGroupEPURL());
                scimProviderMap.put(scimProvider.getProviderId(), currentProvider);
            }
            scimConsumer.setScimProviders(scimProviderMap);
        } else {
            //throw error message.
            throw new IdentitySCIMException("No SCIM providers registered for the given consumer id: "
                    + consumerName);
        }
        return scimConsumer;
    }

    public static void addEnabledToCache(String consumerId) {
        scimCache.put(consumerId, true);
    }

    public static void removeEnabledFromCache(String consumerId) throws IdentitySCIMException {
        //send cache invalidation messages.
        if (MapUtils.isNotEmpty(scimCache) && scimCache.containsKey(consumerId)) {
            scimCache.remove(consumerId);
        }
    }

    public static boolean isDumbMode() {
        return scimConfig.isDumbMode();
    }

    public static String[] getProvisioningHandlers() {
        if (provisioningConnectorClasses == null) {
            synchronized (SCIMProvisioningConfigManager.class) {
                if (provisioningConnectorClasses == null) {
                    String provisioningHandlerString = scimConfig.getProvisioningHandler();
                    provisioningConnectorClasses = provisioningHandlerString.split(",");
                    return provisioningConnectorClasses;
                } else {
                    return provisioningConnectorClasses;
                }
            }
        } else {
            return provisioningConnectorClasses;
        }
    }
}