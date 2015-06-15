/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.mgt;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.model.IdentityErrorMsgContext;
import org.wso2.carbon.identity.core.persistence.JDBCPersistenceManager;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.mgt.beans.UserIdentityMgtBean;
import org.wso2.carbon.identity.mgt.beans.VerificationBean;
import org.wso2.carbon.identity.mgt.config.Config;
import org.wso2.carbon.identity.mgt.config.ConfigBuilder;
import org.wso2.carbon.identity.mgt.config.ConfigType;
import org.wso2.carbon.identity.mgt.config.StorageType;
import org.wso2.carbon.identity.mgt.constants.IdentityMgtConstants;
import org.wso2.carbon.identity.mgt.dto.NotificationDataDTO;
import org.wso2.carbon.identity.mgt.dto.UserIdentityClaimsDO;
import org.wso2.carbon.identity.mgt.dto.UserRecoveryDTO;
import org.wso2.carbon.identity.mgt.dto.UserRecoveryDataDO;
import org.wso2.carbon.identity.mgt.internal.IdentityMgtServiceComponent;
import org.wso2.carbon.identity.mgt.mail.Notification;
import org.wso2.carbon.identity.mgt.mail.NotificationBuilder;
import org.wso2.carbon.identity.mgt.mail.NotificationData;
import org.wso2.carbon.identity.mgt.policy.PolicyRegistry;
import org.wso2.carbon.identity.mgt.policy.PolicyViolationException;
import org.wso2.carbon.identity.mgt.store.UserIdentityDataStore;
import org.wso2.carbon.identity.mgt.util.UserIdentityManagementUtil;
import org.wso2.carbon.identity.mgt.util.Utils;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.common.AbstractUserOperationEventListener;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.jdbc.JDBCUserStoreManager;
import org.wso2.carbon.user.core.ldap.LDAPConnectionContext;
import org.wso2.carbon.user.core.ldap.LDAPConstants;
import org.wso2.carbon.user.core.ldap.ReadWriteLDAPUserStoreManager;
import org.wso2.carbon.user.core.util.JNDIUtil;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Properties;


import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.Connection;
import java.util.Date;
import java.util.Map.Entry;


/**
 * This is an implementation of UserOperationEventListener. This defines
 * additional operations
 * for some of the core user management operations
 */
public class IdentityMgtEventListener extends AbstractUserOperationEventListener {

    /*
     * The thread local variable to hold data with scope only to that variable.
     * This is to pass data from doPreX() method to doPostX() and to avoid
     * infinite loops.
     */

    private static ThreadLocal<Boolean> isRenameOperation = new ThreadLocal<Boolean>() {
        @Override
        protected synchronized Boolean initialValue() {
            return false;
        }
    };
    private static ThreadLocal<Boolean> isAddUserOperation = new ThreadLocal<Boolean>() {
        @Override
        protected synchronized Boolean initialValue() {
            return false;
        }
    };
    private static ThreadLocal<String> newUserName = new ThreadLocal<String>() {
        @Override
        protected synchronized String initialValue() {
            return "";
        }
    };
    public static final ThreadLocal<HashMap<String, Object>> threadLocalProperties = new ThreadLocal<HashMap<String, Object>>() {
        @Override
        protected HashMap<String, Object> initialValue() {
            return new HashMap<String, Object>();
        }
    };
    private static final Log log = LogFactory.getLog(IdentityMgtEventListener.class);
    private static final String EMPTY_PASSWORD_USED = "EmptyPasswordUsed";
    private static final String USER_IDENTITY_DO = "UserIdentityDO";
    private static final String EMAIL_NOTIFICATION_TYPE = "EMAIL";
    public static final String INSERT_STMT = "INSERT INTO IDN_UID_USER (IDN_UID, IDN_USERNAME, IDN_STORE_DOMAIN, " +
            "IDN_TENANT ) VALUES (?,?,?,?);";
    PolicyRegistry policyRegistry = null;
    private UserIdentityDataStore module;


    public IdentityMgtEventListener() {

        module = IdentityMgtConfig.getInstance().getIdentityDataStore();
        String adminUserName = IdentityMgtServiceComponent.getRealmService().getBootstrapRealmConfiguration()
                .getAdminUserName();
        try {
            IdentityMgtConfig config = IdentityMgtConfig.getInstance();

            // Get the policy registry with the loaded policies.
            policyRegistry = config.getPolicyRegistry();

            if (config.isListenerEnable()) {

                UserStoreManager userStoreMng = IdentityMgtServiceComponent.getRealmService()
                        .getBootstrapRealm().getUserStoreManager();
                if (!userStoreMng.isReadOnly()) {
                    Map<String,String> claimMap = new HashMap<String,String>();
                    claimMap.put(UserIdentityDataStore.ACCOUNT_LOCK, Boolean.toString(false));
                    userStoreMng.setUserClaimValues(adminUserName, claimMap, null);
                }
            }
        } catch (UserStoreException e) {
            log.error("Error while init identity listener", e);
        }
    }

    /**
     * What is this ?
     */
    @Override
    public int getExecutionOrderId() {
        return 1357;
    }

