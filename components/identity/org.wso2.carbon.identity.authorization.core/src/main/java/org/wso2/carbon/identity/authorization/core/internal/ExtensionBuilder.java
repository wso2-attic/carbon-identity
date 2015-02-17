/*
*  Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.carbon.identity.authorization.core.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.authorization.core.extension.PostAuthorizationExtension;
import org.wso2.carbon.identity.authorization.core.permission.CarbonPermissionFinderModule;
import org.wso2.carbon.identity.authorization.core.permission.PermissionFinderModule;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.*;
import java.util.Properties;

/**
 *
 */
public class ExtensionBuilder {

    private static final String ENTITLEMENT_CONFIG = "entitlement.properties";

    private static Log log = LogFactory.getLog(ExtensionBuilder.class);

    public void buildAuthorizationConfig(AuthorizationConfigHolder holder) throws Exception {

        Properties properties;

        if ((properties = loadProperties()) != null) {
            populatePermissionFinders(properties, holder);
            populatePostExtensions(properties, holder);
        }
    }

    private void populatePermissionFinders(Properties properties, AuthorizationConfigHolder holder)
            throws Exception {
        int i = 1;
        PermissionFinderModule finderModule = null;

        while (properties.getProperty("Authorization.Permission.Finder." + i) != null) {
            String className = properties.getProperty("Authorization.Permission.Finder." + i++);
            Class clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
            finderModule = (PermissionFinderModule) clazz.newInstance();

            int j = 1;
            Properties designatorProps = new Properties();
            while (properties.getProperty(className + "." + j) != null) {
                String[] props = properties.getProperty(className + "." + j++).split(",");
                designatorProps.put(props[0], props[1]);
            }

            finderModule.init(designatorProps);
            holder.addPermissionFinderModule(finderModule, designatorProps);
        }

        if (holder.getPermissionFinderModules().size() == 0) {
            CarbonPermissionFinderModule defaultModule = new CarbonPermissionFinderModule();
            // init is not needed for default module
            holder.addPermissionFinderModule(defaultModule, new Properties());
        }
    }

    private void populatePostExtensions(Properties properties, AuthorizationConfigHolder holder)
            throws Exception {
        int i = 1;
        PostAuthorizationExtension finderModule = null;

        while (properties.getProperty("Authorization.Post.Extension." + i) != null) {
            String className = properties.getProperty("Authorization.Post.Extension." + i++);
            Class clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
            finderModule = (PostAuthorizationExtension) clazz.newInstance();

            int j = 1;
            Properties designatorProps = new Properties();
            while (properties.getProperty(className + "." + j) != null) {
                String[] props = properties.getProperty(className + "." + j++).split(",");
                designatorProps.put(props[0], props[1]);
            }

            finderModule.init(designatorProps);
            holder.addPostExtensions(finderModule, designatorProps);
        }
    }

    /**
     * @return
     * @throws java.io.IOException
     */
    private Properties loadProperties() {

        Properties properties = new Properties();
        InputStream inStream = null;

        File pipConfigXml = new File(CarbonUtils.getCarbonSecurityConfigDirPath(), ENTITLEMENT_CONFIG);

        try {
            if (pipConfigXml.exists()) {
                inStream = new FileInputStream(pipConfigXml);
            } else {
                return properties;
            }

            properties.load(inStream);
        } catch (FileNotFoundException e) {
            log.error(e.getMessage(), e);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } finally {
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }

        return properties;
    }
}
