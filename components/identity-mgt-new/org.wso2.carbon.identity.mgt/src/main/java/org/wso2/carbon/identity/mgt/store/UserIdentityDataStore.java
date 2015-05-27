/*
 * Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.mgt.store;

import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.mgt.dto.UserIdentityClaimsDO;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.UserCoreConstants;

/**
 * This interface provides to plug module for preferred persistence store.
 */
public abstract class UserIdentityDataStore {

	public static final String FAIL_LOGIN_ATTEMPTS = "http://wso2.org/claims/identity/failedLoginAttempts";
	public static final String UNLOCKING_TIME = "http://wso2.org/claims/identity/unlockTime";
	public static final String ACCOUNT_LOCK = "http://wso2.org/claims/identity/accountLocked";

	/**
	 * Stores data
	 * 
	 * @param userIdentityDTO
	 * @param userStoreManager
	 */
	public abstract void store(UserIdentityClaimsDO userIdentityDTO, UserStoreManager userStoreManager)
                                                                            throws IdentityException;

	/**
	 * Loads
	 * 
	 * @param userName
	 * @param userStoreManager
	 * @return
	 */
	public abstract UserIdentityClaimsDO load(String userName, UserStoreManager userStoreManager);

}
