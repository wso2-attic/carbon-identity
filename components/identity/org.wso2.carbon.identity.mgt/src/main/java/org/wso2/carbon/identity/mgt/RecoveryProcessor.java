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
package org.wso2.carbon.identity.mgt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.mgt.beans.VerificationBean;
import org.wso2.carbon.identity.mgt.config.Config;
import org.wso2.carbon.identity.mgt.config.ConfigBuilder;
import org.wso2.carbon.identity.mgt.config.ConfigType;
import org.wso2.carbon.identity.mgt.config.StorageType;
import org.wso2.carbon.identity.mgt.constants.IdentityMgtConstants;
import org.wso2.carbon.identity.mgt.dto.NotificationDataDTO;
import org.wso2.carbon.identity.mgt.dto.UserDTO;
import org.wso2.carbon.identity.mgt.dto.UserRecoveryDTO;
import org.wso2.carbon.identity.mgt.dto.UserRecoveryDataDO;
import org.wso2.carbon.identity.mgt.internal.IdentityMgtServiceComponent;
import org.wso2.carbon.identity.mgt.mail.Notification;
import org.wso2.carbon.identity.mgt.mail.NotificationBuilder;
import org.wso2.carbon.identity.mgt.mail.NotificationData;
import org.wso2.carbon.identity.mgt.store.UserIdentityDataStore;
import org.wso2.carbon.identity.mgt.store.UserRecoveryDataStore;
import org.wso2.carbon.identity.mgt.util.Utils;
import org.wso2.carbon.registry.core.utils.UUIDGenerator;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.tenant.TenantManager;

/**
 *
 *
 */
public class RecoveryProcessor {

    private static final Log log = LogFactory.getLog(RecoveryProcessor.class);

    private Map<String, NotificationSendingModule> modules =
                                                    new HashMap<String, NotificationSendingModule>();

    private NotificationSendingModule defaultModule;

    private UserRecoveryDataStore dataStore;

    private  NotificationSender notificationSender;

    private ChallengeQuestionProcessor questionProcessor;
    
    /*
     *  Delimiter that will be used to store the registry resource entries. Must be valid characters.
     *  If this changed the split regex also need to changed in getUserExternalCodeStr method.
     */
    private final String REG_DELIMITER = "___";

    public RecoveryProcessor() {

        List<NotificationSendingModule> modules =
                                IdentityMgtConfig.getInstance().getNotificationSendingModules();

        this.defaultModule = modules.get(0);

        for(NotificationSendingModule module : modules){
            this.modules.put(module.getNotificationType(), module);
        }

        this.dataStore = IdentityMgtConfig.getInstance().getRecoveryDataStore();
        this.notificationSender = new NotificationSender();

        questionProcessor = new ChallengeQuestionProcessor();

    }

