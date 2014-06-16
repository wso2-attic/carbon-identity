/*
*  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.claim.mgt.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * @scr.component name="claim.mgt.component" immediate="true"
 * @scr.reference name="registry.service"
 * interface="org.wso2.carbon.registry.core.service.RegistryService"
 * cardinality="1..1" policy="dynamic" bind="setRegistryService"
 * unbind="unsetRegistryService"
 * @scr.reference name="user.realmservice.default"
 * interface="org.wso2.carbon.user.core.service.RealmService" cardinality="1..1"
 * policy="dynamic" bind="setRealmService" unbind="unsetRealmService"
 */

public class ClaimManagementServiceComponent {

    private static Log log = LogFactory.getLog(ClaimManagementServiceComponent.class);

    private static BundleContext bundleContext = null;

    private static RealmService realmService = null;

    private static RegistryService registryService = null;

    public ClaimManagementServiceComponent() {

    }

    /**
     * @param ctxt
     */
    protected void activate(ComponentContext ctxt) {
        try {
            bundleContext = ctxt.getBundleContext();
        } catch (Throwable e) {
            log.error("Error occurred while activating Claim Management Service Component", e);
        }
        if (log.isDebugEnabled()) {
            log.debug("Claim Management Core bundle is activated");
        }
    }

    /**
     * @param ctxt
     */
    protected void deactivate(ComponentContext ctxt) {
        if (log.isDebugEnabled()) {
            log.debug("Claim Management bundle is deactivated");
        }
    }

    protected void setRegistryService(RegistryService registryService) {
        ClaimManagementServiceComponent.registryService = registryService;
        if (log.isDebugEnabled()) {
            log.debug("RegistryService set in Claim Management bundle");
        }
    }

    protected void unsetRegistryService(RegistryService registryService) {
        ClaimManagementServiceComponent.registryService = null;
        if (log.isDebugEnabled()) {
            log.debug("RegistryService unset in Claim Management bundle");
        }
    }

    /**
     * @param realmService
     */
    protected void setRealmService(RealmService realmService) {
        ClaimManagementServiceComponent.realmService = realmService;
        if (log.isDebugEnabled()) {
            log.debug("RealmService set in Claim Management bundle");
        }
    }

    /**
     * @param realmService
     */
    protected void unsetRealmService(RealmService realmService) {
        ClaimManagementServiceComponent.realmService = null;
        if (log.isDebugEnabled()) {
            log.debug("RealmService unset in Claim Management bundle");
        }
    }

    public static BundleContext getBundleContext() {
        return bundleContext;
    }

    public static RealmService getRealmService() {
        return realmService;
    }

    public static RegistryService getRegistryService(){
        return registryService;
    }

}
