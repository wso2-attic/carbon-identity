/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.mgt.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.persistence.JDBCPersistenceManager;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.mgt.IdentityMgtException;
import org.wso2.carbon.identity.mgt.bean.TenantConfiguration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ConfigurationDAOImpl implements ConfigurationDAO {

    Log log = LogFactory.getLog(ConfigurationDAOImpl.class);

    @Override
    public TenantConfiguration getConfiguration(int tenantId) throws IdentityMgtException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet result = null;
        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            prepStmt = connection.prepareStatement(IdentityMgtDBQueries.LOAD_CONFIG_DATA);
            prepStmt.setInt(1, tenantId);
            result = prepStmt.executeQuery();

            Map<String, String> configurationDetails = new HashMap<>();
            if (result != null) {
                while (result.next()) {
                    String property_name = result.getString(1);
                    String property_value = result.getString(2);
                    configurationDetails.put(property_name, property_value);
                }
            }
            TenantConfiguration tenantConfiguration = new TenantConfiguration(tenantId, configurationDetails);
            return tenantConfiguration;

        } catch (SQLException | IdentityException e) {
            log.error("Error while persisting tenant configuration data in database", e);
            throw new IdentityMgtException("Error while persisting tenant configuration data in database");
        } finally {
            IdentityDatabaseUtil.closeResultSet(result);
            IdentityDatabaseUtil.closeStatement(prepStmt);
            IdentityDatabaseUtil.closeConnection(connection);
        }
    }

    @Override
    public void addConfiguration(TenantConfiguration tenantConfiguration) throws IdentityMgtException {

        Connection connection = null;
        PreparedStatement prepStmt = null;

        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            prepStmt = connection.prepareStatement(IdentityMgtDBQueries.STORE_CONFIG_DATA);

            boolean idExist = isTenantIdExist(tenantConfiguration.getTenantId());

            Map<String, String> configurationDetails = tenantConfiguration.getConfigurationDetails();

            if (!idExist) {

                for (Map.Entry<String, String> entry : configurationDetails.entrySet()) {

                    prepStmt.setInt(1, tenantConfiguration.getTenantId());
                    prepStmt.setString(2, entry.getKey());
                    prepStmt.setString(3, entry.getValue());
                    prepStmt.execute();
                }
                connection.commit();
            }
        } catch (SQLException | IdentityException e) {
            log.error("Error while persisting tenant configuration data in database", e);
            throw new IdentityMgtException("Error while persisting tenant configuration data in database");
        } finally {
            IdentityDatabaseUtil.closeStatement(prepStmt);
            IdentityDatabaseUtil.closeConnection(connection);
        }
    }


    public void updateConfiguration(TenantConfiguration tenantConfiguration) throws IdentityMgtException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            prepStmt = connection.prepareStatement(IdentityMgtDBQueries.UPDATE_CONFIG_DATA);

            boolean idExist = isTenantIdExist(tenantConfiguration.getTenantId());

            Map<String, String> configurationDetails = tenantConfiguration.getConfigurationDetails();

            if (idExist) {

                for (Map.Entry<String, String> entry : configurationDetails.entrySet()) {
                    prepStmt.setString(1, entry.getValue());
                    prepStmt.setInt(2, tenantConfiguration.getTenantId());
                    prepStmt.setString(3, entry.getKey());
                    prepStmt.execute();
                }
                connection.commit();
            }

        } catch (SQLException | IdentityException e) {
            log.error("Error while persisting tenant configuration data in database", e);
            throw new IdentityMgtException("Error while persisting tenant configuration data in database");
        } finally {
            IdentityDatabaseUtil.closeStatement(prepStmt);
            IdentityDatabaseUtil.closeConnection(connection);
        }

    }

    public boolean isTenantIdExist(int tenantId) throws IdentityMgtException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet = null;

        boolean isExistingTenantId = false;

        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            prepStmt = connection.prepareStatement(IdentityMgtDBQueries.CHECK_TENANT_ID_EXISTS);
            prepStmt.setInt(1, tenantId);

            resultSet = prepStmt.executeQuery();
            if (resultSet.next()) {
                isExistingTenantId = true;
            }

        } catch (SQLException | IdentityException e) {
            log.error("Error while persisting tenant configuration data in database", e);
            throw new IdentityMgtException("Error while persisting tenant configuration data in database");
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, resultSet, prepStmt);
        }
        return isExistingTenantId;
    }

}