    /**
     * Processing recovery
     *
     * @param recoveryDTO class that contains user and tenant Information
     * @return true if the reset request is processed successfully.
     * @throws IdentityException  if fails
     */
    public NotificationDataDTO recoverWithNotification(UserRecoveryDTO recoveryDTO) throws IdentityException {

        if(!IdentityMgtConfig.getInstance().isNotificationSending()){
            //return new NotificationDataDTO("Email sending is disabled");
        }

        String notificationAddress;
        String secretKey = null;
        String confirmationKey = null;
        NotificationSendingModule module = null;
        boolean persistData = true;
        String userId = recoveryDTO.getUserId();
        String domainName = recoveryDTO.getTenantDomain();
        int tenantId = recoveryDTO.getTenantId();
        NotificationDataDTO notificationData = new NotificationDataDTO();
        String internalCode = null;


        TenantManager tenantManager = IdentityMgtServiceComponent.getRealmService().getTenantManager();
        try {
            Tenant tenant = tenantManager.getTenant(tenantId);
            if(tenant != null) {
                domainName = tenant.getDomain();
            }

        } catch (UserStoreException e) {
            log.warn("No Tenant domain for tenant id " + tenantId);
        }

        String type = recoveryDTO.getNotificationType();
        if(type != null){
            module =  modules.get(type);
        }

        if(module == null){
            module = defaultModule;
        }

        NotificationData emailNotificationData = new NotificationData();
        String emailTemplate = null;
		
        notificationAddress = Utils.getEmailAddressForUser(userId, tenantId);
        String firstName = Utils.getClaimFromUserStoreManager(userId, tenantId, "http://wso2.org/claims/givenname");
        emailNotificationData.setTagData("first-name", firstName);
        emailNotificationData.setTagData("user-name", userId);

        if ((notificationAddress == null) || (notificationAddress.trim().length() < 0)) {
        	String msg = "Notification sending failure. Notification address is not defined for user: " + userId;
            log.error(msg);
            throw new IdentityException(msg);
        }
        emailNotificationData.setSendTo(notificationAddress);

		if(log.isDebugEnabled()) {
			log.debug("Building notification with data - First name: " + firstName + 
			          " User name: " + userId + " Send To: " + notificationAddress);
		}
		
		Config config = null;
		ConfigBuilder configBuilder = ConfigBuilder.getInstance();
		try {
			config = configBuilder.loadConfiguration(ConfigType.EMAIL, StorageType.REGISTRY, tenantId);
		} catch (Exception e1) {
			log.error(e1);
			throw new IdentityException(e1.getMessage());
		}
		
        if(recoveryDTO.getNotification() != null){
        	emailTemplate = config.getProperty(recoveryDTO.getNotification().trim());
        	
            String notification = recoveryDTO.getNotification().trim();
            notificationData.setNotification(notification);
            if(IdentityMgtConstants.Notification.PASSWORD_RESET_RECOVERY.equals(notification)){
            	internalCode = generateUserCode(2, userId);
                try {
					confirmationKey = getUserExternalCodeStr(internalCode);
				} catch (Exception e) {
					throw new IdentityException(e.getMessage());
				}
                secretKey = UUIDGenerator.generateUUID();
                emailNotificationData.setTagData("confirmation-code", confirmationKey);
                emailTemplate = config.getProperty(IdentityMgtConstants.Notification.PASSWORD_RESET_RECOVERY);
                
            } else if(IdentityMgtConstants.Notification.ACCOUNT_CONFORM.equals(notification)){
                confirmationKey = UUIDGenerator.generateUUID();
                secretKey = UUIDGenerator.generateUUID();
                emailNotificationData.setTagData("confirmation-code", confirmationKey);
                emailTemplate = config.getProperty(IdentityMgtConstants.Notification.ACCOUNT_CONFORM);
                
            } else if(IdentityMgtConstants.Notification.TEMPORARY_PASSWORD.equals(notification)){
                String temporaryPassword = recoveryDTO.getTemporaryPassword();  // TODO
                if(temporaryPassword == null || temporaryPassword.trim().length() < 1){
                    char[] chars = IdentityMgtConfig.getInstance().getPasswordGenerator().generatePassword();
                    temporaryPassword = new String(chars);
                }
                Utils.updatePassword(userId, tenantId, temporaryPassword);
                emailNotificationData.setTagData("temporary-password", temporaryPassword);
                emailTemplate = config.getProperty(IdentityMgtConstants.Notification.TEMPORARY_PASSWORD);
                persistData = false;
            } else if(IdentityMgtConstants.Notification.ACCOUNT_UNLOCK.equals(notification)){
            	emailTemplate = config.getProperty(IdentityMgtConstants.Notification.ACCOUNT_UNLOCK);
            	persistData = false;
            } else if(IdentityMgtConstants.Notification.ACCOUNT_ID_RECOVERY.equals(notification)){
            	emailTemplate = config.getProperty(IdentityMgtConstants.Notification.ACCOUNT_ID_RECOVERY);
            	persistData = false;
            } else if(IdentityMgtConstants.Notification.ASK_PASSWORD.equals(notification)){
            	emailNotificationData.setTagData("first-name", userId);
            	internalCode = generateUserCode(2, userId);
                try {
					confirmationKey = getUserExternalCodeStr(internalCode);
				} catch (Exception e) {
					throw new IdentityException(e.getMessage());
				}
                secretKey = UUIDGenerator.generateUUID();
                emailNotificationData.setTagData("confirmation-code", confirmationKey);
                emailNotificationData.setTagData("tenant-domain", domainName);
                emailTemplate = config.getProperty(IdentityMgtConstants.Notification.ASK_PASSWORD);
            }
            
            if(log.isDebugEnabled()) {
            	log.debug("Notification type: " + notification);
            }
        }

        Notification emailNotification = null;
        try {
			emailNotification = NotificationBuilder.createNotification("EMAIL", emailTemplate, emailNotificationData);
		} catch (Exception e) {
			log.error(e);
			throw new IdentityException(e.getMessage());
		}
        
        notificationData.setNotificationAddress(notificationAddress);
        notificationData.setUserId(userId);
        notificationData.setDomainName(domainName);
        notificationData.setNotificationType(recoveryDTO.getNotificationType());

        if(persistData){
            UserRecoveryDataDO recoveryDataDO =
                            new UserRecoveryDataDO(userId, tenantId,  internalCode, secretKey);
            dataStore.store(recoveryDataDO);

        }

        if(IdentityMgtConfig.getInstance().isNotificationInternallyManaged()){ 
            module.setNotificationData(notificationData);
            module.setNotification(emailNotification);
            notificationSender.sendNotification(module);
            notificationData.setNotificationSent(true);
        } else {
            notificationData.setNotificationSent(false);
        }

        return notificationData; 
    }

