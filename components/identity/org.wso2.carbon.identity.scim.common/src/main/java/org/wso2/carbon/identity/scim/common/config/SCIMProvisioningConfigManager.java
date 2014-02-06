package org.wso2.carbon.identity.scim.common.config;

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
    private static Map<String, Boolean> scimCache = new HashMap<String, Boolean>();

    private static SCIMProvisioningConfigManager configManager = null;
    private static SCIMConfig scimConfig;
    //keep the extracted connector list in an array to avoid perf bottleneck in string manipulation
    private static String[] provisioningConnectorClasses;

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

    public static void setSCIMConfig(SCIMConfig scimConfiguration) {
        scimConfig = scimConfiguration;
    }

    public static SCIMConfig getSCIMConfig() {
        return scimConfig;
    }

    public static boolean isConsumerRegistered(String consumerName) {

        //From SCIMProviderDAO and check if exists.
        boolean isConsumerExisting = false;
        try {
            if (scimCache != null && scimCache.size() > 0 && scimCache.containsKey(consumerName)) {
                isConsumerExisting = scimCache.get(consumerName);
                return isConsumerExisting;
            }
            SCIMProviderDAO scimDAO = new SCIMProviderDAO();
            isConsumerExisting = scimDAO.isExistingConsumer(consumerName);
            scimCache.put(consumerName, isConsumerExisting);
            return isConsumerExisting;
        } catch (IdentitySCIMException e) {
            logger.error(e.getMessage());
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
        Map<String, SCIMProvider> scimProviderMap = new HashMap<String, SCIMProvider>();

        if (scimProviders != null && scimProviders.size() != 0) {
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
        if (scimCache != null && scimCache.size() > 0 && scimCache.containsKey(consumerId)) {
            scimCache.remove(consumerId);
            /*CacheInvalidator cacheInvalidator = SCIMCommonComponent.getCacheInvalidator();
            try {
                cacheInvalidator.invalidateCache(STSConstants.KEY_ISSUER_CONFIG, consumerId);
            } catch (CacheException e) {
                String msg = "Failed to invalidate token from cache";
                logger.error(msg, e);
                throw new IdentitySCIMException(msg, e);
            }*/
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