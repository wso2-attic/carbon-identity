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

package org.wso2.carbon.identity.user.account.association;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.identity.user.account.association.dto.UserAccountAssociationDTO;
import org.wso2.carbon.identity.user.account.association.exception.UserAccountAssociationClientException;
import org.wso2.carbon.identity.user.account.association.exception.UserAccountAssociationException;
import org.wso2.carbon.identity.user.account.association.exception.UserAccountAssociationServerException;
import org.wso2.carbon.identity.user.account.association.internal.IdentityAccountAssociationServiceComponent;
import org.wso2.carbon.identity.user.account.association.util.UserAccountAssociationConstants;
import org.wso2.carbon.user.api.UserRealmService;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

public class UserAccountAssociationService extends AbstractAdmin {

    private static final Log log = LogFactory.getLog(UserAccountAssociationService.class);

    /**
     * Create new user account association
     *
     * @param userName
     * @param password
     * @throws org.wso2.carbon.identity.user.account.association.exception.UserAccountAssociationClientException
     */
    public void createUserAccountAssociation(String userName, char [] password) throws
                                                                     UserAccountAssociationClientException {
        try {
            UserAccountConnectorImpl.getInstance().createUserAccountAssociation(userName, password);
        } catch (UserAccountAssociationServerException e) {
            log.error(UserAccountAssociationConstants.ErrorMessages.ACCOUNT_CONNECTING_ERROR.getDescription(), e);
            throw new UserAccountAssociationClientException(UserAccountAssociationConstants.ErrorMessages
                                                                    .ACCOUNT_CONNECTING_ERROR.toString());
        } catch (UserAccountAssociationException e) {
            throw (UserAccountAssociationClientException) e;
        }
    }

    /**
     * Delete an existing user account association
     *
     * @param userName
     * @throws org.wso2.carbon.identity.user.account.association.exception.UserAccountAssociationClientException
     */
    public void deleteUserAccountAssociation(String userName) throws UserAccountAssociationClientException {
        try {
            UserAccountConnectorImpl.getInstance().deleteUserAccountAssociation(userName);
        } catch (UserAccountAssociationServerException e) {
            log.error(UserAccountAssociationConstants.ErrorMessages.CONN_DELETE_ERROR.getDescription(), e);
            throw new UserAccountAssociationClientException(UserAccountAssociationConstants.ErrorMessages
                                                                    .CONN_DELETE_ERROR.toString());
        } catch (UserAccountAssociationException e) {
            throw (UserAccountAssociationClientException) e;
        }
    }

    /**
     * Get all associated accounts of the logged in user
     *
     * @return
     * @throws org.wso2.carbon.identity.user.account.association.exception.UserAccountAssociationClientException
     */
    public UserAccountAssociationDTO[] getAccountAssociationsOfUser() throws UserAccountAssociationClientException {
        try {
            return UserAccountConnectorImpl.getInstance().getAccountAssociationsOfUser(CarbonContext.getThreadLocalCarbonContext().getUsername());
        } catch (UserAccountAssociationServerException e) {
            log.error(UserAccountAssociationConstants.ErrorMessages.CONN_LIST_ERROR.getDescription(), e);
            throw new UserAccountAssociationClientException(UserAccountAssociationConstants.ErrorMessages
                                                                    .CONN_LIST_ERROR.toString());
        } catch (UserAccountAssociationException e) {
            throw (UserAccountAssociationClientException) e;
        }
    }

    /**
     * Get all associated accounts of a user
     *
     * @param username username of user
     * @return
     * @throws org.wso2.carbon.identity.user.account.association.exception.UserAccountAssociationClientException
     */
    public UserAccountAssociationDTO[] getAccountAssociationsOfUserAsAdmin(String username) throws
            UserAccountAssociationClientException {
        try {
            return UserAccountConnectorImpl.getInstance().getAccountAssociationsOfUser(username);
        } catch (UserAccountAssociationServerException e) {
            log.error(UserAccountAssociationConstants.ErrorMessages.CONN_LIST_ERROR.getDescription(), e);
            throw new UserAccountAssociationClientException(UserAccountAssociationConstants.ErrorMessages
                    .CONN_LIST_ERROR.toString());
        } catch (UserAccountAssociationException e) {
            throw (UserAccountAssociationClientException) e;
        }
    }

   /**
     * Get all associated accounts for a not logged in user
     *
     * @param userName username of user
     * @param password passwd
     * @return
     * @throws UserAccountAssociationClientException
     */
    public UserAccountAssociationDTO[] getAccountAssociationsOfUserWithoutLogin(String userName, String password) throws
            UserAccountAssociationClientException {
        try {

            String tenantDomain = MultitenantUtils.getTenantDomain(userName);
            UserRealmService realmService = IdentityAccountAssociationServiceComponent.getRealmService();
            int tenantId = realmService.getTenantManager().getTenantId(tenantDomain);
            org.wso2.carbon.user.api.UserRealm userRealm = realmService.getTenantUserRealm(tenantId);
            String tenantAwareUsername = MultitenantUtils.getTenantAwareUsername(userName);
            boolean isAuthenticated = userRealm.getUserStoreManager().authenticate(tenantAwareUsername, password);
            if(!isAuthenticated){
                throw new UserAccountAssociationClientException("Invalid credential provided ");
            }
            return UserAccountConnectorImpl.getInstance().getAccountAssociationsOfUser(userName);
        } catch (UserAccountAssociationServerException e) {
            log.error(UserAccountAssociationConstants.ErrorMessages.CONN_LIST_ERROR.getDescription(), e);
            throw new UserAccountAssociationClientException(UserAccountAssociationConstants.ErrorMessages
                    .CONN_LIST_ERROR.toString());
        } catch (UserAccountAssociationException e) {
            throw (UserAccountAssociationClientException) e;
        } catch (UserStoreException e) {
            throw new UserAccountAssociationClientException("Error while authenticating user.");
        }
    }

    /**
     * Switch logged in user account to the required associated user account
     *
     * @param userName
     * @return
     * @throws org.wso2.carbon.identity.user.account.association.exception.UserAccountAssociationClientException
     */
    public boolean switchLoggedInUser(String userName) throws UserAccountAssociationClientException {
        try {
            return UserAccountConnectorImpl.getInstance().switchLoggedInUser(userName);
        } catch (UserAccountAssociationServerException e) {
            log.error(UserAccountAssociationConstants.ErrorMessages.ACCOUNT_SWITCHING_ERROR.getDescription(), e);
            throw new UserAccountAssociationClientException(UserAccountAssociationConstants.ErrorMessages
                                                                    .ACCOUNT_SWITCHING_ERROR.toString());
        } catch (UserAccountAssociationException e) {
            throw (UserAccountAssociationClientException) e;
        }
    }

}
