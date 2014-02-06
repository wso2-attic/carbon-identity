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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.mgt.constants.IdentityMgtConstants;
import org.wso2.carbon.identity.mgt.mail.DefaultEmailSendingModule;
import org.wso2.carbon.identity.mgt.password.DefaultPasswordGenerator;
import org.wso2.carbon.identity.mgt.password.RandomPasswordGenerator;
import org.wso2.carbon.identity.mgt.policy.PolicyEnforcer;
import org.wso2.carbon.identity.mgt.policy.PolicyRegistry;
import org.wso2.carbon.identity.mgt.store.RegistryRecoveryDataStore;
import org.wso2.carbon.identity.mgt.store.UserIdentityDataStore;
import org.wso2.carbon.identity.mgt.store.UserRecoveryDataStore;
import org.wso2.carbon.identity.mgt.store.UserStoreBasedIdentityDataStore;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.core.jdbc.JDBCRealmConstants;
import org.wso2.carbon.utils.CarbonUtils;

/**
 * encapsulates recovery config data
 */
public class IdentityMgtConfig {

    private static IdentityMgtConfig identityMgtConfig;

    private boolean listenerEnable;
    
    private int noOfUserChallenges;
    
    private boolean notificationInternallyManaged;

    private boolean captchaVerificationInternallyManaged;

    private String challengeQuestionSeparator;

    private int authPolicyMaxLoginAttempts;

    private int temporaryPasswordExpireTime;

    private String temporaryDefaultPassword;

    private boolean enableTemporaryPassword;

    private boolean enableAuthPolicy;

    private boolean authPolicyOneTimePasswordCheck;

    private boolean authPolicyExpirePasswordCheck;

    private int authPolicyLockingTime;

    private int authPolicyPasswordExpireTime;

    private int notificationExpireTime;

    private boolean authPolicyAccountLockCheck;

    private boolean authPolicyAccountExistCheck;

    private boolean authPolicyAccountLockOnFailure;

    private boolean authPolicyAccountLockOnCreation;

    private boolean enableUserAccountVerification;

    private boolean userAccountVerificationByUser;

    private boolean temporaryPasswordOneTime;

    private String userAccountVerificationRole;

    private boolean notificationSending;

    private String digsestFunction;

    private RandomPasswordGenerator passwordGenerator;

    private UserIdentityDataStore identityDataStore;

    private UserRecoveryDataStore recoveryDataStore;

    private List<NotificationSendingModule> sendingModules =
                                                    new ArrayList<NotificationSendingModule>();

    private List<String> notificationTypes = new ArrayList<String>();

	private String recoveryClaim;

    private PolicyRegistry policyRegistry = new PolicyRegistry();
    
	/*
	 * Define the pattern of the configuration file. Assume following
	 * pattern in config.
	 * Eg. Password.policy.extensions.1.min.length=6
	 */
	private Pattern propertyPattern = Pattern.compile("(\\.\\d\\.)");

    private static final Log log = LogFactory.getLog(IdentityMgtConfig.class);

