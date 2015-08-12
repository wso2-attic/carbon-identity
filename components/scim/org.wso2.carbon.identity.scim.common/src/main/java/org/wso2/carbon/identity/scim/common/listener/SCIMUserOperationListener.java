/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.scim.common.listener;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.identity.scim.common.group.SCIMGroupHandler;
import org.wso2.carbon.identity.scim.common.utils.IdentitySCIMException;
import org.wso2.carbon.identity.scim.common.utils.SCIMCommonConstants;
import org.wso2.carbon.identity.scim.common.utils.SCIMCommonUtils;
import org.wso2.carbon.user.api.ClaimManager;
import org.wso2.carbon.user.api.ClaimMapping;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.listener.UserOperationEventListener;
import org.wso2.carbon.user.core.util.UserCoreUtil;
import org.wso2.charon.core.schema.SCIMConstants;
import org.wso2.charon.core.util.AttributeUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * This is to perform SCIM related operation on User Operations.
 * For eg: when a user is created through UserAdmin API, we need to set some SCIM specific properties
 * as user attributes.
 */
public class SCIMUserOperationListener implements UserOperationEventListener {

    private static Log log = LogFactory.getLog(SCIMUserOperationListener.class);

    @Override
    public int getExecutionOrderId() {
        return 1;
    }

