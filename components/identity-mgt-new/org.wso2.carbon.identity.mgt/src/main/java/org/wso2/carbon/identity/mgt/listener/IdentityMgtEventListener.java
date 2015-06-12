/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.mgt.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.mgt.IdentityMgtConfig;
import org.wso2.carbon.identity.mgt.IdentityMgtConstants;
import org.wso2.carbon.identity.mgt.IdentityMgtException;
import org.wso2.carbon.identity.mgt.event.IdentityMgtEvent;
import org.wso2.carbon.identity.mgt.internal.IdentityMgtServiceComponent;
import org.wso2.carbon.identity.mgt.service.IdentityMgtService;
import org.wso2.carbon.identity.mgt.service.IdentityMgtServiceImpl;
import org.wso2.carbon.identity.mgt.store.UserIdentityDataStore;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.common.AbstractUserOperationEventListener;

import java.util.HashMap;
import java.util.Map;


/**
 * This is an implementation of UserOperationEventListener. This defines
 * additional operations
 * for some of the core user management operations
 */
public class IdentityMgtEventListener extends AbstractUserOperationEventListener {

    private static final Log log = LogFactory.getLog(IdentityMgtEventListener.class);
    private UserIdentityDataStore module;
    IdentityMgtService identityMgtService = new IdentityMgtServiceImpl();

    public IdentityMgtEventListener() {

        module = IdentityMgtConfig.getInstance().getIdentityDataStore();
        String adminUserName =
                IdentityMgtServiceComponent.getRealmService()
                        .getBootstrapRealmConfiguration()
                        .getAdminUserName();
        try {
            UserStoreManager userStoreMng = IdentityMgtServiceComponent.getRealmService()
                    .getBootstrapRealm().getUserStoreManager();
            if (!userStoreMng.isReadOnly()) {
                Map<String, String> claimMap = new HashMap<>();
                claimMap.put(IdentityMgtConstants.Claim.ACCOUNT_LOCK, Boolean.toString(false));
                userStoreMng.setUserClaimValues(adminUserName, claimMap, null);
            }

        } catch (UserStoreException e) {
            log.error("Error while init identity listener", e);
        }
    }

    /**
     * This method checks if the user account exist or is locked. If the account is
     * locked, the authentication process will be terminated after this method
     * returning false.
     */
    @Override
    public boolean doPreAuthenticate(String userName, Object credential,
                                     UserStoreManager userStoreManager) throws UserStoreException {

        if (log.isDebugEnabled()) {
            log.debug("Pre authenticator is called in IdentityMgtEventListener");
        }

        getConfigurations(userStoreManager);

        IdentityMgtConfig config = IdentityMgtConfig.getInstance();

        String eventName = IdentityMgtConstants.Event.PRE_AUTHENTICATION;

        HashMap<String, Object> properties = new HashMap<String, Object>();
        properties.put(IdentityMgtConstants.EventProperty.MODULE, module);
        properties.put(IdentityMgtConstants.EventProperty.USER_NAME, userName);
        properties.put(IdentityMgtConstants.EventProperty.USER_STORE_MANAGER, userStoreManager);
        properties.put(IdentityMgtConstants.EventProperty.IDENTITY_MGT_CONFIG, config);

        IdentityMgtEvent identityMgtEvent = new IdentityMgtEvent(eventName, properties);
        try {
            identityMgtService.handleEvent(identityMgtEvent);
        } catch (IdentityMgtException e) {
            throw new UserStoreException("Error when authenticating user");
        }

        return true;
    }

    /**
     * This method retrieves the configurations for the tenant ID of the user
     */
    protected void getConfigurations(UserStoreManager userStoreManager) {

        try {
            IdentityMgtConfig.getIdentityMgtConfig(userStoreManager.getTenantId());
        } catch (UserStoreException e) {
            log.error(e.getMessage(), e);
        }

    }
}
