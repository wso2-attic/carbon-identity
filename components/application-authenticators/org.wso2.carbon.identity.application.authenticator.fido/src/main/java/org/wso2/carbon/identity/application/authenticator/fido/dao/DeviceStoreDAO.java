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
import org.wso2.carbon.identity.core.util.IdentityCoreConstants;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.TimeZone;

/**
 * Performs DAO operations related to the FIDO Device Store.
 */
public class DeviceStoreDAO {

    private static Log log = LogFactory.getLog(DeviceStoreDAO.class);

    private DeviceStoreDAO(){

    }

    public static DeviceStoreDAO getInstance() {
        return LazyHolder.INSTANCE;
    }



    private static class LazyHolder {
        private static final DeviceStoreDAO INSTANCE = new DeviceStoreDAO();
    }


    /**
     * Add Device Registration to store.
     *
     * @param username     The username of Device Registration.
     * @param registration The FIDO Registration.
     * @param timestamp
     * @throws FIDOAuthenticatorServerException when SQL statement can not be executed.
     */
    public void addDeviceRegistration(String username, DeviceRegistration registration, String tenantDomain,
                                      String userStoreDomain, Timestamp timestamp)
            throws FIDOAuthenticatorServerException {

        if (log.isDebugEnabled()) {
            log.debug("addDeviceRegistration inputs {username: " + username + ", tenantDomain: " + tenantDomain +
                      ", userStoreDomain : " + userStoreDomain +", registration :" +
                      registration.toJsonWithAttestationCert() + "}");
        }
        Connection connection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement preparedStatement = null;

        try {
            preparedStatement = connection.prepareStatement(FIDOAuthenticatorConstants.SQLQueries.ADD_DEVICE_REGISTRATION_QUERY);
            preparedStatement.setInt(1, IdentityTenantUtil.getTenantId(tenantDomain));
            preparedStatement.setString(2, userStoreDomain);
            preparedStatement.setString(3, username);
            preparedStatement.setTimestamp(4, timestamp, Calendar.getInstance(TimeZone.getTimeZone(IdentityCoreConstants.UTC)));
            preparedStatement.setString(5, registration.getKeyHandle());
            preparedStatement.setString(6, registration.toJson());
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
     * @throws FIDOAuthenticatorServerException when SQL statement can not be executed.
     */
    public Collection getDeviceRegistration(String username,String tenantDomain, String userStoreDomain)
            throws FIDOAuthenticatorServerException {

        if (log.isDebugEnabled()) {
            log.debug("getDeviceRegistration inputs {username: " + username + ", tenantDomain: " + tenantDomain +
                      ", userStoreDomain : " + userStoreDomain +"}");
        }
        Connection connection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Multimap<String, String> devices = ArrayListMultimap.create();

        try {
            preparedStatement = connection.prepareStatement(FIDOAuthenticatorConstants.SQLQueries.GET_DEVICE_REGISTRATION_QUERY);
            preparedStatement.setInt(1, IdentityTenantUtil.getTenantId(tenantDomain));
            preparedStatement.setString(2, userStoreDomain);
            preparedStatement.setString(3, username);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                String keyHandle = resultSet.getString(FIDOAuthenticatorConstants.U2F_KEY_HANDLE);
                String deviceData = resultSet.getString(FIDOAuthenticatorConstants.U2F_DEVICE_DATA);
                devices.put(keyHandle, deviceData);

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

    /**
     * Retrieves Device Registration metadata data from store.
     *
     * @param username The username of the Device Registration.
     * @return Collection of Device Registration.
     * @throws FIDOAuthenticatorServerException when SQL statement can not be executed.
     */
    public ArrayList<String> getDeviceMetadata(String username, String tenantDomain, String userStoreDomain)
            throws FIDOAuthenticatorServerException {

        ResultSet resultSet = null;
        ArrayList<String> devicesMetadata = new ArrayList<String>();
        if (log.isDebugEnabled()) {
            log.debug("getDeviceRegistration inputs {username: " + username + ", tenantDomain: " + tenantDomain +
                      ", userStoreDomain : " + userStoreDomain +"}");
        }
        Connection connection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement preparedStatement = null;

        try {
            preparedStatement = connection.prepareStatement(FIDOAuthenticatorConstants.SQLQueries.GET_DEVICE_REGISTRATION_QUERY);
            preparedStatement.setInt(1, IdentityTenantUtil.getTenantId(tenantDomain));
            preparedStatement.setString(2, userStoreDomain);
            preparedStatement.setString(3, username);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                String deviceRemark = resultSet.getTimestamp(FIDOAuthenticatorConstants.U2F_DEVICE_METADATA).toString();
                devicesMetadata.add(deviceRemark);
            }
        } catch (SQLException e) {
            throw new FIDOAuthenticatorServerException(
                    "Error executing get device registration SQL : " +
                    FIDOAuthenticatorConstants.SQLQueries.GET_DEVICE_REGISTRATION_QUERY, e
            );
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, resultSet, preparedStatement);
        }

        return devicesMetadata;
    }

    /**
     * Remove all registered device from store.
     *
     * @param username
     * @param tenantDomain
     * @param userStoreDomain
     * @throws FIDOAuthenticatorServerException
     */
    public void removeAllRegistrations(String username, String tenantDomain, String userStoreDomain)
            throws FIDOAuthenticatorServerException {

        if (log.isDebugEnabled()) {
            log.debug("removeRegistration inputs {username:" + username + "}");
        }
        Connection connection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement preparedStatement = null;

        try {
            preparedStatement = connection.prepareStatement(FIDOAuthenticatorConstants.SQLQueries.REMOVE_ALL_REGISTRATION_QUERY);
            preparedStatement.setInt(1, IdentityTenantUtil.getTenantId(tenantDomain));
            preparedStatement.setString(2, userStoreDomain);
            preparedStatement.setString(3, username);
            preparedStatement.executeUpdate();

            if (!connection.getAutoCommit()) {
                connection.commit();
            }
        } catch (SQLException e) {
            throw new FIDOAuthenticatorServerException(
                    "Error executing remove all registrations SQL : " +
                    FIDOAuthenticatorConstants.SQLQueries.REMOVE_ALL_REGISTRATION_QUERY, e
            );
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, preparedStatement);
        }
    }

    /**
     * Remove all registered device from store.
     *
     * @param username
     * @param tenantDomain
     * @param userStoreDomain
     * @throws FIDOAuthenticatorServerException
     */
    public void removeRegistration(String username, String tenantDomain, String userStoreDomain, Timestamp timestamp )
            throws FIDOAuthenticatorServerException {

        if (log.isDebugEnabled()) {
            log.debug("removeRegistration inputs {username: " + username + ", tenantDomain: " + tenantDomain +
                      ", userStoreDomain : " + userStoreDomain + "}");
        }
        Connection connection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement preparedStatement = null;

        try {
            preparedStatement = connection.prepareStatement(FIDOAuthenticatorConstants.SQLQueries.REMOVE_REGISTRATION_QUERY);
            preparedStatement.setInt(1, IdentityTenantUtil.getTenantId(tenantDomain));
            preparedStatement.setString(2, userStoreDomain);
            preparedStatement.setString(3, username);
            preparedStatement.setTimestamp(4,timestamp);
            preparedStatement.executeUpdate();

            if (!connection.getAutoCommit()) {
                connection.commit();
            }
        } catch (SQLException e) {
            throw new FIDOAuthenticatorServerException(
                    "Error executing remove registrations SQL : " +
                    FIDOAuthenticatorConstants.SQLQueries.REMOVE_REGISTRATION_QUERY, e
            );
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, preparedStatement);
        }
    }

