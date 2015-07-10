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

package org.wso2.carbon.identity.mgt.util;

import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.mgt.IdentityMgtConfig;
import org.wso2.carbon.identity.mgt.IdentityMgtServiceException;
import org.wso2.carbon.identity.mgt.beans.UserIdentityMgtBean;
import org.wso2.carbon.identity.mgt.dto.UserIdentityClaimDTO;
import org.wso2.carbon.identity.mgt.dto.UserIdentityClaimsDO;
import org.wso2.carbon.identity.mgt.dto.UserRecoveryDTO;
import org.wso2.carbon.identity.mgt.dto.UserRecoveryDataDO;
import org.wso2.carbon.identity.mgt.internal.IdentityMgtServiceComponent;
import org.wso2.carbon.identity.mgt.store.JDBCUserRecoveryDataStore;
import org.wso2.carbon.identity.mgt.store.UserIdentityDataStore;
import org.wso2.carbon.user.api.Claim;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the Utility class used by the admin service to read and write
 * identity data.
 *
 * @author sga
 */
public class UserIdentityManagementUtil {

    private UserIdentityManagementUtil() {
    }

    /**
     * Returns the registration information such as the temporary password or
     * the confirmation code
     *
     * @param userName
     * @param userStoreManager
     * @param tenantId
     * @return
     * @throws IdentityException
     */
    public static UserRecoveryDTO getUserIdentityRecoveryData(String userName,
                                                              UserStoreManager userStoreManager,
                                                              int tenantId)
            throws IdentityException {

        JDBCUserRecoveryDataStore metadatStore = new JDBCUserRecoveryDataStore();
        UserRecoveryDTO registrationDTO = new UserRecoveryDTO(userName);
        return registrationDTO;
    }

    /**
     * Locks the user account.
     *
     * @param userName
     * @param userStoreManager
     * @throws IdentityException
     */
    public static void lockUserAccount(String userName, UserStoreManager userStoreManager)
            throws IdentityException {
        UserIdentityDataStore store = IdentityMgtConfig.getInstance().getIdentityDataStore();
        UserIdentityClaimsDO userIdentityDO = store.load(UserCoreUtil.removeDomainFromName(userName), userStoreManager);
        if (userIdentityDO != null) {
            userIdentityDO.setAccountLock(true);
            userIdentityDO.setUnlockTime(0);
            store.store(userIdentityDO, userStoreManager);
        } else {
            throw new IdentityException("No user account found for user " + userName);
        }
    }

    /**
     * Unlocks the user account
     *
     * @param userName
     * @param userStoreManager
     * @throws IdentityException
     */
    public static void unlockUserAccount(String userName, UserStoreManager userStoreManager)
            throws IdentityException {
        UserIdentityDataStore store = IdentityMgtConfig.getInstance().getIdentityDataStore();
        UserIdentityClaimsDO userIdentityDO = store.load(UserCoreUtil.removeDomainFromName(userName), userStoreManager);
        if (userIdentityDO != null) {
            userIdentityDO.setAccountLock(false);
            userIdentityDO.setUnlockTime(0);
            store.store(userIdentityDO, userStoreManager);
        } else {
            throw new IdentityException("No user account found for user " + userName);
        }

    }

    /**
     * Returns an array of primary security questions
     *
     * @param tenantId
     * @return
     * @throws IdentityException
     */
    public static String[] getPrimaryQuestions(int tenantId) throws IdentityException {
        JDBCUserRecoveryDataStore store = new JDBCUserRecoveryDataStore();
        UserRecoveryDataDO[] metadata = store.load("TENANT", tenantId);
        if (metadata.length < 1) {
            return new String[0];
        }
        List<String> validSecurityQuestions = new ArrayList<String>();
        String[] questionsList = new String[validSecurityQuestions.size()];
        return validSecurityQuestions.toArray(questionsList);
    }

    /**
     * Add or update primary security questions
     *
     * @param primarySecurityQuestion
     * @param tenantId
     * @throws IdentityException
     */
    public static void addPrimaryQuestions(String[] primarySecurityQuestion, int tenantId) throws IdentityException {
        JDBCUserRecoveryDataStore store = new JDBCUserRecoveryDataStore();
        UserRecoveryDataDO[] metadata = new UserRecoveryDataDO[primarySecurityQuestion.length];
        int i = 0;
        for (String secQuestion : primarySecurityQuestion) {
            if (!secQuestion.contains(UserCoreConstants.ClaimTypeURIs.CHALLENGE_QUESTION_URI)) {
                throw new IdentityException("One or more security questions does not contain the namespace " +
                        UserCoreConstants.ClaimTypeURIs.CHALLENGE_QUESTION_URI);
            }
            metadata[i++] =
                    new UserRecoveryDataDO("TENANT", tenantId,
                            UserRecoveryDataDO.METADATA_PRIMARAY_SECURITY_QUESTION,
                            secQuestion);
        }
        store.store(metadata);
    }

