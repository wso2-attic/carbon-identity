/*
 *Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */
package org.wso2.carbon.identity.application.mgt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.application.mgt.DBQueries;
import org.wso2.carbon.identity.application.mgt.dto.ApplicationConfigDTO;
import org.wso2.carbon.identity.application.mgt.dto.AuthenticationStepConfigDTO;
import org.wso2.carbon.identity.application.mgt.dto.AuthenticatorConfigDTO;
import org.wso2.carbon.identity.application.mgt.dto.ClientConfigDTO;
import org.wso2.carbon.identity.application.mgt.dto.TrustedIDPConfigDTO;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.persistence.JDBCPersistenceManager;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;

public class ApplicationConfigDAO {

	Log log = LogFactory.getLog(ApplicationConfigDAO.class);

	// TODO : requires serialization

	public synchronized void storeApplicationData(ApplicationConfigDTO appConfigDTO)
			throws IdentityException {
		// basic info
		int tenantID = CarbonContext.getThreadLocalCarbonContext().getTenantId();
		String username = CarbonContext.getThreadLocalCarbonContext().getUsername();
		String appID = appConfigDTO.getApplicatIdentifier();

		// store app basic info
		// APP_ID, TENANT_ID, USERNAME
		Connection connection = JDBCPersistenceManager.getInstance().getDBConnection();
		PreparedStatement storeAppPrepStmt = null;
		try {
			storeAppPrepStmt = connection.prepareStatement(DBQueries.STORE_BASIC_APPINFO);
			storeAppPrepStmt.setString(1, appID);
			storeAppPrepStmt.setInt(2, tenantID);
			storeAppPrepStmt.setString(3, username);
			storeAppPrepStmt.execute();
			connection.commit();
			storeAppPrepStmt.close();

		} catch (SQLException e) {
			log.error(e.getMessage(), e);
			IdentityDatabaseUtil.closeConnection(connection);
			throw new IdentityException("Error while storing application");
		} finally {
			IdentityDatabaseUtil.closeStatement(storeAppPrepStmt);
		}
		// store client info
		// APP_ID, CLIENT_ID, CLIENT_SECRETE, CALLBACK_URL, TYPE
		PreparedStatement storeClientPrepStmt = null;
		try {
			storeClientPrepStmt = connection.prepareStatement(DBQueries.STORE_CLIENT_INFO);
			// client info
			ClientConfigDTO[] clientConf = appConfigDTO.getClientConfig();
			if (clientConf != null && clientConf.length > 0) {
				for (ClientConfigDTO dto : clientConf) {
					String clientid = dto.getClientID();
					String clientSecrete = dto.getClientSecrete();
					String callbackUrl = dto.getCallbackUrl();
					String clientType = dto.getType();

					storeClientPrepStmt.setString(1, appID);
					storeClientPrepStmt.setString(2, clientid);
					storeClientPrepStmt.setString(3, clientSecrete);
					storeClientPrepStmt.setString(4, callbackUrl);
					storeClientPrepStmt.setString(5, clientType);
					storeClientPrepStmt.addBatch();

				}
			}
			storeClientPrepStmt.executeBatch();
			if (!connection.getAutoCommit()) {
				connection.commit();
			}

		} catch (SQLException e) {
			log.error(e.getMessage(), e);
			IdentityDatabaseUtil.closeConnection(connection);
			throw new IdentityException("Error while storing application");
		} finally {
			IdentityDatabaseUtil.closeStatement(storeClientPrepStmt);
		}

		// IDP info
		// store step info
		// APP_ID, STEP_ID, AUTHN_ID, IDP_ID, ENDPOINT, TYPE

		PreparedStatement storeStepPrepStmt = null;
		try {
			storeStepPrepStmt = connection.prepareStatement(DBQueries.STORE_STEP_INFO);

			AuthenticationStepConfigDTO[] stepDTOs = appConfigDTO.getAuthenticationSteps();
			if (stepDTOs != null && stepDTOs.length > 0) {

				for (AuthenticationStepConfigDTO stepDTO : stepDTOs) {
					String stepID = stepDTO.getStepIdentifier();
					AuthenticatorConfigDTO[] authnDTOs = stepDTO.getAuthenticators();
					if (authnDTOs != null && authnDTOs.length > 0) {

						for (AuthenticatorConfigDTO authnDTO : authnDTOs) {
							String authenID = authnDTO.getAuthnticatorIdentifier();
							TrustedIDPConfigDTO[] idpDTOs = authnDTO.getIdps();
							if (idpDTOs != null && idpDTOs.length > 0) {

								for (TrustedIDPConfigDTO idpDTO : idpDTOs) {

									String idpID = idpDTO.getIdpIdentifier();
									String endpoints = idpDTO.getEndpointsString();
									String types = idpDTO.getTypesString();

									storeStepPrepStmt.setString(1, appID);
									storeStepPrepStmt.setString(2, stepID);
									storeStepPrepStmt.setString(3, authenID);
									storeStepPrepStmt.setString(4, idpID);
									storeStepPrepStmt.setString(5, endpoints);
									storeStepPrepStmt.setString(6, types);

									storeStepPrepStmt.addBatch();

								}
							}

						}
					}

				}

			}

			storeStepPrepStmt.executeBatch();
			if (!connection.getAutoCommit()) {
				connection.commit();
			}

		} catch (SQLException e) {
			log.error(e.getMessage(), e);
			throw new IdentityException("Error while storing application");
		} finally {
			IdentityDatabaseUtil.closeStatement(storeStepPrepStmt);
			IdentityDatabaseUtil.closeConnection(connection);
		}

	}