    /**
     * Update registration entry once domain name changed.
     *
     * @param tenantId
     * @param currentUserStoreName
     * @param newUserStoreName
     * @throws FIDOAuthenticatorServerException
     */
    public void updateDomainNameOfRegistration(int tenantId, String currentUserStoreName,
                                               String newUserStoreName)  throws FIDOAuthenticatorServerException{
        if (log.isDebugEnabled()) {
            log.debug("updateDomainNameOfRegistration inputs {tenantId: " + tenantId + ", currentUserStoreName: " +
                      currentUserStoreName +", newUserStoreName: " + newUserStoreName + "}");
        }
        Connection connection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement preparedStatement = null;

        try {
            preparedStatement = connection.prepareStatement(FIDOAuthenticatorConstants.SQLQueries.UPDATE_USER_DOMAIN_NAME);
            preparedStatement.setString(1, newUserStoreName.toUpperCase());
            preparedStatement.setString(2, currentUserStoreName.toUpperCase());
            preparedStatement.setInt(3, tenantId);
            preparedStatement.executeUpdate();
            if (!connection.getAutoCommit()) {
                connection.commit();
            }

        } catch (SQLException e) {
            throw new FIDOAuthenticatorServerException("Error when executing FIDO update domain name SQL : " +
                                                       FIDOAuthenticatorConstants.SQLQueries.UPDATE_USER_DOMAIN_NAME, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, preparedStatement);
        }

    }

