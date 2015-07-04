/*
 *
 * Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * /
 */

package org.wso2.carbon.identity.uma.internal;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.uma.userconsent.UmaReqMessageContext;
import org.wso2.carbon.identity.uma.userconsent.handlers.UserConsentHandler;

import java.util.*;

/**
 * @scr.component name="org.wso2.carbon.identity.uma.handlers.listener" immediate="true"
 * @scr.reference name="user.consent.handler.service"
 * interface="org.wso2.carbon.identity.uma.userconsent.handlers.UserConsentHandler"
 * cardinality="0..n" policy="dynamic"
 * bind="setUserConsentHandler"
 * unbind="unsetUserConsentHandler"
 */
public class UMAListenerServiceComponent {
    private static final Log log = LogFactory.getLog(UMAServiceComponent.class);

    // map of handlers
    private static Map<String, List<UserConsentHandler>> userConsentHandlers;

    // collections to be returned
    private static Collection<UserConsentHandler> userConsentHandlerCollection;


    protected static synchronized void setUserConsentHandler(UserConsentHandler userConsentHandler){

        userConsentHandlerCollection = null;

        if (userConsentHandlers == null){
            userConsentHandlers = new HashMap<>();
        }

        // if the list of handlers does not exist create one
        if (userConsentHandlers.get(userConsentHandler.gethandledUserConsentType()) == null){
            userConsentHandlers.put(userConsentHandler.gethandledUserConsentType(),new ArrayList<UserConsentHandler>());
        }

        // add the new user consent handler to the list
        userConsentHandlers.get(userConsentHandler.gethandledUserConsentType()).add(userConsentHandler);
    }


    protected static synchronized void unsetUserConsentHandler(UserConsentHandler userConsentHandler){


    }


    public static synchronized Collection<UserConsentHandler> getUserConsentHandlers(String consentType,UmaReqMessageContext messageContext){
        List<UserConsentHandler> userConsentHandlerList = userConsentHandlers.get(consentType);

        if (userConsentHandlerList != null){
                // sort according to the priority
        }

        return userConsentHandlerList;
    }

    public static synchronized Collection<UserConsentHandler> getUserConsentHandlers(){

        if (userConsentHandlerCollection == null) {
            userConsentHandlerCollection = new ArrayList<>();

            for (List list : userConsentHandlers.values()) {
                userConsentHandlerCollection.addAll(list);
            }
        }

        return userConsentHandlerCollection;
    }
}


