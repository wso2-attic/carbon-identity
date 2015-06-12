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

package org.wso2.carbon.identity.mgt.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.wso2.carbon.identity.mgt.IdentityMgtConfig;
import org.wso2.carbon.identity.mgt.IdentityMgtException;
import org.wso2.carbon.identity.mgt.bean.TenantConfiguration;
import org.wso2.carbon.identity.mgt.cache.CacheBackedConfig;
import org.wso2.carbon.identity.mgt.IdentityMgtConstants;
import org.wso2.carbon.identity.mgt.event.IdentityMgtEvent;
import org.wso2.carbon.identity.mgt.handler.EventHandler;
import org.wso2.carbon.identity.mgt.internal.IdentityMgtServiceComponent;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.List;

public class IdentityMgtServiceImpl implements IdentityMgtService {

    Log log = LogFactory.getLog(IdentityMgtServiceImpl.class);

    /**
     * Add configurations in identity-mgt.properties file into database and cache
     */
    @Override
    public Properties addConfiguration(int tenantId) throws IdentityMgtException {

        Properties properties = new Properties();
        InputStream inStream = null;

        // key - property name
        // Value - property value
        HashMap<String, String> configurationDetails = new HashMap<>();


        File pipConfigXml = new File(CarbonUtils.getCarbonSecurityConfigDirPath(),
                IdentityMgtConstants.PropertyConfig.CONFIG_FILE_NAME);
        if (pipConfigXml.exists()) {
            try {
                inStream = new FileInputStream(pipConfigXml);
                properties.load(inStream);

                Enumeration enuKeys = properties.keys();
                while (enuKeys.hasMoreElements()) {
                    String key = (String) enuKeys.nextElement();
                    String value = properties.getProperty(key);
                    configurationDetails.put(key, value);
                }

                TenantConfiguration tenantConfiguration = new TenantConfiguration(tenantId, configurationDetails);

                CacheBackedConfig cacheBackedConfig = CacheBackedConfig.getInstance();
                cacheBackedConfig.addConfig(tenantConfiguration);

            } catch (FileNotFoundException e) {
                log.error("Can not load identity-mgt properties file ", e);
            } catch (IOException e) {
                log.error("Can not load identity-mgt properties file ", e);
            } finally {
                if (inStream != null) {
                    try {
                        inStream.close();
                    } catch (IOException e) {
                        log.error("Error while closing stream ", e);
                    }
                }
            }
        }

        return properties;

    }


    /**
     * Store the configurations of a tenant in cache and database
     *
     * @param tenantId             Id of the tenant
     * @param configurationDetails Configurations belong to the tenant
     */
    public void updateConfiguration(int tenantId, Map<String, String> configurationDetails) throws IdentityMgtException {

        CacheBackedConfig cacheBackedConfig = CacheBackedConfig.getInstance();
        TenantConfiguration tenantConfiguration = new TenantConfiguration(tenantId, configurationDetails);
        cacheBackedConfig.updateConfig(tenantConfiguration);

        Properties properties = new Properties();

        for (Map.Entry<String, String> configEntry : configurationDetails.entrySet()) {
            properties.put(configEntry.getKey(), configEntry.getValue());
        }

        IdentityMgtConfig identityMgtConfig = IdentityMgtConfig.getInstance();
        identityMgtConfig.setConfigurations(IdentityMgtServiceComponent.getRealmService().getBootstrapRealmConfiguration(), properties);
    }


    /**
     * Get the configurations of a tenant from cache or database
     *
     * @param tenantId Id of the tenant
     * @return Configurations belong to the tenant
     */
    @Override
    public Map<String, String> getConfiguration(int tenantId) throws IdentityMgtException {

        Properties properties = new Properties();
        InputStream inStream = null;
        Map<String, String> configMap = new HashMap<>();

        CacheBackedConfig cacheBackedConfig = CacheBackedConfig.getInstance();
        configMap = cacheBackedConfig.getConfig(tenantId);

        return configMap;
    }

    /**
     * Handle event by relevant event handlers based on the event name
     */
    @Override
    public boolean handleEvent(IdentityMgtEvent identityMgtEvent) throws IdentityMgtException {

        List<EventHandler> eventHandlerList = IdentityMgtServiceComponent.eventHandlerList;
        boolean returnValue = true;
        for (final EventHandler handler : eventHandlerList) {
            if (handler.isRegistered(identityMgtEvent)) {
                returnValue = handler.handleEvent(identityMgtEvent);
            }
        }
        return returnValue;
    }
}
