/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.application.authentication.framework.store;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.persistence.JDBCPersistenceManager;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.idp.mgt.util.IdPManagementUtil;
import org.wso2.carbon.user.core.util.DatabaseUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

/**
 *
 */
public class SessionDataStore {

    private static final String SQL_SERIALIZE_OBJECT = "INSERT INTO IDN_AUTH_SESSION_STORE(SESSION_ID, SESSION_TYPE, " +
                                                       "SESSION_OBJECT, TIME_CREATED) VALUES (?, ?, ?, ?)";
    private static final String SQL_UPDATE_SERIALIZED_OBJECT =
            "UPDATE IDN_AUTH_SESSION_STORE SET SESSION_OBJECT =?, TIME_CREATED =? WHERE SESSION_ID =? AND " +
            "SESSION_TYPE=?";
    private static final String SQL_DESERIALIZE_OBJECT = "SELECT SESSION_OBJECT FROM IDN_AUTH_SESSION_STORE WHERE " +
                                                         "SESSION_ID =? AND SESSION_TYPE=?";
    private static final String SQL_CHECK_SERIALIZED_OBJECT = "SELECT SESSION_ID FROM IDN_AUTH_SESSION_STORE WHERE " +
                                                              "SESSION_ID = ? AND SESSION_TYPE=?";
    private static final String SQL_DELETE_SERIALIZED_OBJECT = "DELETE FROM IDN_AUTH_SESSION_STORE WHERE SESSION_ID =" +
                                                               " ? AND SESSION_TYPE=?";
    private static final String SQL_DELETE_SERIALIZED_OBJECT_TASK = "DELETE FROM IDN_AUTH_SESSION_STORE WHERE " +
                                                                    "TIME_CREATED<?";
    private static final String SQL_SELECT_TIME_CREATED = "SELECT TIME_CREATED FROM IDN_AUTH_SESSION_STORE WHERE " +
                                                          "SESSION_ID =? AND SESSION_TYPE =?";
    private static final Log log = LogFactory.getLog(SessionDataStore.class);
    private static int maxPoolSize = 100;
    private static BlockingDeque<SessionContextDO> sessionContextQueue = new LinkedBlockingDeque<SessionContextDO>();
    private static volatile SessionDataStore instance;
    private JDBCPersistenceManager jdbcPersistenceManager;
    private boolean enablePersist;
    private String sqlStore;
    private String sqlUpdate;
    private String sqlDelete;
    private String sqlCheck;
    private String sqlSelect;
    private String sqlDeleteTask;

    static {

        try {
            String maxPoolSizeConfigValue = IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist" +
                                                                     ".PoolSize");
            if (StringUtils.isNotBlank(maxPoolSizeConfigValue)) {
                maxPoolSize = Integer.parseInt(maxPoolSizeConfigValue);
            }
        } catch (NumberFormatException e) {
            if(log.isDebugEnabled()) {
                log.debug("Exception ignored : ", e);
            }
            log.warn("Session data persistence pool size is not configured. Using default value.");
        }

