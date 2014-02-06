/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.provider.openid.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.model.OpenIDRememberMeDO;
import org.wso2.carbon.identity.core.persistence.JDBCPersistenceManager;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;

/**
 * This class is the DAO implementation for the OpenIDRememberMe token
 * 
 * @author WSO2 Inc
 *
 */
public class OpenIDRememberMeTokenDAO {

	protected Log log = LogFactory.getLog(OpenIDRememberMeTokenDAO.class);

	/**
	 * Updates the remember me token
	 * 
	 * @param rememberMe
	 * @throws Exception 
	 */
	public void updateTokenData(OpenIDRememberMeDO rememberMe) throws Exception {

		Connection connection = null;
		PreparedStatement prepStmt = null;

		try {
			connection = JDBCPersistenceManager.getInstance().getDBConnection();

			if (isTokenExist(connection, rememberMe)) {
				prepStmt = connection.prepareStatement(OpenIDSQLQueries.UPDATE_REMEMBER_ME_TOKEN);
				prepStmt.setString(2, rememberMe.getUserName());
				prepStmt.setInt(3, IdentityUtil.getTenantIdOFUser(rememberMe.getUserName()));
				prepStmt.setString(1, rememberMe.getToken());
				prepStmt.execute();
				connection.commit();
				log.debug("RememberMe token of " + rememberMe.getUserName() +
				          " successfully updated in the database.");
			} else {
				prepStmt = connection.prepareStatement(OpenIDSQLQueries.STORE_REMEMBER_ME_TOKEN);
				prepStmt.setString(1, rememberMe.getUserName());
				prepStmt.setInt(2, IdentityUtil.getTenantIdOFUser(rememberMe.getUserName()));
				prepStmt.setString(3, rememberMe.getToken());
				prepStmt.execute();
				connection.commit();
				log.debug("RememberMe token of " + rememberMe.getUserName() +
				          " successfully stored in the database.");
			}

		} catch (SQLException e) {
			log.error("Unable to update the token for " + rememberMe.getUserName() +
			          " Error while accessing the database", e);
			throw new Exception("Error while accessing the database");
		} catch (IdentityException e) {
			log.error("Unable to update the token for " + rememberMe.getUserName() +
			          " Error while accessing the database", e);
			throw new Exception("Error while accessing the database");
		} finally {
			IdentityDatabaseUtil.closeStatement(prepStmt);
			IdentityDatabaseUtil.closeConnection(connection);
		}
	}

	/**
	 * Return the remember me token after validations. Expairy will be checked.
	 * 
	 * @param rememberMe
	 * @return <code>OpenIDRememberMeDO</code>
	 * @throws Exception 
	 */
	public OpenIDRememberMeDO getTokenData(OpenIDRememberMeDO rememberMe) throws Exception {
		Connection connection = null;
		PreparedStatement prepStmt = null;

		try {
			connection = JDBCPersistenceManager.getInstance().getDBConnection();
			prepStmt = connection.prepareStatement(OpenIDSQLQueries.LOAD_REMEMBER_ME_TOKEN);
			prepStmt.setString(1, rememberMe.getUserName());
			prepStmt.setInt(2, IdentityUtil.getTenantIdOFUser(rememberMe.getUserName()));

			return buildRememberMeDO(prepStmt.executeQuery(), rememberMe.getUserName());

		} catch (SQLException e) {
			log.error("Unable to load RememberMe token for " + rememberMe.getUserName() +
			          " Error while accessing the database", e);
			throw new Exception("Error while accessing database");
		} finally {
			IdentityDatabaseUtil.closeStatement(prepStmt);
			IdentityDatabaseUtil.closeConnection(connection);
		}
	}

	/**
	 * Check if the token already exist in the database.
	 * 
	 * @param connection
	 * @param rememberMe
	 * @return
	 * @throws SQLException
	 * @throws IdentityException
	 */
	private boolean isTokenExist(Connection connection, OpenIDRememberMeDO rememberMe)
	                                                                                  throws IdentityException {

		PreparedStatement prepStmt = null;
		ResultSet results = null;
		boolean result = false;

		try {
			prepStmt = connection.prepareStatement(OpenIDSQLQueries.CHECK_REMEMBER_ME_TOKEN_EXIST);
			prepStmt.setString(1, rememberMe.getUserName());
			prepStmt.setInt(2, IdentityUtil.getTenantIdOFUser(rememberMe.getUserName()));
			results = prepStmt.executeQuery();

			if (results.next()) {
				result = true;
			}
		} catch (SQLException e) {
			log.error("Failed to load the RememberME token data for " + rememberMe.getUserName() +
			          ". Error while accessing the database", e);
		} finally {
			IdentityDatabaseUtil.closeResultSet(results);
			IdentityDatabaseUtil.closeStatement(prepStmt);
		}

		return result;
	}

	/**
	 * Builds the OpenIDRememberMeDo
	 * 
	 * @param results
	 * @param username
	 * @return
	 * @throws SQLException
	 */
	private OpenIDRememberMeDO buildRememberMeDO(ResultSet results, String username) {

		OpenIDRememberMeDO remDO = new OpenIDRememberMeDO();
		try {
			if (!results.next()) {
				log.debug("RememberMe token not found for the user " + username);
				return remDO;
			}
			remDO.setUserName(results.getString(1));
			remDO.setToken(results.getString(3));
			remDO.setTimestamp(results.getTimestamp(4));

		} catch (SQLException e) {
			log.error("Failed to create RememberMeDO for the user " + username +
			          ". Error while accessing the database", e);
		} finally {
			IdentityDatabaseUtil.closeResultSet(results);
		}
		return remDO;
	}

}