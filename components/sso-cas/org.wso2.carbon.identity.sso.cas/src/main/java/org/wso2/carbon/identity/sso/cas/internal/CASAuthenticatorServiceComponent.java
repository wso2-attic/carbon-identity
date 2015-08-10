/* ***************************************************************************
 * Copyright 2014 Ellucian Company L.P. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *****************************************************************************/
package org.wso2.carbon.identity.sso.cas.internal;

import javax.servlet.Servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.equinox.http.helper.ContextPathServletAdaptor;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpService;
import org.wso2.carbon.identity.sso.cas.CASEndpointConstants;
import org.wso2.carbon.identity.sso.cas.config.CASConfiguration;
import org.wso2.carbon.identity.sso.cas.servlet.CASLoginServlet;
import org.wso2.carbon.identity.sso.cas.servlet.CASLogoutServlet;
import org.wso2.carbon.identity.sso.cas.servlet.CASProxyLoginServlet;
import org.wso2.carbon.identity.sso.cas.servlet.CASProxyValidationServlet;
import org.wso2.carbon.identity.sso.cas.servlet.CASSAMLValidationServlet;
import org.wso2.carbon.identity.sso.cas.servlet.CASServiceValidationServlet;
import org.wso2.carbon.identity.sso.cas.servlet.CASValidationServlet;
import org.wso2.carbon.identity.sso.cas.util.CASSSOUtil;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * @scr.component name="identity.sso.cas" immediate="true"
 * @scr.reference name="registry.service"
 *                interface="org.wso2.carbon.registry.core.service.RegistryService"
 *                cardinality="1..1" policy="dynamic" bind="setRegistryService"
 *                unbind="unsetRegistryService"
 * @scr.reference name="user.realmservice.default" interface="org.wso2.carbon.user.core.service.RealmService"
 *                cardinality="1..1" policy="dynamic" bind="setRealmService"
 *                unbind="unsetRealmService" 
 * @scr.reference name="osgi.httpservice" interface="org.osgi.service.http.HttpService"
 *                cardinality="1..1" policy="dynamic" bind="setHttpService"  
 *                unbind="unsetHttpService"
 */
public class CASAuthenticatorServiceComponent{


    private static Log log = LogFactory.getLog(CASAuthenticatorServiceComponent.class);
    
    protected void activate(ComponentContext ctxt) {
        HttpService httpService = CASSSOUtil.getHttpService();
              
        String casLoginPath = CASConfiguration.buildRelativePath(CASEndpointConstants.LOGIN_PATH);
        String casProxyLoginPath = CASConfiguration.buildRelativePath(CASEndpointConstants.PROXY_LOGIN_PATH);
        String casLogoutPath = CASConfiguration.buildRelativePath(CASEndpointConstants.LOGOUT_PATH);
        String casValidatePath = CASConfiguration.buildRelativePath(CASEndpointConstants.VALIDATE_PATH);
        String casProxyValidatePath = CASConfiguration.buildRelativePath(CASEndpointConstants.PROXY_VALIDATE_PATH);
        String casServiceValidatePath = CASConfiguration.buildRelativePath(CASEndpointConstants.SERVICE_VALIDATE_PATH);
        String casSamlValidatePath = CASConfiguration.buildRelativePath(CASEndpointConstants.SAML_VALIDATE_PATH);
        
        // Register CAS SSO servlets
        Servlet casLoginServlet = new ContextPathServletAdaptor(new CASLoginServlet(), casLoginPath);
        Servlet casProxyLoginServlet = new ContextPathServletAdaptor(new CASProxyLoginServlet(), casProxyLoginPath);
        Servlet casLogoutServlet = new ContextPathServletAdaptor(new CASLogoutServlet(), casLogoutPath);
        Servlet casValidateServlet = new ContextPathServletAdaptor(new CASValidationServlet(), casValidatePath);
        Servlet casProxyValidateServlet = new ContextPathServletAdaptor(new CASProxyValidationServlet(), casProxyValidatePath);
        Servlet casServiceValidateServlet = new ContextPathServletAdaptor(new CASServiceValidationServlet(), casServiceValidatePath);
        Servlet casSamlValidateServlet = new ContextPathServletAdaptor(new CASSAMLValidationServlet(), casSamlValidatePath);

        try {
            httpService.registerServlet(casLoginPath, casLoginServlet, null, null);
            httpService.registerServlet(casProxyLoginPath, casProxyLoginServlet, null, null);
            httpService.registerServlet(casLogoutPath, casLogoutServlet, null, null);
            httpService.registerServlet(casValidatePath, casValidateServlet, null, null);
            httpService.registerServlet(casProxyValidatePath, casProxyValidateServlet, null, null);
            httpService.registerServlet(casServiceValidatePath, casServiceValidateServlet, null, null);   
            httpService.registerServlet(casSamlValidatePath, casSamlValidateServlet, null, null);
        } catch (Exception e) {
            String errMsg = "Error when registering CAS SSO Servlet via the HttpService.";
            log.error(errMsg, e);
            throw new RuntimeException(errMsg, e);
        }
        
        log.info("Identity CAS SSO bundle is activated");
    }

    protected void deactivate(ComponentContext ctxt) {
        if (log.isDebugEnabled()) {
            log.info("Identity CAS SSO bundle is deactivated");
        }
    }

    protected void setRegistryService(RegistryService registryService) {
        if (log.isDebugEnabled()) {
            log.debug("RegistryService set in Identity CAS SSO bundle");
        }
        try {
        	CASSSOUtil.setRegistryService(registryService);
        } catch (Throwable e) {
            log.error("Failed to get a reference to the Registry in CAS SSO bundle", e);
        }
    }
    
    protected void unsetRegistryService(RegistryService registryService) {
        if (log.isDebugEnabled()) {
            log.debug("RegistryService unset in CAS SSO bundle");
        }
        CASSSOUtil.setRegistryService(null);
    }

    protected void setRealmService(RealmService realmService){
        if(log.isDebugEnabled()){
            log.debug("Realm Service is set in the CAS SSO bundle");
        }
        CASSSOUtil.setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService){
        if(log.isDebugEnabled()){
            log.debug("Realm Service is set in the CAS SSO bundle");
        }
        CASSSOUtil.setRegistryService(null);
    }
    
    protected void setHttpService(HttpService httpService){
        if(log.isDebugEnabled()){
            log.debug("HTTP Service is set in the CAS SSO bundle");
        }
        CASSSOUtil.setHttpService(httpService);
    }

    protected void unsetHttpService(HttpService httpService){
        if(log.isDebugEnabled()){
            log.debug("HTTP Service is unset in the CAS SSO bundle");
        }
        CASSSOUtil.setHttpService(null);
    }
}

