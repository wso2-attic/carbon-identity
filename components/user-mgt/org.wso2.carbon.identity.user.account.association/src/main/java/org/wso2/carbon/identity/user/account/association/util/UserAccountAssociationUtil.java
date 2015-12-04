/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.user.account.association.util;

import org.apache.xml.security.utils.Base64;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.identity.user.account.association.exception.UserAccountAssociationException;
import org.wso2.carbon.identity.user.account.association.internal.IdentityAccountAssociationServiceComponent;
import org.wso2.carbon.registry.core.utils.UUIDGenerator;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.listener.UserOperationEventListener;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class UserAccountAssociationUtil {

    private UserAccountAssociationUtil(){}

    /**
     * Generate random number for association key
     *
     * @return random number
     * @throws org.wso2.carbon.identity.user.account.association.exception.UserAccountAssociationException
     */
    public static String getRandomNumber() throws UserAccountAssociationException {
        try {
            String secretKey = UUIDGenerator.generateUUID();
            String baseString = UUIDGenerator.generateUUID();

            SecretKeySpec key = new SecretKeySpec(secretKey.getBytes(), "HmacSHA1");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(key);
            byte[] rawHmac = mac.doFinal(baseString.getBytes());
            String random = Base64.encode(rawHmac);
            random = random.replace("/", "_");
            random = random.replace("=", "a");
            random = random.replace("+", "f");
            return random;
        } catch (Exception e) {
            throw new UserAccountAssociationException("Error when generating a random number.", e);
        }
    }

    /**
     * Execute pre and post authentication listeners
     *
     * @param username
     * @param userStoreManager
     * @return is authentic
     * @throws UserStoreException
     */
    public static boolean executePrePostAuthenticationListeners(String username, UserStoreManager userStoreManager)
            throws UserStoreException {

        // Pre authentication listeners
        for (UserOperationEventListener listener : IdentityAccountAssociationServiceComponent
                .getUserOperationEventListeners()) {
            if (!listener.doPreAuthenticate(username, null, userStoreManager)) {
                return false;
            }
        }

        // Post authentication listeners
        for (UserOperationEventListener listener : IdentityAccountAssociationServiceComponent
                .getUserOperationEventListeners()) {
            if (!listener.doPostAuthenticate(username, true, userStoreManager)) {
                return false;
            }
        }

        return true;
    }

    public static String getUsernameWithoutDomain(String username) {
        int index = username.indexOf(CarbonConstants.DOMAIN_SEPARATOR);
        if (index < 0) {
            return username;
        }
        return username.substring(index + 1, username.length());
    }
}