	public synchronized ApplicationConfigDTO updateApplicationData(ApplicationConfigDTO appConfigDTO) throws IdentityException {
		deleteApplication(appConfigDTO.getApplicatIdentifier(), true);
		storeApplicationData(appConfigDTO);
		return getApplicationDataFromID(appConfigDTO.getApplicatIdentifier());
	}
	
	public synchronized ApplicationConfigDTO getApplicationData(String clientId, String type)
			throws IdentityException {

		Connection connection = null;
		PreparedStatement getAppIDQuery = null;
		ResultSet resultSet = null;
		String appID = "";
		try {
			connection = JDBCPersistenceManager.getInstance().getDBConnection();
			getAppIDQuery = connection.prepareStatement(DBQueries.GET_APP_ID);
			getAppIDQuery.setString(1, clientId);
			getAppIDQuery.setString(2, type);
			resultSet = getAppIDQuery.executeQuery();
			if (resultSet.next()) {
				appID = resultSet.getString(1);
			}
		} catch (SQLException e) {
			log.error(e.getMessage(), e);
			throw new IdentityException("Error while retrieving all application");
		} finally {
			IdentityDatabaseUtil.closeStatement(getAppIDQuery);
			IdentityDatabaseUtil.closeResultSet(resultSet);
			IdentityDatabaseUtil.closeConnection(connection);
		}

		return getApplicationDataFromID(appID);
	}

