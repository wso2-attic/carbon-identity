/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.sts.internal;

import org.apache.axis2.engine.AxisObserver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.sts.STSDeploymentInterceptor;
import org.wso2.carbon.sts.STSDeploymentListener;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.Axis2ConfigurationContextObserver;

import java.util.Dictionary;
import java.util.Hashtable;

/**
 * @scr.component name="carbon.sts.component" immediate="true"
 * @scr.reference name="registry.service"
 * interface="org.wso2.carbon.registry.core.service.RegistryService"
 * cardinality="1..1" policy="dynamic" bind="setRegistryService"
 * unbind="unsetRegistryService"
 * @scr.reference name="user.realmservice.default"
 * interface="org.wso2.carbon.user.core.service.RealmService"
 * cardinality="1..1" policy="dynamic" bind="setRealmService"
 * unbind="unsetRealmService"
 */
public class STSServiceComponent {
    private static Log log = LogFactory.getLog(STSServiceComponent.class);

    public STSServiceComponent() {
    }

    protected void activate(ComponentContext ctxt) {
        if (log.isDebugEnabled()) {
            log.debug("Carbon STS bundle is activated");
        }
        try {
            BundleContext bundleCtx = ctxt.getBundleContext();
            STSServiceDataHolder.getInstance().setBundle(bundleCtx.getBundle());
            // Publish the OSGi service
            Dictionary props = new Hashtable();
            props.put(CarbonConstants.AXIS2_CONFIG_SERVICE, AxisObserver.class.getName());
            ctxt.getBundleContext().registerService(AxisObserver.class.getName(),
                                                    new STSDeploymentInterceptor(), props);

            // Publish an OSGi service to listen tenant configuration context creation events
            bundleCtx.registerService(Axis2ConfigurationContextObserver.class.getName(),
                                      new STSDeploymentListener(),
                                      null);
        } catch (Throwable e) {
            log.error("Error occurred while updating carbon STS service", e);
        }
    }

    protected void deactivate(ComponentContext ctxt) {
        if (log.isDebugEnabled()) {
            log.debug("Carbon STS bundle is deactivated");
        }
    }

    protected void setRegistryService(RegistryService registryService) {
        if (log.isDebugEnabled()) {
            log.debug("RegistryService set in Carbon STS bundle");
        }
        try {
            STSServiceDataHolder.getInstance().setRegistryService(registryService);
        } catch (Throwable e) {
            log.error("Failed to get a reference to the Registry", e);
        }
    }

    protected void unsetRegistryService(RegistryService registryService) {
        STSServiceDataHolder.getInstance().setRegistryService(null);
        if (log.isDebugEnabled()) {
            log.debug("RegistryService unset in Carbon STS bundle");
        }
    }

    protected void setRealmService(RealmService realmService) {
        if(log.isDebugEnabled()){
            log.debug("Setting the RealmService");
        }
        STSServiceDataHolder.getInstance().setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService) {
        if(log.isDebugEnabled()){
            log.debug("Unsetting the RealmService");
        }
        STSServiceDataHolder.getInstance().setRealmService(null);
    }
}