    /**
     * This method checks if the user account exist or is locked. If the account is
     * locked, the authentication process will be terminated after this method
     * returning false.
     */
    @Override
    public boolean doPreAuthenticate(String userName, Object credential,
                                     UserStoreManager userStoreManager) throws UserStoreException {

        if (log.isDebugEnabled()) {
            log.debug("Pre authenticator is called in IdentityMgtEventListener");
        }

        IdentityUtil.clearIdentityErrorMsg();

        IdentityMgtConfig config = IdentityMgtConfig.getInstance();
        //--- Logic checking if this user has been authenticated before and UID created --//
        try {
            Connection indbconn = JDBCPersistenceManager.getInstance().getDBConnection();

            String uidQuery = "SELECT IDN_UID FROM IDN_UID_USER WHERE IDN_USERNAME = ? AND IDN_STORE_DOMAIN=? AND " +
                    "IDN_TENANT=?";

            PreparedStatement uidStatement = indbconn.prepareStatement(uidQuery);

            uidStatement.setString(1, userName);
            uidStatement.setInt(2, getDomainID(userStoreManager));
            uidStatement.setInt(3, userStoreManager.getTenantId());

            String uid = null;

            ResultSet resultSet = uidStatement.executeQuery();

            while (resultSet.next()) {
                uid = resultSet.getString("IDN_UID");
            }

            if (StringUtils.isBlank(uid)) {
                createUIDForUser(userName, userStoreManager);
            }


        } catch (IdentityException | SQLException e) {
            throw new UserStoreException("Error while checking prior authentication", e);
        }

        //-- end of uid logic --//

        if (!config.isEnableAuthPolicy()) {
            return true;
        }

        String domainName = userStoreManager.getRealmConfiguration().getUserStoreProperty(UserCoreConstants
                .RealmConfig.PROPERTY_DOMAIN_NAME);
        String usernameWithDomain = UserCoreUtil.addDomainToName(userName, domainName);
        boolean isUserExistInCurrentDomain = userStoreManager.isExistingUser(usernameWithDomain);

        if (!isUserExistInCurrentDomain) {

            IdentityErrorMsgContext customErrorMessageContext = new IdentityErrorMsgContext(UserCoreConstants
                    .ErrorCode.USER_DOES_NOT_EXIST);
            IdentityUtil.setIdentityErrorMsg(customErrorMessageContext);

            if (log.isDebugEnabled()) {
                log.debug("Username :" + userName + "does not exists in the system, ErrorCode :" + UserCoreConstants
                        .ErrorCode.USER_DOES_NOT_EXIST);
            }
            if (config.isAuthPolicyAccountExistCheck()) {
                throw new UserStoreException(UserCoreConstants.ErrorCode.USER_DOES_NOT_EXIST);
            }
        } else {

            UserIdentityClaimsDO userIdentityDTO = module.load(userName, userStoreManager);

            // if the account is locked, should not be able to log in
            if (userIdentityDTO != null && userIdentityDTO.isAccountLocked()) {

                // If unlock time is specified then unlock the account.
                if ((userIdentityDTO.getUnlockTime() != 0) && (System.currentTimeMillis() >= userIdentityDTO
                        .getUnlockTime())) {

                    userIdentityDTO.setAccountLock(false);
                    userIdentityDTO.setUnlockTime(0);

                    try {
                        module.store(userIdentityDTO, userStoreManager);
                    } catch (IdentityException e) {
                        throw new UserStoreException(
                                "Error while saving user store data for user : "
                                        + userName, e);
                    }
                } else {
                    IdentityErrorMsgContext customErrorMessageContext = new IdentityErrorMsgContext(
                            UserCoreConstants.ErrorCode.USER_IS_LOCKED,
                            userIdentityDTO.getFailAttempts(),
                            config.getAuthPolicyMaxLoginAttempts());
                    IdentityUtil.setIdentityErrorMsg(customErrorMessageContext);
                    String errorMsg = "User account is locked for user : " + userName
                            + ". cannot login until the account is unlocked ";
                    log.warn(errorMsg);
                    throw new UserStoreException(UserCoreConstants.ErrorCode.USER_IS_LOCKED + " "
                            + errorMsg);
                }
            }
        }


        return true;
    }

