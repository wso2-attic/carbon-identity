/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.core.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.core.persistence.JDBCPersistenceManager;
import org.wso2.carbon.identity.core.um.listener.IdentityUserMgtListener;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.security.config.SecurityConfigAdmin;
import org.wso2.carbon.user.core.listener.UserStoreManagerListener;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * @scr.component name="identity.core.component" immediate="true"
 * @scr.reference name="security.config.service"
 * interface="org.wso2.carbon.security.config.SecurityConfigAdmin" cardinality="1..1"
 * policy="dynamic" bind="setSecurityConfigAdminService"
 * unbind="unsetSecurityConfigAdminService"
 * @scr.reference name="registry.service"
 * interface="org.wso2.carbon.registry.core.service.RegistryService"
 * cardinality="1..1" policy="dynamic" bind="setRegistryService"
 * unbind="unsetRegistryService"
 * @scr.reference name="user.realmservice.default"
 * interface="org.wso2.carbon.user.core.service.RealmService" cardinality="1..1"
 * policy="dynamic" bind="setRealmService" unbind="unsetRealmService"
 */

public class IdentityCoreServiceComponent {
    private static Log log = LogFactory.getLog(IdentityCoreServiceComponent.class);

    private static BundleContext bundleContext = null;

    public IdentityCoreServiceComponent() {
    }

    /**
     * @param ctxt
     */
    protected void activate(ComponentContext ctxt) {
        if (log.isDebugEnabled()) {
            log.debug("Identity Core bundle is activated");
        }
        try {
            IdentityUtil.populateProperties();
            IdentityUserMgtListener userMgtListener = new IdentityUserMgtListener();
            ctxt.getBundleContext().registerService(UserStoreManagerListener.class.getName(), userMgtListener, null);
            bundleContext = ctxt.getBundleContext();

            // initialize the identity persistence manager, if it is not already initialized.
            JDBCPersistenceManager jdbcPersistenceManager = JDBCPersistenceManager.getInstance();

            // Identity database schema creation can be avoided by setting
            // JDBCPersistenceManager.SkipDBSchemaCreation property to "true".
            String skipSchemaCreation = IdentityUtil.getProperty(
                    IdentityConstants.ServerConfig.SKIP_DB_SCHEMA_CREATION);

            if (!("true".equals(skipSchemaCreation))) {
                jdbcPersistenceManager.initializeDatabase();
            }

            if(log.isDebugEnabled()){
                if("true".equals(skipSchemaCreation)){
                    log.debug("Identity Database schema initialization check was skipped.");
                }
            }
            //taking the service registration after DB initialization.
            ctxt.getBundleContext().registerService(IdentityUtil.class.getName(),
                    new IdentityUtil(), null);
            
        } catch (Throwable e) {
            log.error("Error occurred while populating identity configuration properties", e);
        }
    }

    /**
     * @param ctxt
     */
    protected void deactivate(ComponentContext ctxt) {
        if (log.isDebugEnabled()) {
            log.debug("Identity Core bundle is deactivated");
        }
    }

    protected void setRegistryService(RegistryService registryService) {
        IdentityTenantUtil.setRegistryService(registryService);
    }

    protected void unsetRegistryService(RegistryService registryService) {
        IdentityTenantUtil.setRegistryService(null);
    }

    /**
     * @param realmService
     */
    protected void setRealmService(RealmService realmService) {
        IdentityTenantUtil.setRealmService(realmService);
    }

    /**
     * @param realmService
     */
    protected void unsetRealmService(RealmService realmService) {
        IdentityTenantUtil.setRealmService(null);
    }


    /**
     * @param securityConfig
     */
    protected void setSecurityConfigAdminService(SecurityConfigAdmin securityConfig) {
        if (log.isDebugEnabled()) {
            log.debug("SecurityConfigAdmin set in Identity Core bundle");
        }
    }

    /**
     * @param securityConfig
     */
    protected void unsetSecurityConfigAdminService(SecurityConfigAdmin securityConfig) {
        if (log.isDebugEnabled()) {
            log.debug("SecurityConfigAdmin unset in Identity Core bundle");
        }
    }

    public static BundleContext getBundleContext() {
        return bundleContext;
    }

}
