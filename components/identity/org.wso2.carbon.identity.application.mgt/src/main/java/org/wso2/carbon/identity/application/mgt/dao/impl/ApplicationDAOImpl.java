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
package org.wso2.carbon.identity.application.mgt.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.application.common.model.ApplicationBasicInfo;
import org.wso2.carbon.identity.application.common.model.AuthenticationStep;
import org.wso2.carbon.identity.application.common.model.Claim;
import org.wso2.carbon.identity.application.common.model.ClaimConfiguration;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.FederatedAuthenticator;
import org.wso2.carbon.identity.application.common.model.FederatedIdentityProvider;
import org.wso2.carbon.identity.application.common.model.InboundAuthenticationConfig;
import org.wso2.carbon.identity.application.common.model.InboundAuthenticationRequest;
import org.wso2.carbon.identity.application.common.model.InboundProvisioningConfiguration;
import org.wso2.carbon.identity.application.common.model.LocalAndOutboundAuthenticationConfig;
import org.wso2.carbon.identity.application.common.model.LocalAuthenticator;
import org.wso2.carbon.identity.application.common.model.LocalRole;
import org.wso2.carbon.identity.application.common.model.OutboundProvisioningConfiguration;
import org.wso2.carbon.identity.application.common.model.PermissionsAndRoleConfiguration;
import org.wso2.carbon.identity.application.common.model.ProvisioningConnector;
import org.wso2.carbon.identity.application.common.model.RequestPathAuthenticator;
import org.wso2.carbon.identity.application.common.model.RoleMapping;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.common.model.ServiceProviderOwner;
import org.wso2.carbon.identity.application.mgt.ApplicationConstants;
import org.wso2.carbon.identity.application.mgt.ApplicationMgtDBQueries;
import org.wso2.carbon.identity.application.mgt.ApplicationMgtSystemConfig;
import org.wso2.carbon.identity.application.mgt.ApplicationMgtUtil;
import org.wso2.carbon.identity.application.mgt.dao.ApplicationDAO;
import org.wso2.carbon.identity.application.mgt.dao.IdentityProviderDAO;
import org.wso2.carbon.identity.application.mgt.internal.ApplicationManagementServiceComponentHolder;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.persistence.JDBCPersistenceManager;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.util.UserCoreUtil;

/**
 * This class access the IDN_APPMGT database to store/update and delete application configurations.
 * The IDN_APPMGT database contains few tables
 * <ul>
 * <li>IDN_APPMGT_APP</li>
 * <li>IDN_APPMGT_CLIENT</li>
 * <li>IDN_APPMGT_STEP</li>
 * <li>IDN_APPMGT_STEP_IDP</li>
 * <li>IDN_APPMGT_CLAIM_MAPPING</li>
 * <li>IDN_APPMGT_ROLE_MAPPING</li>
 * </ul>
 * 
 */
public class ApplicationDAOImpl implements ApplicationDAO {

    Log log = LogFactory.getLog(ApplicationDAOImpl.class);
    boolean debugMode = log.isDebugEnabled();

