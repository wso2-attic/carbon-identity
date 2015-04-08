/*
*  Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.user.account.connector.dao;

import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.application.common.persistence.JDBCPersistenceManager;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationManagementUtil;
import org.wso2.carbon.identity.user.account.connector.internal.IdentityAccountConnectorServiceComponent;
import org.wso2.carbon.identity.user.account.connector.util.UserAccountConnectorConstants;
import org.wso2.carbon.user.core.service.RealmService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ConnectorDAO {

    private static ConnectorDAO instance;

    private ConnectorDAO() {

    }

    public static ConnectorDAO getInstance() {
        if (instance == null) {
            synchronized (ConnectorDAO.class) {
                if (instance == null) {
                    instance = new ConnectorDAO();
                }
            }
        }

        return instance;
    }

    public void createConnection(String associationKey, String userName, int tenantId) throws Exception {

        Connection dbConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            dbConnection = JDBCPersistenceManager.getInstance().getDBConnection();
            preparedStatement = dbConnection.prepareStatement(UserAccountConnectorConstants
                                                                      .SQLQueries.ADD_CONNECTION);

            preparedStatement.setString(1, associationKey);
            preparedStatement.setString(2, userName);
            preparedStatement.setInt(3, tenantId);
            preparedStatement.executeUpdate();

            if (!dbConnection.getAutoCommit()) {
                dbConnection.commit();
            }
        } catch (Exception e) {
            throw e;
        } finally {
            IdentityApplicationManagementUtil.closeStatement(preparedStatement);
            IdentityApplicationManagementUtil.closeConnection(dbConnection);
        }
    }

    public void deleteAccountConnection(String userName, int tenantId) throws Exception {

        Connection dbConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            dbConnection = JDBCPersistenceManager.getInstance().getDBConnection();
            preparedStatement = dbConnection.prepareStatement(UserAccountConnectorConstants
                                                                      .SQLQueries.DELETE_CONNECTION);

            preparedStatement.setString(1, userName);
            preparedStatement.setInt(2, tenantId);
            preparedStatement.executeUpdate();

            if (!dbConnection.getAutoCommit()) {
                dbConnection.commit();
            }
        } catch (Exception e) {
            throw e;
        } finally {
            IdentityApplicationManagementUtil.closeStatement(preparedStatement);
            IdentityApplicationManagementUtil.closeConnection(dbConnection);
        }
    }

    public List<String> getConnectionsOfUser(String userName, int tenantId) throws Exception {

        Connection dbConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List<String> accountConnections = new ArrayList<String>();
        RealmService realmService = null;
        try {
            realmService = IdentityAccountConnectorServiceComponent.getRealmService();

            dbConnection = JDBCPersistenceManager.getInstance().getDBConnection();
            preparedStatement = dbConnection.prepareStatement(UserAccountConnectorConstants
                                                                      .SQLQueries.LIST_USER_CONNECTIONS);

            preparedStatement.setString(1, userName);
            preparedStatement.setInt(2, tenantId);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                String connectedUser = resultSet.getString(1);
                int connectedUserTenantId = resultSet.getInt(2);
                if (userName.equals(connectedUser) && (tenantId == connectedUserTenantId)) {
                    continue;
                }
                if (realmService != null) {
                    connectedUser = connectedUser + CarbonConstants.ROLE_TENANT_DOMAIN_SEPARATOR + realmService
                            .getTenantManager().getDomain(connectedUserTenantId);
                }
                accountConnections.add(connectedUser);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            IdentityApplicationManagementUtil.closeResultSet(resultSet);
            IdentityApplicationManagementUtil.closeStatement(preparedStatement);
            IdentityApplicationManagementUtil.closeConnection(dbConnection);
        }

        return accountConnections;
    }

    public String getAssociationKeyOfUser(String userName, int tenantId) throws Exception {

        Connection dbConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        String associationKey = null;

        try {
            dbConnection = JDBCPersistenceManager.getInstance().getDBConnection();
            preparedStatement = dbConnection.prepareStatement(UserAccountConnectorConstants
                                                                      .SQLQueries.GET_ASSOCIATE_KEY_OF_USER);

            preparedStatement.setString(1, userName);
            preparedStatement.setInt(2, tenantId);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                associationKey = resultSet.getString(1);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            IdentityApplicationManagementUtil.closeResultSet(resultSet);
            IdentityApplicationManagementUtil.closeStatement(preparedStatement);
            IdentityApplicationManagementUtil.closeConnection(dbConnection);
        }
        return associationKey;
    }

    public void updateAssociationKey(String oldAssociationKey, String newAssociationKey) throws Exception {

        Connection dbConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            dbConnection = JDBCPersistenceManager.getInstance().getDBConnection();
            preparedStatement = dbConnection.prepareStatement(UserAccountConnectorConstants
                                                                      .SQLQueries.UPDATE_ASSOCIATION_KEY);

            preparedStatement.setString(1, newAssociationKey);
            preparedStatement.setString(2, oldAssociationKey);
            preparedStatement.executeUpdate();

            if (!dbConnection.getAutoCommit()) {
                dbConnection.commit();
            }
        } catch (Exception e) {
            throw e;
        } finally {
            IdentityApplicationManagementUtil.closeStatement(preparedStatement);
            IdentityApplicationManagementUtil.closeConnection(dbConnection);
        }
    }

    public boolean isValidAssociation(String userName, int tenantId) throws Exception {

        Connection dbConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        boolean valid = false;

        try {
            dbConnection = JDBCPersistenceManager.getInstance().getDBConnection();
            preparedStatement = dbConnection.prepareStatement(UserAccountConnectorConstants
                                                                      .SQLQueries.IS_VALID_ASSOCIATION);

            preparedStatement.setString(1, userName);
            preparedStatement.setInt(2, tenantId);
            preparedStatement.setString(3, CarbonContext.getThreadLocalCarbonContext().getUsername());
            preparedStatement.setInt(4, CarbonContext.getThreadLocalCarbonContext().getTenantId());
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                valid = resultSet.getInt(1) > 0;
            }
        } catch (Exception e) {
            throw e;
        } finally {
            IdentityApplicationManagementUtil.closeResultSet(resultSet);
            IdentityApplicationManagementUtil.closeStatement(preparedStatement);
            IdentityApplicationManagementUtil.closeConnection(dbConnection);
        }

        return valid;
    }

}