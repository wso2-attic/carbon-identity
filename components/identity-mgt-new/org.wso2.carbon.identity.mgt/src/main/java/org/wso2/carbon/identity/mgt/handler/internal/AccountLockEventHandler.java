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

package org.wso2.carbon.identity.mgt.handler.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.model.IdentityErrorMsgContext;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.mgt.IdentityMgtConfig;
import org.wso2.carbon.identity.mgt.IdentityMgtConstants;
import org.wso2.carbon.identity.mgt.IdentityMgtException;
import org.wso2.carbon.identity.mgt.model.UserIdentityClaim;
import org.wso2.carbon.identity.mgt.event.IdentityMgtEvent;
import org.wso2.carbon.identity.mgt.store.UserIdentityDataStore;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AccountLockEventHandler extends AbstractEventHandler {

    private static final Log log = LogFactory.getLog(AccountLockEventHandler.class);

    @Override
    public void init() {

        registeredEventList = new ArrayList<String>() {{
            add(IdentityMgtConstants.Event.PRE_AUTHENTICATION);
        }};
    }

    @Override
    public boolean handleEvent(IdentityMgtEvent identityMgtEvent) throws IdentityMgtException {

        Map<String, Object> properties = identityMgtEvent.getEventProperties();

        String userName = (String) properties.get(IdentityMgtConstants.EventProperty.USER_NAME);
        UserStoreManager userStoreManager = (UserStoreManager) properties.get(IdentityMgtConstants.EventProperty.USER_STORE_MANAGER);
        UserIdentityDataStore module = (UserIdentityDataStore) properties.get(IdentityMgtConstants.EventProperty.MODULE);
        IdentityMgtConfig config = (IdentityMgtConfig) properties.get(IdentityMgtConstants.EventProperty.IDENTITY_MGT_CONFIG);

        IdentityUtil.clearIdentityErrorMsg();

        if (!config.isEnableAuthPolicy()) {
            return true;
        }

        String domainName = userStoreManager.getRealmConfiguration().getUserStoreProperty(UserCoreConstants.RealmConfig.PROPERTY_DOMAIN_NAME);
        String usernameWithDomain = UserCoreUtil.addDomainToName(userName, domainName);
        boolean isUserExistInCurrentDomain = false;
        try {
            isUserExistInCurrentDomain = userStoreManager.isExistingUser(usernameWithDomain);
        } catch (UserStoreException e) {
            throw new IdentityMgtException("Error in accessing user store");
        }

        if (!isUserExistInCurrentDomain) {

            IdentityErrorMsgContext customErrorMessageContext = new IdentityErrorMsgContext(UserCoreConstants.ErrorCode.USER_DOES_NOT_EXIST);
            IdentityUtil.setIdentityErrorMsg(customErrorMessageContext);

            if (log.isDebugEnabled()) {
                log.debug("Username :" + userName + "does not exists in the system, ErrorMessage :" + UserCoreConstants.ErrorCode.USER_DOES_NOT_EXIST);
            }
            if (config.isAuthPolicyAccountExistCheck()) {
                throw new IdentityMgtException(UserCoreConstants.ErrorCode.USER_DOES_NOT_EXIST);
            }
        } else {

            UserIdentityClaim userIdentityDTO = module.load(userName, userStoreManager);

            // if the account is locked, should not be able to log in
            if (userIdentityDTO != null && userIdentityDTO.isAccountLocked()) {

                // If unlock time is specified then unlock the account.
                if ((userIdentityDTO.getUnlockTime() != 0) && (System.currentTimeMillis() >= userIdentityDTO.getUnlockTime())) {

                    userIdentityDTO.setAccountLock(false);
                    userIdentityDTO.setUnlockTime(0);

                    try {
                        module.store(userIdentityDTO, userStoreManager);
                    } catch (IdentityException e) {
                        log.error("Error while saving user : " + userName, e);
                        throw new IdentityMgtException("Error while saving user : " + userName);
                    }
                } else {
                    IdentityErrorMsgContext customErrorMessageContext = new IdentityErrorMsgContext(UserCoreConstants.ErrorCode.USER_IS_LOCKED,
                            userIdentityDTO.getFailAttempts(), config.getAuthPolicyMaxLoginAttempts());
                    IdentityUtil.setIdentityErrorMsg(customErrorMessageContext);
                    String errorMsg = "User account is locked for user : " + userName
                            + ". cannot login until the account is unlocked ";
                    throw new IdentityMgtException(UserCoreConstants.ErrorCode.USER_IS_LOCKED + " "
                            + errorMsg);
                }
            }
        }

        return true;
    }
}
