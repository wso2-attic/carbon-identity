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
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.user.account.association.dao.UserAccountAssociationDAO;
import org.wso2.carbon.identity.user.account.association.dto.UserAccountAssociationDTO;
import org.wso2.carbon.identity.user.account.association.exception.UserAccountAssociationClientException;
import org.wso2.carbon.identity.user.account.association.exception.UserAccountAssociationException;
import org.wso2.carbon.identity.user.account.association.exception.UserAccountAssociationServerException;
import org.wso2.carbon.identity.user.account.association.internal.IdentityAccountAssociationServiceComponent;
import org.wso2.carbon.identity.user.account.association.util.UserAccountAssociationConstants;
import org.wso2.carbon.identity.user.account.association.util.UserAccountAssociationUtil;
import org.wso2.carbon.user.api.UserRealmService;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.util.UserCoreUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

public class UserAccountAssociationService extends AbstractAdmin {

    private static final Log log = LogFactory.getLog(UserAccountAssociationService.class);

    /**
     * Create new user account association
     *
     * @param userName Username of account to associate
     * @param password Password of account to associate
     * @throws org.wso2.carbon.identity.user.account.association.exception.UserAccountAssociationClientException
     */
    public void createUserAccountAssociation(String userName, char[] password) throws
            UserAccountAssociationClientException {

        String loggedInUser = UserCoreUtil.addTenantDomainToEntry(CarbonContext.getThreadLocalCarbonContext()
                .getUsername(), CarbonContext.getThreadLocalCarbonContext().getTenantDomain());

        org.wso2.carbon.user.api.UserRealm userRealm;
        RealmService realmService;
        String tenantAwareUsername = MultitenantUtils.getTenantAwareUsername(userName);
        int tenantId;
        try {
            realmService = IdentityAccountAssociationServiceComponent.getRealmService();
            tenantId = realmService.getTenantManager().getTenantId(MultitenantUtils.getTenantDomain(userName));
        } catch (UserStoreException e) {
            throw new UserAccountAssociationClientException(UserAccountAssociationConstants.ErrorMessages
                    .ERROR_WHILE_GETTING_TENANT_ID
                    .getDescription(), e);
        } catch (Exception e) {
            throw new UserAccountAssociationClientException(UserAccountAssociationConstants.ErrorMessages
                    .ERROR_WHILE_LOADING_REALM_SERVICE
                    .getDescription(), e);
        }

        if (MultitenantConstants.INVALID_TENANT_ID == tenantId) {
            if (log.isDebugEnabled()) {
                log.debug(String.format(UserAccountAssociationConstants.ErrorMessages.DEBUG_INVALID_TENANT_DOMAIN
                        .getDescription(), MultitenantUtils.getTenantDomain(userName)));
            }
            throw new UserAccountAssociationClientException(UserAccountAssociationConstants.ErrorMessages
                    .INVALID_TENANT_DOMAIN.toString());
        }
        boolean authentic;
        try {
            userRealm = realmService.getTenantUserRealm(tenantId);
            authentic = userRealm.getUserStoreManager().authenticate(tenantAwareUsername, String.valueOf(password));
            userName = UserCoreUtil.addDomainToName(userName, UserCoreUtil.getDomainFromThreadLocal());
        } catch (UserStoreException e) {
            throw new UserAccountAssociationClientException(UserAccountAssociationConstants.ErrorMessages
                    .ERROR_WHILE_AUTHENTICATING_USER
                    .getDescription(), e);
        }

        if (!authentic) {
            if (log.isDebugEnabled()) {
                log.debug(UserAccountAssociationConstants.ErrorMessages.USER_NOT_AUTHENTIC.getDescription());

            }
            throw new UserAccountAssociationClientException(UserAccountAssociationConstants.ErrorMessages
                    .USER_NOT_AUTHENTIC.toString());
        }
        try {
            UserAccountConnectorImpl.getInstance().createUserAccountAssociation(loggedInUser, userName);
        } catch (UserAccountAssociationServerException e) {
            log.error(UserAccountAssociationConstants.ErrorMessages.ACCOUNT_CONNECTING_ERROR.getDescription(), e);
            throw new UserAccountAssociationClientException(UserAccountAssociationConstants.ErrorMessages
                    .ACCOUNT_CONNECTING_ERROR.toString());
        } catch (UserAccountAssociationException e) {
            throw (UserAccountAssociationClientException) e;
        }
    }

