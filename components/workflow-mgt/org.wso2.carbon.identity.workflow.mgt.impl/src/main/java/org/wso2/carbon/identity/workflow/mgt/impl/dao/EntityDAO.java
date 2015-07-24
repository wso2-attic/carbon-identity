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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.workflow.mgt.impl.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.persistence.JDBCPersistenceManager;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class EntityDAO {

    private static Log log = LogFactory.getLog(EntityDAO.class);

    public boolean updateEntityLockedState(String entityName, String entityType, String operation) throws
            WorkflowException {

        Connection connection = null;
        PreparedStatement prepStmtGet = null;
        PreparedStatement prepStmtSelect = null;
        ResultSet results;
        try {
            connection = IdentityDatabaseUtil.getDBConnection();;
            prepStmtGet = connection.prepareStatement(SQLConstants.GET_ENTITY_STATE_QUERY);
            prepStmtGet.setString(1, entityName);
            prepStmtGet.setString(2, entityType);
            prepStmtGet.setString(3, "Operation");
            prepStmtGet.setString(4, operation);
            results = prepStmtGet.executeQuery();
            if (results.next()) {
                return false;
            } else {
                prepStmtSelect = connection.prepareStatement(SQLConstants.ADD_ENTITY_STATE_QUERY);
                prepStmtSelect.setString(1, entityName);
                prepStmtSelect.setString(2, entityType);
                prepStmtSelect.setString(3, "Operation");
                prepStmtSelect.setString(4, operation);
                prepStmtSelect.execute();
            }
            connection.commit();
        } catch (SQLException | IdentityException e) {
            throw new WorkflowException("Error while saving new user data for Identity database.", e);
        } finally {
            IdentityDatabaseUtil.closeStatement(prepStmtSelect);
            IdentityDatabaseUtil.closeStatement(prepStmtGet);
            IdentityDatabaseUtil.closeConnection(connection);
        }
        return true;
    }

    public void deleteEntityLockedState(String entityName, String entityType, String operation) throws
            WorkflowException {

        Connection connection = null;
        PreparedStatement prepStmt = null;
        try {
            connection = IdentityDatabaseUtil.getDBConnection();;
            prepStmt = connection.prepareStatement(SQLConstants.DELETE_ENTITY_STATE_QUERY);
            prepStmt.setString(1, entityName);
            prepStmt.setString(2, entityType);
            prepStmt.setString(3, "Operation");
            prepStmt.setString(4, operation);
            prepStmt.execute();
            connection.commit();
        } catch (SQLException | IdentityException e) {
            throw new WorkflowException("Error while deleting temporary user record from Identity database.", e);
        } finally {
            IdentityDatabaseUtil.closeStatement(prepStmt);
            IdentityDatabaseUtil.closeConnection(connection);
        }
    }

    public boolean checkEntityLocked(String entityName, String entityType) throws
            WorkflowException {

        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet;
        try {
            connection = IdentityDatabaseUtil.getDBConnection();;
            prepStmt = connection.prepareStatement(SQLConstants.GET_ENTITY_STATE_LIST);
            prepStmt.setString(1, entityName);
            prepStmt.setString(2, entityType);
            resultSet = prepStmt.executeQuery();
            if (resultSet.next()){
                return false;
            }
            connection.commit();
        } catch (SQLException | IdentityException e) {
            throw new WorkflowException("Error while retrieving user records from Identity database.", e);
        } finally {
            IdentityDatabaseUtil.closeStatement(prepStmt);
            IdentityDatabaseUtil.closeConnection(connection);
        }

        return true;
    }

}
