/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.application.authenticator.social.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator;
import org.wso2.carbon.identity.application.authenticator.social.facebook.FacebookAuthenticator;
import org.wso2.carbon.identity.application.authenticator.social.google.GoogleOAuth2Authenticator;
import org.wso2.carbon.identity.application.authenticator.social.live.WindowsLiveOAuth2Authenticator;
import org.wso2.carbon.identity.application.authenticator.social.yahoo.YahooOAuth2Authenticator;

/**
 * @scr.component name="identity.application.authenticator.social.component"
 * immediate="true"
 */
public class SocialAuthenticatorServiceComponent {

    private static final Log log = LogFactory.getLog(SocialAuthenticatorServiceComponent.class);

    protected void activate(ComponentContext ctxt) {
        try {

            FacebookAuthenticator facebookAuthenticator = new FacebookAuthenticator();
            ctxt.getBundleContext().registerService(ApplicationAuthenticator.class.getName(),
                    facebookAuthenticator,
                    null);

            WindowsLiveOAuth2Authenticator windowsLiveOAuth2Authenticator = new WindowsLiveOAuth2Authenticator();
            ctxt.getBundleContext().registerService(ApplicationAuthenticator.class.getName(),
                    windowsLiveOAuth2Authenticator,
                    null);

            GoogleOAuth2Authenticator googleAuthenticator = new GoogleOAuth2Authenticator();
            ctxt.getBundleContext().registerService(ApplicationAuthenticator.class.getName(),
                    googleAuthenticator,
                    null);

            YahooOAuth2Authenticator yahooOAuth2Authenticator = new YahooOAuth2Authenticator();
            ctxt.getBundleContext().registerService(ApplicationAuthenticator.class.getName(),
                    yahooOAuth2Authenticator,
                    null);

            if (log.isDebugEnabled()) {
                log.debug("Social Authenticator bundle is activated.");
            }

        } catch (Throwable e) {
            log.fatal("Error while activating Social authenticator bundle.", e);
        }
    }

    protected void deactivate(ComponentContext ctxt) {
        if (log.isDebugEnabled()) {
            log.debug("Social Authenticator bundle is deactivated.");
        }
    }
}