    /**
     * Create new user account association as admin
     *
     * @param userName1 Username of first account to associate.
     * @param userName2 Username of second account to associate.
     * @throws org.wso2.carbon.identity.user.account.association.exception.UserAccountAssociationClientException
     */
    public void associateTwoAccounts(String userName1, String userName2) throws UserAccountAssociationClientException {

        RealmService realmService;
        int tenantId;
        try {
            realmService = IdentityAccountAssociationServiceComponent.getRealmService();
            tenantId = realmService.getTenantManager().getTenantId(MultitenantUtils.getTenantDomain(userName2));
        } catch (UserStoreException e) {
            throw new UserAccountAssociationClientException(UserAccountAssociationConstants.ErrorMessages
                    .ERROR_WHILE_GETTING_TENANT_ID
                    .getDescription(), e);
        } catch (Exception e) {
            throw new UserAccountAssociationClientException(UserAccountAssociationConstants.ErrorMessages
                    .ERROR_WHILE_LOADING_REALM_SERVICE
                    .getDescription(), e);
        }

        if (MultitenantConstants.INVALID_TENANT_ID == tenantId) {
            if (log.isDebugEnabled()) {
                log.debug(String.format(UserAccountAssociationConstants.ErrorMessages.DEBUG_INVALID_TENANT_DOMAIN
                        .getDescription(), MultitenantUtils.getTenantDomain(userName2)));
            }
            throw new UserAccountAssociationClientException(UserAccountAssociationConstants.ErrorMessages
                    .INVALID_TENANT_DOMAIN.toString());
        }
        try {
            UserAccountConnectorImpl.getInstance().createUserAccountAssociation(userName1, userName2);
        } catch (UserAccountAssociationServerException e) {
            log.error(UserAccountAssociationConstants.ErrorMessages.ACCOUNT_CONNECTING_ERROR.getDescription(), e);
            throw new UserAccountAssociationClientException(UserAccountAssociationConstants.ErrorMessages
                    .ACCOUNT_CONNECTING_ERROR.toString());
        } catch (UserAccountAssociationException e) {
            throw (UserAccountAssociationClientException) e;
        }
    }

