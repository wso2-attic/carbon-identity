/*
 *
 * Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
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
 * /
 */

package org.wso2.carbon.identity.uma.internal;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.identity.uma.UMAService;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.service.RealmService;/**
 * @scr.component name="org.wso2.carbon.identity.uma.internal.UMAServiceComponent" immediate="true" activate="activate"
 * @scr.reference name="registry.service"
 * interface="org.wso2.carbon.registry.core.service.RegistryService"
 * cardinality="1..1" policy="static" bind="setRegistryService"
 * unbind="unsetRegistryService"
 * @scr.reference name="user.realmservice.default"
 * interface="org.wso2.carbon.user.core.service.RealmService" cardinality="1..1"
 * policy="static" bind="setRealmService" unbind="unsetRealmService"
 */
public class UMAServiceComponent {
    private static Log log = LogFactory.getLog(UMAServiceComponent.class);
    private static BundleContext bundleContext;


    protected void activate(ComponentContext context){
        // register UMAService as an OSGIService
        bundleContext = context.getBundleContext();
        bundleContext.registerService(UMAService.class.getName(),new UMAService(),null);


        if (log.isDebugEnabled()){
            log.debug("Identity User Managed Access bundle activated");
        }
    }

    protected void deactivate(ComponentContext context){
        if (log.isDebugEnabled()){
            log.debug("Identity User Managed Access bundle deactivated");
        }
    }

    protected void setRegistryService(RegistryService registryService) {
        if (log.isDebugEnabled()) {
            log.info("RegistryService set in Identity UMA bundle");
        }
        UMAServiceComponentHolder.setRegistryService(registryService);
    }

    protected void unsetRegistryService(RegistryService registryService) {
        if (log.isDebugEnabled()) {
            log.info("RegistryService unset in Identity UMA bundle");
        }
        UMAServiceComponentHolder.setRegistryService(null);
    }

    protected void setRealmService(RealmService realmService) {
        if (log.isDebugEnabled()) {
            log.info("Setting the Realm Service");
        }
        UMAServiceComponentHolder.setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService) {
        if (log.isDebugEnabled()) {
            log.info("Unsetting the Realm Service");
        }
        UMAServiceComponentHolder.setRealmService(null);
    }



}
