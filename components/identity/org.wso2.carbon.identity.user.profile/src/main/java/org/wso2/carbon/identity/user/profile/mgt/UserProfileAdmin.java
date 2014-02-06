/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.user.profile.mgt;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonException;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.user.api.ClaimMapping;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.api.Claim;
import org.wso2.carbon.user.core.claim.ClaimManager;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.profile.ProfileConfiguration;
import org.wso2.carbon.user.core.profile.ProfileConfigurationManager;
import org.wso2.carbon.utils.ServerConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class UserProfileAdmin extends AbstractAdmin {

    private static Log log = LogFactory.getLog(UserProfileAdmin.class);
    
    public boolean isReadOnlyUserStore() throws UserProfileException {
        try {
            UserRealm realm = getUserRealm();
            if ("true".equals(realm.getRealmConfiguration().getUserStoreProperty(
                    UserCoreConstants.RealmConfig.PROPERTY_READ_ONLY))) {
                return true;
            }
            return false;
        } catch (UserStoreException e) {
            log.error(e.getMessage(), e);
            throw new UserProfileException(e.getMessage(), e);
        }
    }
    
    public void setUserProfile(String username, UserProfileDTO profile) throws UserProfileException {
        UserRealm realm = null;
        try {
           
            if (!this.isAuthorized(username)) {
                throw new UserProfileException("You are not authorized to perform this action.");
            }

            int indexOne;
            indexOne = username.indexOf("/");

            if (indexOne < 0) {
                /*if domain is not provided, this can be the scenario where user from a secondary user store
                logs in without domain name and tries to view his own profile*/
                MessageContext messageContext = MessageContext.getCurrentMessageContext();
                HttpServletRequest request = (HttpServletRequest) messageContext
                        .getProperty("transport.http.servletRequest");
                String domainName = (String) request.getSession().getAttribute("logged_in_domain");

                if (domainName != null) {
                    username = domainName + "/" + username;
                }
            }

            realm = getUserRealm();

            UserFieldDTO[] udatas = profile.getFieldValues();
            Map<String, String> map = new HashMap<String, String>();
            for (UserFieldDTO data : udatas) {
                String claimURI = data.getClaimUri();
				String value = data.getFieldValue();
				if (!data.isReadOnly()) {
					// Quick fix for not to remove OTP checkbox when false
					if(value == "" && "http://wso2.org/claims/identity/otp".equals(claimURI)) {
						value = "false";
					}					
					map.put(claimURI, value);
				}
			}

            if (profile.getProfileConifuration() != null) {
                map.put(UserCoreConstants.PROFILE_CONFIGURATION, profile.getProfileConifuration());
            } else {
                map.put(UserCoreConstants.PROFILE_CONFIGURATION,
                        UserCoreConstants.DEFAULT_PROFILE_CONFIGURATION);
            }

            UserStoreManager admin = realm.getUserStoreManager();
            admin.setUserClaimValues(username, map, profile.getProfileName());

        } catch (UserStoreException e) {
            // Not logging. Already logged.
            throw new UserProfileException(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new UserProfileException(e.getMessage(), e);
        }
    }

    public void deleteUserProfile(String username, String profileName) throws UserProfileException {
        UserRealm realm = null;
        try {
            if (!this.isAuthorized(username)) {
                throw new UserProfileException("You are not authorized to perform this action.");
            }

            if (UserCoreConstants.DEFAULT_PROFILE.equals(profileName)) {
                throw new UserProfileException("Cannot delete default profile");
            }
            realm = getUserRealm();

            ClaimManager cman = realm.getClaimManager();
            String[] claims = cman.getAllClaimUris();
            UserStoreManager admin = realm.getUserStoreManager();
            admin.deleteUserClaimValues(username, claims, profileName);
            admin.deleteUserClaimValue(username, UserCoreConstants.PROFILE_CONFIGURATION,
                            profileName);
        } catch (UserStoreException e) {
            // Not logging. Already logged.
            throw new UserProfileException(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new UserProfileException(e.getMessage(), e);
        }
    }

    public UserProfileDTO[] getUserProfiles(String username)
            throws UserProfileException {
        UserProfileDTO[] profiles;
        String[] availableProfileConfigurations = new String[0];
        String profileConfig = null;
        try {
            if (!this.isAuthorized(username)) {
                throw new UserProfileException("You are not authorized to perform this action.");
            }

            UserRealm realm = getUserRealm();

            UserStoreManager ur = realm.getUserStoreManager(); 
            
            boolean isReadOnly = ur.isReadOnly();
            
			int index;
			index = username.indexOf("/");

			UserStoreManager secUserStoreManager = null;
			
			// Check whether we have a secondary UserStoreManager setup.
			if (index > 0) {
				// Using the short-circuit. User name comes with the domain name.
				String domain = username.substring(0, index);

				if (ur instanceof AbstractUserStoreManager) {
					secUserStoreManager = ((AbstractUserStoreManager) ur)
							.getSecondaryUserStoreManager(domain);
					if (secUserStoreManager != null) {
						isReadOnly = secUserStoreManager.isReadOnly();
					}
				}
			}

            ProfileConfigurationManager profileAdmin = realm
                    .getProfileConfigurationManager();
            if (profileAdmin != null) {
                availableProfileConfigurations = getAvailableProfileConfiguration(profileAdmin);
            }
            
            String[] profileNames = null;

            if(secUserStoreManager != null){
                profileNames = secUserStoreManager.getProfileNames(username);
            } else {
                profileNames = ur.getProfileNames(username);
            }
            
            profiles = new UserProfileDTO[profileNames.length];
            Claim[] claims = getAllSupportedClaims(realm, UserCoreConstants.DEFAULT_CARBON_DIALECT);
            String[] claimUris = new String[claims.length + 1];
            for (int i = 0; i < claims.length; i++) {
                claimUris[i] = claims[i].getClaimUri();
            }

            claimUris[claims.length] = UserCoreConstants.PROFILE_CONFIGURATION;

            for (int i = 0; i < profileNames.length; i++) {
                String profile = profileNames[i];
                Map<String, String> valueMap = ur.getUserClaimValues(username, claimUris, profile);
                ArrayList<UserFieldDTO> userFields = new ArrayList<UserFieldDTO>();
                for (int j = 0; j < claims.length; j++) {
                    UserFieldDTO data = new UserFieldDTO();
                    Claim claim = claims[j];
                    String claimUri = claim.getClaimUri();
                    if (!UserCoreConstants.PROFILE_CONFIGURATION.equals(claimUri)) {
                        data.setClaimUri(claimUri);
                        data.setFieldValue(valueMap.get(claimUri));
                        data.setDisplayName(claim.getDisplayTag());
                        data.setRegEx(claim.getRegEx());
                        data.setRequired(claim.isRequired());
                        data.setDisplayOrder(claim.getDisplayOrder());
                        data.setCheckedAttribute(claim.isCheckedAttribute());
                        data.setReadOnly(claim.isReadOnly());
                        userFields.add(data);
                    }
                }

                UserProfileDTO temp = new UserProfileDTO();
                temp.setProfileName(profile);
                temp.setFieldValues(userFields.toArray(new UserFieldDTO[userFields.size()]));
                temp.setProfileConfigurations(availableProfileConfigurations);

                profileConfig = valueMap.get(UserCoreConstants.PROFILE_CONFIGURATION);
                if (profileConfig == null) {
                    profileConfig = UserCoreConstants.DEFAULT_PROFILE_CONFIGURATION;
                }
                
                if (isReadOnly){
                	profileConfig = "readonly";
                }

                temp.setProfileConifuration(profileConfig);
                profiles[i] = temp;
            }
        } catch (RuntimeException e){
            throw e;
        } catch (Exception e) {
            // Not logging. Already logged.
            throw new UserProfileException(e.getMessage(), e);
        }
        return profiles;

    }

    public UserProfileDTO getProfileFieldsForInternalStore() throws UserProfileException {
        UserFieldDTO[] datas;
        UserProfileDTO profile = new UserProfileDTO();
        String[] availableProfileConfigurations = new String[0];
        try {
            UserRealm realm = getUserRealm();
            Claim[] claims = getClaimsToEnterData(realm);

            ProfileConfigurationManager profileAdmin = realm
                    .getProfileConfigurationManager();
            if (profileAdmin != null) {
                availableProfileConfigurations = getAvailableProfileConfiguration(profileAdmin);
            }

            String[] claimUris = new String[claims.length];
            for (int i = 0; i < claims.length; i++) {
                claimUris[i] = claims[i].getClaimUri();
            }
            datas = new UserFieldDTO[claims.length];
            for (int j = 0; j < claims.length; j++) {
                UserFieldDTO data = new UserFieldDTO();
                Claim claim = claims[j];
                String claimUri = claim.getClaimUri();
                data.setClaimUri(claimUri);
                data.setDisplayName(claim.getDisplayTag());
                data.setRegEx(claim.getRegEx());
                data.setRequired(claim.isRequired());
                data.setDisplayOrder(claim.getDisplayOrder());
                data.setRegEx(claim.getRegEx());
                data.setCheckedAttribute(claim.isCheckedAttribute());
                data.setReadOnly(claim.isReadOnly());
                datas[j] = data;
            }

        } catch (Exception e) {
            // Not logging. Already logged.
            throw new UserProfileException(e.getMessage(), e);
        }

        profile.setFieldValues(datas);
        profile.setProfileConfigurations(availableProfileConfigurations);

        return profile;
    }

    public UserProfileDTO getUserProfile(String username, String profileName)
            throws UserProfileException {
        UserProfileDTO profile = new UserProfileDTO();
        String[] availableProfileConfigurations = new String[0];
        String profileConfig = null;

        try {

            if (username == null || profileName == null) {
                throw new Exception("Invalid input parameters");
            }

            if (!this.isAuthorized(username)) {
                throw new UserProfileException("You are not authorized to perform this action.");
            }

            UserRealm realm = getUserRealm();

            UserStoreManager ur = realm.getUserStoreManager();
            
            boolean isReadOnly = ur.isReadOnly();

            int indexOne;
			indexOne = username.indexOf("/");

            if (indexOne < 0) {
                /*if domain is not provided, this can be the scenario where user from a secondary user store
                logs in without domain name and tries to view his own profile*/
                MessageContext messageContext = MessageContext.getCurrentMessageContext();
                HttpServletRequest request = (HttpServletRequest) messageContext
                        .getProperty("transport.http.servletRequest");
                String domainName = (String) request.getSession().getAttribute("logged_in_domain");

                if (domainName != null) {
                    username = domainName + "/" + username;
                }
            }
			int index;
			index = username.indexOf("/");

			UserStoreManager secUserStoreManager = null;
			
			// Check whether we have a secondary UserStoreManager setup.
			if (index > 0) {
				// Using the short-circuit. User name comes with the domain name.
				String domain = username.substring(0, index);

				if (ur instanceof AbstractUserStoreManager) {
					secUserStoreManager = ((AbstractUserStoreManager) ur)
							.getSecondaryUserStoreManager(domain);
					if (secUserStoreManager != null) {
						isReadOnly = secUserStoreManager.isReadOnly();
					}
				}
			}
            
            ProfileConfigurationManager profileAdmin = realm
                    .getProfileConfigurationManager();

            String[] profileNames = null;
            
            if(secUserStoreManager != null){
                profileNames = secUserStoreManager.getProfileNames(username);
            } else {
                profileNames = ur.getProfileNames(username);
            }
            
            boolean found = false;

            if (profileNames != null && profileNames.length > 0) {
                for (int i = 0; i < profileNames.length; i++) {
                    if (profileName.equals(profileNames[i])) {
                        found = true;
                        break;
                    }
                }
            }

            if (!found) {
                return null;
            }

            if (profileAdmin != null) {
                availableProfileConfigurations = getAvailableProfileConfiguration(profileAdmin);
            }

            Claim[] claims = getClaimsToEnterData(realm);
            String[] claimUris = new String[claims.length + 1];
            for (int i = 0; i < claims.length; i++) {
                claimUris[i] = claims[i].getClaimUri();
            }

            claimUris[claims.length] = UserCoreConstants.PROFILE_CONFIGURATION;

            Map<String, String> valueMap = ur.getUserClaimValues(username, claimUris, profileName);
            ArrayList<UserFieldDTO> userFields = new ArrayList<UserFieldDTO>();

            for (int j = 0; j < claims.length; j++) {
                UserFieldDTO data = new UserFieldDTO();
                Claim claim = claims[j];
                String claimUri = claim.getClaimUri();
                if (!UserCoreConstants.PROFILE_CONFIGURATION.equals(claimUri)) {
                    data.setClaimUri(claimUri);
                    data.setFieldValue(valueMap.get(claimUri));
                    data.setDisplayName(claim.getDisplayTag());
                    data.setRegEx(claim.getRegEx());
                    data.setRequired(claim.isRequired());
                    data.setDisplayOrder(claim.getDisplayOrder());
                    data.setReadOnly(claim.isReadOnly());
                    data.setCheckedAttribute(claim.isCheckedAttribute());
                    userFields.add(data);
                }
            }

            profile.setProfileName(profileName);
            profile.setProfileConfigurations(availableProfileConfigurations);

            profileConfig = valueMap.get(UserCoreConstants.PROFILE_CONFIGURATION);
            if (profileConfig == null) {
                profileConfig = UserCoreConstants.DEFAULT_PROFILE_CONFIGURATION;
            }
            
            if (isReadOnly){
            	profileConfig = "readonly";
            }

            profile.setProfileConifuration(profileConfig);
            profile.setFieldValues(userFields.toArray(new UserFieldDTO[userFields.size()]));

        } catch (Exception e) {
            // Not logging. Already logged.
            throw new UserProfileException(e.getMessage(), e);
        }
        return profile;
    }

    public boolean isAddProfileEnabled() throws UserProfileException {
        UserRealm realm = getUserRealm();
        UserStoreManager userStoreManager = null;
        try {
            userStoreManager = realm.getUserStoreManager();
        } catch (UserStoreException e) {
            String errorMessage = "Error in obtaining UserStoreManager.";
            log.error(errorMessage, e);
            throw new UserProfileException(errorMessage, e);
        }
        return userStoreManager.isMultipleProfilesAllowed();
    }


    public boolean isAddProfileEnabledForDomain(String domain) throws UserProfileException {

        org.wso2.carbon.user.core.UserStoreManager userStoreManager = null;
        org.wso2.carbon.user.core.UserRealm realm = getUserRealm();
        boolean isAddProfileEnabled = false;

        try {
            userStoreManager = realm.getUserStoreManager().getSecondaryUserStoreManager(domain);

        } catch (UserStoreException e) {
            String errorMessage = "Error in obtaining SecondaryUserStoreManager.";
            log.error(errorMessage, e);
            throw new UserProfileException(errorMessage, e);
        }

        if(userStoreManager != null){
            isAddProfileEnabled = userStoreManager.isMultipleProfilesAllowed();
        }

        return isAddProfileEnabled;
    }




    private Claim[] getClaimsToEnterData(UserRealm realm)
            throws UserStoreException {
        try {
            return getAllSupportedClaims(realm, UserCoreConstants.DEFAULT_CARBON_DIALECT);
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            throw new UserStoreException(e);
        }
    }


    
    private boolean isAuthorized(String targetUser) throws UserStoreException, CarbonException {
        boolean isAuthrized = false;
        MessageContext msgContext = MessageContext.getCurrentMessageContext();
        HttpServletRequest request = (HttpServletRequest) msgContext
                .getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST);
        HttpSession httpSession = request.getSession(false);
        if (httpSession != null) {
            String userName = (String) httpSession.getAttribute(ServerConstants.USER_LOGGED_IN);
            isAuthrized = UserProfileUtil.isUserAuthorizedToConfigureProfile(getUserRealm(), userName, targetUser);
        }
        return isAuthrized;
    }

    /**
     * 
     * @return
     * @throws UserStoreException
     */
    private Claim[] getAllSupportedClaims(UserRealm realm, String dialectUri)
            throws org.wso2.carbon.user.api.UserStoreException {
        ClaimMapping[] claims = null;
        ArrayList<Claim> reqClaims = null;

        claims = realm.getClaimManager().getAllSupportClaimMappingsByDefault();
        reqClaims = new ArrayList<Claim>();
        for (int i = 0; i < claims.length; i++) {
            if (dialectUri.equals(claims[i].getClaim().getDialectURI())) {
                if (claims[i] != null && claims[i].getClaim().getDisplayTag() != null
                        && !claims[i].getClaim().getClaimUri().equals(IdentityConstants.CLAIM_PPID))
                    reqClaims.add((Claim)claims[i].getClaim());
            }
        }

        return reqClaims.toArray(new Claim[reqClaims.size()]);
    }

    private String[] getAvailableProfileConfiguration(
            ProfileConfigurationManager profileAdmin) throws UserStoreException {
        ProfileConfiguration[] configurations;
        String[] profileNames = new String[0];
        try {
            configurations = (ProfileConfiguration[]) profileAdmin.getAllProfiles();
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            throw new UserStoreException(e);
        }

        if (configurations != null) {
            profileNames = new String[configurations.length];
            for (int i = 0; i < configurations.length; i++) {
                profileNames[i] = configurations[i].getProfileName();
            }
        }

        return profileNames;
    }
}
