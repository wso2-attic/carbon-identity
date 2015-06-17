/*
 * Copyright (c) 2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.oauth.ui.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.equinox.http.helper.ContextPathServletAdaptor;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpService;
import org.wso2.carbon.base.api.ServerConfigurationService;
import org.wso2.carbon.identity.oauth.ui.OAuthServlet;
import org.wso2.carbon.utils.ConfigurationContextService;

import javax.servlet.Servlet;

/**
 * @scr.component name="identity.provider.oauth.ui.component" immediate="true"
 * @scr.reference name="osgi.httpservice" interface="org.osgi.service.http.HttpService"
 * cardinality="1..1" policy="dynamic" bind="setHttpService"  unbind="unsetHttpService"
 * @scr.reference name="config.context.service"
 * interface="org.wso2.carbon.utils.ConfigurationContextService" cardinality="1..1"
 * policy="dynamic" bind="setConfigurationContextService"
 * unbind="unsetConfigurationContextService"
 * @scr.reference name="server.configuration" interface="org.wso2.carbon.base.api.ServerConfigurationService"
 * cardinality="1..1" policy="dynamic" bind="setServerConfigurationService" unbind="unsetServerConfigurationService"
 */
public class OAuthUIServiceComponent {

    public static final String OAUTH_URL = "/oauth";
    private static final Log log = LogFactory.getLog(OAuthUIServiceComponent.class);

    @SuppressWarnings("unchecked")
    protected void activate(ComponentContext context) {
        log.debug("Activating Identity OAuth UI bundle.");

        HttpService httpService = OAuthUIServiceComponentHolder.getInstance().getHttpService();

        try {

            // Register OAuth 1.a servlet
            Servlet oauth1aServlet = new ContextPathServletAdaptor(new OAuthServlet(), OAUTH_URL);
            httpService.registerServlet(OAUTH_URL, oauth1aServlet, null, null);
            log.debug("Successfully registered an instance of OAuthServlet");

        } catch (Exception e) {
            String errMsg = "Error when registering an OAuth endpoint via the HttpService.";
            log.error(errMsg, e);
            throw new RuntimeException(errMsg, e);
        }

        log.debug("Successfully activated Identity OAuth UI bundle.");

    }

    protected void deactivate(ComponentContext context) {
        log.debug("Identity OAuth UI bundle is deactivated");
    }

    protected void setHttpService(HttpService httpService) {
        OAuthUIServiceComponentHolder.getInstance().setHttpService(httpService);
    }

    protected void unsetHttpService(HttpService httpService) {
        httpService.unregister(OAUTH_URL);
        OAuthUIServiceComponentHolder.getInstance().setHttpService(null);
    }

    protected void setConfigurationContextService(ConfigurationContextService configurationContextService) {
        OAuthUIServiceComponentHolder.getInstance().setConfigurationContextService(configurationContextService);
        log.debug("ConfigurationContextService Instance was set.");
    }

    protected void unsetConfigurationContextService(ConfigurationContextService configurationContextService) {
        OAuthUIServiceComponentHolder.getInstance().setConfigurationContextService(null);
        log.debug("ConfigurationContextService Instance was unset.");
    }

    protected void setServerConfigurationService(ServerConfigurationService serverConfigService) {
        OAuthUIServiceComponentHolder.getInstance().setServerConfigurationService(serverConfigService);
        log.debug("ServerConfigurationService instance was set.");
    }

    protected void unsetServerConfigurationService(ServerConfigurationService serverConfigService) {
        OAuthUIServiceComponentHolder.getInstance().setServerConfigurationService(null);
        log.debug("ServerConfigurationService instance was unset.");
    }

}
