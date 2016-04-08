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
package org.wso2.carbon.identity.oauth.endpoint.auth;

import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.message.Message;

import java.util.Map;
import java.util.TreeMap;

/**
 * This stores the authenticators registered for OAuth REST endpoints and returns the appropriate
 * authenticator as requested by authentication filter associated with the OAuth REST endpoints.
 */
public class OAuthAuthenticatorRegistry {

    private static OAuthAuthenticatorRegistry OAuthAuthRegistry;
    private static Map<Integer, OAuthAuthenticationHandler> OAuthAuthHandlers = new TreeMap<Integer, OAuthAuthenticationHandler>();

    public static OAuthAuthenticatorRegistry getInstance() {
        if (OAuthAuthRegistry == null) {
            synchronized (OAuthAuthenticatorRegistry.class) {
                if (OAuthAuthRegistry == null) {
                    OAuthAuthRegistry = new OAuthAuthenticatorRegistry();
                    return OAuthAuthRegistry;
                } else {
                    return OAuthAuthRegistry;
                }
            }
        } else {
            return OAuthAuthRegistry;
        }
    }

    /**
     * Given the RESTful message and other info, returns the authenticator which can handle the request.
     *
     * @param message
     * @param classResourceInfo
     * @return
     */
    public OAuthAuthenticationHandler getAuthenticator(Message message, ClassResourceInfo classResourceInfo) {
        //since we use a tree map to store authenticators, they are ordered based on the priority.
        //therefore, we iterate over the authenticators and check the can handle method
        for (OAuthAuthenticationHandler oauthAuthHandler : OAuthAuthHandlers.values()) {
            if (oauthAuthHandler.canHandle(message, classResourceInfo)) {
                return oauthAuthHandler;
            }
        }
        return null;
    }

    public void setAuthenticator(OAuthAuthenticationHandler OAuthAuthHandler) {
        OAuthAuthHandlers.put(OAuthAuthHandler.getPriority(), OAuthAuthHandler);
    }

    public void removeAuthenticator(OAuthAuthenticationHandler oauthAuthenticationHandler) {
        OAuthAuthHandlers.remove(oauthAuthenticationHandler.getPriority());
    }
}
