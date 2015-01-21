/*
 * Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.mgt.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.captcha.mgt.beans.CaptchaInfoBean;
import org.wso2.carbon.captcha.mgt.util.CaptchaUtil;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.IdentityClaimManager;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.mgt.ChallengeQuestionProcessor;
import org.wso2.carbon.identity.mgt.IdentityMgtConfig;
import org.wso2.carbon.identity.mgt.IdentityMgtServiceException;
import org.wso2.carbon.identity.mgt.RecoveryProcessor;
import org.wso2.carbon.identity.mgt.beans.VerificationBean;
import org.wso2.carbon.identity.mgt.constants.IdentityMgtConstants;
import org.wso2.carbon.identity.mgt.dto.ChallengeQuestionDTO;
import org.wso2.carbon.identity.mgt.dto.ChallengeQuestionIdsDTO;
import org.wso2.carbon.identity.mgt.dto.NotificationDataDTO;
import org.wso2.carbon.identity.mgt.dto.UserChallengesDTO;
import org.wso2.carbon.identity.mgt.dto.UserDTO;
import org.wso2.carbon.identity.mgt.dto.UserIdentityClaimDTO;
import org.wso2.carbon.identity.mgt.dto.UserRecoveryDTO;
import org.wso2.carbon.identity.mgt.internal.IdentityMgtServiceComponent;
import org.wso2.carbon.identity.mgt.util.UserIdentityManagementUtil;
import org.wso2.carbon.identity.mgt.util.Utils;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.Permission;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.claim.Claim;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.tenant.TenantManager;
import org.wso2.carbon.user.mgt.UserMgtConstants;

/**
 * This service provides the services needed to recover user password and user
 * account information.
 * 
 */
public class UserInformationRecoveryService {

	Log log = LogFactory.getLog(UserInformationRecoveryService.class);

	public CaptchaInfoBean getCaptcha() throws IdentityMgtServiceException {

		if(log.isDebugEnabled()) {
			log.debug("User get captcha image request received");
		}
		
		try {
			CaptchaUtil.cleanOldCaptchas();
			CaptchaInfoBean bean = CaptchaUtil.generateCaptchaImage();

			if(log.isDebugEnabled()) {
				log.debug("Captcha stored: " + bean.getImagePath());
				log.debug("Captcha generated successfully");
			}
			
			return bean;

		} catch (Exception e) {
			log.error("Error while generating captcha", e);
			throw new IdentityMgtServiceException("Error while generating captcha", e);
		}
	}

	public VerificationBean verifyUser(String username, CaptchaInfoBean captcha)
			throws IdentityMgtServiceException {

		UserDTO userDTO;
		VerificationBean bean = new VerificationBean();
		if(log.isDebugEnabled()) {
			log.debug("User verification request received with username : " + username);
		}

		if (IdentityMgtConfig.getInstance().isCaptchaVerificationInternallyManaged()) {
			try {
				CaptchaUtil.processCaptchaInfoBean(captcha);
			} catch (Exception e) {
				if(log.isDebugEnabled()) {
					log.error(e.getMessage());					
				}
				bean.setError(VerificationBean.ERROR_CODE_INVALID_CAPTCHA + " " + e.getMessage());
				bean.setVerified(false);
				return bean;
			}
		}

		try {
			userDTO = Utils.processUserId(username);
		} catch (IdentityException e) {
			log.error("Error when verifying user: " + username, e);
			bean.setError(VerificationBean.ERROR_CODE_INVALID_USER + " " + e.getMessage());
			bean.setVerified(false);
			return bean;
		}

		RecoveryProcessor processor = IdentityMgtServiceComponent.getRecoveryProcessor();

		return processor.verifyUserForRecovery(1, userDTO);
	}

