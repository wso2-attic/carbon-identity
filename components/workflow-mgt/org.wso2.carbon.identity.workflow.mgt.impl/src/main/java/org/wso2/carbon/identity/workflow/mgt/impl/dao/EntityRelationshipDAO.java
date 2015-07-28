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
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class EntityRelationshipDAO {

    private static Log log = LogFactory.getLog(EntityRelationshipDAO.class);

    /**
     * Check if an entity has pending operations with at least one of the entities in entityList
     *
     * @param entityName
     * @param entityType
     * @param entityList
     * @param entityListType
     * @return
     * @throws WorkflowException
     */
    public boolean isEntityRelatedToOneInList(String entityName, String entityType, String[] entityList, String
            entityListType) throws WorkflowException {

        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet;
        StringBuffer queryInComponent = new StringBuffer("(?");
        for (int i = 1; i < entityList.length; i++) {
            queryInComponent.append(",?");
        }
        queryInComponent.append(")");
        try {
            connection = IdentityDatabaseUtil.getDBConnection();
            String query = SQLConstants.GET_ENTITY_RELATED_TO_AT_LEAST_ONE_FROM_LIST.replace("(?)", queryInComponent);
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, entityName);
            prepStmt.setString(3 + entityList.length + 1, entityName);
            prepStmt.setString(2, entityType);
            prepStmt.setString(4 + entityList.length + 1, entityType);
            prepStmt.setString(3, entityListType);
            prepStmt.setString(5 + entityList.length + 1, entityListType);
            for (int i = 0; i < entityList.length; i++) {
                prepStmt.setString(3 + i + 1, entityList[i]);
                prepStmt.setString(6 + entityList.length + i + 1, entityList[i]);
            }
            resultSet = prepStmt.executeQuery();
            if (resultSet.next()) {
                return false;
            }
            connection.commit();
        } catch (SQLException | IdentityException e) {
            throw new WorkflowException("Error while retrieving user role details from Identity database.", e);
        } finally {
            IdentityDatabaseUtil.closeStatement(prepStmt);
            IdentityDatabaseUtil.closeConnection(connection);
        }

        return true;
    }

    /**
     * Add entries when new workflow started with relate two entities
     *
     * @param entity1
     * @param entity1_type
     * @param entity2_List
     * @param entity2_type
     * @param operation
     * @throws WorkflowException
     */
    public void addNewRelationships(String entity1, String entity1_type, String[] entity2_List, String entity2_type,
                                    String operation) throws WorkflowException {

        Connection connection = null;
        PreparedStatement prepStmt = null;
        try {
            connection = IdentityDatabaseUtil.getDBConnection();
            String query = SQLConstants.INSERT_NEW_ENTITY_RELATIONSHIP;
            prepStmt = connection.prepareStatement(query);
            for (int i = 0; i < entity2_List.length; i++) {
                prepStmt.setString(1, entity1);
                prepStmt.setString(2, entity1_type);
                prepStmt.setString(3, entity2_List[i]);
                prepStmt.setString(4, entity2_type);
                prepStmt.setString(5, "Operation");
                prepStmt.setString(6, operation);
                prepStmt.execute();
            }
            connection.commit();
        } catch (SQLException | IdentityException e) {
            throw new WorkflowException("Error while inserting relationship details from Identity database.", e);
        } finally {
            IdentityDatabaseUtil.closeStatement(prepStmt);
            IdentityDatabaseUtil.closeConnection(connection);
        }

    }

    /**
     * Delete entries when workflow completed which relate two entities
     *
     * @param entity1
     * @param entity1_type
     * @param entity2_List
     * @param entity2_type
     * @param operation
     * @throws WorkflowException
     */
    public void deleteEntityRelationshipStates(String entity1, String entity1_type, String[] entity2_List, String
            entity2_type, String operation) throws WorkflowException {

        Connection connection = null;
        PreparedStatement prepStmt = null;
        try {
            connection = IdentityDatabaseUtil.getDBConnection();
            String query = SQLConstants.DELETE_ENTITY_RELATIONSHIP;
            prepStmt = connection.prepareStatement(query);
            for (int i = 0; i < entity2_List.length; i++) {
                prepStmt.setString(1, entity1);
                prepStmt.setString(2, entity1_type);
                prepStmt.setString(3, entity2_List[i]);
                prepStmt.setString(4, entity2_type);
                prepStmt.setString(5, "Operation");
                prepStmt.setString(6, operation);
                prepStmt.execute();
            }
            connection.commit();
        } catch (SQLException | IdentityException e) {
            throw new WorkflowException("Error while inserting relationship details from Identity database.", e);
        } finally {
            IdentityDatabaseUtil.closeStatement(prepStmt);
            IdentityDatabaseUtil.closeConnection(connection);
        }

    }

}