    /**
     * Remove registration entry once user store domain deleted.
     *
     * @param tenantId
     * @param userStoreName
     * @throws FIDOAuthenticatorServerException
     */
    public void deleteRegistrationFromDomain(int tenantId, String userStoreName) throws FIDOAuthenticatorServerException{

        if (log.isDebugEnabled()) {
            log.debug("deleteRegistrationFromDomain inputs {tenantId: " + tenantId + ", userStoreName: " + userStoreName +"}");
        }
        Connection connection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement preparedStatement = null;

        try {
            preparedStatement = connection.prepareStatement(FIDOAuthenticatorConstants.SQLQueries.DELETE_DEVICE_REGISTRATION_FROM_DOMAIN);
            preparedStatement.setInt(1, tenantId);
            preparedStatement.setString(2, userStoreName.toUpperCase());
            preparedStatement.executeUpdate();

            if (!connection.getAutoCommit()) {
                connection.commit();
            }
        } catch (SQLException e) {
            throw new FIDOAuthenticatorServerException(
                    "Error executing remove registrations SQL on domain delete: " +
                    FIDOAuthenticatorConstants.SQLQueries.DELETE_DEVICE_REGISTRATION_FROM_DOMAIN, e
            );
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, preparedStatement);
        }

    }

    private ResultSet getDeviceData(String username, String tenantDomain, String userStoreDomain)
            throws FIDOAuthenticatorServerException {
        if (log.isDebugEnabled()) {
            log.debug("getDeviceRegistration inputs {username: " + username + ", tenantDomain: " + tenantDomain +
                      ", userStoreDomain : " + userStoreDomain +"}");
        }
        Connection connection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        ArrayList<String> devicesMetadata = new ArrayList<String>();

        try {
            preparedStatement = connection.prepareStatement(FIDOAuthenticatorConstants.SQLQueries.GET_DEVICE_REGISTRATION_QUERY);
            preparedStatement.setInt(1, IdentityTenantUtil.getTenantId(tenantDomain));
            preparedStatement.setString(2, userStoreDomain);
            preparedStatement.setString(3, username);
            resultSet = preparedStatement.executeQuery();
        } catch (SQLException e) {
            throw new FIDOAuthenticatorServerException(
                    "Error executing get device registration SQL : " +
                    FIDOAuthenticatorConstants.SQLQueries.GET_DEVICE_REGISTRATION_QUERY, e
            );
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, resultSet, preparedStatement);
        }
        return resultSet;
    }
}