    public IdentityMgtConfig(RealmConfiguration configuration) {

        Properties properties = new Properties();
        InputStream inStream = null;

        File pipConfigXml = new File(CarbonUtils.getCarbonSecurityConfigDirPath(),
                                            IdentityMgtConstants.PropertyConfig.CONFIG_FILE_NAME);
        if (pipConfigXml.exists()) {
            try {
                inStream = new FileInputStream(pipConfigXml);
                properties.load(inStream);
            } catch (FileNotFoundException e) {
                log.error("Can not load identity-mgt properties file ", e);
            } catch (IOException e) {
                log.error("Can not load identity-mgt properties file ", e);
            } finally {
                if(inStream != null){
                    try {
                        inStream.close();
                    } catch (IOException e) {
                        log.error("Error while closing stream ", e);
                    }
                }
            }
        }

        try {
            String notificationInternallyManaged = properties.
                        getProperty(IdentityMgtConstants.PropertyConfig.NOTIFICATION_SEND_INTERNALLY);
            if(notificationInternallyManaged != null){
                this.notificationInternallyManaged = Boolean.
                                            parseBoolean(notificationInternallyManaged.trim());
            }

            String listenerEnable = properties.
                        getProperty(IdentityMgtConstants.PropertyConfig.IDENTITY_LISTENER_ENABLE);
            if(listenerEnable != null){
                this.listenerEnable = Boolean.parseBoolean(listenerEnable.trim());
            }

            String  notificationSending = properties.
                    getProperty(IdentityMgtConstants.PropertyConfig.NOTIFICATION_SEND_ENABLE);
            if(notificationSending != null){
                this.notificationSending = Boolean.parseBoolean(notificationSending.trim());
            }
            
            String  recoveryClaim = properties.
                    getProperty(IdentityMgtConstants.PropertyConfig.RECOVERY_CLAIM);
            if(recoveryClaim != null){
                this.recoveryClaim = recoveryClaim.trim();
            }


            String captchaVerificationInternallyManaged = properties.
                        getProperty(IdentityMgtConstants.PropertyConfig.CAPTCHA_VERIFICATION_INTERNALLY);
            if(captchaVerificationInternallyManaged != null){
                this.captchaVerificationInternallyManaged = Boolean.
                                            parseBoolean(captchaVerificationInternallyManaged.trim());
            }

            String enableUserAccountVerification = properties.
                    getProperty(IdentityMgtConstants.PropertyConfig.ACCOUNT_VERIFICATION_ENABLE);
            if(enableUserAccountVerification != null){
                this.enableUserAccountVerification = Boolean.parseBoolean(enableUserAccountVerification.trim());
            }

            String userAccountVerificationRole = properties.
                    getProperty(IdentityMgtConstants.PropertyConfig.ACCOUNT_VERIFICATION_ROLE);
            if(userAccountVerificationRole != null && userAccountVerificationRole.trim().length() > 0){
                this.userAccountVerificationRole = userAccountVerificationRole;
            } else {
                this.userAccountVerificationByUser = true;
            }

            String allowTemporaryPasswordProperty = properties.
                    getProperty(IdentityMgtConstants.PropertyConfig.TEMPORARY_PASSWORD_ENABLE);
            if(allowTemporaryPasswordProperty != null){
                this.enableTemporaryPassword = Boolean.parseBoolean(allowTemporaryPasswordProperty.trim());
            }

            String temporaryPasswordExpireTime = properties.
                    getProperty(IdentityMgtConstants.PropertyConfig.TEMPORARY_PASSWORD_EXPIRE_TIME);
            if(temporaryPasswordExpireTime != null){
                this.temporaryPasswordExpireTime = Integer.parseInt(temporaryPasswordExpireTime.trim());
            }

            String defaultPasswordProperty = properties.
                    getProperty(IdentityMgtConstants.PropertyConfig.TEMPORARY_PASSWORD_DEFAULT);
            if(defaultPasswordProperty != null){
                this.temporaryDefaultPassword = defaultPasswordProperty.trim();
            }

            String temporaryPasswordOneTime = properties.
                    getProperty(IdentityMgtConstants.PropertyConfig.TEMPORARY_PASSWORD_ONETIME);
            if(temporaryPasswordOneTime != null){
                this.temporaryPasswordOneTime = Boolean.parseBoolean(temporaryPasswordOneTime.trim());
            }

            String enableAuthPolicy = properties.
                    getProperty(IdentityMgtConstants.PropertyConfig.AUTH_POLICY_ENABLE);
            if(enableAuthPolicy != null){
                this.enableAuthPolicy = Boolean.parseBoolean(enableAuthPolicy.trim());
            }

            String oneTimePasswordCheck = properties.
                    getProperty(IdentityMgtConstants.PropertyConfig.AUTH_POLICY_PASSWORD_ONE_TIME);
            if(oneTimePasswordCheck != null){
                this.authPolicyOneTimePasswordCheck = Boolean.parseBoolean(oneTimePasswordCheck.trim());
            }

            String  maxLoginAttemptProperty = properties.
                    getProperty(IdentityMgtConstants.PropertyConfig.AUTH_POLICY_ACCOUNT_LOCKING_FAIL_ATTEMPTS);
            if(maxLoginAttemptProperty != null){
                this.authPolicyMaxLoginAttempts = Integer.valueOf(maxLoginAttemptProperty.trim());
            }

            if(this.authPolicyMaxLoginAttempts == 0){
                // default value is set
                this.authPolicyMaxLoginAttempts = 10;
            }

            String expirePasswordCheck = properties.
                    getProperty(IdentityMgtConstants.PropertyConfig.AUTH_POLICY_PASSWORD_EXPIRE);
            if(expirePasswordCheck != null){
                this.authPolicyExpirePasswordCheck = Boolean.parseBoolean(expirePasswordCheck.trim());
            }

            String authPolicyLockingTime = properties.
                    getProperty(IdentityMgtConstants.PropertyConfig.AUTH_POLICY_ACCOUNT_LOCKING_TIME);
            if(authPolicyLockingTime != null){
                this.authPolicyLockingTime = Integer.parseInt(authPolicyLockingTime.trim());
            }

            String authPolicyPasswordExpireTime = properties.
                    getProperty(IdentityMgtConstants.PropertyConfig.AUTH_POLICY_PASSWORD_EXPIRE_TIME);
            if(authPolicyPasswordExpireTime != null){
                this.authPolicyPasswordExpireTime = Integer.parseInt(authPolicyPasswordExpireTime.trim());
            }

            String notificationExpireTime = properties.
                    getProperty(IdentityMgtConstants.PropertyConfig.NOTIFICATION_LINK_EXPIRE_TIME);
            if(notificationExpireTime != null){
                this.notificationExpireTime = Integer.parseInt(notificationExpireTime.trim());
            }            

            String  authPolicyAccountLockCheck = properties.
                    getProperty(IdentityMgtConstants.PropertyConfig.AUTH_POLICY_ACCOUNT_LOCK);
            if(authPolicyAccountLockCheck != null){
                this.authPolicyAccountLockCheck = Boolean.parseBoolean(authPolicyAccountLockCheck.trim());
            }

            String  authPolicyAccountExistCheck = properties.
                    getProperty(IdentityMgtConstants.PropertyConfig.AUTH_POLICY_ACCOUNT_EXIST);
            if(authPolicyAccountExistCheck != null){
                this.authPolicyAccountExistCheck = Boolean.parseBoolean(authPolicyAccountExistCheck.trim());
            }

            String  authPolicyAccountLockOnFailure = properties.
                    getProperty(IdentityMgtConstants.PropertyConfig.AUTH_POLICY_LOCK_ON_FAILURE);
            if(authPolicyAccountLockOnFailure != null){
                this.authPolicyAccountLockOnFailure = Boolean.parseBoolean(authPolicyAccountLockOnFailure.trim());
            }

            String  authPolicyAccountLockOnCreation = properties.
                    getProperty(IdentityMgtConstants.PropertyConfig.AUTH_POLICY_ACCOUNT_LOCK_ON_CREATION);
            if(authPolicyAccountLockOnCreation != null){
                this.authPolicyAccountLockOnCreation = Boolean.parseBoolean(authPolicyAccountLockOnCreation.trim());
            }

            String digsestFunction = configuration.getUserStoreProperties().get(JDBCRealmConstants.DIGEST_FUNCTION);
            if(digsestFunction != null && digsestFunction.trim().length() > 0){
                this.digsestFunction = digsestFunction;
            }

            String challengeQuestionSeparator = properties.
                        getProperty(IdentityMgtConstants.PropertyConfig.CHALLENGE_QUESTION_SEPARATOR);
            if(challengeQuestionSeparator != null && challengeQuestionSeparator.trim().length() == 1){
                this.challengeQuestionSeparator = challengeQuestionSeparator.trim();
            } else {
                this.challengeQuestionSeparator = IdentityMgtConstants.LINE_SEPARATOR;
            }

            String  passwordGenerator = properties.
                    getProperty(IdentityMgtConstants.PropertyConfig.EXTENSION_PASSWORD_GENERATOR);
            if(passwordGenerator != null && passwordGenerator.trim().length() > 0){
                try{
                    Class clazz = Thread.currentThread().getContextClassLoader().loadClass(passwordGenerator);
                    this.passwordGenerator = (RandomPasswordGenerator) clazz.newInstance();
                } catch (Exception e) {
                    log.error("Error while loading random password generator class. " +
                            "Default random password generator would be used",e);
                }
            }

            String dataPersistModule = properties.
                        getProperty(IdentityMgtConstants.PropertyConfig.EXTENSION_USER_DATA_STORE);
            if(dataPersistModule != null && dataPersistModule.trim().length() > 0){
                try{
                    Class clazz = Thread.currentThread().getContextClassLoader().loadClass(dataPersistModule);
                    this.identityDataStore = (UserIdentityDataStore) clazz.newInstance();
                } catch (Exception e) {
                    log.error("Error while loading user identity data persist class. " + dataPersistModule +
                            " Default module would be used",e);
                }
            }

            String recoveryPersistModule = properties.
                    getProperty(IdentityMgtConstants.PropertyConfig.EXTENSION_USER_RECOVERY_DATA_STORE);
            if(dataPersistModule != null && dataPersistModule.trim().length() > 0){
                try{
                    Class clazz = Thread.currentThread().getContextClassLoader().loadClass(recoveryPersistModule);
                    this.recoveryDataStore = (UserRecoveryDataStore) clazz.newInstance();
                } catch (Exception e) {
                    log.error("Error while loading user recovery data persist class. " + dataPersistModule +
                            " Default module would be used",e);
                }
            }

            int i = 1;
            while(true){
                String module = properties.
                    getProperty(IdentityMgtConstants.PropertyConfig.EXTENSION_NOTIFICATION_SENDING_MODULE + "." + i);
                if(module == null){
                    break;
                }
                if(module != null && module.trim().length() > 0){
                    try{
                        Class clazz = Thread.currentThread().getContextClassLoader().loadClass(module);
                        NotificationSendingModule sendingModule = (NotificationSendingModule) clazz.newInstance();
                        String type = sendingModule.getNotificationType();
                        if(type == null || type.trim().length() == 0){
                            log.error("Notification type can not be null. Module " + module +  "  is not loaded.");
                        } else {
                            if(notificationTypes.contains(type)){
                                log.error("Same Notification type can not be supported by more than " +
                                        "one module. Module " + module +  "  is not loaded.");
                            } else {
                                notificationTypes.add(type);
                                sendingModule.init();
                                sendingModules.add(sendingModule);
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error while loading notification sending class  " + module ,e);
                    }
                }
                i++;
            }

            // Load the configuration for Password.policy.extensions.
            loadPolicyExtensions(properties, IdentityMgtConstants.PropertyConfig.PASSWORD_POLICY_EXTENSIONS);
            
            if(this.passwordGenerator == null){
                this.passwordGenerator = new DefaultPasswordGenerator();
            }

            if(this.identityDataStore == null){
                this.identityDataStore = new UserStoreBasedIdentityDataStore();
            }

            if(this.recoveryDataStore == null){
                this.recoveryDataStore = new RegistryRecoveryDataStore();
            }

            if(this.sendingModules.isEmpty()){
                NotificationSendingModule module = new DefaultEmailSendingModule();
                module.init();
                this.sendingModules.add(module);
                this.notificationTypes.add(module.getNotificationType());
            }
            
        } catch (Exception e) {
            log.error("Error while loading identity mgt configurations", e);    
        }
    }

    /**
     * Gets instance
     *
     * As this is only called in start up syn and null check is not needed
     *
     * @param configuration a primary <code>RealmConfiguration</code>
     * @return <code>IdentityMgtConfig</code>
     */
    public static IdentityMgtConfig getInstance(RealmConfiguration configuration){

        identityMgtConfig = new IdentityMgtConfig(configuration);
        return identityMgtConfig;
    }

    public static IdentityMgtConfig getInstance(){
        return identityMgtConfig;
    }

    public int getNoOfUserChallenges() {
        return noOfUserChallenges;
    }

    public boolean isNotificationInternallyManaged() {
        return notificationInternallyManaged;
    }

    public boolean isCaptchaVerificationInternallyManaged() {
        return captchaVerificationInternallyManaged;
    }

    public boolean isEnableUserAccountVerification() {
        return enableUserAccountVerification;
    }


    public int getAuthPolicyMaxLoginAttempts() {
        return authPolicyMaxLoginAttempts;
    }

    public int getTemporaryPasswordExpireTime() {
        return temporaryPasswordExpireTime;
    }

    public String getTemporaryDefaultPassword() {
        return temporaryDefaultPassword;
    }

    public boolean isEnableTemporaryPassword() {
        return enableTemporaryPassword;
    }

    public boolean isEnableAuthPolicy() {
        return enableAuthPolicy;
    }

    public boolean isAuthPolicyExpirePasswordCheck() {
        return authPolicyExpirePasswordCheck;
    }

    public boolean isAuthPolicyOneTimePasswordCheck() {
        return authPolicyOneTimePasswordCheck;
    }

    public int getAuthPolicyLockingTime() {
        return authPolicyLockingTime;
    }

    public boolean isAuthPolicyAccountLockCheck() {
        return authPolicyAccountLockCheck;
    }

    public boolean isUserAccountVerificationByUser() {
        return userAccountVerificationByUser;
    }

    public boolean isTemporaryPasswordOneTime() {
        return temporaryPasswordOneTime;
    }

    public String getUserAccountVerificationRole() {
        return userAccountVerificationRole;
    }

    public String getChallengeQuestionSeparator() {
        return challengeQuestionSeparator;
    }

    public String getDigsestFunction() {
        return digsestFunction;
    }

    public RandomPasswordGenerator getPasswordGenerator() {
        return passwordGenerator;
    }

    public UserIdentityDataStore getIdentityDataStore() {
        return identityDataStore;
    }

    public boolean isNotificationSending() {
        return notificationSending;
    }

    public boolean isAuthPolicyAccountExistCheck() {
        return authPolicyAccountExistCheck;
    }

    public boolean isAuthPolicyAccountLockOnFailure() {
        return authPolicyAccountLockOnFailure;
    }

    public int getAuthPolicyPasswordExpireTime() {
        return authPolicyPasswordExpireTime;
    }

    public boolean isAuthPolicyAccountLockOnCreation() {
        return authPolicyAccountLockOnCreation;
    }

    public int getNotificationExpireTime() {
        return notificationExpireTime;
    }

    public boolean isListenerEnable() {
        return listenerEnable;
    }

    public List<NotificationSendingModule> getNotificationSendingModules() {
        return sendingModules;
    }

    public String getAccountRecoveryClaim() {
	    return recoveryClaim;
    }

    public List<String> getNotificationTypes() {
        return notificationTypes;
    }

    public UserRecoveryDataStore getRecoveryDataStore() {
        return recoveryDataStore;
    }

    public PolicyRegistry getPolicyRegistry() {
        return policyRegistry;
    }
    
    /**
     * This method is used to load the policies declared in the configuration.
     * 
     * @param properties Loaded properties
     * @param extensionType Type of extension
     */
    private void loadPolicyExtensions(Properties properties, String extensionType){
    	
    	// First property must start with 1.
    	int count = 1;
    	String className = null;
    	
		while((className = properties.getProperty(extensionType + "." + count)) != null){
            try {
                Class clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
                
                PolicyEnforcer policy = (PolicyEnforcer) clazz.newInstance();
                policy.init(getParameters(properties, extensionType, count));
                
                this.policyRegistry.addPolicy((PolicyEnforcer) policy);
                count++;
                
            } catch (ClassNotFoundException e) {
                log.error("Error while loading password policies "+ className + " " + e.getMessage());
            } catch (SecurityException e) {
            	log.error("Error while loading password policies "+ className + " " + e.getMessage());
            } catch (InstantiationException e) {
            	log.error("Error while loading password policies "+ className + " " + e.getMessage());
            } catch (IllegalAccessException e) {
            	log.error("Error while loading password policies "+ className + " " + e.getMessage());
            }
		}
    	
    }
    
	/**
	 * This utility method is used to get the parameters from the configuration
	 * file for a given policy extension.
	 * 
	 * @param prop
	 *            - properties
	 * @param extensionKey
	 *            - extension key which is defined in the
	 *            IdentityMgtConstants
	 * @param sequence
	 *            - property sequence number in the file
	 * @return Map of parameters with key and value from the configuration file.
	 */
	private Map<String, String> getParameters(Properties prop, String extensionKey, int sequence) {

		Set<String> keys = prop.stringPropertyNames();

		Map<String, String> keyValues = new HashMap<String, String>();

		for (String key : keys) {
			// Get only the provided extensions. 
			// Eg.
			// Password.policy.extensions.1
			if (key.contains(extensionKey + "." + String.valueOf(sequence))) {

				Matcher m = propertyPattern.matcher(key);

				// Find the .1. pattern in the property key.
				if (m.find()) {
					int searchIndex = m.end();

					/*
					 * Key length is > matched pattern's end index if it has
					 * parameters
					 * in the config file.
					 */
					if (key.length() > searchIndex) {
						String propKey = key.substring(searchIndex);
						String propValue = prop.getProperty(key);
						keyValues.put(propKey, propValue);
					}
				}

			}
		}

		return keyValues;
	}
}
