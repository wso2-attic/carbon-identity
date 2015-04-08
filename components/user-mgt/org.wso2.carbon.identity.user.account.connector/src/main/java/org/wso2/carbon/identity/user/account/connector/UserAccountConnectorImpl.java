/*
*  Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.user.account.connector;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.core.services.authentication.AuthenticationUtil;
import org.wso2.carbon.core.services.authentication.stats.LoginAttempt;
import org.wso2.carbon.core.services.authentication.stats.LoginStatDatabase;
import org.wso2.carbon.core.services.util.CarbonAuthenticationUtil;
import org.wso2.carbon.identity.user.account.connector.dao.ConnectorDAO;
import org.wso2.carbon.identity.user.account.connector.exception.UserAccountConnectorException;
import org.wso2.carbon.identity.user.account.connector.internal.IdentityAccountConnectorServiceComponent;
import org.wso2.carbon.identity.user.account.connector.util.UserAccountConnectorConstants;
import org.wso2.carbon.identity.user.account.connector.util.UserAccountConnectorUtil;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.util.UserCoreUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class UserAccountConnectorImpl implements UserAccountConnector {

    private static Log log = LogFactory.getLog(UserAccountConnectorImpl.class);
    private static Log audit = CarbonConstants.AUDIT_LOG;
    private static UserAccountConnectorImpl instance;

    private UserAccountConnectorImpl() {

    }

    public static UserAccountConnector getInstance() {
        if (instance == null) {
            synchronized (UserAccountConnectorImpl.class) {
                if (instance == null) {
                    instance = new UserAccountConnectorImpl();
                }
            }
        }
        return instance;
    }

    /**
     * Create new user account association
     *
     * @param userName
     * @param password
     * @throws UserAccountConnectorException
     */
    @Override
    public void connectUserAccount(String userName, String password) throws UserAccountConnectorException {

        if (!StringUtils.isBlank(userName) && !StringUtils.isBlank(password)) {

            boolean authentic = false;
            final String loggedInUser = CarbonContext.getThreadLocalCarbonContext().getUsername();
            final int loggedInTenant = CarbonContext.getThreadLocalCarbonContext().getTenantId();
            String tenantAwareUsername = MultitenantUtils.getTenantAwareUsername(userName);
            int tenantId = -1;

            try {
                RealmService realmService = IdentityAccountConnectorServiceComponent.getRealmService();
                tenantId = getTenantId(realmService, MultitenantUtils.getTenantDomain(userName));

                if (tenantId != -1) {
                    UserRealm userRealm = realmService.getTenantUserRealm(tenantId);
                    authentic = userRealm.getUserStoreManager().authenticate(tenantAwareUsername, password);
                    int index = tenantAwareUsername.indexOf(CarbonConstants.DOMAIN_SEPARATOR);
                    if (index < 0) {
                        String domain = UserCoreUtil.getDomainFromThreadLocal();
                        if (domain != null) {
                            tenantAwareUsername = domain + CarbonConstants.DOMAIN_SEPARATOR + tenantAwareUsername;
                        }
                    }

                } else {
                    if (log.isDebugEnabled()) {
                        log.debug(UserAccountConnectorConstants.ErrorMessages.INVALID_TENANT_DOMAIN.getDescription());
                    }
                    throw new UserAccountConnectorException(UserAccountConnectorConstants.ErrorMessages
                                                                    .INVALID_TENANT_DOMAIN.toString());
                }
            } catch (Exception e) {
                log.error(UserAccountConnectorConstants.ErrorMessages.ACCOUNT_AUTHENTICATE_ERROR.getDescription(), e);
                throw new UserAccountConnectorException(UserAccountConnectorConstants.ErrorMessages
                                                                .ACCOUNT_AUTHENTICATE_ERROR.toString());
            }

            if (authentic) {

                boolean hasAssociation = false;

                try {
                    hasAssociation = ConnectorDAO.getInstance().isValidAssociation(tenantAwareUsername, tenantId);
                    if(!hasAssociation && tenantAwareUsername.equals(CarbonContext.getThreadLocalCarbonContext()
                                                          .getUsername()) && (tenantId == CarbonContext
                            .getThreadLocalCarbonContext().getTenantId())) {
                        throw new UserAccountConnectorException(UserAccountConnectorConstants.ErrorMessages
                                                                        .SAME_ACCOUNT_CONNECTING_ERROR.toString());
                    }
                } catch (Exception e) {
                    log.error(UserAccountConnectorConstants.ErrorMessages.ACCOUNT_CONNECTING_ERROR.getDescription(), e);
                    throw new UserAccountConnectorException(UserAccountConnectorConstants.ErrorMessages
                                                                    .ACCOUNT_CONNECTING_ERROR.toString());
                }

                if (hasAssociation) {
                    if (log.isDebugEnabled()) {
                        log.debug(UserAccountConnectorConstants.ErrorMessages.ALREADY_CONNECTED.getDescription());
                    }
                    throw new UserAccountConnectorException(UserAccountConnectorConstants.ErrorMessages
                                                                    .ALREADY_CONNECTED.toString());
                }

                try {
                    // Get association key if logged in user has one
                    String associationKey = ConnectorDAO.getInstance().getAssociationKeyOfUser(loggedInUser,
                                                                                               loggedInTenant);
                    boolean validAssociationKey = associationKey != null;

                    // If connecting account already connected to other accounts
                    String connUserAssociationKey = ConnectorDAO.getInstance().getAssociationKeyOfUser
                            (tenantAwareUsername, tenantId);
                    boolean validConnUserAssociationKey = connUserAssociationKey != null;

                    if (!validAssociationKey && !validConnUserAssociationKey) {
                        String newAssociationKey = UserAccountConnectorUtil.getRandomNumber();
                        ConnectorDAO.getInstance().createConnection(newAssociationKey, loggedInUser,
                                                                    loggedInTenant);
                        ConnectorDAO.getInstance().createConnection(newAssociationKey, tenantAwareUsername,
                                                                    tenantId);

                    } else if (validAssociationKey && !validConnUserAssociationKey) {
                        ConnectorDAO.getInstance().createConnection(associationKey, tenantAwareUsername, tenantId);

                    } else if (!validAssociationKey && validConnUserAssociationKey) {
                        ConnectorDAO.getInstance().createConnection(connUserAssociationKey, loggedInUser,
                                                                    loggedInTenant);

                    } else {
                        ConnectorDAO.getInstance().updateAssociationKey(connUserAssociationKey, associationKey);

                    }
                } /** catch (SQLException e) {
                    log.error(UserAccountConnectorConstants.ErrorMessages.CONN_CREATE_DB_ERROR.getDescription(), e);
                    throw new UserAccountConnectorException(UserAccountConnectorConstants.ErrorMessages
                                                                    .CONN_CREATE_DB_ERROR.toString());
                } catch (IdentityApplicationManagementException e) {
                    log.error(UserAccountConnectorConstants.ErrorMessages.DB_CONN_ERROR.getDescription(), e);
                    throw new UserAccountConnectorException(UserAccountConnectorConstants.ErrorMessages.DB_CONN_ERROR
                                                                    .toString());
                } */catch (Exception e) {
                    log.error(UserAccountConnectorConstants.ErrorMessages.ACCOUNT_CONNECTING_ERROR.getDescription(), e);
                    throw new UserAccountConnectorException(UserAccountConnectorConstants.ErrorMessages
                                                                    .ACCOUNT_CONNECTING_ERROR.toString());
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug(UserAccountConnectorConstants.ErrorMessages.USER_NOT_AUTHENTIC.getDescription());

                }
                throw new UserAccountConnectorException(UserAccountConnectorConstants.ErrorMessages
                                                                .USER_NOT_AUTHENTIC.toString());
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug(UserAccountConnectorConstants.ErrorMessages.INVALID_INPUTS.getDescription());

            }
            throw new UserAccountConnectorException(UserAccountConnectorConstants.ErrorMessages.INVALID_INPUTS
                                                            .toString());
        }
    }

    /**
     * Delete an existing user account association
     *
     * @param userName
     * @throws UserAccountConnectorException
     */
    @Override
    public void deleteUserAccountConnection(String userName) throws UserAccountConnectorException {

        if (!StringUtils.isBlank(userName)) {

            String tenantAwareUsername = MultitenantUtils.getTenantAwareUsername(userName);
            int tenantId = -1;
            try {
                tenantId = getTenantId(IdentityAccountConnectorServiceComponent
                                               .getRealmService(), MultitenantUtils.getTenantDomain(userName));

            } catch (Exception e) {
                log.error(UserAccountConnectorConstants.ErrorMessages.ERROR_IN_GET_TENANT_ID.getDescription(), e);
                throw new UserAccountConnectorException(UserAccountConnectorConstants.ErrorMessages
                                                                .ERROR_IN_GET_TENANT_ID.toString());
            }

            boolean hasAssociation = false;

            try {
                hasAssociation = ConnectorDAO.getInstance().isValidAssociation(tenantAwareUsername, tenantId);
            } catch (Exception e) {
                log.error(UserAccountConnectorConstants.ErrorMessages.CONN_DELETE_ERROR.getDescription(), e);
                throw new UserAccountConnectorException(UserAccountConnectorConstants.ErrorMessages.CONN_DELETE_ERROR
                                                                .toString());
            }

            if (!hasAssociation) {
                if (log.isDebugEnabled()) {
                    log.debug(UserAccountConnectorConstants.ErrorMessages.INVALID_ASSOCIATION.getDescription());

                }
                throw new UserAccountConnectorException(UserAccountConnectorConstants
                                                                .ErrorMessages.INVALID_ASSOCIATION.toString());
            }

            try {
                ConnectorDAO.getInstance().deleteAccountConnection(tenantAwareUsername, tenantId);

            } /**catch (SQLException e) {
                log.error(UserAccountConnectorConstants.ErrorMessages.CONN_DELETE_DB_ERROR.getDescription(), e);
                throw new UserAccountConnectorException(UserAccountConnectorConstants.ErrorMessages
                                                                .CONN_DELETE_DB_ERROR.toString());
            } catch (IdentityApplicationManagementException e) {
                log.error(UserAccountConnectorConstants.ErrorMessages.DB_CONN_ERROR.getDescription(), e);
                throw new UserAccountConnectorException(UserAccountConnectorConstants.ErrorMessages.DB_CONN_ERROR
                                                                .toString());
            } */catch (Exception e) {
                log.error(UserAccountConnectorConstants.ErrorMessages.CONN_DELETE_ERROR.getDescription(), e);
                throw new UserAccountConnectorException(UserAccountConnectorConstants.ErrorMessages.CONN_DELETE_ERROR
                                                                .toString());
            }

        } else {
            if (log.isDebugEnabled()) {
                log.debug(UserAccountConnectorConstants.ErrorMessages.INVALID_INPUTS.getDescription());

            }
            throw new UserAccountConnectorException(UserAccountConnectorConstants
                                                            .ErrorMessages.INVALID_INPUTS.toString());
        }
    }

    /**
     * Get all associated accounts of the logged in user
     *
     * @return
     * @throws UserAccountConnectorException
     */
    @Override
    public String[] getConnectedAccountsOfUser() throws UserAccountConnectorException {

        List<String> connections = null;
        try {
            connections = ConnectorDAO.getInstance().getConnectionsOfUser(CarbonContext.getThreadLocalCarbonContext()
                                                                                  .getUsername(),
                                                                          CarbonContext.getThreadLocalCarbonContext()
                                                                                  .getTenantId());

        } /**catch (SQLException e) {
            log.error(UserAccountConnectorConstants.ErrorMessages.CONN_LIST_DB_ERROR.getDescription(), e);
            throw new UserAccountConnectorException(UserAccountConnectorConstants
                                                            .ErrorMessages.CONN_LIST_DB_ERROR.toString());
        } catch (UserStoreException e) {
            log.error(UserAccountConnectorConstants.ErrorMessages.CONN_LIST_USER_STORE_ERROR.getDescription(), e);
            throw new UserAccountConnectorException(UserAccountConnectorConstants
                                                            .ErrorMessages.CONN_LIST_USER_STORE_ERROR.toString());
        } catch (IdentityApplicationManagementException e) {
            log.error(UserAccountConnectorConstants.ErrorMessages.DB_CONN_ERROR.getDescription(), e);
            throw new UserAccountConnectorException(UserAccountConnectorConstants.ErrorMessages.DB_CONN_ERROR
                                                            .toString());
        }*/ catch (Exception e) {
            log.error(UserAccountConnectorConstants.ErrorMessages.CONN_LIST_ERROR.getDescription(), e);
            throw new UserAccountConnectorException(UserAccountConnectorConstants
                                                            .ErrorMessages.CONN_LIST_ERROR.toString(), e);
        }

        if (connections.size() > 0) {
            return connections.toArray(new String[connections.size()]);
        }
        return new String[0];
    }

    /**
     * Switch logged in user account to the required associated user account
     *
     * @param userName
     * @return
     * @throws UserAccountConnectorException
     */
    @Override
    public boolean switchLoggedInUser(String userName) throws UserAccountConnectorException {

        if (!StringUtils.isBlank(userName)) {

            String tenantAwareUsername = MultitenantUtils.getTenantAwareUsername(userName);
            String tenantDomain = MultitenantUtils.getTenantDomain(userName);
            RealmService realmService = null;
            int tenantId = -1;

            try {
                realmService = IdentityAccountConnectorServiceComponent.getRealmService();
                tenantId = getTenantId(realmService, tenantDomain);
            } catch (Exception e) {
                log.error(UserAccountConnectorConstants.ErrorMessages.ERROR_IN_GET_TENANT_ID.getDescription(), e);
                throw new UserAccountConnectorException(UserAccountConnectorConstants.ErrorMessages
                                                                .ERROR_IN_GET_TENANT_ID.toString());
            }

            boolean hasAssociation = false;

            try {
                hasAssociation = ConnectorDAO.getInstance().isValidAssociation(tenantAwareUsername, tenantId);
            } catch (Exception e) {
                log.error(UserAccountConnectorConstants.ErrorMessages.ACCOUNT_SWITCHING_ERROR.getDescription(),
                          e);
                throw new UserAccountConnectorException(UserAccountConnectorConstants.ErrorMessages
                                                                .ACCOUNT_SWITCHING_ERROR.toString());
            }

            if (!hasAssociation) {
                if (log.isDebugEnabled()) {
                    log.debug(UserAccountConnectorConstants.ErrorMessages.INVALID_ASSOCIATION.getDescription());
                }
                throw new UserAccountConnectorException(UserAccountConnectorConstants.ErrorMessages
                                                                .INVALID_ASSOCIATION.toString());
            }

            try {

                MessageContext msgCtx = MessageContext.getCurrentMessageContext();
                HttpServletRequest request = (HttpServletRequest) msgCtx.getProperty(HTTPConstants
                                                                                             .MC_HTTP_SERVLETREQUEST);
                HttpSession httpSession = request.getSession();
                String remoteAddress = AuthenticationUtil.getRemoteAddress(msgCtx);

                UserRealm userRealm = realmService.getTenantUserRealm(tenantId);

                if (userRealm != null) {

                    if (!realmService.getTenantManager().isTenantActive(tenantId)) {
                        log.warn("Tenant has been deactivated. TenantID : " + tenantId);
                        return false;
                    }

                    int index = tenantAwareUsername.indexOf(CarbonConstants.DOMAIN_SEPARATOR);
                    if (index < 0) {
                        String domain = UserCoreUtil.getDomainFromThreadLocal();
                        if (domain != null) {
                            tenantAwareUsername = domain + CarbonConstants.DOMAIN_SEPARATOR + tenantAwareUsername;
                        }
                    }

                    // Only pre and post authentication listeners will get executed,
                    // as user is already authenticated during the account association creation phase
                    boolean isAuthenticated = UserAccountConnectorUtil.executePrePostAuthenticationListeners
                            (tenantAwareUsername, (org.wso2.carbon.user.core.UserStoreManager) userRealm
                                    .getUserStoreManager());

                    boolean isAuthorized = userRealm.getAuthorizationManager().isUserAuthorized
                            (tenantAwareUsername, "/permission/admin/login", CarbonConstants.UI_PERMISSION_ACTION);

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
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug(UserAccountConnectorConstants.ErrorMessages.INVALID_ASSOCIATION.getDescription());
                    }
                    throw new UserAccountConnectorException(UserAccountConnectorConstants.ErrorMessages
                                                                    .INVALID_ASSOCIATION.toString());
                }
            } /**catch (SQLException e) {
                log.error(UserAccountConnectorConstants.ErrorMessages.CONN_SWITCH_DB_ERROR.getDescription(), e);
                throw new UserAccountConnectorException(UserAccountConnectorConstants.ErrorMessages
                                                                .CONN_SWITCH_DB_ERROR.toString());
            } catch (IdentityApplicationManagementException e) {
                log.error(UserAccountConnectorConstants.ErrorMessages.DB_CONN_ERROR.getDescription(), e);
                throw new UserAccountConnectorException(UserAccountConnectorConstants.ErrorMessages.DB_CONN_ERROR
                                                                .toString());
            } catch (AuthenticationException e) {
                log.error(UserAccountConnectorConstants.ErrorMessages.ERROR_RETRIEVE_REMOTE_ADDRESS.getDescription(),
                          e);
                throw new UserAccountConnectorException(UserAccountConnectorConstants.ErrorMessages
                                                                .ERROR_RETRIEVE_REMOTE_ADDRESS.toString());
            } */catch (Exception e) {
                log.error(UserAccountConnectorConstants.ErrorMessages.ACCOUNT_SWITCHING_ERROR.getDescription(),
                          e);
                throw new UserAccountConnectorException(UserAccountConnectorConstants.ErrorMessages
                                                                .ACCOUNT_SWITCHING_ERROR.toString());
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug(UserAccountConnectorConstants.ErrorMessages.INVALID_INPUTS.getDescription());
            }
            throw new UserAccountConnectorException(UserAccountConnectorConstants.ErrorMessages.INVALID_INPUTS
                                                            .toString());
        }

        return false;
    }

    /**
     * Get tenant id for given tenant domain
     *
     * @param realmService
     * @param tenantDomain
     * @return
     * @throws Exception
     */
    private int getTenantId(RealmService realmService, String tenantDomain) throws Exception {
        int tenantId = -1;

        try {
            tenantId = realmService.getTenantManager().getTenantId(tenantDomain);
        } catch (UserStoreException e) {
            throw e;
        }

        return tenantId;
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
