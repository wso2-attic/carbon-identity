/*
 * Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.mgt.cache;

import org.wso2.carbon.identity.mgt.beans.TenantConfigBean;
import org.wso2.carbon.identity.mgt.dao.impl.ConfigurationDAOImpl;

import java.util.HashMap;

public class CacheBackedConfig {

    private CacheByTenantId cacheByTenantId = null;
    private ConfigurationDAOImpl configurationDAO = null;

    public CacheBackedConfig() {
        cacheByTenantId = CacheByTenantId.getInstance();
        configurationDAO = new ConfigurationDAOImpl();
    }

    /**
     * Add configurations in cache and database if not exists
     *
     * @param tenantConfigBean
     */
    public void addConfig(TenantConfigBean tenantConfigBean) {

        configurationDAO.addConfigurations(tenantConfigBean);

        ConfigTenantIdCacheKey cacheKey = new ConfigTenantIdCacheKey(tenantConfigBean.getTenantId());

        cacheByTenantId.addToCache(cacheKey, new ConfigCacheEntry(tenantConfigBean.getConfigurationDetails()));

    }

    /**
     * Get configurations from cache if exists or get from database
     * @param tenantId  Tenant ID
     * @return
     */
    public HashMap<String, String> getConfig(int tenantId) {

        ConfigTenantIdCacheKey cacheKey = new ConfigTenantIdCacheKey(tenantId);
        ConfigCacheEntry entry = (ConfigCacheEntry) cacheByTenantId.getValueFromCache(cacheKey);

        if (entry != null && !entry.getConfigurationDetails().isEmpty()) {
            return entry.getConfigurationDetails();

        }

        TenantConfigBean tenantConfigBean = configurationDAO.getConfigurations(tenantId);

        HashMap<String, String> configurationDetails = tenantConfigBean.getConfigurationDetails();
        if (configurationDetails != null) {
            cacheByTenantId.addToCache(cacheKey, new ConfigCacheEntry(configurationDetails));
        }

        return configurationDetails;
    }


    /**
     * Update configurations in cache and database
     * @param tenantConfigBean
     */
    public void updateConfig(TenantConfigBean tenantConfigBean) {

        configurationDAO.updateConfigurations(tenantConfigBean);

        ConfigTenantIdCacheKey cacheKey = new ConfigTenantIdCacheKey(tenantConfigBean.getTenantId());
        ConfigCacheEntry entry = (ConfigCacheEntry) cacheByTenantId.getValueFromCache(cacheKey);

        if (entry != null) {
            cacheByTenantId.clearCacheEntry(cacheKey);
            cacheByTenantId.addToCache(cacheKey, new ConfigCacheEntry(tenantConfigBean.getConfigurationDetails()));
        }
    }
}
