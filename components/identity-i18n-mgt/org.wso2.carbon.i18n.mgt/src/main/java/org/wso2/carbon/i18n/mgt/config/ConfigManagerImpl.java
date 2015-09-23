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

package org.wso2.carbon.i18n.mgt.config;

import org.wso2.carbon.i18n.mgt.exceptions.I18nMgtEmailConfigException;

import java.util.Properties;
import java.util.Set;

/**
 * Implementation of the Configuration Manager interface
 * which used to manage the email template configurations.
 */
public class ConfigManagerImpl implements ConfigManager {

    private ConfigReader reader;
    private ConfigWriter writer;
    private Config config;
    private String resourcePath;

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public void setReader(ConfigReader reader) {
        this.reader = reader;
    }

    public void setWriter(ConfigWriter writer) {
        this.writer = writer;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    @Override
    public Config loadConfig(int tenantId) throws I18nMgtEmailConfigException {
        Properties props = reader.read(tenantId, resourcePath);
        if (!props.isEmpty()) {
            Set<String> keySet = props.stringPropertyNames();
            for (String key : keySet) {
                this.config.setProperty(key, props.getProperty(key));
            }
        } else {
            return null;
        }

        return this.config;
    }

    @Override
    public void saveConfig(Config config, int tenantId) throws I18nMgtEmailConfigException {
        Properties props = config.getProperties();
        this.writer.write(tenantId, props, resourcePath);
    }

    @Override
    public void addEmailConfig(Config config, int tenantId) throws I18nMgtEmailConfigException {
        Properties props = config.getProperties();
        this.writer.write(tenantId, props, resourcePath);
    }
}