    /**
     * Remove primary security questions
     *
     * @param tenantId
     * @throws IdentityException
     */
    public static void removePrimaryQuestions(String[] primarySecurityQuestion, int tenantId) throws IdentityException {

        UserRecoveryDataDO[] metadata = new UserRecoveryDataDO[primarySecurityQuestion.length];
        int i = 0;
        for (String secQuestion : primarySecurityQuestion) {
            if (!secQuestion.contains(UserCoreConstants.ClaimTypeURIs.CHALLENGE_QUESTION_URI)) {
                throw new IdentityException("One or more security questions does not contain the namespace " +
                        UserCoreConstants.ClaimTypeURIs.CHALLENGE_QUESTION_URI);
            }
            metadata[i++] =
                    new UserRecoveryDataDO("TENANT", tenantId,
                            UserRecoveryDataDO.METADATA_PRIMARAY_SECURITY_QUESTION,
                            secQuestion);
        }

    }

    // ---- Util methods for authenticated users ----///

    /**
     * Update security questions of the logged in user.
     *
     * @param securityQuestion
     * @param userStoreManager
     * @throws IdentityException
     */
    public static void updateUserSecurityQuestions(String userName, UserIdentityClaimDTO[] securityQuestion,
                                                   UserStoreManager userStoreManager)
            throws IdentityException {
        UserIdentityDataStore store = IdentityMgtConfig.getInstance().getIdentityDataStore();
        UserIdentityClaimsDO userIdentityDO = store.load(userName, userStoreManager);
        if (userIdentityDO != null) {
            userIdentityDO.updateUserSequeiryQuestions(securityQuestion);
            store.store(userIdentityDO, userStoreManager);
        } else {
            throw new IdentityException("No user account found for user " + userName);
        }
    }

    /**
     * Returns security questions of the logged in user
     *
     * @param userStoreManager
     * @return
     * @throws IdentityMgtServiceException
     */
    public static UserIdentityClaimDTO[] getUserSecurityQuestions(String userName,
                                                                  UserStoreManager userStoreManager)
            throws IdentityMgtServiceException {
        UserIdentityDataStore store = IdentityMgtConfig.getInstance().getIdentityDataStore();
        UserIdentityClaimsDO userIdentityDO;
        userIdentityDO = store.load(userName, userStoreManager);
        if (userIdentityDO != null) {
            return userIdentityDO.getUserSequeiryQuestions();
        } else {
            throw new IdentityMgtServiceException("No user account found for user " + userName);
        }
    }

    /**
     * Updates users recovery data such as the phone number, email etc
     *
     * @param userStoreManager
     * @param userIdentityRecoveryData
     * @throws IdentityException
     */
    public static void updateUserIdentityClaims(String userName, UserStoreManager userStoreManager,
                                                UserIdentityClaimDTO[] userIdentityRecoveryData)
            throws IdentityException {

        UserIdentityDataStore store = IdentityMgtConfig.getInstance().getIdentityDataStore();
        UserIdentityClaimsDO userIdentityDO = store.load(userName, userStoreManager);
        if (userIdentityDO != null) {
            userIdentityDO.updateUserIdentityRecoveryData(userIdentityRecoveryData);
            store.store(userIdentityDO, userStoreManager);
        } else {
            throw new IdentityException("No user account found for user " + userName);
        }

    }

    /**
     * Returns all user claims which can be used in the identity recovery
     * process
     *
     * @param userName
     * @param userStoreManager
     * @return
     * @throws IdentityException
     */
    public static UserIdentityClaimDTO[] getUserIdentityClaims(String userName,
                                                               UserStoreManager userStoreManager)
            throws IdentityException {
        UserIdentityDataStore store = IdentityMgtConfig.getInstance().getIdentityDataStore();
        UserIdentityClaimsDO userIdentityDO = store.load(userName, userStoreManager);
        if (userIdentityDO != null) {
            return userIdentityDO.getUserIdentityRecoveryData();
        } else {
            throw new IdentityException("No user account found for user " + userName);
        }
    }

    /**
     * Validates user identity metadata to be valid or invalid.
     *
     * @param userName
     * @param tenantId
     * @param metadataType
     * @param metadata
     * @return
     * @throws IdentityException
     */
    public static boolean isValidIdentityMetadata(String userName, int tenantId, String metadataType,
                                                  String metadata) throws IdentityException {

        return false;
    }

    /**
     * Invalidates the identity metadata
     *
     * @param userName
     * @param tenantId
     * @param metadataType
     * @param metadata
     * @throws IdentityException
     */
    public static void invalidateUserIdentityMetadata(String userName, int tenantId, String metadataType,
                                                      String metadata) throws IdentityException {
        JDBCUserRecoveryDataStore store = new JDBCUserRecoveryDataStore();
        UserRecoveryDataDO metadataDO =
                new UserRecoveryDataDO(userName, tenantId, metadataType,
                        metadata);
        store.invalidate(metadataDO);

    }

    /**
     * Stores new metadata
     *
     * @param metadata
     * @throws IdentityException
     */
    public static void storeUserIdentityMetadata(UserRecoveryDataDO metadata) throws IdentityException {
        JDBCUserRecoveryDataStore store = new JDBCUserRecoveryDataStore();
        metadata.setValid(true);
        store.store(metadata);
    }


