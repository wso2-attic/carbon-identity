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

package org.wso2.carbon.identity.authorization.core.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.authorization.core.AuthorizationKey;
import org.wso2.carbon.identity.authorization.core.jdbc.dao.JDBCConstantsDAO;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * @author venura
 * @date May 14, 2013
 */
public abstract class GenericDAO {

    protected static final String WHERE = " WHERE ";
    protected static final String AND = " AND ";
    private static Log log = LogFactory.getLog(GenericDAO.class);
    protected String appendTxt = " WHERE ";
    private byte status;
    private int tenantId;

    public byte getStatus() {
        return status;
    }

    public void setStatus(byte status) {
        this.status = status;
    }

    public int getTenantId() {
        return tenantId;
    }

    public void setTenantId(int tenantId) {
        this.tenantId = tenantId;
    }

    public void save(Connection connection) throws UserStoreException {
        save(connection, true);
    }

    public void save(Connection connection, boolean commit) throws UserStoreException {

        log.debug(this.toString());
        try {
            if (connection.getAutoCommit()) {
                connection.setAutoCommit(false);
            }

            deleteObjects(connection);
            if (status == JDBCConstantsDAO.INSERT) {
                insert(connection, false);
            } else if (status == JDBCConstantsDAO.UPDATE) {
                update(connection, false);
            } else if (status == JDBCConstantsDAO.DELETE) {
                delete(connection, false);
            }
            saveDependentModules(connection, false);
            if (commit) {
                connection.commit();
            }

        } catch (SQLException e) {
            String error = "Error while setting the connection to autocommit false ";
            log.error(error, e);
            try {
                connection.rollback();
            } catch (SQLException e1) {
                log.error(e);
            }
            throw new UserStoreException(error, e);
        } finally {
            if (commit) {
                DatabaseUtil.closeConnection(connection);
            }
        }
    }

    protected void deleteObjects(Connection connection) throws SQLException, UserStoreException {

    }

    protected void insert(Connection connection, boolean commit) throws UserStoreException {
        PreparedStatement stmt = null;
        ResultSet res = null;
        try {
            insert(stmt, res, connection);
        } catch (SQLException e) {
            String error = "Insertion faild for the permission";
            log.error(error + e.getMessage());
            throw new UserStoreException(error + e.getMessage());

        } finally {
            if (!commit) {
                DatabaseUtil.closeAllConnections(null, res, stmt);
            } else {
                commitConnection(connection);
                DatabaseUtil.closeAllConnections(connection, res, stmt);
            }
        }

    }

    protected abstract void insert(PreparedStatement stmt, ResultSet res, Connection connection)
            throws SQLException,
            UserStoreException;

    protected abstract void update(Connection connection, boolean commit) throws UserStoreException;

    protected abstract void delete(Connection connection, boolean commit) throws UserStoreException;

    protected abstract void saveDependentModules(Connection connection, boolean commit)
            throws UserStoreException;

    public abstract List<? extends GenericDAO> load(Connection connection)
            throws UserStoreException;

    public List<? extends GenericDAO> load(Connection connection, boolean closeConnection)
            throws UserStoreException {
        throw new UserStoreException("Need implementation");
    }

    protected void resetAppendTxt() {
        appendTxt = WHERE;
    }

    protected void commitConnection(Connection connection) throws UserStoreException {
        try {
            connection.commit();
        } catch (SQLException e) {
            String error = "Error commiting changes to the database " + e.getMessage();
            log.error(error);
            throw new UserStoreException(error, e);
        }

    }

    public abstract int getIdentifier();

    public Map<AuthorizationKey, Boolean> createCacheEntry(Connection connection)
            throws SQLException,
            UserStoreException {
        return null;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(super.toString());
        builder.append("{").append(getClass()).append(" Status: ").append(status)
                .append(" Tenant: ").append(tenantId).append("}");
        return builder.toString();
    }

}
