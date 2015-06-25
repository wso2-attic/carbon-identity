/*
 * Copyright (c) 2006, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.sts.mgt;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ws.security.WSPasswordCallback;
import org.wso2.carbon.CarbonException;
import org.wso2.carbon.core.util.AnonymousSessionUtil;
import org.wso2.carbon.identity.sts.mgt.internal.IdentitySTSMgtServiceComponent;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;

public class IPPasswordCallbackHandler implements CallbackHandler {

    private static final Log log = LogFactory.getLog(IPPasswordCallbackHandler.class);

    public IPPasswordCallbackHandler() {

    }

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        try {
            for (int i = 0; i < callbacks.length; i++) {

                if (callbacks[i] instanceof WSPasswordCallback) {
                    WSPasswordCallback pwcb = (WSPasswordCallback) callbacks[i];
                    String username = pwcb.getIdentifer();
                    int value = pwcb.getUsage();
                    String receivedPasswd = pwcb.getPassword();
                    if (WSPasswordCallback.USERNAME_TOKEN_UNKNOWN == value) {
                        if (receivedPasswd == null || !this.authenticateUser(username, receivedPasswd)) {
                            throw new UnsupportedCallbackException(callbacks[i], "check failed");
                        }
                    }
                }
            }
        } catch (UnsupportedCallbackException e) {
            throw e;
        } catch (Exception e) {
            log.error("User not authenticated : " + e.getMessage(), e);
            throw new IOException("User not authenticated");
        }
    }

    public boolean authenticateUser(String userName, String password) throws CarbonException,
                                                                             UserStoreException {
        RealmService realmService = IdentitySTSMgtServiceComponent.getRealmService();
        RegistryService registryService = IdentitySTSMgtServiceComponent.getRegistryService();
        boolean isAuthenticated = false;
        UserRealm realm = AnonymousSessionUtil.getRealmByUserName(registryService, realmService, userName);
        String tenantFreeUsername = MultitenantUtils.getTenantAwareUsername(userName);
        isAuthenticated = realm.getUserStoreManager().authenticate(tenantFreeUsername, password);
        return isAuthenticated;
    }

}