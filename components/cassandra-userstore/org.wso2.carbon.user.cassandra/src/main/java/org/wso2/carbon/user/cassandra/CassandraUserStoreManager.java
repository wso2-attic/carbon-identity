/*
 * Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.user.cassandra;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import javax.sql.DataSource;

import me.prettyprint.cassandra.model.BasicColumnFamilyDefinition;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.SliceQuery;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.cassandra.credentialtypes.AbstractCassandraCredential;
import org.wso2.carbon.user.core.Permission;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.claim.Claim;
import org.wso2.carbon.user.core.claim.ClaimManager;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.dto.RoleDTO;
import org.wso2.carbon.user.core.hybrid.HybridRoleManager;
import org.wso2.carbon.user.core.multiplecredentials.Credential;
import org.wso2.carbon.user.core.multiplecredentials.CredentialType;
import org.wso2.carbon.user.core.multiplecredentials.CredentialTypeNotSupportedException;
import org.wso2.carbon.user.core.multiplecredentials.MultipleCredentialUserStoreManager;
import org.wso2.carbon.user.core.multiplecredentials.UserAlreadyExistsException;
import org.wso2.carbon.user.core.multiplecredentials.UserDoesNotExistException;
import org.wso2.carbon.user.core.profile.ProfileConfigurationManager;
import org.wso2.carbon.user.core.tenant.Tenant;
import org.wso2.carbon.user.core.util.DatabaseUtil;
import org.wso2.carbon.user.core.util.JDBCRealmUtil;

public class CassandraUserStoreManager extends AbstractUserStoreManager implements
                                                                        MultipleCredentialUserStoreManager{

    private static final int MAX_CREDENTIALS_NO = 100;
    private static final int MAX_CLAIMS_PER_USER = 100000;
    private Cluster cluster;
    private Keyspace keyspace;

    private static Log log = LogFactory.getLog(CassandraUserStoreManager.class);
    private static boolean DEBUG = log.isDebugEnabled();
    private StringSerializer stringSerializer = StringSerializer.get();

    private static final int MAX_PASSWORDS_VALUE = 100;
    private static final int MAX_USERS_PER_ROLE = 100000;
    private static final int MAX_ROLES = 100000;


    protected DataSource jdbcDataSource = null;
    protected UserRealm jdbcUserRealm = null;
    protected int tenantId;
    protected boolean useOnlyInternalRoles;
    protected Random random = new Random();
    protected Map<String, CredentialType> credentialTypeMap = new HashMap<String, CredentialType>();


    public CassandraUserStoreManager(RealmConfiguration realmConfig, int tenantId)
            throws UserStoreException {
        this.realmConfig = realmConfig;
        Util.setRealmConfig(realmConfig);

        this.tenantId = tenantId;
        realmConfig.setUserStoreProperties(JDBCRealmUtil.getSQL(realmConfig
                                                                        .getUserStoreProperties()));
        if ("true".equals(realmConfig
                                  .getUserStoreProperty(UserCoreConstants.RealmConfig.PROPERTY_INTERNAL_ROLES_ONLY))) {
            useOnlyInternalRoles = true;
        }
        /*Initialize user roles cache as implemented in AbstractUserStoreManager*/
        initUserRolesCache();

        // TODO Abstract this out to a super class for multi credentials

        // create Map from Credential Types definition
        initCredTypesMap(credentialTypeMap);

        // set Cassandra specific properties
        cluster = HFactory.getOrCreateCluster(realmConfig.getUserStoreProperty(CFConstants.KEYSPACE_NAME_XML_ATTRIB),
                                              realmConfig.getUserStoreProperty(CFConstants.HOST_XML_ATTRIB) + ":" +
                                              realmConfig.getUserStoreProperty(CFConstants.PORT_XML_ATTRIB));

        keyspace = HFactory.createKeyspace(realmConfig.getUserStoreProperty(CFConstants.KEYSPACE_NAME_XML_ATTRIB), cluster);



        // injecting Cassandra specific values to the Credential types
        for (CredentialType credentialType : credentialTypeMap.values()) {
            ((AbstractCassandraCredential) credentialType).setKeyspace(keyspace);
        }

        insertInitialData(keyspace);

    }

    private void initCredTypesMap(Map<String, CredentialType> credentialTypeMap) throws UserStoreException {
        Map<String, String> credsMapFromConfig = realmConfig.getMultipleCredentialProps().get(this.getClass().getName());
        if (credsMapFromConfig != null) {
            for (Map.Entry<String, String> credentialClasses : credsMapFromConfig.entrySet()) {
                Class credentialsClass = null;
                try {
                    credentialsClass = this.getClass().getClassLoader().loadClass(credentialClasses.getValue());
                    CredentialType credentialType = (CredentialType) credentialsClass.newInstance();
                    String credentialTypeName = credentialClasses.getKey();
                    credentialType.setCredentialTypeName(credentialTypeName);
                    credentialTypeMap.put(credentialTypeName, credentialType);
                } catch (ClassNotFoundException e) {

                    String message = "Unable to instantiate credentials type class. " + e.getMessage();
                    log.error(message, e);
                    throw new UserStoreException(message, e);
                } catch (InstantiationException e) {
                    String message = "Unable to instantiate credentials type class. " + e.getMessage();
                    log.error(message, e);
                    throw new UserStoreException(message, e);
                } catch (IllegalAccessException e) {
                    String message = "Unable to instantiate credentials type class. " + e.getMessage();
                    log.error(message, e);
                    throw new UserStoreException(message, e);
                }
            }
        }
    }

    public CassandraUserStoreManager(RealmConfiguration realmConfig, Map<String, Object> properties,
                                     ClaimManager claimManager, ProfileConfigurationManager profileManager, UserRealm realm,
                                     Integer tenantId) throws UserStoreException {
        this(realmConfig, tenantId);
        if (log.isDebugEnabled()) {
            log.debug("Started " + System.currentTimeMillis());
        }
        this.claimManager = claimManager;
        this.profileManager = profileManager;
        this.jdbcUserRealm = realm;

        if ("true".equals(realmConfig
                                  .getUserStoreProperty(UserCoreConstants.RealmConfig.PROPERTY_INTERNAL_ROLES_ONLY))) {
            useOnlyInternalRoles = true;
        }

        dataSource = (DataSource) properties.get(UserCoreConstants.DATA_SOURCE);
        if (dataSource == null) {
            dataSource = DatabaseUtil.getRealmDataSource(realmConfig);
        }
        if (dataSource == null) {
            throw new UserStoreException("User Management Data Source is null");
        }

        properties.put(UserCoreConstants.DATA_SOURCE, dataSource);

        if (log.isDebugEnabled()) {
            log.debug("The jdbcDataSource being used by JDBCUserStoreManager :: "
                      + dataSource.hashCode());
        }
        realmConfig.setUserStoreProperties(JDBCRealmUtil.getSQL(realmConfig
                                                                        .getUserStoreProperties()));
        hybridRoleManager = new HybridRoleManager(dataSource, tenantId, realmConfig, jdbcUserRealm);
        this.addInitialData();
        if (log.isDebugEnabled()) {
            log.debug("Ended " + System.currentTimeMillis());
        }
        /*Initialize user roles cache as implemented in AbstractUserStoreManager*/
        initUserRolesCache();
    }

    private void addInitialData() throws UserStoreException {


//        boolean isAdminRoleAdded = false;
//        if (!isExistingRole(realmConfig.getAdminRoleName())) {
//            this.addRole(realmConfig.getAdminRoleName(), null, null);
//            isAdminRoleAdded = true;
//        }
//
//        if (!isExistingRole(realmConfig.getEveryOneRoleName())) {
//            this.addRole(realmConfig.getEveryOneRoleName(), null, null);
//        }
//
//        String adminUserName = getAdminUser();
//        if (adminUserName != null) {
//            realmConfig.setAdminUserName(adminUserName);
//        } else {
//            if (!isExistingUser(realmConfig.getAdminUserName())) {
//                if ("true".equals(realmConfig
//                                          .getUserStoreProperty(UserCoreConstants.RealmConfig.PROPERTY_READ_ONLY))) {
//                    log.error("Admin user name is not valid");
//                    throw new UserStoreException("Admin user name is not valid");
//                }
//                // it is not required to notify to the listeners, just persist data.
//                this.doAddUser(realmConfig.getAdminUserName(), realmConfig.getAdminPassword(),
//                                 null, null, null, false);
//            }
//        }
//
//        // use isUserInRole method
//        if (isAdminRoleAdded) {
//            this.updateRoleListOfUser(realmConfig.getAdminUserName(), null,
//                                      new String[] { realmConfig.getAdminRoleName() });
//        }
//
//        // anonymous user and role
//        if (!isExistingUser(CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME) && !this.isReadOnly()) {
//            byte[] password = new byte[12];
//            random.nextBytes(password);
//            this.doAddUser(CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME, Base64.encode(password),
//                             null, null, null, false);
//
//        }
//        // if the realm is read only the role will be hybrid
//        if (!isExistingRole(CarbonConstants.REGISTRY_ANONNYMOUS_ROLE_NAME)) {
//            this.addRole(CarbonConstants.REGISTRY_ANONNYMOUS_ROLE_NAME,
//                         new String[] { CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME }, null);
//        }

    }

    /**
     * @deprecated
     *
     * Returns the admin users for the given tenant.
     * @return the admin user.
     * @throws org.wso2.carbon.user.core.UserStoreException from the getUserNames()
     */
    public String getAdminUser() throws UserStoreException {
        String[] users = getUserListOfRole(this.realmConfig.getAdminRoleName());
        if (users != null && users.length > 0) {
            return users[0];
        }
        return null;
    }


    public void insertInitialData(Keyspace keyspace) throws UserStoreException {


        List<KeyspaceDefinition> keyspaceDefinitions = cluster.describeKeyspaces();


        boolean foundKS = false;
        String keyspaceName = keyspace.getKeyspaceName();
        for (KeyspaceDefinition keyspaceDefinition : keyspaceDefinitions) {
            if (keyspaceDefinition.getName().equals(keyspaceName)) {
                foundKS = true;
            }
        }

        if (!foundKS) {
            // add keyspace
            KeyspaceDefinition keyspaceDefinition = HFactory.createKeyspaceDefinition(keyspaceName);

            cluster.addKeyspace(keyspaceDefinition, true);

            // add role cf
            ColumnFamilyDefinition roleCF = new BasicColumnFamilyDefinition();
            roleCF.setName(CFConstants.ROLES);
            roleCF.setKeyspaceName(keyspaceName);
            cluster.addColumnFamily(roleCF, true);

            // add user roles index

            ColumnFamilyDefinition userRolesIndexCF = new BasicColumnFamilyDefinition();
            userRolesIndexCF.setName(CFConstants.USERNAME_ROLES_INDEX);
            userRolesIndexCF.setKeyspaceName(keyspaceName);
            cluster.addColumnFamily(userRolesIndexCF, true);


            // add user cf
            ColumnFamilyDefinition userCF = new BasicColumnFamilyDefinition();
            userCF.setName(CFConstants.USERS);
            userCF.setKeyspaceName(keyspaceName);
            cluster.addColumnFamily(userCF, true);

            // add username index cf
            ColumnFamilyDefinition usernameIndexCF = new BasicColumnFamilyDefinition();
            usernameIndexCF.setName(CFConstants.USERNAME_INDEX);
            usernameIndexCF.setKeyspaceName(keyspaceName);
            cluster.addColumnFamily(usernameIndexCF, true);

            // add password index cf
            ColumnFamilyDefinition passwordIndexCF = new BasicColumnFamilyDefinition();
            passwordIndexCF.setName(CFConstants.PASSWORD_INDEX);
            passwordIndexCF.setKeyspaceName(keyspaceName);
            cluster.addColumnFamily(passwordIndexCF, true);

            // add claims CF
            ColumnFamilyDefinition claimsCF = new BasicColumnFamilyDefinition();
            claimsCF.setName(CFConstants.CLAIMS);
            claimsCF.setKeyspaceName(keyspaceName);
            cluster.addColumnFamily(claimsCF, true);




            // add user

            Credential credential = new Credential();
            credential.setCredentialsType(CFConstants.DEFAULT_TYPE);
            credential.setIdentifier(realmConfig.getAdminUserName());
            credential.setSecret(realmConfig.getAdminPassword());

            this.addUser(credential, new String[]{ realmConfig.getAdminRoleName()}, null, null);
//            this.doAddUser("admin", "admin", new String[] { "admin" }, null, null);
//            mutator.addInsertion(userId, CFConstants.USERS, HFactory.createColumn("emailAddress", "admin"));
//
//            mutator.addInsertion("admin", CFConstants.USERNAME_INDEX, HFactory.createColumn(CFConstants.USER_ID, userId ));
//
//            mutator.addInsertion(userId, CFConstants.PASSWORD_INDEX, HFactory.createColumn("emailAddress", "admin" ));
//
//            mutator.addInsertion("admin", CFConstants.ROLES, HFactory.createColumn("admin", "" ));
//
//            mutator.addInsertion("admin", CFConstants.USERNAME_ROLES_INDEX, HFactory.createColumn("admin", "" ));
//
//            mutator.execute();

//            jdbcUserRealm.getAuthorizationManager().authorizeRole("admin");

        }
        String msg = "Connected to Cassandra keyspace : " + keyspace.getKeyspaceName() + ". ";
        if (foundKS) {
            msg += " Keyspace already found. Not creating any column families or intialization data.";
        } else {
            msg += " Keyspace not found. Creating all column families and adding initialization data.";
        }
        log.info(msg);
    }