    /**
     * Stores basic application information and meta-data such as the application name, creator and
     * tenant.
     * 
     * @param serviceProvider
     * @throws IdentityException
     */
    public int createApplication(ServiceProvider serviceProvider) throws IdentityException {

        // get logged-in users tenant identifier.
        int tenantID = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        String qualifiedUsername = CarbonContext.getThreadLocalCarbonContext().getUsername();
        String username = UserCoreUtil.removeDomainFromName(qualifiedUsername);
        String userStoreDomain = UserCoreUtil.extractDomainFromName(qualifiedUsername);
        String applicationName = serviceProvider.getApplicationName();
        String description = serviceProvider.getDescription();

        if (applicationName == null || description == null) {
            // check for required attributes.
            throw new IdentityException("Application Name and Description are required.");
        }

        if (debugMode) {
            log.debug("Creating Application " + applicationName + " for user " + qualifiedUsername);
        }

        Connection connection = JDBCPersistenceManager.getInstance().getDBConnection();
        PreparedStatement storeAppPrepStmt = null;
        ResultSet results = null;

        try {
            storeAppPrepStmt = connection.prepareStatement(
                    ApplicationMgtDBQueries.STORE_BASIC_APPINFO, new String[] { "ID" });
            // APP_NAME, USERNAME, TENANT_ID, USER_STORE, AUTH_TYPE, DESCRIPTION
            storeAppPrepStmt.setString(1, applicationName);
            storeAppPrepStmt.setString(2, username);
            storeAppPrepStmt.setInt(3, tenantID);
            storeAppPrepStmt.setString(4, userStoreDomain);
            // by default authentication type would be default.
            // default authenticator is defined system-wide - in the configuration file.
            storeAppPrepStmt.setString(5, ApplicationConstants.AUTH_TYPE_DEFAULT);
            storeAppPrepStmt.setString(6, description);
            storeAppPrepStmt.execute();

            results = storeAppPrepStmt.getGeneratedKeys();

            if (!connection.getAutoCommit()) {
                connection.commit();
            }

            int applicationId = 0;
            if (results.next()) {
                applicationId = results.getInt(1);
            }
            // some JDBC Drivers returns this in the result, some don't
            if (applicationId == 0) {
                if (debugMode) {
                    log.debug("JDBC Driver did not return the application id, executing Select operation");
                }
                applicationId = getApplicationIDByName(applicationName, tenantID, connection);
            }

            if (debugMode) {
                log.debug("Application Stored successfully with application id " + applicationId);
            }

            return applicationId;

        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException sql) {
                throw new IdentityException("Error while Creating Application", sql);
            }
            throw new IdentityException("Error while Creating Application", e);
        } finally {
            IdentityDatabaseUtil.closeResultSet(results);
            IdentityDatabaseUtil.closeStatement(storeAppPrepStmt);
            IdentityDatabaseUtil.closeConnection(connection);
        }
    }

    /**
     * 
     */
    public void updateApplication(ServiceProvider serviceProvider) throws IdentityException {

        Connection connection = IdentityDatabaseUtil.getDBConnection();
        int applicationId = serviceProvider.getApplicationID();

        try {

            // update basic information of the application.
            // you can change application name, description...
            updateBasicApplicationData(applicationId, serviceProvider.getApplicationName(),
                    serviceProvider.getDescription(), connection);
            updateInboundProvisioningConfiguration(applicationId,
                    serviceProvider.getInboundProvisioningConfiguration(), connection);

            // delete all in-bound authentication requests.
            deleteInboundAuthRequestConfiguration(serviceProvider.getApplicationID(), connection);

            // update all in-bound authentication requests.
            updateInboundAuthRequestConfiguration(serviceProvider.getApplicationID(),
                    serviceProvider.getInboundAuthenticationConfig(), connection);

            // delete local and out-bound authentication configuration.
            deleteLocalAndOutboundAuthenticationConfiguration(applicationId, connection);

            // update local and out-bound authentication configuration.
            updateLocalAndOutboundAuthenticationConfiguration(serviceProvider.getApplicationID(),
                    serviceProvider.getLocalAndOutBoundAuthenticationConfig(), connection);

            deleteRequestPathAuthenticators(applicationId, connection);
            updateRequestPathAuthenticators(applicationId,
                    serviceProvider.getRequestPathAuthenticators(), connection);

            deteClaimConfiguration(applicationId, connection);
            updateClaimConfiguration(serviceProvider.getApplicationID(),
                    serviceProvider.getClaimConfiguration(), applicationId, connection);

            deleteOutboundProvisioningConfiguration(applicationId, connection);
            updateOutboundProvisioningConfiguration(applicationId,
                    serviceProvider.getOutboundProvisioningConfiguration(), connection);

            deletePermissionAndRoleConfiguration(applicationId, connection);
            updatePermissionAndRoleConfiguration(serviceProvider.getApplicationID(),
                    serviceProvider.getPermissionAndRoleConfiguration(), connection);

            if (!connection.getAutoCommit()) {
                connection.commit();
            }
        } catch (IdentityException e) {
            try {
                connection.rollback();
            } catch (SQLException e1) {
                throw new IdentityException("Failed to update service provider " + applicationId, e);
            }
            throw new IdentityException("Failed to update service provider " + applicationId, e);
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException e1) {
                throw new IdentityException("Failed to update service provider " + applicationId, e);
            }
            throw new IdentityException("Failed to update service provider " + applicationId, e);
        } catch (UserStoreException e) {
            try {
                connection.rollback();
            } catch (SQLException e1) {
                throw new IdentityException("Failed to update service provider " + applicationId, e);
            }
            throw new IdentityException("Failed to update service provider " + applicationId, e);
        } finally {
            IdentityDatabaseUtil.closeConnection(connection);
        }
    }

    /**
     * 
     * @param applicationId
     * @param applicationName
     * @param description
     * @param connection
     * @throws SQLException
     * @throws UserStoreException
     * @throws IdentityException
     */
    private void updateBasicApplicationData(int applicationId, String applicationName,
            String description, Connection connection) throws SQLException, UserStoreException,
            IdentityException {

        int tenantID = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        String storedAppName = null;

        if (applicationName == null || description == null) {
            // check for required attributes.
            throw new IdentityException("Application Name and Description are required.");
        }

        if (debugMode) {
            log.debug("Updating Application with ID: " + applicationId);
        }
        // reads back the Application Name. This is to check if the Application
        // has been renamed
        storedAppName = getApplicationName(applicationId, connection);

        if (debugMode) {
            log.debug("Stored Application Name " + storedAppName);
        }

        // only if the application has been renamed
        if (!applicationName.equalsIgnoreCase(storedAppName)) {
            // rename the role
            ApplicationMgtUtil.renameRole(storedAppName, applicationName);
            if (debugMode) {
                log.debug("Renaming application role from " + storedAppName + " to "
                        + applicationName);
            }
        }

        // update the application data
        PreparedStatement storeAppPrepStmt = null;
        try {
            storeAppPrepStmt = connection
                    .prepareStatement(ApplicationMgtDBQueries.UPDATE_BASIC_APPINFO);
            // SET APP_NAME=?, DESCRIPTION=? WHERE TENANT_ID= ? AND ID = ?
            storeAppPrepStmt.setString(1, applicationName);
            storeAppPrepStmt.setString(2, description);
            storeAppPrepStmt.setInt(3, tenantID);
            storeAppPrepStmt.setInt(4, applicationId);
            storeAppPrepStmt.executeUpdate();

        } finally {
            IdentityDatabaseUtil.closeStatement(storeAppPrepStmt);
        }

        if (debugMode) {
            log.debug("Updated Application successfully");
        }

    }

    /**
     * 
     * @param applicationId
     * @param inBoundAuthenticationConfig
     * @param connection
     * @throws SQLException
     */
    private void updateInboundAuthRequestConfiguration(int applicationId,
            InboundAuthenticationConfig inBoundAuthenticationConfig, Connection connection)
            throws SQLException {
        int tenantID = CarbonContext.getThreadLocalCarbonContext().getTenantId();

        PreparedStatement inboundAuthReqConfigPrepStmt = null;

        try {
            if (inBoundAuthenticationConfig == null
                    || inBoundAuthenticationConfig.getInboundAuthenticationRequests() == null
                    || inBoundAuthenticationConfig.getInboundAuthenticationRequests().length == 0) {
                // no in-bound authentication requests defined.
                return;
            }

            inboundAuthReqConfigPrepStmt = connection
                    .prepareStatement(ApplicationMgtDBQueries.STORE_CLIENT_INFO);
            InboundAuthenticationRequest[] authRequests = inBoundAuthenticationConfig
                    .getInboundAuthenticationRequests();

            for (InboundAuthenticationRequest authRequest : authRequests) {
                if (authRequest == null || authRequest.getClientID() == null
                        || authRequest.getType() == null) {
                    log.warn("Invalid in-bound authentication request");
                    // not a valid authentication request. Must have client and a type.
                    continue;
                }
                // CLIENT_ID, CLIENT_TYPE, TENANT_ID, APP_ID
                inboundAuthReqConfigPrepStmt.setString(1, authRequest.getClientID());
                inboundAuthReqConfigPrepStmt.setString(2, authRequest.getType());
                inboundAuthReqConfigPrepStmt.setInt(3, tenantID);
                inboundAuthReqConfigPrepStmt.setInt(4, applicationId);
                inboundAuthReqConfigPrepStmt.addBatch();

                if (debugMode) {
                    log.debug("Updating inbound authentication request configuration of the application "
                            + applicationId
                            + "ClientID: "
                            + authRequest.getClientID()
                            + " ClientType: " + authRequest.getType());
                }
            }

            inboundAuthReqConfigPrepStmt.executeBatch();
        } finally {
            IdentityDatabaseUtil.closeStatement(inboundAuthReqConfigPrepStmt);
        }
    }

    /**
     * 
     * @param applicationId
     * @param inBoundProvisioningConfig
     * @param connection
     * @throws SQLException
     */
    private void updateInboundProvisioningConfiguration(int applicationId,
            InboundProvisioningConfiguration inBoundProvisioningConfig, Connection connection)
            throws SQLException {

        int tenantID = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        PreparedStatement inboundProConfigPrepStmt = null;

        try {
            if (inBoundProvisioningConfig == null
                    || inBoundProvisioningConfig.getProvisioningUserStore() == null) {
                // no in-bound authentication requests defined.
                return;
            }

            inboundProConfigPrepStmt = connection
                    .prepareStatement(ApplicationMgtDBQueries.UPDATE_BASIC_APPINFO_WITH_PRO_USERSTORE);

            // PROVISIONING_USERSTORE_DOMAIN=?
            inboundProConfigPrepStmt.setString(1,
                    inBoundProvisioningConfig.getProvisioningUserStore());
            inboundProConfigPrepStmt.setInt(2, tenantID);
            inboundProConfigPrepStmt.setInt(3, applicationId);
            inboundProConfigPrepStmt.execute();

        } finally {
            IdentityDatabaseUtil.closeStatement(inboundProConfigPrepStmt);
        }
    }

    /**
     * 
     * @param applicationId
     * @param outBoundProvisioningConfig
     * @param connection
     * @throws SQLException
     */
    private void updateOutboundProvisioningConfiguration(int applicationId,
            OutboundProvisioningConfiguration outBoundProvisioningConfig, Connection connection)
            throws SQLException {

        int tenantID = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        PreparedStatement outboundProConfigPrepStmt = null;

        FederatedIdentityProvider[] proProviders = outBoundProvisioningConfig
                .getProvisioningIdentityProviders();

        try {
            if (outBoundProvisioningConfig == null || proProviders == null
                    || proProviders.length == 0) {
                // no in-bound authentication requests defined.
                return;
            }

            outboundProConfigPrepStmt = connection
                    .prepareStatement(ApplicationMgtDBQueries.STORE_PRO_CONNECTORS);
            // IDP_NAME, CONNECTOR_NAME,APP_ID,TENANT_ID

            for (FederatedIdentityProvider proProvider : proProviders) {
                if (proProvider != null) {
                    ProvisioningConnector proConnector = proProvider
                            .getDefaultProvisioinongConnector();
                    if (proConnector == null) {
                        continue;
                    }

                    outboundProConfigPrepStmt.setString(1, proProvider.getIdentityProviderName());
                    outboundProConfigPrepStmt.setString(2, proConnector.getName());
                    outboundProConfigPrepStmt.setInt(3, applicationId);
                    outboundProConfigPrepStmt.setInt(4, tenantID);
                    outboundProConfigPrepStmt.addBatch();

                }
            }

            outboundProConfigPrepStmt.executeBatch();

        } finally {
            IdentityDatabaseUtil.closeStatement(outboundProConfigPrepStmt);
        }
    }

    /**
     * 
     * @param applicationId
     * @param connection
     * @return
     * @throws SQLException
     */
    private InboundProvisioningConfiguration getInboundProvisioningConfiguration(int applicationId,
            Connection connection) throws SQLException {

        int tenantID = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        PreparedStatement inboundProConfigPrepStmt = null;
        InboundProvisioningConfiguration inBoundProvisioningConfig = new InboundProvisioningConfiguration();
        ResultSet resultSet = null;

        try {

            inboundProConfigPrepStmt = connection
                    .prepareStatement(ApplicationMgtDBQueries.LOAD_PRO_USERSTORE_BY_APP_ID);
            // PROVISIONING_USERSTORE_DOMAIN
            inboundProConfigPrepStmt.setInt(1, tenantID);
            inboundProConfigPrepStmt.setInt(2, applicationId);
            resultSet = inboundProConfigPrepStmt.executeQuery();

            while (resultSet.next()) {
                inBoundProvisioningConfig.setProvisioningUserStore(resultSet.getString(1));
            }

        } finally {
            IdentityDatabaseUtil.closeStatement(inboundProConfigPrepStmt);
        }
        return inBoundProvisioningConfig;
    }

    /**
     * 
     * @param applicationId
     * @param connection
     * @return
     * @throws SQLException
     */
    private OutboundProvisioningConfiguration getOutboundProvisioningConfiguration(
            int applicationId, Connection connection) throws SQLException {

        int tenantID = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        PreparedStatement outboundProConfigPrepStmt = null;
        OutboundProvisioningConfiguration outBoundProvisioningConfig = new OutboundProvisioningConfiguration();
        ResultSet resultSet = null;
        List<FederatedIdentityProvider> idpProConnectors = new ArrayList<FederatedIdentityProvider>();

        try {

            outboundProConfigPrepStmt = connection
                    .prepareStatement(ApplicationMgtDBQueries.LOAD_PRO_CONNECTORS_BY_APP_ID);
            // IDP_NAME, CONNECTOR_NAM
            outboundProConfigPrepStmt.setInt(1, applicationId);
            outboundProConfigPrepStmt.setInt(2, tenantID);
            resultSet = outboundProConfigPrepStmt.executeQuery();

            while (resultSet.next()) {
                ProvisioningConnector proConnector = null;
                FederatedIdentityProvider fedIdp = null;

                fedIdp = new FederatedIdentityProvider();
                fedIdp.setIdentityProviderName(resultSet.getString(1));

                proConnector = new ProvisioningConnector();
                proConnector.setName(resultSet.getString(2));

                fedIdp.setDefaultProvisioinongConnector(proConnector);
                idpProConnectors.add(fedIdp);

            }

            outBoundProvisioningConfig.setProvisioningIdentityProviders(idpProConnectors
                    .toArray(new FederatedIdentityProvider[idpProConnectors.size()]));

        } finally {
            IdentityDatabaseUtil.closeStatement(outboundProConfigPrepStmt);
        }
        return outBoundProvisioningConfig;
    }

    /**
     * 
     * @param applicationId
     * @param localAndOutboundAuthConfig
     * @param connection
     * @throws SQLException
     * @throws IdentityException
     */
    private void updateLocalAndOutboundAuthenticationConfiguration(int applicationId,
            LocalAndOutboundAuthenticationConfig localAndOutboundAuthConfig, Connection connection)
            throws SQLException, IdentityException {

        int tenantID = CarbonContext.getThreadLocalCarbonContext().getTenantId();

        PreparedStatement updateAuthTypePrepStmt = null;
        if (localAndOutboundAuthConfig == null) {
            // no local or out-bound configuration for this service provider.
            return;
        }

        AuthenticationStep[] authSteps = localAndOutboundAuthConfig.getAuthenticationSteps();

        if (authSteps == null || authSteps.length == 0) {
            // if no authentication steps defined - it should be the default behavior.
            localAndOutboundAuthConfig
                    .setAuthenticationType(ApplicationConstants.AUTH_TYPE_DEFAULT);
        }

        try {
            if (localAndOutboundAuthConfig.getAuthenticationType() == null) {
                // no authentication type defined - set to default.
                localAndOutboundAuthConfig
                        .setAuthenticationType(ApplicationConstants.AUTH_TYPE_DEFAULT);
            }

            updateAuthTypePrepStmt = connection
                    .prepareStatement(ApplicationMgtDBQueries.UPDATE_BASIC_APPINFO_WITH_AUTH_TYPE);
            // AUTH_TYPE=? WHERE TENANT_ID= ? AND ID = ?
            updateAuthTypePrepStmt.setString(1, localAndOutboundAuthConfig.getAuthenticationType());
            updateAuthTypePrepStmt.setInt(2, tenantID);
            updateAuthTypePrepStmt.setInt(3, applicationId);
            updateAuthTypePrepStmt.execute();
        } finally {
            IdentityDatabaseUtil.closeStatement(updateAuthTypePrepStmt);
        }

        if (authSteps != null && authSteps.length > 0) {
            // we have authentications steps defined.
            PreparedStatement storeStepIDPAuthnPrepStmt = null;
            storeStepIDPAuthnPrepStmt = connection
                    .prepareStatement(ApplicationMgtDBQueries.STORE_STEP_IDP_AUTH);
            try {

                if (ApplicationConstants.AUTH_TYPE_LOCAL
                        .equalsIgnoreCase(localAndOutboundAuthConfig.getAuthenticationType())) {
                    // for local authentication there can only be only one authentication step and
                    // only one local authenticator.
                    if (authSteps.length != 1 || authSteps[0] == null
                            || authSteps[0].getLocalAuthenticators() == null
                            || authSteps[0].getLocalAuthenticators().length != 1
                            || authSteps[0].getFederatedIdentityProviders() != null) {
                        String errorMessage = "Invalid local authentication configuration."
                                + " For local authentication there can only be only one authentication step and only one local authenticator";
                        throw new IdentityException(errorMessage);
                    }
                } else if (ApplicationConstants.AUTH_TYPE_FEDERATED
                        .equalsIgnoreCase(localAndOutboundAuthConfig.getAuthenticationType())) {
                    // for federated authentication there can only be only one authentication step
                    // and only one federated authenticator - which is the default authenticator of
                    // the corresponding authenticator.
                    if (authSteps.length != 1 || authSteps[0] == null
                            || authSteps[0].getFederatedIdentityProviders() == null
                            || authSteps[0].getFederatedIdentityProviders().length != 1
                            || authSteps[0].getLocalAuthenticators() != null) {
                        String errorMessage = "Invalid federated authentication configuration."
                                + " For federated authentication there can only be only one authentication step and only one federated authenticator";
                        throw new IdentityException(errorMessage);
                    }

                    FederatedIdentityProvider fedIdp = authSteps[0].getFederatedIdentityProviders()[0];
                    IdentityProviderDAO idpDAO = ApplicationMgtSystemConfig.getInstance()
                            .getIdentityProviderDAO();
                    FederatedIdentityProvider systemFedIdp = idpDAO
                            .getFederatedIdentityProvider(fedIdp.getIdentityProviderName());
                    String defualtAuthName = idpDAO.getDefaultAuthenticator(systemFedIdp
                            .getIdentityProviderName());
                    // set the default authenticator.
                    FederatedAuthenticator defaultAuth = new FederatedAuthenticator();
                    defaultAuth.setName(defualtAuthName);
                    fedIdp.setDefaultAuthenticator(defaultAuth);
                    fedIdp.setFederatedAuthenticators(new FederatedAuthenticator[] { defaultAuth });
                }

                // iterating through each step.
                for (AuthenticationStep authStep : authSteps) {
                    int stepId = 0;

                    FederatedIdentityProvider[] federatedIdps = authStep
                            .getFederatedIdentityProviders();

                    // an authentication step should have at least one federated identity
                    // provider or a local authenticator.
                    if ((federatedIdps == null || federatedIdps.length == 0)
                            && (authStep.getLocalAuthenticators() == null || authStep
                                    .getLocalAuthenticators().length == 0)) {
                        String errorMesssage = "Invalid authentication configuration."
                                + "An authentication step should have at least one federated identity "
                                + "provider or a local authenticator";
                        throw new IdentityException(errorMesssage);
                    }

                    // we have valid federated identity providers.
                    PreparedStatement storeStepPrepStmtz = null;
                    ResultSet result = null;

                    try {
                        storeStepPrepStmtz = connection.prepareStatement(
                                ApplicationMgtDBQueries.STORE_STEP_INFO, new String[] { "ID" });
                        // STEP_ORDER, APP_ID
                        storeStepPrepStmtz.setInt(1, authStep.getStepOrder());
                        storeStepPrepStmtz.setInt(2, applicationId);
                        storeStepPrepStmtz.setInt(3, tenantID);
                        storeStepPrepStmtz.execute();

                        result = storeStepPrepStmtz.getGeneratedKeys();

                        if (result.next()) {
                            stepId = result.getInt(1);
                        }
                    } finally {
                        IdentityDatabaseUtil.closeResultSet(result);
                        IdentityDatabaseUtil.closeStatement(storeStepPrepStmtz);
                    }

                    if (authStep.getLocalAuthenticators() != null
                            && authStep.getLocalAuthenticators().length > 0) {

                        for (LocalAuthenticator lclAuthenticator : authStep
                                .getLocalAuthenticators()) {
                            // set the identity provider name to wso2carbon-local-idp.
                            String idpName = ApplicationConstants.LOCAL_IDP;
                            // STEP_ID, IDP_NAME, AUTHENTICATOR_NAME
                            storeStepIDPAuthnPrepStmt.setInt(1, stepId);
                            storeStepIDPAuthnPrepStmt.setString(2, idpName);
                            storeStepIDPAuthnPrepStmt.setString(3, lclAuthenticator.getName());
                            storeStepIDPAuthnPrepStmt.addBatch();

                            if (debugMode) {
                                log.debug("Updating Local IdP of Application " + applicationId
                                        + " Step Order: " + authStep.getStepOrder() + " IdP: "
                                        + idpName + " Authenticator: " + lclAuthenticator.getName());
                            }
                        }
                    }

                    // we have federated identity providers.
                    if (federatedIdps != null && federatedIdps.length > 0) {

                        // iterating through each IDP of the step
                        for (FederatedIdentityProvider federatedIdp : federatedIdps) {
                            String idpName = federatedIdp.getIdentityProviderName();

                            // the identity provider name wso2carbon-local-idp is reserved.
                            if (ApplicationConstants.LOCAL_IDP.equalsIgnoreCase(idpName)) {
                                throw new IdentityException(
                                        "The federated IdP name cannot be equal to "
                                                + ApplicationConstants.LOCAL_IDP);
                            }

                            FederatedAuthenticator[] authenticators = federatedIdp
                                    .getFederatedAuthenticators();

                            if (authenticators != null && authenticators.length > 0) {

                                for (FederatedAuthenticator authenticator : authenticators) {
                                    // STEP_ID, IDP_NAME, AUTHENTICATOR_NAME
                                    if (authenticator != null) {
                                        storeStepIDPAuthnPrepStmt.setInt(1, stepId);
                                        storeStepIDPAuthnPrepStmt.setString(2, idpName);
                                        storeStepIDPAuthnPrepStmt.setString(3,
                                                authenticator.getName());
                                        storeStepIDPAuthnPrepStmt.addBatch();

                                        if (debugMode) {
                                            log.debug("Updating Federated IdP of Application "
                                                    + applicationId + " Step Order: "
                                                    + authStep.getStepOrder() + " IdP: " + idpName
                                                    + " Authenticator: " + authenticator);
                                        }
                                    }
                                }
                            }

                        }
                    }
                }

                storeStepIDPAuthnPrepStmt.executeBatch();
            } finally {
                IdentityDatabaseUtil.closeStatement(storeStepIDPAuthnPrepStmt);
            }
        }
    }

    /**
     * 
     * @param applicationId
     * @param claimConfiguration
     * @param applicationID
     * @param connection
     * @throws SQLException
     */
    private void updateClaimConfiguration(int applicationId, ClaimConfiguration claimConfiguration,
            int applicationID, Connection connection) throws SQLException {

        int tenantID = CarbonContext.getThreadLocalCarbonContext().getTenantId();

        PreparedStatement storeRoleClaimPrepStmt = null;

        if (claimConfiguration == null) {
            return;
        }

        try {
            // update the application data
            String roleClaim = claimConfiguration.getRoleClaimURI();
            if (roleClaim != null) {
                storeRoleClaimPrepStmt = connection
                        .prepareStatement(ApplicationMgtDBQueries.UPDATE_BASIC_APPINFO_WITH_ROLE_CLAIM);
                // ROLE_CLAIM=? WHERE TENANT_ID= ? AND ID =
                storeRoleClaimPrepStmt.setString(1, roleClaim);
                storeRoleClaimPrepStmt.setInt(2, tenantID);
                storeRoleClaimPrepStmt.setInt(3, applicationId);
                storeRoleClaimPrepStmt.executeUpdate();
            }

        } finally {
            IdentityDatabaseUtil.closeStatement(storeRoleClaimPrepStmt);
        }

        if (claimConfiguration.getClaimMappings() == null
                || claimConfiguration.getClaimMappings().length == 0) {
            return;
        }

        List<ClaimMapping> claimMappings = Arrays.asList(claimConfiguration.getClaimMappings());

        if (claimConfiguration == null || claimMappings.size() < 1) {
            log.debug("No claim mapping found, Skipping ..");
            return;
        }

        PreparedStatement storeClaimMapPrepStmt = null;
        try {
            storeClaimMapPrepStmt = connection
                    .prepareStatement(ApplicationMgtDBQueries.STORE_CLAIM_MAPPING);

            for (ClaimMapping mapping : claimMappings) {
                if (mapping.getLocalClaim() == null
                        || mapping.getLocalClaim().getClaimUri() == null
                        || mapping.getSpClaim().getClaimUri() == null
                        || mapping.getSpClaim() == null) {
                    continue;
                }
                // IDP_CLAIM, SP_CLAIM, APP_ID,IS_REQUESTED, TENANT_ID
                storeClaimMapPrepStmt.setString(1, mapping.getLocalClaim().getClaimUri());
                storeClaimMapPrepStmt.setString(2, mapping.getSpClaim().getClaimUri());
                storeClaimMapPrepStmt.setInt(3, applicationID);
                if (mapping.getSpClaim().isRequested()) {
                    storeClaimMapPrepStmt.setString(4, "true");
                } else {
                    storeClaimMapPrepStmt.setString(4, "false");
                }
                storeClaimMapPrepStmt.setInt(5, tenantID);
                storeClaimMapPrepStmt.addBatch();

                if (debugMode) {
                    log.debug("Storing Claim Mapping. Local Claim: "
                            + mapping.getLocalClaim().getClaimUri() + " SPClaim: "
                            + mapping.getSpClaim().getClaimUri());
                }
            }

            storeClaimMapPrepStmt.executeBatch();
        } finally {
            IdentityDatabaseUtil.closeStatement(storeClaimMapPrepStmt);
        }
    }

    /**
     * 
     * @param applicationID
     * @param permissionsAndRoleConfiguration
     * @param connection
     * @throws SQLException
     */
    private void updatePermissionAndRoleConfiguration(int applicationID,
            PermissionsAndRoleConfiguration permissionsAndRoleConfiguration, Connection connection)
            throws SQLException {

        if (permissionsAndRoleConfiguration == null
                || permissionsAndRoleConfiguration.getRoleMappings() == null
                || permissionsAndRoleConfiguration.getRoleMappings().length == 0) {
            return;
        }

        RoleMapping[] roleMappings = permissionsAndRoleConfiguration.getRoleMappings();
        int tenantID = CarbonContext.getThreadLocalCarbonContext().getTenantId();

        PreparedStatement storeRoleMapPrepStmt = null;
        try {
            storeRoleMapPrepStmt = connection
                    .prepareStatement(ApplicationMgtDBQueries.STORE_ROLE_MAPPING);
            for (RoleMapping roleMapping : roleMappings) {
                // IDP_ROLE, SP_ROLE, APP_ID, TENANT_ID
                storeRoleMapPrepStmt.setString(1, roleMapping.getLocalRole().getLocalRoleName());
                storeRoleMapPrepStmt.setString(2, roleMapping.getRemoteRole());
                storeRoleMapPrepStmt.setInt(3, applicationID);
                storeRoleMapPrepStmt.setInt(4, tenantID);
                storeRoleMapPrepStmt.addBatch();

                if (debugMode) {
                    log.debug("Storing Claim Mapping. IDPRole: " + roleMapping.getLocalRole()
                            + " SPRole: " + roleMapping.getRemoteRole());
                }
            }

            storeRoleMapPrepStmt.executeBatch();
        } finally {
            IdentityDatabaseUtil.closeStatement(storeRoleMapPrepStmt);
        }
    }

    /**
     * 
     */
    public ServiceProvider getApplication(String applicationName) throws IdentityException {

        int applicationId = 0;
        Connection connection = IdentityDatabaseUtil.getDBConnection();

        try {
            // Get the connection

            // Load basic application data
            ServiceProvider serviceProvider = getBasicApplicationData(applicationName, connection);
            applicationId = serviceProvider.getApplicationID();

            if (serviceProvider.getApplicationName() == null
                    && ApplicationConstants.LOCAL_SP.equals(applicationName)) {
                ServiceProvider localServiceProvider = new ServiceProvider();
                localServiceProvider.setApplicationName(applicationName);
                localServiceProvider.setDescription("Local Service Provider");
                createApplication(localServiceProvider);
                serviceProvider = getBasicApplicationData(applicationName, connection);
                applicationId = serviceProvider.getApplicationID();
            }

            serviceProvider.setInboundAuthenticationConfig(getInboundAuthenticationConfig(
                    applicationId, connection));
            serviceProvider
                    .setLocalAndOutBoundAuthenticationConfig(getLocalAndOutboundAuthenticationConfig(
                            applicationId, connection));

            serviceProvider
                    .setInboundProvisioningConfiguration(getInboundProvisioningConfiguration(
                            applicationId, connection));
            serviceProvider
                    .setOutboundProvisioningConfiguration(getOutboundProvisioningConfiguration(
                            applicationId, connection));

            // Load Claim Mapping
            serviceProvider.setClaimConfiguration(getClaimConfiguration(applicationId, connection));

            // Load Role Mappings
            List<RoleMapping> roleMappings = getRoleMappingOfApplication(applicationId, connection);
            PermissionsAndRoleConfiguration permissionAndRoleConfig = new PermissionsAndRoleConfiguration();
            permissionAndRoleConfig.setRoleMappings(roleMappings
                    .toArray(new RoleMapping[roleMappings.size()]));
            serviceProvider.setPermissionAndRoleConfiguration(permissionAndRoleConfig);

            RequestPathAuthenticator[] requestPathAuthenticators = getRequestPathAuthenticators(
                    applicationId, connection);
            serviceProvider.setRequestPathAuthenticators(requestPathAuthenticators);
            return serviceProvider;

        } catch (SQLException e) {
            throw new IdentityException("Failed to update service provider " + applicationId, e);
        } finally {
            IdentityDatabaseUtil.closeConnection(connection);
        }
    }

    /**
     * 
     * @param applicationName
     * @param connection
     * @return
     * @throws SQLException
     */
    private ServiceProvider getBasicApplicationData(String applicationName, Connection connection)
            throws SQLException {

        int tenantID = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        ServiceProvider serviceProvider = new ServiceProvider();

        if (debugMode) {
            log.debug("Loading Basic Application Data of " + applicationName);
        }

        PreparedStatement loadBasicAppInfoStmt = null;
        ResultSet basicAppDataResultSet = null;
        try {
            loadBasicAppInfoStmt = connection
                    .prepareStatement(ApplicationMgtDBQueries.LOAD_BASIC_APP_INFO_BY_APP_NAME);
            // SELECT * FROM IDN_APPMGT_APP WHERE APP_NAME = ? AND TENANT_ID = ?
            loadBasicAppInfoStmt.setString(1, applicationName);
            loadBasicAppInfoStmt.setInt(2, tenantID);
            basicAppDataResultSet = loadBasicAppInfoStmt.executeQuery();

            if (basicAppDataResultSet.next()) {
                serviceProvider.setApplicationID(basicAppDataResultSet.getInt(1));
                serviceProvider.setApplicationName(basicAppDataResultSet.getString(2));
                serviceProvider.setDescription(basicAppDataResultSet.getString(4));

                ServiceProviderOwner owner = new ServiceProviderOwner();
                owner.setUserName(basicAppDataResultSet.getString(3));
                owner.setTenantId(basicAppDataResultSet.getInt(5));
                owner.setUserStoreDomain(basicAppDataResultSet.getString(6));
                serviceProvider.setOwner(owner);

                ClaimConfiguration claimConfig = new ClaimConfiguration();
                claimConfig.setRoleClaimURI(basicAppDataResultSet.getString(7));
                serviceProvider.setClaimConfiguration(claimConfig);

                if (debugMode) {
                    log.debug("ApplicationID: " + serviceProvider.getApplicationID()
                            + " ApplicationName: " + serviceProvider.getApplicationName()
                            + " UserName: " + serviceProvider.getOwner().getUserName()
                            + " TenantID: " + serviceProvider.getOwner().getTenantId());
                }
            }

            return serviceProvider;
        } finally {
            IdentityDatabaseUtil.closeResultSet(basicAppDataResultSet);
            IdentityDatabaseUtil.closeStatement(loadBasicAppInfoStmt);
        }

    }

    /**
     * 
     * @param applicationid
     * @param connection
     * @return
     * @throws SQLException
     */
    private String getAuthenticationType(int applicationid, Connection connection)
            throws SQLException {

        int tenantID = CarbonContext.getThreadLocalCarbonContext().getTenantId();

        PreparedStatement authTypeStmt = null;
        ResultSet authTypeResultSet = null;
        try {
            authTypeStmt = connection
                    .prepareStatement(ApplicationMgtDBQueries.LOAD_AUTH_TYPE_BY_APP_ID);
            authTypeStmt.setInt(1, applicationid);
            authTypeStmt.setInt(2, tenantID);
            authTypeResultSet = authTypeStmt.executeQuery();

            if (authTypeResultSet.next()) {
                return authTypeResultSet.getString(1);
            }

            return ApplicationConstants.AUTH_TYPE_DEFAULT;

        } finally {
            IdentityDatabaseUtil.closeResultSet(authTypeResultSet);
            IdentityDatabaseUtil.closeStatement(authTypeStmt);
        }

    }

    /**
     * This method will be heavily used by the Authentication Framework. The framework would ask for
     * application data with the given client key and secrete
     * 
     * @param clientId
     * @param type
     * @param tenantDomain
     * @return
     * @throws IdentityException
     */
    public ServiceProvider getApplicationData(String clientId, String type, String tenantDomain)
            throws IdentityException {

        if (debugMode) {
            log.debug("Loading Application Data of Client " + clientId);
        }

        int tenantID = -123;

        try {
            tenantID = ApplicationManagementServiceComponentHolder.getRealmService()
                    .getTenantManager().getTenantId(tenantDomain);
        } catch (UserStoreException e1) {
            throw new IdentityException("Error while reading application");
        }

        String applicationName = null;

        // Reading application name from the database
        Connection connection = JDBCPersistenceManager.getInstance().getDBConnection();
        PreparedStatement storeAppPrepStmt = null;
        ResultSet appNameResult = null;
        try {
            storeAppPrepStmt = connection
                    .prepareStatement(ApplicationMgtDBQueries.LOAD_APPLICATION_NAME_BY_CLIENT_ID_AND_TYPE);
            storeAppPrepStmt.setString(1, clientId);
            storeAppPrepStmt.setString(2, type);
            storeAppPrepStmt.setInt(3, tenantID);
            appNameResult = storeAppPrepStmt.executeQuery();
            connection.commit();
            if (appNameResult.next()) {
                applicationName = appNameResult.getString(1);
            }

        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            IdentityDatabaseUtil.closeConnection(connection);
            throw new IdentityException("Error while reading application");
        } finally {
            IdentityDatabaseUtil.closeResultSet(appNameResult);
            IdentityDatabaseUtil.closeStatement(storeAppPrepStmt);
            IdentityDatabaseUtil.closeConnection(connection);
        }

        return getApplication(applicationName);
    }

    /**
     * 
     * @param applicationID
     * @return
     * @throws IdentityException
     */
    public String getApplicationName(int applicationID) throws IdentityException {
        Connection connection = null;
        try {
            connection = IdentityDatabaseUtil.getDBConnection();
            return getApplicationName(applicationID, connection);
        } catch (SQLException e) {
            throw new IdentityException("Failed loading the application with " + applicationID, e);
        } finally {
            IdentityDatabaseUtil.closeConnection(connection);
        }
    }

    /**
     * Reads back the basic application data
     * 
     * @param applicationID
     * @param connection
     * @return
     * 
     * @throws IdentityException
     */
    private String getApplicationName(int applicationID, Connection connection) throws SQLException {

        int tenantID = CarbonContext.getThreadLocalCarbonContext().getTenantId();

        if (debugMode) {
            log.debug("Loading Application Name for ID: " + applicationID);
        }

        PreparedStatement loadBasicAppInfoStmt = null;
        ResultSet appNameResultSet = null;
        String applicationName = null;

        try {
            loadBasicAppInfoStmt = connection
                    .prepareStatement(ApplicationMgtDBQueries.LOAD_APP_NAME_BY_APP_ID);
            loadBasicAppInfoStmt.setInt(1, applicationID);
            loadBasicAppInfoStmt.setInt(2, tenantID);
            appNameResultSet = loadBasicAppInfoStmt.executeQuery();

            if (appNameResultSet.next()) {
                applicationName = appNameResultSet.getString(1);
            }

            if (debugMode) {
                log.debug("ApplicationName : " + applicationName);
            }
            return applicationName;

        } finally {
            IdentityDatabaseUtil.closeResultSet(appNameResultSet);
            IdentityDatabaseUtil.closeStatement(loadBasicAppInfoStmt);
        }
    }

    /**
     * Returns the application ID for a given application name
     * 
     * @param applicationName
     * @param tenantID
     * @param connection
     * @return
     * @throws IdentityException
     */
    private int getApplicationIDByName(String applicationName, int tenantID, Connection connection)
            throws IdentityException {

        int applicationId = 0;
        PreparedStatement getAppIDPrepStmt = null;
        ResultSet appidResult = null;

        try {
            getAppIDPrepStmt = connection
                    .prepareStatement(ApplicationMgtDBQueries.LOAD_APP_ID_BY_APP_NAME);
            getAppIDPrepStmt.setString(1, applicationName);
            getAppIDPrepStmt.setInt(2, tenantID);
            appidResult = getAppIDPrepStmt.executeQuery();

            if (!connection.getAutoCommit()) {
                connection.commit();
            }

            if (appidResult.next()) {
                applicationId = appidResult.getInt(1);
            }

        } catch (SQLException e) {
            IdentityDatabaseUtil.closeConnection(connection);
            throw new IdentityException("Error while storing application");
        } finally {
            IdentityDatabaseUtil.closeResultSet(appidResult);
            IdentityDatabaseUtil.closeStatement(getAppIDPrepStmt);
        }

        return applicationId;
    }

    /**
     * 
     * @param applicationId
     * @param connection
     * @return
     * @throws SQLException
     */
    private InboundAuthenticationConfig getInboundAuthenticationConfig(int applicationId,
            Connection connection) throws SQLException {

        int tenantID = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        ArrayList<InboundAuthenticationRequest> clientConfigList = new ArrayList<InboundAuthenticationRequest>();

        if (debugMode) {
            log.debug("Reading Clients of Application " + applicationId);
        }

        PreparedStatement getClientInfo = null;
        ResultSet resultSet = null;
        try {
            getClientInfo = connection
                    .prepareStatement(ApplicationMgtDBQueries.LOAD_CLIENTS_INFO_BY_APP_ID);
            //
            getClientInfo.setInt(1, applicationId);
            getClientInfo.setInt(2, tenantID);
            resultSet = getClientInfo.executeQuery();

            while (resultSet.next()) {
                InboundAuthenticationRequest inbountAuthRequest = new InboundAuthenticationRequest();
                inbountAuthRequest.setClientID(resultSet.getString(1));
                inbountAuthRequest.setType(resultSet.getString(2));
                clientConfigList.add(inbountAuthRequest);

                if (debugMode) {
                    log.debug("ClientID: " + inbountAuthRequest.getClientID() + " ClientType: "
                            + inbountAuthRequest.getType());
                }
            }

        } finally {
            IdentityDatabaseUtil.closeStatement(getClientInfo);
            IdentityDatabaseUtil.closeResultSet(resultSet);
        }

        InboundAuthenticationConfig inboundAuthenticationConfig = new InboundAuthenticationConfig();
        inboundAuthenticationConfig.setInboundAuthenticationRequests(clientConfigList
                .toArray(new InboundAuthenticationRequest[clientConfigList.size()]));
        return inboundAuthenticationConfig;
    }

    /**
     * 
     * @param applicationId
     * @param connection
     * @return
     * @throws SQLException
     */
    private LocalAndOutboundAuthenticationConfig getLocalAndOutboundAuthenticationConfig(
            int applicationId, Connection connection) throws SQLException {
        PreparedStatement getStepInfoPrepStmt = null;
        ResultSet stepInfoResultSet = null;

        if (debugMode) {
            log.debug("Reading Steps of Application " + applicationId);
        }

        try {
            getStepInfoPrepStmt = connection
                    .prepareStatement(ApplicationMgtDBQueries.LOAD_STEPS_INFO_BY_APP_ID);
            // STEP_ORDER, IDP_NAME, AUTHENTICATOR_NAME
            getStepInfoPrepStmt.setInt(1, applicationId);
            stepInfoResultSet = getStepInfoPrepStmt.executeQuery();

            Map<String, AuthenticationStep> authSteps = new HashMap<String, AuthenticationStep>();
            Map<String, Map<String, List<FederatedAuthenticator>>> stepFedIdPAuthenticators = new HashMap<String, Map<String, List<FederatedAuthenticator>>>();
            Map<String, List<LocalAuthenticator>> stepLocalAuth = new HashMap<String, List<LocalAuthenticator>>();

            while (stepInfoResultSet.next()) {

                String step = String.valueOf(stepInfoResultSet.getInt(1));
                AuthenticationStep authStep;

                if (authSteps.containsKey(step)) {
                    authStep = authSteps.get(step);
                } else {
                    authStep = new AuthenticationStep();
                    authStep.setStepOrder(stepInfoResultSet.getInt(1));
                    stepLocalAuth.put(step, new ArrayList<LocalAuthenticator>());
                    stepFedIdPAuthenticators.put(step,
                            new HashMap<String, List<FederatedAuthenticator>>());
                }

                String idPName = stepInfoResultSet.getString(2);

                if (ApplicationConstants.LOCAL_IDP.equalsIgnoreCase(idPName)) {
                    LocalAuthenticator localAuthenticator = new LocalAuthenticator();
                    localAuthenticator.setName(stepInfoResultSet.getString(3));
                    stepLocalAuth.get(step).add(localAuthenticator);
                } else {
                    Map<String, List<FederatedAuthenticator>> stepFedIdps = stepFedIdPAuthenticators
                            .get(step);

                    if (!stepFedIdps.containsKey(idPName)) {
                        stepFedIdps.put(idPName, new ArrayList<FederatedAuthenticator>());
                    }

                    List<FederatedAuthenticator> idpAuths = stepFedIdps.get(idPName);
                    FederatedAuthenticator fedAuthenticator = new FederatedAuthenticator();
                    fedAuthenticator.setName(stepInfoResultSet.getString(3));
                    idpAuths.add(fedAuthenticator);
                }

                authSteps.put(step, authStep);
            }

            LocalAndOutboundAuthenticationConfig localAndOutboundConfiguration = new LocalAndOutboundAuthenticationConfig();
            AuthenticationStep[] authenticationSteps = new AuthenticationStep[authSteps.size()];

            int authStepCount = 0;

            for (Iterator<Entry<String, AuthenticationStep>> iterator = authSteps.entrySet()
                    .iterator(); iterator.hasNext();) {

                Entry<String, AuthenticationStep> entry = iterator.next();
                AuthenticationStep authStep = entry.getValue();
                String stepId = entry.getKey();

                List<LocalAuthenticator> localAuthenticatorList = stepLocalAuth.get(stepId);

                if (localAuthenticatorList != null && localAuthenticatorList.size() > 0) {
                    authStep.setLocalAuthenticators(localAuthenticatorList
                            .toArray(new LocalAuthenticator[localAuthenticatorList.size()]));
                }

                Map<String, List<FederatedAuthenticator>> idpList = stepFedIdPAuthenticators
                        .get(stepId);

                if (idpList != null && idpList.size() > 0) {
                    FederatedIdentityProvider[] fedIdpList = new FederatedIdentityProvider[idpList
                            .size()];
                    int idpCount = 0;

                    for (Iterator<Entry<String, List<FederatedAuthenticator>>> idpItr = idpList
                            .entrySet().iterator(); idpItr.hasNext();) {
                        Entry<String, List<FederatedAuthenticator>> idpEntry = idpItr.next();
                        String idpName = idpEntry.getKey();
                        List<FederatedAuthenticator> fedAuthenticators = idpEntry.getValue();
                        FederatedIdentityProvider idp = new FederatedIdentityProvider();
                        idp.setIdentityProviderName(idpName);
                        idp.setFederatedAuthenticators(fedAuthenticators
                                .toArray(new FederatedAuthenticator[fedAuthenticators.size()]));
                        idp.setDefaultAuthenticator(idp.getFederatedAuthenticators()[0]);
                        fedIdpList[idpCount++] = idp;
                    }
                    authStep.setFederatedIdentityProviders(fedIdpList);
                }

                authenticationSteps[authStepCount++] = authStep;
            }

            Comparator<AuthenticationStep> comparator = new Comparator<AuthenticationStep>() {
                public int compare(AuthenticationStep step1, AuthenticationStep step2) {
                    return step1.getStepOrder() - step2.getStepOrder();
                }
            };

            Arrays.sort(authenticationSteps, comparator);

            localAndOutboundConfiguration.setAuthenticationSteps(authenticationSteps);

            String authType = getAuthenticationType(applicationId, connection);
            localAndOutboundConfiguration.setAuthenticationType(authType);

            return localAndOutboundConfiguration;
        } finally {
            IdentityDatabaseUtil.closeStatement(getStepInfoPrepStmt);
            IdentityDatabaseUtil.closeResultSet(stepInfoResultSet);
        }
    }

    /**
     * 
     * @param applicationId
     * @param connection
     * @return
     * @throws IdentityException
     */
    private ClaimConfiguration getClaimConfiguration(int applicationId, Connection connection)
            throws IdentityException {

        int tenantID = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        ClaimConfiguration claimConfig = new ClaimConfiguration();
        ArrayList<ClaimMapping> claimMappingList = new ArrayList<ClaimMapping>();

        if (debugMode) {
            log.debug("Reading Claim Mappings of Application " + applicationId);
        }

        PreparedStatement get = null;
        ResultSet resultSet = null;
        try {
            get = connection.prepareStatement(ApplicationMgtDBQueries.LOAD_CLAIM_MAPPING_BY_APP_ID);
            // IDP_CLAIM, SP_CLAIM, IS_REQUESTED
            get.setInt(1, applicationId);
            get.setInt(2, tenantID);
            resultSet = get.executeQuery();

            while (resultSet.next()) {
                ClaimMapping claimMapping = new ClaimMapping();
                Claim localClaim = new Claim();
                Claim spClaim = new Claim();

                localClaim.setClaimUri(resultSet.getString(1));
                spClaim.setClaimUri(resultSet.getString(2));

                String requested = resultSet.getString(3);

                if ("true".equalsIgnoreCase(requested)) {
                    spClaim.setRequested(true);
                } else {
                    spClaim.setRequested(false);
                }

                claimMapping.setLocalClaim(localClaim);
                claimMapping.setSpClaim(spClaim);

                claimMappingList.add(claimMapping);

                if (debugMode) {
                    log.debug("Local Claim: " + claimMapping.getLocalClaim().getClaimUri()
                            + " SPClaim: " + claimMapping.getSpClaim().getClaimUri());
                }
            }

            claimConfig.setClaimMappings(claimMappingList.toArray(new ClaimMapping[claimMappingList
                    .size()]));
        } catch (SQLException e) {
            throw new IdentityException("Error while retrieving all application");
        } finally {
            IdentityDatabaseUtil.closeStatement(get);
            IdentityDatabaseUtil.closeResultSet(resultSet);
        }

        PreparedStatement loadRoleClaim = null;
        ResultSet roleResultSet = null;

        try {
            loadRoleClaim = connection
                    .prepareStatement(ApplicationMgtDBQueries.LOAD_ROLE_CLAIM_BY_APP_ID);
            loadRoleClaim.setInt(1, tenantID);
            loadRoleClaim.setInt(2, applicationId);
            roleResultSet = loadRoleClaim.executeQuery();

            while (roleResultSet.next()) {
                claimConfig.setRoleClaimURI(roleResultSet.getString(1));
            }
        } catch (SQLException e) {
            throw new IdentityException("Error while retrieving all application");
        } finally {
            IdentityDatabaseUtil.closeStatement(loadRoleClaim);
            IdentityDatabaseUtil.closeResultSet(roleResultSet);
        }

        return claimConfig;
    }

    /**
     * 
     * @param applicationId
     * @param connection
     * @return
     * @throws IdentityException
     */
    private RequestPathAuthenticator[] getRequestPathAuthenticators(int applicationId,
            Connection connection) throws IdentityException {

        int tenantID = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        PreparedStatement loadReqPathAuthenticators = null;
        ResultSet authResultSet = null;
        List<RequestPathAuthenticator> authenticators = new ArrayList<RequestPathAuthenticator>();

        try {
            loadReqPathAuthenticators = connection
                    .prepareStatement(ApplicationMgtDBQueries.LOAD_REQ_PATH_AUTHENTICATORS_BY_APP_ID);
            loadReqPathAuthenticators.setInt(1, applicationId);
            loadReqPathAuthenticators.setInt(2, tenantID);
            authResultSet = loadReqPathAuthenticators.executeQuery();

            while (authResultSet.next()) {
                RequestPathAuthenticator reqAuth = new RequestPathAuthenticator();
                reqAuth.setName(authResultSet.getString(1));
                authenticators.add(reqAuth);
            }
        } catch (SQLException e) {
            throw new IdentityException("Error while retrieving all application");
        } finally {
            IdentityDatabaseUtil.closeStatement(loadReqPathAuthenticators);
            IdentityDatabaseUtil.closeResultSet(authResultSet);
        }

        return authenticators.toArray(new RequestPathAuthenticator[authenticators.size()]);
    }

    /**
     * 
     * @param applicationId
     * @param authenticators
     * @param connection
     * @throws IdentityException
     */
    private void updateRequestPathAuthenticators(int applicationId,
            RequestPathAuthenticator[] authenticators, Connection connection)
            throws IdentityException {

        int tenantID = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        PreparedStatement storeReqPathAuthenticators = null;

        try {
            storeReqPathAuthenticators = connection
                    .prepareStatement(ApplicationMgtDBQueries.STORE_REQ_PATH_AUTHENTICATORS);
            if (authenticators != null && authenticators.length > 0) {
                for (RequestPathAuthenticator auth : authenticators) {
                    storeReqPathAuthenticators.setString(1, auth.getName());
                    storeReqPathAuthenticators.setInt(2, applicationId);
                    storeReqPathAuthenticators.setInt(3, tenantID);
                    storeReqPathAuthenticators.addBatch();
                }
                storeReqPathAuthenticators.executeBatch();
            }
        } catch (SQLException e) {
            throw new IdentityException("Error while retrieving all application");
        } finally {
            IdentityDatabaseUtil.closeStatement(storeReqPathAuthenticators);
        }
    }

    /**
     * 
     * @param applicationID
     * @param connection
     * @throws SQLException
     */
    private void deleteRequestPathAuthenticators(int applicationID, Connection connection)
            throws SQLException {

        if (debugMode) {
            log.debug("Deleting request path authenticators " + applicationID);
        }

        int tenantID = CarbonContext.getThreadLocalCarbonContext().getTenantId();

        PreparedStatement deleteReqAuthPrepStmt = null;
        try {
            deleteReqAuthPrepStmt = connection
                    .prepareStatement(ApplicationMgtDBQueries.REMOVE_REQ_PATH_AUTHENTICATOR);
            deleteReqAuthPrepStmt.setInt(1, applicationID);
            deleteReqAuthPrepStmt.setInt(2, tenantID);
            deleteReqAuthPrepStmt.execute();

        } finally {
            IdentityDatabaseUtil.closeStatement(deleteReqAuthPrepStmt);
        }
    }

    /**
     * Reads the claim mappings for a given appID
     * 
     * @param applicationId
     * @param connection
     * @return
     * @throws IdentityException
     */
    private List<RoleMapping> getRoleMappingOfApplication(int applicationId, Connection connection)
            throws IdentityException {

        int tenantID = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        ArrayList<RoleMapping> roleMappingList = new ArrayList<RoleMapping>();

        if (debugMode) {
            log.debug("Reading Role Mapping of Application " + applicationId);
        }

        PreparedStatement getClientInfo = null;
        ResultSet resultSet = null;
        try {
            getClientInfo = connection
                    .prepareStatement(ApplicationMgtDBQueries.LOAD_ROLE_MAPPING_BY_APP_ID);
            // IDP_ROLE, SP_ROLE
            getClientInfo.setInt(1, applicationId);
            getClientInfo.setInt(2, tenantID);
            resultSet = getClientInfo.executeQuery();

            while (resultSet.next()) {
                RoleMapping roleMapping = new RoleMapping();
                LocalRole localRole = new LocalRole();
                localRole.setLocalRoleName(resultSet.getString(1));
                roleMapping.setLocalRole(localRole);
                roleMapping.setRemoteRole(resultSet.getString(2));
                roleMappingList.add(roleMapping);

                if (debugMode) {
                    log.debug("Local Role: " + roleMapping.getLocalRole().getLocalRoleName()
                            + " SPRole: " + roleMapping.getRemoteRole());
                }
            }

        } catch (SQLException e) {
            throw new IdentityException("Error while retrieving all application");
        } finally {
            IdentityDatabaseUtil.closeStatement(getClientInfo);
            IdentityDatabaseUtil.closeResultSet(resultSet);
        }
        return roleMappingList;
    }

    /**
     * Get application Names for user
     * 
     * @return
     * @throws IdentityException
     */
    public ApplicationBasicInfo[] getAllApplicationBasicInfo() throws IdentityException {

        int tenantID = CarbonContext.getThreadLocalCarbonContext().getTenantId();

        if (debugMode) {
            log.debug("Reading all Applications of Tenant " + tenantID);
        }

        Connection connection = null;
        PreparedStatement getAppNamesStmt = null;
        ResultSet appNameResultSet = null;

        ArrayList<ApplicationBasicInfo> appInfo = new ArrayList<ApplicationBasicInfo>();

        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            getAppNamesStmt = connection
                    .prepareStatement(ApplicationMgtDBQueries.LOAD_APP_NAMES_BY_TENANT);
            getAppNamesStmt.setInt(1, tenantID);
            appNameResultSet = getAppNamesStmt.executeQuery();

            while (appNameResultSet.next()) {
                ApplicationBasicInfo basicInfo = new ApplicationBasicInfo();
                if (ApplicationConstants.LOCAL_SP.equals(appNameResultSet.getString(1))) {
                    continue;
                }
                basicInfo.setApplicationName(appNameResultSet.getString(1));
                basicInfo.setDescription(appNameResultSet.getString(2));

                if (ApplicationMgtUtil.isUserAuthorized(basicInfo.getApplicationName())) {
                    appInfo.add(basicInfo);
                    if (debugMode) {
                        log.debug("Application Name:" + basicInfo.getApplicationName());
                    }
                }
            }
        } catch (SQLException e) {
            throw new IdentityException("Error while Reading all Applications");
        } finally {
            IdentityDatabaseUtil.closeStatement(getAppNamesStmt);
            IdentityDatabaseUtil.closeResultSet(appNameResultSet);
            IdentityDatabaseUtil.closeConnection(connection);
        }

        return appInfo.toArray(new ApplicationBasicInfo[appInfo.size()]);
    }

    /**
     * Deletes the application from IDN_APPMGT_APP table. Cascade deletes with foreign key
     * constraints should delete the corresponding entries from the tables
     * 
     * @param appName
     * @throws IdentityException
     */
    public void deleteApplication(String appName) throws IdentityException {

        int tenantID = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        Connection connection = JDBCPersistenceManager.getInstance().getDBConnection();

        if (debugMode) {
            log.debug("Deleting Application " + appName);
        }

        // Now, delete the application
        PreparedStatement deleteClientPrepStmt = null;
        try {

            // First, delete all the clients of the application
            int applicationID = getApplicationIDByName(appName, tenantID, connection);
            InboundAuthenticationConfig clients = getInboundAuthenticationConfig(applicationID,
                    connection);
            for (InboundAuthenticationRequest client : clients.getInboundAuthenticationRequests()) {
                deleteClient(client.getClientID(), client.getType());
            }

            deleteClientPrepStmt = connection
                    .prepareStatement(ApplicationMgtDBQueries.REMOVE_APP_FROM_APPMGT_APP);
            deleteClientPrepStmt.setString(1, appName);
            deleteClientPrepStmt.setInt(2, tenantID);
            deleteClientPrepStmt.execute();

            if (!connection.getAutoCommit()) {
                connection.commit();
            }

        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new IdentityException("Error deleting application");
        } finally {
            IdentityDatabaseUtil.closeStatement(deleteClientPrepStmt);
            IdentityDatabaseUtil.closeConnection(connection);
        }
    }

    /**
     * Deletes the Application with application ID
     * 
     * @param applicationID
     * @param connection
     * @throws IdentityException
     */
    public void deleteApplication(int applicationID, Connection connection)
            throws IdentityException {

        int tenantID = CarbonContext.getThreadLocalCarbonContext().getTenantId();

        if (debugMode) {
            log.debug("Deleting Application " + applicationID);
        }

        // Now, delete the application
        PreparedStatement deleteClientPrepStmt = null;
        try {

            // delete clients
            InboundAuthenticationConfig clients = getInboundAuthenticationConfig(applicationID,
                    connection);
            for (InboundAuthenticationRequest client : clients.getInboundAuthenticationRequests()) {
                deleteClient(client.getClientID(), client.getType());
            }

            String applicationName = getApplicationName(applicationID, connection);
            // delete roles
            ApplicationMgtUtil.deleteAppRole(applicationName);

            deleteClientPrepStmt = connection
                    .prepareStatement(ApplicationMgtDBQueries.REMOVE_APP_FROM_APPMGT_APP_WITH_ID);
            deleteClientPrepStmt.setInt(1, applicationID);
            deleteClientPrepStmt.setInt(2, tenantID);
            deleteClientPrepStmt.execute();

            if (!connection.getAutoCommit()) {
                connection.commit();
            }

        } catch (SQLException e) {
            IdentityDatabaseUtil.closeConnection(connection);
            log.error(e.getMessage(), e);
            throw new IdentityException("Error deleting application");

        } finally {
            IdentityDatabaseUtil.closeStatement(deleteClientPrepStmt);
        }

    }

    /**
     * Deleting Clients of the Application
     * 
     * @param applicationID
     * @param connection
     * @throws IdentityException
     */
    private void deleteInboundAuthRequestConfiguration(int applicationID, Connection connection)
            throws SQLException {

        if (debugMode) {
            log.debug("Deleting Clients of the Application " + applicationID);
        }

        int tenantID = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        PreparedStatement deleteClientPrepStmt = null;

        try {
            deleteClientPrepStmt = connection
                    .prepareStatement(ApplicationMgtDBQueries.REMOVE_CLIENT_FROM_APPMGT_CLIENT);
            // APP_ID = ? AND TENANT_ID = ?
            deleteClientPrepStmt.setInt(1, applicationID);
            deleteClientPrepStmt.setInt(2, tenantID);
            deleteClientPrepStmt.execute();
        } finally {
            IdentityDatabaseUtil.closeStatement(deleteClientPrepStmt);
        }
    }

    /**
     * 
     * @param applicationId
     * @param connection
     * @throws SQLException
     */
    private void deleteLocalAndOutboundAuthenticationConfiguration(int applicationId,
            Connection connection) throws SQLException {

        if (debugMode) {
            log.debug("Deleting Steps of Application " + applicationId);
        }

        PreparedStatement deleteLocalAndOutboundAuthConfigPrepStmt = null;
        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();

        try {
            deleteLocalAndOutboundAuthConfigPrepStmt = connection
                    .prepareStatement(ApplicationMgtDBQueries.REMOVE_STEP_FROM_APPMGT_STEP);
            deleteLocalAndOutboundAuthConfigPrepStmt.setInt(1, applicationId);
            deleteLocalAndOutboundAuthConfigPrepStmt.setInt(2, tenantId);
            deleteLocalAndOutboundAuthConfigPrepStmt.execute();

        } finally {
            IdentityDatabaseUtil.closeStatement(deleteLocalAndOutboundAuthConfigPrepStmt);
        }
    }

    /**
     * 
     * @param applicationId
     * @param connection
     * @throws SQLException
     */
    private void deleteOutboundProvisioningConfiguration(int applicationId, Connection connection)
            throws SQLException {

        if (debugMode) {
            log.debug("Deleting Steps of Application " + applicationId);
        }

        PreparedStatement deleteOutboundProConfigPrepStmt = null;
        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();

        try {
            deleteOutboundProConfigPrepStmt = connection
                    .prepareStatement(ApplicationMgtDBQueries.REMOVE_PRO_CONNECTORS);
            deleteOutboundProConfigPrepStmt.setInt(1, applicationId);
            deleteOutboundProConfigPrepStmt.setInt(2, tenantId);
            deleteOutboundProConfigPrepStmt.execute();

        } finally {
            IdentityDatabaseUtil.closeStatement(deleteOutboundProConfigPrepStmt);
        }
    }

    /**
     * Deletes clients using the service stubs
     * 
     * @param clientID
     * @param type
     * @throws IdentityException
     */
    private void deleteClient(String clientIdentifier, String type) throws IdentityException {
        if ("samlsso".equalsIgnoreCase(type)) {
            new SAMLApplicationDAOImpl().removeServiceProviderConfiguration(clientIdentifier);
            ;
        } else if ("oauth2".equalsIgnoreCase(type)) {
            new OAuthApplicationDAOImp().removeOAuthApplication(clientIdentifier);
        }
    }

    /**
     * Delete Claim Mapping of the Application
     * 
     * @param applicationID
     * @param connection
     * @throws IdentityException
     */
    private void deteClaimConfiguration(int applicationID, Connection connection)
            throws SQLException {

        if (debugMode) {
            log.debug("Deleting Application Claim Mapping " + applicationID);
        }

        int tenantID = CarbonContext.getThreadLocalCarbonContext().getTenantId();

        PreparedStatement deleteCliamPrepStmt = null;
        try {
            deleteCliamPrepStmt = connection
                    .prepareStatement(ApplicationMgtDBQueries.REMOVE_CLAIM_MAPPINGS_FROM_APPMGT_CLAIM_MAPPING);
            deleteCliamPrepStmt.setInt(1, applicationID);
            deleteCliamPrepStmt.setInt(2, tenantID);
            deleteCliamPrepStmt.execute();

        } finally {
            IdentityDatabaseUtil.closeStatement(deleteCliamPrepStmt);
        }
    }

    /**
     * 
     * @param applicationID
     * @param connection
     * @throws IdentityException
     */
    public void deletePermissionAndRoleConfiguration(int applicationID, Connection connection)
            throws SQLException {

        int tenantID = CarbonContext.getThreadLocalCarbonContext().getTenantId();

        if (debugMode) {
            log.debug("Deleting Role Mapping of Application " + applicationID);
        }

        PreparedStatement deleteRoleMappingPrepStmt = null;
        try {
            deleteRoleMappingPrepStmt = connection
                    .prepareStatement(ApplicationMgtDBQueries.REMOVE_ROLE_MAPPINGS_FROM_APPMGT_ROLE_MAPPING);
            deleteRoleMappingPrepStmt.setInt(1, applicationID);
            deleteRoleMappingPrepStmt.setInt(2, tenantID);
            deleteRoleMappingPrepStmt.execute();
        } finally {
            IdentityDatabaseUtil.closeStatement(deleteRoleMappingPrepStmt);
        }
    }

    public String getServiceProviderNameByClientId(String clientId, String clientType,
            String tenantDomain) throws IdentityException {
        int tenantID = -123;

        if (tenantDomain != null) {
            try {
                tenantID = ApplicationManagementServiceComponentHolder.getRealmService()
                        .getTenantManager().getTenantId(tenantDomain);
            } catch (UserStoreException e1) {
                throw new IdentityException("Error while reading application");
            }
        }

        String applicationName = null;

        // Reading application name from the database
        Connection connection = JDBCPersistenceManager.getInstance().getDBConnection();
        PreparedStatement storeAppPrepStmt = null;
        ResultSet appNameResult = null;
        try {
            storeAppPrepStmt = connection
                    .prepareStatement(ApplicationMgtDBQueries.LOAD_APPLICATION_NAME_BY_CLIENT_ID_AND_TYPE);
            storeAppPrepStmt.setString(1, clientId);
            storeAppPrepStmt.setString(2, clientType);
            storeAppPrepStmt.setInt(3, tenantID);
            storeAppPrepStmt.setInt(4, tenantID);
            appNameResult = storeAppPrepStmt.executeQuery();
            if (appNameResult.next()) {
                applicationName = appNameResult.getString(1);
            }
        } catch (SQLException e) {
            throw new IdentityException("Error while reading application");
        } finally {
            IdentityDatabaseUtil.closeResultSet(appNameResult);
            IdentityDatabaseUtil.closeStatement(storeAppPrepStmt);
            IdentityDatabaseUtil.closeConnection(connection);
        }

        return applicationName;
    }

    /**
     * 
     * @param serviceProviderName
     * @param tenantDomain
     * @param localIdpAsKey
     * @return
     * @throws SQLException
     * @throws IdentityException
     */
    private Map<String, String> getClaimMapping(String serviceProviderName, String tenantDomain,
            boolean localIdpAsKey) throws SQLException, IdentityException {

        int tenantID = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        Map<String, String> claimMapping = new HashMap<String, String>();

        if (debugMode) {
            log.debug("Reading Claim Mappings of Application " + serviceProviderName);
        }

        PreparedStatement getClaimPreStmt = null;
        ResultSet resultSet = null;
        Connection connection = null;
        try {

            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            getClaimPreStmt = connection
                    .prepareStatement(ApplicationMgtDBQueries.LOAD_CLAIM_MAPPING_BY_APP_NAME);
            // IDP_CLAIM, SP_CLAIM, IS_REQUESTED
            getClaimPreStmt.setString(1, serviceProviderName);
            getClaimPreStmt.setInt(2, tenantID);
            resultSet = getClaimPreStmt.executeQuery();

            while (resultSet.next()) {
                if (localIdpAsKey) {
                    claimMapping.put(resultSet.getString(1), resultSet.getString(2));
                } else {
                    claimMapping.put(resultSet.getString(2), resultSet.getString(1));
                }
            }

        } finally {
            IdentityDatabaseUtil.closeStatement(getClaimPreStmt);
            IdentityDatabaseUtil.closeResultSet(resultSet);
            IdentityDatabaseUtil.closeConnection(connection);
        }

        return claimMapping;
    }

    @Override
    public Map<String, String> getServiceProviderToLocalIdPClaimMapping(String serviceProviderName,
            String tenantDomain) throws IdentityException {
        try {
            return getClaimMapping(serviceProviderName, tenantDomain, false);
        } catch (SQLException e) {
            throw new IdentityException("Error while retrieving claim mapping", e);
        }
    }

    @Override
    public Map<String, String> getLocalIdPToServiceProviderClaimMapping(String serviceProviderName,
            String tenantDomain) throws IdentityException {
        try {
            return getClaimMapping(serviceProviderName, tenantDomain, true);
        } catch (SQLException e) {
            throw new IdentityException("Error while retrieving claim mapping", e);
        }
    }

    @Override
    public List<String> getAllRequestedClaimsByServiceProvider(String serviceProviderName,
            String tenantDomain) throws IdentityException {
        int tenantID = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        List<String> reqClaimUris = new ArrayList<String>();

        if (debugMode) {
            log.debug("Reading Claim Mappings of Application " + serviceProviderName);
        }

        PreparedStatement getClaimPreStmt = null;
        ResultSet resultSet = null;
        Connection connection = null;
        try {

            connection = JDBCPersistenceManager.getInstance().getDBConnection();

            getClaimPreStmt = connection
                    .prepareStatement(ApplicationMgtDBQueries.LOAD_CLAIM_MAPPING_BY_APP_NAME);

            // IDP_CLAIM, SP_CLAIM, IS_REQUESTED
            getClaimPreStmt.setString(1, serviceProviderName);
            getClaimPreStmt.setInt(2, tenantID);
            resultSet = getClaimPreStmt.executeQuery();

            while (resultSet.next()) {
                if ("true".equalsIgnoreCase(resultSet.getString(3))) {
                    reqClaimUris.add(resultSet.getString(1));
                }
            }
        } catch (SQLException e) {
            throw new IdentityException("Error while retreiving requested claims", e);
        } finally {
            IdentityDatabaseUtil.closeStatement(getClaimPreStmt);
            IdentityDatabaseUtil.closeResultSet(resultSet);
            IdentityDatabaseUtil.closeConnection(connection);
        }
        return reqClaimUris;

    }

}