	public synchronized ApplicationConfigDTO getApplicationDataFromID(String applicationID)
			throws IdentityException {

		ApplicationConfigDTO appConfigDTO = new ApplicationConfigDTO();
		appConfigDTO.setApplicatIdentifier(applicationID);
		ClientConfigDTO[] clientConfig;
		ArrayList<ClientConfigDTO> clientConfigList = new ArrayList<ClientConfigDTO>();
		AuthenticationStepConfigDTO[] authenticationStep;
		ArrayList<AuthenticationStepConfigDTO> authenticationStepList = new ArrayList<AuthenticationStepConfigDTO>();
		Connection connection = null;

		connection = JDBCPersistenceManager.getInstance().getDBConnection();

		PreparedStatement getClientInfo = null;
		ResultSet resultSet = null;
		try {
			getClientInfo = connection.prepareStatement(DBQueries.GET_CLIENT_INFO);
			getClientInfo.setString(1, applicationID);
			resultSet = getClientInfo.executeQuery();

			while (resultSet.next()) {
				ClientConfigDTO clientConfigDTO = new ClientConfigDTO();
				clientConfigDTO.setClientID(resultSet.getString(1));
				clientConfigDTO.setClientSecrete(resultSet.getString(2));
				clientConfigDTO.setCallbackUrl(resultSet.getString(3));
				clientConfigDTO.setType(resultSet.getString(4));
				clientConfigList.add(clientConfigDTO);

			}
		} catch (SQLException e) {
			log.error(e.getMessage(), e);
			throw new IdentityException("Error while retrieving all application");
		} finally {
			IdentityDatabaseUtil.closeStatement(getClientInfo);
			IdentityDatabaseUtil.closeResultSet(resultSet);
		}

		clientConfig = clientConfigList.toArray(new ClientConfigDTO[clientConfigList.size()]);
		appConfigDTO.setClientConfig(clientConfig);

		PreparedStatement getStepInfo = null;
		ResultSet stepInfoResultSet = null;

		try {
			getStepInfo = connection.prepareStatement(DBQueries.GET_STEP_INFO);
			getStepInfo.setString(1, applicationID);
			stepInfoResultSet = getStepInfo.executeQuery();

			HashMap<String, HashMap<String, ArrayList<TrustedIDPConfigDTO>>> stepInfoMap = new HashMap<String, HashMap<String, ArrayList<TrustedIDPConfigDTO>>>();
			while (stepInfoResultSet.next()) {
				String stepID = stepInfoResultSet.getString(1);

				if (stepInfoMap.containsKey(stepID)) {
					String authID = stepInfoResultSet.getString(2);
					HashMap<String, ArrayList<TrustedIDPConfigDTO>> authMap = stepInfoMap
							.get(stepID);
					if (authMap.containsKey(authID)) {
						ArrayList<TrustedIDPConfigDTO> trustedIDPConfigList = authMap.get(authID);
						TrustedIDPConfigDTO trustedIDP = new TrustedIDPConfigDTO();
						trustedIDP.setIdpIdentifier(stepInfoResultSet.getString(3));
						trustedIDP.setEndpointsString(stepInfoResultSet.getString(4));
						trustedIDP.setTypesString(stepInfoResultSet.getString(5));
						trustedIDPConfigList.add(trustedIDP);

					} else {
						ArrayList<TrustedIDPConfigDTO> trustedIDPConfigList = new ArrayList<TrustedIDPConfigDTO>();
						TrustedIDPConfigDTO trustedIDP = new TrustedIDPConfigDTO();
						trustedIDP.setIdpIdentifier(stepInfoResultSet.getString(3));
						trustedIDP.setEndpointsString(stepInfoResultSet.getString(4));
						trustedIDP.setTypesString(stepInfoResultSet.getString(5));
						trustedIDPConfigList.add(trustedIDP);
						authMap.put(authID, trustedIDPConfigList);
					}

				} else {
					String authID = stepInfoResultSet.getString(2);
					HashMap<String, ArrayList<TrustedIDPConfigDTO>> authMap = new HashMap<String, ArrayList<TrustedIDPConfigDTO>>();
					ArrayList<TrustedIDPConfigDTO> tempIDPList = new ArrayList<TrustedIDPConfigDTO>();
					authMap.put(authID, tempIDPList);
					TrustedIDPConfigDTO trustedIDP = new TrustedIDPConfigDTO();
					trustedIDP.setIdpIdentifier(stepInfoResultSet.getString(3));
					trustedIDP.setEndpointsString(stepInfoResultSet.getString(4));
					trustedIDP.setTypesString(stepInfoResultSet.getString(5));
					tempIDPList.add(trustedIDP);
					stepInfoMap.put(stepID, authMap);
				}
			}

			for (String key : stepInfoMap.keySet()) {
				AuthenticationStepConfigDTO authConfigDTO = new AuthenticationStepConfigDTO();
				authConfigDTO.setStepIdentifier(key);
				ArrayList<AuthenticatorConfigDTO> authConfigList = new ArrayList<AuthenticatorConfigDTO>();

				HashMap<String, ArrayList<TrustedIDPConfigDTO>> authMap = stepInfoMap.get(key);
				for (String authKey : authMap.keySet()) {
					AuthenticatorConfigDTO authConfig = new AuthenticatorConfigDTO();
					authConfig.setAuthnticatorIdentifier(authKey);
					ArrayList<TrustedIDPConfigDTO> tempTrustedList = authMap.get(authKey);
					authConfig.setIdps(tempTrustedList
							.toArray(new TrustedIDPConfigDTO[tempTrustedList.size()]));
					authConfigList.add(authConfig);

				}
				authConfigDTO.setAuthenticators(authConfigList
						.toArray(new AuthenticatorConfigDTO[authConfigList.size()]));
				authenticationStepList.add(authConfigDTO);

			}
			authenticationStep = authenticationStepList
					.toArray(new AuthenticationStepConfigDTO[authenticationStepList.size()]);
			stepInfoResultSet.close();
			connection.close();
			appConfigDTO.setAuthenticationSteps(authenticationStep);

		} catch (SQLException e) {
			log.error(e.getMessage(), e);
			throw new IdentityException("Error while retrieving all application");
		} finally {
			IdentityDatabaseUtil.closeStatement(getStepInfo);
			IdentityDatabaseUtil.closeResultSet(stepInfoResultSet);
			IdentityDatabaseUtil.closeConnection(connection);
		}

		return appConfigDTO;
	}