	public VerificationBean sendRecoveryNotification(String username, String key, String notificationType)
			throws IdentityMgtServiceException {

		UserDTO userDTO = null;

		if(log.isDebugEnabled()) {
			log.debug("User recovery notification sending request received with username : " + username + " notification type :" + notificationType);
		}
		try {
			userDTO = Utils.processUserId(username);
		} catch (IdentityException e) {
			if(log.isDebugEnabled()) {
				log.debug(e.getMessage());
			}	
		}

		RecoveryProcessor processor = IdentityMgtServiceComponent.getRecoveryProcessor();

		VerificationBean bean = null;
		try {
			bean = processor.verifyConfirmationCode(1, userDTO.getUserId(), key);
			
			if (!bean.isVerified()) {
				log.error("Invalid user is trying to recover the password with username : " + username);
				return new VerificationBean(VerificationBean.ERROR_CODE_INVALID_USER + 
				                            " Invalid user is trying to recover the password with username : " + username);
			}
		} catch (IdentityException e1) {
			log.error("Error when sending recovery message for user: " + username, e1);
			return new VerificationBean(VerificationBean.ERROR_CODE_INVALID_CODE + " " + e1.getMessage());
		}

		UserRecoveryDTO dto = new UserRecoveryDTO(userDTO);
		dto.setNotification(IdentityMgtConstants.Notification.PASSWORD_RESET_RECOVERY);
		dto.setNotificationType(notificationType);

		NotificationDataDTO dataDTO = null;
		try {
			if(log.isDebugEnabled()) {
				log.debug("Initiating the notification sending process");
			}
			dataDTO = processor.recoverWithNotification(dto);
			
//			Send email data only if not internally managed.
			if(! (IdentityMgtConfig.getInstance().isNotificationInternallyManaged())){ 
				bean.setNotificationData(dataDTO);
			}	

			
		} catch (IdentityException e) {
			log.error("Error when sending recovery message for user: " + username, e);
			bean.setError(VerificationBean.ERROR_CODE_UN_EXPECTED + " " + e.getMessage());
			bean.setVerified(false);
		}
		return bean;
	}

	/**
	 * This method is used to verify the confirmation code sent to user is
	 * correct and validates. Before calling this method it needs to supply a
	 * Captcha and should call getCaptcha().
	 * 
	 * @param username
	 *            - username of whom the password needs to be recovered.
	 * @param code
	 *            - confirmation code sent to user by notification.
	 * @param captcha
	 *            - generated captcha with answer for this communication.
	 * 
	 * @return - VerificationBean with new code to be used in updatePassword().
	 * 
	 * @throws IdentityMgtServiceException
	 */
	public VerificationBean verifyConfirmationCode(String username, String code,
			CaptchaInfoBean captcha) throws IdentityMgtServiceException {

		UserDTO userDTO;
		VerificationBean bean = new VerificationBean();

		if(log.isDebugEnabled()) {
			log.debug("User confirmation code verification request received with username :"+ username);
		}
		if (IdentityMgtConfig.getInstance().isCaptchaVerificationInternallyManaged()) {
			try {
				CaptchaUtil.processCaptchaInfoBean(captcha);
			} catch (Exception e) {
				if(log.isDebugEnabled()) {
					log.debug(e.getMessage());
				}
				bean.setError(VerificationBean.ERROR_CODE_INVALID_CAPTCHA + " " + e.getMessage());
				bean.setVerified(false);
				return bean;
			}
		}

		try {
			userDTO = Utils.processUserId(username);
		} catch (IdentityException e) {
			if(log.isDebugEnabled()) {
				log.debug(e.getMessage());
			}
			bean.setError(VerificationBean.ERROR_CODE_INVALID_USER + " " + e.getMessage());
			bean.setVerified(false);
			return bean;
		}

		RecoveryProcessor processor = IdentityMgtServiceComponent.getRecoveryProcessor();

		try {
			bean = processor.verifyConfirmationCode(2, userDTO.getUserId(), code);
			if(bean.isVerified()) {
				bean = processor.updateConfirmationCode(3, userDTO.getUserId(), userDTO.getTenantId());
				if(log.isDebugEnabled()){
					log.debug("User confirmation code verification successful for user: " + username);
				}
			} else {
				bean.setVerified(false);
				bean.setKey("");
				log.error(bean.getError());
			}
		} catch (IdentityException e) {
			log.error("Error verifying confirmation code for user: "+ username, e);
			bean.setError(VerificationBean.ERROR_CODE_INVALID_CODE + " " + e.getMessage());
			bean.setVerified(false);
		}

		return bean;

	}

