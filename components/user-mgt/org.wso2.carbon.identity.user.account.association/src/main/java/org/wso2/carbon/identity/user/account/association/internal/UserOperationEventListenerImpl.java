/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.user.account.association.internal;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.core.AbstractIdentityUserOperationEventListener;
import org.wso2.carbon.identity.core.util.IdentityCoreConstants;
import org.wso2.carbon.identity.user.account.association.dao.UserAccountAssociationDAO;
import org.wso2.carbon.identity.user.account.association.exception.UserAccountAssociationException;
import org.wso2.carbon.identity.user.account.association.util.UserAccountAssociationConstants;
import org.wso2.carbon.user.api.Permission;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.util.Map;

public class UserOperationEventListenerImpl extends AbstractIdentityUserOperationEventListener {

    private static final Log log = LogFactory.getLog(UserOperationEventListenerImpl.class);

    private static final int EXEC_ORDER = 40;

    @Override
    public int getExecutionOrderId() {
        int orderId = getOrderId();
        if (orderId != IdentityCoreConstants.EVENT_LISTENER_ORDER_ID) {
            return orderId;
        }
        return EXEC_ORDER;
    }

    @Override
    public boolean doPreAuthenticate(String s, Object o, UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostAuthenticate(String s, boolean b, UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPreAddUser(String s, Object o, String[] strings, Map<String, String> stringStringMap, String s2,
                                UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostAddUser(String s, Object o, String[] strings, Map<String, String> stringStringMap, String s2,
                                 UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPreUpdateCredential(String s, Object o, Object o2, UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostUpdateCredential(String s, Object o, UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPreUpdateCredentialByAdmin(String s, Object o, UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostUpdateCredentialByAdmin(String s, Object o, UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPreDeleteUser(String userName, UserStoreManager userStoreManager) throws UserStoreException {
        if (!isEnable()) {
            return true;
        }

        String domainName = UserCoreUtil.getDomainName(userStoreManager.getRealmConfiguration());
        if (StringUtils.isBlank(domainName)) {
            domainName = UserAccountAssociationConstants.PRIMARY_USER_DOMAIN;
        }

        try {
            if (log.isDebugEnabled()) {
                log.debug("User account associations for user " + userName + " with tenant id " +
                          userStoreManager.getTenantId() + " is getting deleted.");
            }

            UserAccountAssociationDAO.getInstance().deleteUserAssociation(domainName, userStoreManager.getTenantId()
                    , userName);
            return true;

        } catch (UserAccountAssociationException e) {
            throw new UserStoreException(String.format(UserAccountAssociationConstants.ErrorMessages
                                                 .ERROR_WHILE_DELETING_USER_ASSOC.getDescription(), userName), e);
        }
    }

    @Override
    public boolean doPostDeleteUser(String userName, UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPreSetUserClaimValue(String s, String s2, String s3, String s4, UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostSetUserClaimValue(String s, UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPreSetUserClaimValues(String s, Map<String, String> stringStringMap, String s2,
                                           UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostSetUserClaimValues(String s, Map<String, String> stringStringMap, String s2,
                                            UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPreDeleteUserClaimValues(String s, String[] strings, String s2, UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostDeleteUserClaimValues(String s, UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPreDeleteUserClaimValue(String s, String s2, String s3, UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostDeleteUserClaimValue(String s, UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPreAddRole(String s, String[] strings, Permission[] permissions, UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostAddRole(String s, String[] strings, Permission[] permissions,
                                 UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPreDeleteRole(String s, UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostDeleteRole(String s, UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPreUpdateRoleName(String s, String s2, UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostUpdateRoleName(String s, String s2, UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPreUpdateUserListOfRole(String s, String[] strings, String[] strings2,
                                             UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostUpdateUserListOfRole(String s, String[] strings, String[] strings2,
                                              UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPreUpdateRoleListOfUser(String s, String[] strings, String[] strings2,
                                             UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostUpdateRoleListOfUser(String s, String[] strings, String[] strings2,
                                              UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }
}