    /**
     * Create new user account association without login
     *
     * @param userName1 Username of first account to associate.
     * @param password1 Password of first account to associate.
     * @param userName2 Username of second account to associate.
     * @param password2 Password of second account to associate.
     * @throws org.wso2.carbon.identity.user.account.association.exception.UserAccountAssociationClientException
     */
    public void associateMyAccounts(String userName1, char[] password1, String userName2, char[] password2) throws
            UserAccountAssociationClientException {

        org.wso2.carbon.user.api.UserRealm userRealm;
        RealmService realmService;
        String tenantAwareUsername1 = MultitenantUtils.getTenantAwareUsername(userName1);
        String tenantAwareUsername2 = MultitenantUtils.getTenantAwareUsername(userName2);
        int tenantId1;
        int tenantId2;
        try {
            realmService = IdentityAccountAssociationServiceComponent.getRealmService();
            tenantId1 = realmService.getTenantManager().getTenantId(MultitenantUtils.getTenantDomain(userName1));
            tenantId2 = realmService.getTenantManager().getTenantId(MultitenantUtils.getTenantDomain(userName2));
        } catch (UserStoreException e) {
            throw new UserAccountAssociationClientException(UserAccountAssociationConstants.ErrorMessages
                    .ERROR_WHILE_GETTING_TENANT_ID
                    .getDescription(), e);
        } catch (Exception e) {
            throw new UserAccountAssociationClientException(UserAccountAssociationConstants.ErrorMessages
                    .ERROR_WHILE_LOADING_REALM_SERVICE
                    .getDescription(), e);
        }
        if (MultitenantConstants.INVALID_TENANT_ID == tenantId1) {
            if (log.isDebugEnabled()) {
                log.debug(String.format(UserAccountAssociationConstants.ErrorMessages.DEBUG_INVALID_TENANT_DOMAIN
                        .getDescription(), MultitenantUtils.getTenantDomain(userName1)));
            }
            throw new UserAccountAssociationClientException(UserAccountAssociationConstants.ErrorMessages
                    .INVALID_TENANT_DOMAIN.toString());
        }
        if (MultitenantConstants.INVALID_TENANT_ID == tenantId2) {
            if (log.isDebugEnabled()) {
                log.debug(String.format(UserAccountAssociationConstants.ErrorMessages.DEBUG_INVALID_TENANT_DOMAIN
                        .getDescription(), MultitenantUtils.getTenantDomain(userName2)));
            }
            throw new UserAccountAssociationClientException(UserAccountAssociationConstants.ErrorMessages
                    .INVALID_TENANT_DOMAIN.toString());
        }
        boolean authentic1;
        try {
            userRealm = realmService.getTenantUserRealm(tenantId1);
            authentic1 = userRealm.getUserStoreManager().authenticate(tenantAwareUsername1, String.valueOf(password1));
        } catch (UserStoreException e) {
            throw new UserAccountAssociationClientException(UserAccountAssociationConstants.ErrorMessages
                    .ERROR_WHILE_AUTHENTICATING_USER
                    .getDescription(), e);
        }
        boolean authentic2;
        try {
            userRealm = realmService.getTenantUserRealm(tenantId2);
            authentic2 = userRealm.getUserStoreManager().authenticate(tenantAwareUsername2, String.valueOf(password2));
        } catch (UserStoreException e) {
            throw new UserAccountAssociationClientException(UserAccountAssociationConstants.ErrorMessages
                    .ERROR_WHILE_AUTHENTICATING_USER
                    .getDescription(), e);
        }

        if (!authentic1 || !authentic2) {
            if (log.isDebugEnabled()) {
                log.debug(UserAccountAssociationConstants.ErrorMessages.USER_NOT_AUTHENTIC.getDescription());

            }
            throw new UserAccountAssociationClientException(UserAccountAssociationConstants.ErrorMessages
                    .USER_NOT_AUTHENTIC.toString());
        }
        try {
            UserAccountConnectorImpl.getInstance().createUserAccountAssociation(userName1, userName2);
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
     * @param userName Username of account to delete associations.
     * @throws org.wso2.carbon.identity.user.account.association.exception.UserAccountAssociationClientException
     */
    public void deleteUserAccountAssociation(String userName) throws UserAccountAssociationClientException {

        String tenantAwareUsername = MultitenantUtils.getTenantAwareUsername(userName);
        int tenantId;
        RealmService realmService;

        try {
            realmService = IdentityAccountAssociationServiceComponent.getRealmService();
            tenantId = realmService.getTenantManager().getTenantId(MultitenantUtils.getTenantDomain(userName));
        } catch (UserStoreException e) {
            throw new UserAccountAssociationClientException(UserAccountAssociationConstants.ErrorMessages
                    .ERROR_WHILE_GETTING_TENANT_ID
                    .getDescription(), e);
        } catch (Exception e) {
            throw new UserAccountAssociationClientException(UserAccountAssociationConstants.ErrorMessages
                    .ERROR_WHILE_LOADING_REALM_SERVICE
                    .getDescription(), e);
        }
        String domainName = IdentityUtil.extractDomainFromName(tenantAwareUsername);
        tenantAwareUsername = UserAccountAssociationUtil.getUsernameWithoutDomain(tenantAwareUsername);
        try {
            if (!UserAccountAssociationDAO.getInstance().isValidUserAssociation(domainName, tenantId,
                    tenantAwareUsername)) {
                if (log.isDebugEnabled()) {
                    log.debug(UserAccountAssociationConstants.ErrorMessages.INVALID_ASSOCIATION.getDescription());

                }
                throw new UserAccountAssociationClientException(UserAccountAssociationConstants
                        .ErrorMessages.INVALID_ASSOCIATION.toString());
            }
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
     * Delete account association of logged in user
     *
     * @throws org.wso2.carbon.identity.user.account.association.exception.UserAccountAssociationClientException
     */
    public void deleteMyAssociations() throws UserAccountAssociationClientException {

        String userName = UserCoreUtil.addTenantDomainToEntry(CarbonContext.getThreadLocalCarbonContext()
                .getUsername(), CarbonContext.getThreadLocalCarbonContext().getTenantDomain());
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
     * Delete an existing user account association as admin
     *
     * @param userName Username of account to delete associations of.
     * @throws org.wso2.carbon.identity.user.account.association.exception.UserAccountAssociationClientException
     */
    public void deleteAssociationsOfUser(String userName) throws UserAccountAssociationClientException {

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
     * Delete an existing user account association of user without login
     *
     * @param initiateUser As whom to delete associations of a user
     * @param password     Password to authenticate
     * @param userName     Username of account to delete associations of
     * @throws org.wso2.carbon.identity.user.account.association.exception.UserAccountAssociationClientException
     */
    public void deleteMyAssociationsWithoutLogin(String initiateUser, char[] password, String userName) throws
            UserAccountAssociationClientException {

        String tenantAwareUsername = MultitenantUtils.getTenantAwareUsername(userName);
        String initiator = MultitenantUtils.getTenantAwareUsername(initiateUser);
        int tenantId;
        int tenantIdOfInitiateUser;
        RealmService realmService;

        try {
            realmService = IdentityAccountAssociationServiceComponent.getRealmService();
            tenantId = realmService.getTenantManager().getTenantId(MultitenantUtils.getTenantDomain(userName));
            tenantIdOfInitiateUser = realmService.getTenantManager().getTenantId(MultitenantUtils.getTenantDomain
                    (initiateUser));
        } catch (UserStoreException e) {
            throw new UserAccountAssociationClientException(UserAccountAssociationConstants.ErrorMessages
                    .ERROR_WHILE_GETTING_TENANT_ID
                    .getDescription(), e);
        } catch (Exception e) {
            throw new UserAccountAssociationClientException(UserAccountAssociationConstants.ErrorMessages
                    .ERROR_WHILE_LOADING_REALM_SERVICE
                    .getDescription(), e);
        }
        String domainName = IdentityUtil.extractDomainFromName(tenantAwareUsername);
        tenantAwareUsername = UserAccountAssociationUtil.getUsernameWithoutDomain(tenantAwareUsername);
        String domainNameOfInitiator = IdentityUtil.extractDomainFromName(initiator);
        initiator = UserAccountAssociationUtil.getUsernameWithoutDomain(initiator);
        boolean authentic;
        org.wso2.carbon.user.api.UserRealm userRealm;
        try {
            userRealm = realmService.getTenantUserRealm(tenantIdOfInitiateUser);
            authentic = userRealm.getUserStoreManager().authenticate(initiator, String.valueOf(password));
        } catch (UserStoreException e) {
            throw new UserAccountAssociationClientException(UserAccountAssociationConstants.ErrorMessages
                    .ERROR_WHILE_AUTHENTICATING_USER
                    .getDescription(), e);
        }
        if (!authentic) {
            if (log.isDebugEnabled()) {
                log.debug(UserAccountAssociationConstants.ErrorMessages.USER_NOT_AUTHENTIC.getDescription());

            }
            throw new UserAccountAssociationClientException(UserAccountAssociationConstants.ErrorMessages
                    .USER_NOT_AUTHENTIC.toString());
        }
        try {
            if (!UserAccountAssociationDAO.getInstance().isValidUserAssociation(domainNameOfInitiator,
                    tenantIdOfInitiateUser, initiator, domainName, tenantId, tenantAwareUsername) && initiateUser !=
                    userName) {
                if (log.isDebugEnabled()) {
                    log.debug(UserAccountAssociationConstants.ErrorMessages.INVALID_ASSOCIATION.getDescription());

                }
                throw new UserAccountAssociationClientException(UserAccountAssociationConstants
                        .ErrorMessages.INVALID_ASSOCIATION.toString());
            }
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
        String loggedInUser = UserCoreUtil.addTenantDomainToEntry(CarbonContext.getThreadLocalCarbonContext()
                .getUsername(), CarbonContext.getThreadLocalCarbonContext().getTenantDomain());
        try {
            return UserAccountConnectorImpl.getInstance().getAccountAssociationsOfUser(loggedInUser);
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
    public UserAccountAssociationDTO[] getAccountAssociations(String username) throws
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
     * @param password password
     * @return
     * @throws UserAccountAssociationClientException
     */
    public UserAccountAssociationDTO[] getMyAccountAssociationsWithoutLogin(String userName, char[] password) throws
            UserAccountAssociationClientException {
        try {

            String tenantDomain = MultitenantUtils.getTenantDomain(userName);
            UserRealmService realmService = IdentityAccountAssociationServiceComponent.getRealmService();
            int tenantId = realmService.getTenantManager().getTenantId(tenantDomain);
            org.wso2.carbon.user.api.UserRealm userRealm = realmService.getTenantUserRealm(tenantId);
            String tenantAwareUsername = MultitenantUtils.getTenantAwareUsername(userName);
            boolean isAuthenticated = userRealm.getUserStoreManager().authenticate(tenantAwareUsername, String
                    .valueOf(password));
            if (!isAuthenticated) {
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
     * @param userName Username of the account to switch to.
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