	/**
	 * This method is used to update the password in the system for password
	 * recovery process. Before calling this method caller needs to call
	 * verifyConfirmationCode and get the newly generated confirmation code.
	 * 
	 * @param username
	 *            - username
	 * @param confirmationCode
	 *            - newly generated confirmation code
	 * @param newPassword
	 *            - new password
	 * 
	 * @return - VerificationBean with operation status true or false.
	 * 
	 * @throws IdentityMgtServiceException
	 */
	public VerificationBean updatePassword(String username, String confirmationCode,
			String newPassword) throws IdentityMgtServiceException {

		RecoveryProcessor recoveryProcessor = IdentityMgtServiceComponent.getRecoveryProcessor();
		VerificationBean bean = null;
		
		if(log.isDebugEnabled()) {
			log.debug("User update password request received with username: "+ username);
		}
		
		try {
			UserDTO userDTO = Utils.processUserId(username);

			TenantManager tenantManager = IdentityMgtServiceComponent.getRealmService().getTenantManager();
			int tenantId = 0;
			try {
				tenantId = tenantManager.getTenantId(userDTO.getTenantDomain());
			} catch (UserStoreException e) {
				log.warn("No Tenant id for tenant domain " + userDTO.getTenantDomain());
			}

			if (recoveryProcessor.verifyConfirmationCode(3, userDTO.getUserId(), confirmationCode).isVerified()) {
				Utils.updatePassword(userDTO.getUserId(), tenantId, newPassword);
				log.info("Credential is updated for user : " + userDTO.getUserId()
						+ " and tenant domain : " + userDTO.getTenantDomain());
				bean = new VerificationBean(true);
			} else {
				String msg = "Invalid user tried to update credential with user Id : "
						+ userDTO.getUserId() + " and tenant domain : " + userDTO.getTenantDomain();
				bean = new VerificationBean(VerificationBean.ERROR_CODE_INVALID_USER + " " + msg); 
				bean.setVerified(false);
				log.error(msg);
			}

		} catch (Exception e) {
			bean = new VerificationBean(VerificationBean.ERROR_CODE_UN_EXPECTED + " Unable to change the password"); 
			bean.setVerified(false);
			log.error("Error while updating credential for user : " + username, e);
		}
		return bean;
	}

	public ChallengeQuestionIdsDTO getUserChallengeQuestionIds(String username, String confirmation)
			throws IdentityMgtServiceException {

		UserDTO userDTO = null;
		ChallengeQuestionIdsDTO idsDTO = new ChallengeQuestionIdsDTO();

		if(log.isDebugEnabled()) {
			log.debug("User challenge questions id request received with username: "+ username);
		}
		try {
			userDTO = Utils.processUserId(username);
		} catch (IdentityException e) {
			log.error("Error validating user: "+ username, e);
			idsDTO.setError(e.getMessage());
			return idsDTO;
		}

		RecoveryProcessor processor = IdentityMgtServiceComponent.getRecoveryProcessor();

		VerificationBean bean = null;
		try {
			bean = processor.verifyConfirmationCode(1, userDTO.getUserId(), confirmation);
			if(bean.isVerified()) {
				bean = processor.updateConfirmationCode(3, userDTO.getUserId(), userDTO.getTenantId());
			} else {
				bean.setVerified(false);
			}
		} catch (IdentityException e1) {
			log.error("Error when getting user claims for user: "+ username, e1);
			idsDTO.setError(VerificationBean.ERROR_CODE_INVALID_CODE+ " " + e1.getMessage());
			return idsDTO;
		}
		if (bean.isVerified()) {
			try {
				idsDTO = processor.getQuestionProcessor().getUserChallengeQuestionIds(
						userDTO.getUserId(), userDTO.getTenantId());
				idsDTO.setKey(bean.getKey());
				if(log.isDebugEnabled()){
					log.debug("User chanllenge question response successful for user: "+ username);
				}
			} catch (Exception e) {
				log.error("Error when getting user claims for user: "+ username, e);
				idsDTO.setError(VerificationBean.ERROR_CODE_UN_EXPECTED+ " " + e.getMessage());
				idsDTO.setKey("");
			}
		} else {
			log.error("Verfication failed for user. Error : " + bean.getError());
			idsDTO.setError(VerificationBean.ERROR_CODE_UN_EXPECTED+ " " + bean.getError());
			idsDTO.setKey("");
		}

		return idsDTO;

	}

