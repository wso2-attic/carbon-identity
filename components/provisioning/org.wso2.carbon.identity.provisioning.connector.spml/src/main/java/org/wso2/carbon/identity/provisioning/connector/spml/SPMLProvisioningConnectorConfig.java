/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.provisioning.connector.spml;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class SPMLProvisioningConnectorConfig implements Serializable {

    private static final long serialVersionUID = 4084821032056382692L;

    private static final Log log = LogFactory.getLog(SPMLProvisioningConnectorConfig.class);
    private Properties configs;

    /**
     * @param configs
     */
    public SPMLProvisioningConnectorConfig(Properties configs) {
        this.configs = configs;
    }

    /**
     * @param key
     * @return
     */
    public String getValue(String key) {
        return this.configs.getProperty(key);
    }

    /**
     * @return
     */
    public List<String> extractAttributes() {
        boolean isDebugEnabled = log.isDebugEnabled();
        if (isDebugEnabled) {
            log.debug("Starting extract Attributes  " + SPMLProvisioningConnectorConfig.class);
        }

        List<String> attributeList = new ArrayList<>();
        Set<Object> keySet = configs.keySet();
        for (Object key : keySet) {
            if (key.toString().startsWith("spml-atribute-name_")) {
                attributeList.add(key.toString());
            }
        }

        if (isDebugEnabled) {
            log.debug("Attributes set : " + attributeList);
            log.debug("Ending extractAttributes  " + SPMLProvisioningConnectorConfig.class);
        }

        return attributeList;
    }

}