    /**
     * This method locks the accounts after a configured number of
     * authentication failure attempts. And unlocks accounts based on successful
     * authentications.
     */
    @Override
    public boolean doPostAuthenticate(String userName, boolean authenticated,
                                      UserStoreManager userStoreManager) throws UserStoreException {

        if (log.isDebugEnabled()) {
            log.debug("Post authenticator is called in IdentityMgtEventListener");
        }

        IdentityMgtConfig config = IdentityMgtConfig.getInstance();

        if (!config.isEnableAuthPolicy()) {
            return true;
        }

        UserIdentityClaimsDO userIdentityDTO = module.load(userName, userStoreManager);
        if (userIdentityDTO == null) {
            userIdentityDTO = new UserIdentityClaimsDO(userName);
        }

        boolean userOTPEnabled = userIdentityDTO.getOneTimeLogin();

        // One time password check
        if (authenticated && config.isAuthPolicyOneTimePasswordCheck() &&
                (!userStoreManager.isReadOnly()) && userOTPEnabled ) {

            // reset password of the user and notify user of the new password

            String password = new String(UserIdentityManagementUtil.generateTemporaryPassword());
            userStoreManager.updateCredentialByAdmin(userName, password);

            // Get email user claim value
            String email = userStoreManager.getUserClaimValue(userName,UserCoreConstants.ClaimTypeURIs.EMAIL_ADDRESS,
                    null);

            if (StringUtils.isBlank(email)) {
                throw new UserStoreException("No user email provided for user : " + userName);
            }

            List<NotificationSendingModule> notificationModules =
                    config.getNotificationSendingModules();

            if (notificationModules != null) {

                NotificationDataDTO notificationData = new NotificationDataDTO();

                NotificationData emailNotificationData = new NotificationData();
                String emailTemplate = null;
                int tenantId = userStoreManager.getTenantId();
                String firstName = null;
                try {
                    firstName =
                            Utils.getClaimFromUserStoreManager(userName, tenantId,
                                    "http://wso2.org/claims/givenname");
                } catch (IdentityException e2) {
                    throw new UserStoreException("Could not load user given name", e2);
                }
                emailNotificationData.setTagData("first-name", firstName);
                emailNotificationData.setTagData("user-name", userName);
                emailNotificationData.setTagData("otp-password", password);

                emailNotificationData.setSendTo(email);

                Config emailConfig = null;
                ConfigBuilder configBuilder = ConfigBuilder.getInstance();
                try {
                    emailConfig =
                            configBuilder.loadConfiguration(ConfigType.EMAIL,
                                    StorageType.REGISTRY,
                                    tenantId);
                } catch (Exception e1) {
                    throw new UserStoreException(
                            "Could not load the email template configuration for user : "
                                    + userName, e1);
                }


                emailTemplate = emailConfig.getProperty("otp");

                Notification emailNotification = null;
                try {
                    emailNotification =
                            NotificationBuilder.createNotification(EMAIL_NOTIFICATION_TYPE, emailTemplate,
                                    emailNotificationData);
                } catch (Exception e) {
                    throw new UserStoreException(
                            "Could not create the email notification for template: "
                                    + emailTemplate, e);
                }
                NotificationSender sender = new NotificationSender();


                for (NotificationSendingModule notificationSendingModule : notificationModules) {

                    if (IdentityMgtConfig.getInstance().isNotificationInternallyManaged()) {
                        notificationSendingModule.setNotificationData(notificationData);
                        notificationSendingModule.setNotification(emailNotification);
                        sender.sendNotification(notificationSendingModule);
                        notificationData.setNotificationSent(true);
                    }
                }

            } else {
                throw new UserStoreException("No notification modules configured");
            }


        }

        // Password expire check. Not for OTP enabled users.
        if (authenticated && config.isAuthPolicyExpirePasswordCheck() && !userOTPEnabled && (!userStoreManager
                .isReadOnly())) {
            // TODO - password expire impl
            // Refactor adduser and change password api to stamp the time
            // Check user's expire time in the claim
            // if expired redirect to change password
            // else pass through

        }


        if (!authenticated && config.isAuthPolicyAccountLockOnFailure()) {
            // reading the max allowed #of failure attempts

            String domainName = userStoreManager.getRealmConfiguration().getUserStoreProperty(UserCoreConstants
                    .RealmConfig.PROPERTY_DOMAIN_NAME);
            String usernameWithDomain = UserCoreUtil.addDomainToName(userName, domainName);
            boolean isUserExistInCurrentDomain = userStoreManager.isExistingUser(usernameWithDomain);

            if (isUserExistInCurrentDomain) {
                userIdentityDTO.setFailAttempts();

                if (userIdentityDTO.getFailAttempts() >= config.getAuthPolicyMaxLoginAttempts()) {
                    log.info("User, " + userName + " has exceed the max failed login attempts. " +
                            "User account would be locked");
                    IdentityErrorMsgContext customErrorMessageContext = new IdentityErrorMsgContext
                            (UserCoreConstants.ErrorCode.USER_IS_LOCKED,
                                    userIdentityDTO.getFailAttempts(), config.getAuthPolicyMaxLoginAttempts());
                    IdentityUtil.setIdentityErrorMsg(customErrorMessageContext);

                    if (log.isDebugEnabled()) {
                        log.debug("Username :" + userName + "Exceeded the maximum login attempts. User locked, " +
                                "ErrorCode :" + UserCoreConstants.ErrorCode.USER_IS_LOCKED);
                    }

                    userIdentityDTO.setAccountLock(true);
                    userIdentityDTO.setFailAttempts(0);
                    // lock time from the config
                    int lockTime = IdentityMgtConfig.getInstance().getAuthPolicyLockingTime();
                    if (lockTime != 0) {
                        userIdentityDTO.setUnlockTime(System.currentTimeMillis() +
                                (lockTime * 60 * 1000L));
                    }
                } else {
                    IdentityErrorMsgContext customErrorMessageContext = new IdentityErrorMsgContext
                            (UserCoreConstants.ErrorCode.INVALID_CREDENTIAL,
                                    userIdentityDTO.getFailAttempts(), config.getAuthPolicyMaxLoginAttempts());
                    IdentityUtil.setIdentityErrorMsg(customErrorMessageContext);

                    if (log.isDebugEnabled()) {
                        log.debug("Username :" + userName + "Invalid Credential, ErrorCode :" + UserCoreConstants
                                .ErrorCode.INVALID_CREDENTIAL);
                    }

                }

                try {
                    module.store(userIdentityDTO, userStoreManager);
                } catch (IdentityException e) {
                    throw new UserStoreException("Error while saving user store data for user : "
                            + userName, e);
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("User, " + userName + " is not exists in " + domainName);
                }
            }

        } else {
            // if the account was locked due to account verification process,
            // the unlock the account and reset the number of failedAttempts
            if (userIdentityDTO.isAccountLocked() || userIdentityDTO.getFailAttempts() > 0 || userIdentityDTO
                    .getAccountLock()) {
                userIdentityDTO.setAccountLock(false);
                userIdentityDTO.setFailAttempts(0);
                userIdentityDTO.setUnlockTime(0);
                try {
                    module.store(userIdentityDTO, userStoreManager);
                } catch (IdentityException e) {
                    throw new UserStoreException("Error while saving user store data for user : "
                            + userName, e);
                }
            }
        }

        return true;
    }

