/*
 * Copyright (c) 2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 *
 */

package org.wso2.carbon.identity.sso.agent.saml;

import org.wso2.carbon.identity.sso.agent.SSOAgentConstants;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SSOAgentHttpSessionListener implements HttpSessionListener {

    private static final Logger LOGGER = Logger.getLogger(SSOAgentConstants.LOGGER_NAME);

    @Override
    public void sessionCreated(HttpSessionEvent httpSessionEvent) {
        if (httpSessionEvent.getSession().getAttribute(SSOAgentConstants.SESSION_BEAN_NAME) == null) {
            LOGGER.log(Level.WARNING, "HTTP Session created without LoggedInSessionBean");
        }
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent httpSessionEvent) {
        // No need to invalidate session here, as it is going to be invalidated soon
        SSOAgentSessionManager.invalidateSession(httpSessionEvent.getSession());
        httpSessionEvent.getSession().removeAttribute(SSOAgentConstants.SESSION_BEAN_NAME);
    }
}
