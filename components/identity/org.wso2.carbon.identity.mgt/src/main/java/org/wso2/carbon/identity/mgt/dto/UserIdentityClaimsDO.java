/*
 * Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.mgt.dto;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.wso2.carbon.identity.mgt.store.UserIdentityDataStore;
import org.wso2.carbon.user.core.UserCoreConstants;

/**
 * This encapsulates the user's data that is related user's login information
 */
public class UserIdentityClaimsDO implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -5135352332427377484L;
	public static final int TRUE = 1;
	public static final int FALSE = 2;

	private String userName;
	private int tenantId;

	private long unlockTime;
	private long lastLogonTime;
	private long lastFailAttemptTime;
	private long passwordTimeStamp;
	private int failedAttempts;
	private boolean accountLock;
	private boolean passwordChangeRequired;
	private boolean oneTimeLogin;
	private Map<String, String> userIdentityDataMap = new HashMap<String, String>();
	private char[] temporaryPassword = null;
	private String confirmationCode = null;

	public UserIdentityClaimsDO(String userName) {
		this.userName = userName;
	}

	public UserIdentityClaimsDO(String userName, Map<String, String> userDataMap) {

		this.userName = userName;
		this.userIdentityDataMap = userDataMap;

		if (userDataMap.get(UserIdentityDataStore.FAIL_LOGIN_ATTEMPTS) != null) {
			setFailAttempts(Integer.parseInt(userDataMap.get(UserIdentityDataStore.FAIL_LOGIN_ATTEMPTS)));
		}
		if (userDataMap.get(UserIdentityDataStore.LAST_FAILED_LOGIN_ATTEMPT_TIME) != null) {
			setLastFailAttemptTime(Long.parseLong(userDataMap.get(UserIdentityDataStore.LAST_FAILED_LOGIN_ATTEMPT_TIME)));
		}
		if (userDataMap.get(UserIdentityDataStore.UNLOCKING_TIME) != null) {
			setUnlockTime(Long.parseLong(userDataMap.get(UserIdentityDataStore.UNLOCKING_TIME)));
		}
		if (userDataMap.get(UserIdentityDataStore.ONE_TIME_PASSWORD) != null) {
			setOneTimeLogin(Boolean.parseBoolean(userDataMap.get(UserIdentityDataStore.ONE_TIME_PASSWORD)));
		}
		if (userDataMap.get(UserIdentityDataStore.PASSWORD_CHANGE_REQUIRED) != null) {
			setPasswordChangeRequired(Boolean.parseBoolean(userDataMap.get(UserIdentityDataStore.PASSWORD_CHANGE_REQUIRED)));
		}
		if (userDataMap.get(UserIdentityDataStore.LAST_LOGON_TIME) != null) {
			setLastLogonTime(Long.parseLong(userDataMap.get(UserIdentityDataStore.LAST_LOGON_TIME)));
		}
		if (userDataMap.get(UserIdentityDataStore.ACCOUNT_LOCK) != null) {
			setAccountLock(Boolean.parseBoolean(userDataMap.get(UserIdentityDataStore.ACCOUNT_LOCK)));
		}
		if (userDataMap.get(UserIdentityDataStore.PASSWORD_TIME_STAMP) != null) {
			setPasswordTimeStamp(Long.parseLong(userDataMap.get(UserIdentityDataStore.PASSWORD_TIME_STAMP)));
		}
//		if (userDataMap.get(UserIdentityDataStore.PASSWORD_TIME_STAMP) != null) {
//			setPasswordTimeStamp(Long.parseLong(userDataMap.get(UserIdentityDataStore.PASSWORD_TIME_STAMP)));
//		}
	}

	public String getUserName() {
		return userName;
	}

	public long getUnlockTime() {
		return unlockTime;
	}

	public Map<String, String> getUserIdentityDataMap() {
		return userIdentityDataMap;
	}

	public void setUnlockTime(long unlockTime) {
		this.unlockTime = unlockTime;
		this.userIdentityDataMap.put(UserIdentityDataStore.UNLOCKING_TIME,
		                             Long.toString(unlockTime));
	}

	public long getLastLogonTime() {
		return lastLogonTime;
	}

	public void setLastLogonTime(long lastLogonTime) {
		this.lastLogonTime = lastLogonTime;
		this.userIdentityDataMap.put(UserIdentityDataStore.LAST_LOGON_TIME,
		                             Long.toString(lastLogonTime));
	}

	public long getLastFailAttemptTime() {
		return lastFailAttemptTime;
	}

	public void setLastFailAttemptTime(long lastFailAttemptTime) {
		this.lastFailAttemptTime = lastFailAttemptTime;
		this.userIdentityDataMap.put(UserIdentityDataStore.LAST_FAILED_LOGIN_ATTEMPT_TIME,
		                             Long.toString(lastFailAttemptTime));
	}

	public int getFailAttempts() {
		return failedAttempts;
	}

	public void setFailAttempts() {
		this.failedAttempts++;
		this.userIdentityDataMap.put(UserIdentityDataStore.FAIL_LOGIN_ATTEMPTS,
		                             Integer.toString(failedAttempts));
	}

	public void setFailAttempts(int failAttempts) {
		this.failedAttempts = failAttempts;
		this.userIdentityDataMap.put(UserIdentityDataStore.FAIL_LOGIN_ATTEMPTS,
		                             Integer.toString(failAttempts));
	}

	public boolean getOneTimeLogin() {
		return oneTimeLogin;
	}

	public void setOneTimeLogin(boolean oneTimeLogin) {
		this.oneTimeLogin = oneTimeLogin;
		this.userIdentityDataMap.put(UserIdentityDataStore.ONE_TIME_PASSWORD,
		                             Boolean.toString(oneTimeLogin));
	}

	public boolean getPasswordChangeRequired() {
		return passwordChangeRequired;
	}

	public void setPasswordChangeRequired(boolean passwordChangeRequired) {
		this.passwordChangeRequired = passwordChangeRequired;
		this.userIdentityDataMap.put(UserIdentityDataStore.PASSWORD_CHANGE_REQUIRED,
		                             Boolean.toString(passwordChangeRequired));
	}

	public boolean isAccountLocked() {
		if (unlockTime != 0 && unlockTime < System.currentTimeMillis()) {
			return false;
		}
		return accountLock;
	}

    public boolean getAccountLock() {
        return accountLock;
    }

	public UserIdentityClaimsDO setAccountLock(boolean accountLock) {
		this.accountLock = accountLock;
		this.userIdentityDataMap.put(UserIdentityDataStore.ACCOUNT_LOCK,
		                             Boolean.toString(accountLock));
		return this;
	}

	public Map<String, String> getUserDataMap() {
		return userIdentityDataMap;
	}

	public void setUserDataMap(Map<String, String> userDataMap) {
		this.userIdentityDataMap = userDataMap;
	}

	/**
	 * Sets user identity data claim
	 * 
	 * @param claim
	 * @param value
	 */
	public void setUserIdentityDataClaim(String claim, String value) {
		userIdentityDataMap.put(claim, value);
	}

	public long getPasswordTimeStamp() {
		return passwordTimeStamp;
	}

	/**
	 * 
	 * @param passwordTimeStamp
	 * @return
	 */
	public UserIdentityClaimsDO setPasswordTimeStamp(long passwordTimeStamp) {
		this.passwordTimeStamp = passwordTimeStamp;
		this.userIdentityDataMap.put(UserIdentityDataStore.PASSWORD_TIME_STAMP,
		                             Long.toString(passwordTimeStamp));
		return this;
	}

	public int getTenantId() {
		return tenantId;
	}

	public void setTenantId(int tenantId) {
		this.tenantId = tenantId;
	}

	public boolean getBoolean(Object value) {
		int IntegerValue = (Integer) value;
		return IntegerValue == TRUE;
	}

	public char[] getTemporaryPassword() {
		return temporaryPassword;
	}

	public void setTemporaryPassword(char[] temporaryPassword) {
		this.temporaryPassword = temporaryPassword;
	}

	public String getConfirmationCode() {
		return confirmationCode;
	}

	public void setConfirmationCode(String confirmationCode) {
		this.confirmationCode = confirmationCode;
	}

	/**
	 * Update the security question
	 * 
	 * @param securityQuestions
	 */
	public void updateUserSequeiryQuestions(UserIdentityClaimDTO[] securityQuestions) {
		for (UserIdentityClaimDTO dto : securityQuestions) {
			// if the uri and the value is not null and if its only a security
			// question claim then update
			if (dto.getClaimUri() != null && dto.getClaimValue() != null &&
			    dto.getClaimUri().contains(UserCoreConstants.ClaimTypeURIs.CHALLENGE_QUESTION_URI)) {
				userIdentityDataMap.put(dto.getClaimUri(), dto.getClaimValue());
			}
		}
	}

	/**
	 * Returns all user identity claims
	 * 
	 * @return
	 */
	public UserIdentityClaimDTO[] getUserSequeiryQuestions() {
		Map<String, String> tempMap = new HashMap<String, String>();
		// reading them to a temporary map
		for (Map.Entry<String, String> entry : userIdentityDataMap.entrySet()) {
			// only if a security question uri
			if (entry.getKey().contains(UserCoreConstants.ClaimTypeURIs.CHALLENGE_QUESTION_URI)) {
				tempMap.put(entry.getKey(), entry.getValue());
			}
		}
		// no security questions found
		if (tempMap.size() == 0) {
			return null;
		}
		// creating claim dtos
		UserIdentityClaimDTO[] securityQuestions = new UserIdentityClaimDTO[tempMap.size()];
		int i = 0;
		for (Map.Entry<String, String> entry : tempMap.entrySet()) {
			UserIdentityClaimDTO dto = new UserIdentityClaimDTO();
			dto.setClaimUri(entry.getKey());
			dto.setClaimValue(entry.getValue());
			securityQuestions[i] = dto;
			i++;
		}
		return securityQuestions;
	}

	/**
	 * 
	 * @param userIdentityRecoveryData
	 */
	public void updateUserIdentityRecoveryData(UserIdentityClaimDTO[] userIdentityRecoveryData) {
		for (UserIdentityClaimDTO dto : userIdentityRecoveryData) {
			// if the uri and the value is not null and if not a security
			// question or an identity claim
			if (dto.getClaimUri() != null &&
			    dto.getClaimValue() != null &&
			    !dto.getClaimUri().contains(UserCoreConstants.ClaimTypeURIs.CHALLENGE_QUESTION_URI) &&
			    !dto.getClaimUri().contains(UserCoreConstants.ClaimTypeURIs.IDENTITY_CLAIM_URI)) {
				userIdentityDataMap.put(dto.getClaimUri(), dto.getClaimValue());
			}
		}
	}

	/**
	 * Returns user claims
	 * 
	 * @return
	 */
	public UserIdentityClaimDTO[] getUserIdentityRecoveryData() {
		Map<String, String> tempMap = new HashMap<String, String>();
		// reading them to a temporary map
		for (Map.Entry<String, String> entry : userIdentityDataMap.entrySet()) {
			// only if not a security question uri or an identity claim uri
			if (!entry.getKey().contains(UserCoreConstants.ClaimTypeURIs.CHALLENGE_QUESTION_URI) &&
			    !entry.getKey().contains(UserCoreConstants.ClaimTypeURIs.IDENTITY_CLAIM_URI)) {
				tempMap.put(entry.getKey(), entry.getValue());
			}
		}
		// no user claim found
		if (tempMap.size() == 0) {
			return null;
		}
		// creating claim dtos
		UserIdentityClaimDTO[] identityRecoveryData = new UserIdentityClaimDTO[tempMap.size()];
		int i = 0;
		for (Map.Entry<String, String> entry : tempMap.entrySet()) {
			UserIdentityClaimDTO dto = new UserIdentityClaimDTO();
			dto.setClaimUri(entry.getKey());
			dto.setClaimValue(entry.getValue());
			identityRecoveryData[i] = dto;
			i++;
		}
		return identityRecoveryData;
	}

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
