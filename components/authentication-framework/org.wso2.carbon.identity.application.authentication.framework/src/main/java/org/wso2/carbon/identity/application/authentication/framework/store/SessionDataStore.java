/*
*  Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.application.authentication.framework.store;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.cache.SessionContextCacheEntry;
import org.wso2.carbon.identity.application.authentication.framework.config.model.SequenceConfig;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.persistence.JDBCPersistenceManager;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.user.core.UserStoreConfigConstants;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

/**
 *
 */
public class SessionDataStore {

    private static final String SQL_SERIALIZE_OBJECT = "INSERT INTO IDN_AUTH_SESSION_STORE(SESSION_ID, SESSION_TYPE, SESSION_OBJECT, TIME_CREATED) VALUES (?, ?, ?, ?)";
    private static final String SQL_UPDATE_SERIALIZED_OBJECT =
            "UPDATE IDN_AUTH_SESSION_STORE SET SESSION_OBJECT =?, TIME_CREATED =? WHERE SESSION_ID =? AND SESSION_TYPE=?";
    private static final String SQL_DESERIALIZE_OBJECT = "SELECT SESSION_OBJECT FROM IDN_AUTH_SESSION_STORE WHERE SESSION_ID =? AND SESSION_TYPE=?";
    private static final String SQL_CHECK_SERIALIZED_OBJECT = "SELECT SESSION_ID FROM IDN_AUTH_SESSION_STORE WHERE SESSION_ID = ? AND SESSION_TYPE=?";
    private static final String SQL_DELETE_SERIALIZED_OBJECT = "DELETE FROM IDN_AUTH_SESSION_STORE WHERE SESSION_ID = ? AND SESSION_TYPE=?";
    private static final String SQL_DELETE_SERIALIZED_OBJECT_TASK = "DELETE FROM IDN_AUTH_SESSION_STORE WHERE TIME_CREATED<?";
    private static final String SQL_SELECT_TIME_CREATED = "SELECT TIME_CREATED FROM IDN_AUTH_SESSION_STORE WHERE SESSION_ID =? AND SESSION_TYPE =?";
    private static final String SQL_SELECT_LOGGED_IN_SESSION_IDS = "SELECT * FROM IDN_AUTH_SESSION_STORE AS a JOIN USER_SESSION_DATA AS b ON a.SESSION_ID = b.SESSION_ID " +
                                                                 "WHERE a.SESSION_TYPE=? AND b.TENANT_NAME =?";
    private static final String SQL_SELECT_SESSION_IDS_BY_USERNAME = "SELECT * FROM IDN_AUTH_SESSION_STORE AS a JOIN USER_SESSION_DATA AS b ON a.SESSION_ID = b.SESSION_ID" +
            " WHERE a.SESSION_TYPE=? AND b.TENANT_NAME =? AND b.USER_DOMAIN_NAME =? AND b.USER_NAME   =?";
    private static final String SQL_INSERT_USER_SESSION_DATA = "INSERT INTO USER_SESSION_DATA(SESSION_ID,USER_NAME,USER_DOMAIN_NAME,TENANT_NAME,LAST_UPDATED_TIME) VALUES (?, ?, ?, ?,?)";
    private static final String SQL_SELECT_SESSION_IDS_MAPPED_TO_USER_NAME="SELECT SESSION_ID FROM USER_SESSION_DATA WHERE USER_NAME=? AND USER_DOMAIN_NAME=? AND TENANT_NAME=?";

    private static int maxPoolSize = 100;
    private static BlockingDeque<SessionContextDO> sessionContextQueue = new LinkedBlockingDeque<SessionContextDO>();
    private static Log log = LogFactory.getLog(SessionDataStore.class);
    private static final String SESSION_CONTEXT_CACHE_NAME = "AppAuthFrameworkSessionContextCache";

