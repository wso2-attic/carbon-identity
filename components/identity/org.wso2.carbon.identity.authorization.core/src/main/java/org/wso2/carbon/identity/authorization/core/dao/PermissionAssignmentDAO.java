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
import org.wso2.carbon.identity.authorization.core.dto.PermissionAssignment;
import org.wso2.carbon.identity.authorization.core.jdbc.dao.JDBCConstantsDAO;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * @author venura
 * @date May 16, 2013
 */
public abstract class PermissionAssignmentDAO extends GenericDAO {

    private static Log log = LogFactory.getLog(GenericDAO.class);

    private int id;
    private int permissionId;
    private boolean authorized;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(int permissionId) {
        this.permissionId = permissionId;
    }

    public boolean isAuthorized() {
        return authorized;
    }

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }

    public void map(PermissionAssignment assignment) {
        // super.map(assignment);
        id = assignment.getId();
        permissionId = assignment.getPermissionId();
        authorized = assignment.isAuthorized();
    }

    public List<? extends GenericDAO> load(Connection connection) throws UserStoreException {
        return null;
    }

    public void save(Connection connection) throws UserStoreException {

        log.info("Module save state: " + getStatus());
        try {
            if (getStatus() == JDBCConstantsDAO.INSERT) {
                connection.setAutoCommit(false);
                delete(connection, false);
                insert(connection, false);
                connection.commit();
            } else {
                super.save(connection);
                return;
            }
        } catch (SQLException e) {
            String error = "Error while setting the connection to autocommit false ";
            log.error(error, e);
            throw new UserStoreException(error, e);
        } finally {
            DatabaseUtil.closeConnection(connection);
        }
    }

    @Override
    public int getIdentifier() {
        return id;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(super.toString());
        builder.append("{").append(getClass()).append(" Permission Assignment ID: ").append(id)
                .append(" Permission Id: ").append(permissionId).append(" Authorized: ")
                .append(authorized).append("}");
        return builder.toString();
    }

}