    /**
     * This method will set the default/random password if the password provided is
     * null. The thread local parameter EMPTY_PASSWORD_USED will be used to
     * track if the password empty in the doPostAddUser.
     * This method will filter the security question URIs from claims and put those
     * to the thread local properties.
     */
    @Override
    public boolean doPreAddUser(String userName, Object credential, String[] roleList,
                                Map<String, String> claims, String profile,
                                UserStoreManager userStoreManager) throws UserStoreException {

        if (log.isDebugEnabled()) {
            log.debug("Pre add user is called in IdentityMgtEventListener");
        }
        IdentityMgtConfig config = IdentityMgtConfig.getInstance();
        if (!config.isListenerEnable()) {
            return true;
        }

        try {
            // Enforcing the password policies.
            if (credential != null &&
                    (credential instanceof StringBuffer && (credential.toString().trim().length() > 0))) {
                policyRegistry.enforcePasswordPolicies(credential.toString(), userName);
            }

        } catch (PolicyViolationException pe) {
            throw new UserStoreException(pe.getMessage(), pe);
        }


        // empty password account creation
        if (credential == null ||
                (credential instanceof StringBuffer && (credential.toString().trim().length() < 1))) {

            if (!config.isEnableTemporaryPassword()) {
                log.error("Empty passwords are not allowed");
                return false;
            }
            if (log.isDebugEnabled()) {
                log.debug("Credentials are null. Using a temporary password as credentials");
            }
            // setting the thread-local to check in doPostAddUser
            threadLocalProperties.get().put(EMPTY_PASSWORD_USED, true);
            // temporary passwords will be used
            char[] temporaryPassword = null;
            if (IdentityMgtConfig.getInstance().getTemporaryDefaultPassword() != null) {
                temporaryPassword = IdentityMgtConfig.getInstance().getTemporaryDefaultPassword()
                        .toCharArray();
            } else {
                temporaryPassword = UserIdentityManagementUtil.generateTemporaryPassword();
            }

            // setting the password value
            ((StringBuffer) credential).replace(0, temporaryPassword.length, new String(temporaryPassword));
        }

        // Filtering security question URIs from claims and add them to the thread local dto
        Map<String, String> userDataMap = new HashMap<String, String>();

        // TODO why challenge Q
        Iterator<Entry<String, String>> it = claims.entrySet().iterator();
        while (it.hasNext()) {

            Map.Entry<String, String> claim = it.next();

            if (claim.getKey().contains(UserCoreConstants.ClaimTypeURIs.CHALLENGE_QUESTION_URI) ||
                    claim.getKey().contains(UserCoreConstants.ClaimTypeURIs.IDENTITY_CLAIM_URI)) {
                userDataMap.put(claim.getKey(), claim.getValue());
                it.remove();
            }
        }

        UserIdentityClaimsDO identityDTO = new UserIdentityClaimsDO(userName, userDataMap);
        // adding dto to thread local to be read again from the doPostAddUser method
        threadLocalProperties.get().put(USER_IDENTITY_DO, identityDTO);

        isAddUserOperation.set(true);

        return true;

    }

    private String createUID(String userName) {

        UUID uuid = UUID.randomUUID();
        Date today = new Date();
        String uids = uuid.toString() + (today.getTime() % 310000);

        return uids;

    }