        if (maxPoolSize > 0) {
            log.info("Thread pool size for session persistent consumer : " + maxPoolSize);

            ExecutorService threadPool = Executors.newFixedThreadPool(maxPoolSize);

            for (int i = 0; i < maxPoolSize; i++) {
                threadPool.execute(new SessionDataPersistTask(sessionContextQueue));
            }
        }

    }

    private SessionDataStore() {
        try {

            jdbcPersistenceManager = JDBCPersistenceManager.getInstance();
            //hidden config parameter
            String enablePersistVal = IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.Enable");
            enablePersist = true;
            if(enablePersistVal != null){
                enablePersist = Boolean.parseBoolean(enablePersistVal);
            }
            String storeSQL = IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.SQL.Store");
            String updateSQL = IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.SQL.Update");
            String deleteSQL = IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.SQL.Delete");
            String checkSQL = IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.SQL.Check");
            String selectSQL = IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.SQL.Select");
            String deleteTaskSQL = IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.SQL.Task");
            if (storeSQL != null && storeSQL.trim().length() > 0) {
                this.sqlStore = storeSQL;
            } else {
                this.sqlStore = SQL_SERIALIZE_OBJECT;
            }
            if (updateSQL != null && updateSQL.trim().length() > 0) {
                this.sqlUpdate = updateSQL;
            } else {
                this.sqlUpdate = SQL_UPDATE_SERIALIZED_OBJECT;
            }
            if (deleteSQL != null && deleteSQL.trim().length() > 0) {
                this.sqlDelete = deleteSQL;
            } else {
                this.sqlDelete = SQL_DELETE_SERIALIZED_OBJECT;
            }
            if (checkSQL != null && checkSQL.trim().length() > 0) {
                this.sqlCheck = checkSQL;
            } else {
                this.sqlCheck = SQL_CHECK_SERIALIZED_OBJECT;
            }
            if (selectSQL != null && selectSQL.trim().length() > 0) {
                this.sqlSelect = selectSQL;
            } else {
                this.sqlSelect = SQL_DESERIALIZE_OBJECT;
            }
            if (deleteTaskSQL != null && deleteTaskSQL.trim().length() > 0) {
                this.sqlDeleteTask = deleteTaskSQL;
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
        //hidden config parameter
        String isCleanUpEnabledVal = IdentityUtil.getProperty("JDBCPersistenceManager" +
                                                                                ".SessionDataPersist.CleanUp.Enable");
        if (isCleanUpEnabledVal == null) {
            isCleanUpEnabledVal = "true";
        }
        if (Boolean.parseBoolean(isCleanUpEnabledVal)) {
            long sessionCleanupPeriod = IdPManagementUtil.
                    getCleanUpPeriod(CarbonContext.getThreadLocalCarbonContext().getTenantDomain());
            SessionCleanUpService sessionCleanUpService = new SessionCleanUpService(sessionCleanupPeriod,
                    sessionCleanupPeriod);
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
        } catch (IdentityException | ClassNotFoundException | IOException | SQLException |
                IdentityApplicationManagementException e) {
            //ignore
            log.error("Error while retrieving session data", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, resultSet, preparedStatement);
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
            log.error("Error while removing session data from the database for the timestamp " + timestamp.toString(), e);
        } catch (IdentityException e) {
            log.error("Error while obtaining the database connection", e);
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
        } catch (IdentityException | SQLException e) {
            //ignore
            log.error("Error while retrieving session data", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, resultSet, preparedStatement);
        }

        return false;
    }

    public void persistSessionData(String key, String type, Object entry) {

        if (!enablePersist) {
            return;
        }

        boolean isExist = isExist(key, type);


        Connection connection = null;
        PreparedStatement preparedStatement = null;
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
                    preparedStatement.close();
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
            }
            preparedStatement.executeUpdate();
            connection.commit();
        } catch (IdentityException | SQLException | IOException e) {
            //ignore
            log.error("Error while storing session data", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, resultSet, preparedStatement);
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
        } catch (IdentityException | SQLException e) {
            //ignore
            log.error("Error while deleting session data", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, preparedStatement);
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
            prepStmt.setBinaryStream(index, inputStream, inputStream.available());
        } else {
            prepStmt.setBinaryStream(index, null, 0);
        }
    }

    private Object getBlobObject(InputStream is) throws IdentityApplicationManagementException,
                                                        IOException, ClassNotFoundException {
        if (is != null) {
            ObjectInput ois = null;
            Object object = null;
            try {
                ois = new ObjectInputStream(is);
                object = ois.readObject();
            } finally {
                if (ois != null) {
                    try {
                        ois.close();
                    } catch (IOException e) {
                        log.error("IOException while trying to close ObjectInputStream.", e);
                    }
                }
            }
            return object;
        }
        return null;
    }

    public Timestamp getTimeStamp(String key, String type) {
        boolean isExist = isExist(key, type);

        if (isExist) {
            Connection connection = null;
            PreparedStatement preparedStatement = null;
            ResultSet resultSet = null;
            Timestamp timestamp = null;
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
            } catch (SQLException e) {
                //ignore
                log.error("Error while storing session data in the database for key = " + key + " and type = " + type, e);
            } catch (IdentityException e) {
                //ignore
                log.error("Error while obtaining the database connection", e);
            } finally {
                if(resultSet != null){
                    try{
                        resultSet.close();
                    } catch (SQLException e){
                        log.error("Error when closing the result set of session time stamps");
                    }
                }
                DatabaseUtil.closeAllConnections(connection, preparedStatement);
            }
            return timestamp;
        } else {
            return null;
        }
    }

}
