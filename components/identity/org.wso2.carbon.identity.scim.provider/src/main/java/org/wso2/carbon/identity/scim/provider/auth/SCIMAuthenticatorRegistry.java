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
package org.wso2.carbon.identity.scim.provider.auth;

import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.message.Message;

import java.util.Map;
import java.util.TreeMap;

/**
 * This stores the authenticators registered for SCIM REST endpoints and returns the appropriate
 * authenticator as requested by authentication filter associated with SCIM REST endpoints.
 */
public class SCIMAuthenticatorRegistry {

    private static SCIMAuthenticatorRegistry SCIMAuthRegistry;
    private static Map<Integer, SCIMAuthenticationHandler> SCIMAuthHandlers = new TreeMap<Integer,
            SCIMAuthenticationHandler>();

    public static SCIMAuthenticatorRegistry getInstance() {
        if (SCIMAuthRegistry == null) {
            synchronized (SCIMAuthenticatorRegistry.class) {
                if (SCIMAuthRegistry == null) {
                    SCIMAuthRegistry = new SCIMAuthenticatorRegistry();
                    return SCIMAuthRegistry;
                } else {
                    return SCIMAuthRegistry;
                }
            }
        } else {
            return SCIMAuthRegistry;
        }
    }

    /**
     * Given the RESTful message and other info, returns the authenticator which can handle the request.
     *
     * @param message
     * @param classResourceInfo
     * @return
     */
    public SCIMAuthenticationHandler getAuthenticator(Message message,
                                                      ClassResourceInfo classResourceInfo) {
        //since we use a tree map to store authenticators, they are ordered based on the priority.
        //therefore, we iterate over the authenticators and check the can handle method
        for (SCIMAuthenticationHandler scimAuthenticationHandler : SCIMAuthHandlers.values()) {
            if (scimAuthenticationHandler.canHandle(message, classResourceInfo)) {
                return scimAuthenticationHandler;
            }
        }
        return null;
    }

    public void setAuthenticator(SCIMAuthenticationHandler SCIMAuthHandler) {
        SCIMAuthHandlers.put(SCIMAuthHandler.getPriority(), SCIMAuthHandler);
    }

    public void removeAuthenticator(SCIMAuthenticationHandler scimAuthenticationHandler) {
        SCIMAuthHandlers.remove(scimAuthenticationHandler.getPriority());
    }
    
}