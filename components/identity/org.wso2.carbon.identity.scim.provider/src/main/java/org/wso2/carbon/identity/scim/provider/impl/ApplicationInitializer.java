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
package org.wso2.carbon.identity.scim.provider.impl;

import org.wso2.carbon.base.ServerConfigurationException;
import org.wso2.carbon.identity.core.util.IdentityConfigParser;
import org.wso2.carbon.identity.scim.provider.auth.BasicAuthHandler;
import org.wso2.carbon.identity.scim.provider.auth.OAuthHandler;
import org.wso2.carbon.identity.scim.provider.auth.SCIMAuthConfigReader;
import org.wso2.carbon.identity.scim.provider.auth.SCIMAuthenticationHandler;
import org.wso2.carbon.identity.scim.provider.auth.SCIMAuthenticatorRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.charon.core.exceptions.CharonException;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.List;

/**
 * This performs one-time initialization tasks at the application startup.
 */
public class ApplicationInitializer implements ServletContextListener {

    private Log logger = LogFactory.getLog(ApplicationInitializer.class);

    public void contextInitialized(ServletContextEvent servletContextEvent) {
        if (logger.isDebugEnabled()) {
            logger.debug("Initializing SCIM Webapp...");
        }
        try {
            //Initialize Authentication Registry
            initSCIMAuthenticatorRegistry();

            //initialize identity scim manager
            IdentitySCIMManager.getInstance();

        } catch (CharonException e) {
            logger.error("Error in initializing the IdentitySCIMManager at the initialization of " +
                         "SCIM webapp");
        }
    }

    public void contextDestroyed(ServletContextEvent servletContextEvent) {

    }

    private void initSCIMAuthenticatorRegistry() {
        SCIMAuthenticatorRegistry scimAuthRegistry = SCIMAuthenticatorRegistry.getInstance();
        if (scimAuthRegistry != null) {
            //set authenticators after building auth config
            SCIMAuthConfigReader configReader = new SCIMAuthConfigReader();
            List<SCIMAuthenticationHandler> SCIMAuthenticators = configReader.buildSCIMAuthenticators();
            if (SCIMAuthenticators != null && !SCIMAuthenticators.isEmpty()) {
                for (SCIMAuthenticationHandler scimAuthenticator : SCIMAuthenticators) {
                    scimAuthRegistry.setAuthenticator(scimAuthenticator);
                }
                                
            } else {
                //initialize default basic auth authenticator & OAuth authenticator and set it in the auth registry.
                BasicAuthHandler basicAuthHandler = new BasicAuthHandler();
                basicAuthHandler.setDefaultPriority();
                scimAuthRegistry.setAuthenticator(basicAuthHandler);

                OAuthHandler oauthHandler = new OAuthHandler();
                oauthHandler.setDefaultPriority();
                oauthHandler.setDefaultAuthzServer();
                scimAuthRegistry.setAuthenticator(oauthHandler);
            }
        }
    }
}
