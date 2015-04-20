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
import org.wso2.carbon.identity.user.account.connector.util.UserAccountConnectorUtil;
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

    public void createConnection(String associationKey, String domainName, int tenantId,
                                 String userName) throws Exception {

        Connection dbConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            dbConnection = JDBCPersistenceManager.getInstance().getDBConnection();
            preparedStatement = dbConnection.prepareStatement(UserAccountConnectorConstants
                                                                      .SQLQueries.ADD_USER_ACCOUNT_ASSOCIATION);

            preparedStatement.setString(1, associationKey);
            preparedStatement.setInt(2, tenantId);
            preparedStatement.setString(3, userName);
            preparedStatement.setString(4, domainName);
            preparedStatement.setInt(5, tenantId);
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

    public void deleteAccountConnection(String domainName, int tenantId, String userName) throws Exception {

        Connection dbConnection = null;
        PreparedStatement preparedStatement = null;

        try {
            dbConnection = JDBCPersistenceManager.getInstance().getDBConnection();
            preparedStatement = dbConnection.prepareStatement(UserAccountConnectorConstants
                                                                      .SQLQueries.DELETE_CONNECTION);

            preparedStatement.setString(1, domainName);
            preparedStatement.setInt(2, tenantId);
            preparedStatement.setInt(3, tenantId);
            preparedStatement.setString(4, userName);
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

    public List<String> getConnectionsOfUser(String domainName, int tenantId, String userName) throws Exception {

        Connection dbConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List<String> accountConnections = new ArrayList<String>();
        RealmService realmService = null;
        String associationKey = getAssociationKeyOfUser(domainName, tenantId, userName);

        if (associationKey != null) {
            try {
                realmService = IdentityAccountConnectorServiceComponent.getRealmService();

                dbConnection = JDBCPersistenceManager.getInstance().getDBConnection();
                preparedStatement = dbConnection.prepareStatement(UserAccountConnectorConstants
                                                                          .SQLQueries.LIST_USER_ACCOUNT_ASSOCIATIONS);
                preparedStatement.setString(1, associationKey);
                resultSet = preparedStatement.executeQuery();

                while (resultSet.next()) {
                    String conUserDomainName = resultSet.getString(1);
                    int conUserTenantId = resultSet.getInt(2);
                    String conUserName = resultSet.getString(3);
                    if (domainName.equals(conUserDomainName) && (tenantId == conUserTenantId) && userName.equals
                            (conUserName)) {
                        continue;
                    }
                    if (!UserAccountConnectorConstants.PRIMARY_USER_DOMAIN.equals(conUserDomainName)) {
                        conUserName = conUserDomainName + CarbonConstants.DOMAIN_SEPARATOR + conUserName;
                    }
                    if (realmService != null) {
                        conUserName = conUserName + CarbonConstants.ROLE_TENANT_DOMAIN_SEPARATOR + realmService
                                .getTenantManager().getDomain(conUserTenantId);
                    }
                    accountConnections.add(conUserName);
                }
            } catch (Exception e) {
                throw e;
            } finally {
                IdentityApplicationManagementUtil.closeResultSet(resultSet);
                IdentityApplicationManagementUtil.closeStatement(preparedStatement);
                IdentityApplicationManagementUtil.closeConnection(dbConnection);
            }
        }

        return accountConnections;
    }

    public String getAssociationKeyOfUser(String domainName, int tenantId, String userName) throws Exception {

        Connection dbConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        String associationKey = null;

        try {
            dbConnection = JDBCPersistenceManager.getInstance().getDBConnection();
            preparedStatement = dbConnection.prepareStatement(UserAccountConnectorConstants
                                                                      .SQLQueries.GET_ASSOCIATION_KEY_OF_USER);

            preparedStatement.setString(1, domainName);
            preparedStatement.setInt(2, tenantId);
            preparedStatement.setInt(3, tenantId);
            preparedStatement.setString(4, userName);
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

    public boolean isValidAssociation(String domainName, int tenantId, String userName) throws Exception {

        Connection dbConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        boolean valid = false;

        try {
            dbConnection = JDBCPersistenceManager.getInstance().getDBConnection();
            preparedStatement = dbConnection.prepareStatement(UserAccountConnectorConstants
                                                                      .SQLQueries.IS_VALID_ASSOCIATION);

            preparedStatement.setString(1, domainName);
            preparedStatement.setInt(2, tenantId);
            preparedStatement.setInt(3, tenantId);
            preparedStatement.setString(4, userName);
            preparedStatement.setString(5, UserAccountConnectorUtil.getDomainName(CarbonContext
                                                                                          .getThreadLocalCarbonContext()
                                                                                          .getUsername()));
            preparedStatement.setInt(6, CarbonContext.getThreadLocalCarbonContext().getTenantId());
            preparedStatement.setInt(7, CarbonContext.getThreadLocalCarbonContext().getTenantId());
            preparedStatement.setString(8, UserAccountConnectorUtil.getUsernameWithoutDomain(CarbonContext
                                                                                                     .getThreadLocalCarbonContext().getUsername()));
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