/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 *
 */

package org.wso2.carbon.identity.entitlement.filter.exception;

import org.apache.axis2.context.ConfigurationContext;

import javax.servlet.ServletConfig;

public class EntitlementCacheUpdateServletDataHolder {


    private static EntitlementCacheUpdateServletDataHolder instance = new EntitlementCacheUpdateServletDataHolder();

    private String httpsPort;
    private ConfigurationContext configCtx;
    private String remoteServiceUserName;
    private String remoteServicePassword;
    private String remoteServiceUrl;
    private String authCookie;
    private ServletConfig servletConfig;
    private String authentication;
    private String authenticationPage;
    private String authenticationPageURL;


    public static EntitlementCacheUpdateServletDataHolder getInstance() {
        return instance;
    }

    public String getAuthenticationPageURL() {
        return authenticationPageURL;
    }

    public void setAuthenticationPageURL(String authPageURL) {
        authenticationPageURL = authPageURL;
    }


    public String getAuthentication() {
        return authentication;
    }

    public void setAuthentication(String auth) {
        authentication = auth;
    }

    public ServletConfig getServletConfig() {
        return servletConfig;
    }

    public void setServletConfig(ServletConfig servletConfiguration) {
        servletConfig = servletConfiguration;
    }

    public String getAuthCookie() {
        return authCookie;
    }

    public void setAuthCookie(String authenticationCookie) {
        authCookie = authenticationCookie;
    }


    public String getRemoteServiceUrl() {
        return remoteServiceUrl;
    }

    public void setRemoteServiceUrl(String remoteServiceURL) {
        remoteServiceUrl = remoteServiceURL;
    }


    public String getRemoteServicePassword() {
        return remoteServicePassword;
    }

    public void setRemoteServicePassword(String remoteServicePswd) {
        remoteServicePassword = remoteServicePswd;
    }

    public String getRemoteServiceUserName() {
        return remoteServiceUserName;
    }

    public void setRemoteServiceUserName(String rmtServiceUserName) {
        remoteServiceUserName = rmtServiceUserName;
    }

    public String getAuthenticationPage() {
        return authenticationPage;
    }

    public void setAuthenticationPage(String authPage) {
        authenticationPage = authPage;
    }


    public String getHttpsPort() {
        return httpsPort;
    }

    public void setHttpsPort(String httpsPortStr) {
        httpsPort = httpsPortStr;
    }


    public ConfigurationContext getConfigCtx() {
        return configCtx;
    }

    public void setConfigCtx(ConfigurationContext configContext) {
        configCtx = configContext;
    }

}
