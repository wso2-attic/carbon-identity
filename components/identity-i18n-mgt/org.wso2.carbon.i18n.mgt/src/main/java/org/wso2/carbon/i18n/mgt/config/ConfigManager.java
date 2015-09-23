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

/**
 * This is used to manage the tenant specific Email Template configurations.
 */
public interface ConfigManager {

    /**
     * This loads tenant specific email template configuration.
     *
     * @param tenantId - Tenant Id which the email template configuration belongs.
     * @return The populated email template configuration object.
     */
    Config loadConfig(int tenantId) throws I18nMgtEmailConfigException;

    /**
     * This saves the given email template configuration in specific tenant space.
     *
     * @param config   - Configuration that needs to be saved.
     * @param tenantId - The tenant Id of the tenant that this email template configuration needs to be saved.
     */
    void saveConfig(Config config, int tenantId) throws I18nMgtEmailConfigException;

    /**
     * This adds a new email template configuration in specific tenant space.
     *
     * @param config   - Configuration that needs to be add.
     * @param tenantId - Tenant Id of the tenant that this email template configuration needs to be add.
     */

    void addEmailConfig(Config config, int tenantId) throws I18nMgtEmailConfigException;

    /**
     * This is used to set the specific email template configuration reader impl.
     *
     * @param reader - Configuration reader implementation.
     */
    void setReader(ConfigReader reader);

    /**
     * This sets the specific email template configuration.
     *
     * @param config - Configuration that needs to be set.
     */
    void setConfig(Config config);

    /**
     * This is used to set the resource path in which the email template configuration going to be saved.
     *
     * @param path - path to be saved.
     */
    void setResourcePath(String path);

    /**
     * This is used to set the specific email template configuration writer impl.
     *
     * @param writer - Configuration writer implementation.
     */
    void setWriter(ConfigWriter writer);
}
