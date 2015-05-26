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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.application.authenticator.fido.dao;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.yubico.u2f.data.DeviceRegistration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authenticator.fido.exception.FIDOAuthenticatorServerException;
import org.wso2.carbon.identity.application.authenticator.fido.util.FIDOAuthenticatorConstants;
import org.wso2.carbon.identity.application.authenticator.fido.util.FIDOUtil;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

public class DeviceStoreDAO {

    private static Log log = LogFactory.getLog(DeviceStoreDAO.class);

    /**
     * Add Device Registration to store.
     *
     * @param username     The username of Device Registration.
     * @param registration The FIDO Registration.
     * @throws IdentityException when SQL statement can not be executed.
     */
    public void addDeviceRegistration(String username, DeviceRegistration registration,
                                      int tenantID, String userStoreDomain)
            throws FIDOAuthenticatorServerException {

        if (log.isDebugEnabled()) {
            log.debug("addDeviceRegistration inputs {username: " + username + ", registration :" +
                      registration.toJsonWithAttestationCert() + "}");
        }
        Connection connection = getDBConnection();
        PreparedStatement preparedStatement = null;

        try {
            preparedStatement = connection.prepareStatement(FIDOAuthenticatorConstants.SQLQueries.ADD_DEVICE_REGISTRATION_QUERY);
            preparedStatement.setInt(1, tenantID);
            preparedStatement.setString(2, username);
            preparedStatement.setString(3, registration.getKeyHandle());
            preparedStatement.setString(4, registration.toJson());
            preparedStatement.setString(5, userStoreDomain);
            preparedStatement.setInt(6, tenantID);
            preparedStatement.executeUpdate();
            if (!connection.getAutoCommit()) {
                connection.commit();
            }

        } catch (SQLException e) {
            throw new FIDOAuthenticatorServerException("Error when executing FIDO registration SQL : " +
                                                       FIDOAuthenticatorConstants.SQLQueries.ADD_DEVICE_REGISTRATION_QUERY, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, preparedStatement);
        }
    }

    /**
     * Retrieves Device Registration data from store.
     *
     * @param username The username of the Device Registration.
     * @return Collection of Device Registration.
     * @throws IdentityException when SQL statement can not be executed.
     */
    public Collection getDeviceRegistration(String username, int tenantID, String userStoreDomain)
            throws FIDOAuthenticatorServerException {

        if (log.isDebugEnabled()) {
            log.debug("getDeviceRegistration inputs {username:" + username + "}");
        }
        Connection connection = getDBConnection();
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Multimap<String, String> devices = ArrayListMultimap.create();

        try {
            preparedStatement = connection.prepareStatement(FIDOAuthenticatorConstants.SQLQueries.GET_DEVICE_REGISTRATION_QUERY);
            preparedStatement.setString(1, userStoreDomain);
            preparedStatement.setInt(2, tenantID);
            preparedStatement.setInt(3, tenantID);
            preparedStatement.setString(4, username);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                String keyHandle = resultSet.getString(FIDOAuthenticatorConstants.U2F_KEY_HANDLE);
                String deviceData = resultSet.getString(FIDOAuthenticatorConstants.U2F_DEVICE_DATA);
                devices.put(keyHandle, deviceData);

            }
            if (log.isDebugEnabled()) {
                log.debug("getDeviceRegistration result length {" + devices.size() + "}");
            }
        } catch (SQLException e) {
            throw new FIDOAuthenticatorServerException(
                    "Error executing get device registration SQL : " +
                    FIDOAuthenticatorConstants.SQLQueries.GET_DEVICE_REGISTRATION_QUERY, e
            );
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, resultSet, preparedStatement);
        }
        return devices.values();
    }

    public void removeRegistration(String username, int tenantID, String userStoreDomain)
            throws FIDOAuthenticatorServerException {

        if (log.isDebugEnabled()) {
            log.debug("removeRegistration inputs {username:" + username + "}");
        }
        Connection connection = getDBConnection();
        PreparedStatement preparedStatement = null;

        try {
            preparedStatement = connection.prepareStatement(FIDOAuthenticatorConstants.SQLQueries.REMOVE_REGISTRATION_QUERY);
            preparedStatement.setString(1, userStoreDomain);
            preparedStatement.setInt(2, tenantID);
            preparedStatement.setInt(3, tenantID);
            preparedStatement.setString(4, username);
            preparedStatement.executeUpdate();

            if (!connection.getAutoCommit()) {
                connection.commit();
            }
        } catch (SQLException e) {
            throw new FIDOAuthenticatorServerException(
                    "Error executing remove registrations SQL : " +
                    FIDOAuthenticatorConstants.SQLQueries.GET_DEVICE_REGISTRATION_QUERY, e
            );
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, preparedStatement);
        }
    }

    private Connection getDBConnection() throws FIDOAuthenticatorServerException {
        try {
            return IdentityDatabaseUtil.getDBConnection();
        } catch (IdentityException e) {
            throw new FIDOAuthenticatorServerException("Error while getting database connection ", e);
        }
    }
}