    /**
     * Confirm that confirmation key has been sent to the same user.
     * @param sequence TODO
     * @param username TODO
     * @param confirmationKey confirmation key from the user
     *
     * @return verification result as a bean
     */
    public VerificationBean verifyConfirmationKey(String confirmationKey) {

        UserRecoveryDataDO dataDO = null;

        try {
            dataDO = dataStore.load(confirmationKey);
        } catch (IdentityException e) {
            return new VerificationBean(VerificationBean.ERROR_CODE_INVALID_USER);
        }


        if(dataDO == null){
            return new VerificationBean(VerificationBean.ERROR_CODE_INVALID_CODE);
        }

        if(!dataDO.isValid()){
            return new VerificationBean(VerificationBean.ERROR_CODE_EXPIRED_CODE);
        } else {
        	// Verification successful.
            return new VerificationBean(true);
        }

    }
    
    /**
     * This method is used to verify the confirmation code supplied by user. This invalidates 
     * the current code and generates a new code and send to user.
     * @param sequence TODO
     * @param username TODO
     * @param code
     * @param userDto
     * 
     * @return
     * @throws IdentityException
     */
    public VerificationBean verifyConfirmationCode(int sequence, String username, String code) throws IdentityException{

        UserRecoveryDataDO dataDO = null;
        String internalCode = getUserInternalCodeStr(sequence, username, code);
        
        try {
            dataDO = dataStore.load(internalCode);
        } catch (IdentityException e) {
        	throw new IdentityException("Error loading data");
        }

        if(dataDO == null){
            throw new IdentityException("Invalid confirmation code");
        }

        if(!dataDO.isValid()){
            throw new IdentityException("Expired code");
        } else {
        	return new VerificationBean(true);
        }

    }
    
    public VerificationBean updateConfirmationCode(int sequence, String username, int tenantId) throws IdentityException {
    	
        String confirmationKey = generateUserCode(sequence, username);
        String secretKey = UUIDGenerator.generateUUID();
        
		UserRecoveryDataDO recoveryDataDO = new UserRecoveryDataDO(username,
				tenantId, confirmationKey, secretKey);

		dataStore.store(recoveryDataDO);
		String externalCode = null;
		try {
			externalCode = getUserExternalCodeStr(confirmationKey);
		} catch (Exception e) {
			throw new IdentityException(e.getMessage());
		}
		
        return new VerificationBean(username, externalCode);
    }

    /**
     * Verifies user id with underline user store
     * @param sequence TODO
     * @param userDTO  bean class that contains user and tenant Information
     *
     * @return true/false whether user is verified or not. If user is a tenant
     *         user then always return false
     */
    public VerificationBean verifyUserForRecovery(int sequence, UserDTO userDTO) {

        String userId = userDTO.getUserId();
        int tenantId = userDTO.getTenantId();
        boolean success = false;
        VerificationBean bean = null;
        try {
            UserStoreManager userStoreManager = IdentityMgtServiceComponent.getRealmService().
                    getTenantUserRealm(tenantId).getUserStoreManager();

            if(userStoreManager.isExistingUser(userId)){
                if(IdentityMgtConfig.getInstance().isAuthPolicyAccountLockCheck()){
                    String accountLock = userStoreManager.
                            getUserClaimValue(userId, UserIdentityDataStore.ACCOUNT_LOCK, null);
                    if(!Boolean.parseBoolean(accountLock)){
                        success = true;
                    }
                } else {
                    success = true;
                }
            } else {
                if(log.isDebugEnabled()){
                    log.error("User with user name : " + userId +
                            " does not exists in tenant domain : " + userDTO.getTenantDomain());
                    bean = new VerificationBean(VerificationBean.ERROR_CODE_INVALID_USER + " " + "User does not exists");
                }
            }

            if(success){
                String internalCode = generateUserCode(sequence, userId);
                String key = UUID.randomUUID().toString();
                UserRecoveryDataDO  dataDO =
                        new UserRecoveryDataDO(userId, tenantId, internalCode, key);
                dataStore.store(dataDO);
                log.info("User verification successful for user : "+ userId +
                        " from tenant domain :"+ userDTO.getTenantDomain());
                
                bean = new VerificationBean(userId, getUserExternalCodeStr(internalCode));
            }
        } catch (Exception e) {
        	if(log.isDebugEnabled()) {
        		log.error(e.getMessage());
        	}
            bean =  new VerificationBean(VerificationBean.ERROR_CODE_UN_EXPECTED + " " + e.getMessage());
            
        }
        
        if(bean == null) {
        	bean = new VerificationBean(VerificationBean.ERROR_CODE_UN_EXPECTED);
        }
        return bean;
    }
    
