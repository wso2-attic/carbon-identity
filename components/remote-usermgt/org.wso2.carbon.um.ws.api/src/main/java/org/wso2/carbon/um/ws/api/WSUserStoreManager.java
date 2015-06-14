/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.um.ws.api;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.transport.http.HttpTransportProperties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.um.ws.api.stub.ClaimValue;
import org.wso2.carbon.um.ws.api.stub.PermissionDTO;
import org.wso2.carbon.um.ws.api.stub.RemoteUserStoreManagerServiceStub;
import org.wso2.carbon.user.api.ClaimManager;
import org.wso2.carbon.user.api.Properties;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.core.Permission;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.claim.Claim;
import org.wso2.carbon.user.core.tenant.Tenant;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class WSUserStoreManager implements UserStoreManager {

    private static final Log log = LogFactory.getLog(WSUserStoreManager.class);
    private RemoteUserStoreManagerServiceStub stub = null;

    private static final String UNSUPPORTED_PASSWORD_MESSAGE = "Unsupported type of password";
    private static final String SERVICE_NAME = "RemoteUserStoreManagerService";

    public WSUserStoreManager(String serverUrl, String cookie, ConfigurationContext configCtxt)
            throws UserStoreException {
        try {
            stub = new RemoteUserStoreManagerServiceStub(configCtxt, serverUrl
                    + SERVICE_NAME);
            ServiceClient client = stub._getServiceClient();
            Options option = client.getOptions();
            option.setManageSession(true);
            option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);
        } catch (AxisFault e) {
            handleException(e.getMessage(), e);
        }
    }

    public WSUserStoreManager(String userName, String password, String serverUrl,
                              ConfigurationContext configCtxt) throws UserStoreException {
        try {

            if (serverUrl != null && !serverUrl.endsWith("/")) {
                serverUrl += "/";
            }

            stub = new RemoteUserStoreManagerServiceStub(configCtxt, serverUrl
                    + SERVICE_NAME);

            HttpTransportProperties.Authenticator authenticator = new HttpTransportProperties.Authenticator();
            authenticator.setUsername(userName);
            authenticator.setPassword(password);
            authenticator.setPreemptiveAuthentication(true);

            ServiceClient client = stub._getServiceClient();
            Options option = client.getOptions();
            option.setManageSession(true);
            option.setProperty(org.apache.axis2.transport.http.HTTPConstants.AUTHENTICATE,
                    authenticator);
        } catch (AxisFault e) {
            handleException(e.getMessage(), e);
        }
    }

    @Override
    public void addUser(String userName, Object credential, String[] roleList,
                        Map<String, String> claims, String profileName, boolean requirePasswordChange)
            throws UserStoreException {
        try {
            if (!(credential instanceof String)) {
                throw new UserStoreException(UNSUPPORTED_PASSWORD_MESSAGE);
            }
            String password = (String) credential;
            ClaimValue[] claimValues = WSRealmUtil.convertMapToClaimValue(claims);
            stub.addUser(userName, password, roleList, claimValues, profileName,
                    requirePasswordChange);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
    }

    public void addRole(String roleName, String[] userList, Permission[] permissions)
            throws UserStoreException {
        try {
            stub.addRole(roleName, userList, convertPermission(permissions));
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }

    }

    @Override
    public void addUser(String userName, Object credential, String[] roleList,
                        Map<String, String> claims, String profileName) throws UserStoreException {
        if (!(credential instanceof String)) {
            throw new UserStoreException(UNSUPPORTED_PASSWORD_MESSAGE);
        }
        try {
            stub.addUser(userName, (String) credential, roleList,
                    WSRealmUtil.convertMapToClaimValue(claims), profileName, false);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }

    }

    @Override
    public boolean authenticate(String userName, Object credential) throws UserStoreException {
        if (!(credential instanceof String)) {
            throw new UserStoreException(UNSUPPORTED_PASSWORD_MESSAGE);
        }
        try {
            return stub.authenticate(userName, (String) credential);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
        return false;
    }

    @Override
    public void deleteRole(String roleName) throws UserStoreException {
        try {
            stub.deleteRole(roleName);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }

    }

    @Override
    public void deleteUser(String userName) throws UserStoreException {
        try {
            stub.deleteUser(userName);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
    }

    @Override
    public void deleteUserClaimValue(String userName, String claimURI, String profileName)
            throws UserStoreException {
        try {
            stub.deleteUserClaimValue(userName, claimURI, profileName);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
    }

    @Override
    public void deleteUserClaimValues(String userName, String[] claims, String profileName)
            throws UserStoreException {
        try {
            stub.deleteUserClaimValues(userName, claims, profileName);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }

    }

    @Override
    public String[] getAllProfileNames() throws UserStoreException {
        try {
            return stub.getAllProfileNames();
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
        return new String[0];
    }

    @Override
    public String[] getHybridRoles() throws UserStoreException {
        try {
            return stub.getHybridRoles();
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
        return new String[0];
    }

    @Override
    public String[] getAllSecondaryRoles() throws UserStoreException {
        return new String[0]; // To change body of implemented methods use File | Settings | File
        // Templates.
    }

    @Override
    public Date getPasswordExpirationTime(String username) throws UserStoreException {
        try {
            long time = stub.getPasswordExpirationTime(username);
            if (time != -1) {
                return new Date(time);
            }
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String[] getProfileNames(String userName) throws UserStoreException {
        try {
            return stub.getProfileNames(userName);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
        return new String[0];
    }

    @Override
    public String[] getRoleListOfUser(String userName) throws UserStoreException {
        try {
            return stub.getRoleListOfUser(userName);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
        return new String[0];
    }

    @Override
    public String[] getRoleNames() throws UserStoreException {
        try {
            return stub.getRoleNames();
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
        return new String[0];
    }

    @Override
    public String[] getRoleNames(boolean b) throws UserStoreException {
        return new String[0]; // To change body of implemented methods use File | Settings | File
        // Templates.
    }

    @Override
    public int getTenantId() throws UserStoreException {
        try {
            return stub.getTenantId();
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
        return -1;
    }

    @Override
    public int getTenantId(String username) throws UserStoreException {
        try {
            return stub.getTenantIdofUser(username);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
        return -1;
    }

    @Override
    public String getUserClaimValue(String userName, String claim, String profileName)
            throws UserStoreException {
        try {
            return stub.getUserClaimValue(userName, claim, profileName);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
        return profileName;
    }

    @Override
    public Claim[] getUserClaimValues(String userName, String profileName)
            throws UserStoreException {
        try {
            return WSRealmUtil.convertToClaims(stub.getUserClaimValues(userName, profileName));
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
        return new Claim[0];
    }

    @Override
    public Map<String, String> getUserClaimValues(String userName, String[] claims,
                                                  String profileName) throws UserStoreException {
        try {
            return WSRealmUtil.convertClaimValuesToMap(stub.getUserClaimValuesForClaims(userName,
                    claims, profileName));
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
        return new TreeMap<String, String>();
    }

    @Override
    public int getUserId(String username) throws UserStoreException {
        try {
            return stub.getUserId(username);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
        return -1;
    }

    @Override
    public String[] getUserListOfRole(String roleName) throws UserStoreException {
        try {
            return stub.getUserListOfRole(roleName);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
        return new String[0];
    }

    @Override
    public boolean isExistingRole(String roleName, boolean isSharedRole) throws UserStoreException {

        try {
            return stub.isExistingRole(roleName);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean isExistingUser(String userName) throws UserStoreException {

        try {
            return stub.isExistingUser(userName);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean isReadOnly() throws UserStoreException {

        try {
            return stub.isReadOnly();
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
        return false;
    }

    @Override
    public String[] listUsers(String filter, int maxItemLimit) throws UserStoreException {

        try {
            return stub.listUsers(filter, maxItemLimit);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
        return new String[0];
    }

    @Override
    public void setUserClaimValue(String userName, String claimURI, String claimValue,
                                  String profileName) throws UserStoreException {

        try {
            stub.setUserClaimValue(userName, claimURI, claimValue, profileName);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
    }

    @Override
    public void setUserClaimValues(String userName, Map<String, String> claims, String profileName)
            throws UserStoreException {
        try {
            stub.setUserClaimValues(userName, WSRealmUtil.convertMapToClaimValue(claims),
                    profileName);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
    }

    public void addUserClaimValue(String userName, String claimURI, String claimValue,
                                  String profileName) throws UserStoreException {

        try {
            stub.addUserClaimValue(userName, claimURI, claimValue, profileName);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
    }

    public void addUserClaimValues(String userName, Map<String, String> claims, String profileName)
            throws UserStoreException {
        try {
            stub.addUserClaimValues(userName, WSRealmUtil.convertMapToClaimValue(claims),
                    profileName);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
    }

    @Override
    public void updateCredential(String userName, Object newCredential, Object oldCredential)
            throws UserStoreException {
        if (!(newCredential instanceof String) || !(oldCredential instanceof String)) {
            throw new UserStoreException(UNSUPPORTED_PASSWORD_MESSAGE);
        }
        try {
            stub.updateCredential(userName, (String) newCredential, (String) oldCredential);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
    }

    @Override
    public void updateCredentialByAdmin(String userName, Object newCredential)
            throws UserStoreException {
        if (!(newCredential instanceof String)) {
            throw new UserStoreException(UNSUPPORTED_PASSWORD_MESSAGE);
        }

        try {
            stub.updateCredentialByAdmin(userName, (String) newCredential);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
    }

    @Override
    public void updateRoleListOfUser(String userName, String[] deletedRoles, String[] newRoles)
            throws UserStoreException {
        try {
            stub.updateRoleListOfUser(userName, deletedRoles, newRoles);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
    }

    @Override
    public void updateUserListOfRole(String roleName, String[] deletedUsers, String[] newUsers)
            throws UserStoreException {
        try {
            stub.updateUserListOfRole(roleName, deletedUsers, newUsers);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
    }

    @Override
    public void updateRoleName(String roleName, String newRoleName) throws UserStoreException {
        try {
            stub.updateRoleName(roleName, newRoleName);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }

    }

    /**
     * This method is to check whether multiple profiles are allowed with a particular user-store.
     * For an example, currently, JDBC user store supports multiple profiles and where as ApacheDS
     * does not allow.
     *
     * @return
     */
    @Override
    public boolean isMultipleProfilesAllowed() {
        return true;
    }

    private PermissionDTO[] convertPermission(Permission[] permissions) {
        if (permissions == null) {
            return new PermissionDTO[0];
        }
        PermissionDTO[] perms = new PermissionDTO[permissions.length];
        for (int i = 0; i < permissions.length; i++) {
            perms[i] = new org.wso2.carbon.um.ws.api.stub.PermissionDTO();
            perms[i].setAction(permissions[i].getAction());
            perms[i].setResourceId(permissions[i].getResourceId());
        }
        return perms;

    }

    private String[] handleException(String msg, Exception e) throws UserStoreException {
        log.error(e.getMessage(), e);
        throw new UserStoreException(msg, e);
    }

    @Override
    public Map<String, String> getProperties(Tenant tenant) throws UserStoreException {
        return new HashMap<>();
    }

    @Override
    public void addRole(String roleName, String[] userList,
                        org.wso2.carbon.user.api.Permission[] permissions, boolean isSharedRole)
            throws org.wso2.carbon.user.core.UserStoreException {
        addRole(roleName, userList, getUseCorePermission(permissions));

    }

    private Permission[] getUseCorePermission(org.wso2.carbon.user.api.Permission[] permissions) {
        if (permissions != null && permissions.length > 0) {
            Permission[] perm = new Permission[permissions.length];
            for (int i = 0; i < permissions.length; i++) {
                perm[i] = new Permission(permissions[i].getResourceId(), permissions[i].getAction());
            }
            return perm;
        } else {
            return new Permission[0];
        }
    }


    @Override
    public Map<String, String> getProperties(org.wso2.carbon.user.api.Tenant tenant)
            throws org.wso2.carbon.user.core.UserStoreException {
        return getProperties(Tenant.class.cast(tenant));
    }

    @Override
    public void addRememberMe(String userName, String token)
            throws org.wso2.carbon.user.api.UserStoreException {
        return;
    }

    @Override
    public boolean isValidRememberMeToken(String userName, String token)
            throws org.wso2.carbon.user.api.UserStoreException {
        return false;
    }

    @Override
    public ClaimManager getClaimManager() throws org.wso2.carbon.user.api.UserStoreException {
        return null; // To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isSCIMEnabled() throws org.wso2.carbon.user.api.UserStoreException {
        return false; // To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isBulkImportSupported() throws UserStoreException {
        return false;
    }

    @Override
    public String[] getUserList(String claim, String claimValue, String profileName)
            throws UserStoreException {
        try {
            return stub.getUserList(claim, claimValue, profileName);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }

        return new String[0];
    }

    @Override
    public UserStoreManager getSecondaryUserStoreManager() {
        return null; // To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setSecondaryUserStoreManager(UserStoreManager userStoreManager) {
        // To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public UserStoreManager getSecondaryUserStoreManager(String s) {
        return null; // To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addSecondaryUserStoreManager(String s, UserStoreManager userStoreManager) {
        // To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public RealmConfiguration getRealmConfiguration() {
        return null;
    }

    @Override
    public Properties getDefaultUserStoreProperties() {
        return null;
    }

    @Override
    public void addRole(String roleName, String[] userList,
                        org.wso2.carbon.user.api.Permission[] permissions)
            throws org.wso2.carbon.user.api.UserStoreException {
        addRole(roleName, userList, permissions, false);
    }

    @Override
    public boolean isExistingRole(String roleName) throws UserStoreException {
        return isExistingRole(roleName, false);
    }

    public boolean isSharedGroupEnabled() {
        return false;
    }

}
