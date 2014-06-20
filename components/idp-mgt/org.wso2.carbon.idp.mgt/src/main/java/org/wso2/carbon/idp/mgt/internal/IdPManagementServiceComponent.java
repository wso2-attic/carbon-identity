/*
 *Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */

package org.wso2.carbon.idp.mgt.internal;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.identity.application.common.persistence.JDBCPersistenceManager;
import org.wso2.carbon.stratos.common.listeners.TenantMgtListener;
import org.wso2.carbon.user.core.listener.UserOperationEventListener;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.ConfigurationContextService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @scr.component name="idp.mgt.dscomponent" immediate=true
 * @scr.reference name="user.realmservice.default"
 *                interface="org.wso2.carbon.user.core.service.RealmService" cardinality="1..1"
 *                policy="dynamic" bind="setRealmService" unbind="unsetRealmService"
 * @scr.reference name="config.context.service"
 *                interface="org.wso2.carbon.utils.ConfigurationContextService" cardinality="1..1"
 *                policy="dynamic" bind="setConfigurationContextService"
 *                unbind="unsetConfigurationContextService"
 */
public class IdPManagementServiceComponent {

    private static Log log = LogFactory.getLog(IdPManagementServiceComponent.class);

    private static RealmService realmService = null;

    private static ConfigurationContextService configurationContextService = null;

    private static Map<String, IdentityProvider> fileBasedIdPs = new HashMap<String, IdentityProvider>();

    protected void activate(ComponentContext ctxt) {
        try {
            BundleContext bundleCtx = ctxt.getBundleContext();

            TenantManagementListener idPMgtTenantMgtListener = new TenantManagementListener();
            ServiceRegistration tenantMgtListenerSR = bundleCtx.registerService(
                    TenantMgtListener.class.getName(), idPMgtTenantMgtListener, null);
            if (tenantMgtListenerSR != null) {
                log.debug("Identity Provider Management - TenantMgtListener registered");
            } else {
                log.error("Identity Provider Management - TenantMgtListener could not be registered");
            }

            ServiceRegistration userOperationListenerSR = bundleCtx.registerService(
                    UserOperationEventListener.class.getName(), new UserStoreListener(), null);
            if (userOperationListenerSR != null) {
                log.debug("Identity Provider Management - UserOperationEventListener registered");
            } else {
                log.error("Identity Provider Management - UserOperationEventListener could not be registered");
            }

            JDBCPersistenceManager jdbcPersistenceManager = JDBCPersistenceManager.getInstance();
            if(System.getProperty("setup") != null){
                // initialize the identity application persistence manager
                jdbcPersistenceManager.initializeDatabase();
            } else {
                log.info("Identity Application Management Database initialization not attempted since \'setup\' " +
                        "variable was not provided during startup");
            }

            buidFileBasedIdPList();

            log.debug("Identity Provider Management bundle is activated");

        } catch (Throwable e) {

            log.error("Error while activating Identity Provider Management bundle", e);

        }
    }

    /**
     * 
     */
    private void buidFileBasedIdPList() {

        String spConfigDirPath = CarbonUtils.getCarbonConfigDirPath() + File.separator + "identity"
                + File.separator + "identity-providers";
        FileInputStream fileInputStream = null;
        File spConfigDir = new File(spConfigDirPath);
        OMElement documentElement = null;

        if (spConfigDir.exists()) {

            for (final File fileEntry : spConfigDir.listFiles()) {
                try {
                    if (!fileEntry.isDirectory()) {
                        fileInputStream = new FileInputStream(new File(fileEntry.getAbsolutePath()));
                        documentElement = new StAXOMBuilder(fileInputStream).getDocumentElement();
                        IdentityProvider idp = IdentityProvider.build(documentElement);
                        if (idp != null) {
                            fileBasedIdPs.put(idp.getIdentityProviderName(), idp);
                        }
                    }
                } catch (Exception e) {
                    log.error("Error while loading idp from file system.", e);
                } finally {
                    if(fileInputStream != null){
                        try {
                            fileInputStream.close();
                        } catch (IOException e) {
                            log.error(e.getMessage(), e);
                        }
                    }
                }
            }
        }
    }

    /**
     * 
     * @return
     */
    public static Map<String, IdentityProvider> getFileBasedIdPs() {
        return fileBasedIdPs;
    }

    /**
     * 
     * @param ctxt
     */
    protected void deactivate(ComponentContext ctxt) {
        log.debug("Identity Provider Management bundle is deactivated");
    }

    /**
     * 
     * @return
     */
    public static RealmService getRealmService() {
        return realmService;
    }

    /**
     * 
     * @return
     */
    public static ConfigurationContextService getConfigurationContextService() {
        return configurationContextService;
    }

    /**
     * 
     * @param rlmService
     */
    protected void setRealmService(RealmService rlmService) {
        realmService = rlmService;
    }

    /**
     * 
     * @param realmService
     */
    protected void unsetRealmService(RealmService realmService) {
        realmService = null;
    }

    /**
     * 
     * @param service
     */
    protected void setConfigurationContextService(ConfigurationContextService service) {
        configurationContextService = service;
    }

    /**
     * 
     * @param service
     */
    protected void unsetConfigurationContextService(ConfigurationContextService service) {
        configurationContextService = null;
    }
}
