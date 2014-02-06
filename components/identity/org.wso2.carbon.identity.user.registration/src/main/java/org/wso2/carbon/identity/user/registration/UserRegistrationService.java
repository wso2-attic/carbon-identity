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
package org.wso2.carbon.identity.user.registration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.IdentityClaimManager;
import org.wso2.carbon.identity.core.persistence.IdentityPersistenceManager;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.user.registration.dto.PasswordRegExDTO;
import org.wso2.carbon.identity.user.registration.dto.UserDTO;
import org.wso2.carbon.identity.user.registration.dto.UserFieldDTO;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.user.core.*;
import org.wso2.carbon.user.core.claim.Claim;
import org.wso2.carbon.user.core.util.UserCoreUtil;
import org.wso2.carbon.user.mgt.UserMgtConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserRegistrationService {
	
    private static Log log = LogFactory.getLog(UserRegistrationService.class);

	/**
	 * This service method will return back all available password validation regular expressions
	 * against the corresponding domain names.
	 * 
	 * @return
	 * @throws IdentityException
	 */
	public PasswordRegExDTO[] getPasswordRegularExpressions() throws IdentityException {
		UserRealm realm = null;
		realm = IdentityTenantUtil.getRealm(null, null);
		List<PasswordRegExDTO> passwordRegExList = new ArrayList<PasswordRegExDTO>();
		PasswordRegExDTO passwordRegEx;

		try {

			UserStoreManager manager = realm.getUserStoreManager();
			String domainName;
			String regEx;

			while (manager != null) {
				domainName = manager.getRealmConfiguration().getUserStoreProperty(
						UserCoreConstants.RealmConfig.PROPERTY_DOMAIN_NAME);
				regEx = manager.getRealmConfiguration().getUserStoreProperty(
						UserCoreConstants.RealmConfig.PROPERTY_JS_REG_EX);
				if (regEx != null && regEx.length() > 0) {
					passwordRegEx = new PasswordRegExDTO();
					passwordRegEx.setDomainName(domainName);
					passwordRegEx.setRegEx(regEx);
					passwordRegExList.add(passwordRegEx);
				}
				manager = manager.getSecondaryUserStoreManager();
			}

		} catch (UserStoreException e) {
			log.error(e);
			throw new IdentityException(
					"Error occured while loading password validation regular expressions.");
		}

		return passwordRegExList.toArray(new PasswordRegExDTO[passwordRegExList.size()]);
	}

    public UserFieldDTO[] readUserFieldsForUserRegistration(String dialect)
            throws IdentityException {
        IdentityClaimManager claimManager = null;
        Claim[] claims = null;
        List<UserFieldDTO> claimList = null;
        UserRealm realm = null;

        claimManager = IdentityClaimManager.getInstance();
        realm = IdentityTenantUtil.getRealm(null, null);
        claims = claimManager.getAllSupportedClaims(dialect, realm);

        if (claims == null || claims.length == 0) {
            return new UserFieldDTO[0];
        }

        claimList = new ArrayList<UserFieldDTO>();

        for (Claim claim : claims) {
            if (claim.getDisplayTag() != null
                    && !IdentityConstants.PPID_DISPLAY_VALUE.equals(claim.getDisplayTag())) {
                if(UserCoreConstants.ClaimTypeURIs.ACCOUNT_STATUS.equals(claim.getClaimUri())){
                    continue;
                }
				if (!claim.isReadOnly()) {
					claimList.add(getUserFieldDTO(claim.getClaimUri(),
							claim.getDisplayTag(), claim.isRequired(),
							claim.getDisplayOrder(), claim.getRegEx()));
				}
            }
        }

        return claimList.toArray(new UserFieldDTO[claimList.size()]);
    }

    public void addUser(UserDTO user) throws Exception {
        UserFieldDTO[] userFieldDTOs = null;
        Map<String, String> userClaims = null;

        userFieldDTOs = user.getUserFields();
        userClaims = new HashMap<String, String>();

        if (userFieldDTOs != null) {
            for (UserFieldDTO userFieldDTO : userFieldDTOs) {
                userClaims.put(userFieldDTO.getClaimUri(), userFieldDTO.getFieldValue());
            }
        }

        UserRealm realm = null;
        String tenantAwareUserName = MultitenantUtils.getTenantAwareUsername(user.getUserName());
        String tenantName = MultitenantUtils.getTenantDomain(user.getUserName());
        realm = IdentityTenantUtil.getRealm(tenantName, null);
        Registry registry = IdentityTenantUtil.getRegistry(null, null);
        addUser(tenantAwareUserName, user.getPassword(), userClaims, null, realm);

        // OpenId Sign-Up if necessary.
        if (user.getOpenID() != null) {
            IdentityPersistenceManager persistentManager = IdentityPersistenceManager
                    .getPersistanceManager();
            persistentManager.doOpenIdSignUp(registry, realm, user.getOpenID(), user.getUserName());
        }

    }

    public boolean isAddUserEnabled() throws Exception {

        UserRealm userRealm = IdentityTenantUtil.getRealm(null, null);
        if (userRealm != null) {
            UserStoreManager userStoreManager = userRealm.getUserStoreManager();
            if (userStoreManager != null) {
                return !userStoreManager.isReadOnly();
            }
        }

        return false;

    }

    public boolean isAddUserWithOpenIDEnabled() throws Exception {
        return false;
    }

    public boolean isAddUserWithInfoCardEnabled() throws Exception {
        return false;
    }

    private UserFieldDTO getUserFieldDTO(String claimUri, String displayName, boolean isRequired,
            int displayOrder, String regex) {
        UserFieldDTO fieldDTO = null;
        fieldDTO = new UserFieldDTO();
        fieldDTO.setClaimUri(claimUri);
        fieldDTO.setFieldName(displayName);
        fieldDTO.setRequired(isRequired);
        fieldDTO.setDisplayOrder(displayOrder);
        fieldDTO.setRegEx(regex);
        return fieldDTO;
    }

    private void addUser(String userName, String password, Map<String, String> claimList,
            String profileName, UserRealm realm) throws IdentityException {
        UserStoreManager admin = null;
        Permission permission = null;
        try {
            if(realm != null){
                admin = realm.getUserStoreManager();

                if (!isUserNameWithAllowedDomainName(userName,realm)){
                    throw new IdentityException("Domain does not permit self registration");
                }

                // add user
                admin.addUser(userName, password, null, claimList, profileName);

            }else{
                throw new IdentityException("Domain is inactive.");
            }
            String identityRoleName = getRoleName(userName);
            
            // if this is the first time a user signs up, needs to create role
            try {
				if (!admin.isExistingRole(identityRoleName, false)) {
				    permission = new Permission("/permission/admin/login",
				            UserMgtConstants.EXECUTE_ACTION);     
				    admin.addRole(identityRoleName, new String[] { userName },
				            new Permission[] { permission }, false);
				} else {
				    // if role already exists, just add user to role
				    admin.updateUserListOfRole(identityRoleName,
				            new String[] {}, new String[] { userName });
				}
			} catch (org.wso2.carbon.user.api.UserStoreException e) {
				// If something goes wrong here - them remove the already added user.
				admin.deleteUser(userName);
	            throw new IdentityException("Error occurred while adding user : " + userName, e);
			}
        } catch (UserStoreException e) {
            throw new IdentityException("Error occurred while adding user : " + userName, e);
        }
    }
    
	private boolean isUserNameWithAllowedDomainName(String userName, UserRealm realm)
			throws IdentityException {
		int index;
		index = userName.indexOf("/");

		// Check whether we have a secondary UserStoreManager setup.
		if (index > 0) {
			// Using the short-circuit. User name comes with the domain name.
			try {
				return !realm.getRealmConfiguration().isRestrictedDomainForSlefSignUp(
						userName.substring(0, index));
			} catch (UserStoreException e) {
				throw new IdentityException(e.getMessage(), e);
			}
		}

		return true;
	}

    private String getRoleName(String userName){

        String roleName = IdentityUtil.getProperty(SelfRegistrationConstants.ROLE_NAME_PROPERTY);
        boolean externalRole = Boolean.parseBoolean(IdentityUtil.getProperty(SelfRegistrationConstants.ROLE_EXTERNAL_PROPERTY));

        String domainName = UserCoreConstants.INTERNAL_DOMAIN;
        if(externalRole){
            domainName = UserCoreUtil.extractDomainFromName(userName);
        }

        if(roleName == null || roleName.trim().length() == 0){
            roleName = IdentityConstants.IDENTITY_DEFAULT_ROLE;
        }

        if(domainName != null && domainName.trim().length() > 0){
            roleName = UserCoreUtil.addDomainToName(roleName, domainName);
        }

        return roleName;
    }

}
