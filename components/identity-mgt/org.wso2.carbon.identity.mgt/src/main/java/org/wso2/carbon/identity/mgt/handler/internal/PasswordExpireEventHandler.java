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
import org.wso2.carbon.identity.mgt.IdentityMgtConfig;
import org.wso2.carbon.identity.mgt.constants.IdentityMgtConstants;
import org.wso2.carbon.identity.mgt.dto.UserIdentityClaimsDO;
import org.wso2.carbon.identity.mgt.event.IdentityMgtEvent;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class PasswordExpireEventHandler extends AbstractEventHandler {

    private static final Log log = LogFactory.getLog(PasswordExpireEventHandler.class);

    // save what are the event types registered in this handler
    protected ArrayList<String> registeredEventList;

    @Override
    public void init() {

        registeredEventList = new ArrayList<>() {{
            add("POST_AUTHENTICATION");
        }};
    }


    @Override
    public boolean isRegistered(IdentityMgtEvent event) {

        HashMap<String, Object> properties = event.getEventProperties();
        IdentityMgtConfig config = (IdentityMgtConfig) properties.get(IdentityMgtConstants.EventProperty.IDENTITY_MGT_CONFIG);
        if (config.isAccountPasswordExpireEnable()) {
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

        UserIdentityClaimsDO userIdentityDTO = (UserIdentityClaimsDO) properties.get("userIdentityDTO");
        IdentityMgtConfig config = (IdentityMgtConfig) properties.get("identityMgtConfig");
        boolean authenticated = (Boolean) properties.get("authenticated");
        Boolean userOTPEnabled = (Boolean) properties.get("userOTPEnabled");
        UserStoreManager userStoreManager = (UserStoreManager) properties.get("userStoreManager");


        // Password expire check. Not for OTP enabled users.
        if (authenticated && config.isAuthPolicyExpirePasswordCheck() && !userOTPEnabled && (!userStoreManager.isReadOnly())) {

            long expireTimeConfiguration = TimeUnit.DAYS.toMillis(config.getPasswordExpireTime());
            long lastPasswordChangeTime = userIdentityDTO.getPasswordTimeStamp();
            long currentTime = Calendar.getInstance().getTimeInMillis();


            int expireFrequencyConfiguration = config.getPasswordExpireFrequency();
            int noOfTimePasswordIsUsed = userIdentityDTO.getPasswordUseFrequency();

            // Password Expire based on time
            if (lastPasswordChangeTime > 0 && ((currentTime - lastPasswordChangeTime) > expireTimeConfiguration)) {
                if (log.isDebugEnabled()) {
                    log.debug("Same password is used for maximum duration, has to change the password");
                }
                throw new UserStoreException(
                        "Password is expired after using maximum duration :" + expireTimeConfiguration);


            }

            // Password Expire based on frequency
            else if (noOfTimePasswordIsUsed > expireFrequencyConfiguration) {
                if (log.isDebugEnabled()) {
                    log.debug("Same password is used for maximum no of times, has to change the password");
                }
                throw new UserStoreException(
                        "Password is expired after using : " + noOfTimePasswordIsUsed + " no of times");

            }

        }

        return true;
    }

}