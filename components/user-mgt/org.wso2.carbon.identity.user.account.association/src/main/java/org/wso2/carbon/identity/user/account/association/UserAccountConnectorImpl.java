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

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.core.common.AuthenticationException;
import org.wso2.carbon.core.services.authentication.AuthenticationUtil;
import org.wso2.carbon.core.services.authentication.stats.LoginAttempt;
import org.wso2.carbon.core.services.authentication.stats.LoginStatDatabase;
import org.wso2.carbon.core.services.util.CarbonAuthenticationUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.user.account.association.dao.UserAccountAssociationDAO;
import org.wso2.carbon.identity.user.account.association.dto.UserAccountAssociationDTO;
import org.wso2.carbon.identity.user.account.association.exception.UserAccountAssociationClientException;
import org.wso2.carbon.identity.user.account.association.exception.UserAccountAssociationException;
import org.wso2.carbon.identity.user.account.association.exception.UserAccountAssociationServerException;
import org.wso2.carbon.identity.user.account.association.internal.IdentityAccountAssociationServiceComponent;
import org.wso2.carbon.identity.user.account.association.util.UserAccountAssociationConstants;
import org.wso2.carbon.identity.user.account.association.util.UserAccountAssociationUtil;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.util.UserCoreUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class UserAccountConnectorImpl implements UserAccountConnector {

    private static final Log log = LogFactory.getLog(UserAccountConnectorImpl.class);
    private static final Log audit = CarbonConstants.AUDIT_LOG;

    private UserAccountConnectorImpl() {

    }

    private static class LazyHolder {
        private static final UserAccountConnectorImpl INSTANCE = new UserAccountConnectorImpl();
    }

    public static UserAccountConnectorImpl getInstance() {
        return LazyHolder.INSTANCE;
    }

    /**
     * Create new user account association
     *
     * @param userName1 Username of account 1
     * @param userName2 Username of account 2
     * @throws org.wso2.carbon.identity.user.account.association.exception.UserAccountAssociationException
     */
    @Override
    public void createUserAccountAssociation(String userName1, String userName2) throws
            UserAccountAssociationException {

        if (StringUtils.isNotBlank(userName1) && StringUtils.isNotBlank(userName2)) {
            RealmService realmService;
            String tenantAwareUsername1 = MultitenantUtils.getTenantAwareUsername(userName1);
            String tenantAwareUsername2 = MultitenantUtils.getTenantAwareUsername(userName2);

            String user1Domain = IdentityUtil.extractDomainFromName(tenantAwareUsername1);
            String user2Domain = IdentityUtil.extractDomainFromName(tenantAwareUsername2);
            String username1WithoutDomain = UserAccountAssociationUtil.getUsernameWithoutDomain(tenantAwareUsername1);
            String username2WithoutDomain = UserAccountAssociationUtil.getUsernameWithoutDomain(tenantAwareUsername2);
            int user1Tenant = MultitenantConstants.INVALID_TENANT_ID;
            int user2Tenant = MultitenantConstants.INVALID_TENANT_ID;
            try {
                realmService = IdentityAccountAssociationServiceComponent.getRealmService();
                user1Tenant = realmService.getTenantManager().getTenantId(MultitenantUtils.getTenantDomain(userName1));
                user2Tenant = realmService.getTenantManager().getTenantId(MultitenantUtils.getTenantDomain(userName2));

            } catch (UserStoreException e) {
                throw new UserAccountAssociationClientException("Error while retrieving tenant ID of user", e);
            }
            if (MultitenantConstants.INVALID_TENANT_ID == user1Tenant) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format(UserAccountAssociationConstants.ErrorMessages.DEBUG_INVALID_TENANT_DOMAIN
                            .getDescription(), MultitenantUtils.getTenantDomain(userName1)));
                }
                throw new UserAccountAssociationClientException(UserAccountAssociationConstants.ErrorMessages
                        .INVALID_TENANT_DOMAIN.toString());
            }
            if (MultitenantConstants.INVALID_TENANT_ID == user2Tenant) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format(UserAccountAssociationConstants.ErrorMessages.DEBUG_INVALID_TENANT_DOMAIN
                            .getDescription(), MultitenantUtils.getTenantDomain(userName2)));
                }
                throw new UserAccountAssociationClientException(UserAccountAssociationConstants.ErrorMessages
                        .INVALID_TENANT_DOMAIN.toString());
            }
            if (username1WithoutDomain.equals(username2WithoutDomain) && user1Domain.equals(user2Domain) &&
                    user1Tenant == user2Tenant) {
                if (log.isDebugEnabled()) {
                    log.debug(UserAccountAssociationConstants.ErrorMessages.SAME_ACCOUNT_CONNECTING_ERROR
                            .getDescription());

                }
                throw new UserAccountAssociationClientException(UserAccountAssociationConstants.ErrorMessages
                        .SAME_ACCOUNT_CONNECTING_ERROR.toString());
            }
            if (UserAccountAssociationDAO.getInstance().isValidUserAssociation(user1Domain, user1Tenant,
                    username1WithoutDomain, user2Domain, user2Tenant, username2WithoutDomain)) {
                if (log.isDebugEnabled()) {
                    log.debug(UserAccountAssociationConstants.ErrorMessages.ALREADY_CONNECTED.getDescription());
                }
                throw new UserAccountAssociationClientException(UserAccountAssociationConstants.ErrorMessages
                        .ALREADY_CONNECTED.toString());
            }
            String associationKey = UserAccountAssociationDAO.getInstance().getAssociationKeyOfUser(user1Domain,
                    user1Tenant, username1WithoutDomain);
            boolean validAssociationKey = associationKey != null;

            // If connecting account already connected to other accounts
            String connUserAssociationKey = UserAccountAssociationDAO.getInstance().getAssociationKeyOfUser
                    (user2Domain, user2Tenant, username2WithoutDomain);

            boolean validConnUserAssociationKey = connUserAssociationKey != null;

            if (!validAssociationKey && !validConnUserAssociationKey) {
                String newAssociationKey = UserAccountAssociationUtil.getRandomNumber();
                UserAccountAssociationDAO.getInstance().createUserAssociation(newAssociationKey, user1Domain,
                        user1Tenant, username1WithoutDomain);
                UserAccountAssociationDAO.getInstance().createUserAssociation(newAssociationKey, user2Domain,
                        user2Tenant, username2WithoutDomain);

            } else if (validAssociationKey && !validConnUserAssociationKey) {
                UserAccountAssociationDAO.getInstance().createUserAssociation(associationKey, user2Domain,
                        user2Tenant, username2WithoutDomain);

            } else if (!validAssociationKey && validConnUserAssociationKey) {
                UserAccountAssociationDAO.getInstance().createUserAssociation(connUserAssociationKey, user1Domain,
                        user1Tenant, username1WithoutDomain);

            } else {
                UserAccountAssociationDAO.getInstance().updateUserAssociationKey(connUserAssociationKey,
                        associationKey);
            }


        } else {
            if (log.isDebugEnabled()) {
                log.debug(UserAccountAssociationConstants.ErrorMessages.INVALID_INPUTS.getDescription());

            }
            throw new UserAccountAssociationClientException(UserAccountAssociationConstants.ErrorMessages.INVALID_INPUTS
                    .toString());
        }

    }

    /**
     * Delete an existing user account association
     *
     * @param userName Username of account to delete associations of.
     * @throws org.wso2.carbon.identity.user.account.association.exception.UserAccountAssociationException
     */
    @Override
    public void deleteUserAccountAssociation(String userName) throws UserAccountAssociationException {

        if (!StringUtils.isBlank(userName)) {

            String tenantAwareUsername = MultitenantUtils.getTenantAwareUsername(userName);
            int tenantId = MultitenantConstants.INVALID_TENANT_ID;
            RealmService realmService;

            try {
                realmService = IdentityAccountAssociationServiceComponent.getRealmService();
                tenantId = realmService.getTenantManager().getTenantId(MultitenantUtils.getTenantDomain(userName));
            } catch (UserStoreException e) {
                throw new UserAccountAssociationServerException(UserAccountAssociationConstants.ErrorMessages
                        .ERROR_WHILE_GETTING_TENANT_ID
                        .getDescription(), e);
            } catch (Exception e) {
                throw new UserAccountAssociationServerException(UserAccountAssociationConstants.ErrorMessages
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

            String domainName = IdentityUtil.extractDomainFromName(tenantAwareUsername);
            tenantAwareUsername = UserAccountAssociationUtil.getUsernameWithoutDomain(tenantAwareUsername);

            UserAccountAssociationDAO.getInstance().deleteUserAssociation(domainName, tenantId, tenantAwareUsername);

        } else {
            if (log.isDebugEnabled()) {
                log.debug(UserAccountAssociationConstants.ErrorMessages.INVALID_INPUTS.getDescription());

            }
            throw new UserAccountAssociationClientException(UserAccountAssociationConstants
                    .ErrorMessages.INVALID_INPUTS.toString());
        }
    }

    /**
     * Get all associated accounts of the logged in user
     *
     * @param userName Username to get account list of
     * @return
     * @throws UserAccountAssociationException
     */
    @Override
    public UserAccountAssociationDTO[] getAccountAssociationsOfUser(String userName) throws
            UserAccountAssociationException {

        String tenantAwareUserName = MultitenantUtils.getTenantAwareUsername(userName);
        String tenantDomain = MultitenantUtils.getTenantDomain(userName);
        RealmService realmService;
        int tenantId = MultitenantConstants.INVALID_TENANT_ID;

        try {
            realmService = IdentityAccountAssociationServiceComponent.getRealmService();
            tenantId = realmService.getTenantManager().getTenantId(tenantDomain);
        } catch (UserStoreException e) {
            throw new UserAccountAssociationServerException(UserAccountAssociationConstants.ErrorMessages
                    .ERROR_WHILE_GETTING_TENANT_ID
                    .getDescription(), e);
        } catch (Exception e) {
            throw new UserAccountAssociationServerException(UserAccountAssociationConstants.ErrorMessages
                    .ERROR_WHILE_LOADING_REALM_SERVICE
                    .getDescription(), e);
        }
        List<UserAccountAssociationDTO> userAccountAssociations = UserAccountAssociationDAO.getInstance()
                .getAssociationsOfUser(
                        IdentityUtil.extractDomainFromName(tenantAwareUserName),
                        tenantId, UserCoreUtil.removeDomainFromName(tenantAwareUserName));

        if (!userAccountAssociations.isEmpty()) {
            return userAccountAssociations.toArray(new UserAccountAssociationDTO[userAccountAssociations.size()]);
        }
        return new UserAccountAssociationDTO[0];
    }

    /**
     * Switch logged in user account to the required associated user account
     *
     * @param userName Username of associated account to switch
     * @return
     * @throws org.wso2.carbon.identity.user.account.association.exception.UserAccountAssociationException
     */
    @Override
    public boolean switchLoggedInUser(String userName) throws UserAccountAssociationException {

        if (!StringUtils.isBlank(userName)) {

            String tenantAwareUsername = MultitenantUtils.getTenantAwareUsername(userName);
            String tenantDomain = MultitenantUtils.getTenantDomain(userName);
            String domainName = IdentityUtil.extractDomainFromName(tenantAwareUsername);
            tenantAwareUsername = UserAccountAssociationUtil.getUsernameWithoutDomain(tenantAwareUsername);
            RealmService realmService;
            int tenantId = MultitenantConstants.INVALID_TENANT_ID;

            try {
                realmService = IdentityAccountAssociationServiceComponent.getRealmService();
                tenantId = realmService.getTenantManager().getTenantId(tenantDomain);
            } catch (UserStoreException e) {
                throw new UserAccountAssociationServerException(UserAccountAssociationConstants.ErrorMessages
                        .ERROR_WHILE_GETTING_TENANT_ID
                        .getDescription(), e);
            } catch (Exception e) {
                throw new UserAccountAssociationServerException(UserAccountAssociationConstants.ErrorMessages
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

            if (!UserAccountAssociationDAO.getInstance().isValidUserAssociation(domainName, tenantId,
                    tenantAwareUsername)) {
                if (log.isDebugEnabled()) {
                    log.debug(UserAccountAssociationConstants.ErrorMessages.INVALID_ASSOCIATION.getDescription());
                }
                throw new UserAccountAssociationClientException(UserAccountAssociationConstants.ErrorMessages
                        .INVALID_ASSOCIATION.toString());
            }

            try {

                if (!realmService.getTenantManager().isTenantActive(tenantId)) {
                    log.warn("Tenant has been deactivated. TenantID : " + tenantId);
                    return false;
                }

                MessageContext msgCtx = MessageContext.getCurrentMessageContext();
                HttpServletRequest request = (HttpServletRequest) msgCtx.getProperty(HTTPConstants
                        .MC_HTTP_SERVLETREQUEST);
                HttpSession httpSession = request.getSession();
                String remoteAddress = AuthenticationUtil.getRemoteAddress(msgCtx);
                UserRealm userRealm = realmService.getTenantUserRealm(tenantId);

                if (!UserAccountAssociationConstants.PRIMARY_USER_DOMAIN.equals(domainName)) {
                    tenantAwareUsername = domainName + CarbonConstants.DOMAIN_SEPARATOR + tenantAwareUsername;
                }

                // Only pre and post authentication listeners will get executed,
                // as user is already authenticated during the account association creation phase
                boolean isAuthenticated = UserAccountAssociationUtil.executePrePostAuthenticationListeners
                        (tenantAwareUsername, (org.wso2.carbon.user.core.UserStoreManager) userRealm
                                .getUserStoreManager());

                boolean isAuthorized = userRealm.getAuthorizationManager().isUserAuthorized
                        (tenantAwareUsername, UserAccountAssociationConstants.LOGIN_PERMISSION, CarbonConstants
                                .UI_PERMISSION_ACTION);

                if (isAuthenticated && isAuthorized) {
                    CarbonAuthenticationUtil.onSuccessAdminLogin(httpSession, tenantAwareUsername, tenantId,
                            tenantDomain, remoteAddress);
                    audit.info(getAuditMessage(true, CarbonContext.getThreadLocalCarbonContext().getUsername(),
                            CarbonContext.getThreadLocalCarbonContext().getTenantId(),
                            tenantAwareUsername, tenantId, tenantDomain));
                    return true;
                } else {
                    LoginAttempt loginAttempt =
                            new LoginAttempt(tenantAwareUsername, tenantId, remoteAddress, new Date(), false,
                                    "unauthorized");
                    LoginStatDatabase.recordLoginAttempt(loginAttempt);
                    audit.warn(getAuditMessage(false, CarbonContext.getThreadLocalCarbonContext().getUsername
                                    (), CarbonContext.getThreadLocalCarbonContext().getTenantId(),
                            tenantAwareUsername, tenantId, tenantDomain));
                }
            } catch (org.wso2.carbon.user.core.UserStoreException e) {
                throw new UserAccountAssociationServerException(UserAccountAssociationConstants.ErrorMessages
                        .ERROR_WHILE_UPDATING_SESSION
                        .getDescription(), e);
            } catch (UserStoreException e) {
                throw new UserAccountAssociationServerException(UserAccountAssociationConstants.ErrorMessages
                        .ERROR_WHILE_ACCESSING_REALM_SERVICE
                        .getDescription(), e);
            } catch (AuthenticationException e) {
                throw new UserAccountAssociationServerException(UserAccountAssociationConstants.ErrorMessages
                        .ERROR_WHILE_RETRIEVING_REMOTE_ADDRESS
                        .getDescription(), e);
            } catch (Exception e) {
                throw new UserAccountAssociationServerException(UserAccountAssociationConstants.ErrorMessages
                        .ERROR_WHILE_EXECUTING_AUTHENTICATORS
                        .getDescription(), e);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug(UserAccountAssociationConstants.ErrorMessages.INVALID_INPUTS.getDescription());
            }
            throw new UserAccountAssociationClientException(UserAccountAssociationConstants.ErrorMessages.INVALID_INPUTS
                    .toString());
        }

        return false;
    }

    /**
     * Get audit message for on success or fail of switching
     *
     * @param success
     * @param loggedInUser
     * @param loggedInTenant
     * @param userName
     * @param tenantId
     * @param tenantDomain
     * @return
     */
    private String getAuditMessage(boolean success, String loggedInUser, int loggedInTenant, String userName,
                                   int tenantId, String tenantDomain) {

        Date currentTime = Calendar.getInstance().getTime();
        SimpleDateFormat date = new SimpleDateFormat("'['yyyy-MM-dd HH:mm:ss,SSSZ']'");

        if (success) {
            return "\'" + loggedInUser + "\' [" + loggedInTenant + "] switched to \'" + userName
                    + "@" + tenantDomain + " [" + tenantId + "]\' successfully at " + date.format(currentTime);
        }
        return "Failed to switch from \'" + loggedInUser + "\' [" + loggedInTenant + "] to \'"
                + userName + "@" + tenantDomain + " [" + tenantId + "]\' at " + date.format(currentTime);
    }
}
