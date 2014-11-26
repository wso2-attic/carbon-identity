/*
*
*   Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*   WSO2 Inc. licenses this file to you under the Apache License,
*   Version 2.0 (the "License"); you may not use this file except
*   in compliance with the License.
*   You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*
*/

package org.wso2.carbon.identity.entitlement.listner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.entitlement.EntitlementException;
import org.wso2.carbon.identity.entitlement.NotificationConstants;
import org.wso2.carbon.identity.entitlement.cache.EntitlementPolicyInvalidationCache;
import org.wso2.carbon.identity.entitlement.internal.EntitlementServiceComponent;
import org.wso2.carbon.identity.entitlement.internal.UserNotificationConfig;
import org.wso2.carbon.identity.entitlement.model.PEPEndpointInfo;
import org.wso2.carbon.identity.entitlement.modules.EmailSendingModuleOnUserOperation;
import org.wso2.carbon.identity.entitlement.modules.MessageSendingModuleOnUserOperation;
import org.wso2.carbon.identity.entitlement.pdp.EntitlementEngine;
import org.wso2.carbon.identity.entitlement.pip.AbstractPIPAttributeFinder;
import org.wso2.carbon.identity.entitlement.pip.CarbonAttributeFinder;
import org.wso2.carbon.identity.entitlement.pip.PIPAttributeFinder;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.common.AbstractUserOperationEventListener;

import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This listener is registered as a user operation listener. Whenever a user operation takes place
 * this listener fires notifications to registered endpoints
 */
public class UserOperationsNotificationListener extends AbstractUserOperationEventListener {

	private static final Log log = LogFactory.getLog(UserOperationsNotificationListener.class);

	private static ExecutorService threadPool = Executors.newFixedThreadPool(5);
	private UserNotificationConfig config = null;


	public UserOperationsNotificationListener(UserNotificationConfig config) {

		this.config = config;
	}

	@Override
	public boolean doPostDeleteUser(String userName, UserStoreManager userStoreManager)
            throws UserStoreException {

		if(log.isDebugEnabled()) {
			log.debug("Clearing local cache and sending user delete notification");
		}

        sendNotification(NotificationConstants.EVENT_TYPE_PROFILE_UPDATE, userName);
		return true;
	}

	@Override
	public int getExecutionOrderId() {
		return 1360;
	}

	@Override
	public boolean doPostDeleteUserClaimValues(String userName, UserStoreManager userStoreManager)
            throws UserStoreException {

		if(log.isDebugEnabled()) {
			log.debug("Updated claims of user profile of user: " + userName);
			log.debug("clearing local cache and sending user profile update notification");
		}

        sendNotification(NotificationConstants.EVENT_TYPE_PROFILE_UPDATE, userName);
		return true;
	}

	@Override
	public boolean doPostDeleteUserClaimValue(String userName, UserStoreManager userStoreManager)
            throws UserStoreException {

		if(log.isDebugEnabled()) {
			log.debug("Updated claims of user profile of user: "+ userName);
			log.debug("Clearing local cache and sending user profile update notification");
		}

        sendNotification(NotificationConstants.EVENT_TYPE_PROFILE_UPDATE, userName);
		return true;
	}


	public boolean doPostUpdateRoleListOfUser(String userName,
	                                          String[] deletedRoles, String[] newRoles,
	                                          UserStoreManager userStoreManager)
            throws UserStoreException {

		if(log.isDebugEnabled()) {
			log.debug("Updated role of user: "+ userName +" Added roles: "+ Arrays.asList(newRoles) +
                    " Deleted roles: "+ Arrays.asList(deletedRoles));
			log.debug("Clearing local cache and sending user profile update notification");
		}

        sendNotification(NotificationConstants.EVENT_TYPE_ROLE_UPDATE, userName);
		return true;
	}

	@Override
	public boolean doPostSetUserClaimValues(String userName,
	                                        Map<String, String> claims, String profileName,
	                                        UserStoreManager userStoreManager)
            throws UserStoreException {

		if(log.isDebugEnabled()) {
			log.debug("Updated user profile of user: "+ userName +" with claims: "+ claims);
			log.debug("Clearing local cache and sending user profile update notification");
		}

		sendNotification(NotificationConstants.EVENT_TYPE_PROFILE_UPDATE, userName);
		return true;
	}

    private void sendNotification(String operation, String username){

        if(config.getEmailEnabled()) {
            EmailSendingModuleOnUserOperation module = new EmailSendingModuleOnUserOperation(
                    NotificationConstants.EVENT_TYPE_PROFILE_UPDATE);
            module.setEmailAddress(config.getEmailGroup());
            module.setSalutation(config.getEmailUsername());
            module.setSubjectId(username);
            threadPool.submit(module);
        }

        PEPEndpointInfo endpointInfo = config.getTargetEndpoint();
        String notificationType = config.getNotificationType();

        MessageSendingModuleOnUserOperation moduleMsg = new MessageSendingModuleOnUserOperation(
                endpointInfo, notificationType, operation);
        moduleMsg.setSubjectId(username);
        threadPool.submit(moduleMsg);
    }
}