    @Override
    public boolean doPreAuthenticate(String s, Object o, UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostAuthenticate(String userName, boolean authenticated,
                                      UserStoreManager userStoreManager)
            throws UserStoreException {
        try {
            String activeAttributeValue = userStoreManager.getUserClaimValue(userName, SCIMConstants.ACTIVE_URI, null);
            boolean isUserActive = true;
            if (activeAttributeValue != null) {
                isUserActive = Boolean.parseBoolean(activeAttributeValue);
                if (isUserActive) {
                    return true;
                } else {
                    log.error("Trying to login from an inactive account of user: " + userName);
                    return false;
                }
            }
            return true;

        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            throw new UserStoreException(e);
        }

    }

    @Override
    public boolean doPreAddUser(String s, Object o, String[] strings,
                                Map<String, String> stringStringMap, String s1,
                                UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostAddUser(String userName, Object credential, String[] roleList,
                                 Map<String, String> claims, String profile,
                                 UserStoreManager userStoreManager)
            throws UserStoreException {

        try {
        /*add mandatory attributes in core schema like id, meta attributes etc
        if SCIM Enabled in User Store and if not already added.*/
            Map<String, String> attributes = null;
            try {
                if (userStoreManager.isSCIMEnabled()) {
                    //get claim manager from user store manager
                    ClaimManager claimManager = userStoreManager.getClaimManager();

                    //get existingClaims related to SCIM claim dialect
                    ClaimMapping[] existingClaims = claimManager.getAllClaimMappings(SCIMCommonConstants.SCIM_CLAIM_DIALECT);
                    List<String> claimURIList = new ArrayList<>();
                    for (ClaimMapping claim : existingClaims) {
                        claimURIList.add(claim.getClaim().getClaimUri());
                    }
                    //obtain user claim values (since user is already added at this point by CARBON UM)
                    attributes = userStoreManager.getUserClaimValues(
                            userName, claimURIList.toArray(new String[claimURIList.size()]), null);
                    //if null, or if id attribute not present, add them

                    if (MapUtils.isNotEmpty(attributes)) {
                        if (!attributes.containsKey(SCIMConstants.ID_URI)) {
                            Map<String, String> updatesAttributes =
                                    this.getSCIMAttributes(userName,
                                            attributes);
                            // set the thread local to avoid calling the
                            // listener for setUserClaimValues.
                            SCIMCommonUtils.setThreadLocalToSkipSetUserClaimsListeners(true);
                            userStoreManager.setUserClaimValues(userName, updatesAttributes, null);
                        } else if (!attributes.containsKey(SCIMConstants.USER_NAME_URI)) {
                            //Adding this since user name claim is not saved. For JDBC SCIM provisioning is not working
                            attributes.put(SCIMConstants.USER_NAME_URI, userName);
                        }
                    } else {
                        Map<String, String> newAttributes = this.getSCIMAttributes(userName, null);
                        userStoreManager.setUserClaimValues(userName, newAttributes, null);
                    }
                }
            } catch (org.wso2.carbon.user.api.UserStoreException e) {
                throw new UserStoreException("Error when updating SCIM attributes of the user.", e);
            }
            return true;

        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            throw new UserStoreException(e);
        }

    }

    @Override
    public boolean doPreUpdateCredential(String s, Object o, Object o1,
                                         UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostUpdateCredential(String userName, Object credential, UserStoreManager userStoreManager)
            throws UserStoreException {
        return doPostUpdateCredentialByAdmin(userName, credential, userStoreManager);
    }

    @Override
    public boolean doPreUpdateCredentialByAdmin(String s, Object o,
                                                UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostUpdateCredentialByAdmin(String userName, Object credential,
                                                 UserStoreManager userStoreManager)
            throws UserStoreException {

        try {
            //update last-modified-date
            try {
                if (userStoreManager.isSCIMEnabled()) {
                    Date date = new Date();
                    String lastModifiedDate = AttributeUtil.formatDateTime(date);
                    userStoreManager.setUserClaimValue(
                            userName, SCIMConstants.META_LAST_MODIFIED_URI, lastModifiedDate, null);
                }
            } catch (org.wso2.carbon.user.api.UserStoreException e) {
                log.debug(e);
                throw new UserStoreException(
                        "Error in obtaining user store information: isSCIMEnabled", e);
            }
            return true;

        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            throw new UserStoreException(e);
        }

    }

    @Override
    public boolean doPreDeleteUser(String userName, UserStoreManager userStoreManager)
            throws UserStoreException {

            return true;

    }

    @Override
    public boolean doPostDeleteUser(String s, UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPreSetUserClaimValue(String s, String s1, String s2, String s3,
                                          UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostSetUserClaimValue(String s, UserStoreManager userStoreManager)
            throws UserStoreException {
        //TODO: need to set last modified time.
        return true;
    }

    @Override
    public boolean doPreSetUserClaimValues(String userName, Map<String, String> claims,
                                           String profileName, UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostSetUserClaimValues(String userName, Map<String, String> claims,
                                            String profileName, UserStoreManager userStoreManager)
            throws UserStoreException {

        try {
            String newUserName = claims.get("urn:scim:schemas:core:1.0:userName");
            if(newUserName != null && !newUserName.isEmpty()){
                userName = newUserName;
            }

            //check if it is specified to skip this listner.
            if ((SCIMCommonUtils.getThreadLocalToSkipSetUserClaimsListeners() != null &&
                    !SCIMCommonUtils.getThreadLocalToSkipSetUserClaimsListeners()) ||
                    (SCIMCommonUtils.getThreadLocalToSkipSetUserClaimsListeners() == null)) {
                //update last-modified-date and proceed if scim enabled.
                try {
                    if (userStoreManager.isSCIMEnabled()) {
                        Date date = new Date();
                        String lastModifiedDate = AttributeUtil.formatDateTime(date);
                        userStoreManager.setUserClaimValue(
                                userName, SCIMConstants.META_LAST_MODIFIED_URI, lastModifiedDate, null);
                        String userNameInClaims = userStoreManager.getUserClaimValue(
                                userName, SCIMConstants.USER_NAME_URI, null);

                    }
                } catch (org.wso2.carbon.user.api.UserStoreException e) {
                    throw new UserStoreException("Error in retrieving claim values while provisioning " +
                            "'update user' operation.", e);
                }
            }
            return true;

        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            throw new UserStoreException(e);
        }
    }

    @Override
    public boolean doPreDeleteUserClaimValues(String s, String[] strings, String s1,
                                              UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostDeleteUserClaimValues(String s, UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPreDeleteUserClaimValue(String s, String s1, String s2,
                                             UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostDeleteUserClaimValue(String s, UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPreAddRole(String s, String[] strings,
                                org.wso2.carbon.user.api.Permission[] permissions,
                                UserStoreManager userStoreManager) throws UserStoreException {

        return true;
    }

    @Override
    public boolean doPostAddRole(String roleName, String[] userList,
                                 org.wso2.carbon.user.api.Permission[] permissions,
                                 UserStoreManager userStoreManager) throws UserStoreException {

        try {

            SCIMGroupHandler scimGroupHandler = new SCIMGroupHandler(userStoreManager.getTenantId());

            String domainName = UserCoreUtil.getDomainName(userStoreManager.getRealmConfiguration());
            if (domainName == null) {
                domainName = UserCoreConstants.PRIMARY_DEFAULT_DOMAIN_NAME;
            }
            String roleNameWithDomain = domainName + CarbonConstants.DOMAIN_SEPARATOR + roleName;

            //query role name from identity table
            try {
                if (!scimGroupHandler.isGroupExisting(roleNameWithDomain)) {
                    //if no attributes - i.e: group added via mgt console, not via SCIM endpoint
                    //add META
                    scimGroupHandler.addMandatoryAttributes(roleNameWithDomain);
                }
            } catch (IdentitySCIMException e) {
                throw new UserStoreException("Error retrieving group information from SCIM Tables.", e);
            }

            return true;

        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            throw new UserStoreException(e);
        }


    }

    @Override
    public boolean doPreDeleteRole(String roleName, UserStoreManager userStoreManager)
            throws UserStoreException {

        try {
            SCIMGroupHandler scimGroupHandler = new SCIMGroupHandler(userStoreManager.getTenantId());

            String domainName = UserCoreUtil.getDomainName(userStoreManager.getRealmConfiguration());
            if (domainName == null) {
                domainName = UserCoreConstants.PRIMARY_DEFAULT_DOMAIN_NAME;
            }
            String roleNameWithDomain = domainName + CarbonConstants.DOMAIN_SEPARATOR + roleName;
            try {
                //delete group attributes - no need to check existence here,
                //since it is checked in below method.
                scimGroupHandler.deleteGroupAttributes(roleNameWithDomain);
            } catch (IdentitySCIMException e) {
                throw new UserStoreException("Error retrieving group information from SCIM Tables.", e);
            }
            return true;

        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            throw new UserStoreException(e);
        }


    }

    @Override
    public boolean doPostDeleteRole(String roleName, UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPreUpdateRoleName(String s, String s1, UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostUpdateRoleName(String roleName, String newRoleName,
                                        UserStoreManager userStoreManager)
            throws UserStoreException {

        try {
            //TODO:set last update date
            SCIMGroupHandler scimGroupHandler = new SCIMGroupHandler(userStoreManager.getTenantId());

            String domainName = UserCoreUtil.getDomainName(userStoreManager.getRealmConfiguration());
            if (domainName == null) {
                domainName = UserCoreConstants.PRIMARY_DEFAULT_DOMAIN_NAME;
            }
            String roleNameWithDomain = domainName + CarbonConstants.DOMAIN_SEPARATOR + roleName;
            String newRoleNameWithDomain = domainName + CarbonConstants.DOMAIN_SEPARATOR + newRoleName;
            try {
                scimGroupHandler.updateRoleName(roleNameWithDomain, newRoleNameWithDomain);

            } catch (IdentitySCIMException e) {
                throw new UserStoreException("Error updating group information in SCIM Tables.", e);
            }
            return true;

        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            throw new UserStoreException(e);
        }


    }

    @Override
    public boolean doPreUpdateUserListOfRole(String s, String[] strings, String[] strings1,
                                             UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostUpdateUserListOfRole(String roleName, String[] deletedUsers,
                                              String[] newUsers, UserStoreManager userStoreManager)
            throws UserStoreException {

            return true;

    }

    @Override
    public boolean doPreUpdateRoleListOfUser(String s, String[] strings, String[] strings1,
                                             UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostUpdateRoleListOfUser(String s, String[] strings, String[] strings1,
                                              UserStoreManager userStoreManager)
            throws UserStoreException {
        //TODO:
        return true;
    }

    public Map<String, String> getSCIMAttributes(String userName, Map<String, String> claimsMap) {
        Map<String, String> attributes = null;
        if (MapUtils.isNotEmpty(claimsMap)) {
            attributes = claimsMap;
        } else {
            attributes = new HashMap<>();
        }
        String id = UUID.randomUUID().toString();
        attributes.put(SCIMConstants.ID_URI, id);

        Date date = new Date();
        String createdDate = AttributeUtil.formatDateTime(date);
        attributes.put(SCIMConstants.META_CREATED_URI, createdDate);

        attributes.put(SCIMConstants.META_LAST_MODIFIED_URI, createdDate);

        attributes.put(SCIMConstants.USER_NAME_URI, userName);

        return attributes;
        //TODO: add other optional attributes like location etc.
    }

}
