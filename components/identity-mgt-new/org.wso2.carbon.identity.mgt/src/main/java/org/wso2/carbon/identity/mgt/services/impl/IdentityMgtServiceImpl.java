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

package org.wso2.carbon.identity.mgt.services.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.mgt.beans.TenantConfigBean;
import org.wso2.carbon.identity.mgt.cache.CacheBackedConfig;
import org.wso2.carbon.identity.mgt.constants.IdentityMgtConstants;
import org.wso2.carbon.identity.mgt.event.IdentityMgtEvent;
import org.wso2.carbon.identity.mgt.handler.EventHandler;
import org.wso2.carbon.identity.mgt.internal.IdentityMgtServiceComponent;
import org.wso2.carbon.identity.mgt.services.IdentityMgtService;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.*;
import java.util.*;

public class IdentityMgtServiceImpl implements IdentityMgtService {

    Log log = LogFactory.getLog(IdentityMgtServiceImpl.class);

    /**
     * Add configurations in identity-mgt.properties file into database and cache
     */
    @Override
    public Properties addConfigurations(int tenantId) {

        Properties properties = new Properties();
        InputStream inStream = null;

        // key - property name
        // Value - property value
        HashMap<String, String> configurationDetails = new HashMap<String, String>();


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

                TenantConfigBean tenantConfigBean = new TenantConfigBean(tenantId, configurationDetails);

                CacheBackedConfig cacheBackedConfig = new CacheBackedConfig();
                cacheBackedConfig.addConfig(tenantConfigBean);

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
     * Retrieve configurations for the tenant ID
     */
    @Override
    public Properties getConfigurations(int tenantId) {

        Properties properties = new Properties();
        InputStream inStream = null;
        HashMap<String, String> configMap = new HashMap<String, String>();

        CacheBackedConfig cacheBackedConfig = new CacheBackedConfig();
        configMap = cacheBackedConfig.getConfig(tenantId);

        Iterator<Map.Entry<String, String>> iterator = configMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> pair = iterator.next();
            properties.setProperty(pair.getKey(), pair.getValue());
            System.out.println(pair.getKey() + " = " + pair.getValue());
        }

        return properties;
    }

    /**
     * Handle event by relevant event handlers based on the event name
     */
    @Override
    public boolean handleEvent(IdentityMgtEvent identityMgtEvent) throws UserStoreException {

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