    static {

        try {
            maxPoolSize =
                    Integer.parseInt(IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.PoolSize"));
        } catch (Exception e) {
        }

        if (maxPoolSize > 0) {
            log.info("Thread pool size for session persistent consumer : " + maxPoolSize);

            ExecutorService threadPool = Executors.newFixedThreadPool(maxPoolSize);

            for (int i = 0; i < maxPoolSize; i++) {
                threadPool.execute(new SessionDataPersistTask(sessionContextQueue));
            }
        }

    }
    private static volatile SessionDataStore instance;
    private JDBCPersistenceManager jdbcPersistenceManager;
    private boolean enablePersist;
    private String sqlStore;
    private String sqlUpdate;
    private String sqlDelete;
    private String sqlCheck;
    private String sqlSelect;
    private String sqlDeleteTask;

    private SessionDataStore() {
        try {

            jdbcPersistenceManager = JDBCPersistenceManager.getInstance();
            enablePersist = Boolean.parseBoolean(IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.Enable"));
            String sqlStore = IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.SQL.Store");
            String sqlUpdate = IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.SQL.Update");
            String sqlDelete = IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.SQL.Delete");
            String sqlCheck = IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.SQL.Check");
            String sqlSelect = IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.SQL.Select");
            String sqlDeleteTask = IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.SQL.Task");
            if (sqlStore != null && sqlStore.trim().length() > 0) {
                this.sqlStore = sqlStore;
            } else {
                this.sqlStore = SQL_SERIALIZE_OBJECT;
            }
            if (sqlUpdate != null && sqlUpdate.trim().length() > 0) {
                this.sqlUpdate = sqlUpdate;
            } else {
                this.sqlUpdate = SQL_UPDATE_SERIALIZED_OBJECT;
            }
            if (sqlDelete != null && sqlDelete.trim().length() > 0) {
                this.sqlDelete = sqlDelete;
            } else {
                this.sqlDelete = SQL_DELETE_SERIALIZED_OBJECT;
            }
            if (sqlCheck != null && sqlCheck.trim().length() > 0) {
                this.sqlCheck = sqlCheck;
            } else {
                this.sqlCheck = SQL_CHECK_SERIALIZED_OBJECT;
            }
            if (sqlSelect != null && sqlSelect.trim().length() > 0) {
                this.sqlSelect = sqlSelect;
            } else {
                this.sqlSelect = SQL_DESERIALIZE_OBJECT;
            }
            if (sqlDeleteTask != null && sqlDeleteTask.trim().length() > 0) {
                this.sqlDeleteTask = sqlDeleteTask;
            } else {
                this.sqlDeleteTask = SQL_DELETE_SERIALIZED_OBJECT_TASK;
            }
        } catch (IdentityException e) {
            //ignore
            log.error("Error while loading session data store manager", e);
        }

        if (!enablePersist) {
            log.info("Session Data Persistence of Authentication framework is not enabled.");
        }

        if (Boolean.parseBoolean(IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.CleanUp.Enable"))) {
            String sessionCleanupPeriod = IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.CleanUp.Period");
            if (sessionCleanupPeriod == null || sessionCleanupPeriod.trim().length() == 0) {
                // default period is set to 1 day
                sessionCleanupPeriod = "1140";
            }
            long sessionCleanupTime = Long.parseLong(sessionCleanupPeriod);
            SessionCleanUpService sessionCleanUpService = new SessionCleanUpService(sessionCleanupTime,
                    sessionCleanupTime);
            sessionCleanUpService.activateCleanUp();
        } else {
            log.info("Session Data CleanUp Task of Authentication framework is not enabled.");
        }
    }

    public static SessionDataStore getInstance() {
        if (instance == null) {
            synchronized (SessionDataStore.class) {
                if (instance == null) {
                    instance = new SessionDataStore();
                }
            }
        }
        return instance;
    }


    public Object getSessionData(String key, String type) {

        if (!enablePersist) {
            return null;
        }

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            connection = jdbcPersistenceManager.getDBConnection();
            preparedStatement = connection.prepareStatement(sqlSelect);
            preparedStatement.setString(1, key);
            preparedStatement.setString(2, type);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return getBlobObject(resultSet.getBinaryStream(1));
            }
        } catch (IdentityException e) {
            //ignore
            log.error("Error while retrieving session data", e);
        } catch (ClassNotFoundException e) {
            //ignore
            log.error("Error while retrieving session data", e);
        } catch (SQLException e) {
            //ignore
            log.error("Error while retrieving session data", e);
        } catch (IOException e) {
            //ignore
            log.error("Error while retrieving session data", e);
        } catch (IdentityApplicationManagementException e) {
            //ignore
            log.error("Error while retrieving session data", e);
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                log.error("Error while closing the stream", e);
            }
        }

        return null;
    }