    /**
     * This method locks the created accounts based on the account policies or
     * based on the account confirmation method being used. Two account
     * confirmation methods are used : Temporary Password and Verification Code.
     * In the case of temporary password is used the temporary password will be
     * emailed to the user. In the case of verification code, the code will be
     * emailed to the user. The security questions filter ad doPreAddUser will
     * be persisted in this method.
     */
    @Override
    public boolean doPostAddUser(String userName, Object credential, String[] roleList,
                                 Map<String, String> claims, String profile,
                                 UserStoreManager userStoreManager) throws UserStoreException {
        if (log.isDebugEnabled()) {
            log.debug("Post add user is called in IdentityMgtEventListener");
        }
        IdentityMgtConfig config = IdentityMgtConfig.getInstance();
        if (!config.isListenerEnable()) {
            return true;
        }
        // reading the value from the thread local
        UserIdentityClaimsDO userIdentityClaimsDO = (UserIdentityClaimsDO) threadLocalProperties.get().get
                (USER_IDENTITY_DO);


        if (config.isEnableUserAccountVerification() && threadLocalProperties.get().containsKey(EMPTY_PASSWORD_USED)) {

            // empty password account creation
            String domainName = ((org.wso2.carbon.user.core.UserStoreManager) userStoreManager)
                    .getRealmConfiguration().getUserStoreProperty(
                            UserCoreConstants.RealmConfig.PROPERTY_DOMAIN_NAME);
            if (!UserCoreConstants.PRIMARY_DEFAULT_DOMAIN_NAME.equals(domainName)) {
                userName = domainName + UserCoreConstants.DOMAIN_SEPARATOR + userName;
            }

            // store identity data
            userIdentityClaimsDO.setAccountLock(false).setPasswordTimeStamp(System.currentTimeMillis());
            try {
                module.store(userIdentityClaimsDO, userStoreManager);
            } catch (IdentityException e) {
                throw new UserStoreException("Error while saving user store for user : "
                        + userName, e);
            }
            // store identity metadata
            UserRecoveryDataDO metadataDO = new UserRecoveryDataDO();
            metadataDO.setUserName(userName).setTenantId(userStoreManager.getTenantId())
                    .setCode((String) credential);

            // set recovery data
            RecoveryProcessor processor = new RecoveryProcessor();
            VerificationBean verificationBean;

            try {
                verificationBean = processor.updateConfirmationCode(1, userName, userStoreManager.getTenantId());
            } catch (IdentityException e) {
                throw new UserStoreException(
                        "Error while updating confirmation code for user : " + userName, e);
            }

            // preparing a bean to send the email
            UserIdentityMgtBean bean = new UserIdentityMgtBean();
            bean.setUserId(userName).setConfirmationCode(verificationBean.getKey())
                    .setRecoveryType(IdentityMgtConstants.Notification.TEMPORARY_PASSWORD)
                    .setEmail(claims.get(config.getAccountRecoveryClaim()));

            UserRecoveryDTO recoveryDto = new UserRecoveryDTO(userName);
            recoveryDto.setNotification(IdentityMgtConstants.Notification.ASK_PASSWORD);
            recoveryDto.setNotificationType("EMAIL");
            recoveryDto.setTenantId(userStoreManager.getTenantId());
            recoveryDto.setConfirmationCode(verificationBean.getKey());

            NotificationDataDTO notificationDto = null;

            try {
                notificationDto = processor.recoverWithNotification(recoveryDto);
            } catch (IdentityException e) {
                throw new UserStoreException("Error while sending notification for user : "
                        + userName, e);
            }

            return notificationDto != null && notificationDto.isNotificationSent();

        }
        // No account recoveries are defined, no email will be sent.
        if (config.isAuthPolicyAccountLockOnCreation()) {
            // accounts are locked. Admin should unlock
            userIdentityClaimsDO.setAccountLock(true);
            userIdentityClaimsDO.setPasswordTimeStamp(System.currentTimeMillis());
            try {
                config.getIdentityDataStore().store(userIdentityClaimsDO, userStoreManager);
            } catch (IdentityException e) {
                throw new UserStoreException("Error while saving user store data for user : "
                        + userName, e);
            }
        }


        // When claims available in user add request like http://wso2.org/claims/identity/accountLocked
        if (!config.isEnableUserAccountVerification() &&
                !config.isAuthPolicyAccountLockOnCreation() && userIdentityClaimsDO != null) {
            try {
                if (log.isDebugEnabled()) {
                    log.debug("Storing identity-mgt claims since they are available in the addUser request");
                }
                module.store(userIdentityClaimsDO, userStoreManager);
            } catch (IdentityException e) {
                throw new UserStoreException("Error while saving user store data for user : "
                        + userName, e);
            }
        }



        if (!isRenameOperation.get()) {
            createUIDForUser(userName, userStoreManager);
        }
        isAddUserOperation.set(false);
        return true;
    }


    private void createUIDForUser(String userName, UserStoreManager userStoreManager) throws UserStoreException {
        AbstractUserStoreManager aum = (AbstractUserStoreManager) userStoreManager;
        RealmConfiguration realmConfig = aum.getRealmConfiguration();
        String domainName = realmConfig.getUserStoreProperty(UserCoreConstants.RealmConfig.PROPERTY_DOMAIN_NAME);


        try {
            Connection dbCon = JDBCPersistenceManager.getInstance().getDBConnection();


            int domainID = getDomainID(userStoreManager);


            PreparedStatement insertToDB = dbCon.prepareStatement(INSERT_STMT);

            insertToDB.setString(1, createUID(userName));
            insertToDB.setString(2, userName);
            insertToDB.setInt(3, domainID);
            insertToDB.setInt(4, userStoreManager.getTenantId());

            insertToDB.execute();

            dbCon.commit();
            dbCon.close();


        } catch (IdentityException | SQLException | UserStoreException e) {
            throw new UserStoreException("Error while creating unique entry for user in embedded DB", e);
        }
    }

    /**
     * This method is used to check pre conditions when changing the user
     * password.
     */
    public boolean doPreUpdateCredential(String userName, Object newCredential,
                                         Object oldCredential, UserStoreManager userStoreManager) throws
            UserStoreException {

        if (log.isDebugEnabled()) {
            log.debug("Pre update credential is called in IdentityMgtEventListener");
        }

        IdentityMgtConfig config = IdentityMgtConfig.getInstance();
        if (!config.isListenerEnable()) {
            return true;
        }

        try {
            // Enforcing the password policies.
            if (newCredential != null
                    && (newCredential instanceof String && (newCredential.toString().trim()
                    .length() > 0))) {
                policyRegistry.enforcePasswordPolicies(newCredential.toString(), userName);

            }

        } catch (PolicyViolationException pe) {
            throw new UserStoreException(pe.getMessage(), pe);
        }

        return true;
    }

