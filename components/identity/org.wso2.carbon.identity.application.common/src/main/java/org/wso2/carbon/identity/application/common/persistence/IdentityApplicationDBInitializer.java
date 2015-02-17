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

import javax.sql.DataSource;
import java.io.*;
import java.sql.*;
import java.util.StringTokenizer;

/**
 * This class handles Identity Application Management database creation in the first start-up. It checks for the
 * SQL scripts for creating the tables inside $CARBON_HOME/dbscripts/service-provider directory.
 */
class IdentityApplicationDBInitializer {

    private static final String DB_CHECK_SQL = "SELECT ID FROM SP_APP";
    private static Log log = LogFactory.getLog(IdentityApplicationDBInitializer.class);
    Statement statement;
    private DataSource dataSource;
    private String delimiter = ";";

    IdentityApplicationDBInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Checks that a string buffer ends up with a given string. It may sound
     * trivial with the existing JDK API but the various implementation among
     * JDKs can make those methods extremely resource intensive and perform
     * poorly due to massive memory allocation and copying. See
     *
     * @param buffer the buffer to perform the check on
     * @param suffix the suffix
     * @return <code>true</code> if the character sequence represented by the
     * argument is a suffix of the character sequence represented by
     * the StringBuffer object; <code>false</code> otherwise. Note that the
     * result will be <code>true</code> if the argument is the
     * empty string.
     */
    private static boolean checkStringBufferEndsWith(StringBuffer buffer, String suffix) {

        if (suffix.length() > buffer.length()) {
            return false;
        }
        // this loop is done on purpose to avoid memory allocation performance
        // problems on various JDKs StringBuffer.lastIndexOf() was introduced
        // in jdk 1.4 and implementation is ok though does allocation/copying
        // StringBuffer.toString().endsWith() does massive memory
        // allocation/copying on JDK 1.5
        // See http://issues.apache.org/bugzilla/show_bug.cgi?id=37169
        int endIndex = suffix.length() - 1;
        int bufferIndex = buffer.length() - 1;
        while (endIndex >= 0) {
            if (buffer.charAt(bufferIndex) != suffix.charAt(endIndex)) {
                return false;
            }
            bufferIndex--;
            endIndex--;
        }
        return true;
    }

    void createIdentityProviderDB() throws IdentityApplicationManagementException {

        boolean isDBCreated = false;
        try {
            isDBCreated = isDatabaseStructureCreated();
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            String errorMsg = "Error occurred while checking if Identity Application Management " +
                    "DB structure has been created";
            throw new IdentityApplicationManagementException(errorMsg);
        }
        if (!isDBCreated) {
            Connection conn = null;
            try {
                conn = dataSource.getConnection();
                conn.setAutoCommit(false);
                statement = conn.createStatement();
                executeSQLScript();
                conn.commit();
                log.debug("Identity Application Management data store created successfully.");
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
                String msg = "Failed to create database tables for Identity Application Management data store";
                throw new IdentityApplicationManagementException(msg);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                String msg = "Failed to create database tables for Identity Application Management data store\"";
                throw new IdentityApplicationManagementException(msg);
            } finally {
                try {
                    if (conn != null) {
                        conn.close();
                    }
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                }
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Identity Application Management database already exists. Not creating a new database");
            }
        }
    }

