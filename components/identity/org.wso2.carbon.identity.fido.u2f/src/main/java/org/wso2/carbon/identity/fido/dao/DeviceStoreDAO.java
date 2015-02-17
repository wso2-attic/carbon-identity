package org.wso2.carbon.identity.fido.dao;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.yubico.u2f.data.DeviceRegistration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

	private static final Log LOG = LogFactory.getLog(DeviceStoreDAO.class);
	private static final String ADD_DEVICE_REGISTRATION_QUERY = "INSERT INTO FIDO_DEVICE_STORE values(?, ?, ?)";
	private static final String GET_DEVICE_REGSITRATION_QUERY = "SELECT * FROM FIDO_DEVICE_STORE WHERE USER_NAME = ?";
	public static boolean isTraceEnabled = LOG.isTraceEnabled();

	/*static {
		LogManager.getRootLogger().setLevel(Level.TRACE);
	}*/

	/**
	 * Add Device Registration to store.
	 * @param username The username of Device Registration.
	 * @param registration The FIDO Registration.
	 * @throws IdentityException when SQL statement can not be executed.
	 */
	public void addDeviceRegistration(String username, DeviceRegistration registration) throws IdentityException {
		logTrace("Executing {addDeviceRegistration} method");
		Connection connection = null;
		String sql = "";
		PreparedStatement preparedStatement = null;

		try {
			connection = IdentityDatabaseUtil.getDBConnection();
			preparedStatement = connection.prepareStatement(sql);
			preparedStatement.setString(1, username);
			preparedStatement.setString(2, registration.getKeyHandle());
			preparedStatement.setString(3, registration.toJsonWithAttestationCert());
			preparedStatement.executeUpdate();
			connection.commit();

		} catch (SQLException e) {
			try {
				connection.rollback();
			} catch (SQLException e1) {
				LOG.error("Error rolling back the transaction to FIDO registration", e1);
			}
			throw new IdentityException("Error when executing FIDO registration SQL : " + sql, e);
		} finally {
			IdentityDatabaseUtil.closeAllConnections(connection, null, preparedStatement);
		}
		logTrace("Completed {addDeviceRegistration} method");
	}

	/**
	 * Retrieves Device Registration data from store.
	 * @param username The username of the Device Registration.
	 * @return Collection of Device Registration.
	 * @throws IdentityException when SQL statement can not be executed.
	 */
	public Collection getDeviceRegistration(String username) throws IdentityException {
		logTrace("Executing {getDeviceRegistration} method");
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		String sql = "";
		Multimap<String, String> devices = ArrayListMultimap.create();
		try {
			connection = IdentityDatabaseUtil.getDBConnection();
			preparedStatement = connection.prepareStatement(sql);
			preparedStatement.setString(1, username);
			resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				String keyHandle = resultSet.getString("KEY_HANDLE");
				String deviceData = resultSet.getString("DEVICE_DATA");
				devices.put(keyHandle, deviceData);

			}
		} catch (SQLException e) {
			throw new IdentityException("Error executing get device registration SQL : " + sql, e);
		} finally {
			IdentityDatabaseUtil.closeAllConnections(connection, resultSet, preparedStatement);
		}
		logTrace("Completed {getDeviceRegistration} method, returns devices of size :" + devices.size());
		return devices.values();
	}

	private void logTrace(String msg) {
		if (isTraceEnabled) {
			LOG.trace(msg);
		}
	}
}