    /**
     * This method is used when the admin is updating the credentials with an
     * empty credential. A random password will be generated and will be mailed
     * to the user.
     */
    @Override
    public boolean doPreUpdateCredentialByAdmin(String userName, Object newCredential,
                                                UserStoreManager userStoreManager) throws UserStoreException {

        if (log.isDebugEnabled()) {
            log.debug("Pre update credential by admin is called in IdentityMgtEventListener");
        }
        IdentityMgtConfig config = IdentityMgtConfig.getInstance();
        if (!config.isListenerEnable()) {
            return true;
        }

        try {
            // Enforcing the password policies.
            if (newCredential != null
                    && (newCredential instanceof StringBuffer && (newCredential.toString().trim()
                    .length() > 0))) {
                policyRegistry.enforcePasswordPolicies(newCredential.toString(), userName);
            }

        } catch (PolicyViolationException pe) {
            throw new UserStoreException(pe.getMessage(), pe);
        }

        if (newCredential == null
                || (newCredential instanceof StringBuffer && ((StringBuffer) newCredential)
                .toString().trim().length() < 1)) {

            if (!config.isEnableTemporaryPassword()) {
                log.error("Empty passwords are not allowed");
                return false;
            }
            if (log.isDebugEnabled()) {
                log.debug("Credentials are null. Using a temporary password as credentials");
            }
            // temporary passwords will be used
            char[] temporaryPassword = UserIdentityManagementUtil.generateTemporaryPassword();
            // setting the password value
            ((StringBuffer) newCredential).replace(0, temporaryPassword.length, new String(
                    temporaryPassword));

            UserIdentityMgtBean bean = new UserIdentityMgtBean();
            bean.setUserId(userName);
            bean.setConfirmationCode(newCredential.toString());
            bean.setRecoveryType(IdentityMgtConstants.Notification.TEMPORARY_PASSWORD);
            if (log.isDebugEnabled()) {
                log.debug("Sending the temporary password to the user " + userName);
            }
            UserIdentityManagementUtil.notifyViaEmail(bean);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Updating credentials of user " + userName
                        + " by admin with a non-empty password");
            }
        }
        return true;
    }

    /**
     * This method checks if the updating claim is an user identity data or
     * security question. Identity data and security questions are updated by
     * the identity store, therefore they will not be added to the user store.
     * Other claims are skipped to the set or update.
     */
    @Override
    public boolean doPreSetUserClaimValue(String userName, String claimURI, String claimValue,
                                          String profileName, UserStoreManager userStoreManager)
            throws UserStoreException {
        IdentityMgtConfig config = IdentityMgtConfig.getInstance();
        if (!config.isListenerEnable()) {
            return true;
        }

        // security questions and identity claims are updated at the identity store
        if (claimURI.contains(UserCoreConstants.ClaimTypeURIs.CHALLENGE_QUESTION_URI) ||
                claimURI.contains(UserCoreConstants.ClaimTypeURIs.IDENTITY_CLAIM_URI)) {
//			  the whole listner to return and fail adding the cliam in doSetUserClaim
            return true;
        } else {
            // a simple user claim. add it to the user store
            return true;
        }
    }

    /**
     * As in the above method the user account lock claim, primary challenges
     * claim will be separately handled. Identity claims will be removed from
     * the claim set before adding claims to the user store.
     */
    @Override
    public boolean doPreSetUserClaimValues(String userName, Map<String, String> claims,
                                           String profileName, UserStoreManager userStoreManager)
            throws UserStoreException {
        IdentityMgtConfig config = IdentityMgtConfig.getInstance();
        if (!config.isListenerEnable()) {
            return true;
        }

        UserIdentityDataStore identityDataStore = IdentityMgtConfig.getInstance().getIdentityDataStore();
        UserIdentityClaimsDO identityDTO = identityDataStore.load(userName, userStoreManager);
        if (identityDTO == null) {
            identityDTO = new UserIdentityClaimsDO(userName);
        }


        Iterator<Entry<String, String>> it = claims.entrySet().iterator();
        while (it.hasNext()) {

            Map.Entry<String, String> claim = it.next();

            final String USER_NAME_CLAIM_URI = "http://wso2.org/claims/userName";

            if (claim.getKey().contains(USER_NAME_CLAIM_URI)) {
                if (!claim.getValue().equalsIgnoreCase(userName)) {
                    isRenameOperation.set(true);
                    newUserName.set(claim.getValue());
                }
                it.remove();
            }

            if (claim.getKey().contains(UserCoreConstants.ClaimTypeURIs.CHALLENGE_QUESTION_URI)
                    || claim.getKey().contains(UserCoreConstants.ClaimTypeURIs.IDENTITY_CLAIM_URI)) {
                identityDTO.setUserIdentityDataClaim(claim.getKey(), claim.getValue());
                it.remove();
            }
        }

        // storing the identity claims and security questions
        try {
            identityDataStore.store(identityDTO, userStoreManager);
        } catch (IdentityException e) {
            throw new UserStoreException(
                    "Error while saving user store data for user : " + userName, e);
        }

        if (userStoreManager instanceof ReadWriteLDAPUserStoreManager) {


        }

        return true;
    }


    private int getDomainID(UserStoreManager userStoreManager) throws UserStoreException {

        int domainId = 0;

        AbstractUserStoreManager ausm = (AbstractUserStoreManager) userStoreManager;

        String domainName = ausm.getRealmConfiguration().getUserStoreProperty(UserCoreConstants.RealmConfig
                .PROPERTY_DOMAIN_NAME).toUpperCase();

        String domainIdQuery = "SELECT UM_DOMAIN_ID FROM UM_DOMAIN WHERE UM_DOMAIN_NAME=? AND UM_TENANT_ID=?";

        try {

            Connection indbs = JDBCPersistenceManager.getInstance().getDBConnection();

            PreparedStatement domainIdStatement = indbs.prepareStatement(domainIdQuery);

            domainIdStatement.setString(1, domainName);
            domainIdStatement.setInt(2, userStoreManager.getTenantId());

            ResultSet resultSet = domainIdStatement.executeQuery();

            while (resultSet.next()) {
                domainId = resultSet.getInt("UM_DOMAIN_ID");
            }

        } catch (IdentityException | SQLException | UserStoreException e) {
            throw new UserStoreException("Error while obtaining domain ID of the userstore", e);
        }
        return domainId;
    }

    @Override
    public boolean doPostSetUserClaimValues(String userName, Map<String, String> claims,
                                            String profileName, UserStoreManager userStoreManager)
            throws UserStoreException {


        if (isRenameOperation.get() && !isAddUserOperation.get()) {

            //----Begin Rename Logic----//


            AbstractUserStoreManager abstractUserStoreManager = (AbstractUserStoreManager) userStoreManager;


            claims.remove("profileConfiguration");

            Map<String, String> realmProperties = userStoreManager.getRealmConfiguration().getRealmProperties();

            String passwordString = "";

            if (userStoreManager instanceof ReadWriteLDAPUserStoreManager) {

                RealmConfiguration realmConfig = userStoreManager.getRealmConfiguration();

                LDAPConnectionContext connectionContext = new LDAPConnectionContext(realmConfig);

                // get the LDAP Directory context
                DirContext dirContext = connectionContext.getContext();
                DirContext subDirContext = null;
                // search the relevant user entry by user name
                String userSearchBase = realmConfig.getUserStoreProperty(LDAPConstants.USER_SEARCH_BASE);
                String userSearchFilter = realmConfig.getUserStoreProperty(LDAPConstants.USER_NAME_SEARCH_FILTER);

                String[] userNames = userName.split(CarbonConstants.DOMAIN_SEPARATOR);
                if (userNames.length > 1) {
                    userName = userNames[1];
                }
                userSearchFilter = userSearchFilter.replace("?", userName);

                SearchControls searchControls = new SearchControls();
                searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
                searchControls.setReturningAttributes(null);

                NamingEnumeration<SearchResult> returnedResultList = null;
                SearchResult res = null;
                String scimId;
                try {
                    returnedResultList = dirContext
                            .search(userSearchBase, userSearchFilter, searchControls);
                    res = returnedResultList.next();
                    Attributes allAttrs = res.getAttributes();

                    Object cred = allAttrs.get("userPassword").get();

                    scimId = allAttrs.get("scimId").get().toString();

                    byte[] passwordBytes = (byte[]) cred;

                    passwordString = new String(passwordBytes);


                } catch (NamingException e) {
                    String errorMessage = "Results could not be retrieved from the directory context for user : " + userName;
                    if (log.isDebugEnabled()) {
                        log.debug(errorMessage, e);
                    }
                    throw new UserStoreException(errorMessage, e);
                } finally {
                    JNDIUtil.closeNamingEnumeration(returnedResultList);
                }

                userStoreManager.addUser(newUserName.get(), passwordString, null, claims, profileName);

                Attributes updatedAttributes = new BasicAttributes();
                Attribute scimMod = new BasicAttribute("scimId", scimId);

                updatedAttributes.put(scimMod);

                try {
                    subDirContext = (DirContext) dirContext.lookup(userSearchBase);

                    String newUserEntry = "uid=?".replace("?", newUserName.get());

                    subDirContext.modifyAttributes(newUserEntry, DirContext.REPLACE_ATTRIBUTE,
                            updatedAttributes);
                } catch (NamingException e) {

                    throw new UserStoreException("Error while updating scimId of the user", e);
                }

                userStoreManager.deleteUser(userName);


            } else if (userStoreManager instanceof JDBCUserStoreManager) {

                realmProperties.get("");
                userStoreManager.getDefaultUserStoreProperties();
                RealmConfiguration realmConfig = userStoreManager.getRealmConfiguration();

                String connectionURL = realmConfig.getUserStoreProperty("url");
                String connectionName = realmConfig.getUserStoreProperty("userName");
                String connectionPassword = realmConfig.getUserStoreProperty("password");


                try {
                    Connection usDbCon = null;

                    Properties connectionProperties = new Properties();
                    connectionProperties.put("user", connectionName);
                    connectionProperties.put("password", connectionPassword);

                    usDbCon = DriverManager.getConnection(connectionURL, connectionProperties);

                    String renameQuery = "UPDATE UM_USER SET UM_USER_NAME=? WHERE UM_USER_NAME=? AND UM_TENANT_ID=?";

                    PreparedStatement renameStatement = usDbCon.prepareStatement(renameQuery);


                    renameStatement.setString(1, newUserName.get());
                    renameStatement.setString(2, userName);
                    renameStatement.setInt(3, userStoreManager.getTenantId());

                    renameStatement.execute();

                    usDbCon.close();

                } catch (SQLException e) {
                    throw new UserStoreException("Error while updating user information ", e);
                }
            }

            try {
                Connection indbConn = JDBCPersistenceManager.getInstance().getDBConnection();

                String renameQuery = "UPDATE IDN_UID_USER SET IDN_USERNAME=? WHERE IDN_USERNAME=? AND " +
                        "IDN_STORE_DOMAIN=? AND IDN_TENANT=?";
                PreparedStatement renameStatement = indbConn.prepareStatement(renameQuery);

                renameStatement.setString(1, newUserName.get());
                renameStatement.setString(2, userName);
                renameStatement.setInt(3, getDomainID(userStoreManager));
                renameStatement.setInt(4, userStoreManager.getTenantId());

                renameStatement.execute();

                indbConn.commit();
                indbConn.close();

            } catch (IdentityException | SQLException e) {
                throw new UserStoreException("Error while updating user UID information ", e);
            }


            //---- End of Rename Logic --//
            isRenameOperation.set(false);


        }

        return true;

    }


    /**
     * Deleting user from the identity database. What are the registry keys ?
     */
    @Override
    public boolean doPostDeleteUser(String userName, UserStoreManager userStoreManager)
            throws UserStoreException {

        IdentityMgtConfig config = IdentityMgtConfig.getInstance();
        if (!config.isListenerEnable()) {
            return true;
        }
        // remove from the identity store
        try {
            IdentityMgtConfig.getInstance().getIdentityDataStore()
                    .remove(userName, userStoreManager);
        } catch (IdentityException e) {
            throw new UserStoreException("Error while removing user: " + userName
                    + " from identity data store", e);
        }
        // deleting registry meta-data
        UserRegistry registry = null;
        try {
            registry = IdentityMgtServiceComponent.getRegistryService().getConfigSystemRegistry(
                    userStoreManager.getTenantId());
            String identityKeyMgtPath = IdentityMgtConstants.IDENTITY_MANAGEMENT_KEYS
                    + RegistryConstants.PATH_SEPARATOR + userStoreManager.getTenantId()
                    + RegistryConstants.PATH_SEPARATOR + userName;

            if (registry.resourceExists(identityKeyMgtPath)) {
                registry.delete(identityKeyMgtPath);
            }
        } catch (RegistryException e) {
            log.error("Error while deleting recovery data for user : " + userName + " in tenant : "
                    + userStoreManager.getTenantId(), e);
        }

        AbstractUserStoreManager abstractUserStoreManager = (AbstractUserStoreManager) userStoreManager;

        String domainName = abstractUserStoreManager.getRealmConfiguration().getUserStoreProperty
                (UserCoreConstants.RealmConfig.PROPERTY_DOMAIN_NAME);

        if (!isRenameOperation.get()) {
            try {
                Connection indbConn = JDBCPersistenceManager.getInstance().getDBConnection();

                String deleteQuery = "DELETE FROM IDN_UID_USER WHERE IDN_USERNAME=? AND " +
                        "IDN_STORE_DOMAIN=? AND IDN_TENANT=?";
                PreparedStatement deleteStatement = indbConn.prepareStatement(deleteQuery);

                deleteStatement.setString(1, userName);
                deleteStatement.setInt(2, getDomainID(userStoreManager));
                deleteStatement.setInt(3, userStoreManager.getTenantId());

                deleteStatement.execute();

                indbConn.commit();
                indbConn.close();

            } catch (IdentityException | SQLException | UserStoreException e) {
                throw new UserStoreException("Error occurred while deleting user unique identifier", e);
            }
        }

        return true;
    }

    /**
     * Adding the user identity data to the claims set
     */
    @Override
    public boolean doPostGetUserClaimValues(String userName, String[] claims, String profileName,
                                            Map<String, String> claimMap,
                                            UserStoreManager storeManager)
            throws UserStoreException {
        IdentityMgtConfig config = IdentityMgtConfig.getInstance();
        if (!config.isListenerEnable()) {
            return true;
        }
        if (claimMap == null) {
            claimMap = new HashMap<String, String>();
        }
        UserIdentityDataStore identityDataStore =
                IdentityMgtConfig.getInstance()
                        .getIdentityDataStore();
        // check if there are identity claims
        boolean containsIdentityClaims = false;
        for (String claim : claims) {
            if (claim.contains(UserCoreConstants.ClaimTypeURIs.CHALLENGE_QUESTION_URI) ||
                    claim.contains(UserCoreConstants.ClaimTypeURIs.IDENTITY_CLAIM_URI)) {
                containsIdentityClaims = true;
                break;
            }
        }
        // if there are no identity claims, let it go
        if (!containsIdentityClaims) {
            return true;
        }
        // there is/are identity claim/s . load the dto
        UserIdentityClaimsDO identityDTO = identityDataStore.load(userName, storeManager);
        // if no user identity data found, just continue
        if (identityDTO == null) {
            return true;
        }
        // data found, add the values for security questions and identity claims
        for (String claim : claims) {
            if (identityDTO.getUserDataMap().containsKey(claim)) {
                claimMap.put(claim, identityDTO.getUserDataMap().get(claim));
            }
        }
        return true;
    }

    /**
     * Returning the user identity data as a claim
     */
    @Override
    public boolean doPostGetUserClaimValue(String userName, String claim, List<String> claimValue,
                                           String profileName, UserStoreManager storeManager)
            throws UserStoreException {

        return true;
    }

    @Override
    public boolean doPostUpdateCredential(String userName, Object credential, UserStoreManager userStoreManager) throws UserStoreException {

        IdentityMgtConfig config = IdentityMgtConfig.getInstance();

        UserIdentityClaimsDO userIdentityDTO = module.load(userName, userStoreManager);

        if (userIdentityDTO == null) {
            userIdentityDTO = new UserIdentityClaimsDO(userName);
        }

        // Do not timestamp if OTP enabled.
        boolean userOTPEnabled = userIdentityDTO.getOneTimeLogin();

        if (config.isAuthPolicyExpirePasswordCheck() && !userOTPEnabled && (!userStoreManager.isReadOnly())) {

            userIdentityDTO.setPasswordTimeStamp(Calendar.getInstance().getTimeInMillis());

            try {
                // Store the new timestamp after change password
                module.store(userIdentityDTO, userStoreManager);

            } catch (IdentityException e) {
                throw new UserStoreException(
                        "Error while saving user store data for user : "
                                + userName, e);
            }

        }

        return true;
    }
}