    public void createConfirmationCode(UserDTO userDTO, String code) throws IdentityException {
        String key = UUID.randomUUID().toString();
        UserRecoveryDataDO  dataDO =
                new UserRecoveryDataDO(userDTO.getUserId(), userDTO.getTenantId(), key, code);
        dataStore.store(dataDO);
    }
    
    
    public ChallengeQuestionProcessor getQuestionProcessor() {
        return questionProcessor;
    }
    
    /*
     * TODO - Important. Refactor this method and use recoveryWithNotification instead.
     */
    public NotificationDataDTO notifyWithEmail(UserRecoveryDTO notificationBean) throws IdentityException{

    	if(!IdentityMgtConfig.getInstance().isNotificationSending()){
//          return new NotificationDataDTO("Email sending is disabled");
      }

      String notificationAddress;

      String confirmationKey = null;
      NotificationSendingModule module = null;

      String userId = notificationBean.getUserId();
      String domainName = notificationBean.getTenantDomain();
      int tenantId = notificationBean.getTenantId();
      confirmationKey = notificationBean.getConfirmationCode();
      
      NotificationDataDTO notificationData = new NotificationDataDTO();

      
      String type = notificationBean.getNotificationType();
      if(type != null){
          module =  modules.get(type);
      }

      if(module == null){
          module = defaultModule;
      }
      
      NotificationData emailNotificationData = new NotificationData();
      String emailTemplate = null;
      notificationAddress = module.getNotificationAddress(userId, tenantId);

      if ((notificationAddress == null) || (notificationAddress.trim().length() < 0)) {
          log.warn("Notification sending failure. Notification address is not defined for user " + userId);
      }
      
      String firstName = Utils.getClaimFromUserStoreManager(userId, tenantId, "http://wso2.org/claims/givenname");
      emailNotificationData.setTagData("first-name", firstName);
      emailNotificationData.setTagData("user-name", userId);
      
      emailNotificationData.setSendTo(notificationAddress);
      
		Config config = null;
		ConfigBuilder configBuilder = ConfigBuilder.getInstance();
		try {
			config = configBuilder.loadConfiguration(ConfigType.EMAIL,
					StorageType.REGISTRY, tenantId);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

      if(notificationBean.getNotification() != null){
    	  emailTemplate = config.getProperty(notificationBean.getNotification().trim());
          String notification = notificationBean.getNotification().trim();
          notificationData.setNotification(notification);
          if(IdentityMgtConstants.Notification.PASSWORD_RESET_RECOVERY.equals(notification)){
              notificationData.setNotificationCode(confirmationKey);
              emailNotificationData.setTagData("confirmation-code", confirmationKey);
              emailTemplate = config.getProperty(IdentityMgtConstants.Notification.PASSWORD_RESET_RECOVERY);
              
          } else if(IdentityMgtConstants.Notification.ACCOUNT_CONFORM.equals(notification)){
              notificationData.setNotificationCode(confirmationKey);
              emailNotificationData.setTagData("confirmation-code", confirmationKey);
              emailNotificationData.setTagData("tenant-domain", domainName);
              emailTemplate = config.getProperty(IdentityMgtConstants.Notification.ACCOUNT_CONFORM);
              
          } else if(IdentityMgtConstants.Notification.TEMPORARY_PASSWORD.equals(notification)){
              String temporaryPassword = notificationBean.getTemporaryPassword();  // TODO
              notificationData.setNotificationCode(temporaryPassword);
              emailNotificationData.setTagData("temporary-password", temporaryPassword);
              emailTemplate = config.getProperty(IdentityMgtConstants.Notification.TEMPORARY_PASSWORD);

          } else if(IdentityMgtConstants.Notification.ACCOUNT_UNLOCK.equals(notification)){
          	emailTemplate = config.getProperty(IdentityMgtConstants.Notification.ACCOUNT_UNLOCK);
          	notificationData.setNotificationCode(userId);

          } else if(IdentityMgtConstants.Notification.ACCOUNT_ID_RECOVERY.equals(notification)){
          	emailTemplate = config.getProperty(IdentityMgtConstants.Notification.ACCOUNT_ID_RECOVERY);
          	notificationData.setNotificationCode(userId);
          	
          } else if(IdentityMgtConstants.Notification.ASK_PASSWORD.equals(notification)){          	
              notificationData.setNotificationCode(confirmationKey);
              emailNotificationData.setTagData("confirmation-code", confirmationKey);
              emailNotificationData.setTagData("tenant-domain", domainName);

              emailTemplate = config.getProperty(IdentityMgtConstants.Notification.ASK_PASSWORD);
              
          } 
      }
      
      Notification emailNotification = null;
      try {
			emailNotification = NotificationBuilder.createNotification("EMAIL", emailTemplate, emailNotificationData);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

      notificationData.setNotificationAddress(notificationAddress);
      notificationData.setUserId(userId);
      notificationData.setDomainName(domainName);
      notificationData.setNotificationType(notificationBean.getNotificationType());

      if(IdentityMgtConfig.getInstance().isNotificationInternallyManaged()){ 
          module.setNotificationData(notificationData);
          module.setNotification(emailNotification);
          notificationSender.sendNotification(module);
          notificationData.setNotificationSent(true);
      } else {
          notificationData.setNotificationSent(false);
      }

      return notificationData; 
    }
    
    /**
     * Generates the code specific to user and operations sequence value.
     * @param sequence
     * @param username
     * @return
     */
    private String generateUserCode(int sequence, String username) {
    	
    	String genCode = null;
    	
    	if(username != null) {
    		
    		StringBuilder userCode = new StringBuilder();
    		userCode.append(sequence);
    		userCode.append(REG_DELIMITER);
    		userCode.append(stripSpecialChars(username));
    		userCode.append(REG_DELIMITER);
    		userCode.append(UUID.randomUUID().toString());
    		
    		genCode = userCode.toString();
    	}
    	
    	return genCode;
    }
    

    /**
     * Creates the user specific code  by the given sequence, username and code to be search in
     * the datastore.
     * 
     * @param sequence
     * @param username
     * @param code - user provided code
     * @return
     */
    private String getUserInternalCodeStr(int sequence, String username, String code) {
    	
    	String searchCode = null;
    	
    	if(username != null && code != null) {
    		
    		StringBuilder userCode = new StringBuilder();
    		userCode.append(sequence);
    		userCode.append(REG_DELIMITER);
    		userCode.append(stripSpecialChars(username));
    		userCode.append(REG_DELIMITER);
    		userCode.append(code);
    		
    		searchCode = userCode.toString();
    	}
    	
    	return searchCode;
    }
    
    /**
     * 
     * @param internalCode - code with the format "sequence_username_usercode".
     * @return
     */
    private String getUserExternalCodeStr(String internalCode) throws Exception{
    	
    	String userCode = null;
    	
    	if(internalCode != null) {
    		String[] codeParts = internalCode.split("_{3}", 3);
    		// Must have 3 elements and 3rd one must have code.
    		if(codeParts.length == 3) {
    			userCode = codeParts[2];
    		} else {
    			throw new Exception("Invalid code");
    		}
    	} else {
    		throw new Exception("Code not found");
    	}
    	
    	return userCode;
    }
    
    /**
     * This removes the special chars which the registry resource does not accepts.
     * 
     * @param input
     * @return
     */
    private String stripSpecialChars(String input) {
    	
    	StringBuilder output = new StringBuilder();
    	
    	if(input != null) {
    		
    		char[] inputArr = input.toCharArray();
    		
    		for (char c : inputArr) {
				
    			switch(c){
    			
    			case '~':
    			case '!':
    			case '@':
    			case '#':
    			case ';':
    			case '%':
    			case '^':
    			case '*':
    			case '(':
    			case ')':
    			case '+':
    			case '=':
    			case '{':
    			case '}':
    			case '|':
    			case '\\':
    			case '<':
    			case '>':
    			case '"':
    			case '\'':
    			case ',':
    			case '$':
    				output.append('z');
    				break;
    			default:
    				output.append(c);
    			}
			}
    	}
    	
    	return output.toString();
    }
}
