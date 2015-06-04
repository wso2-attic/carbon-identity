/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.application.authenticator.openid.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator;
import org.wso2.carbon.identity.application.authenticator.openid.OpenIDAuthenticator;


/**
 * @scr.component name="identity.application.authenticator.openid.component" immediate="true"
 */
public class OpenIDAuthenticatorServiceComponent {

    private static Log log = LogFactory.getLog(OpenIDAuthenticatorServiceComponent.class);

    protected void activate(ComponentContext ctxt) {
        OpenIDAuthenticator openIdAuthenticator = new OpenIDAuthenticator();

        ctxt.getBundleContext().registerService(ApplicationAuthenticator.class.getName(), openIdAuthenticator, null);

        if (log.isDebugEnabled()) {
            log.info("OpenID Authenticator bundle is activated");
        }
    }

    protected void deactivate(ComponentContext ctxt) {
        if (log.isDebugEnabled()) {
            log.info("OpenID Authenticator bundle is deactivated");
        }
    }
}