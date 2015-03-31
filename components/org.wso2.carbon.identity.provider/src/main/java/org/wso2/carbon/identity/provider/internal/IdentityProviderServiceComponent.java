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

package org.wso2.carbon.identity.provider.internal;

import javax.servlet.ServletContext;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.provider.IdentityAttributeService;
import org.wso2.carbon.identity.provider.IdentityAttributeServiceStore;
import org.wso2.carbon.identity.provider.IdentityProviderUtil;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.ConfigurationContextService;

/**
 * @scr.component name="identity.provider.component" immediate="true"
 * @scr.reference name="registry.service"
 *                interface="org.wso2.carbon.registry.core.service.RegistryService"
 *                cardinality="1..1" policy="dynamic" bind="setRegistryService"
 *                unbind="unsetRegistryService"
 * @scr.reference name="config.context.service"
 *                interface="org.wso2.carbon.utils.ConfigurationContextService" cardinality="1..1"
 *                policy="dynamic" bind="setConfigurationContextService"
 *                unbind="unsetConfigurationContextService"             
 * @scr.reference name="user.realmservice.default" interface="org.wso2.carbon.user.core.service.RealmService"
 *                cardinality="1..1" policy="dynamic" bind="setRealmService"
 *                unbind="unsetRealmService"
 * @scr.reference name="identity.core.util.service"
 *                interface="org.wso2.carbon.identity.core.util.IdentityUtil" cardinality="1..1"
 *                policy="dynamic" bind="setIdentityUtil" unbind="unsetIdentityUtil"
 * @scr.reference name="identity.attribute.service"
 *                interface="org.wso2.carbon.identity.provider.IdentityAttributeService"
 *                cardinality="0..n" policy="dynamic" bind="addAttributeService"
 *                unbind="removeAttributeService"
 */
public class IdentityProviderServiceComponent {
    private static Log log = LogFactory.getLog(IdentityProviderServiceComponent.class);
    private static ConfigurationContext configContext;
    private static RealmService realmService;    
    private static RegistryService registryService;

    public static RealmService getRealmService() {
        return realmService;
    }

    /**
     * 
     */
    public IdentityProviderServiceComponent() {
    }

    /**
     * 
     * @param ctxt
     */
    protected void activate(ComponentContext ctxt) {
        if (log.isDebugEnabled()) {
            log.info("Identity Provider bundle is activated");
        }
        try {
            ctxt.getBundleContext().registerService(IdentityProviderUtil.class.getName(),
                    new IdentityProviderUtil(), null);
            
            String filter = "(objectclass=" + ServletContext.class.getName() + ")";
            ctxt.getBundleContext().addServiceListener(
                    new ServletContextListener(ctxt.getBundleContext()), filter);
        } catch (Throwable e) {
            log.error("Failed to initialize Identity Provider", e);
        }
    }

    /**
     * 
     * @param ctxt
     */
    protected void deactivate(ComponentContext ctxt) {
        if (log.isDebugEnabled()) {
            log.info("Identity Provider bundle is deactivated");
        }
    }

    /**
     * 
     * @param registryService
     */
    protected void setRegistryService(RegistryService registryService) {
        this.registryService = registryService;
        if (log.isDebugEnabled()) {
            log.info("RegistryService set in Identity Provider bundle");
        }
    }

    /**
     * 
     * @param registryService
     */
    protected void unsetRegistryService(RegistryService registryService) {
        this.registryService = null;
        if (log.isDebugEnabled()) {
            log.info("RegistryService unset in Identity Provider bundle");
        }
    }

    /**
     * 
     * @param userRealmDelegating
     */
    protected void unsetUserRealmDelegating(UserRealm userRealmDelegating) {
        if (log.isDebugEnabled()) {
            log.info("DelegatingUserRealm set in Identity Provider bundle");
        }
    }

    /**
     * 
     * @param userRealmDefault
     */
    protected void unsetUserRealmDefault(UserRealm userRealmDefault) {
        if (log.isDebugEnabled()) {
            log.info("DefaultUserRealm unset in Identity Provider bundle");
        }
    }

    /**
     *
     * @param realmService
     */
    protected void setRealmService(RealmService realmService){
        if(log.isDebugEnabled()){
            log.info("ReleamService is set in Identity Provider Service Bundle");
        }        
        this.realmService = realmService;
    }

    /**
     * 
     * @param realmService
     */
    protected void unsetRealmService(RealmService realmService){
        if (log.isDebugEnabled()) {
            log.info("ReleamService is unset in Identity Provider Service Bundle");
        }
    }

    protected void addAttributeService(IdentityAttributeService attributeService) {
        if (log.isDebugEnabled()) {
            log.info("IdentityAttributeService added in Identity Provider bundle");
        }
        IdentityAttributeServiceStore.addAttributeService(attributeService);
    }

    /**
     * 
     * @param attributeService
     */
    protected void removeAttributeService(IdentityAttributeService attributeService) {
        if (log.isDebugEnabled()) {
            log.info("IdentityAttributeService removed in Identity Provider bundle");
            IdentityAttributeServiceStore.removeAttributeService(attributeService);
        }
    }

    /**
     * 
     * @param contextService
     */
    protected void setConfigurationContextService(ConfigurationContextService contextService) {
        if (log.isDebugEnabled()) {
            log.info("ConfigurationContextService set in Identity Provider bundle");
        }
        configContext = contextService.getServerConfigContext();
    }

    /**
     * 
     * @param contextService
     */
    protected void unsetConfigurationContextService(ConfigurationContextService contextService) {
        if (log.isDebugEnabled()) {
            log.info("ConfigurationContextService unset in Identity Provider bundle");
        }
    }
    
    /**
     * 
     * @param identityUtil
     */
    protected void setIdentityUtil(IdentityUtil identityUtil) {
        if (log.isDebugEnabled()) {
            log.info("IdentityUtil set in Identity Provider bundle");
        }
    }

    /**
     * 
     * @param identityUtil
     */
    protected void unsetIdentityUtil(IdentityUtil identityUtil) {
        if (log.isDebugEnabled()) {
            log.info("IdentityUtil unset in Identity Provider bundle");
        }
    }

    /**
     * 
     * @return
     */
    public static ConfigurationContext getConfigContext() {
        return configContext;
    }

    public static RegistryService getRegistryService(){
        return registryService;
    }

}