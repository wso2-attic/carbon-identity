/*
*  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.user.account.association.dao;

import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationManagementUtil;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.user.account.association.dto.UserAccountAssociationDTO;
import org.wso2.carbon.identity.user.account.association.exception.UserAccountAssociationException;
import org.wso2.carbon.identity.user.account.association.exception.UserAccountAssociationServerException;
import org.wso2.carbon.identity.user.account.association.internal.IdentityAccountAssociationServiceComponent;
import org.wso2.carbon.identity.user.account.association.util.UserAccountAssociationConstants;
import org.wso2.carbon.identity.user.account.association.util.UserAccountAssociationUtil;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserAccountAssociationDAO {

    private UserAccountAssociationDAO() {

    }

    public static UserAccountAssociationDAO getInstance() {
        return LazyHolder.INSTANCE;
    }

    public void createUserAssociation(String associationKey, String domainName, int tenantId,
                                      String userName) throws UserAccountAssociationException {

        Connection dbConnection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement preparedStatement = null;

        try {
            preparedStatement = dbConnection.prepareStatement(UserAccountAssociationConstants
                                                                      .SQLQueries.ADD_USER_ACCOUNT_ASSOCIATION);

            preparedStatement.setString(1, associationKey);
            preparedStatement.setInt(2, tenantId);
            preparedStatement.setString(3, domainName);
            preparedStatement.setString(4, userName);
            preparedStatement.executeUpdate();

            if (!dbConnection.getAutoCommit()) {
                dbConnection.commit();
            }
        } catch (SQLException e) {
            throw new UserAccountAssociationServerException(UserAccountAssociationConstants.ErrorMessages
                                                                    .CONN_CREATE_DB_ERROR.getDescription(), e);
        } finally {
            IdentityApplicationManagementUtil.closeStatement(preparedStatement);
            IdentityApplicationManagementUtil.closeConnection(dbConnection);
        }
    }

    public void deleteUserAssociation(String domainName, int tenantId,
                                      String userName) throws UserAccountAssociationException {

        Connection dbConnection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement preparedStatement = null;

        try {
            preparedStatement = dbConnection.prepareStatement(UserAccountAssociationConstants
                                                                      .SQLQueries.DELETE_CONNECTION);

            preparedStatement.setInt(1, tenantId);
            preparedStatement.setString(2, domainName);
            preparedStatement.setString(3, userName);
            preparedStatement.executeUpdate();

            if (!dbConnection.getAutoCommit()) {
                dbConnection.commit();
            }
        } catch (SQLException e) {
            throw new UserAccountAssociationServerException(UserAccountAssociationConstants.ErrorMessages
                                                                    .CONN_DELETE_DB_ERROR.getDescription(), e);
        } finally {
            IdentityApplicationManagementUtil.closeStatement(preparedStatement);
            IdentityApplicationManagementUtil.closeConnection(dbConnection);
        }
    }

    public List<UserAccountAssociationDTO> getAssociationsOfUser(String domainName, int tenantId,
                                                                 String userName)
            throws UserAccountAssociationException {

        Connection dbConnection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List<UserAccountAssociationDTO> accountAssociations = new ArrayList<>();
        RealmService realmService;
        String associationKey = getAssociationKeyOfUser(domainName, tenantId, userName);

        if (associationKey != null) {
            try {
                realmService = IdentityAccountAssociationServiceComponent.getRealmService();
                preparedStatement = dbConnection.prepareStatement(UserAccountAssociationConstants
                                                                          .SQLQueries.LIST_USER_ACCOUNT_ASSOCIATIONS);
                preparedStatement.setString(1, associationKey);
                resultSet = preparedStatement.executeQuery();

                while (resultSet.next()) {
                    int conUserTenantId = resultSet.getInt(1);
                    String conUserDomain = resultSet.getString(2);
                    String conUserName = resultSet.getString(3);

                    if (domainName.equals(conUserDomain) && (tenantId == conUserTenantId) && userName.equals
                            (conUserName)) {
                        continue;
                    }

                    UserAccountAssociationDTO associationDTO = new UserAccountAssociationDTO();
                    associationDTO.setUsername(conUserName);
                    associationDTO.setDomain(conUserDomain);
                    associationDTO.setTenantDomain(realmService.getTenantManager().getDomain(conUserTenantId));
                    accountAssociations.add(associationDTO);
                }
                dbConnection.commit();
            } catch (SQLException e) {
                throw new UserAccountAssociationServerException(UserAccountAssociationConstants.ErrorMessages
                                                                        .CONN_DELETE_DB_ERROR.getDescription(), e);
            } catch (UserStoreException e) {
                throw new UserAccountAssociationServerException(UserAccountAssociationConstants.ErrorMessages
                                                                        .ERROR_WHILE_GETTING_TENANT_NAME
                                                                        .getDescription(), e);
            } finally {
                IdentityApplicationManagementUtil.closeResultSet(resultSet);
                IdentityApplicationManagementUtil.closeStatement(preparedStatement);
                IdentityApplicationManagementUtil.closeConnection(dbConnection);
            }
        }

        return accountAssociations;
    }

    public String getAssociationKeyOfUser(String domainName, int tenantId,
                                          String userName) throws UserAccountAssociationException {

        Connection dbConnection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        String associationKey = null;

        try {
            preparedStatement = dbConnection.prepareStatement(UserAccountAssociationConstants
                                                                      .SQLQueries.GET_ASSOCIATION_KEY_OF_USER);

            preparedStatement.setInt(1, tenantId);
            preparedStatement.setString(2, domainName);
            preparedStatement.setString(3, userName);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                associationKey = resultSet.getString(1);
            }
            dbConnection.commit();
        } catch (SQLException e) {
            throw new UserAccountAssociationServerException(UserAccountAssociationConstants.ErrorMessages
                                                                    .ERROR_WHILE_RETRIEVING_ASSOC_KEY.getDescription
                            (), e);
        } finally {
            IdentityApplicationManagementUtil.closeResultSet(resultSet);
            IdentityApplicationManagementUtil.closeStatement(preparedStatement);
            IdentityApplicationManagementUtil.closeConnection(dbConnection);
        }
        return associationKey;
    }

    public void updateUserAssociationKey(String oldAssociationKey, String newAssociationKey) throws
                                                                                             UserAccountAssociationException {

        Connection dbConnection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement preparedStatement = null;

        try {
            preparedStatement = dbConnection.prepareStatement(UserAccountAssociationConstants
                                                                      .SQLQueries.UPDATE_ASSOCIATION_KEY);

            preparedStatement.setString(1, newAssociationKey);
            preparedStatement.setString(2, oldAssociationKey);
            preparedStatement.executeUpdate();

            if (!dbConnection.getAutoCommit()) {
                dbConnection.commit();
            }
        } catch (SQLException e) {
            throw new UserAccountAssociationServerException(UserAccountAssociationConstants.ErrorMessages
                                                                    .CONN_UPDATE_DB_ERROR.getDescription(), e);
        } finally {
            IdentityApplicationManagementUtil.closeStatement(preparedStatement);
            IdentityApplicationManagementUtil.closeConnection(dbConnection);
        }
    }

    public boolean isValidUserAssociation(String domainName, int tenantId,
                                          String userName) throws UserAccountAssociationException {

        Connection dbConnection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        boolean valid = false;

        try {
            preparedStatement = dbConnection.prepareStatement(UserAccountAssociationConstants
                                                                      .SQLQueries.IS_VALID_ASSOCIATION);

            preparedStatement.setInt(1, tenantId);
            preparedStatement.setString(2, domainName);
            preparedStatement.setString(3, userName);
            preparedStatement.setInt(4, CarbonContext.getThreadLocalCarbonContext().getTenantId());
            preparedStatement.setString(5, UserAccountAssociationUtil.getDomainName(CarbonContext
                                                                                            .getThreadLocalCarbonContext()
                                                                                            .getUsername()));
            preparedStatement.setString(6, UserAccountAssociationUtil.getUsernameWithoutDomain(CarbonContext
                                                                                                       .getThreadLocalCarbonContext().getUsername()));
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                valid = resultSet.getInt(1) > 0;
            }
            dbConnection.commit();
        } catch (SQLException e) {
            throw new UserAccountAssociationServerException(UserAccountAssociationConstants.ErrorMessages
                                                                    .CHECK_ASSOCIATION_DB_ERROR.getDescription(), e);
        } finally {
            IdentityApplicationManagementUtil.closeResultSet(resultSet);
            IdentityApplicationManagementUtil.closeStatement(preparedStatement);
            IdentityApplicationManagementUtil.closeConnection(dbConnection);
        }

        return valid;
    }

    public void deleteUserAssociationsFromTenantId(int tenantId) throws UserAccountAssociationException {

        Connection dbConnection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement preparedStatement = null;

        try {
            preparedStatement = dbConnection.prepareStatement(UserAccountAssociationConstants
                                                                      .SQLQueries.DELETE_CONNECTION_FROM_TENANT_ID);

            preparedStatement.setInt(1, tenantId);
            preparedStatement.executeUpdate();

            if (!dbConnection.getAutoCommit()) {
                dbConnection.commit();
            }
        } catch (SQLException e) {
            throw new UserAccountAssociationServerException(UserAccountAssociationConstants.ErrorMessages
                                                                    .ASSOCIATIONS_DELETE_DB_ERROR.getDescription(), e);
        } finally {
            IdentityApplicationManagementUtil.closeStatement(preparedStatement);
            IdentityApplicationManagementUtil.closeConnection(dbConnection);
        }
    }

    public void updateDomainNameOfAssociations(int tenantId, String currentDomainName, String newDomainName) throws
                                                                                                             UserAccountAssociationException {

        Connection dbConnection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement preparedStatement = null;

        try {
            preparedStatement = dbConnection.prepareStatement(UserAccountAssociationConstants
                                                                      .SQLQueries.UPDATE_USER_DOMAIN_NAME);

            preparedStatement.setString(1, newDomainName);
            preparedStatement.setString(2, currentDomainName);
            preparedStatement.setInt(3, tenantId);
            preparedStatement.executeUpdate();

            if (!dbConnection.getAutoCommit()) {
                dbConnection.commit();
            }
        } catch (SQLException e) {
            throw new UserAccountAssociationServerException(String.format(UserAccountAssociationConstants.ErrorMessages
                                                                                  .ERROR_UPDATE_DOMAIN_NAME.getDescription(),
                                                                          currentDomainName, tenantId), e);
        } finally {
            IdentityApplicationManagementUtil.closeStatement(preparedStatement);
            IdentityApplicationManagementUtil.closeConnection(dbConnection);
        }
    }

    public void deleteAssociationsFromDomain(int tenantId, String domainName) throws
                                                                              UserAccountAssociationException {

        Connection dbConnection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement preparedStatement = null;

        try {
            preparedStatement = dbConnection.prepareStatement(UserAccountAssociationConstants
                                                                      .SQLQueries.DELETE_USER_ASSOCIATION_FROM_DOMAIN);
            preparedStatement.setInt(1, tenantId);
            preparedStatement.setString(2, domainName);
            preparedStatement.executeUpdate();

            if (!dbConnection.getAutoCommit()) {
                dbConnection.commit();
            }
        } catch (SQLException e) {
            throw new UserAccountAssociationServerException(String.format(UserAccountAssociationConstants.ErrorMessages
                                                                                  .ERROR_DELETE_ASSOC_FROM_DOMAIN_NAME
                                                                                  .getDescription(),  domainName,
                                                                          tenantId), e);
        } finally {
            IdentityApplicationManagementUtil.closeStatement(preparedStatement);
            IdentityApplicationManagementUtil.closeConnection(dbConnection);
        }
    }

    private static class LazyHolder {
        private static final UserAccountAssociationDAO INSTANCE = new UserAccountAssociationDAO();
    }

}