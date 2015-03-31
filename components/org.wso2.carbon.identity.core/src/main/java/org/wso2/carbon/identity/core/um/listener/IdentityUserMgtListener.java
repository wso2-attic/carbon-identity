/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.identity.core.um.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.core.IdentityRegistryResources;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.registry.core.Association;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.jdbc.utils.Transaction;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.listener.UserStoreManagerListener;

import java.util.Map;

/**
 * This is the class for deleting IS specific resources in registry for user.
 * Triggered before user profile is being deleted in registry. 
 */

public class IdentityUserMgtListener implements UserStoreManagerListener {

    private static Log log = LogFactory.getLog(IdentityUserMgtListener.class);

    public boolean addUser(String userName, Object credential, String[] roleList,
            Map<String, String> claims, String profileName, UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

    public boolean authenticate(String userName, Object credential,
            UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    public boolean deleteUser(String userName, UserStoreManager userStoreManager)
            throws UserStoreException {
        String profilePath = RegistryConstants.PROFILES_PATH + userName;
        int tenantId = userStoreManager.getTenantId();
        boolean transactionStarted = Transaction.isStarted();
        try {
            Registry registry = IdentityTenantUtil.getConfigRegistry(tenantId);
            try {
                if (!transactionStarted) {
                    registry.beginTransaction();
                }
                Association[] associations = registry.getAssociations(profilePath,
                        IdentityRegistryResources.ASSOCIATION_USER_INFOCARD);
                deleteTargets(registry, associations);
                associations = registry.getAssociations(profilePath,
                        IdentityRegistryResources.ASSOCIATION_USER_OPENID_RP);
                deleteTargets(registry, associations);
                associations = registry.getAssociations(profilePath,
                        IdentityRegistryResources.ASSOCIATION_USER_PPID);
                deleteTargets(registry, associations);
                associations = registry.getAssociations(profilePath,
                        IdentityRegistryResources.ASSOCIATION_USER_TRUSTED_RP);
                deleteTargets(registry, associations);
                associations = registry.getAssociations(profilePath,
                        IdentityRegistryResources.ASSOCIATION_USER_XMPP_SETTINGS);
                deleteTargets(registry, associations);
                associations = registry.getAssociations(profilePath,
                        IdentityRegistryResources.ASSOCIATION_USER_OPENID);
                deleteTargets(registry, associations);
                associations = registry.getAssociations(profilePath,
                        IdentityRegistryResources.ASSOCIATION_USER_OAUTH_APP);
                deleteTargets(registry, associations);
                if (!transactionStarted) {
                    registry.commitTransaction();
                }
            } catch (Exception e) {
                if (!transactionStarted) {
                    registry.rollbackTransaction();
                }
                String msg = "Error deleting resources in IS because " + e.getMessage();
                log.error(msg, e);
                throw new UserStoreException(msg, e);
            }
        } catch (Exception e) {
            String msg = "Error deleting resources in IS because " + e.getMessage();
            log.error(msg, e);
            throw new UserStoreException(msg, e);
        }
        return true;
    }

    public boolean updateRoleName(String s, String s1) throws UserStoreException {
        return false;
    }

    private void deleteTargets(Registry registry, Association[] associations)
            throws RegistryException {
        for (Association association : associations) {
            String targetPath = association.getDestinationPath();
            registry.delete(targetPath);
        }
    }

    public int getExecutionOrderId() {
        return UserStoreManagerListener.IDENTITY_UM_LISTENER_EXECUTION_ORDER_ID;
    }

    public boolean updateCredential(String userName, Object newCredential, Object oldCredential,
            UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    public boolean updateCredentialByAdmin(String userName, Object newCredential,
            UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }
}
