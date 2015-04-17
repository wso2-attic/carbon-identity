/*
*  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.security.util;


import org.apache.axis2.description.*;
import org.wso2.carbon.core.RegistryResources;

import java.util.ArrayList;
import java.util.ListIterator;

public class ServicePersistenceUtil {

    private static final String PROXY_SERVICE = "proxy";

    /**
     * Get Resource path from service
     * @param service Axis Service
     * @return Resource path
     */
    public static String getResourcePath(AxisService service) {

        return RegistryResources.SERVICE_GROUPS + service.getAxisServiceGroup().getServiceGroupName()
                + RegistryResources.SERVICES + service.getName();
    }

    /**
     * Get resource path
     *
     * @param operation Axis operation
     * @return resource path
     */
    public static String getResourcePath(AxisOperation operation) {

        AxisService service = operation.getAxisService();
        return RegistryResources.SERVICE_GROUPS
                + service.getAxisServiceGroup().getServiceGroupName() + RegistryResources.SERVICES
                + service.getName() + RegistryResources.ServiceProperties.OPERATIONS
                + operation.getName().getLocalPart();
    }

    /**
     * Get resource path
     *
     * @param module Axis module
     * @return resource path
     */
    public static String getResourcePath(AxisModule module) {

        String version = RegistryResources.ModuleProperties.UNDEFINED;
        if (module.getVersion() != null) {
            version = module.getVersion().toString();
        }
        return RegistryResources.MODULES + module.getName() + "/" + version;
    }

    /**
     * Check weather axis service is a proxy service
     *
     * @param service Axis Service
     * @return is proxy
     */
    public static boolean isProxyService(AxisService service) {

        ArrayList axisServiceParameters = service.getParameters();
        ListIterator iterator = axisServiceParameters.listIterator();
        boolean isProxyService = false;

        while (iterator.hasNext()) {
            Parameter elem = (Parameter) (iterator.next());
            Object value = elem.getValue();
            if (value != null && PROXY_SERVICE.equals(value.toString())) {
                isProxyService = true;
            }
        }
        return isProxyService;
    }

    /**
     * Get Binding operation path
     *
     * @param servicePath Service path
     * @param abo         Axis binding operation
     * @return service path
     */
    public static String getBindingOperationPath(String servicePath, AxisBindingOperation abo) {
        AxisBinding binding = abo.getAxisBinding();
        return servicePath + RegistryResources.ServiceProperties.BINDINGS
                + binding.getName().getLocalPart() + RegistryResources.ServiceProperties.OPERATIONS
                + abo.getName().getLocalPart();
    }

}
