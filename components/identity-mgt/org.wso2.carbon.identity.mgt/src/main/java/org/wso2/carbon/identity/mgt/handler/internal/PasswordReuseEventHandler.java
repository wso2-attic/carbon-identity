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
import org.wso2.carbon.identity.core.model.IdentityErrorMsgContext;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.mgt.IdentityMgtConfig;
import org.wso2.carbon.identity.mgt.constants.IdentityMgtConstants;
import org.wso2.carbon.identity.mgt.dto.UserIdentityClaimsDO;
import org.wso2.carbon.identity.mgt.event.IdentityMgtEvent;
import org.wso2.carbon.identity.mgt.store.UserIdentityDataStore;
import org.wso2.carbon.idp.mgt.IdentityProviderManager;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class PasswordReuseEventHandler extends AbstractEventHandler {

    private static final Log log = LogFactory.getLog(PasswordReuseEventHandler.class);

    // save what are the event types registered in this handler
    protected ArrayList<String> registeredEventList;

    @Override
    public void init() {

        registeredEventList = new ArrayList<>() {{
            add(IdentityMgtConstants.Event.PRE_UPDATE_CREDENTIAL);
        }};
    }


    @Override
    public boolean isRegistered(IdentityMgtEvent event) {
        HashMap<String, Object> properties = event.getEventProperties();
        IdentityMgtConfig config = (IdentityMgtConfig) properties.get(IdentityMgtConstants.EventProperty.IDENTITY_MGT_CONFIG);

        if (config.isAccountPasswordReuseEnable()) {

            //check whether the event name is exists in the registeredEventList
            for (String eventName : registeredEventList) {
                if (eventName.equals(event.getEventName())) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean handleEvent(IdentityMgtEvent identityMgtEvent) throws UserStoreException {

        String eventName = identityMgtEvent.getEventName();
        HashMap<String, Object> properties = identityMgtEvent.getEventProperties();

        UserIdentityClaimsDO userIdentityDTO = (UserIdentityClaimsDO) properties.get(IdentityMgtConstants.UserClaimProperty.USER_IDENTITY_DTO);
        IdentityMgtConfig config = (IdentityMgtConfig) properties.get(IdentityMgtConstants.UserClaimProperty.IDENTITY_MGT_CONFIG);
        String credential = (String) properties.get(IdentityMgtConstants.UserClaimProperty.CREDENTIAL);

        long currentTime = Calendar.getInstance().getTimeInMillis();

        boolean isPasswordReused = false;

        Map<Long, Object> usedPasswordMap = userIdentityDTO.getUsedPasswordMap();


        // password reuse based on time
        List<Long> keyList = new ArrayList<>(usedPasswordMap.keySet());
        List<Object> valueList = new ArrayList<>(usedPasswordMap.values());
        int iterateCount = keyList.size() - 1;

        if (iterateCount != -1) {
            for (int i = iterateCount; i >= 0; i--) {

                long time = keyList.get(iterateCount);
                String password = (String) valueList.get(iterateCount);

                if ((currentTime - time) <= TimeUnit.DAYS.toMillis(config.getPasswordReuseTime())) {
                    if (password.equals(credential)) {
                        isPasswordReused = true;
                        break;
                    }
                } else {
                    break;
                }

                --iterateCount;

            }
        }


        // password reuse based on frequency
        int count = 1;
        int iterateCount2 = valueList.size() - 1;

        while (count <= config.getPasswordReuseFrequency()) {
            if (iterateCount2 != -1) {
                if (valueList.get(iterateCount2).equals(credential)) {
                    isPasswordReused = true;
                    break;
                } else {
                    --iterateCount2;
                    ++count;
                }
            } else {
                break;
            }

        }

        // check password is reused or not
        // if reused throw an error message
        if (isPasswordReused) {
            IdentityErrorMsgContext customErrorMessageContext = new IdentityErrorMsgContext(UserCoreConstants.ErrorCode.USER_IS_LOCKED,
                    userIdentityDTO.getFailAttempts(), config.getAuthPolicyMaxLoginAttempts());
            IdentityUtil.setIdentityErrorMsg(customErrorMessageContext);
            String errorMsg = "Error in password change: Used same password used before. Please use another password. ";
            throw new UserStoreException(errorMsg);
        }

        return true;
    }
}