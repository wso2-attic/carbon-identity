/*
 *  Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.carbon.identity.mgt.config;

import org.wso2.carbon.identity.mgt.internal.IdentityMgtServiceComponent;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.exceptions.ResourceNotFoundException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;

import java.util.List;
import java.util.Map;
import java.util.Properties;

public class RegistryConfigReader implements ConfigReader {

    @Override
    public Properties read(int tenantId, String resourcePath) {

        Resource resource = null;
        Properties readerProps = null;
        RegistryService registry = IdentityMgtServiceComponent
                .getRegistryService();

        try {
            UserRegistry userReg = registry.getConfigSystemRegistry(tenantId);
            resource = userReg.get(resourcePath);

            Properties props = resource.getProperties();
            readerProps = new Properties();

            for (Map.Entry<Object, Object> entry : props.entrySet()) {
                String key = (String) entry.getKey();
                List<String> listValue = (List<String>) entry.getValue();
                String value = listValue.get(0);
                readerProps.put(key, value);
            }

        } catch (ResourceNotFoundException re) {
            readerProps = new Properties();

        } catch (RegistryException rnfe) {
            rnfe.printStackTrace();
        }

        return readerProps;
    }

}
