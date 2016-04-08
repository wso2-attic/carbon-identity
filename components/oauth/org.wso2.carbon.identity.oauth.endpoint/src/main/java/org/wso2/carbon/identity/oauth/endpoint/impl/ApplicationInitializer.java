/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.oauth.endpoint.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.oauth.endpoint.auth.BasicAuthenticationHandler;
import org.wso2.carbon.identity.oauth.endpoint.auth.OAuthAuthenticationHandler;
import org.wso2.carbon.identity.oauth.endpoint.auth.OAuthAuthenticatorConfigReader;
import org.wso2.carbon.identity.oauth.endpoint.auth.OAuthAuthenticatorRegistry;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.List;

/**
 * This performs one-time initialization tasks at the application startup.
 */
public class ApplicationInitializer implements ServletContextListener {

    private Log log = LogFactory.getLog(ApplicationInitializer.class);

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        if (log.isDebugEnabled()) {
            log.debug("Initializing OAuth Webapp...");
        }
        //Initialize OAuth Authentication Registry
        initOAuthAuthenticatorRegistry();
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        // Do nothing
    }

    private void initOAuthAuthenticatorRegistry() {
        OAuthAuthenticatorRegistry oauthAuthRegistry = OAuthAuthenticatorRegistry.getInstance();
        if (oauthAuthRegistry != null) {
            //set authenticators after building auth config
            OAuthAuthenticatorConfigReader configReader = new OAuthAuthenticatorConfigReader();
            List<OAuthAuthenticationHandler> OAuthAuthenticators = configReader.buildOAuthAuthenticators();
            if (OAuthAuthenticators != null && !OAuthAuthenticators.isEmpty()) {
                for (OAuthAuthenticationHandler oauthAuthenticator : OAuthAuthenticators) {
                    oauthAuthRegistry.setAuthenticator(oauthAuthenticator);
                }

            } else {
                //initialize default basic auth authenticator and set it in the auth registry.
                BasicAuthenticationHandler basicAuthHandler = new BasicAuthenticationHandler();
                basicAuthHandler.setDefaultPriority();
                oauthAuthRegistry.setAuthenticator(basicAuthHandler);
            }
        }
    }
}