//    /**
//     * Creates a credential object using passed claims for adduser
//     *
//     * @param claims
//     * @return
//     * @throws UserStoreException
//     */
//    private TNCredential buildTNCredential(Map<String, String> claims) throws UserStoreException {
//
//        log.info("Reading credentials");
//
//        TNCredential cred = new TNCredential();
//
//        String property;
//        String propValue;
//
//        if (claims != null) {
//            Iterator<Map.Entry<String, String>> ite = claims.entrySet().iterator();
//            while (ite.hasNext()) {
//                Map.Entry<String, String> entry = ite.next();
//
//                property = entry.getKey();
//                propValue = entry.getValue();
//
//                if (TNConstants.CREDENTIAL_TYPE.equals(property)) {
//                    cred.setCredentialType(propValue);
//                } else if (TNConstants.CRED_DEVICE_ID.equals(property)) {
//                    cred.setDeviceID(propValue);
//                } else if (TNConstants.CRED_EXT_USERNAME.equals(property)) {
//                    cred.setExtUsername(propValue);
//                } else if (TNConstants.CRED_EXT_PROVIDER.equals(property)) {
//                    cred.setExtProvider(propValue);
//                } else if (TNConstants.CRED_EXT_ACCESS_TOKEN.equals(property)) {
//                    cred.setExtAccessToken(propValue);
//                } else if (TNConstants.CRED_EXT_EMAIL_ADDRESS.equals(property)) {
//                    cred.setExtEmailAddress(propValue);
//                } else if (TNConstants.CRED_PHONE_NUMBER.equals(property)) {
//                    cred.setPhoneNumber(propValue);
//                } else if (TNConstants.CRED_EMAIL_ADDRESS.equals(property)) {
//                    cred.setEmail(propValue);
//                } else if (TNConstants.CRED_PASSWORD.equals(property)) {
//                    cred.setPassword(propValue);
//                }
//            }
//        } else {
//            throw new UserStoreException("No credentials found. Unable to add user");
//        }
//
//        return cred;
//    }

    @Override
    public Map<String, String> getUserPropertyValues(String userName, String[] propertyNames,
                                                     String profileName) throws UserStoreException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected boolean doCheckExistingRole(String s) throws UserStoreException {
        throw new UserStoreException("doCheckExistingRole(String) not implemented for CassandraUserStoreManager");
    }

    @Override
    protected boolean doCheckExistingUser(String s) throws UserStoreException {
        throw new UserStoreException("doCheckExistingUser(String) not implemented for CassandraUserStoreManager");
    }

    @Override
    public String[] getUserListFromProperties(String property, String value, String profileName)
            throws UserStoreException {
        return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean doAuthenticate(String userName, Object credential) throws UserStoreException {
    	String credentialType = CFConstants.DEFAULT_TYPE;
    	if(userName.contains(":")) {
    		String[] cred = userName.split(":");
    		credentialType = cred[0];
    		userName = cred[1];
    	}
    	if(DEBUG) {
    		log.debug("Authenticating " + userName);
    	}
    	Credential credentialObj = new Credential();
        credentialObj.setCredentialsType(credentialType);
        credentialObj.setIdentifier(userName);
        // this is a bad cast, but this has to be done to be compatible with this legacy interface
        credentialObj.setSecret((String) credential);
        
		if (credentialObj.getSecret() == null
				|| credentialObj.getSecret().trim().length() == 0 ||
                                    getCredentialType(credentialObj).isNullSecretAllowed()) {
			return getCredentialType(credentialObj).authenticate(credentialObj);
		} else {
			return authenticate(credentialObj);
		}
        
        //return getCredentialType(credentialObj).authenticate(credentialObj);
//
//        String userId = getExistingUserId(userName, CFConstants.DEFAULT_TYPE);
//
//        if (userId == null) {
//            return false;
//        }
//
//        // if device id or external provider id user login is now successful
//        if (TNConstants.TYPE_DEVICE_ID.equals(credType) || (TNConstants.TYPE_EXT_LOGIN.equals(credType))) {
//            return true;
//        }
//
//
//        SliceQuery<String,String,String> passwordIndexQuery = HFactory
//                .createSliceQuery(keyspace, stringSerializer, stringSerializer,
//                                  stringSerializer);
//        passwordIndexQuery.setRange("", "", false, MAX_PASSWORDS_VALUE)
//                .setColumnFamily(CFConstants.PASSWORD_INDEX)
//                .setKey(userId);
//
//
//        ColumnSlice<String, String> passwordResults = passwordIndexQuery.execute().get();
//        for (HColumn<String, String> passwordColumns : passwordResults.getColumns()) {
//            if (credential.equals(passwordColumns.getValue())) {
//                return true;
//            }
//        }
//
//        return false;


    }



    @Override
    public void doAddUser(String userName, Object credential, String[] roleList,
                          Map<String, String> claims, String profileName)
            throws UserStoreException {
    	if(DEBUG) {
    		log.debug("Adding  " + userName);
    	}
        Credential newCredential = new Credential();
        newCredential.setIdentifier(userName);
        newCredential.setSecret((String) credential);
        newCredential.setCredentialsType(CFConstants.DEFAULT_TYPE);
        this.addUser(newCredential, roleList, claims, profileName);

    }

    private void addUserToRole(String userName, String[] roleList) {
        Mutator<String> mutator = HFactory.createMutator(keyspace, stringSerializer);
        for (String role : roleList) {
            // add user to role list
            mutator.addInsertion(userName, CFConstants.USERNAME_ROLES_INDEX,
                                 HFactory.createColumn(role, ""));
            mutator.addInsertion(role, CFConstants.ROLES, HFactory.createColumn(userName, ""));
        }
        mutator.execute();
    }

    private void deleteUserFromRole(String userid) throws UserStoreException {
        Mutator<String> mutator = HFactory.createMutator(keyspace, stringSerializer);

        String[] roleList = getRoleListOfUserForUserId(userid);

        for (String role : roleList) {
            mutator.addDeletion(role, CFConstants.ROLES, userid, stringSerializer);
        }

        mutator.addDeletion(userid, CFConstants.USERNAME_ROLES_INDEX, null, stringSerializer);

        mutator.execute();
    }

//    private void overwriteCredential(String userId, TNCredential cred) {

//        Mutator<String> mutator = HFactory.createMutator(keyspace, stringSerializer);
//
//
//
//        if (cred.getCredentialType().equals(TNConstants.TYPE_DEVICE_ID)) {
//
//            // add user
//            mutator.addInsertion(userId, CFConstants.USERS, HFactory.createColumn("deviceId", cred.getDeviceID()))
//                    .addInsertion(userId, CFConstants.USERS, HFactory.createColumn("isActive", "TRUE"));
//
//            // add reverse look up
//            mutator.addInsertion(cred.getDeviceID(), CFConstants.USERNAME_INDEX, HFactory.createColumn(CFConstants.USER_ID, userId));
//
//        } else if (cred.getCredentialType().equals(TNConstants.TYPE_EXT_LOGIN)) {
//
//            log.info("Registering external user " + cred.getExtUsername() + " of " +
//                     cred.getExtProvider());
//
//            // add external user
//            mutator.addInsertion(userId, CFConstants.USERS, HFactory.createColumn("extUsername", cred.getDeviceID()))
//                    .addInsertion(userId, CFConstants.USERS, HFactory.createColumn("extProvider", cred.getExtProvider()))
//                    .addInsertion(userId, CFConstants.USERS, HFactory.createColumn("extAccessToken", cred.getExtAccessToken()))
//                    .addInsertion(userId, CFConstants.USERS, HFactory.createColumn("extEmailAddress", cred.getExtEmailAddress()))
//                    .addInsertion(userId, CFConstants.USERS, HFactory.createColumn("isActive", "TRUE"));
//
//
//            // add reverse look up - external username
//            mutator.addInsertion(cred.getExtProvider() + ":" +
//                                 cred.getExtUsername(), CFConstants.USERNAME_INDEX,
//                                 HFactory.createColumn(CFConstants.USER_ID, userId));
//
//            // add reverse look up - external email
//            mutator.addInsertion(cred.getExtProvider() + ":" + cred.getExtEmailAddress(),
//                                 CFConstants.USERNAME_INDEX,
//                                 HFactory.createColumn(CFConstants.USER_ID, userId));
//
//            // add reverse look up - password
//            mutator.addInsertion(userId, CFConstants.PASSWORD_INDEX,
//                                 HFactory.createColumn(
//                                         cred.getExtEmailAddress() + "password",
//                                         Integer.toString(new Random().nextInt())));
//
//
//        } else if (cred.getCredentialType().equals(TNConstants.TYPE_PHONE_NUMBER)) {
//
//            log.info("Registering phone number " + cred.getPhoneNumber());
//
//            // add phone user
//            mutator.addInsertion(userId, CFConstants.USERS, HFactory.createColumn("phoneNo", cred.getPhoneNumber()))
//                    .addInsertion(userId, CFConstants.USERS, HFactory.createColumn("isActive", "TRUE"));
//
//            // add reverse lookup - phone user
//
//            mutator.addInsertion(cred.getPhoneNumber(), CFConstants.USERNAME_INDEX, HFactory.createColumn(CFConstants.USER_ID, userId));
//
//            // add reverse lookup -  password
//
//            mutator.addInsertion(userId, CFConstants.PASSWORD_INDEX,
//                                 HFactory.createColumn(cred.getPhoneNumber() + "password", cred.getPassword()));
//
//
//        } else if (cred.getCredentialType().equals(TNConstants.TYPE_EMAIL)) {
//
//            log.info("Registering email " + cred.getEmail());
//
//            // add email user
//            mutator.addInsertion(userId, CFConstants.USERS, HFactory.createColumn("emailAddress", cred.getEmail()))
//                    .addInsertion(userId, CFConstants.USERS, HFactory.createColumn("isActive", "TRUE"));
//
//            // add reverse lookup - email user w/ password
//
//            mutator.addInsertion(cred.getEmail(), CFConstants.USERNAME_INDEX, HFactory.createColumn(CFConstants.USER_ID, userId));
//
//            // add reverse lookup -  password
//
//            mutator.addInsertion(userId, CFConstants.PASSWORD_INDEX,
//                                 HFactory.createColumn(cred.getEmail() + "password", cred.getPassword()));
//
//        }
//        mutator.execute();

//    }

    @Override
    public Claim[] getUserClaimValues(String userName, String profileName)
            throws UserStoreException {
    	if(DEBUG) {
    		log.debug("Getting claims for " + userName);
    	}
        String credentialType = CFConstants.DEFAULT_TYPE;
        if(userName.contains(":")) {
            String[] cred = userName.split(":");
            credentialType = cred[0];
            userName = cred[1];
        }

        return getUserClaimValues(userName, credentialType, profileName);

    }

    @Override
    public String[] getAllProfileNames() throws UserStoreException {
        return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isReadOnly() throws UserStoreException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addRole(String roleName, String[] userList,
                        org.wso2.carbon.user.api.Permission[] permissions)
            throws org.wso2.carbon.user.api.UserStoreException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String[] getHybridRoles() throws UserStoreException {
        return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Date getPasswordExpirationTime(String username) throws UserStoreException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getUserId(String username) throws UserStoreException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getTenantId(String username) throws UserStoreException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getTenantId() throws UserStoreException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Map<String, String> getProperties(org.wso2.carbon.user.api.Tenant tenant)
            throws org.wso2.carbon.user.api.UserStoreException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isMultipleProfilesAllowed() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addRememberMe(String userName, String token)
            throws org.wso2.carbon.user.api.UserStoreException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isValidRememberMeToken(String userName, String token)
            throws org.wso2.carbon.user.api.UserStoreException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Map<String, String> getProperties(Tenant tenant) throws UserStoreException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isBulkImportSupported() throws UserStoreException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public RoleDTO[] getRoleNamesWithDomain() throws UserStoreException {
        return new RoleDTO[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public RealmConfiguration getRealmConfiguration() {
        return realmConfig;
    }

    @Override
    public void doAddUser(String userName, Object credential, String[] roleList,
                          Map<String, String> claims, String profileName,
                          boolean requirePasswordChange) throws UserStoreException {
        this.doAddUser(userName, credential, roleList, claims, profileName);
    }

    @Override
    public void doUpdateCredential(String userName, Object newCredential, Object oldCredential)
            throws UserStoreException {

    }

    @Override
    public void doUpdateCredentialByAdmin(String userName, Object newCredential)
            throws UserStoreException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void doDeleteUser(String userName) throws UserStoreException {
        String credentialType = CFConstants.DEFAULT_TYPE;
        if(userName.contains(":")) {
            String[] cred = userName.split(":");
            credentialType = cred[0];
            userName = cred[1];
        }

        this.deleteUser(userName, credentialType);
    }

    @Override
    public void doSetUserClaimValue(String userName, String claimURI, String claimValue,
                                    String profileName) throws UserStoreException {
        String credentialType = CFConstants.DEFAULT_TYPE;
        if(userName.contains(":")) {
            String[] cred = userName.split(":");
            credentialType = cred[0];
            userName = cred[1];
        }

        setUserClaimValue(userName, credentialType, claimURI, claimValue, profileName);
    }

    @Override
    public void doSetUserClaimValues(String userName, Map<String, String> claims,
                                     String profileName) throws UserStoreException {

        String credentialType = CFConstants.DEFAULT_TYPE;
        if(userName.contains(":")) {
            String[] cred = userName.split(":");
            credentialType = cred[0];
            userName = cred[1];
        }

        setUserClaimValues(userName, credentialType, claims, profileName);

    }

    @Override
    public void doDeleteUserClaimValue(String userName, String claimURI, String profileName)
            throws UserStoreException {
        String credentialType = CFConstants.DEFAULT_TYPE;
        if(userName.contains(":")) {
            String[] cred = userName.split(":");
            credentialType = cred[0];
            userName = cred[1];
        }
        deleteUserClaimValue(userName, credentialType, claimURI, profileName);
    }

    @Override
    public void doDeleteUserClaimValues(String userName, String[] claims, String profileName)
            throws UserStoreException {
        String credentialType = CFConstants.DEFAULT_TYPE;
        if(userName.contains(":")) {
            String[] cred = userName.split(":");
            credentialType = cred[0];
            userName = cred[1];
        }

        deleteUserClaimValues(userName, credentialType, claims, profileName);
    }

    @Override
    public void doUpdateUserListOfRole(String roleName, String[] deletedUsers, String[] newUsers)
            throws UserStoreException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void doUpdateRoleListOfUser(String userName, String[] deletedRoles, String[] newRoles)
            throws UserStoreException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String[] getInternalRoleListOfUser(String s) throws UserStoreException {
        return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<String> getExternalRoleListOfUser(String s) throws UserStoreException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void doAddRole(String s, String[] strings, org.wso2.carbon.user.api.Permission[] permissions) throws UserStoreException {
        throw new UserStoreException("doAddRole(String,String[],Permission[]) not implemented for CassandraUserStoreManager");
    }

    @Override
    public void doAddRole(String roleName, String[] userList, Permission[] permissions)
            throws UserStoreException {

        Mutator<String> mutator = HFactory.createMutator(keyspace, stringSerializer);

        for (String user : userList) {
            HColumn<String, String> userCol = HFactory.createColumn(user, "");
            mutator.addInsertion(roleName, CFConstants.ROLES, userCol);
        }

        mutator.execute();

        if (permissions != null) {
            for (Permission permission : permissions) {
                String resourceId = permission.getResourceId();
                String action = permission.getAction();
                jdbcUserRealm.getAuthorizationManager().authorizeRole(roleName, resourceId, action);
            }
        }

//        //if existing users are added to role, need to update user role cache
//        if ((userList != null) && (userList.length > 0)) {
//            clearUserRolesCacheByTenant(this.tenantId);
//        }
    }

    @Override
    public void doDeleteRole(String roleName) throws UserStoreException {

        Mutator<String> mutator = HFactory.createMutator(keyspace, stringSerializer);

        for (String users : getUserListOfRoleAsList(roleName)) {
            mutator.addDeletion(users, CFConstants.USERNAME_ROLES_INDEX, roleName, stringSerializer);
        }

        // add role deletion
        mutator.addDeletion(roleName, CFConstants.ROLES, null, stringSerializer);
        mutator.execute();
    }

    @Override
    public void doUpdateRoleName(String roleName, String newRoleName) throws UserStoreException {
        doAddRole(newRoleName, getUserListOfRole(roleName), null);
        doDeleteRole(roleName);

    }

    @Override
    protected String[] doGetRoleNames(String s, int i) throws UserStoreException {
        throw new UserStoreException("doGetRoleNames(String,int) not implemented for CassandraUserStoreManager");
    }

    @Override
    protected String[] doListUsers(String s, int i) throws UserStoreException {
        throw new UserStoreException("doListUsers(String,int) not implemented for CassandraUserStoreManager");
    }

    @Override
    protected String[] doGetDisplayNamesForInternalRole(String[] strings) throws UserStoreException {
        throw new UserStoreException("doGetDisplayNamesForInternalRole(String[]) not implemented for CassandraUserStoreManager");
    }

    @Override
    public String[] listUsers(String filter, int maxItemLimit) throws UserStoreException {
        return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isExistingUser(String userName) throws UserStoreException {
        if (CarbonConstants.REGISTRY_SYSTEM_USERNAME.equals(userName)) {
            return true;
        }

        if (getExistingUserId(userName, CFConstants.DEFAULT_TYPE) == null) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public boolean isExistingRole(String roleName) throws UserStoreException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String[] getRoleNames() throws UserStoreException {
        return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String[] getRoleNames(boolean noHybridRoles) throws UserStoreException {
        return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected String[] doGetUserListOfRole(String s, String s2) throws UserStoreException {
        throw new UserStoreException("doGetUserListOfRole(String,String) not implemented for CassandraUserStoreManager");
    }

    @Override
    public String[] getProfileNames(String userName) throws UserStoreException {
        return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    private String getExistingUserId(Credential credential) {
        return getExistingUserId(credential.getIdentifier(), credential.getCredentialsType());
    }

    private String getExistingUserId(String identifier, String credentialTypeName) {
        return Util.getExistingUserId(credentialTypeName, identifier, keyspace);
    }

    @Override
    public String[] getRoleListOfUser(String userName) throws UserStoreException {

        return getRoleListOfUserForUserId(getExistingUserId(userName, CFConstants.DEFAULT_TYPE));
    }

    @Override
    public String[] getRoleListOfUser(String identifer, String credentialType)
            throws UserStoreException {
        return getRoleListOfUserForUserId(getExistingUserId(identifer, credentialType));
    }

    @Override
    public void addUserWithUserId(String userId, Credential credential, String[] roleList,
                                  Map<String, String> claimMap, String profileName) throws UserStoreException {
        if (getExistingUserId(credential) != null) {
            String msg = "User already exists for identifer : " + credential.getIdentifier() +
                         " for credential type : " + credential.getCredentialsType();
            UserAlreadyExistsException userAlreadyExistsException = new UserAlreadyExistsException(msg);
            log.error(msg, userAlreadyExistsException);
            throw userAlreadyExistsException;
        }
        if(DEBUG) {
        	log.debug("Adding user " + credential.getIdentifier());
        }
        CredentialType credentialType = getCredentialType(credential);
        credentialType.add(userId, credential);
        addUserToRole(userId, roleList);

        if (claimMap != null) {
            addClaimsForUser(userId, claimMap);
        }
        // TODO add profile support

    }

    @Override
    public String getUserId(Credential credential) {
    	if(DEBUG) {
    		log.debug("Getting user ID for " + credential.getIdentifier());
    	}
        return getExistingUserId(credential);
    }

    @Override
    public void setUserClaimValues(String identifer, String credentialType,
                                   Map<String, String> claims, String profileName)
            throws UserStoreException {
        String existingUserId = getExistingUserId(identifer, credentialType);
        Mutator<String> mutator = HFactory.createMutator(keyspace, stringSerializer);
        if(DEBUG) {
        	log.debug("Adding claims for " + identifer);
        }
        // add claims
        for (Map.Entry<String, String> claimEntry : claims.entrySet()) {

            mutator.addInsertion(existingUserId, CFConstants.CLAIMS,
                                 HFactory.createColumn(claimEntry.getKey(), claimEntry.getValue()));
        }
        mutator.execute();
    }

    @Override
    public void setUserClaimValue(String identifer, String credentialType, String claimURI,
                                  String claimValue, String profileName) throws UserStoreException {
		if (DEBUG) {
			log.debug("Setting " + claimURI + " for " + identifer);
		}
        String existingUserId = getExistingUserId(identifer, credentialType);
        Mutator<String> mutator = HFactory.createMutator(keyspace, stringSerializer);
        // add claims
        mutator.addInsertion(existingUserId, CFConstants.CLAIMS, HFactory.createColumn(claimURI, claimValue));
        mutator.execute();
    }

    @Override
    public void deleteUserClaimValue(String identifer, String credentialType, String claimURI,
                                     String profileName) throws UserStoreException {
    	if(DEBUG) {
    		log.debug("Deleting claims for " + identifer);
    	}
        String existingUserId = getExistingUserId(identifer, credentialType);

        Mutator<String> mutator = HFactory.createMutator(keyspace, stringSerializer);

        // delete claim value
        mutator.addDeletion(existingUserId , CFConstants.CLAIMS, claimURI, stringSerializer);
        mutator.execute();
    }

    @Override
    public void deleteUserClaimValues(String identifer, String credentialType, String[] claims,
                                      String profileName) throws UserStoreException {
        String existingUserId = getExistingUserId(identifer, credentialType);

        Mutator<String> mutator = HFactory.createMutator(keyspace, stringSerializer);

        // delete claim value
        for (String claim : claims) {
            mutator.addDeletion(existingUserId , CFConstants.CLAIMS, claim, stringSerializer);
        }
        mutator.execute();
    }

    @Override
    public String getUserClaimValue(String identifer, String credentialType, String claimUri,
                                    String profileName) throws UserStoreException {
    	if(DEBUG) {
    		log.debug("Getting claims for " + identifer);
    	}
        String existingUserId = getExistingUserId(identifer, credentialType);
        ColumnQuery<String, String, String> claimValQuery = HFactory
                .createColumnQuery(keyspace, stringSerializer, stringSerializer, stringSerializer);

        claimValQuery.setColumnFamily(CFConstants.CLAIMS).setKey(existingUserId)
                .setName(claimUri);
        HColumn<String, String> claimValResult = claimValQuery.execute().get();

        if (claimValResult == null) {
            return null;
        }

        return claimValResult.getValue();
    }

    @Override
    public Claim[] getUserClaimValues(String identifer, String credentialType, String[] claims,
                                      String profileName) throws UserStoreException {
        String existingUserId = getExistingUserId(identifer, credentialType);
        SliceQuery<String,String,String> claimsQuery = HFactory
                .createSliceQuery(keyspace, stringSerializer, stringSerializer,
                                  stringSerializer);
        claimsQuery.setColumnNames(claims)
                .setColumnFamily(CFConstants.CLAIMS)
                .setKey(existingUserId);

        List<Claim> claimsList = new ArrayList<Claim>();
        ColumnSlice<String, String> claimsColSlice = claimsQuery.execute().get();
        if (claimsColSlice == null) {
            return new Claim[0];
        }
        for (HColumn<String, String> entry : claimsColSlice.getColumns()) {
            Claim claim = new Claim();
            claim.setValue(entry.getValue());
            claim.setClaimUri(entry.getName());
//            String displayTag;
//            try {
//                displayTag = claimManager.getClaim(entry.getName()).getDisplayTag();
//            } catch (org.wso2.carbon.user.api.UserStoreException e) {
//                throw new UserStoreException(e);
//            }
//            claim.setDisplayTag(displayTag);
            claimsList.add(claim);
        }

        return claimsList.toArray(new Claim[claimsList.size()]);
    }

    @Override
    public Claim[] getUserClaimValues(String identifer, String credentialType, String profileName)
            throws UserStoreException {
        String existingUserId = getExistingUserId(identifer, credentialType);
        SliceQuery<String,String,String> claimsQuery = HFactory
                .createSliceQuery(keyspace, stringSerializer, stringSerializer,
                                  stringSerializer);
        claimsQuery.setRange("", "", false, MAX_CLAIMS_PER_USER)
                .setColumnFamily(CFConstants.CLAIMS)
                .setKey(existingUserId);


        List<Claim> claimsList = new ArrayList<Claim>();
        ColumnSlice<String, String> claimsColSlice = claimsQuery.execute().get();
        if (claimsColSlice == null) {
            return new Claim[0];
        }
        for (HColumn<String, String> entry : claimsColSlice.getColumns()) {
            Claim claim = new Claim();
            claim.setValue(entry.getValue());
            claim.setClaimUri(entry.getName());
//            String displayTag;
//            try {
//                displayTag = claimManager.getClaim(entry.getName()).getDisplayTag();
//            } catch (org.wso2.carbon.user.api.UserStoreException e) {
//                throw new UserStoreException(e);
//            }
//            claim.setDisplayTag(displayTag);
            claimsList.add(claim);
        }

        return claimsList.toArray(new Claim[claimsList.size()]);
    }

    public String[] getRoleListOfUserForUserId(String userId) throws UserStoreException {

        SliceQuery<String,String,String> roleListQuery = HFactory
                .createSliceQuery(keyspace, stringSerializer, stringSerializer,
                                  stringSerializer);
        roleListQuery.setRange("", "", false, MAX_USERS_PER_ROLE)
                .setColumnFamily(CFConstants.USERNAME_ROLES_INDEX)
                .setKey(userId);


        List<String> rolesList = new ArrayList<String>();
        ColumnSlice<String, String> rolesForUserList = roleListQuery.execute().get();
        for (HColumn<String, String> roleForUser : rolesForUserList.getColumns()) {
            rolesList.add(roleForUser.getName());
        }

        return rolesList.toArray(new String[rolesList.size()]);
    }

    @Override
    public String[] getUserListOfRole(String roleName) throws UserStoreException {

        List<String> userList = getUserListOfRoleAsList(roleName);

        return userList.toArray(new String[userList.size()]);
    }

    private List<String> getUserListOfRoleAsList(String roleName) {
        SliceQuery<String,String,String> userListQuery = HFactory
                .createSliceQuery(keyspace, stringSerializer, stringSerializer,
                                  stringSerializer);
        userListQuery.setRange("", "", false, MAX_USERS_PER_ROLE)
                .setColumnFamily(CFConstants.ROLES)
                .setKey(roleName);


        List<String> userList = new ArrayList<String>();
        ColumnSlice<String, String> usersForRolesResult = userListQuery.execute().get();
        for (HColumn<String, String> userForRole : usersForRolesResult.getColumns()) {
            userList.add(userForRole.getName());
        }
        return userList;
    }

    @Override
    public void addUser(Credential credential, String[] roleList, Map<String, String> claims,
                        String profileName) throws UserStoreException {
    	if(DEBUG) {
    		log.debug("Adding user " + credential.getIdentifier());
    	}
        // create new userId
        String userId = UUID.randomUUID().toString();
        addUserWithUserId(userId, credential, roleList, claims, profileName);
    }

    private void addClaimsForUser(String userId, Map<String, String> claims) {

        Mutator<String> mutator = HFactory.createMutator(keyspace, stringSerializer);

        // add claims
        for (Map.Entry<String, String> claimsVals : claims.entrySet()) {
            mutator.addInsertion(userId, CFConstants.CLAIMS, HFactory.createColumn(claimsVals.getKey(), claimsVals.getValue()));
        }

        mutator.execute();
    }

    private CredentialType getCredentialType(Credential credential)
            throws CredentialTypeNotSupportedException {

        String credentialsType = credential.getCredentialsType();
        return getCredentialType(credentialsType);
    }

    private CredentialType getCredentialType(String credentialsType)
            throws CredentialTypeNotSupportedException {
        CredentialType credentialType = credentialTypeMap.get(credentialsType);
        if (credentialType == null) {
            String msg = "Credential type " + credentialsType + " not found.";
            CredentialTypeNotSupportedException credentialTypeNotSupportedException = new CredentialTypeNotSupportedException();
            log.error(msg, credentialTypeNotSupportedException);
            throw credentialTypeNotSupportedException;
        }
        return credentialType;
    }

    @Override
    public void addUsers(Credential[] credentials, String[] roleList, Map<String, String> claims,
                        String profileName) throws UserStoreException {
        for (int i = 0; i < credentials.length; i++) {
            Credential credential = credentials[i];
            this.addUser(credential, roleList, claims, profileName);
        }
    }

    @Override
    public void deleteUser(String identifier, String credentialType) throws UserStoreException {
    	if(DEBUG) {
    		log.debug("Deleting user " + identifier);
    	}
        Credential credential = new Credential();
        credential.setIdentifier(identifier);
        credential.setCredentialsType(credentialType);
        deleteUser(credential);
    }

    @Override
    public void addCredential(String identifier, String credentialType, Credential credential)
            throws UserStoreException {
    	if(DEBUG) {
    		log.debug("Adding credential " + credential.getIdentifier() + " to " + identifier);
    	}
        String existingUserId = getExistingUserId(identifier, credentialType);
        if (existingUserId == null) {
            String message = "User does not exist for credential : " + credential.getIdentifier();
            UserDoesNotExistException userDoesNotExistException = new UserDoesNotExistException(message);
            log.error(message, userDoesNotExistException);
            throw userDoesNotExistException;
        }
        getCredentialType(credential).add(existingUserId, credential);
    }

    @Override
    public void updateCredential(String identifier, String credentialType, Credential newCredential)
            throws UserStoreException {
    	if(DEBUG) {
    		log.debug("Updating credential " + identifier);
    	}
        getCredentialType(newCredential).update(getExistingUserId(identifier, credentialType), newCredential);
    }

    @Override
    public void deleteCredential(String identifier, String credentialType) throws UserStoreException {
    	if(DEBUG) {
    		log.debug("Deleting credential " + identifier);
    	}
        Credential credential = new Credential();
        credential.setIdentifier(identifier);
        credential.setCredentialsType(credentialType);
        getCredentialType(credentialType).delete(credential);
    }

    @Override
    public void deleteUser(Credential credential) throws UserStoreException {

        String userId = getExistingUserId(credential);

        if (userId == null) {
            String message = "User does not exist for credential : " + credential.getIdentifier();
            UserDoesNotExistException userDoesNotExistException = new UserDoesNotExistException(message);
            log.error(message, userDoesNotExistException);
            throw userDoesNotExistException;
        }

        Mutator<String> mutator = HFactory.createMutator(keyspace, stringSerializer);

//        String userId = getExistingUserId(credential);

        // delete user row
        mutator.addDeletion(userId, CFConstants.USERS, null, stringSerializer);

        // delete reverse look up  for all credentials
        Credential[] credentials = getCredentials(credential);
        for (Credential userCreds : credentials) {
            mutator.addDeletion(Util.createRowKeyForReverseLookup(userCreds), CFConstants.USERNAME_INDEX, null, stringSerializer);
        }
        mutator.execute();

        deleteUserFromRole(userId);

        // TODO add claims and profile support
    }


    @Override
    public Credential[] getCredentials(Credential credential) throws UserStoreException {
        return getCredentials(credential.getIdentifier(), credential.getCredentialsType());
    }

    @Override
    public boolean authenticate(Credential credential) throws UserStoreException {
    	if(DEBUG) {
    		log.debug("Authenticating " + credential.getIdentifier());
    	}
        if ("true".equals(realmConfig.getUserStoreProperty(CFConstants.AUTH_WITH_ANY_CREDENTIAL))) {
            boolean isAuthenticated = false;

            for (Credential retrievedCredential : getCredentials(credential)) {
                if (retrievedCredential.getSecret() == null) {
                    continue;
                }
                String preparedPasswordHash = Util
                        .preparePassword(credential.getSecret(), Util.getSaltValue(
                                retrievedCredential.getIdentifier(), retrievedCredential.getCredentialsType(),
                                keyspace));
                if (retrievedCredential.getSecret().equals(preparedPasswordHash)) {
                    isAuthenticated = true;
                }
            }
            return isAuthenticated;
        }
        return getCredentialType(credential.getCredentialsType()).authenticate(credential);
    }

    @Override
    public Credential[] getCredentials(String anIdentifier, String credentialType) throws UserStoreException {
    	if(DEBUG) {
    		log.debug("Getting credentials for " + anIdentifier);
    	}
        String existingUserId = getExistingUserId(anIdentifier, credentialType);

        if (existingUserId == null) {
            String msg = "User with identifier : " + anIdentifier + " does not exist for credential type : " + credentialType;
            UserDoesNotExistException userDoesNotExistException = new UserDoesNotExistException(msg);
            log.error(msg, userDoesNotExistException);
            throw userDoesNotExistException;
        }

        SliceQuery<String,String,String> allCredentialsSliceQuery = HFactory
                .createSliceQuery(keyspace, stringSerializer, stringSerializer,
                                  stringSerializer);
        allCredentialsSliceQuery.setRange("", "", false, MAX_CREDENTIALS_NO)
                .setColumnFamily(CFConstants.USERS)
                .setKey(existingUserId);


        List<Credential> credentialList = new ArrayList<Credential>();
        ColumnSlice<String, String> credentialResults = allCredentialsSliceQuery.execute().get();
        for (HColumn<String, String> credentialCol : credentialResults.getColumns()) {
            credentialList.add(getCredentialType(credentialCol.getName()).get(credentialCol.getValue()));
        }
        return credentialList.toArray(new Credential[credentialList.size()]);
    }
}
