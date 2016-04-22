/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.mgt;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.persistence.JDBCPersistenceManager;
import org.wso2.carbon.user.api.Permission;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.jdbc.JDBCUserStoreManager;
import org.wso2.carbon.user.core.ldap.LDAPConnectionContext;
import org.wso2.carbon.user.core.ldap.LDAPConstants;
import org.wso2.carbon.user.core.ldap.ReadWriteLDAPUserStoreManager;
import org.wso2.carbon.user.core.listener.UserOperationEventListener;
import org.wso2.carbon.user.core.util.JNDIUtil;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.sql.*;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

public class UserRenameOperationEventListener implements UserOperationEventListener {
    public static final String INSERT_STMT = "INSERT INTO IDN_UID_USER (IDN_UID, IDN_USERNAME, IDN_STORE_DOMAIN, " +
            "IDN_TENANT ) VALUES (?,?,?,?);";

    private static final Log log = LogFactory.getLog(UserRenameOperationEventListener.class);


    private static ThreadLocal<Boolean> isRenameOperation = new ThreadLocal<Boolean>() {
        @Override
        protected synchronized Boolean initialValue() {
            return false;
        }
    };
    private static ThreadLocal<Boolean> isAddUserOperation = new ThreadLocal<Boolean>() {
        @Override
        protected synchronized Boolean initialValue() {
            return false;
        }
    };
    private static ThreadLocal<String> newUserName = new ThreadLocal<String>() {
        @Override
        protected synchronized String initialValue() {
            return "";
        }
    };


    @Override
    public int getExecutionOrderId() {
        return this.hashCode();
    }

    private int getDomainID(UserStoreManager userStoreManager) throws UserStoreException {

        int domainId = 0;
        String domainName = "";
        if (userStoreManager instanceof AbstractUserStoreManager) {
            AbstractUserStoreManager ausm =
                    (AbstractUserStoreManager) userStoreManager;


            domainName = ausm.getRealmConfiguration().getUserStoreProperty(UserCoreConstants.RealmConfig
                    .PROPERTY_DOMAIN_NAME).toUpperCase();
        }

        String domainIdQuery = "SELECT UM_DOMAIN_ID FROM UM_DOMAIN WHERE UM_DOMAIN_NAME=? AND UM_TENANT_ID=?";

        try {

            Connection indbConn = JDBCPersistenceManager.getInstance().getDBConnection();

            PreparedStatement domainIdStatement = indbConn.prepareStatement(domainIdQuery);

            domainIdStatement.setString(1, domainName);
            domainIdStatement.setInt(2, userStoreManager.getTenantId());

            ResultSet resultSet = domainIdStatement.executeQuery();

            while (resultSet.next()) {
                domainId = resultSet.getInt("UM_DOMAIN_ID");
            }

        } catch (SQLException | UserStoreException| IdentityException e) {
            throw new UserStoreException("Error while obtaining domain ID of the userstore", e);
        }
        return domainId;
    }


    private Connection getDBConnection() {

        try {


        } catch (Exception e) {

        }

        return null;
    }

    @Override
    public boolean doPreAuthenticate(String userName, Object o, UserStoreManager userStoreManager) throws
            UserStoreException {

        //--- Logic checking if this user has been authenticated before and UID created --//
        try {

            Connection indbConn = JDBCPersistenceManager.getInstance().getDBConnection();


            String uidQuery = "SELECT IDN_UID FROM IDN_UID_USER WHERE IDN_USERNAME = ? AND IDN_STORE_DOMAIN=? AND " +
                    "IDN_TENANT=?";

            PreparedStatement uidStatement = indbConn.prepareStatement(uidQuery);

            uidStatement.setString(1, userName);
            uidStatement.setInt(2, getDomainID(userStoreManager));
            uidStatement.setInt(3, userStoreManager.getTenantId());

            String uid = null;

            ResultSet resultSet = uidStatement.executeQuery();

            while (resultSet.next()) {
                uid = resultSet.getString("IDN_UID");
            }

            if (StringUtils.isEmpty(uid)) {
                createUIDForUser(userName, userStoreManager);
            }


        } catch (SQLException | IdentityException e) {
            throw new UserStoreException("Error while checking prior authentication", e);
        }

        //-- end of uid logic --//
        return true;
    }

