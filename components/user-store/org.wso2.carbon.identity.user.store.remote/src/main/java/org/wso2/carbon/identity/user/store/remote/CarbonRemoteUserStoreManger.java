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
package org.wso2.carbon.identity.user.store.remote;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.um.ws.api.WSUserStoreManager;
import org.wso2.carbon.user.api.ClaimManager;
import org.wso2.carbon.user.api.Permission;
import org.wso2.carbon.user.api.Properties;
import org.wso2.carbon.user.api.Property;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.core.UserStoreConfigConstants;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.claim.Claim;
import org.wso2.carbon.user.core.tenant.Tenant;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class CarbonRemoteUserStoreManger implements UserStoreManager {

    private static final String CONNECTION_REFUSED = "Connection refused";
    private static final Log log = LogFactory.getLog(CarbonRemoteUserStoreManger.class);
    public static final String SERVER_URLS = "serverUrls";
    public static final String REMOTE_USER_NAME = "remoteUserName";
    public static final String PASSWORD = "password";
    private WSUserStoreManager remoteUserStore;
    private RealmConfiguration realmConfig;
    private String domainName;
    private UserStoreManager secondaryUserStoreManager;
    private Map<String, WSUserStoreManager> remoteServers = new HashMap<String, WSUserStoreManager>();
    private static final String REMOTE_ERROR_MSG = "Error occured while getting remote store value: ignoring the error";

    public CarbonRemoteUserStoreManger() {

    }

    /**
     * @param realmConfig
     * @param properties
     * @throws Exception
     */
    public CarbonRemoteUserStoreManger(RealmConfiguration realmConfig, Map properties)
            throws Exception {

        ConfigurationContext configurationContext = ConfigurationContextFactory
                .createDefaultConfigurationContext();

        Map<String, TransportOutDescription> transportsOut = configurationContext
                .getAxisConfiguration().getTransportsOut();
        for (TransportOutDescription transportOutDescription : transportsOut.values()) {
            transportOutDescription.getSender().init(configurationContext, transportOutDescription);
        }

        String[] serverUrls = realmConfig.getUserStoreProperty(SERVER_URLS).split(",");

        for (int i = 0; i < serverUrls.length; i++) {
            remoteUserStore = new WSUserStoreManager(
                    realmConfig.getUserStoreProperty(REMOTE_USER_NAME),
                    realmConfig.getUserStoreProperty(PASSWORD), serverUrls[i],
                    configurationContext);

            if (log.isDebugEnabled()) {
                log.debug("Remote Servers for User Management : " + serverUrls[i]);
            }

            remoteServers.put(serverUrls[i], remoteUserStore);
        }

        this.realmConfig = realmConfig;
        domainName = realmConfig.getUserStoreProperty(UserStoreConfigConstants.DOMAIN_NAME);
    }

    /**
     *
     */

    @Override
    public Properties getDefaultUserStoreProperties() {
        Properties properties = new Properties();
        Property[] mandatoryProperties = null;
        Property[] optionalProperties = null;
        Property remoteServerUserName = new Property(
                REMOTE_USER_NAME,
                "",
                "Remote Sever Username#Name of a user from the remote server, having enough privileges for user management",
                null);
        Property password = new Property(PASSWORD, "",
                "Remote Server Password#The password correspoing to the remote server " +
                        "username#encrypt",
                null);
        Property serverUrls = new Property(
                SERVER_URLS,
                "",
                "Remote Server URL(s)#Remote server URLs. e.g.: https://ca-datacenter/services,https://va-datacenter/services",
                null);
        Property disabled = new Property("Disabled", "false", "Disabled#Check to disable the user store", null);

        Property passwordJavaScriptRegEx = new Property(
                UserStoreConfigConstants.passwordJavaScriptRegEx, "^[\\S]{5,30}$",
                "Password RegEx (Javascript)#"
                        + UserStoreConfigConstants.passwordJavaScriptRegExDescription, null);
        Property usernameJavaScriptRegEx = new Property(
                UserStoreConfigConstants.usernameJavaScriptRegEx, "^[\\S]{3,30}$",
                "Username RegEx (Javascript)#"
                        + UserStoreConfigConstants.usernameJavaRegExDescription, null);
        Property roleNameJavaScriptRegEx = new Property(
                UserStoreConfigConstants.roleNameJavaScriptRegEx, "^[\\S]{3,30}$",
                "Role Name RegEx (Javascript)#"
                        + UserStoreConfigConstants.roleNameJavaScriptRegExDescription, null);

        mandatoryProperties = new Property[] {remoteServerUserName, password, serverUrls, passwordJavaScriptRegEx,
                usernameJavaScriptRegEx, roleNameJavaScriptRegEx};
        optionalProperties = new Property[] {disabled};

        properties.setOptionalProperties(optionalProperties);
        properties.setMandatoryProperties(mandatoryProperties);
        return properties;
    }

    /**
     *
     */
    @Override
    public boolean isExistingRole(String roleName, boolean isShared)
            throws org.wso2.carbon.user.api.UserStoreException {
        boolean rolesExists = false;
        try {
            rolesExists = remoteUserStore.isExistingRole(roleName, isShared);
        } catch (UserStoreException e) {
            if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {
                throw e;
            }
            synchronized (this) {
                for (Iterator<Entry<String, WSUserStoreManager>> iterator = remoteServers
                        .entrySet().iterator(); iterator.hasNext(); ) {
                    Entry<String, WSUserStoreManager> remoteStore = iterator.next();
                    try {
                        rolesExists = remoteStore.getValue().isExistingRole(roleName, isShared);
                        remoteUserStore = remoteStore.getValue();
                        break;
                    } catch (UserStoreException ex) {
                        if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {

                            if(log.isDebugEnabled()){

                                log.debug(REMOTE_ERROR_MSG,ex);

                            }

                            throw e;
                        }
                        log.error("Failed to connect to the remote server : "
                                + remoteStore.getKey());
                    }
                }
            }
        }
        return rolesExists;
    }

    @Override
    public void addRole(String roleName, String[] userList, Permission[] permissions,
                        boolean isSharedRole) throws org.wso2.carbon.user.api.UserStoreException {

        for (Iterator<Entry<String, WSUserStoreManager>> iterator = remoteServers.entrySet()
                .iterator(); iterator.hasNext(); ) {
            Entry<String, WSUserStoreManager> remoteStore = iterator.next();
            try {
                remoteStore.getValue().addRole(roleName, userList, permissions);
            } catch (UserStoreException e) {
                if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {
                    throw e;
                }
                log.error("Failed to update the remote server : " + remoteStore.getKey());
            }
        }

    }

    @Override
    public void addRole(String roleName, String[] userList, Permission[] permissions)
            throws org.wso2.carbon.user.api.UserStoreException {
        for (Iterator<Entry<String, WSUserStoreManager>> iterator = remoteServers.entrySet()
                .iterator(); iterator.hasNext(); ) {
            Entry<String, WSUserStoreManager> remoteStore = iterator.next();

            try {
                remoteStore.getValue().addRole(roleName, userList, permissions);
            } catch (org.wso2.carbon.user.api.UserStoreException e) {
                if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {
                    throw e;
                }
                log.error("Failed to update the remote server : " + remoteStore.getKey());
            }
        }
    }

    @Override
    public Map<String, String> getProperties(org.wso2.carbon.user.api.Tenant tenant)
            throws org.wso2.carbon.user.api.UserStoreException {
        Map<String, String> properties = new HashMap<String, String>();
        try {
            properties = remoteUserStore.getProperties(tenant);
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {
                throw e;
            }
            synchronized (this) {
                for (Iterator<Entry<String, WSUserStoreManager>> iterator = remoteServers
                        .entrySet().iterator(); iterator.hasNext(); ) {
                    Entry<String, WSUserStoreManager> remoteStore = iterator.next();
                    try {
                        properties = remoteStore.getValue().getProperties(tenant);
                        remoteUserStore = remoteStore.getValue();
                        break;
                    } catch (UserStoreException ex) {
                        if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {

                            if(log.isDebugEnabled()){

                                log.debug(REMOTE_ERROR_MSG,ex);

                            }

                            throw e;
                        }
                        log.error("Failed to connect to the remote server : "
                                + remoteStore.getKey());
                    }
                }
            }
        }
        return properties;
    }

    @Override
    public boolean isMultipleProfilesAllowed() {
        // CarbonRemoteUserStoreManger does not support multiple profiles.
        return false;
    }

    @Override
    public void addRememberMe(String userName, String token)
            throws org.wso2.carbon.user.api.UserStoreException {
        // CarbonRemoteUserStoreManger does not support remember-me..
    }

    @Override
    public boolean isValidRememberMeToken(String userName, String token)
            throws org.wso2.carbon.user.api.UserStoreException {
        // CarbonRemoteUserStoreManger does not support remember-me..
        return false;
    }

    @Override
    public ClaimManager getClaimManager() throws org.wso2.carbon.user.api.UserStoreException {
        return remoteUserStore.getClaimManager();
    }

    @Override
    public boolean isSCIMEnabled() throws org.wso2.carbon.user.api.UserStoreException {
        // CarbonRemoteUserStoreManger does not support SCIM.
        return false;
    }

    @Override
    public boolean authenticate(String userName, Object credential) throws UserStoreException {
        // CarbonRemoteUserStoreManger does not support authentication.
        return false;
    }

    @Override
    public String[] listUsers(String filter, int maxItemLimit) throws UserStoreException {

        String[] users = null;

        try {
            users = remoteUserStore.listUsers(filter, maxItemLimit);
        } catch (UserStoreException e) {
            if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {
                throw e;
            }
            synchronized (this) {
                for (Iterator<Entry<String, WSUserStoreManager>> iterator = remoteServers
                        .entrySet().iterator(); iterator.hasNext(); ) {
                    Entry<String, WSUserStoreManager> remoteStore = iterator.next();
                    try {
                        users = remoteStore.getValue().listUsers(filter, maxItemLimit);
                        remoteUserStore = remoteStore.getValue();
                        break;
                    } catch (UserStoreException ex) {
                        if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {

                            if(log.isDebugEnabled()){

                                log.debug(REMOTE_ERROR_MSG,ex);

                            }

                            throw e;
                        }
                        log.error("Failed to connect to the remote server : "
                                + remoteStore.getKey());
                    }
                }
            }
        }

        if (users != null) {
            for (int i = 0; i < users.length; i++) {
                users[i] = domainName + "/" + users[i];
            }
        } else {
            users = new String[0];
        }

        return users;
    }

    @Override
    public boolean isExistingUser(String userName) throws UserStoreException {
        boolean usersExists = false;
        try {
            usersExists = remoteUserStore.isExistingUser(userName);
        } catch (UserStoreException e) {
            if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {
                throw e;
            }
            synchronized (this) {
                for (Iterator<Entry<String, WSUserStoreManager>> iterator = remoteServers
                        .entrySet().iterator(); iterator.hasNext(); ) {
                    Entry<String, WSUserStoreManager> remoteStore = iterator.next();
                    try {
                        usersExists = remoteStore.getValue().isExistingUser(userName);
                        remoteUserStore = remoteStore.getValue();
                        break;
                    } catch (UserStoreException ex) {
                        if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {

                            if(log.isDebugEnabled()){

                                log.debug(REMOTE_ERROR_MSG,ex);

                            }

                            throw e;
                        }
                        log.error("Failed to connect to the remote server : "
                                + remoteStore.getKey());
                    }
                }
            }
        }
        return usersExists;
    }

    @Override
    public boolean isExistingRole(String roleName) throws UserStoreException {
        boolean roleExists = false;
        try {
            roleExists = remoteUserStore.isExistingRole(roleName);
        } catch (UserStoreException e) {
            if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {
                throw e;
            }
            synchronized (this) {
                for (Iterator<Entry<String, WSUserStoreManager>> iterator = remoteServers
                        .entrySet().iterator(); iterator.hasNext(); ) {
                    Entry<String, WSUserStoreManager> remoteStore = iterator.next();
                    try {
                        roleExists = remoteStore.getValue().isExistingRole(roleName);
                        remoteUserStore = remoteStore.getValue();
                        break;
                    } catch (UserStoreException ex) {
                        if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {

                            if(log.isDebugEnabled()){

                                log.debug(REMOTE_ERROR_MSG,ex);

                            }

                            throw e;
                        }
                        log.error("Failed to connect to the remote server : "
                                + remoteStore.getKey());
                    }
                }
            }
        }
        return roleExists;
    }

    @Override
    public String[] getRoleNames() throws UserStoreException {

        String[] roles = null;

        try {
            roles = remoteUserStore.getRoleNames();
        } catch (UserStoreException e) {
            if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {
                throw e;
            }
            synchronized (this) {
                for (Iterator<Entry<String, WSUserStoreManager>> iterator = remoteServers
                        .entrySet().iterator(); iterator.hasNext(); ) {
                    Entry<String, WSUserStoreManager> remoteStore = iterator.next();
                    try {
                        roles = remoteStore.getValue().getRoleNames();
                        remoteUserStore = remoteStore.getValue();
                        break;
                    } catch (UserStoreException ex) {
                        if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {

                            if(log.isDebugEnabled()){

                                log.debug(REMOTE_ERROR_MSG,ex);

                            }

                            throw e;
                        }
                        log.error("Failed to connect to the remote server : "
                                + remoteStore.getKey());
                    }
                }
            }
        }

        if (roles != null) {
            for (int i = 0; i < roles.length; i++) {
                roles[i] = domainName + "/" + roles[i];
            }
        } else {
            roles = new String[0];
        }

        return roles;
    }

    @Override
    public String[] getRoleNames(boolean noHybridRoles) throws UserStoreException {
        String[] roles = null;

        try {
            roles = remoteUserStore.getRoleNames(noHybridRoles);
        } catch (UserStoreException e) {
            if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {
                throw e;
            }
            synchronized (this) {
                for (Iterator<Entry<String, WSUserStoreManager>> iterator = remoteServers
                        .entrySet().iterator(); iterator.hasNext(); ) {
                    Entry<String, WSUserStoreManager> remoteStore = iterator.next();
                    try {
                        roles = remoteStore.getValue().getRoleNames(noHybridRoles);
                        remoteUserStore = remoteStore.getValue();
                        break;
                    } catch (UserStoreException ex) {
                        if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {

                            if(log.isDebugEnabled()){

                                log.debug(REMOTE_ERROR_MSG,ex);

                            }
                            throw e;
                        }
                        log.error("Failed to connect to the remote server : "
                                + remoteStore.getKey());
                    }
                }
            }
        }
        if (roles != null) {
            for (int i = 0; i < roles.length; i++) {
                roles[i] = domainName + "/" + roles[i];
            }
        } else {
            roles = new String[0];
        }

        return roles;
    }

    @Override
    public String[] getProfileNames(String userName) throws UserStoreException {

        String[] profileNames = new String[0];

        try {
            profileNames = remoteUserStore.getProfileNames(userName);
        } catch (UserStoreException e) {
            if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {
                throw e;
            }
            synchronized (this) {
                for (Iterator<Entry<String, WSUserStoreManager>> iterator = remoteServers
                        .entrySet().iterator(); iterator.hasNext(); ) {
                    Entry<String, WSUserStoreManager> remoteStore = iterator.next();
                    try {
                        profileNames = remoteStore.getValue().getRoleListOfUser(userName);
                        remoteUserStore = remoteStore.getValue();
                        break;
                    } catch (UserStoreException ex) {
                        if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {

                            if(log.isDebugEnabled()){

                                log.debug(REMOTE_ERROR_MSG,ex);

                            }

                            throw e;
                        }
                        log.error("Failed to connect to the remote server : "
                                + remoteStore.getKey());
                    }
                }
            }
        }
        return profileNames;
    }

    @Override
    public String[] getRoleListOfUser(String userName) throws UserStoreException {
        String[] roles = null;

        try {
            roles = remoteUserStore.getRoleListOfUser(userName);
        } catch (UserStoreException e) {
            if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {
                throw e;
            }
            synchronized (this) {
                for (Iterator<Entry<String, WSUserStoreManager>> iterator = remoteServers
                        .entrySet().iterator(); iterator.hasNext(); ) {
                    Entry<String, WSUserStoreManager> remoteStore = iterator.next();
                    try {
                        roles = remoteStore.getValue().getRoleListOfUser(userName);
                        remoteUserStore = remoteStore.getValue();
                        break;
                    } catch (UserStoreException ex) {
                        if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {

                            if(log.isDebugEnabled()){

                                log.debug(REMOTE_ERROR_MSG,ex);

                            }

                            throw e;
                        }
                        log.error("Failed to connect to the remote server : "
                                + remoteStore.getKey());
                    }
                }
            }
        }
        if (roles != null) {
            for (int i = 0; i < roles.length; i++) {
                roles[i] = domainName + "/" + roles[i];
            }
        } else {
            roles = new String[0];
        }
        return roles;
    }

    @Override
    public String[] getUserListOfRole(String roleName) throws UserStoreException {
        String[] users = null;

        try {
            users = remoteUserStore.getUserListOfRole(roleName);
        } catch (UserStoreException e) {
            if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {
                throw e;
            }
            synchronized (this) {
                for (Iterator<Entry<String, WSUserStoreManager>> iterator = remoteServers
                        .entrySet().iterator(); iterator.hasNext(); ) {
                    Entry<String, WSUserStoreManager> remoteStore = iterator.next();
                    try {
                        users = remoteStore.getValue().getUserListOfRole(roleName);
                        remoteUserStore = remoteStore.getValue();
                        break;
                    } catch (UserStoreException ex) {
                        if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {

                            if(log.isDebugEnabled()){

                                log.debug(REMOTE_ERROR_MSG,ex);

                            }

                            throw e;
                        }
                        log.error("Failed to connect to the remote server : "
                                + remoteStore.getKey());
                    }
                }
            }
        }

        if (users != null) {
            for (int i = 0; i < users.length; i++) {
                users[i] = domainName + "/" + users[i];
            }
        } else {
            users = new String[0];
        }

        return users;
    }

    @Override
    public String getUserClaimValue(String userName, String claim, String profileName)
            throws UserStoreException {
        String claimValue = null;
        try {
            claimValue = remoteUserStore.getUserClaimValue(userName, claim, profileName);
        } catch (UserStoreException e) {
            if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {
                throw e;
            }
            synchronized (this) {
                for (Iterator<Entry<String, WSUserStoreManager>> iterator = remoteServers
                        .entrySet().iterator(); iterator.hasNext(); ) {
                    Entry<String, WSUserStoreManager> remoteStore = iterator.next();
                    try {
                        claimValue = remoteStore.getValue().getUserClaimValue(userName, claim,
                                profileName);
                        remoteUserStore = remoteStore.getValue();
                        break;
                    } catch (UserStoreException ex) {
                        if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {

                            if(log.isDebugEnabled()){

                                log.debug(REMOTE_ERROR_MSG,ex);

                            }

                            throw e;
                        }
                        log.error("Failed to connect to the remote server : "
                                + remoteStore.getKey());
                    }
                }
            }
        }
        return claimValue;
    }

    @Override
    public Map<String, String> getUserClaimValues(String userName, String[] claims,
                                                  String profileName) throws UserStoreException {
        Map<String, String> claimValue = new HashMap<String, String>();

        try {
            claimValue = remoteUserStore.getUserClaimValues(userName, claims, profileName);
        } catch (UserStoreException e) {
            if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {
                throw e;
            }
            synchronized (this) {
                for (Iterator<Entry<String, WSUserStoreManager>> iterator = remoteServers
                        .entrySet().iterator(); iterator.hasNext(); ) {
                    Entry<String, WSUserStoreManager> remoteStore = iterator.next();
                    try {
                        claimValue = remoteStore.getValue().getUserClaimValues(userName, claims,
                                profileName);
                        remoteUserStore = remoteStore.getValue();
                        break;
                    } catch (UserStoreException ex) {
                        if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {

                            if(log.isDebugEnabled()){

                                log.debug(REMOTE_ERROR_MSG,ex);

                            }

                            throw e;
                        }
                        log.error("Failed to connect to the remote server : "
                                + remoteStore.getKey());
                    }
                }
            }
        }
        return claimValue;
    }

    /**
     *
     */
    @Override
    public Claim[] getUserClaimValues(String userName, String profileName)
            throws UserStoreException {
        Claim[] claim = new Claim[0];
        try {
            claim = remoteUserStore.getUserClaimValues(userName, profileName);
        } catch (UserStoreException e) {
            if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {
                throw e;
            }
            synchronized (this) {
                for (Iterator<Entry<String, WSUserStoreManager>> iterator = remoteServers
                        .entrySet().iterator(); iterator.hasNext(); ) {
                    Entry<String, WSUserStoreManager> remoteStore = iterator.next();
                    try {
                        claim = remoteStore.getValue().getUserClaimValues(userName, profileName);
                        remoteUserStore = remoteStore.getValue();
                        break;
                    } catch (UserStoreException ex) {
                        if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {

                            if(log.isDebugEnabled()){

                                log.debug(REMOTE_ERROR_MSG,ex);

                            }

                            throw e;
                        }
                        log.error("Failed to connect to the remote server : "
                                + remoteStore.getKey());
                    }
                }
            }
        }
        return claim;
    }

    /**
     *
     */
    @Override
    public String[] getAllProfileNames() throws UserStoreException {
        String[] profileNames = new String[0];
        try {
            profileNames = remoteUserStore.getAllProfileNames();
        } catch (UserStoreException e) {
            if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {
                throw e;
            }
            synchronized (this) {
                for (Iterator<Entry<String, WSUserStoreManager>> iterator = remoteServers
                        .entrySet().iterator(); iterator.hasNext(); ) {
                    Entry<String, WSUserStoreManager> remoteStore = iterator.next();
                    try {
                        profileNames = remoteStore.getValue().getAllProfileNames();
                        remoteUserStore = remoteStore.getValue();
                        break;
                    } catch (UserStoreException ex) {
                        if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {

                            if(log.isDebugEnabled()){

                                log.debug(REMOTE_ERROR_MSG,ex);

                            }

                            throw e;
                        }
                        log.error("Failed to connect to the remote server : "
                                + remoteStore.getKey());
                    }
                }
            }
        }
        return profileNames;
    }

    @Override
    public boolean isReadOnly() throws UserStoreException {
        boolean readOnly = false;
        try {
            readOnly = remoteUserStore.isReadOnly();
        } catch (UserStoreException e) {
            if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {
                throw e;
            }
            synchronized (this) {
                for (Iterator<Entry<String, WSUserStoreManager>> iterator = remoteServers
                        .entrySet().iterator(); iterator.hasNext(); ) {
                    Entry<String, WSUserStoreManager> remoteStore = iterator.next();
                    try {
                        readOnly = remoteStore.getValue().isReadOnly();
                        remoteUserStore = remoteStore.getValue();
                        break;
                    } catch (UserStoreException ex) {
                        if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {

                            if(log.isDebugEnabled()){

                                log.debug(REMOTE_ERROR_MSG,ex);

                            }

                            throw e;
                        }
                        log.error("Failed to connect to the remote server : "
                                + remoteStore.getKey());
                    }
                }
            }
        }
        return readOnly;
    }

    @Override
    public void addUser(String userName, Object credential, String[] roleList,
                        Map<String, String> claims, String profileName) throws UserStoreException {

        for (Iterator<Entry<String, WSUserStoreManager>> iterator = remoteServers.entrySet()
                .iterator(); iterator.hasNext(); ) {
            Entry<String, WSUserStoreManager> remoteStore = iterator.next();
            try {
                remoteStore.getValue().addUser(userName, credential, roleList, claims, profileName);
            } catch (UserStoreException e) {
                if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {


                    throw e;
                }
                log.error("Failed to connect to the remote server : " + remoteStore.getKey());
            }
        }
    }

    @Override
    public void addUser(String userName, Object credential, String[] roleList,
                        Map<String, String> claims, String profileName, boolean requirePasswordChange)
            throws UserStoreException {
        for (Iterator<Entry<String, WSUserStoreManager>> iterator = remoteServers.entrySet()
                .iterator(); iterator.hasNext(); ) {
            Entry<String, WSUserStoreManager> remoteStore = iterator.next();
            try {
                remoteStore.getValue().addUser(userName, credential, roleList, claims, profileName);
            } catch (UserStoreException e) {
                if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {
                    throw e;
                }
                log.error("Failed to connect to the remote server : " + remoteStore.getKey());
            }
        }
    }

    @Override
    public void updateCredential(String userName, Object newCredential, Object oldCredential)
            throws UserStoreException {
        for (Iterator<Entry<String, WSUserStoreManager>> iterator = remoteServers.entrySet()
                .iterator(); iterator.hasNext(); ) {
            Entry<String, WSUserStoreManager> remoteStore = iterator.next();
            try {
                remoteStore.getValue().updateCredential(userName, newCredential, oldCredential);
            } catch (UserStoreException e) {
                if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {
                    throw e;
                }
                log.error("Failed to connect to the remote server : " + remoteStore.getKey());
            }
        }
    }

    @Override
    public void updateCredentialByAdmin(String userName, Object newCredential)
            throws UserStoreException {
        for (Iterator<Entry<String, WSUserStoreManager>> iterator = remoteServers.entrySet()
                .iterator(); iterator.hasNext(); ) {
            Entry<String, WSUserStoreManager> remoteStore = iterator.next();
            try {
                remoteStore.getValue().updateCredentialByAdmin(userName, newCredential);
            } catch (UserStoreException e) {
                if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {
                    throw e;
                }
                log.error("Failed to connect to the remote server : " + remoteStore.getKey());
            }
        }
    }

    @Override
    public void deleteUser(String userName) throws UserStoreException {

        String domainAwareUserName = UserCoreUtil.removeDomainFromName(userName);

        for (Iterator<Entry<String, WSUserStoreManager>> iterator = remoteServers.entrySet()
                .iterator(); iterator.hasNext(); ) {
            Entry<String, WSUserStoreManager> remoteStore = iterator.next();
            try {
                remoteStore.getValue().deleteUser(domainAwareUserName);
            } catch (UserStoreException e) {
                if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {
                    throw e;
                }
                log.error("Failed to connect to the remote server : " + remoteStore.getKey());
            }
        }
    }

    @Override
    public void deleteRole(String roleName) throws UserStoreException {

        String domainAwareRoleName = UserCoreUtil.removeDomainFromName(roleName);

        for (Iterator<Entry<String, WSUserStoreManager>> iterator = remoteServers.entrySet()
                .iterator(); iterator.hasNext(); ) {
            Entry<String, WSUserStoreManager> remoteStore = iterator.next();
            try {
                remoteStore.getValue().deleteRole(domainAwareRoleName);
            } catch (UserStoreException e) {
                if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {
                    throw e;
                }
                log.error("Failed to connect to the remote server : " + remoteStore.getKey());
            }
        }
    }

    @Override
    public void updateUserListOfRole(String roleName, String[] deletedUsers, String[] newUsers)
            throws UserStoreException {
        for (Iterator<Entry<String, WSUserStoreManager>> iterator = remoteServers.entrySet()
                .iterator(); iterator.hasNext(); ) {
            Entry<String, WSUserStoreManager> remoteStore = iterator.next();
            try {
                remoteStore.getValue().updateUserListOfRole(roleName, deletedUsers, newUsers);
            } catch (UserStoreException e) {
                if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {
                    throw e;
                }
                log.error("Failed to connect to the remote server : " + remoteStore.getKey());
            }
        }
    }

    @Override
    public void updateRoleListOfUser(String userName, String[] deletedRoles, String[] newRoles)
            throws UserStoreException {
        for (Iterator<Entry<String, WSUserStoreManager>> iterator = remoteServers.entrySet()
                .iterator(); iterator.hasNext(); ) {
            Entry<String, WSUserStoreManager> remoteStore = iterator.next();
            try {
                remoteStore.getValue().updateRoleListOfUser(userName, deletedRoles, newRoles);
            } catch (UserStoreException e) {
                if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {
                    throw e;
                }
                log.error("Failed to connect to the remote server : " + remoteStore.getKey());
            }
        }
    }

    @Override
    public void setUserClaimValue(String userName, String claimURI, String claimValue,
                                  String profileName) throws UserStoreException {
        for (Iterator<Entry<String, WSUserStoreManager>> iterator = remoteServers.entrySet()
                .iterator(); iterator.hasNext(); ) {
            Entry<String, WSUserStoreManager> remoteStore = iterator.next();
            try {
                remoteStore.getValue().setUserClaimValue(userName, claimURI, claimValue,
                        profileName);
            } catch (UserStoreException e) {
                if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {
                    throw e;
                }
                log.error("Failed to connect to the remote server : " + remoteStore.getKey());
            }
        }
    }

    @Override
    public void setUserClaimValues(String userName, Map<String, String> claims, String profileName)
            throws UserStoreException {
        for (Iterator<Entry<String, WSUserStoreManager>> iterator = remoteServers.entrySet()
                .iterator(); iterator.hasNext(); ) {
            Entry<String, WSUserStoreManager> remoteStore = iterator.next();
            try {
                remoteStore.getValue().setUserClaimValues(userName, claims, profileName);
            } catch (UserStoreException e) {
                if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {
                    throw e;
                }
                log.error("Failed to connect to the remote server : " + remoteStore.getKey());
            }
        }
    }

    @Override
    public void deleteUserClaimValue(String userName, String claimURI, String profileName)
            throws UserStoreException {
        for (Iterator<Entry<String, WSUserStoreManager>> iterator = remoteServers.entrySet()
                .iterator(); iterator.hasNext(); ) {
            Entry<String, WSUserStoreManager> remoteStore = iterator.next();
            try {
                remoteStore.getValue().deleteUserClaimValue(userName, claimURI, profileName);
            } catch (UserStoreException e) {
                if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {
                    throw e;
                }
                log.error("Failed to connect to the remote server : " + remoteStore.getKey());
            }
        }
    }

    @Override
    public void deleteUserClaimValues(String userName, String[] claims, String profileName)
            throws UserStoreException {
        for (Iterator<Entry<String, WSUserStoreManager>> iterator = remoteServers.entrySet()
                .iterator(); iterator.hasNext(); ) {
            Entry<String, WSUserStoreManager> remoteStore = iterator.next();
            try {
                remoteStore.getValue().deleteUserClaimValues(userName, claims, profileName);
            } catch (UserStoreException e) {
                if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {
                    throw e;
                }
                log.error("Failed to connect to the remote server : " + remoteStore.getKey());
            }
        }
    }

    @Override
    public String[] getHybridRoles() throws UserStoreException {
        String[] roles = new String[0];
        try {
            roles = remoteUserStore.getHybridRoles();
        } catch (UserStoreException e) {
            if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {
                throw e;
            }
            synchronized (this) {
                for (Iterator<Entry<String, WSUserStoreManager>> iterator = remoteServers
                        .entrySet().iterator(); iterator.hasNext(); ) {
                    Entry<String, WSUserStoreManager> remoteStore = iterator.next();
                    try {
                        roles = remoteStore.getValue().getHybridRoles();
                        remoteUserStore = remoteStore.getValue();
                        break;
                    } catch (UserStoreException ex) {
                        if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {

                            if(log.isDebugEnabled()){

                                log.debug(REMOTE_ERROR_MSG,ex);

                            }

                            throw e;
                        }
                        log.error("Failed to connect to the remote server : "
                                + remoteStore.getKey());
                    }
                }
            }
        }

        if (roles != null) {
            for (int i = 0; i < roles.length; i++) {
                roles[i] = domainName + "/" + roles[i];
            }
        } else {
            return new String[0];
        }
        return roles;
    }

    @Override
    public String[] getAllSecondaryRoles() throws UserStoreException {
        String[] roles = new String[0];

        try {
            roles = remoteUserStore.getAllSecondaryRoles();
        } catch (UserStoreException e) {
            if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {
                throw e;
            }
            synchronized (this) {
                for (Iterator<Entry<String, WSUserStoreManager>> iterator = remoteServers
                        .entrySet().iterator(); iterator.hasNext(); ) {
                    Entry<String, WSUserStoreManager> remoteStore = iterator.next();
                    try {
                        roles = remoteStore.getValue().getAllSecondaryRoles();
                        remoteUserStore = remoteStore.getValue();
                        break;
                    } catch (UserStoreException ex) {
                        if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {

                            if(log.isDebugEnabled()){

                                log.debug(REMOTE_ERROR_MSG,ex);

                            }

                            throw e;
                        }
                        log.error("Failed to connect to the remote server : "
                                + remoteStore.getKey());
                    }
                }
            }
        }
        if (roles != null) {
            for (int i = 0; i < roles.length; i++) {
                roles[i] = domainName + "/" + roles[i];
            }
        } else {
            return new String[0];
        }
        return roles;
    }

    @Override
    public Date getPasswordExpirationTime(String username) throws UserStoreException {
        Date date = null;
        try {
            date = remoteUserStore.getPasswordExpirationTime(username);
        } catch (UserStoreException e) {
            if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {
                throw e;
            }
            synchronized (this) {
                for (Iterator<Entry<String, WSUserStoreManager>> iterator = remoteServers
                        .entrySet().iterator(); iterator.hasNext(); ) {
                    Entry<String, WSUserStoreManager> remoteStore = iterator.next();
                    try {
                        date = remoteStore.getValue().getPasswordExpirationTime(username);
                        remoteUserStore = remoteStore.getValue();
                        break;
                    } catch (UserStoreException ex) {
                        if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {

                            if(log.isDebugEnabled()){

                                log.debug(REMOTE_ERROR_MSG,ex);

                            }

                            throw e;
                        }
                        log.error("Failed to connect to the remote server : "
                                + remoteStore.getKey());
                    }
                }
            }
        }
        return date;
    }

    @Override
    public int getUserId(String username) throws UserStoreException {
        int userId = -1;
        try {
            userId = remoteUserStore.getUserId(username);
        } catch (UserStoreException e) {
            if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {
                throw e;
            }
            synchronized (this) {
                for (Iterator<Entry<String, WSUserStoreManager>> iterator = remoteServers
                        .entrySet().iterator(); iterator.hasNext(); ) {
                    Entry<String, WSUserStoreManager> remoteStore = iterator.next();
                    try {
                        userId = remoteStore.getValue().getUserId(username);
                        remoteUserStore = remoteStore.getValue();
                        break;
                    } catch (UserStoreException ex) {
                        if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {

                            if(log.isDebugEnabled()){

                                log.debug(REMOTE_ERROR_MSG,ex);

                            }

                            throw e;
                        }
                        log.error("Failed to connect to the remote server : "
                                + remoteStore.getKey());
                    }
                }
            }
        }
        return userId;
    }

    @Override
    public int getTenantId(String username) throws UserStoreException {
        int tenantId = -1;
        try {
            tenantId = remoteUserStore.getTenantId(username);
        } catch (UserStoreException e) {
            if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {
                throw e;
            }
            synchronized (this) {
                for (Iterator<Entry<String, WSUserStoreManager>> iterator = remoteServers
                        .entrySet().iterator(); iterator.hasNext(); ) {
                    Entry<String, WSUserStoreManager> remoteStore = iterator.next();
                    try {
                        tenantId = remoteStore.getValue().getTenantId(username);
                        remoteUserStore = remoteStore.getValue();
                        break;
                    } catch (UserStoreException ex) {
                        if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {

                            if(log.isDebugEnabled()){

                                log.debug(REMOTE_ERROR_MSG,ex);

                            }

                            throw e;
                        }
                        log.error("Failed to connect to the remote server : "
                                + remoteStore.getKey());
                    }
                }
            }
        }
        return tenantId;
    }

    @Override
    public int getTenantId() throws UserStoreException {
        int tenantId = -1;
        try {
            tenantId = remoteUserStore.getTenantId();
        } catch (UserStoreException e) {
            if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {
                throw e;
            }
            synchronized (this) {
                for (Iterator<Entry<String, WSUserStoreManager>> iterator = remoteServers
                        .entrySet().iterator(); iterator.hasNext(); ) {
                    Entry<String, WSUserStoreManager> remoteStore = iterator.next();
                    try {
                        tenantId = remoteStore.getValue().getTenantId();
                        remoteUserStore = remoteStore.getValue();
                        break;
                    } catch (UserStoreException ex) {
                        if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {

                            if(log.isDebugEnabled()){

                                log.debug(REMOTE_ERROR_MSG,ex);

                            }

                            throw e;
                        }
                        log.error("Failed to connect to the remote server : "
                                + remoteStore.getKey());
                    }
                }
            }
        }
        return tenantId;
    }

    @Override
    public Map<String, String> getProperties(Tenant tenant) throws UserStoreException {
        Map<String, String> properties = new HashMap<String, String>();
        try {
            properties = remoteUserStore.getProperties(tenant);
        } catch (UserStoreException e) {
            if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {
                throw e;
            }
            synchronized (this) {
                for (Iterator<Entry<String, WSUserStoreManager>> iterator = remoteServers
                        .entrySet().iterator(); iterator.hasNext(); ) {
                    Entry<String, WSUserStoreManager> remoteStore = iterator.next();
                    try {
                        properties = remoteStore.getValue().getProperties(tenant);
                        remoteUserStore = remoteStore.getValue();
                        break;
                    } catch (UserStoreException ex) {
                        if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {

                            if(log.isDebugEnabled()){

                                log.debug(REMOTE_ERROR_MSG,ex);

                            }

                            throw e;
                        }
                        log.error("Failed to connect to the remote server : "
                                + remoteStore.getKey());
                    }
                }
            }
        }
        return properties;
    }

    @Override
    public void updateRoleName(String roleName, String newRoleName) throws UserStoreException {
        for (Iterator<Entry<String, WSUserStoreManager>> iterator = remoteServers.entrySet()
                .iterator(); iterator.hasNext(); ) {
            Entry<String, WSUserStoreManager> remoteStore = iterator.next();
            try {
                remoteStore.getValue().updateRoleName(roleName, newRoleName);
            } catch (UserStoreException e) {
                if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {
                    throw e;
                }
                log.error("Failed to connect to the remote server : " + remoteStore.getKey());
            }
        }
    }

    @Override
    public boolean isBulkImportSupported() throws UserStoreException {
        return false;
    }

    @Override
    public String[] getUserList(String claim, String claimValue, String profileName)
            throws UserStoreException {
        String[] users = new String[0];
        try {
            users = remoteUserStore.getUserList(claim, claimValue, profileName);
        } catch (UserStoreException e) {
            if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {
                throw e;
            }
            synchronized (this) {
                for (Iterator<Entry<String, WSUserStoreManager>> iterator = remoteServers
                        .entrySet().iterator(); iterator.hasNext(); ) {
                    Entry<String, WSUserStoreManager> remoteStore = iterator.next();
                    try {
                        users = remoteStore.getValue().getUserList(claim, claimValue, profileName);
                        remoteUserStore = remoteStore.getValue();
                        break;
                    } catch (UserStoreException ex) {
                        if (!CONNECTION_REFUSED.equalsIgnoreCase(e.getMessage())) {

                            if(log.isDebugEnabled()){

                                log.debug(REMOTE_ERROR_MSG,ex);

                            }

                            throw e;
                        }
                        log.error("Failed to connect to the remote server : "
                                + remoteStore.getKey());
                    }
                }
            }
        }

        if (users != null) {
            for (int i = 0; i < users.length; i++) {
                users[i] = domainName + "/" + users[i];
            }
        } else {
            return new String[0];
        }
        return users;
    }

    @Override
    public UserStoreManager getSecondaryUserStoreManager() {
        return secondaryUserStoreManager;
    }

    @Override
    public void setSecondaryUserStoreManager(UserStoreManager userStoreManager) {
        this.secondaryUserStoreManager = userStoreManager;

    }

    @Override
    public UserStoreManager getSecondaryUserStoreManager(String userDomain) {
        return secondaryUserStoreManager;
    }

    @Override
    public void addSecondaryUserStoreManager(String userDomain, UserStoreManager userStoreManager) {
        return;
    }

    @Override
    public RealmConfiguration getRealmConfiguration() {
        return realmConfig;
    }

}