    public void storeSessionData(String key, String type, Object entry) {

       if (!enablePersist) {
            return;
        }

        if (maxPoolSize > 0) {
            sessionContextQueue.push(new SessionContextDO(key, type, entry));
        } else {
            persistSessionData(key, type, entry);
        }
    }

    public void clearSessionData(String key, String type) {

        if (!enablePersist) {
            return;
        }

        if (maxPoolSize > 0) {
            sessionContextQueue.push(new SessionContextDO(key, type, null));
        } else {
            removeSessionData(key, type);
        }
    }

    public void removeExpiredSessionData(Timestamp timestamp) {

        Connection connection = null;
        PreparedStatement statement = null;

        try {
            connection = jdbcPersistenceManager.getDBConnection();
            statement = connection.prepareStatement(sqlDeleteTask);
            statement.setTimestamp(1, timestamp);
            statement.execute();
            if (!connection.getAutoCommit()) {
                connection.commit();
            }
        } catch (SQLException e) {
            log.error("Error while removing Session Data ", e);
        } catch (IdentityException e) {
            log.error("Error while removing Session Data", e);
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                log.error("Error while closing the stream", e);
            }
        }

    }

    private boolean isExist(String key, String type) {

        if (!enablePersist) {
            return false;
        }

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            connection = jdbcPersistenceManager.getDBConnection();
            preparedStatement = connection.prepareStatement(sqlCheck);
            preparedStatement.setString(1, key);
            preparedStatement.setString(2, type);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return true;
            }
        } catch (IdentityException e) {
            //ignore
            log.error("Error while retrieving session data", e);
        } catch (SQLException e) {
            //ignore
            log.error("Error while retrieving session data", e);
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                log.error("Error while closing the stream", e);
            }
        }

        return false;
    }

    /**
     * Store Session data if SessionDataPersist is enable.
     * @param key String.
     * @param type String.
     * @param entry Object.
     */
    public void persistSessionData(String key, String type, Object entry) {
        if (!enablePersist) {
            return;
        }
        boolean isExist = isExist(key, type);
        boolean IsLoggedIn = false;
        String userName = "";
        String tenantDomainName = "";
        String userStoreDomain = "";

        Connection connection = null;
        Connection usdConnection = null;
        PreparedStatement preparedStatement = null;
        PreparedStatement usdPreparedStatement = null;
        ResultSet resultSet = null;
        Timestamp timestamp = new java.sql.Timestamp(new java.util.Date().getTime());
        try {
            connection = jdbcPersistenceManager.getDBConnection();
            connection.setAutoCommit(false);
            if (isExist) {
                preparedStatement = connection.prepareStatement(SQL_SELECT_TIME_CREATED);
                preparedStatement.setString(1, key);
                preparedStatement.setString(2, type);
                resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    timestamp = resultSet.getTimestamp(1);
                    if (preparedStatement != null) {
                        preparedStatement.close();
                    }
                }
                preparedStatement = connection.prepareStatement(sqlUpdate);
                setBlobObject(preparedStatement, entry, 1);
                preparedStatement.setTimestamp(2, timestamp);
                preparedStatement.setString(3, key);
                preparedStatement.setString(4, type);
            } else {
                preparedStatement = connection.prepareStatement(sqlStore);
                preparedStatement.setString(1, key);
                preparedStatement.setString(2, type);
                setBlobObject(preparedStatement, entry, 3);
                preparedStatement.setTimestamp(4, timestamp);
                usdConnection = jdbcPersistenceManager.getDBConnection();
                usdConnection.setAutoCommit(false);
                if (SESSION_CONTEXT_CACHE_NAME.equalsIgnoreCase(type)) {
                    IsLoggedIn = true;
                    usdPreparedStatement = usdConnection.prepareStatement(SQL_INSERT_USER_SESSION_DATA);
                    usdPreparedStatement.setString(1, key);
                    usdPreparedStatement.setString(1,key);
                    if(entry instanceof SessionContextCacheEntry) {
                        Set<Map.Entry<String, SequenceConfig>> sessions = ((SessionContextCacheEntry) entry)
                                .getContext().getAuthenticatedSequences().entrySet();
                        for (Map.Entry<String, SequenceConfig> session : sessions) {
                            userName = session.getValue().getAuthenticatedUser().getUserName();
                            userStoreDomain = session.getValue().getAuthenticatedUser().getUserStoreDomain();
                            tenantDomainName = session.getValue().getAuthenticatedUser().getTenantDomain();
                            if (userStoreDomain != null) {
                                if (userStoreDomain.length() < 1) {
                                    userStoreDomain = UserStoreConfigConstants.PRIMARY;
                                }
                            } else {
                                userStoreDomain = UserStoreConfigConstants.PRIMARY;
                            }
                        }
                    }
                    usdPreparedStatement.setString(2, userName);
                    usdPreparedStatement.setString(3, userStoreDomain);
                    usdPreparedStatement.setString(4, tenantDomainName);
                    usdPreparedStatement.setTimestamp(5, timestamp);
                }
            }
            preparedStatement.executeUpdate();
            connection.commit();
            if (IsLoggedIn) {
                usdPreparedStatement.executeUpdate();
                usdConnection.commit();
            }
        } catch (IdentityException e) {
            //ignore
            log.error("Error while storing session data", e);
        } catch (SQLException e) {
            //ignore
            log.error("Error while storing session data", e);
        } catch (IOException e) {
            //ignore
            log.error("Error while storing session data", e);
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
                if (connection != null) {
                    connection.close();
                }
                if (usdPreparedStatement != null) {
                    usdPreparedStatement.close();
                }
                if (usdConnection != null) {
                    usdConnection.close();
                }
            } catch (SQLException e) {
                log.error("Error while closing the stream", e);
            }
        }
    }


    public void removeSessionData(String key, String type) {

        if (!enablePersist) {
            return;
        }

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = jdbcPersistenceManager.getDBConnection();

            preparedStatement = connection.prepareStatement(sqlDelete);
            preparedStatement.setString(1, key);
            preparedStatement.setString(2, type);
            preparedStatement.executeUpdate();
            connection.commit();
        } catch (IdentityException e) {
            //ignore
            log.error("Error while deleting session data", e);
        } catch (SQLException e) {
            //ignore
            log.error("Error while deleting session data", e);
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                log.error("Error while closing the stream", e);
            }
        }
    }

    private void setBlobObject(PreparedStatement prepStmt, Object value, int index)
            throws SQLException, IOException {
        if (value != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(value);
            oos.flush();
            oos.close();
            InputStream inputStream = new ByteArrayInputStream(baos.toByteArray());
            if (inputStream != null) {
                prepStmt.setBinaryStream(index, inputStream, inputStream.available());
            } else {
                prepStmt.setBinaryStream(index, inputStream, 0);
            }
        } else {
            prepStmt.setBinaryStream(index, null, 0);
        }
    }

    private Object getBlobObject(InputStream is) throws IdentityApplicationManagementException,
            IOException, ClassNotFoundException {
        if (is != null) {
            BufferedReader br = null;
            Object object = null;
            try {
                ObjectInput ois = new ObjectInputStream(is);
                object = ois.readObject();
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                        throw new IdentityApplicationManagementException(e);
                    }
                }
            }
            return object;
        }
        return null;
    }

    /**
     * Get all logged in sessionIds From Database.
     * @param type String.
     * @param tenantName String.
     * @return cacheKey List.
     */
    public List<String> getLoggedInSessionIdsFromDb(String type, String tenantName) {
        List<String> cacheKeyList = null;
        if (!enablePersist) {
            return cacheKeyList;
        }
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet;
        try {
            cacheKeyList = new ArrayList<String>();
            connection = jdbcPersistenceManager.getDBConnection();
            connection.setAutoCommit(false);
            preparedStatement = connection.prepareStatement(SQL_SELECT_LOGGED_IN_SESSION_IDS);
            preparedStatement.setString(1, type);
            preparedStatement.setString(2, tenantName);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                cacheKeyList.add(resultSet.getString(1));
            }
        } catch (IdentityException e) {
            //ignore
            log.error("Error while getting session data", e);
        } catch (SQLException e) {
            //ignore
            log.error("Error while getting session data", e);
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                log.error("Error while closing the stream", e);
            }
        }
        return cacheKeyList;
    }

    /**
     * Get all logged in sessionIds From Database.
     *
     * @param type String.
     * @return cacheKey List.
     */
    public List<String> getSessionIdsFromDbByUserName(String type, String tenantName, String userName, String userDomain) {
        List<String> cacheKeyList = null;
        if (!enablePersist) {
            return cacheKeyList;
        }
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet;
        try {
            cacheKeyList = new ArrayList<String>();
            connection = jdbcPersistenceManager.getDBConnection();
            connection.setAutoCommit(false);
            preparedStatement = connection.prepareStatement(SQL_SELECT_SESSION_IDS_BY_USERNAME);
            preparedStatement.setString(1, type);
            preparedStatement.setString(2, tenantName);
            preparedStatement.setString(3, userDomain);
            preparedStatement.setString(4, userName);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                cacheKeyList.add(resultSet.getString(1));
            }
        } catch (IdentityException e) {
            //ignore
            log.error("Error while getting session data", e);
        } catch (SQLException e) {
            //ignore
            log.error("Error while getting session data", e);
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                log.error("Error while closing the stream", e);
            }
        }
        return cacheKeyList;
    }

    /**
     * Get Session Created Time.
     *
     * @param key  String.
     * @param type String.
     * @return sessionCreatedTimeStamp.
     */
    public Timestamp getSessionCreatedTime(String key, String type) {
        Timestamp timestamp = null;
        if (!enablePersist) {
            return timestamp;
        }
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            connection = jdbcPersistenceManager.getDBConnection();
            connection.setAutoCommit(false);
            preparedStatement = connection.prepareStatement(SQL_SELECT_TIME_CREATED);
            preparedStatement.setString(1, key);
            preparedStatement.setString(2, type);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                timestamp = resultSet.getTimestamp(1);
            }
        } catch (IdentityException e) {
            //ignore
            log.error("Error while getting session data", e);
        } catch (SQLException e) {
            //ignore
            log.error("Error while getting session data", e);
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                log.error("Error while closing the stream", e);
            }
        }
        return timestamp;
    }

    /**
     * Get SessionIds for a given user name.
     *
     * @param userName        String.
     * @param tenantDomain    String.
     * @param userStoreDomain String.
     * @return sessionIdsList.
     */
    public List<String> getSessionIdsForUserName(String userName, String tenantDomain, String userStoreDomain) {
        List<String> sessionIdsList = null;
        if (!enablePersist) {
            return sessionIdsList;
        }
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            sessionIdsList = new ArrayList<String>();
            connection = jdbcPersistenceManager.getDBConnection();
            connection.setAutoCommit(false);
            preparedStatement = connection.prepareStatement(SQL_SELECT_SESSION_IDS_MAPPED_TO_USER_NAME);
            preparedStatement.setString(1, userName);
            preparedStatement.setString(2, userStoreDomain);
            preparedStatement.setString(3, tenantDomain);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                sessionIdsList.add(resultSet.getString(1));
            }
        } catch (IdentityException e) {
            //ignore
            log.error("Error while getting session data", e);
        } catch (SQLException e) {
            //ignore
            log.error("Error while getting session data", e);
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                log.error("Error while closing the stream", e);
            }
        }
        return sessionIdsList;
    }
}