	public synchronized ApplicationConfigDTO[] getAllApplicationData() throws IdentityException {

		Connection connection = null;
		PreparedStatement getClientInfo = null;
		ResultSet resultSet = null;

		ArrayList<ApplicationConfigDTO> appConfigList = new ArrayList<ApplicationConfigDTO>();
		ArrayList<String> appIds = new ArrayList<String>();
		int tenantID = CarbonContext.getThreadLocalCarbonContext().getTenantId();

		try {
			connection = JDBCPersistenceManager.getInstance().getDBConnection();
			getClientInfo = connection.prepareStatement(DBQueries.GET_ALL_APP);
			getClientInfo.setInt(1, tenantID);
			resultSet = getClientInfo.executeQuery();

			while (resultSet.next()) {
				appIds.add(resultSet.getString(1));
			}

			resultSet.close();
			connection.close();
		} catch (SQLException e) {
			log.error(e.getMessage(), e);
			throw new IdentityException("Error while retrieving all application");
		} finally {
			IdentityDatabaseUtil.closeStatement(getClientInfo);
			IdentityDatabaseUtil.closeResultSet(resultSet);
			IdentityDatabaseUtil.closeConnection(connection);
		}

		for (String appId : appIds) {
			appConfigList.add(getApplicationDataFromID(appId));
		}

		return appConfigList.toArray(new ApplicationConfigDTO[appConfigList.size()]);
	}

	public synchronized void deleteApplication(String appID, boolean isUpdate) throws IdentityException {

		Connection connection = null;
		PreparedStatement getClientsQuery = null;
		PreparedStatement deleteAppIDQuery = null;
		PreparedStatement deleteAppIDQuery2 = null;
		PreparedStatement deleteAppIDQuery3 = null;

		connection = JDBCPersistenceManager.getInstance().getDBConnection();

		if (!isUpdate) {
			ResultSet clientResults = null;
			try {
				getClientsQuery = connection.prepareStatement(DBQueries.GET_CLIENT_INFO);
				getClientsQuery.setString(1, appID);
				clientResults = getClientsQuery.executeQuery();
				if (clientResults != null) {
					while (clientResults.next()) {
						String clientID = clientResults.getString(1);
						String type = clientResults.getString(4);
						deleteClient(clientID, type);
					}
				}

			} catch (SQLException e) {
				log.error(e.getMessage(), e);
				throw new IdentityException("Error while deleting application");
			}
		}

		try {
			deleteAppIDQuery = connection.prepareStatement(DBQueries.REMOVE_APP_FROM_APPMGT_STEP);
			deleteAppIDQuery.setString(1, appID);
			deleteAppIDQuery.executeUpdate();
		} catch (SQLException e) {

		} finally {
			IdentityDatabaseUtil.closeStatement(deleteAppIDQuery);
		}

		try {
			deleteAppIDQuery2 = connection
					.prepareStatement(DBQueries.REMOVE_APP_FROM_APPMGT_CLIENT);
			deleteAppIDQuery2.setString(1, appID);
			deleteAppIDQuery2.executeUpdate();
		} catch (SQLException e) {
			log.error(e.getMessage(), e);
			throw new IdentityException("Error while deletig application");
		} finally {
			IdentityDatabaseUtil.closeStatement(deleteAppIDQuery2);
		}

		try {
			deleteAppIDQuery3 = connection.prepareStatement(DBQueries.REMOVE_APP_FROM_APPMGT_APP);
			deleteAppIDQuery3.setString(1, appID);
			deleteAppIDQuery3.executeUpdate();
			if (!connection.getAutoCommit()) {
				connection.commit();
			}

		} catch (SQLException e) {
			log.error(e.getMessage(), e);
			throw new IdentityException("Error while deleting application");
		} finally {
			IdentityDatabaseUtil.closeStatement(deleteAppIDQuery3);
			IdentityDatabaseUtil.closeConnection(connection);
		}

	}

	private void deleteClient(String clientID, String type) throws IdentityException {
		if ("samlsso".equalsIgnoreCase(type)) {
			new SAMLConfigDAO().removeSPConfig(clientID);
		} else if ("oauth2".equalsIgnoreCase(type)) {
			new OAuthOIDCConfigDAO().removeClient(clientID);
		}
	}

}
