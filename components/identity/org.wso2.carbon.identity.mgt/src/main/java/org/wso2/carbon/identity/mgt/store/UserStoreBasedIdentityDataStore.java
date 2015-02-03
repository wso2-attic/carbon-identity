/*
 * Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.mgt.store;

import java.util.HashMap;
import java.util.Map;

import javax.cache.Cache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.mgt.dto.UserIdentityClaimsDO;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.claim.Claim;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.jdbc.JDBCUserStoreManager;
import org.wso2.carbon.user.core.ldap.ActiveDirectoryUserStoreManager;
import org.wso2.carbon.user.core.ldap.ReadWriteLDAPUserStoreManager;

/**
 * This module persists data in to user store as user's attribute
 * //TODO remove method when user is deleted
 */
public class UserStoreBasedIdentityDataStore extends InMemoryIdentityDataStore {

    private static Log log = LogFactory.getLog(UserStoreBasedIdentityDataStore.class);
    private static ThreadLocal<String> userStoreInvoked = new ThreadLocal<String>(){
    	protected String initialValue() {return "FALSE";};
    };

    /**
     * This method stores data in the read write user stores. 
     */
    @Override
    public void store(UserIdentityClaimsDO userIdentityDTO, UserStoreManager userStoreManager) throws IdentityException {
    	
    	UserIdentityClaimsDO newIdentityClaimDO = new UserIdentityClaimsDO(userIdentityDTO.getUserName(), userIdentityDTO.getUserDataMap());
        super.store(newIdentityClaimDO, userStoreManager);
        
        if(userIdentityDTO.getUserName() == null){
            log.error("Error while persisting user data.  Null user name is provided.");
            return;
        }
        // using userstore implementations directly to avoid listeners which can cause for infinite loops
		try {
			if (userStoreManager instanceof JDBCUserStoreManager) {
				((JDBCUserStoreManager) userStoreManager).doSetUserClaimValues(userIdentityDTO.getUserName(),
				                                                             userIdentityDTO.getUserDataMap(),
				                                                             null);
			} else if (userStoreManager instanceof ActiveDirectoryUserStoreManager) {
				((ActiveDirectoryUserStoreManager) userStoreManager).doSetUserClaimValues(userIdentityDTO.getUserName(),
				                                                                        userIdentityDTO.getUserDataMap(),
				                                                                        null);
			} else if (userStoreManager instanceof ReadWriteLDAPUserStoreManager) {
				((ReadWriteLDAPUserStoreManager) userStoreManager).doSetUserClaimValues(userIdentityDTO.getUserName(),
				                                                                      userIdentityDTO.getUserDataMap(),
				                                                                      null);
			} else {
				throw new IdentityException("Cannot persist identity data in to the user store");
			}

		} catch (org.wso2.carbon.user.api.UserStoreException e) {
            throw new IdentityException("Error while persisting identity user data in to user store", e);
		}
    }

    /**
     * This method loads identity and security questions from the user stores
     */
	@Override
	public UserIdentityClaimsDO load(String userName, UserStoreManager userStoreManager){
		UserIdentityClaimsDO userIdentityDTO = super.load(userName, userStoreManager);
		if (userIdentityDTO != null) {
			return userIdentityDTO;
		}
		
		if (userStoreInvoked.get().equals("TRUE")) {
			return null;
		} else {
			userStoreInvoked.set("TRUE");
		}
		
		Map<String, String> userDataMap = new HashMap<String, String>();
		try {
			// reading all claims of the user
			Claim[] claims =
			                 ((AbstractUserStoreManager) userStoreManager).getUserClaimValues(userName,
			                                                                                  null);
			// select the security questions and identity claims
            if(claims != null) {
                for (Claim claim : claims) {
                    String claimUri = claim.getClaimUri();
                    if (claimUri.contains(UserCoreConstants.ClaimTypeURIs.IDENTITY_CLAIM_URI) ||
                            claimUri.contains(UserCoreConstants.ClaimTypeURIs.CHALLENGE_QUESTION_URI)) {
                        userDataMap.put(claimUri, claim.getValue());
                    }
                }
            }
		} catch (UserStoreException e) {
			// ignore may be user is not exist
		}
		// if user is exiting there must be at least one user attribute.
		if (userDataMap != null && userDataMap.size() > 0) {
			userIdentityDTO = new UserIdentityClaimsDO(userName, userDataMap);
			int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
			userIdentityDTO.setTenantId(tenantId);

			Cache<String, UserIdentityClaimsDO> cache = getCache();
			if(cache != null) {
				cache.put(tenantId + userName, userIdentityDTO);
			}
			return userIdentityDTO;
		}
		return null;
	}
    
}