	/**
	 * To get the challenge question for the user.
	 * 
	 * @param userName
	 * @param confirmation
	 * @param questionId
	 *            - Question id returned from the getUserChanllegneQuestionIds
	 *            method.
	 * 
	 * @return Populated question bean with the question details and the key.
	 * @throws IdentityMgtServiceException
	 */
	public UserChallengesDTO getUserChallengeQuestion(String userName, String confirmation,
			String questionId) throws IdentityMgtServiceException {

		UserDTO userDTO = null;
		UserChallengesDTO userChallengesDTO = new UserChallengesDTO();

		if(log.isDebugEnabled()) {
			log.debug("User challenge question request received with username :"+ userName);
		}
		
		try {
			userDTO = Utils.processUserId(userName);
		} catch (IdentityException e) {
			log.error("Error validating user: "+ userName, e);
			userChallengesDTO.setError(e.getMessage());
			return userChallengesDTO;
		}

		RecoveryProcessor processor = IdentityMgtServiceComponent.getRecoveryProcessor();

		VerificationBean bean;
		try {
			bean = processor.verifyConfirmationCode(3, userDTO.getUserId(), confirmation);
			if(bean.isVerified()) {
				bean = processor.updateConfirmationCode(3, userDTO.getUserId(), userDTO.getTenantId());
			}else {
				bean.setVerified(false);
			}
		} catch (IdentityException e1) {
			log.error("Error verifying confirmation code", e1);
			userChallengesDTO.setError(VerificationBean.ERROR_CODE_INVALID_CODE+ " "+ e1.getMessage());
			return userChallengesDTO;
		}

		if (bean.isVerified()) {
			userChallengesDTO = processor.getQuestionProcessor().getUserChallengeQuestion(
					userDTO.getUserId(), userDTO.getTenantId(), questionId);
			userChallengesDTO.setKey(bean.getKey());
			userChallengesDTO.setVerfied(true);
			if(log.isDebugEnabled()) {
				log.debug("User chanllenge question retrieved successfully");
			}
		} else {
			if(log.isDebugEnabled()) {
				log.debug("Verificaton failed for user. Error : " + bean.getError());
			}
			userChallengesDTO.setError(VerificationBean.ERROR_CODE_INVALID_USER+ " "+ bean.getError());
		}

		return userChallengesDTO;
	}

	/**
	 * This method is to verify the user supplied answer for the challenge
	 * question.
	 * 
	 * @param userName
	 * @param confirmation
	 * @param questionId
	 * @param answer
	 * 
	 * @return status and key details about the operation status.
	 * @throws IdentityMgtServiceException
	 */
	public VerificationBean verifyUserChallengeAnswer(String userName, String confirmation,
			String questionId, String answer) throws IdentityMgtServiceException {

		VerificationBean bean = new VerificationBean();
		bean.setVerified(false);

		if(log.isDebugEnabled()) {
			log.debug("User challenge answer request received with username :"+ userName);
		}
		
		if (questionId != null && answer != null) {

		} else {
			String error = "No challenge question id provided for verification";
			bean.setError(error);
			if(log.isDebugEnabled()) {
				log.debug(error);			
			}

			return bean;
		}

		UserDTO userDTO = null;
		try {
			userDTO = Utils.processUserId(userName);
		} catch (IdentityException e) {
			log.error("Error verifying user: "+ userName, e);
			bean.setError(VerificationBean.ERROR_CODE_INVALID_USER+ " "+ e.getMessage());
			bean.setVerified(false);
			return bean;
		}

		RecoveryProcessor recoveryProcessor = IdentityMgtServiceComponent.getRecoveryProcessor();

		try {
			bean = recoveryProcessor.verifyConfirmationCode(3, userDTO.getUserId(), confirmation);
			if(bean.isVerified()) {
				bean = recoveryProcessor.updateConfirmationCode(3, userDTO.getUserId(), userDTO.getTenantId());
			}else {
				bean.setVerified(false);
			}
		} catch (IdentityException e1) {
			log.error("Error verifying confirmation code", e1);
			bean.setError(VerificationBean.ERROR_CODE_INVALID_CODE+ " "+ e1.getMessage());
			bean.setVerified(false);
			return bean;
		}

		ChallengeQuestionProcessor processor = recoveryProcessor.getQuestionProcessor();

		UserChallengesDTO userChallengesDTO = new UserChallengesDTO();
		userChallengesDTO.setId(questionId);
		userChallengesDTO.setAnswer(answer);

		boolean verification = processor.verifyUserChallengeAnswer(userDTO.getUserId(),
				userDTO.getTenantId(), userChallengesDTO);

		if (verification) {
			bean.setError("");
			bean.setUserId(userName);
			if(log.isDebugEnabled()) {
				log.debug("User answer verification successful for user: " + userName);
			}
		} else {
			bean.setError("Answer verification failed for user: " + userName);
			bean.setVerified(false);
			bean.setKey(""); // clear the key to avoid returning to caller.
			if(log.isDebugEnabled()) {
				log.debug(bean.getError());
			}
		}

		return bean;
	}
	