    public static void storeUserIdentityClaims(UserIdentityClaimsDO identityClaims,
                                               org.wso2.carbon.user.core.UserStoreManager userStoreManager)
            throws IdentityException {
        IdentityMgtConfig.getInstance().getIdentityDataStore()
                .store(identityClaims, userStoreManager);
    }

    public static UserRecoveryDataDO getUserIdentityMetadata(String userName, int tenantId,
                                                             String metadataType) {
        return null;
    }

    /**
     * Returns all user claims
     *
     * @param userName
     * @return
     * @throws IdentityMgtServiceException
     */
    public static UserIdentityClaimDTO[] getAllUserIdentityClaims(String userName)
            throws IdentityMgtServiceException {
        int tenantId = 0;
        try {
            tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
            UserStoreManager userStoreManager =
                    IdentityMgtServiceComponent.getRealmService()
                            .getTenantUserRealm(tenantId)
                            .getUserStoreManager();
            // read all claims and convert them to UserIdentityClaimDTO
            Claim[] claims = userStoreManager.getUserClaimValues(userName, null);
            List<UserIdentityClaimDTO> allDefaultClaims = new ArrayList<UserIdentityClaimDTO>();
            for (Claim claim : claims) {
                if (claim.getClaimUri().contains(UserCoreConstants.DEFAULT_CARBON_DIALECT)) {
                    UserIdentityClaimDTO claimDTO = new UserIdentityClaimDTO();
                    claimDTO.setClaimUri(claim.getClaimUri());
                    claimDTO.setClaimValue(claim.getValue());
                    allDefaultClaims.add(claimDTO);
                }
            }
            UserIdentityClaimDTO[] claimDTOs = new UserIdentityClaimDTO[allDefaultClaims.size()];
            return allDefaultClaims.toArray(claimDTOs);
        } catch (UserStoreException e) {
            throw new IdentityMgtServiceException("Error while getting user identity claims", e);
        }
    }


    public static void notifyViaEmail(UserIdentityMgtBean bean) {

        //TODO
        // if not module is defined, the default will be loaded

    }

    public static void notifyWithEmail(UserRecoveryDTO notificationBean) {

        // if not module is defined, the default will be loaded

    }

    /**
     * Generates a random password
     *
     * @return
     */
    public static char[] generateTemporaryPassword() {
        IdentityMgtConfig config = IdentityMgtConfig.getInstance();
        return config.getPasswordGenerator().generatePassword();

    }

    /**
     * Returns a random confirmation code
     *
     * @return
     */
    public static String generateRandomConfirmationCode() {
        return new String(generateTemporaryPassword());
    }

    /**
     * @param claims
     * @param tenantId
     * @return
     * @throws IdentityMgtServiceException - If user cannot be retrieved using the provided claims.
     */
    public static String getUsernameByClaims(UserIdentityClaimDTO[] claims, int tenantId)
            throws IdentityMgtServiceException {

        if (claims == null || claims.length < 1) {
            throw new IdentityMgtServiceException("No fields found for user search");
        }

        String userName = null;
        String[] tempUserList = null;

        // Need to populate the claim email as the first element in the
        // passed array.
        for (int i = 0; i < claims.length; i++) {

            UserIdentityClaimDTO claim = claims[i];
            if (claim.getClaimUri() != null && claim.getClaimValue() != null) {

                String[] userList = getUserList(tenantId, claim.getClaimUri(),
                        claim.getClaimValue());

                if (userList != null && userList.length > 0) {
                    if (userList.length == 1) {
                        return userList[0];
                    } else {
                        //If more than one user find the first matching user. Hence need to define unique claims
                        if (tempUserList != null) {
                            for (int j = 0; j < tempUserList.length; j++) {
                                for (int x = 0; x < userList.length; x++) {
                                    if (tempUserList[j].equals(userList[x])) {
                                        return userList[x];
                                    }
                                }
                            }
                        }
                        tempUserList = userList;
                        continue;
                    }
                } else {
                    throw new IdentityMgtServiceException(
                            "No associated user is found for given claim values");

                }
            }
        }

        return userName;
    }

    private static String[] getUserList(int tenantId, String claim, String value) throws IdentityMgtServiceException {

        org.wso2.carbon.user.core.UserStoreManager userStoreManager = null;
        String[] userList = null;
        RealmService realmService = IdentityMgtServiceComponent.getRealmService();

        try {
            if (realmService.getTenantUserRealm(tenantId) != null) {
                userStoreManager = (org.wso2.carbon.user.core.UserStoreManager) realmService.getTenantUserRealm(tenantId).
                        getUserStoreManager();
            }

        } catch (Exception e) {
            String msg = "Error retrieving the user store manager for the tenant";
            throw new IdentityMgtServiceException(msg, e);
        }
        try {
            if (userStoreManager != null) {
                userList = userStoreManager.getUserList(claim, value, null);
            }
            return userList;
        } catch (Exception e) {
            String msg = "Unable to retrieve the claim for the given tenant";
            throw new IdentityMgtServiceException(msg, e);
        }
    }
}
