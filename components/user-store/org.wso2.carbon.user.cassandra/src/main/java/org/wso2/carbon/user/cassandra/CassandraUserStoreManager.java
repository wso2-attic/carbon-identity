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

package org.wso2.carbon.user.cassandra;

import me.prettyprint.cassandra.model.BasicColumnDefinition;
import me.prettyprint.cassandra.model.BasicColumnFamilyDefinition;
import me.prettyprint.cassandra.serializers.CompositeSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.cassandra.service.ColumnSliceIterator;
import me.prettyprint.cassandra.service.template.ColumnFamilyResult;
import me.prettyprint.cassandra.service.template.ColumnFamilyTemplate;
import me.prettyprint.cassandra.service.template.ThriftColumnFamilyTemplate;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.Composite;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.ColumnIndexType;
import me.prettyprint.hector.api.ddl.ComparatorType;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;
import me.prettyprint.hector.api.exceptions.HectorException;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.RangeSlicesQuery;
import me.prettyprint.hector.api.query.SliceQuery;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.user.api.Properties;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.claim.ClaimManager;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.common.RoleContext;
import org.wso2.carbon.user.core.jdbc.JDBCRealmConstants;
import org.wso2.carbon.user.core.profile.ProfileConfigurationManager;
import org.wso2.carbon.user.core.tenant.Tenant;
import org.wso2.carbon.user.core.util.DatabaseUtil;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class CassandraUserStoreManager extends AbstractUserStoreManager {

    private static final String TRUE = "TRUE";
    private static final Log log = LogFactory.getLog(CassandraUserStoreManager.class);
    private final StringSerializer stringSerializer = StringSerializer.get();
    protected DataSource jdbcDataSource = null;
    protected boolean useOnlyInternalRoles;
    protected Random random = new Random();
    private Cluster cluster;
    private Keyspace keyspace;
    private String tenantIdString;
    private String domain = null;

    public CassandraUserStoreManager() {

    }

    public CassandraUserStoreManager(RealmConfiguration realmConfig, int tenantId) throws UserStoreException {
        this.realmConfig = realmConfig;
        Util.setRealmConfig(realmConfig);
        this.tenantIdString = Integer.toString(tenantId);
        this.tenantId = tenantId;

        // Set groups read/write configuration
        if (realmConfig.getUserStoreProperty(UserCoreConstants.RealmConfig.READ_GROUPS_ENABLED) != null) {
            readGroupsEnabled = Boolean.parseBoolean(realmConfig
                    .getUserStoreProperty(UserCoreConstants.RealmConfig.READ_GROUPS_ENABLED));
        }

        if (realmConfig.getUserStoreProperty(UserCoreConstants.RealmConfig.WRITE_GROUPS_ENABLED) != null) {
            writeGroupsEnabled = Boolean.parseBoolean(realmConfig
                    .getUserStoreProperty(UserCoreConstants.RealmConfig.WRITE_GROUPS_ENABLED));
        } else {
            if (!isReadOnly()) {
                writeGroupsEnabled = true;
            }
        }
        if (writeGroupsEnabled) {
            readGroupsEnabled = true;
        }

        /*
         * Initialize user roles cache as implemented in AbstractUserStoreManager
         */
        initUserRolesCache();

        Map<String, String> credentials = new HashMap<String, String>();
        credentials.put(CFConstants.USERNAME_PROPERTY,
                realmConfig.getUserStoreProperty(CFConstants.USERNAME_XML_ATTRIB));
        credentials.put(CFConstants.PASSWORD_PROPERTY,
                realmConfig.getUserStoreProperty(CFConstants.PASSWORD_XML_ATTRIB));

        CassandraHostConfigurator hostConf = new CassandraHostConfigurator();
        hostConf.setHosts(realmConfig.getUserStoreProperty(CFConstants.HOST_XML_ATTRIB));
        hostConf.setPort(Integer.parseInt(realmConfig.getUserStoreProperty(CFConstants.PORT_XML_ATTRIB)));
        // set Cassandra specific properties
        cluster = HFactory.getOrCreateCluster(realmConfig.getUserStoreProperty(CFConstants.KEYSPACE_NAME_XML_ATTRIB),
                hostConf, credentials);
        keyspace = HFactory.createKeyspace(realmConfig.getUserStoreProperty(CFConstants.KEYSPACE_NAME_XML_ATTRIB),
                cluster);
        insertInitialData(keyspace);
    }

    public CassandraUserStoreManager(RealmConfiguration realmConfig, Map<String, Object> properties,
                                     ClaimManager claimManager, ProfileConfigurationManager profileManager, UserRealm realm, Integer tenantId)
            throws UserStoreException {

        this(realmConfig, tenantId);

        if (log.isDebugEnabled()) {
            log.debug("Started " + System.currentTimeMillis());
        }
        this.claimManager = claimManager;
        this.userRealm = realm;

        dataSource = (DataSource) properties.get(UserCoreConstants.DATA_SOURCE);
        if (dataSource == null) {
            dataSource = DatabaseUtil.getRealmDataSource(realmConfig);
        }
        if (dataSource == null) {
            throw new UserStoreException("User Management Data Source is null");
        }

        doInitialSetup();
        this.persistDomain();
        if (realmConfig.isPrimary()) {
            addInitialAdminData(Boolean.parseBoolean(realmConfig.getAddAdmin()), !isInitSetupDone());
        }

        properties.put(UserCoreConstants.DATA_SOURCE, dataSource);

        if (log.isDebugEnabled()) {
            log.debug("The jdbcDataSource being used by JDBCUserStoreManager :: " + dataSource.hashCode());
        }

        if (log.isDebugEnabled()) {
            log.debug("Ended " + System.currentTimeMillis());
        }

        domain = realmConfig.getUserStoreProperty(UserCoreConstants.RealmConfig.PROPERTY_DOMAIN_NAME);
        /*
         * Initialize user roles cache as implemented in AbstractUserStoreManager
         */
        initUserRolesCache();
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

            // Holds the roles.
            ColumnFamilyDefinition roleCF = new BasicColumnFamilyDefinition();
            roleCF.setName(CFConstants.UM_ROLES);
            roleCF.setKeyspaceName(keyspaceName);
            cluster.addColumnFamily(roleCF, true);

            // add user roles. Holds the roles per user. Mapped as (user_name,
            // tenant_id) -> role_name
            ColumnFamilyDefinition userRolesIndexCF = new BasicColumnFamilyDefinition();
            userRolesIndexCF.setName(CFConstants.UM_USER_ROLE);
            userRolesIndexCF.setKeyspaceName(keyspaceName);
            cluster.addColumnFamily(userRolesIndexCF, true);

            // add user roles. Holds the users per role. Mapped as (role_name,
            // tenant_id) -> users
            ColumnFamilyDefinition rolesToUserIndex = new BasicColumnFamilyDefinition();
            rolesToUserIndex.setName(CFConstants.UM_ROLE_USER_INDEX);
            rolesToUserIndex.setKeyspaceName(keyspaceName);
            cluster.addColumnFamily(rolesToUserIndex, true);

            // Holds the users.
            ColumnFamilyDefinition userCF = new BasicColumnFamilyDefinition();
            userCF.setName(CFConstants.UM_USER);
            userCF.setKeyspaceName(keyspaceName);
            BasicColumnDefinition columnDefinition = new BasicColumnDefinition();
            columnDefinition.setName(StringSerializer.get().toByteBuffer(CFConstants.UM_USER_NAME));
            columnDefinition.setIndexName(CFConstants.UM_USER_NAME_INDEX);
            columnDefinition.setIndexType(ColumnIndexType.KEYS);
            columnDefinition.setValidationClass(ComparatorType.UTF8TYPE.getClassName());
            userCF.addColumnDefinition(columnDefinition);
            cluster.addColumnFamily(userCF, true);

            // Holds user's attributes.
            ColumnFamilyDefinition claimsCF = new BasicColumnFamilyDefinition();
            claimsCF.setName(CFConstants.UM_USER_ATTRIBUTE);
            claimsCF.setKeyspaceName(keyspaceName);
            cluster.addColumnFamily(claimsCF, true);

        }
        String msg = "Connected to Cassandra keyspace : " + keyspace.getKeyspaceName() + ". ";
        if (foundKS) {
            msg += " Keyspace already found. Not creating any column families or intialization data.";
        } else {
            msg += " Keyspace not found. Creating all column families and adding initialization data.";
        }
        log.info(msg);
    }

    /**
     * Checks if the role is existing the role store.
     */
    @Override
    protected boolean doCheckExistingRole(String roleNameWithTenantDomain) throws UserStoreException {

        RoleContext roleContext = createRoleContext(roleNameWithTenantDomain);
        boolean isExisting = false;

        String roleName = roleContext.getRoleName();

        Composite key = new Composite();
        key.addComponent(roleName, stringSerializer);
        key.addComponent(tenantIdString, stringSerializer);

        ColumnQuery<Composite, String, String> getCredentialQuery = HFactory.createColumnQuery(keyspace,
                CompositeSerializer.get(), stringSerializer, stringSerializer);

        getCredentialQuery.setColumnFamily(CFConstants.UM_ROLES).setKey(key).setName(CFConstants.UM_ROLE_NAME);

        HColumn<String, String> result = getCredentialQuery.execute().get();
        if (result != null && result.getValue() != null) {
            isExisting = true;
        }

        return isExisting;
    }

    /**
     * Adds the user to the user store.
     */
    @Override
    public void doAddUser(String userName, Object credential, String[] roleList, Map<String, String> claims,
                          String profileName, boolean requirePasswordChange) throws UserStoreException {

        String userId = UUID.randomUUID().toString();
        String saltValue = null;

        if (TRUE.equalsIgnoreCase(realmConfig.getUserStoreProperties().get(JDBCRealmConstants.STORE_SALTED_PASSWORDS))) {
            saltValue = Util.getSaltValue();
        }

        String password = Util.preparePassword((String) credential, saltValue);

        if (doCheckExistingUser(userName)) {

            String message = "User with credentials " + userName + "exists";

            UserStoreException userStoreException = new UserStoreException(message);
            log.error(message, userStoreException);
            throw userStoreException;
        } else {

            Mutator<Composite> mutator = HFactory.createMutator(keyspace, CompositeSerializer.get());

            Composite key = new Composite();
            key.addComponent(userName, stringSerializer);
            key.addComponent(tenantIdString, stringSerializer);

            // add user ID
            mutator.addInsertion(key, CFConstants.UM_USER,
                    HFactory.createColumn(CFConstants.UM_USER_ID, userId, stringSerializer, stringSerializer));
            mutator.addInsertion(key, CFConstants.UM_USER,
                    HFactory.createColumn(CFConstants.UM_USER_NAME, userName, stringSerializer, stringSerializer));
            mutator.addInsertion(key, CFConstants.UM_USER,
                    HFactory.createColumn(CFConstants.UM_SECRET, password, stringSerializer, stringSerializer));
            mutator.addInsertion(key, CFConstants.UM_USER,
                    HFactory.createColumn(CFConstants.UM_SALT_VALUE, saltValue, stringSerializer, stringSerializer));
            mutator.addInsertion(key, CFConstants.UM_USER, HFactory.createColumn(CFConstants.UM_REQUIRE_CHANGE_BOOLEAN,
                    "false", stringSerializer, stringSerializer));
            mutator.addInsertion(key, CFConstants.UM_USER,
                    HFactory.createColumn(CFConstants.UM_TENANT_ID, tenantIdString, stringSerializer, stringSerializer));
            mutator = addUserToRoleList(userName, roleList, mutator);

            if (claims != null) {
                mutator = addClaimsForUser(userId, claims, mutator);
            }

            try {
                mutator.execute();
                if (log.isDebugEnabled()) {
                    log.debug("Added user " + userName + " successfully");
                }
            } catch (HectorException e) {
                // TODO- research and check how to identify cassandra failure
                // and handle it efficiently.
                throw new UserStoreException("Adding user failed.", e);
            }
            mutator.execute();

        }
    }

    /**
     * Deletes a user by userName.
     */
    @Override
    public void doDeleteUser(String userName) throws UserStoreException {

        Mutator<Composite> mutator = HFactory.createMutator(keyspace, CompositeSerializer.get());
        String[] roles = doGetExternalRoleListOfUser(userName, "");
        for (String role : roles) {
            Composite key = new Composite();
            key.addComponent(role, stringSerializer);
            key.addComponent(tenantIdString, stringSerializer);
            ColumnFamilyTemplate<Composite, String> userCFTemplate = new ThriftColumnFamilyTemplate<Composite, String>(
                    keyspace, CFConstants.UM_ROLE_USER_INDEX, CompositeSerializer.get(), StringSerializer.get());
            try {
                userCFTemplate.deleteColumn(key, userName);
            } catch (HectorException e) {
                log.error("Error during deletion ", e);
            }
        }

        Composite userKey = new Composite();
        userKey.addComponent(userName, stringSerializer);
        userKey.addComponent(tenantIdString, stringSerializer);
        mutator.addDeletion(userKey, CFConstants.UM_USER_ROLE, null, CompositeSerializer.get());
        mutator.addDeletion(userKey, CFConstants.UM_USER, null, CompositeSerializer.get());
        mutator.execute();

        if (log.isDebugEnabled()) {
            log.debug("Deleted user " + userName + " successfully");
        }
    }

    /**
     * Changes the password of the user.
     */
    @Override
    public void doUpdateCredential(String userName, Object newCredential, Object oldCredential)
            throws UserStoreException {
        this.doUpdateCredentialByAdmin(userName, newCredential);
    }

    @Override
    public void doUpdateCredentialByAdmin(String userName, Object newCredential) throws UserStoreException {
        if (!checkUserPasswordValid(newCredential)) {
            throw new UserStoreException(
                    "Credential not valid. Credential must be a non null string with following format, "
                            + realmConfig.getUserStoreProperty(UserCoreConstants.RealmConfig.PROPERTY_JAVA_REG_EX));

        }

        String saltValue = null;
        if (TRUE.equalsIgnoreCase(realmConfig.getUserStoreProperties().get(JDBCRealmConstants.STORE_SALTED_PASSWORDS))) {
            saltValue = Util.getSaltValue();
        }
        String password = Util.preparePassword((String) newCredential, saltValue);
        Mutator<Composite> mutator = HFactory.createMutator(keyspace, CompositeSerializer.get());
        Composite key = new Composite();
        key.addComponent(userName, stringSerializer);
        key.addComponent(tenantIdString, stringSerializer);
        mutator.addInsertion(key, CFConstants.UM_USER,
                HFactory.createColumn(CFConstants.UM_SECRET, password, stringSerializer, stringSerializer));
        mutator.addInsertion(key, CFConstants.UM_USER,
                HFactory.createColumn(CFConstants.UM_SALT_VALUE, saltValue, stringSerializer, stringSerializer));
        try {
            mutator.execute();
            if (log.isDebugEnabled()) {
                log.debug("Changed password for user " + userName + "successfully");
            }
        } catch (HectorException e) {
            throw new UserStoreException("Change Password failed.", e);
        }
    }

    /**
     * Checks if the user is existing in the user store.
     */
    @Override
    protected boolean doCheckExistingUser(String userName) throws UserStoreException {

        Boolean isExist = false;

        Composite key = new Composite();
        key.addComponent(userName, stringSerializer);
        key.addComponent(tenantIdString, stringSerializer);

        ColumnQuery<Composite, String, String> getCredentialQuery = HFactory.createColumnQuery(keyspace,
                CompositeSerializer.get(), stringSerializer, stringSerializer);

        getCredentialQuery.setColumnFamily(CFConstants.UM_USER).setKey(key).setName(CFConstants.UM_USER_NAME);

        HColumn<String, String> result = getCredentialQuery.execute().get();
        if (result != null && result.getValue() != null) {
            isExist = true;
        }

        return isExist;

    }

    /**
     * Adds a role to the role store.
     */
    @Override
    public void doAddRole(String roleName, String[] userList, boolean shared) throws UserStoreException {

        Mutator<Composite> mutator = HFactory.createMutator(keyspace, CompositeSerializer.get());
        Composite composite = new Composite();
        composite.addComponent(roleName, stringSerializer);
        composite.addComponent(tenantIdString, stringSerializer);

        mutator.addInsertion(composite, CFConstants.UM_ROLES,
                HFactory.createColumn(CFConstants.UM_ROLE_NAME, roleName, stringSerializer, stringSerializer));
        mutator.addInsertion(composite, CFConstants.UM_ROLES,
                HFactory.createColumn(CFConstants.UM_TENANT_ID, tenantIdString, stringSerializer, stringSerializer));

        if (userList != null && userList.length > 0) {
            addRoleToUsersList(userList, roleName, mutator);
        }

        mutator.execute();
    }

    /**
     * Deletes a role by role name from the role store.
     */
    @Override
    public void doDeleteRole(String roleName) throws UserStoreException {

        Mutator<Composite> mutator = HFactory.createMutator(keyspace, CompositeSerializer.get());
        String[] users = getUserListOfRole(roleName);
        for (String userName : users) {
            Composite key = new Composite();
            key.addComponent(userName, stringSerializer);
            key.addComponent(tenantIdString, stringSerializer);
            ColumnFamilyTemplate<Composite, String> userCFTemplate = new ThriftColumnFamilyTemplate<Composite, String>(
                    keyspace, CFConstants.UM_USER_ROLE, CompositeSerializer.get(), StringSerializer.get());
            try {
                userCFTemplate.deleteColumn(key, roleName);
            } catch (HectorException e) {
                throw new UserStoreException("Exception occured when deleting Role", e);
            }
        }

        Composite roleKey = new Composite();
        roleKey.addComponent(roleName, stringSerializer);
        roleKey.addComponent(tenantIdString, stringSerializer);
        mutator.addDeletion(roleKey, CFConstants.UM_ROLE_USER_INDEX, null, CompositeSerializer.get());
        mutator.addDeletion(roleKey, CFConstants.UM_ROLES, null, CompositeSerializer.get());

        try {
            mutator.execute();
        } catch (HectorException e) {
            // TODO- research and check how to identify cassandra failure and
            // handle it efficiently.
            throw new UserStoreException("Role deletion failed.", e);
        }

    }

    /**
     * Updates the role name in the role store.
     */
    @Override
    public void doUpdateRoleName(String roleName, String newRoleName) throws UserStoreException {
        doAddRole(newRoleName, getUserListOfRole(roleName), false);
        doDeleteRole(roleName);

    }

    /**
     * Maps the users to a role list. Adds the (username, tenantId) -> roleList
     * and (role, tenantId) -> userName
     *
     * @param userName The username of the user the roles need to be added to.
     * @param roleList The list of roles that needs to be mapped against the user.
     */
    private void addUserToRoleList(String userName, String[] roleList) {

        Mutator<Composite> mutator = HFactory.createMutator(keyspace, CompositeSerializer.get());

        if (roleList != null) {
            for (String role : roleList) {
                Composite key = new Composite();
                key.addComponent(userName, stringSerializer);
                key.addComponent(tenantIdString, stringSerializer);

                mutator.addInsertion(key, CFConstants.UM_USER_ROLE, HFactory.createColumn(role, role));

                Composite keyRole = new Composite();
                keyRole.addComponent(role, stringSerializer);
                keyRole.addComponent(tenantIdString, stringSerializer);

                mutator.addInsertion(keyRole, CFConstants.UM_ROLE_USER_INDEX, HFactory.createColumn(userName, userName));

            }
            mutator.execute();
        }
    }

    /**
     * Maps the users to a role list. Adds the (username, tenantId) -> roleList
     * and (role, tenantId) -> userName
     *
     * @param userName The username of the user the roles need to be added to.
     * @param roleList The list of roles that needs to be mapped against the user.
     * @param mutator  Passes the mutator and returns it with the insert statements.
     */
    private Mutator<Composite> addUserToRoleList(String userName, String[] roleList, Mutator<Composite> mutator) {
        if (roleList != null && mutator != null) {
            for (String role : roleList) {
                Composite key = new Composite();
                key.addComponent(userName, stringSerializer);
                key.addComponent(tenantIdString, stringSerializer);

                mutator.addInsertion(key, CFConstants.UM_USER_ROLE, HFactory.createColumn(role, role));

                Composite keyRole = new Composite();
                keyRole.addComponent(role, stringSerializer);
                keyRole.addComponent(tenantIdString, stringSerializer);

                mutator.addInsertion(keyRole, CFConstants.UM_ROLE_USER_INDEX, HFactory.createColumn(userName, userName));

            }
        }
        return mutator;
    }

    /**
     * Maps the role to a user list. Adds the (username, tenantId) -> roleList
     * and (role, tenantId) -> userName
     *
     * @param userNames The username list of the user the role need to be added to.
     * @param roleName  The role that needs to be mapped against the user list.
     * @param mutator   Passes the mutator and returns it with the insert statements.
     */
    private Mutator<Composite> addRoleToUsersList(String[] userNames, String roleName, Mutator<Composite> mutator) {
        if (userNames != null) {
            for (String userName : userNames) {

                Composite key = new Composite();
                key.addComponent(userName, stringSerializer);
                key.addComponent(tenantIdString, stringSerializer);

                mutator.addInsertion(key, CFConstants.UM_USER_ROLE, HFactory.createColumn(roleName, roleName));

                Composite keyRole = new Composite();
                keyRole.addComponent(roleName, stringSerializer);
                keyRole.addComponent(tenantIdString, stringSerializer);

                mutator.addInsertion(keyRole, CFConstants.UM_ROLE_USER_INDEX, HFactory.createColumn(userName, userName));

            }

        }
        return mutator;
    }

    /**
     * Authenticates a user given the user name and password against the user
     * store.
     */
    @Override
    public boolean doAuthenticate(String userName, Object credential) throws UserStoreException {

        String password = (String) credential;
        boolean isAuthed = false;
        if (!checkUserNameValid(userName)) {
            log.error("Invalid Username");
            return false;
        }

        if (!checkUserPasswordValid(credential)) {
            log.error("Invalid password");
            return false;
        }

        if (UserCoreUtil.isRegistryAnnonymousUser(userName)) {
            log.error("Anonnymous user trying to login");
            return false;
        }

        Composite key = new Composite();
        key.addComponent(userName, stringSerializer);
        key.addComponent(tenantIdString, stringSerializer);

        ColumnFamilyTemplate<Composite, String> userCFTemplate = new ThriftColumnFamilyTemplate<Composite, String>(
                keyspace, CFConstants.UM_USER, CompositeSerializer.get(), StringSerializer.get());

        ColumnFamilyResult<Composite, String> result = userCFTemplate.queryColumns(key);
        String saltVallue = result.getString(CFConstants.UM_SALT_VALUE);
        String storedPassword = result.getString(CFConstants.UM_SECRET);

        if (TRUE.equalsIgnoreCase(realmConfig.getUserStoreProperty(JDBCRealmConstants.STORE_SALTED_PASSWORDS))) {
            password = Util.preparePassword(password, saltVallue);
            if ((storedPassword != null) && (storedPassword.equals(password))) {
                isAuthed = true;
            }
        }
        return isAuthed;
    }

    /**
     * Lists the users in the user store.
     */
    @Override
    protected String[] doListUsers(String filter, int maxItemLimit) throws UserStoreException {

        List<String> users = new ArrayList<String>();
        int arrayLength = 0;

        if (maxItemLimit == 0) {
            return new String[0];
        }

        int givenMax = UserCoreConstants.MAX_USER_ROLE_LIST;

        try {
            givenMax = Integer.parseInt(realmConfig
                    .getUserStoreProperty(UserCoreConstants.RealmConfig.PROPERTY_MAX_USER_LIST));
        } catch (Exception e) {
            givenMax = UserCoreConstants.MAX_USER_ROLE_LIST;

            if (log.isDebugEnabled()) {
                log.debug("Realm configuration maximum not set : Using User Core Constant value instead!", e);
            }
        }

        if (maxItemLimit < 0 || maxItemLimit > givenMax) {
            maxItemLimit = givenMax;
        }

        RangeSlicesQuery<String, String, String> rangeSliceQuery = HFactory.createRangeSlicesQuery(keyspace,
                stringSerializer, stringSerializer, stringSerializer);

        rangeSliceQuery.setColumnFamily(CFConstants.UM_USER);
        rangeSliceQuery.setRange(filter, null, false, Integer.MAX_VALUE);
        rangeSliceQuery.addEqualsExpression(CFConstants.UM_TENANT_ID, tenantIdString);

        // TODO - Need to check how to use the filter for range
        rangeSliceQuery.setKeys("", "");
        rangeSliceQuery.setRowCount(maxItemLimit);
        QueryResult<OrderedRows<String, String, String>> result = rangeSliceQuery.execute();
        if (result != null) {
            OrderedRows<String, String, String> rows = result.get();
            if (rows.getCount() <= 0) {
                // reformatted to avoid nesting too many blocks
                return users.toArray(new String[arrayLength]);

            }
            arrayLength = rows.getCount();

            Iterator<Row<String, String, String>> rowsIterator = rows.iterator();

            while (rowsIterator.hasNext()) {
                Row<String, String, String> row = rowsIterator.next();
                if (row.getColumnSlice().getColumnByName(CFConstants.UM_USER_ID).getValue() != null) {
                    String name = row.getColumnSlice().getColumnByName(CFConstants.UM_USER_NAME).getValue();
                    // append the domain if exist
                    name = UserCoreUtil.addDomainToName(name, domain);
                    users.add(name);
                }
            }

        }
        return users.toArray(new String[arrayLength]);

    }

    /**
     * Get the role names in the roles store.
     */
    @Override
    public String[] doGetRoleNames(String filter, int maxItemLimit) throws UserStoreException {
        List<String> roles = new ArrayList<String>();

        if (maxItemLimit == 0) {
            return new String[0];
        }

        int givenMax = UserCoreConstants.MAX_USER_ROLE_LIST;

        try {
            givenMax = Integer.parseInt(realmConfig
                    .getUserStoreProperty(UserCoreConstants.RealmConfig.PROPERTY_MAX_ROLE_LIST));
        } catch (Exception e) {
            givenMax = UserCoreConstants.MAX_USER_ROLE_LIST;

            if (log.isDebugEnabled()) {
                log.debug("Realm configuration maximum not set : Using User Core Constant value instead!", e);
            }
        }

        if (maxItemLimit < 0 || maxItemLimit > givenMax) {
            maxItemLimit = givenMax;
        }

        int arrayLength = 0;
        String domain = realmConfig.getUserStoreProperty(UserCoreConstants.RealmConfig.PROPERTY_DOMAIN_NAME);
        RangeSlicesQuery<String, String, String> rangeSliceQuery = HFactory.createRangeSlicesQuery(keyspace,
                stringSerializer, stringSerializer, stringSerializer);
        rangeSliceQuery.setColumnFamily(CFConstants.UM_ROLES);
        rangeSliceQuery.setRange(null, null, false, Integer.MAX_VALUE);
        rangeSliceQuery.addEqualsExpression(CFConstants.UM_TENANT_ID, tenantIdString);
        rangeSliceQuery.setKeys("", "");
        rangeSliceQuery.setRowCount(maxItemLimit);
        QueryResult<OrderedRows<String, String, String>> result = rangeSliceQuery.execute();
        if (result != null) {
            OrderedRows<String, String, String> rows = result.get();
            if (rows.getCount() <= 0) {
                return roles.toArray(new String[arrayLength]);
            }
            arrayLength = rows.getCount();

            Iterator<Row<String, String, String>> rowsIterator = rows.iterator();

            while (rowsIterator.hasNext()) {
                Row<String, String, String> row = rowsIterator.next();
                if (row.getColumnSlice().getColumnByName(CFConstants.UM_ROLE_NAME).getValue() != null) {
                    String name = row.getColumnSlice().getColumnByName(CFConstants.UM_ROLE_NAME).getValue();
                    // append the domain if exist
                    name = UserCoreUtil.addDomainToName(name, domain);
                    roles.add(name);
                }
            }

        }
        return roles.toArray(new String[arrayLength]);
    }

    /**
     * Checks if user is existing in the user store.
     */
    @Override
    public boolean isExistingUser(String userName) throws UserStoreException {
        if (CarbonConstants.REGISTRY_SYSTEM_USERNAME.equals(userName)) {
            return true;
        }

        return !(getExistingUserId(userName, CFConstants.DEFAULT_TYPE) == null);


    }

    /**
     * Check if role is existing in the role store.
     */
    @Override
    public boolean isExistingRole(String roleName) throws UserStoreException {
        return doCheckExistingRole(roleName);
    }

    /**
     * Get the list of users mapped to a role.
     */
    @Override
    public String[] doGetUserListOfRole(String roleName, String filter) throws UserStoreException {

        List<String> usersList = new ArrayList<String>();
        Composite key = new Composite();
        key.addComponent(roleName, stringSerializer);
        key.addComponent(tenantIdString, stringSerializer);
        SliceQuery<Composite, String, String> query = HFactory
                .createSliceQuery(keyspace, CompositeSerializer.get(), StringSerializer.get(), StringSerializer.get())
                .setKey(key).setColumnFamily(CFConstants.UM_ROLE_USER_INDEX);

        ColumnSliceIterator<Composite, String, String> iterator = new ColumnSliceIterator<Composite, String, String>(
                query, null, "\uFFFF", false);

        while (iterator.hasNext()) {
            HColumn<String, String> column = iterator.next();
            usersList.add(column.getValue());
        }
        return usersList.toArray(new String[usersList.size()]);
    }

    /**
     * Get the role list of a user.
     */
    @Override
    public String[] getRoleListOfUser(String userName) throws UserStoreException {
        return doGetRoleListOfUser(userName, null);
    }

    private Mutator<Composite> addClaimsForUser(String userName, Map<String, String> claims, Mutator<Composite> mutator) {

        Composite key = new Composite();
        key.addComponent(userName, stringSerializer);
        key.addComponent(tenantIdString, stringSerializer);
        // add claims
        for (Map.Entry<String, String> claimsVals : claims.entrySet()) {
            mutator.addInsertion(key, CFConstants.UM_USER_ATTRIBUTE,
                    HFactory.createColumn(claimsVals.getKey(), claimsVals.getValue()));
            mutator.addInsertion(key, CFConstants.UM_USER_ATTRIBUTE,
                    HFactory.createColumn(CFConstants.UM_TENANT_ID, tenantIdString));
        }
        return mutator;
    }

    /**
     * Gets the external role list of a user.
     */
    @Override
    public String[] doGetExternalRoleListOfUser(String userName, String filter) throws UserStoreException {

        List<String> roles = new ArrayList<String>();
        int arrayLength = 0;
        Composite key = new Composite();
        key.addComponent(userName, stringSerializer);
        key.addComponent(tenantIdString, stringSerializer);
        SliceQuery<Composite, String, String> query = HFactory
                .createSliceQuery(keyspace, CompositeSerializer.get(), StringSerializer.get(), StringSerializer.get())
                .setKey(key).setColumnFamily(CFConstants.UM_USER_ROLE);

        ColumnSliceIterator<Composite, String, String> iterator = new ColumnSliceIterator<Composite, String, String>(
                query, null, "\uFFFF", false);

        while (iterator.hasNext()) {
            HColumn<String, String> column = iterator.next();
            roles.add(column.getValue());
        }
        return roles.toArray(new String[arrayLength]);
    }

    /**
     * Update the role list of a user.
     */
    @Override
    public void doUpdateRoleListOfUser(String userName, String[] deletedRoles, String[] newRoles)
            throws UserStoreException {

        RoleBreakdown breakdown;
        String[] roles;
        String[] sharedRoles;

        try {
            Mutator<Composite> mutator = HFactory.createMutator(keyspace, CompositeSerializer.get());
            // if user name and role names are prefixed with domain name, remove
            // the domain name
            String[] userNames = userName.split(CarbonConstants.DOMAIN_SEPARATOR);
            if (userNames.length > 1) {
                userName = userNames[1];
            }
            if (deletedRoles != null && deletedRoles.length > 0) {
                // if user name and role names are prefixed with domain name,
                // remove the domain name
                breakdown = getSharedRoleBreakdown(deletedRoles);
                roles = breakdown.getRoles();
                sharedRoles = breakdown.getSharedRoles();

                if (roles.length > 0) {

                    Composite userKey = new Composite();
                    userKey.addComponent(userName, stringSerializer);
                    userKey.addComponent(tenantIdString, stringSerializer);

                    for (String role : roles) {
                        Composite key = new Composite();
                        key.addComponent(role, stringSerializer);
                        key.addComponent(tenantIdString, stringSerializer);

                        ColumnFamilyTemplate<Composite, String> userCFTemplate = new ThriftColumnFamilyTemplate<Composite, String>(
                                keyspace, CFConstants.UM_USER_ROLE, CompositeSerializer.get(), StringSerializer.get());
                        ColumnFamilyTemplate<Composite, String> roleCFTemplate = new ThriftColumnFamilyTemplate<Composite, String>(
                                keyspace, CFConstants.UM_ROLE_USER_INDEX, CompositeSerializer.get(),
                                StringSerializer.get());
                        try {
                            roleCFTemplate.deleteColumn(mutator, key, userName);
                            userCFTemplate.deleteColumn(mutator, userKey, role);
                        } catch (HectorException e) {
                            throw new UserStoreException("Ex eption occured when updating role list of a user", e);
                        }
                    }
                }
                if (sharedRoles != null && sharedRoles.length > 0) {
                    //TODO TO-Be Completed
                }
                clearUserRolesCacheByTenant(this.tenantId);
            }

            if (newRoles != null && newRoles.length > 0) {
                // if user name and role names are prefixed with domain name,
                // remove the domain name

                breakdown = getSharedRoleBreakdown(newRoles);
                roles = breakdown.getRoles();
                sharedRoles = breakdown.getSharedRoles();

                if (roles.length > 0) {
                    addUserToRoleList(userName, roles);
                }
                if (sharedRoles != null && sharedRoles.length > 0) {
                    //TODO TO-Be Completed
                }
            }

        } catch (HectorException e) {
            throw new UserStoreException(e.getMessage(), e);
        }
    }

    /**
     * Update the user list mapped to a role.
     */
    @Override
    public void doUpdateUserListOfRole(String roleName, String[] deletedUsers, String[] newUsers)
            throws UserStoreException {

        Mutator<Composite> mutator = HFactory.createMutator(keyspace, CompositeSerializer.get());
        RoleContext ctx = createRoleContext(roleName);
        roleName = ctx.getRoleName();
        boolean isShared = ctx.isShared();
        if (!isShared) {
            //TODO TO BE Implemented
        }
        if (deletedUsers != null && deletedUsers.length > 0) {
            if (isShared) {
                //TODO TO BE Implemented
            } else {
                if (deletedUsers.length > 0) {
                    Composite key = new Composite();
                    key.addComponent(roleName, stringSerializer);
                    key.addComponent(tenantIdString, stringSerializer);

                    for (String user : deletedUsers) {

                        Composite userKey = new Composite();
                        userKey.addComponent(user, stringSerializer);
                        userKey.addComponent(tenantIdString, stringSerializer);

                        ColumnFamilyTemplate<Composite, String> userCFTemplate = new ThriftColumnFamilyTemplate<Composite, String>(
                                keyspace, CFConstants.UM_USER_ROLE, CompositeSerializer.get(), StringSerializer.get());
                        ColumnFamilyTemplate<Composite, String> roleCFTemplate = new ThriftColumnFamilyTemplate<Composite, String>(
                                keyspace, CFConstants.UM_ROLE_USER_INDEX, CompositeSerializer.get(),
                                StringSerializer.get());
                        try {
                            roleCFTemplate.deleteColumn(mutator, key, user);
                            userCFTemplate.deleteColumn(mutator, userKey, roleName);
                        } catch (HectorException e) {
                            log.error(e.getMessage(), e);
                            throw new UserStoreException("Error during the updating of a user's role list");
                        }
                    }
                }

            }
        }
        // need to clear user roles cache upon roles update
        clearUserRolesCacheByTenant(this.tenantId);

        if (newUsers != null && newUsers.length > 0) {
            if (isShared) {
                //TODO TO BE Implemented
            } else {
                addRoleToUsersList(newUsers, roleName, mutator);
            }
        }
        mutator.execute();

    }

    /**
     * Break the provided role list based on whether roles are shared or not
     *
     * @param rolesList
     * @return
     */
    private RoleBreakdown getSharedRoleBreakdown(String[] rolesList) {
        List<String> roles = new ArrayList<String>();
        List<Integer> tenantIds = new ArrayList<Integer>();

        List<String> sharedRoles = new ArrayList<String>();
        List<Integer> sharedTenantIds = new ArrayList<Integer>();

        for (String role : rolesList) {

            String[] deletedRoleNames = role.split(CarbonConstants.DOMAIN_SEPARATOR);
            if (deletedRoleNames.length > 1) {
                role = deletedRoleNames[1];
            }

            CassandraRoleContext ctx = (CassandraRoleContext) createRoleContext(role);
            role = ctx.getRoleName();
            int roleTenantId = ctx.getTenantId();
            boolean isShared = ctx.isShared();

            if (isShared) {
                sharedRoles.add(role);
                sharedTenantIds.add(roleTenantId);
            } else {
                roles.add(role);
                tenantIds.add(roleTenantId);
            }

        }

        RoleBreakdown breakdown = new RoleBreakdown();

        // Non shared roles and tenant ids
        breakdown.setRoles(roles.toArray(new String[roles.size()]));
        breakdown.setTenantIds(tenantIds.toArray(new Integer[tenantIds.size()]));

        // Shared roles and tenant ids
        breakdown.setSharedRoles(sharedRoles.toArray(new String[sharedRoles.size()]));
        breakdown.setSharedTenantids(sharedTenantIds.toArray(new Integer[sharedTenantIds.size()]));

        return breakdown;

    }

    /**
     * Role context.
     */
    @Override
    protected RoleContext createRoleContext(String roleName) {

        CassandraRoleContext searchCtx = new CassandraRoleContext();
        String[] roleNameParts = roleName.split(UserCoreConstants.TENANT_DOMAIN_COMBINER);

        String nullString = "null";

        if (roleNameParts.length > 1 && (roleNameParts[1] == null || nullString.equals(roleNameParts[1]))) {
            roleNameParts = new String[]{roleNameParts[0]};
        }
        int tenantId = -1;
        if (roleNameParts.length > 1) {
            tenantId = Integer.parseInt(roleNameParts[1]);
            searchCtx.setTenantId(tenantId);
        } else {
            tenantId = this.tenantId;
            searchCtx.setTenantId(tenantId);
        }

        if (tenantId != this.tenantId) {
            searchCtx.setShared(true);
        }

        searchCtx.setRoleName(roleNameParts[0]);
        return searchCtx;
    }

    @Override
    protected void persistDomain() throws UserStoreException {
        String domain = UserCoreUtil.getDomainName(this.realmConfig);
        if (domain != null) {
            UserCoreUtil.persistDomain(domain, this.tenantId, this.dataSource);
        }
    }

    /**
     * Get the existing user's Id
     */
    private String getExistingUserId(String identifier, String credentialTypeName) {
        return Util.getExistingUserId(credentialTypeName, identifier, keyspace);
    }

    @Override
    public RealmConfiguration getRealmConfiguration() {
        return this.realmConfig;
    }

    @Override
    protected String getMyDomainName() {
        return UserCoreUtil.getDomainName(realmConfig);
    }

    @Override
    public String[] getUserListFromProperties(String property, String value, String profileName)
            throws UserStoreException {
        return new String[0];
    }

    @Override
    protected String[] doGetSharedRoleNames(String tenantDomain, String filter, int maxItemLimit)
            throws UserStoreException {
        return new String[0];
    }

    @Override
    public boolean doCheckIsUserInRole(String userName, String roleName) throws UserStoreException {
        String[] roles = doGetExternalRoleListOfUser(userName, "*");
        if (roles != null) {
            for (String role : roles) {
                if (role.equalsIgnoreCase(roleName)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected String[] doGetSharedRoleListOfUser(String userName, String tenantDomain, String filter)
            throws UserStoreException {
        return new String[0];

    }

    @Override
    public String[] getAllProfileNames() throws UserStoreException {
        return new String[0];
    }

    @Override
    public boolean isReadOnly() throws UserStoreException {
        if (TRUE.equalsIgnoreCase(realmConfig.getUserStoreProperty(UserCoreConstants.RealmConfig.PROPERTY_READ_ONLY))) {
            return true;
        }
        return false;
    }

    @Override
    public Date getPasswordExpirationTime(String username) throws UserStoreException {
        return null;
    }

    @Override
    public int getUserId(String username) throws UserStoreException {
        return 0;
    }

    @Override
    public int getTenantId(String username) throws UserStoreException {
        return tenantId;
    }

    @Override
    public int getTenantId() throws UserStoreException {
        return this.tenantId;
    }

    @Override
    public Map<String, String> getProperties(org.wso2.carbon.user.api.Tenant tenant)
            throws org.wso2.carbon.user.api.UserStoreException {
        return null;
    }

    @Override
    public boolean isMultipleProfilesAllowed() {
        return false;
    }

    @Override
    public void addRememberMe(String userName, String token) throws org.wso2.carbon.user.api.UserStoreException {
    }

    @Override
    public boolean isValidRememberMeToken(String userName, String token)
            throws org.wso2.carbon.user.api.UserStoreException {
        return false;
    }

    @Override
    public boolean isBulkImportSupported() throws UserStoreException {
        return false;
    }

    @Override
    public void doSetUserClaimValue(String userName, String claimURI, String claimValue, String profileName)
            throws UserStoreException {
    }

    @Override
    public void doSetUserClaimValues(String userName, Map<String, String> claims, String profileName)
            throws UserStoreException {

    }

    @Override
    public void doDeleteUserClaimValue(String userName, String claimURI, String profileName) throws UserStoreException {
    }

    @Override
    public void doDeleteUserClaimValues(String userName, String[] claims, String profileName) throws UserStoreException {
    }

    @Override
    protected String[] doGetDisplayNamesForInternalRole(String[] strings) throws UserStoreException {
        throw new UserStoreException(
                "doGetDisplayNamesForInternalRole(String[]) not implemented for CassandraUserStoreManager");
    }

    @Override
    public String[] getProfileNames(String userName) throws UserStoreException {
        return new String[0];
    }

    /**
     * Returns the propertis of the userstore
     */
    @Override
    public Map<String, String> getProperties(Tenant tenant) throws UserStoreException {
        return null;
    }

    @Override
    public org.wso2.carbon.user.api.Properties getDefaultUserStoreProperties() {

        return new Properties();
    }

    @Override
    public Map<String, String> getUserPropertyValues(String userName, String[] propertyNames, String profileName)
            throws UserStoreException {
        return null;
    }

    public class RoleBreakdown {

        private String[] roles;
        private Integer[] tenantIds;

        private String[] sharedRoles;
        private Integer[] sharedTenantids;

        public String[] getRoles() {
            if (roles != null) {
                return roles.clone();
            }
            return new String[0];
        }

        public void setRoles(String[] roles) {

            if (roles != null) {
                this.roles = roles.clone();
            }

        }

        public Integer[] getTenantIds() {

            if (tenantIds != null) {
                return tenantIds.clone();
            }
            return new Integer[0];
        }

        public void setTenantIds(Integer[] tenantIds) {
            if (tenantIds != null) {
                this.tenantIds = tenantIds.clone();

            }
        }

        public String[] getSharedRoles() {
            if (sharedRoles != null) {
                return sharedRoles.clone();
            }
            return new String[0];
        }

        public void setSharedRoles(String[] sharedRoles) {
            if (sharedRoles != null) {
                this.sharedRoles = sharedRoles.clone();
            }
        }

        public Integer[] getSharedTenantids() {
            if (sharedTenantids != null) {
                return sharedTenantids.clone();
            } else return new Integer[0];
        }

        public void setSharedTenantids(Integer[] sharedTenantids) {
            if (sharedTenantids != null) {
                this.sharedTenantids = sharedTenantids.clone();
            }
        }

    }

}
