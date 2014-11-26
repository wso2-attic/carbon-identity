/*
 *Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */

package org.wso2.carbon.identity.application.common.persistence;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.config.IdentityApplicationConfig;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationManagementUtil;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.xml.namespace.QName;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * This class is used for handling Identity Application Management data persistence in a JDBC Store.
 * It reads the data source properties from the JNDI name given in application-authentication.xml.
 * During the server start-up, it checks whether the database is created, if not it creates one.
 * This is implemented as a singleton. An instance of this class can be obtained through
 * JDBCPersistenceManager.getInstance() method.
 */
public class JDBCPersistenceManager {

    private static Log log = LogFactory.getLog(JDBCPersistenceManager.class);
    private static volatile JDBCPersistenceManager instance;
    private DataSource dataSource;

    /**
     * Get an instance of the JDBCPersistenceManager.
     * It implements a lazy initialization with double checked locking.
     *
     * @return JDBCPersistenceManager instance
     * @throws IdentityApplicationManagementException Error when reading the data source configurations
     */
    public static JDBCPersistenceManager getInstance() throws IdentityApplicationManagementException {

        if (instance == null) {
            synchronized (JDBCPersistenceManager.class) {
                if (instance == null) {
                    instance = new JDBCPersistenceManager();
                }
            }
        }
        return instance;
    }

    private JDBCPersistenceManager() throws IdentityApplicationManagementException {
        initDataSource();
    }

    private void initDataSource() throws IdentityApplicationManagementException {

        try {
            String dataSourceName = IdentityApplicationConfig.getInstance().getConfigElement("JDBCPersistenceManager")
                    .getFirstChildWithName(new QName(
                            IdentityApplicationConstants.APPLICATION_AUTHENTICATION_DEFAULT_NAMESPACE, "DataSource"))
                    .getFirstChildWithName(new QName(
                            IdentityApplicationConstants.APPLICATION_AUTHENTICATION_DEFAULT_NAMESPACE, "Name"))
                    .getText();
            Context ctx = new InitialContext();
            dataSource = (DataSource) ctx.lookup(dataSourceName);
        }  catch (NamingException e) {
            log.error(e.getMessage(), e);
            String errorMsg = "Error occurred while looking up the Identity Application Management data source";
            throw new IdentityApplicationManagementException(errorMsg);
        }  catch (Exception e) {
            log.error(e.getMessage(), e);
            String errorMsg = "Error occurred while reading " +
                    IdentityApplicationConstants.APPLICATION_AUTHENTICATION_CONGIG;
            throw new IdentityApplicationManagementException(errorMsg);
        }
    }

    /**
     * Get an instance of the JDBCPersistenceManager.
     * It implements a lazy initialization with double checked locking.
     *
     * @throws IdentityApplicationManagementException Error when initializing Identity Application Management data store
     */
    public void initializeDatabase() throws IdentityApplicationManagementException {

        IdentityApplicationDBInitializer dbInitializer = new IdentityApplicationDBInitializer(dataSource);
        try {
            dbInitializer.createIdentityProviderDB();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            String errorMsg = "Error when creating the Identity Application Management data store";
            throw new IdentityApplicationManagementException(errorMsg);
        }
    }

    /**
     * Returns a database connection for Identity Application Management data store.
     *
     * @return Database connection
     * @throws IdentityApplicationManagementException Error when getting DB connection
     *         on the Identity Provider Management data source
     */
    public Connection getDBConnection() throws IdentityApplicationManagementException {

        Connection dbConnection = null;
        try {
            dbConnection = dataSource.getConnection();
            if (dbConnection.getTransactionIsolation() != Connection.TRANSACTION_READ_COMMITTED) {
                dbConnection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            }
            dbConnection.setAutoCommit(false);
            return dbConnection;
        } catch (SQLException e) {
            String errorMsg = "Error occurred while trying to get a database connection on " +
                    "Identity Application Management data source";
            log.error(errorMsg, e);
            if(dbConnection != null){
                IdentityApplicationManagementUtil.closeConnection(dbConnection);
            }
            throw new IdentityApplicationManagementException(errorMsg);
        }
    }

}
