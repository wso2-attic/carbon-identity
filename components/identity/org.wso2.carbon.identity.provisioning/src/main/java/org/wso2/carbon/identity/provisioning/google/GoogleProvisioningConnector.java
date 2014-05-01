/*
 *  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.identity.provisioning.google;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.provisioning.AbstractIdentityProvisioningConnector;
import org.wso2.carbon.identity.provisioning.IdentityProvisioningException;
import org.wso2.carbon.user.api.Claim;
import org.wso2.carbon.user.api.Permission;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;
import com.google.api.services.admin.directory.model.User;
import com.google.api.services.admin.directory.model.UserName;
import com.google.api.services.admin.directory.model.Users;

public class GoogleProvisioningConnector extends
		AbstractIdentityProvisioningConnector {

	private static final Log log = LogFactory
			.getLog(GoogleProvisioningConnector.class);
	
	private GoogleProvisioningConnectorConfig configBuilder;

	/**
	 * 
	 */
	public GoogleProvisioningConnector(String name, boolean isEnabled,
			Properties configs) {
		super(name, isEnabled, configs);
		this.configBuilder = new GoogleProvisioningConnectorConfig(configs);
	}

	/**
	 * Build and returns a Directory service object authorized with the service accounts
	 * that act on behalf of the given user.
	 *
	 * @param userEmail The email of the user.
	 * @return Directory service object that is ready to make requests.
	 * @throws IdentityProvisioningException 
	 */
	private Directory getDirectoryService() throws IdentityProvisioningException {
		if (log.isTraceEnabled()) {
			log.trace("Starting authenticate() of "
					+ GoogleProvisioningConnector.class);
		}
		boolean isDebugEnabled = log.isDebugEnabled();

    	/** Email of the Service Account */
		String serviceAccountId = this.configBuilder.getValue(GoogleConnectorConstants.PropertyConfig.SERVICE_ACCOUNT_EMAIL);
    	/** Admin email */
		String serviceAccountUser = this.configBuilder.getValue(GoogleConnectorConstants.PropertyConfig.ADMIN_EMAIL);
    	/** Path to the Service Account's Private Key file */
		String serviceAccountPrivateKeyPath = this.configBuilder.getValue(GoogleConnectorConstants.PropertyConfig.SERVICE_ACCOUNT_PKCS12_FILE_PATH);
		File serviceAccountPrivateKeyFile = new java.io.File(serviceAccountPrivateKeyPath);
		
    	/** Application name */
		String applicationName = this.configBuilder.getValue(GoogleConnectorConstants.PropertyConfig.APPLICATION_NAME);
		
		
		HttpTransport httpTransport = new NetHttpTransport();
		JacksonFactory jsonFactory = new JacksonFactory();

		Directory service = null;
		try {

			GoogleCredential credential = new GoogleCredential.Builder()
					.setTransport(httpTransport)
					.setJsonFactory(jsonFactory)
					.setServiceAccountId(serviceAccountId)
					.setServiceAccountScopes(Arrays.asList(DirectoryScopes.ADMIN_DIRECTORY_USER))
					.setServiceAccountUser(serviceAccountUser)
					.setServiceAccountPrivateKeyFromP12File(serviceAccountPrivateKeyFile)
					.build();
			

			service = new Directory.Builder(httpTransport, jsonFactory,
					credential).setHttpRequestInitializer(credential)
					.setApplicationName(applicationName).build();
		} catch (GeneralSecurityException e) {
			throw new IdentityProvisioningException(e);
		} catch (IOException e) {
			throw new IdentityProvisioningException(e);
		}

		if (log.isTraceEnabled()) {
			log.trace("Ending authentication() of "
					+ GoogleProvisioningConnector.class);
		}
		return service;
	}

	/**
	 * Provision the new user to Google.
	 * @throws UserStoreException 
	 */
	public String createUser(String userName, Object credential,
			String[] roleList, Map<String, String> claims, String profile,
			UserStoreManager userStoreManager)
			throws UserStoreException {

		if (log.isTraceEnabled()) {
			log.trace("Starting createUser() of "
					+ GoogleProvisioningConnector.class);
		}
		boolean isDebugEnabled = log.isDebugEnabled();

		

		String googleRoleName = "Google";
		
		// Check whether google role assigned to the user
		boolean isUserHavingGoogleRole = false;
		
		if(roleList != null) {
			for(String role : roleList) {
				if(role != null && role.equals(googleRoleName)) {
					isUserHavingGoogleRole = true;
					break;
				}
			}
		}		
		

		User createdUser = null;
		// Provision is user have google role
		if(isUserHavingGoogleRole) {
			try {
				User newUser = new User();
				// Mandetory properties : 4
				// familyName, givenName, password, primaryEmail
	
				Map<String, String> requiredAttributes = getRequiredAttributes(userName, credential,
							roleList, claims, profile, userStoreManager);
				
	
				UserName username = new UserName();
				if(requiredAttributes.containsKey(GoogleConnectorConstants.ATTRIBUTE_FAMILYNAME)) {
					username.setFamilyName(requiredAttributes.get(GoogleConnectorConstants.ATTRIBUTE_FAMILYNAME));
				}
				else {
					throw new IdentityProvisioningException("Required parameter "+GoogleConnectorConstants.ATTRIBUTE_FAMILYNAME+" is empty");
				}
				
				if(requiredAttributes.containsKey(GoogleConnectorConstants.ATTRIBUTE_GIVENNAME)) {
					username.setGivenName(requiredAttributes.get(GoogleConnectorConstants.ATTRIBUTE_GIVENNAME));
				}
				else {
					throw new IdentityProvisioningException("Required parameter "+GoogleConnectorConstants.ATTRIBUTE_GIVENNAME+" is empty");
				}
				newUser.setName(username);
	
				if(requiredAttributes.containsKey(GoogleConnectorConstants.ATTRIBUTE_PASSWORD)) {
					newUser.setPassword(requiredAttributes.get(GoogleConnectorConstants.ATTRIBUTE_PASSWORD));
				}
				else {
					throw new IdentityProvisioningException("Required parameter "+GoogleConnectorConstants.ATTRIBUTE_PASSWORD+" is empty");
				}			
	
				if(requiredAttributes.containsKey(GoogleConnectorConstants.ATTRIBUTE_PRIMARYEMAIL)) {
					newUser.setPrimaryEmail(requiredAttributes.get(GoogleConnectorConstants.ATTRIBUTE_PRIMARYEMAIL));
				}
				else {
					throw new IdentityProvisioningException("Required parameter "+GoogleConnectorConstants.ATTRIBUTE_PRIMARYEMAIL+" is empty");
				}	
				
				Directory.Users.Insert request = getDirectoryService().users().insert(newUser);			
				createdUser = request.execute();
				
				try {
					userStoreManager.setUserClaimValue(userName, configBuilder.getUserIdClaim(), createdUser.getPrimaryEmail(), null);
				} catch (org.wso2.carbon.user.api.UserStoreException e) {
					log.error("Required claim value for user id is not stored in user store");
					throw new IdentityProvisioningException("Required claim value for user id is not stored in user store", e);
				}
				
			} catch (GoogleJsonResponseException e) {
				throw new IdentityProvisioningException("Error while creating user : " + userName, e);
			} catch (IOException e) {
				throw new IdentityProvisioningException("Error while creating user : " + userName, e);
			}
	
	
			if (isDebugEnabled) {
				log.debug("Returning created user's email : " + createdUser.getPrimaryEmail());
			}
		}
		else {
			if (isDebugEnabled) {
				log.debug("User does not have google role, hence proceed with not provisioning");
			}
			return null;
			
		}

		if (log.isTraceEnabled()) {
			log.trace("Ending createUser() of "
					+ GoogleProvisioningConnector.class);
		}
		return createdUser.getPrimaryEmail();
	}

	public String deleteUser(String userName, UserStoreManager userStoreManager) throws IdentityProvisioningException {

		if (log.isTraceEnabled()) {
			log.trace("Starting deleteUser() of "
					+ GoogleProvisioningConnector.class);
		}
		boolean isDebugEnabled = log.isDebugEnabled();

		User deletingUser = new User();

		String result = "";
		String userId;
		try {
				userId = userStoreManager.getUserClaimValue(userName, configBuilder.getUserIdClaim(), null);

				if(userId != null && !userId.isEmpty()) {
					deletingUser.setPrimaryEmail(userId);

					Directory.Users.Delete request;
					try {
						request = getDirectoryService().users().delete(userId);
						request.execute();

						// Removing userID claim value
						try {
							userStoreManager.setUserClaimValue(userName, configBuilder.getUserIdClaim(), "", null);
						} catch (org.wso2.carbon.user.api.UserStoreException e) {
							log.error("Required claim value for user id is not stored in user store");
							throw new IdentityProvisioningException("Required claim value for user id is not stored in user store", e);
						}
						
					} catch (GoogleJsonResponseException e) {
						throw new IdentityProvisioningException("Error while deleting user : " + userName, e);
					} catch (IOException e) {
						throw new IdentityProvisioningException("Error while deleting user : " + userName, e);
					}	

					result =  "Deleted user :" + userName + " with the primaryEmail : " + userId;
					if(isDebugEnabled) {
						log.debug(result);
					}
					
				} else {
					throw new IdentityProvisioningException("Cannot delete user, userId is invalide.");
				}
			} catch (org.wso2.carbon.user.api.UserStoreException e) {
				log.error("Required claim value for user id is not stored in user store");
				throw new IdentityProvisioningException("Required claim value for user id is not stored in user store", e);
			}
			
		if (log.isTraceEnabled()) {
			log.trace("Ending deleteUser() of "
					+ GoogleProvisioningConnector.class);
		}
		return result;
	}

	@Override
	public boolean updateUserListOfRole(String roleName, String[] deletedUsers,
			String[] newUsers, UserStoreManager userStoreManager)
			throws IdentityProvisioningException {
		log.debug("Google Connector updateUserListOfRole");
		return true;
	}

	@Override
	public boolean updateRoleListOfUser(String userName, String[] deletedRoles,
			String[] newRoles, UserStoreManager userStoreManager)
			throws UserStoreException {
		if (log.isTraceEnabled()) {
			log.debug("Starting updateRoleListOfUser() of "
					+ GoogleProvisioningConnector.class);
		}
		boolean isDebugEnabled = log.isDebugEnabled();
		
		String googleRoleName = this.configBuilder.getValue(GoogleConnectorConstants.PropertyConfig.ROLE_NAME);
		
		if(googleRoleName == null || googleRoleName.isEmpty()) {
			googleRoleName = "Google";
		}
		
		// Check whether role is added or removed
		boolean isAddingGoogleRole = false;
		boolean isRemovingGoogleRole = false;
		
		if(newRoles != null) {
			for(String role : newRoles) {
				if(role != null && role.equals(googleRoleName)) {
					isAddingGoogleRole = true;
					break;
				}
			}
		}
		
		if(deletedRoles != null) {
			for(String role : deletedRoles) {
				if(role != null && role.equals(googleRoleName)) {
					isRemovingGoogleRole = true;
					break;
				}
			}
		}

		// Check for already provisioned user
		boolean isAlreadyProvisioned = false;

		String userId = isAlreadyProvisionedUser(userName, userStoreManager);
		if(userId != null && !userId.isEmpty() ) {
			isAlreadyProvisioned = true;
		}
		

		if(isDebugEnabled) {
			log.debug("isAddingGoogleRole : " + isAddingGoogleRole + ", and isRemovingGoogleRole : " + isRemovingGoogleRole);
			
			if(isAlreadyProvisioned) {
				log.debug("User : " + userName + " already exists in google domain with id : " + userId);
			}
			else {
				log.debug("User : " + userName + " does not exist in google domain");
			}
				
		}
		
		// Provision
		if(!isAlreadyProvisioned && isAddingGoogleRole) {
			if (isDebugEnabled) {
				log.debug("Provision user");
			}
			String userID;
			try {
				userID = createUser(userName, userName, newRoles, null, null, userStoreManager);
			} catch (org.wso2.carbon.user.api.UserStoreException e) {
				throw new UserStoreException(e);
			}
			if (isDebugEnabled) {
				log.debug("Returning created user's email : " + userID);
			}
		}
		// De-provision
		else if(isAlreadyProvisioned && (isRemovingGoogleRole && !isAddingGoogleRole)) {
			if (isDebugEnabled) {
				log.debug("De-provision user");
			}
			String userID;
			try {
				userID = deleteUser(userName, userStoreManager);
			} catch (org.wso2.carbon.user.api.UserStoreException e) {
				throw new UserStoreException(e);
			}
			if (isDebugEnabled) {
				log.debug("Deleted google user email : " + userID);
			}
			
		}
		// Update
		else {
			if (isDebugEnabled) {
				log.debug("Update user");
			}
		}

		if (log.isTraceEnabled()) {
			log.debug("Ending updateRoleListOfUser() of "
					+ GoogleProvisioningConnector.class);
		}
		return true;
	}

	@Override
	public boolean addRole(String roleName, String[] userList,
			Permission[] permissions, UserStoreManager userStoreManager)
			throws IdentityProvisioningException {
		log.debug("Google Connector addRole");
		return true;
	}

	@Override
	public boolean deleteRole(String roleName, UserStoreManager userStoreManager)
			throws IdentityProvisioningException {
		log.debug("Google Connector deleteRole");
		return true;
	}

	/**
	 * 
	 * @return
	 * @throws IdentityProvisioningException
	 */
	public String listUsers(String query) throws IdentityProvisioningException {

		if (log.isTraceEnabled()) {
			log.trace("Starting listUsers() of "
					+ GoogleProvisioningConnector.class);
		}
		boolean isDebugEnabled = log.isDebugEnabled();
		


		List<User> allUsers = new ArrayList<User>();
		Directory.Users.List request;
		try {
			request = getDirectoryService().users().list()
					.setCustomer("my_customer");
			
			// Get all users
			do {
				try {
					Users currentPage = request.execute();
					allUsers.addAll(currentPage.getUsers());
					request.setPageToken(currentPage.getNextPageToken());
				} catch (IOException e) {
					log.error("Error while retrieving user info, continur to retrieve");
					request.setPageToken(null);
				}
			} while (request.getPageToken() != null
					&& request.getPageToken().length() > 0);

			// Print all users
			StringBuilder sb = new StringBuilder();
			for (User currentUser : allUsers) {
				sb.append(currentUser.getPrimaryEmail() + "\n");
				if (isDebugEnabled) {
					log.debug("List Google users : "
							+ currentUser.getPrimaryEmail());
				}
			}
			return sb.toString();
			
		} catch (IOException e) {
			throw new IdentityProvisioningException(e);
		}
	}

	private Map<String, String> getRequiredAttributes(String userName,
			Object credential, String[] roleList, Map<String, String> claims,
			String profile, UserStoreManager userStoreManager) throws UserStoreException {

		if (log.isTraceEnabled()) {
			log.trace("Starting getRequiredAttributes() of "
					+ GoogleProvisioningConnector.class);
		}
		boolean isDebugEnabled = log.isDebugEnabled();
		
		List<String> requiredAttributeNameList = configBuilder.getRequiredAttributeNames();
		if(isDebugEnabled) {
			log.debug(" Required attributes for google connector : " + requiredAttributeNameList.toString());
		}
		
		Map<String,String> requiredAttributeValueMap = new HashMap<String,String>();
		for(String attribute : requiredAttributeNameList) {
			String value = null;
			// Find from claims
			String claim = this.getProperty(GoogleConnectorConstants.PropertyConfig.REQUIRED_CLAIM_PREFIX + attribute);
			if (claim != null && !claim.isEmpty()) {
				if(claims != null && !claims.isEmpty() && claims.containsKey(claim)) {
					value = claims.get(claim);
				}
				else {
					// Search for claim from user store
					try {
						String clamValue = userStoreManager.getUserClaimValue(userName, claim, null);
						if(clamValue != null && !clamValue.isEmpty()) {
							value = clamValue;
						}
					} catch (org.wso2.carbon.user.api.UserStoreException e) {
						throw new UserStoreException(e);
					}
				}
				
				// TODO : change defining way username pick, apart from special value from claims
				// check for email username also
				if (claim.equals("USE_USERNAME")) {
					value = userName;
				}
				// TODO : change defining way password pick, apart from special value from claims
				else if (claim.equals("USE_PASSWORD")) {
					value = credential.toString();
				}
			}
			else {
				// Find from default value
				String defaultValue = this.getProperty(GoogleConnectorConstants.PropertyConfig.REQUIRED_DEFAULT_PREFIX + attribute);
				
				// TODO : change defining way special names pick, apart from special value from default
				// If the default value is special name
				if (defaultValue != null && !defaultValue.isEmpty() && defaultValue.equals("USE_USERNAME")) {
					value = userName;
				}
				// If the default value is special name
				else if (defaultValue != null && !defaultValue.isEmpty() && defaultValue.equals("USE_PASSWORD")) {
					value = credential.toString();
				}
				else if (defaultValue != null && !defaultValue.isEmpty()) {
					value = defaultValue;
				}
			}
			
			// Still attribute doesn't has a value
			if(value == null) {
				log.error("Required attribute : " + attribute + " cannot mapped to a value. Either required claim doesn't specified' or dont have a default value to attribute.");
				throw new IdentityProvisioningException("Required attribute : " + attribute + " cannot mapped to a value.");
			}
			
			
			requiredAttributeValueMap.put(attribute, value);
			if(isDebugEnabled) {
				log.debug("Required attribute : " + attribute + " mapped to value : " + value);
			}
		}
		
		if (log.isTraceEnabled()) {
			log.trace("Ending createUser() of "
					+ GoogleProvisioningConnector.class);
		}
		
		return requiredAttributeValueMap;
	}
	
	private String isAlreadyProvisionedUser(String userName, UserStoreManager userStoreManager) {
		// read userId claim value from user store
		try {
			return userStoreManager.getUserClaimValue(userName, configBuilder.getUserIdClaim(), null);
		} catch (IdentityProvisioningException e) {
			return null;
		} catch (org.wso2.carbon.user.api.UserStoreException e) {
			return null;
		}
	}
}