    @Override
    public boolean doPostAuthenticate(String s, boolean b, UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPreAddUser(String s, Object o, String[] strings, Map<String, String> map, String s1, UserStoreManager userStoreManager) throws UserStoreException {
        isAddUserOperation.set(true);

        return true;
    }

    private String createUID(String userName) {

        UUID uuid = UUID.randomUUID();
        java.util.Date today = new java.util.Date();
        String uids = uuid.toString() + (today.getTime() % 310000);

        return uids;

    }


    private void createUIDForUser(String userName, UserStoreManager userStoreManager) throws UserStoreException {
        AbstractUserStoreManager aum = (AbstractUserStoreManager) userStoreManager;
        RealmConfiguration realmConfig = aum.getRealmConfiguration();
        String domainName = realmConfig.getUserStoreProperty(UserCoreConstants.RealmConfig.PROPERTY_DOMAIN_NAME);


        try {
            Connection dbCon = JDBCPersistenceManager.getInstance().getDBConnection();


            int domainID = getDomainID(userStoreManager);


            PreparedStatement insertToDB = dbCon.prepareStatement(INSERT_STMT);

            insertToDB.setString(1, createUID(userName));
            insertToDB.setString(2, userName);
            insertToDB.setInt(3, domainID);
            insertToDB.setInt(4, userStoreManager.getTenantId());

            insertToDB.execute();

            dbCon.commit();
            dbCon.close();


        } catch (SQLException | UserStoreException | IdentityException e) {
            throw new UserStoreException("Error while creating unique entry for user in embedded DB", e);
        }
    }

    @Override
    public boolean doPostAddUser(String userName, Object o, String[] strings, Map<String, String> map, String s1,
                                 UserStoreManager userStoreManager) throws UserStoreException {
        if (!isRenameOperation.get()) {
            createUIDForUser(userName, userStoreManager);
        }
        isAddUserOperation.set(false);
        return true;

    }

    @Override
    public boolean doPreUpdateCredential(String s, Object o, Object o1, UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostUpdateCredential(String s, Object o, UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPreUpdateCredentialByAdmin(String s, Object o, UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostUpdateCredentialByAdmin(String s, Object o, UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPreDeleteUser(String s, UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostDeleteUser(String userName, UserStoreManager userStoreManager) throws UserStoreException {
        if (!isRenameOperation.get()) {
            try {
                Connection indbConn = JDBCPersistenceManager.getInstance().getDBConnection();

                String deleteQuery = "DELETE FROM IDN_UID_USER WHERE IDN_USERNAME=? AND " +
                        "IDN_STORE_DOMAIN=? AND IDN_TENANT=?";
                PreparedStatement deleteStatement = indbConn.prepareStatement(deleteQuery);

                deleteStatement.setString(1, userName);
                deleteStatement.setInt(2, getDomainID(userStoreManager));
                deleteStatement.setInt(3, userStoreManager.getTenantId());

                deleteStatement.execute();

                indbConn.commit();
                indbConn.close();

            } catch (SQLException | UserStoreException | IdentityException e) {
                throw new UserStoreException("Error occurred while deleting user unique identifier", e);
            }
        }
        return true;
    }

    @Override
    public boolean doPreSetUserClaimValue(String s, String s1, String s2, String s3, UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostSetUserClaimValue(String s, UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPreSetUserClaimValues(String userName, Map<String, String> claims, String s1, UserStoreManager
            userStoreManager) throws UserStoreException {


        Iterator<Map.Entry<String, String>> it = claims.entrySet().iterator();
        while (it.hasNext()) {

            Map.Entry<String, String> claim = it.next();

            final String USER_NAME_CLAIM_URI = "http://wso2.org/claims/userName";

            if (claim.getKey().contains(USER_NAME_CLAIM_URI)) {
                if (!claim.getValue().equalsIgnoreCase(userName)) {
                    isRenameOperation.set(true);
                    newUserName.set(claim.getValue());
                }
                it.remove();
            }

        }

        return true;
    }

    @Override
    public boolean doPostSetUserClaimValues(String userName, Map<String, String> claims, String profileName, UserStoreManager
            userStoreManager) throws UserStoreException {


        if (isRenameOperation.get() && !isAddUserOperation.get()) {

            //----Begin Rename Logic----//


            AbstractUserStoreManager abstractUserStoreManager = (AbstractUserStoreManager) userStoreManager;


            claims.remove("profileConfiguration");

            Map<String, String> realmProperties = userStoreManager.getRealmConfiguration().getRealmProperties();

            String passwordString = "";

            if (userStoreManager instanceof ReadWriteLDAPUserStoreManager) {

                RealmConfiguration realmConfig = userStoreManager.getRealmConfiguration();

                LDAPConnectionContext connectionContext = new LDAPConnectionContext(realmConfig);

                // get the LDAP Directory context
                DirContext dirContext = connectionContext.getContext();
                DirContext subDirContext = null;
                // search the relevant user entry by user name
                String userSearchBase = realmConfig.getUserStoreProperty(LDAPConstants.USER_SEARCH_BASE);
                String userSearchFilter = realmConfig.getUserStoreProperty(LDAPConstants.USER_NAME_SEARCH_FILTER);

                String[] userNames = userName.split(CarbonConstants.DOMAIN_SEPARATOR);
                if (userNames.length > 1) {
                    userName = userNames[1];
                }
                userSearchFilter = userSearchFilter.replace("?", userName);

                SearchControls searchControls = new SearchControls();
                searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
                searchControls.setReturningAttributes(null);

                NamingEnumeration<SearchResult> returnedResultList = null;
                SearchResult res = null;
                String scimId;
                try {
                    returnedResultList = dirContext
                            .search(userSearchBase, userSearchFilter, searchControls);
                    res = returnedResultList.next();
                    Attributes allAttrs = res.getAttributes();

                    Object cred = allAttrs.get("userPassword").get();

                    scimId = allAttrs.get("scimId").get().toString();

                    byte[] passwordBytes = (byte[]) cred;

                    passwordString = new String(passwordBytes);


                } catch (NamingException e) {
                    String errorMessage = "Results could not be retrieved from the directory context for user : " + userName;
                    if (log.isDebugEnabled()) {
                        log.debug(errorMessage, e);
                    }
                    throw new UserStoreException(errorMessage, e);
                } finally {
                    JNDIUtil.closeNamingEnumeration(returnedResultList);
                }

                userStoreManager.addUser(newUserName.get(), passwordString, null, claims, profileName);

                Attributes updatedAttributes = new BasicAttributes();
                Attribute scimMod = new BasicAttribute("scimId", scimId);

                updatedAttributes.put(scimMod);

                try {
                    subDirContext = (DirContext) dirContext.lookup(userSearchBase);

                    String newUserEntry = "uid=?".replace("?", newUserName.get());

                    subDirContext.modifyAttributes(newUserEntry, DirContext.REPLACE_ATTRIBUTE,
                            updatedAttributes);
                } catch (NamingException e) {

                    throw new UserStoreException("Error while updating scimId of the user", e);
                }

                userStoreManager.deleteUser(userName);


            } else if (userStoreManager instanceof JDBCUserStoreManager) {

                realmProperties.get("");
                userStoreManager.getDefaultUserStoreProperties();
                RealmConfiguration realmConfig = userStoreManager.getRealmConfiguration();

                String connectionURL = realmConfig.getUserStoreProperty("url");
                String connectionName = realmConfig.getUserStoreProperty("userName");
                String connectionPassword = realmConfig.getUserStoreProperty("password");


                try {
                    Connection usDbCon = null;

                    Properties connectionProperties = new Properties();
                    connectionProperties.put("user", connectionName);
                    connectionProperties.put("password", connectionPassword);

                    usDbCon = DriverManager.getConnection(connectionURL, connectionProperties);

                    String renameQuery = "UPDATE UM_USER SET UM_USER_NAME=? WHERE UM_USER_NAME=? AND UM_TENANT_ID=?";

                    PreparedStatement renameStatement = usDbCon.prepareStatement(renameQuery);


                    renameStatement.setString(1, newUserName.get());
                    renameStatement.setString(2, userName);
                    renameStatement.setInt(3, userStoreManager.getTenantId());

                    renameStatement.execute();

                    usDbCon.close();

                } catch (SQLException e) {
                    throw new UserStoreException("Error while updating user information ", e);
                }
            }

            try {
                Connection indbConn =JDBCPersistenceManager.getInstance().getDBConnection();

                String renameQuery = "UPDATE IDN_UID_USER SET IDN_USERNAME=? WHERE IDN_USERNAME=? AND " +
                        "IDN_STORE_DOMAIN=? AND IDN_TENANT=?";
                PreparedStatement renameStatement = indbConn.prepareStatement(renameQuery);

                renameStatement.setString(1, newUserName.get());
                renameStatement.setString(2, userName);
                renameStatement.setInt(3, getDomainID(userStoreManager));
                renameStatement.setInt(4, userStoreManager.getTenantId());

                renameStatement.execute();

                indbConn.commit();
                indbConn.close();

            } catch (SQLException | IdentityException e) {
                throw new UserStoreException("Error while updating user UID information ", e);
            }


            //---- End of Rename Logic --//
            isRenameOperation.set(false);


        }

        return true;
    }

    @Override
    public boolean doPreDeleteUserClaimValues(String s, String[] strings, String s1, UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostDeleteUserClaimValues(String s, UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPreDeleteUserClaimValue(String s, String s1, String s2, UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostDeleteUserClaimValue(String s, UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPreAddRole(String s, String[] strings, Permission[] permissions, UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostAddRole(String s, String[] strings, Permission[] permissions, UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPreDeleteRole(String s, UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostDeleteRole(String s, UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPreUpdateRoleName(String s, String s1, UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostUpdateRoleName(String s, String s1, UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPreUpdateUserListOfRole(String s, String[] strings, String[] strings1, UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostUpdateUserListOfRole(String s, String[] strings, String[] strings1, UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPreUpdateRoleListOfUser(String s, String[] strings, String[] strings1, UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostUpdateRoleListOfUser(String s, String[] strings, String[] strings1, UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }
}