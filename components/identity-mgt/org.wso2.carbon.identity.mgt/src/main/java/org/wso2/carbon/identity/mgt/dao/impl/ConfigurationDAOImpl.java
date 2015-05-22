/*
 *  Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.carbon.identity.mgt.dao.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.persistence.JDBCPersistenceManager;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.mgt.beans.TenantConfigBean;
import org.wso2.carbon.identity.mgt.dao.ConfigurationDAO;
import org.wso2.carbon.identity.mgt.dao.IdentityMgtDBQueries;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ConfigurationDAOImpl implements ConfigurationDAO {

    Log log = LogFactory.getLog(ConfigurationDAOImpl.class);

    @Override
    public TenantConfigBean getConfigurations(int tenantId) {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet results = null;
        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            prepStmt = connection.prepareStatement(IdentityMgtDBQueries.LOAD_CONFIG_DATA);
            prepStmt.setInt(1, tenantId);
            results = prepStmt.executeQuery();
            results.next();

            byte[] buf = results.getBytes(1);
            ObjectInputStream objectIn = null;
            if (buf != null)
                objectIn = new ObjectInputStream(new ByteArrayInputStream(buf));

            Object deSerializedObject = objectIn.readObject();

            return (TenantConfigBean) deSerializedObject;

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IdentityException e) {
            log.error("Error while persisting tenant configuration data in database", e);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IdentityDatabaseUtil.closeResultSet(results);
            IdentityDatabaseUtil.closeStatement(prepStmt);
            IdentityDatabaseUtil.closeConnection(connection);
        }
        return null;
    }

    @Override
    public void addConfigurations(TenantConfigBean tenantConfigBean) {

        Connection connection = null;
        PreparedStatement prepStmt = null;

        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            prepStmt = connection.prepareStatement(IdentityMgtDBQueries.STORE_CONFIG_DATA);

            boolean idExist = isTenantIdExist(tenantConfigBean.getTenantId());

            if (!idExist) {

                prepStmt.setInt(1, tenantConfigBean.getTenantId());
                prepStmt.setObject(2, tenantConfigBean);

                prepStmt.execute();
                connection.commit();
            }
        } catch (SQLException e) {
            log.error("Error while persisting tenant configuration data in database", e);
        } catch (IdentityException e) {
            log.error("Error while persisting tenant configuration data in database", e);
        } finally {
            IdentityDatabaseUtil.closeStatement(prepStmt);
            IdentityDatabaseUtil.closeConnection(connection);
        }
    }


    public void updateConfigurations(TenantConfigBean tenantConfigBean) {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            prepStmt = connection.prepareStatement(IdentityMgtDBQueries.UPDATE_CONFIG_DATA);

            boolean idExist = isTenantIdExist(tenantConfigBean.getTenantId());

            if (idExist) {

                prepStmt.setObject(1, tenantConfigBean);
                prepStmt.setInt(2, tenantConfigBean.getTenantId());

                prepStmt.execute();
                connection.commit();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        catch (IdentityException e) {
            log.error("Error while persisting tenant configuration data in database", e);
        }
        finally {
            IdentityDatabaseUtil.closeStatement(prepStmt);
            IdentityDatabaseUtil.closeConnection(connection);
        }

    }

    public boolean isTenantIdExist(int tenantId) {
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

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IdentityException e) {
            log.error("Error while persisting tenant configuration data in database", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, resultSet, prepStmt);
        }
        return isExistingTenantId;
    }

}