    /**
     * Get all challenge questions
     *
     * @return array of questions
     * @throws IdentityMgtServiceException if fails
     */
    public ChallengeQuestionDTO[] getAllChallengeQuestions() throws IdentityMgtServiceException {

        ChallengeQuestionProcessor processor = IdentityMgtServiceComponent.
                getRecoveryProcessor().getQuestionProcessor();
        List<ChallengeQuestionDTO> questionDTOs = null;
        try {
            questionDTOs = processor.getAllChallengeQuestions();
        } catch (IdentityException e) {
            log.error("Error while loading user challenges", e);
            throw new IdentityMgtServiceException("Error while loading user challenges");
        }
        return questionDTOs.toArray(new ChallengeQuestionDTO[questionDTOs.size()]);

    }
    
	/**
	 * This returns the user supported claims.
	 * 
	 * @param dialect
	 * @return
	 * @throws IdentityException
	 */
	public UserIdentityClaimDTO[] getUserIdentitySupportedClaims(String dialect)
			throws IdentityException {
		IdentityClaimManager claimManager = null;
		Claim[] claims = null;
		UserRealm realm = null;

		claimManager = IdentityClaimManager.getInstance();
		realm = IdentityTenantUtil.getRealm(null, null);
		claims = claimManager.getAllSupportedClaims(dialect, realm);

		if (claims == null || claims.length == 0) {
			return new UserIdentityClaimDTO[0];
		}

		ArrayList<UserIdentityClaimDTO> claimList = new ArrayList<UserIdentityClaimDTO>();

		for (int i = 0; i < claims.length; i++) {
			if (claims[i].getDisplayTag() != null
					&& !IdentityConstants.PPID_DISPLAY_VALUE.equals(claims[i].getDisplayTag())) {
				if (UserCoreConstants.ClaimTypeURIs.ACCOUNT_STATUS.equals(claims[i].getClaimUri())) {
					continue;
				}
				if (claims[i].isSupportedByDefault() && (!claims[i].isReadOnly())) {

					UserIdentityClaimDTO claimDto = new UserIdentityClaimDTO();
					claimDto.setClaimUri(claims[i].getClaimUri());
					claimDto.setClaimValue(claims[i].getValue());
					claimList.add(claimDto);
				}
			}
		}
		
		return claimList.toArray(new UserIdentityClaimDTO[claimList.size()]);
	}

	/**
	 * Verifies the user against the provided claims and captcha information.
	 * 
	 * @param claims
	 * @param captcha
	 * @param tenantDomain
	 * @return
	 * @throws IdentityMgtServiceException
	 */
	public VerificationBean verifyAccount(UserIdentityClaimDTO[] claims, CaptchaInfoBean captcha,
			String tenantDomain) throws IdentityMgtServiceException {

		VerificationBean vBean = new VerificationBean();

		if (IdentityMgtConfig.getInstance().isCaptchaVerificationInternallyManaged()) {
			try {
				CaptchaUtil.processCaptchaInfoBean(captcha);
			} catch (Exception e) {
				log.error("Error processing captcha", e);
				vBean.setError(VerificationBean.ERROR_CODE_INVALID_CAPTCHA + " " + e.getMessage());
				vBean.setVerified(false);
				return vBean;
			}
		}
		
		String loggedInTenant = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
		if(tenantDomain != null && !tenantDomain.isEmpty() && !loggedInTenant.equals(tenantDomain)){
			String msg = "Trying to verify account unauthorized tenant space";
			log.error(msg);
			throw new IdentityMgtServiceException(msg);
		}
		if (tenantDomain == null || tenantDomain.isEmpty()) {
			tenantDomain = loggedInTenant;
		}

		try {
			int tenantId = Utils.getTenantId(tenantDomain);
			String userName = UserIdentityManagementUtil.getUsernameByClaims(claims, tenantId);

			if (userName != null) {
				UserDTO userDTO = new UserDTO(userName);
				userDTO.setTenantId(tenantId);

				UserRecoveryDTO dto = new UserRecoveryDTO(userDTO);
				dto.setNotification(IdentityMgtConstants.Notification.ACCOUNT_ID_RECOVERY);
				dto.setNotificationType("EMAIL");

				RecoveryProcessor processor = IdentityMgtServiceComponent.getRecoveryProcessor();
				NotificationDataDTO notificationDto = processor.notifyWithEmail(dto);

				vBean.setVerified(notificationDto.isNotificationSent());
				
//				Send email data only if not internally managed.
				if(! (IdentityMgtConfig.getInstance().isNotificationInternallyManaged())){ 
					vBean.setNotificationData(notificationDto);
				}				

			} else {
				vBean.setError("User not found");
				vBean.setVerified(false);
			}
		} catch (Exception e) {
			log.error("Error verifying user account", e);
			vBean.setError(e.getMessage());
			vBean.setVerified(false);
			return vBean;
		}

		return vBean;
	}
	
