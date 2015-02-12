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
import org.wso2.carbon.identity.application.authenticator.fido.util.Util;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

/**
 * Performs DAO operations related to the FIDO Device Store.
 */
public class DeviceStoreDAO {

	private static Log log = LogFactory.getLog(DeviceStoreDAO.class);
	private static final String ADD_DEVICE_REGISTRATION_QUERY = "INSERT INTO FIDO_DEVICE_STORE values(?, ?, ?)";
	private static final String GET_DEVICE_REGISTRATION_QUERY = "SELECT * FROM FIDO_DEVICE_STORE WHERE USER_NAME = ?";
	//public static boolean isTraceEnabled = log.isTraceEnabled();

	/*static {
		LogManager.getRootLogger().setLevel(Level.TRACE);
	}*/

	/**
	 * Add Device Registration to store.
	 *
	 * @param username     The username of Device Registration.
	 * @param registration The FIDO Registration.
	 * @throws IdentityException when SQL statement can not be executed.
	 */
	public void addDeviceRegistration(String username, DeviceRegistration registration) throws IdentityException {
		Util.logTrace("Executing {addDeviceRegistration} method", log);
		if (log.isDebugEnabled()) {
			log.debug("addDeviceRegistration inputs {username: " + username + ", registration :" +
			          registration.toJsonWithAttestationCert() + "}");
		}
		Connection connection = null;
		//String sql = "";
		PreparedStatement preparedStatement = null;

		try {
			connection = IdentityDatabaseUtil.getDBConnection();
			preparedStatement = connection.prepareStatement(ADD_DEVICE_REGISTRATION_QUERY);
			preparedStatement.setString(1, username);
			preparedStatement.setString(2, registration.getKeyHandle());
			preparedStatement.setString(3, registration.toJsonWithAttestationCert());
			preparedStatement.executeUpdate();
			connection.commit();

		} catch (SQLException e) {
			try {
				connection.rollback();
			} catch (SQLException e1) {
				log.error("Error rolling back the transaction to FIDO registration", e1);
			}
			throw new IdentityException("Error when executing FIDO registration SQL : " + ADD_DEVICE_REGISTRATION_QUERY,
			                            e);
		} finally {
			IdentityDatabaseUtil.closeAllConnections(connection, null, preparedStatement);
		}
		Util.logTrace("Completed {addDeviceRegistration} method", log);
	}

	/**
	 * Retrieves Device Registration data from store.
	 *
	 * @param username The username of the Device Registration.
	 * @return Collection of Device Registration.
	 * @throws IdentityException when SQL statement can not be executed.
	 */
	public Collection getDeviceRegistration(String username) throws IdentityException {
		Util.logTrace("Executing {getDeviceRegistration} method", log);
		if (log.isDebugEnabled()) {
			log.debug("getDeviceRegistration inputs {username:" + username + "}");
		}
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		//String sql = "";
		Multimap<String, String> devices = ArrayListMultimap.create();
		try {
			connection = IdentityDatabaseUtil.getDBConnection();
			preparedStatement = connection.prepareStatement(GET_DEVICE_REGISTRATION_QUERY);
			preparedStatement.setString(1, username);
			resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				String keyHandle = resultSet.getString("KEY_HANDLE");
				String deviceData = resultSet.getString("DEVICE_DATA");
				devices.put(keyHandle, deviceData);

			}
			if (log.isDebugEnabled()) {
				log.debug("getDeviceRegistration result length {" + devices.size() + "}");
			}
		} catch (SQLException e) {
			throw new IdentityException(
					"Error executing get device registration SQL : " + GET_DEVICE_REGISTRATION_QUERY, e);
		} finally {
			IdentityDatabaseUtil.closeAllConnections(connection, resultSet, preparedStatement);
		}
		Util.logTrace("Completed {getDeviceRegistration} method, returns devices of size :" + devices.size(), log);
		return devices.values();
	}
}
