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

package org.wso2.carbon.identity.mgt.util;

import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.mgt.IdentityMgtConfig;
import org.wso2.carbon.identity.mgt.beans.TenantConfigBean;
import org.wso2.carbon.identity.mgt.cache.CacheBackedConfig;
import org.wso2.carbon.identity.mgt.internal.IdentityMgtServiceComponent;
import org.wso2.carbon.user.api.Claim;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.util.*;

/**
 * This is the Utility class used by the admin service to read and write
 * identity data.
 *
 * @author sga
 */
public class UserIdentityManagementUtil {

    /**
     * Get the configurations of a tenant from cache or database
     *
     * @param tenantId Id of the tenant
     * @return Configurations belong to the tenant
     */
    public static HashMap<String, String> getAllConfigurations(int tenantId) {

        CacheBackedConfig cacheBackedConfig = new CacheBackedConfig();
        HashMap<String, String> configurations = cacheBackedConfig.getConfig(tenantId);

        return configurations;
    }

    /**
     * Store the configurations of a tenant in cache and database
     *
     * @param tenantId             Id of the tenant
     * @param configurationDetails Configurations belong to the tenant
     */
    public static void setAllConfigurations(int tenantId, HashMap<String, String> configurationDetails) {

        HashMap<String, String> configurationDetailsDB = getAllConfigurations(tenantId);

        for (Map.Entry<String, String> entrydb : configurationDetailsDB.entrySet()) {

            boolean isConfigExists = false;

            for (Map.Entry<String, String> entry : configurationDetails.entrySet()) {

                if (entry.getKey().equals(entrydb.getKey())) {

                    configurationDetails.put(entry.getKey(), entry.getValue());
                    isConfigExists = true;
                    break;
                }
            }

            if (!isConfigExists) {
                configurationDetails.put(entrydb.getKey(), entrydb.getValue());
            }
        }

        CacheBackedConfig cacheBackedConfig = new CacheBackedConfig();
        TenantConfigBean tenantConfigBean = new TenantConfigBean(tenantId, configurationDetails);
        cacheBackedConfig.updateConfig(tenantConfigBean);

        Properties properties = new Properties();

        for (Map.Entry<String, String> configEntry : configurationDetails.entrySet()) {
            properties.put(configEntry.getKey(), configEntry.getValue());
        }

        IdentityMgtConfig identityMgtConfig = IdentityMgtConfig.getInstance();
        identityMgtConfig.setConfigurations(IdentityMgtServiceComponent.getRealmService().getBootstrapRealmConfiguration(), properties);
    }
}
