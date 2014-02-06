/*
*Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.oauth2.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.identity.oauth2.OAuth2Service;
import org.wso2.carbon.identity.oauth2.OAuth2TokenValidationService;

/**
 * @scr.component name="identity.oauth2.component" immediate="true"
 */
public class OAuth2ServiceComponent {
    private static Log log = LogFactory.getLog(OAuth2ServiceComponent.class);
    private static BundleContext bundleContext;

    protected void activate(ComponentContext context) {
        //Registering OAuth2Service as a OSGIService
        bundleContext = context.getBundleContext();
        bundleContext.registerService(OAuth2Service.class.getName(), new OAuth2Service(), null);
        // exposing server configuration as a service 
        OAuthServerConfiguration oauthServerConfig = OAuthServerConfiguration.getInstance();
        bundleContext.registerService(OAuthServerConfiguration.class.getName(), oauthServerConfig, null);
        OAuth2TokenValidationService tokenValidationService = new OAuth2TokenValidationService();
        bundleContext.registerService(OAuth2TokenValidationService.class.getName(), tokenValidationService, null);
        if (log.isDebugEnabled()) {
            log.info("Identity OAuth bundle is activated");
        }
    }
}
