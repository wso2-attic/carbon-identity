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

package org.wso2.carbon.identity.mgt.cache;

import org.wso2.carbon.identity.mgt.IdentityMgtException;
import org.wso2.carbon.identity.mgt.bean.TenantConfiguration;
import org.wso2.carbon.identity.mgt.dao.ConfigurationDAOImpl;

import java.util.HashMap;
import java.util.Map;

public class CacheBackedConfig {

    private static ResidentIDPConfigCache residentIDPConfigCache = null;
    private static ConfigurationDAOImpl configurationDAO = null;
    private static volatile CacheBackedConfig cacheBackedConfig = null;

    private CacheBackedConfig(){

    }

    public static CacheBackedConfig getInstance() {
        if(cacheBackedConfig == null) {
            synchronized (ConfigurationDAOImpl.class) {
                if(cacheBackedConfig == null) {
                    cacheBackedConfig = new CacheBackedConfig();
                    residentIDPConfigCache = ResidentIDPConfigCache.getInstance();
                    configurationDAO = new ConfigurationDAOImpl();
                }
            }
        }
        return cacheBackedConfig;
    }

    /**
     * Add configurations in cache and database if not exists
     */
    public void addConfig(TenantConfiguration tenantConfiguration) throws IdentityMgtException {

        configurationDAO.addConfiguration(tenantConfiguration);

        ConfigTenantIdCacheKey cacheKey = new ConfigTenantIdCacheKey(tenantConfiguration.getTenantId());

        residentIDPConfigCache.addToCache(cacheKey, new ConfigCacheEntry(tenantConfiguration.getConfigurationDetails()));

    }

    /**
     * Get configurations from cache if exists or get from database
     */
    public Map<String, String> getConfig(int tenantId) throws IdentityMgtException {

        ConfigTenantIdCacheKey cacheKey = new ConfigTenantIdCacheKey(tenantId);
        ConfigCacheEntry entry = (ConfigCacheEntry) residentIDPConfigCache.getValueFromCache(cacheKey);

        if (entry != null && !entry.getConfigurationDetails().isEmpty()) {
            return entry.getConfigurationDetails();

        }

        TenantConfiguration tenantConfiguration = configurationDAO.getConfiguration(tenantId);

        Map<String, String> configurationDetails = tenantConfiguration.getConfigurationDetails();
        if (configurationDetails != null) {
            residentIDPConfigCache.addToCache(cacheKey, new ConfigCacheEntry(configurationDetails));
        }

        return configurationDetails;
    }


    /**
     * Update configurations in cache and database
     */
    public void updateConfig(TenantConfiguration tenantConfiguration) throws IdentityMgtException {

        configurationDAO.updateConfiguration(tenantConfiguration);

        ConfigTenantIdCacheKey cacheKey = new ConfigTenantIdCacheKey(tenantConfiguration.getTenantId());
        ConfigCacheEntry entry = (ConfigCacheEntry) residentIDPConfigCache.getValueFromCache(cacheKey);

        if (entry != null) {
            residentIDPConfigCache.clearCacheEntry(cacheKey);
            residentIDPConfigCache.addToCache(cacheKey, new ConfigCacheEntry(tenantConfiguration.getConfigurationDetails()));
        }
    }
}
