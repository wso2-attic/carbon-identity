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

import java.util.Properties;

/**
 * This is used to manage the properties of Email Notification object.
 */
public interface Config {

    /**
     * This loads the specific property value of the email notification configuration object.
     *
     * @param key - Specific property to be retrieved.
     * @return Property value of the target email notification configuration object.
     */
    String getProperty(String key);

    /**
     * This sets the specific email notification configuration object.
     *
     * @param key   - Key of the specific property value to be set.
     * @param value - Property value to be set.
     */
    void setProperty(String key, String value);

    /**
     * This loads all the property values of the email notification configuration object.
     *
     * @return Property values of the email notification configuration object.
     */
    Properties getProperties();

    /**
     * This sets a specific property object for the email notification configuration object.
     *
     * @param props - Property object value to be set.
     */
    void setProperties(Properties props);

}