	/**
	 * This method is used to register an user in the system. The account will be locked if the
	 * Authentication.Policy.Account.Lock.On.Creation is set to true. Else user will be able to
	 * login after registration.
	 * 
	 * @param userName
	 * @param password
	 * @param claims
	 * @param profileName
	 * @param tenantDomain
	 * @return
	 * @throws IdentityMgtServiceException
	 */
	public VerificationBean registerUser(String userName, String password,
			UserIdentityClaimDTO[] claims, String profileName, String tenantDomain)
			throws IdentityMgtServiceException {

		VerificationBean vBean = new VerificationBean();

		org.wso2.carbon.user.core.UserStoreManager userStoreManager = null;
		Permission permission = null;

		String loggedInTenant = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
		if(tenantDomain != null && !tenantDomain.isEmpty() && !loggedInTenant.equals(tenantDomain)){
			String msg = "Trying to create users in unauthorized tenant space";
			log.error(msg);
			throw new IdentityMgtServiceException(msg);
		}
		if (tenantDomain == null || tenantDomain.isEmpty()) {
			tenantDomain = loggedInTenant;
		}

		RealmService realmService = IdentityMgtServiceComponent.getRealmService();
		int tenantId;

		try {

			tenantId = Utils.getTenantId(tenantDomain);
			if (realmService.getTenantUserRealm(tenantId) != null) {
				userStoreManager = (org.wso2.carbon.user.core.UserStoreManager) realmService
						.getTenantUserRealm(tenantId).getUserStoreManager();
			}

		} catch (Exception e) {
			String msg = "Error retrieving the user store manager for the tenant";
			vBean.setVerified(false);
			throw new IdentityMgtServiceException(msg, e);
		}

		try {

			Map<String, String> claimsMap = new HashMap<String, String>();
			for (UserIdentityClaimDTO userIdentityClaimDTO : claims) {
				claimsMap.put(userIdentityClaimDTO.getClaimUri(),
						userIdentityClaimDTO.getClaimValue());
			}

			userStoreManager.addUser(userName, password, null, claimsMap, profileName);

			String identityRoleName = UserCoreConstants.INTERNAL_DOMAIN
					+ CarbonConstants.DOMAIN_SEPARATOR + IdentityConstants.IDENTITY_DEFAULT_ROLE;

			try {
				if (!userStoreManager.isExistingRole(identityRoleName, false)) {
					permission = new Permission("/permission/admin/login",
							UserMgtConstants.EXECUTE_ACTION);
					userStoreManager.addRole(identityRoleName, new String[] { userName },
							new Permission[] { permission }, false);
				} else {
					userStoreManager.updateUserListOfRole(identityRoleName, new String[] {},
							new String[] { userName });
				}
			} catch (org.wso2.carbon.user.api.UserStoreException e) {
				userStoreManager.deleteUser(userName);
				throw new IdentityMgtServiceException("Error occurred while adding user : "
						+ userName, e);
			}
		} catch (UserStoreException e) {
			throw new IdentityMgtServiceException("Error occurred while adding user : " + userName,
					e);
		}

		IdentityMgtConfig config = IdentityMgtConfig.getInstance();

		if (config.isListenerEnable() && config.isAuthPolicyAccountLockOnCreation()) {
			UserDTO userDTO = new UserDTO(userName);
			userDTO.setTenantId(tenantId);

			UserRecoveryDTO dto = new UserRecoveryDTO(userDTO);
			dto.setNotification(IdentityMgtConstants.Notification.ACCOUNT_CONFORM);
			dto.setNotificationType("EMAIL");

			RecoveryProcessor processor = IdentityMgtServiceComponent.getRecoveryProcessor();
			try {
				vBean = processor.updateConfirmationCode(1, userName, tenantId);

				dto.setConfirmationCode(vBean.getKey());
				NotificationDataDTO notificationDto = processor.notifyWithEmail(dto);
				vBean.setVerified(notificationDto.isNotificationSent());
				
//				Send email data only if not internally managed.
				if(! (IdentityMgtConfig.getInstance().isNotificationInternallyManaged())){ 
					vBean.setNotificationData(notificationDto);
				}					

			} catch (IdentityException e) {
				vBean.setVerified(false);
				vBean.setError("Failed to complete notification sending");
				vBean.setKey("");
			}

		} else {
			vBean.setVerified(true);
		}

		return vBean;
	}

