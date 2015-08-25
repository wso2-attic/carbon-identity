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

package org.wso2.carbon.identity.mgt.model;

import org.wso2.carbon.identity.mgt.IdentityMgtConstants;
import org.wso2.carbon.identity.mgt.store.UserIdentityDataStore;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * This encapsulates the user's data that is related user's login information
 */
public class UserIdentityClaim implements Serializable {

    private String userName;
    private long unlockTime;
    private boolean accountLock;
    private int failedAttempts;
    private Map<String, String> userIdentityDataMap = new HashMap<>();

    public UserIdentityClaim(String userName, Map<String, String> userDataMap) {

        this.userName = userName;
        this.userIdentityDataMap = userDataMap;

        if (userDataMap.get(IdentityMgtConstants.Claim.UNLOCKING_TIME) != null) {
            String unlockTime = userDataMap.get(IdentityMgtConstants.Claim.UNLOCKING_TIME).trim();
            if (!unlockTime.isEmpty()) {
                setUnlockTime(Long.parseLong(unlockTime));
            } else {
                setUnlockTime(0);
            }
        }
        if (userDataMap.get(IdentityMgtConstants.Claim.ACCOUNT_LOCK) != null) {
            setAccountLock(Boolean.parseBoolean(userDataMap.get(IdentityMgtConstants.Claim.ACCOUNT_LOCK)));
        }

        if (userDataMap.get(IdentityMgtConstants.Claim.FAIL_LOGIN_ATTEMPTS) != null) {
            String failedAttempts = userDataMap.get(IdentityMgtConstants.Claim.FAIL_LOGIN_ATTEMPTS)
                    .trim();
            if (!failedAttempts.isEmpty()) {
                setFailAttempts(Integer.parseInt(failedAttempts));
            } else {
                setFailAttempts(0);
            }
        }
    }

    public boolean isAccountLocked() {
        if (unlockTime != 0 && unlockTime < System.currentTimeMillis()) {
            return false;
        }
        return accountLock;
    }

    public UserIdentityClaim setAccountLock(boolean accountLock) {
        this.accountLock = accountLock;
        this.userIdentityDataMap.put(IdentityMgtConstants.Claim.ACCOUNT_LOCK,
                Boolean.toString(accountLock));
        return this;
    }

    public boolean getAccountLock() {
        return accountLock;
    }

    public void setUnlockTime(long unlockTime) {
        this.unlockTime = unlockTime;
        this.userIdentityDataMap.put(IdentityMgtConstants.Claim.UNLOCKING_TIME,
                Long.toString(unlockTime));
    }

    public long getUnlockTime() {
        return unlockTime;
    }

    public int getFailAttempts() {
        return failedAttempts;
    }

    public void setFailAttempts(int failAttempts) {
        this.failedAttempts = failAttempts;
        this.userIdentityDataMap.put(IdentityMgtConstants.Claim.FAIL_LOGIN_ATTEMPTS,
                Integer.toString(failAttempts));
    }
}