    /**
     * Checks whether database tables are created.
     *
     * @return <code>true</core> if DB tables are created, <code>false</code> otherwise.
     */
    private boolean isDatabaseStructureCreated() throws SQLException {

        if (log.isDebugEnabled()) {
            log.debug("Running a query to test Identity Application Management database tables existence");
        }
        // check whether the tables are already created with a query
        Connection conn = dataSource.getConnection();
        Statement statement = null;
        try {
            statement = conn.createStatement();
            ResultSet rs;
            try {
                rs = statement.executeQuery(DB_CHECK_SQL);
            } catch (SQLException e) {
                // assuming here that tables have not been created
                return false;
            }
            if (rs != null) {
                rs.close();
            }
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } finally {
                if (conn != null) {
                    conn.close();
                }
            }
        }
        return true;
    }

    private void executeSQLScript() throws IdentityApplicationManagementException, SQLException, IOException {

        String databaseType = getDatabaseType(dataSource.getConnection());
        if (databaseType == null) {
            String msg = "Unsupported database: Database will not be created automatically by the Carbon Server. " +
                    "Please create the database using appropriate database scripts for the database.";
            log.warn(msg);
        }

        boolean keepFormat = false;

        if ("oracle".equals(databaseType)) {
            delimiter = "/";
        } else if ("db2".equals(databaseType)) {
            delimiter = "/";
        }

        String dbScriptLocation = getDbScriptLocation(databaseType);
        StringBuffer sql = new StringBuffer();
        BufferedReader reader = null;

        try {
            InputStream is = new FileInputStream(dbScriptLocation);
            reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!keepFormat) {
                    if (line.startsWith("//")) {
                        continue;
                    }
                    if (line.startsWith("--")) {
                        continue;
                    }
                    StringTokenizer st = new StringTokenizer(line);
                    if (st.hasMoreTokens()) {
                        String token = st.nextToken();
                        if ("REM".equalsIgnoreCase(token)) {
                            continue;
                        }
                    }
                }
                sql.append(keepFormat ? "\n" : " ").append(line);

                // SQL defines "--" as a comment to EOL
                // and in Oracle it may contain a hint
                // so we cannot just remove it, instead we must end it
                if (!keepFormat && line.contains("--")) {
                    sql.append("\n");
                }
                if ((checkStringBufferEndsWith(sql, delimiter))) {
                    executeSQL(sql.substring(0, sql.length() - delimiter.length()));
                    sql.replace(0, sql.length(), "");
                }
            }
            // Catch any statements not followed by ;
            if (sql.length() > 0) {
                executeSQL(sql.toString());
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    private String getDatabaseType(Connection conn) throws IdentityApplicationManagementException {

        String type = null;
        try {
            if (conn != null && (!conn.isClosed())) {
                DatabaseMetaData metaData = conn.getMetaData();
                String databaseProductName = metaData.getDatabaseProductName();
                if (databaseProductName.matches("(?i).*mysql.*")) {
                    type = "mysql";
                } else if (databaseProductName.matches("(?i).*oracle.*")) {
                    type = "oracle";
                } else if (databaseProductName.matches("(?i).*microsoft.*")) {
                    type = "mssql";
                } else if (databaseProductName.matches("(?i).*h2.*")) {
                    type = "h2";
                } else if (databaseProductName.matches("(?i).*db2.*")) {
                    type = "db2";
                } else if (databaseProductName.matches("(?i).*postgresql.*")) {
                    type = "postgresql";
                }
            } else {
                String msg = "Illegal arguments: Connection is either \'NULL\' or already closed";
                log.debug(msg);
                throw new IllegalArgumentException(msg);
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            String msg = "Failed to create Identity Application Management Database";
            throw new IdentityApplicationManagementException(msg);
        }
        return type;
    }

    private String getDbScriptLocation(String databaseType) {

        String scriptName = databaseType + ".sql";
        if (log.isDebugEnabled()) {
            log.debug("Loading database script from :" + scriptName);
        }
        String carbonHome = System.getProperty("carbon.home");
        return carbonHome + "/dbscripts/identity/application-mgt/" + scriptName;
    }

    private void executeSQL(String sql) throws SQLException {

        // Check and ignore empty statements
        if ("".equals(sql.trim())) {
            return;
        }
        Connection conn = null;
        ResultSet resultSet = null;
        try {
            if (log.isDebugEnabled()) {
                log.debug("SQL : " + sql);
            }
            boolean ret;
            int updateCount, updateCountTotal = 0;
            ret = statement.execute(sql);
            updateCount = statement.getUpdateCount();
            resultSet = statement.getResultSet();
            do {
                if (!ret) {
                    if (updateCount != -1) {
                        updateCountTotal += updateCount;
                    }
                }
                ret = statement.getMoreResults();
                if (ret) {
                    updateCount = statement.getUpdateCount();
                    resultSet = statement.getResultSet();
                }
            } while (ret);
            if (log.isDebugEnabled()) {
                log.debug(sql + " : " + updateCountTotal + " rows affected");
            }
            conn = dataSource.getConnection();
            SQLWarning warning = conn.getWarnings();
            while (warning != null) {
                log.debug(warning + " sql warning");
                warning = warning.getNextWarning();
            }
            conn.clearWarnings();
        } catch (SQLException e) {
            if (e.getSQLState().equals("42710")) {
                // eliminating the table already exception for the derby and DB2 database types
                if (log.isDebugEnabled()) {
                    log.info("Identity Application Management database already exists. Not creating a new database");
                }
            } else {
                throw e;
            }
        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }
}