	/**
	 * This method used to confirm the self registered user account and unlock it.
	 * 
	 * @param username
	 * @param code
	 * @param captcha
	 * @param tenantDomain
	 * @return
	 * @throws IdentityMgtServiceException
	 */
	public VerificationBean confirmUserSelfRegistration(String username, String code,
			CaptchaInfoBean captcha, String tenantDomain) throws IdentityMgtServiceException {

		VerificationBean bean = new VerificationBean();

		if (log.isDebugEnabled()) {
			log.debug("User registration verification request received with username :" + username);
		}
		if (IdentityMgtConfig.getInstance().isCaptchaVerificationInternallyManaged()) {
			try {
				CaptchaUtil.processCaptchaInfoBean(captcha);
			} catch (Exception e) {
				if (log.isDebugEnabled()) {
					log.debug(e.getMessage());
				}
				bean.setError(VerificationBean.ERROR_CODE_INVALID_CAPTCHA);
				bean.setVerified(false);
				return bean;
			}
		}

		String loggedInTenant = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
		if(tenantDomain != null && !tenantDomain.isEmpty() && !loggedInTenant.equals(tenantDomain)){
			String msg = "Trying to confirm users in unauthorized tenant space";
			log.error(msg);
			throw new IdentityMgtServiceException(msg);
		}
		if (tenantDomain == null || tenantDomain.isEmpty()) {
			tenantDomain = loggedInTenant;
		}

		try {
			Utils.processUserId(username + "@" + tenantDomain);
			
		} catch (IdentityException e) {
			if (log.isDebugEnabled()) {
				log.debug(e.getMessage());
			}
			bean.setError(VerificationBean.ERROR_CODE_INVALID_USER);
			bean.setVerified(false);
			return bean;
		}

		RecoveryProcessor processor = IdentityMgtServiceComponent.getRecoveryProcessor();

		org.wso2.carbon.user.core.UserStoreManager userStoreManager = null;

		RealmService realmService = IdentityMgtServiceComponent.getRealmService();
		int tenantId;

		try {

			tenantId = Utils.getTenantId(tenantDomain);
			if (realmService.getTenantUserRealm(tenantId) != null) {
				userStoreManager = (org.wso2.carbon.user.core.UserStoreManager) realmService
						.getTenantUserRealm(tenantId).getUserStoreManager();
			}

		} catch (Exception e) {
			String msg = "Error retrieving the user store manager for the tenant";
			bean.setVerified(false);
			throw new IdentityMgtServiceException(msg, e);
		}

		try {
			bean = processor.verifyConfirmationCode(1, username , code);
			if (bean.isVerified()) {
				UserIdentityManagementUtil.unlockUserAccount(username, userStoreManager);
				bean.setVerified(true);

			} else {
				bean.setVerified(false);
				bean.setKey("");
				log.error("User verification failed against the given confirmation code");
			}
		} catch (IdentityException e) {
			bean.setError("Error while validating confirmation code");
			if (log.isDebugEnabled()) {
				log.debug(e.getMessage());
			}
			throw new IdentityMgtServiceException("Error while validating given confirmation code");
		}

		return bean;
	}
    
}
