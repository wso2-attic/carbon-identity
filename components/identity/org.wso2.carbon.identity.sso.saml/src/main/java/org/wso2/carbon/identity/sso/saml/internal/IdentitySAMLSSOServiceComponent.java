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

package org.wso2.carbon.identity.sso.saml.internal;

import java.io.File;
import java.io.FileInputStream;
import java.util.Scanner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.equinox.http.helper.ContextPathServletAdaptor;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpService;
import org.wso2.carbon.identity.authenticator.saml2.sso.common.Util;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.sso.saml.SSOServiceProviderConfigManager;
import org.wso2.carbon.identity.sso.saml.admin.FileBasedConfigManager;
import org.wso2.carbon.identity.sso.saml.servlet.SAMLSSOProviderServlet;
import org.wso2.carbon.identity.sso.saml.util.SAMLSSOUtil;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.ConfigurationContextService;

import javax.servlet.Servlet;

/**
 * @scr.component name="identity.sso.saml.component" immediate="true"
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
 * @scr.reference name="osgi.httpservice" interface="org.osgi.service.http.HttpService"
 *                cardinality="1..1" policy="dynamic" bind="setHttpService"  
 *                unbind="unsetHttpService"
 */
public class IdentitySAMLSSOServiceComponent{

    private static Log log = LogFactory.getLog(IdentitySAMLSSOServiceComponent.class);
    
    public static final String SAMLSSO_URL = "/samlsso";

    private static int defaultSingleLogoutRetryCount = 5;

    private static long defaultSingleLogoutRetryInterval = 60000;
    
    private static String ssoRedirectPage = null;

    protected void activate(ComponentContext ctxt) {
        SAMLSSOUtil.setBundleContext(ctxt.getBundleContext());
        HttpService httpService = SAMLSSOUtil.getHttpService();
        
        // Register SAML SSO servlet
        Servlet samlSSOServlet = new ContextPathServletAdaptor(new SAMLSSOProviderServlet(), SAMLSSO_URL);
        try {
            httpService.registerServlet(SAMLSSO_URL, samlSSOServlet, null, null);
        } catch (Exception e) {
            String errMsg = "Error when registering SAML SSO Servlet via the HttpService.";
            log.error(errMsg, e);
            throw new RuntimeException(errMsg, e);
        }
        
        // Register a SSOServiceProviderConfigManager object as an OSGi Service
        ctxt.getBundleContext().registerService(SSOServiceProviderConfigManager.class.getName(),
                                                SSOServiceProviderConfigManager.getInstance(), null);

        try {
            IdentityUtil.populateProperties();
            SAMLSSOUtil.setSingleLogoutRetryCount(Integer.parseInt(
                    IdentityUtil.getProperty(IdentityConstants.ServerConfig.SINGLE_LOGOUT_RETRY_COUNT)));
            SAMLSSOUtil.setSingleLogoutRetryInterval(Long.parseLong(IdentityUtil.getProperty(
                    IdentityConstants.ServerConfig.SINGLE_LOGOUT_RETRY_INTERVAL)));
            
            SAMLSSOUtil.setResponseBuilder(IdentityUtil.getProperty("SSOService.SAMLSSOResponseBuilder"));
            
            log.debug("Single logout retry count is set to " + SAMLSSOUtil.getSingleLogoutRetryCount());
            log.debug("Single logout retry interval is set to " +
                    SAMLSSOUtil.getSingleLogoutRetryInterval() + " in seconds.");
            
            
            String redirectHtmlPath = CarbonUtils.getCarbonHome() + File.separator + "repository"
                    + File.separator + "resources" + File.separator + "security" + File.separator + "sso_redirect.html";
            FileInputStream fis = new FileInputStream(new File(redirectHtmlPath));
            ssoRedirectPage = new Scanner(fis,"UTF-8").useDelimiter("\\A").next();
            log.debug("sso_redirect.html " + ssoRedirectPage);

            FileBasedConfigManager.getInstance().addServiceProviders();

            Util.initSSOConfigParams();
            if (log.isDebugEnabled()) {
                log.info("Identity SAML SSO bundle is activated");
            }
        } catch (Throwable e) {
            SAMLSSOUtil.setSingleLogoutRetryCount(defaultSingleLogoutRetryCount);
            SAMLSSOUtil.setSingleLogoutRetryInterval(defaultSingleLogoutRetryInterval);
            if (log.isDebugEnabled()) {
                log.debug("Failed to load the single logout retry count and interval values." +
                        " Default values for retry count: " + defaultSingleLogoutRetryCount +
                        " and interval: " + defaultSingleLogoutRetryInterval + " will be used.");
            }
        }

    }

    protected void deactivate(ComponentContext ctxt) {
        SAMLSSOUtil.setBundleContext(null);
        if (log.isDebugEnabled()) {
            log.info("Identity SAML SSO bundle is deactivated");
        }
    }

    protected void setRegistryService(RegistryService registryService) {
        if (log.isDebugEnabled()) {
            log.debug("RegistryService set in Identity SAML SSO bundle");
        }
        try {
            SAMLSSOUtil.setRegistryService(registryService);
        } catch (Throwable e) {
            log.error("Failed to get a reference to the Registry in SAML SSO bundle", e);
        }
    }

    protected void unsetRegistryService(RegistryService registryService) {
        if (log.isDebugEnabled()) {
            log.debug("RegistryService unset in SAML SSO bundle");
        }
        SAMLSSOUtil.setRegistryService(null);
    }

    protected void setRealmService(RealmService realmService){
        if(log.isDebugEnabled()){
            log.debug("Realm Service is set in the SAML SSO bundle");
        }
        SAMLSSOUtil.setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService){
        if(log.isDebugEnabled()){
            log.debug("Realm Service is set in the SAML SSO bundle");
        }
        SAMLSSOUtil.setRegistryService(null);
    }

    protected void setConfigurationContextService(ConfigurationContextService configCtxService){
        if(log.isDebugEnabled()){
            log.debug("Configuration Context Service is set in the SAML SSO bundle");
        }
        SAMLSSOUtil.setConfigCtxService(configCtxService);
    }

    protected void unsetConfigurationContextService(ConfigurationContextService configCtxService){
        if(log.isDebugEnabled()){
            log.debug("Configuration Context Service is unset in the SAML SSO bundle");
        }
        SAMLSSOUtil.setConfigCtxService(null);
    }
    
    protected void setHttpService(HttpService httpService){
        if(log.isDebugEnabled()){
            log.debug("HTTP Service is set in the SAML SSO bundle");
        }
        SAMLSSOUtil.setHttpService(httpService);
    }

    protected void unsetHttpService(HttpService httpService){
        if(log.isDebugEnabled()){
            log.debug("HTTP Service is unset in the SAML SSO bundle");
        }
        SAMLSSOUtil.setHttpService(null);
    }
    
    public static String getSsoRedirectHtml() {
    	return ssoRedirectPage;
    }
}