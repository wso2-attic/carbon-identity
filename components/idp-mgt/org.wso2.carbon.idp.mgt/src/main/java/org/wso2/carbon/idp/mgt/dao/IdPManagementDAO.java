/*
 * Copyright (c) 2014 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.idp.mgt.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.CertData;
import org.wso2.carbon.identity.application.common.model.Claim;
import org.wso2.carbon.identity.application.common.model.ClaimConfig;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.FederatedAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.identity.application.common.model.JustInTimeProvisioningConfig;
import org.wso2.carbon.identity.application.common.model.LocalRole;
import org.wso2.carbon.identity.application.common.model.PermissionsAndRoleConfig;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.application.common.model.ProvisioningConnectorConfig;
import org.wso2.carbon.identity.application.common.model.RoleMapping;
import org.wso2.carbon.identity.application.common.util.CharacterEncoder;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationManagementUtil;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.idp.mgt.util.IdPManagementConstants;
import org.wso2.carbon.utils.DBUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.cert.CertificateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IdPManagementDAO {

    private static final Log log = LogFactory.getLog(IdPManagementDAO.class);

    /**
     * @param dbConnection
     * @param tenantId
     * @param tenantDomain
     * @return
     * @throws IdentityApplicationManagementException
     */
    public List<IdentityProvider> getIdPs(Connection dbConnection, int tenantId, String tenantDomain)
            throws IdentityApplicationManagementException {

        boolean dbConnInitialized = true;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        List<IdentityProvider> idps = new ArrayList<IdentityProvider>();
        try {
            if (dbConnection == null) {
                dbConnection = IdentityDatabaseUtil.getDBConnection();
            } else {
                dbConnInitialized = false;
            }
            String sqlStmt = IdPManagementConstants.SQLQueries.GET_IDPS_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, tenantId);
            prepStmt.setInt(2, MultitenantConstants.SUPER_TENANT_ID);
            rs = prepStmt.executeQuery();
            while (rs.next()) {
                IdentityProvider identityProvider = new IdentityProvider();
                identityProvider.setIdentityProviderName(rs.getString(1));
                if (("1").equals(rs.getString("IS_PRIMARY"))) {
                    identityProvider.setPrimary(true);
                } else {
                    identityProvider.setPrimary(false);
                }
                identityProvider.setHomeRealmId(rs.getString("HOME_REALM_ID"));
                identityProvider.setIdentityProviderDescription(rs.getString("DESCRIPTION"));

                // IS_FEDERATION_HUB_IDP
                if ("1".equals(rs.getString("IS_FEDERATION_HUB"))) {
                    identityProvider.setFederationHub(false);
                }

                // IS_LOCAL_CLAIM_DIALECT
                if ("1".equals(rs.getString("IS_LOCAL_CLAIM_DIALECT"))) {
                    if (identityProvider.getClaimConfig() == null) {
                        identityProvider.setClaimConfig(new ClaimConfig());
                    }
                    identityProvider.getClaimConfig().setLocalClaimDialect(true);
                }

                // IS_ENABLE
                if ("1".equals(rs.getString("IS_ENABLED"))) {
                    identityProvider.setEnable(true);
                } else {
                    identityProvider.setEnable(false);
                }

                identityProvider.setDisplayName(rs.getString("DISPLAY_NAME"));

                if (!IdentityApplicationConstants.RESIDENT_IDP_RESERVED_NAME
                        .equals(identityProvider.getIdentityProviderName())) {
                    idps.add(identityProvider);
                }
            }
            dbConnection.commit();
            return idps;
        } catch (SQLException | IdentityException e) {
            log.error(e.getMessage(), e);
            IdentityApplicationManagementUtil.rollBack(dbConnection);
            String msg = "Error occurred while retrieving registered Identity Provider Entity IDs "
                    + "for tenant " + tenantDomain;
            throw new IdentityApplicationManagementException(msg);
        } finally {
            if (dbConnInitialized) {
                IdentityApplicationManagementUtil.closeStatement(prepStmt);
                IdentityApplicationManagementUtil.closeResultSet(rs);
                IdentityApplicationManagementUtil.closeConnection(dbConnection);
            }
        }
    }

    /**
     * @param dbConnection
     * @param idPName
     * @param tenantId
     * @return
     * @throws IdentityApplicationManagementException
     * @throws SQLException
     */
    private FederatedAuthenticatorConfig[] getFederatedAuthenticatorConfigs(
            Connection dbConnection, String idPName, IdentityProvider federatedIdp, int tenantId)
            throws IdentityApplicationManagementException, SQLException {

        int idPId = getIdentityProviderIdentifier(dbConnection, idPName, tenantId);

        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        ResultSet proprs = null;
        String defaultAuthName = null;

        if (federatedIdp != null && federatedIdp.getDefaultAuthenticatorConfig() != null) {
            defaultAuthName = federatedIdp.getDefaultAuthenticatorConfig().getName();
        }

        String sqlStmt = IdPManagementConstants.SQLQueries.GET_ALL_IDP_AUTH_SQL;
        Set<FederatedAuthenticatorConfig> federatedAuthenticatorConfigs = new HashSet<FederatedAuthenticatorConfig>();
        try {
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, idPId);
            rs = prepStmt.executeQuery();

            while (rs.next()) {
                FederatedAuthenticatorConfig authnConfig = new FederatedAuthenticatorConfig();
                int authnId = rs.getInt("ID");
                authnConfig.setName(rs.getString("NAME"));

                if (("1").equals(rs.getString("IS_ENABLED"))) {
                    authnConfig.setEnabled(true);
                } else {
                    authnConfig.setEnabled(false);
                }

                authnConfig.setDisplayName(rs.getString("DISPLAY_NAME"));

                if (defaultAuthName != null && authnConfig.getName().equals(defaultAuthName)) {
                    federatedIdp.getDefaultAuthenticatorConfig().setDisplayName(authnConfig.getDisplayName());
                }

                sqlStmt = IdPManagementConstants.SQLQueries.GET_IDP_AUTH_PROPS_SQL;
                prepStmt.clearParameters();
                prepStmt = dbConnection.prepareStatement(sqlStmt);
                prepStmt.setInt(1, authnId);
                proprs = prepStmt.executeQuery();
                Set<Property> properties = new HashSet<Property>();
                while (proprs.next()) {
                    Property property = new Property();
                    property.setName(proprs.getString("PROPERTY_KEY"));
                    property.setValue(proprs.getString("PROPERTY_VALUE"));
                    if (("1").equals(proprs.getString("IS_SECRET"))) {
                        property.setConfidential(true);
                    }
                    properties.add(property);
                }

                IdentityApplicationManagementUtil.closeResultSet(proprs);
                authnConfig.setProperties(properties.toArray(new Property[properties.size()]));
                federatedAuthenticatorConfigs.add(authnConfig);
            }

            return federatedAuthenticatorConfigs
                    .toArray(new FederatedAuthenticatorConfig[federatedAuthenticatorConfigs.size()]);
        } finally {
            IdentityApplicationManagementUtil.closeResultSet(rs);
            IdentityApplicationManagementUtil.closeResultSet(proprs);
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
        }
    }

    /**
     * @param newFederatedAuthenticatorConfigs
     * @param oldFederatedAuthenticatorConfigs
     * @param dbConnection
     * @param idpId
     * @param tenantId
     * @throws IdentityApplicationManagementException
     * @throws SQLException
     */
    private void updateFederatedAuthenticatorConfigs(
            FederatedAuthenticatorConfig[] newFederatedAuthenticatorConfigs,
            FederatedAuthenticatorConfig[] oldFederatedAuthenticatorConfigs,
            Connection dbConnection, int idpId, int tenantId)
            throws IdentityApplicationManagementException, SQLException {

        Map<String, FederatedAuthenticatorConfig> oldFedAuthnConfigMap = new HashMap<String, FederatedAuthenticatorConfig>();
        if (oldFederatedAuthenticatorConfigs != null && oldFederatedAuthenticatorConfigs.length > 0) {
            for (FederatedAuthenticatorConfig fedAuthnConfig : oldFederatedAuthenticatorConfigs) {
                oldFedAuthnConfigMap.put(fedAuthnConfig.getName(), fedAuthnConfig);
            }
        }

        if (newFederatedAuthenticatorConfigs != null && newFederatedAuthenticatorConfigs.length > 0) {
            for (FederatedAuthenticatorConfig fedAuthenticator : newFederatedAuthenticatorConfigs) {
                if (oldFedAuthnConfigMap.containsKey(fedAuthenticator.getName())
                        && oldFedAuthnConfigMap.get(fedAuthenticator.getName()).isValid()) {
                    // we already have an openidconnect authenticator in the system - update it.
                    updateFederatedAuthenticatorConfig(fedAuthenticator, dbConnection, idpId,
                            tenantId);
                } else {
                    addFederatedAuthenticatorConfig(fedAuthenticator, dbConnection, idpId, tenantId);
                }
            }
        }
    }

    /**
     * @param federatedAuthenticatorConfig
     * @param dbConnection
     * @param idpId
     * @throws IdentityApplicationManagementException
     * @throws SQLException
     */
    private void updateFederatedAuthenticatorConfig(
            FederatedAuthenticatorConfig federatedAuthenticatorConfig, Connection dbConnection,
            int idpId, int tenantId) throws IdentityApplicationManagementException, SQLException {

        PreparedStatement prepStmt = null;
        ResultSet rs = null;

        try {
            String sqlStmt = IdPManagementConstants.SQLQueries.UPDATE_IDP_AUTH_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);

            if (federatedAuthenticatorConfig.isEnabled()) {
                prepStmt.setString(1, "1");
            } else {
                prepStmt.setString(1, "0");
            }
            prepStmt.setInt(2, idpId);
            prepStmt.setString(3, CharacterEncoder.getSafeText(federatedAuthenticatorConfig.getName()));
            prepStmt.executeUpdate();

            int authnId = getAuthenticatorIdentifier(dbConnection, idpId,
                    federatedAuthenticatorConfig.getName());

            for (Property property : federatedAuthenticatorConfig.getProperties()) {

                sqlStmt = IdPManagementConstants.SQLQueries.UPDATE_IDP_AUTH_PROP_SQL;
                prepStmt = dbConnection.prepareStatement(sqlStmt);
                prepStmt.setString(1, CharacterEncoder.getSafeText(property.getValue()));
                if (property.isConfidential()) {
                    prepStmt.setString(2, "1");
                } else {
                    prepStmt.setString(2, "0");
                }
                prepStmt.setInt(3, authnId);
                prepStmt.setString(4, CharacterEncoder.getSafeText(property.getName()));
                int rows = prepStmt.executeUpdate();

                if (rows == 0) {
                    // this should be an insert.
                    prepStmt.clearParameters();
                    sqlStmt = IdPManagementConstants.SQLQueries.ADD_IDP_AUTH_PROP_SQL;
                    prepStmt = dbConnection.prepareStatement(sqlStmt);
                    prepStmt.setInt(1, authnId);
                    prepStmt.setInt(2, tenantId);
                    prepStmt.setString(3, CharacterEncoder.getSafeText(property.getName()));
                    prepStmt.setString(4, CharacterEncoder.getSafeText(property.getValue()));
                    if (property.isConfidential()) {
                        prepStmt.setString(5, "1");
                    } else {
                        prepStmt.setString(5, "0");
                    }

                    prepStmt.executeUpdate();
                }

            }
        } finally {
            IdentityApplicationManagementUtil.closeResultSet(rs);
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
        }
    }

    /**
     * @param authnConfigs
     * @param dbConnection
     * @param idpId
     * @throws IdentityApplicationManagementException
     * @throws SQLException
     */
    public void addFederatedAuthenticatorConfigs(FederatedAuthenticatorConfig[] authnConfigs,
                                                 Connection dbConnection, int idpId, int tenantId)
            throws IdentityApplicationManagementException, SQLException {

        for (FederatedAuthenticatorConfig authnConfig : authnConfigs) {
            addFederatedAuthenticatorConfig(authnConfig, dbConnection, idpId, tenantId);
        }
    }

    public void addFederatedAuthenticatorConfig(FederatedAuthenticatorConfig authnConfig,
                                                Connection dbConnection, int idpId, int tenantId)
            throws IdentityApplicationManagementException, SQLException {

        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        String sqlStmt = IdPManagementConstants.SQLQueries.ADD_IDP_AUTH_SQL;

        try {
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, idpId);
            prepStmt.setInt(2, tenantId);
            if (authnConfig.isEnabled()) {
                prepStmt.setString(3, "1");
            } else {
                prepStmt.setString(3, "0");
            }
            prepStmt.setString(4, CharacterEncoder.getSafeText(authnConfig.getName()));
            prepStmt.setString(5, CharacterEncoder.getSafeText(authnConfig.getDisplayName()));
            prepStmt.execute();

            int authnId = getAuthenticatorIdentifier(dbConnection, idpId, authnConfig.getName());

            sqlStmt = IdPManagementConstants.SQLQueries.ADD_IDP_AUTH_PROP_SQL;

            if (authnConfig.getProperties() == null) {
                authnConfig.setProperties(new Property[0]);
            }
            for (Property property : authnConfig.getProperties()) {

                prepStmt = dbConnection.prepareStatement(sqlStmt);
                prepStmt.setInt(1, authnId);
                prepStmt.setInt(2, tenantId);
                prepStmt.setString(3, CharacterEncoder.getSafeText(property.getName()));
                prepStmt.setString(4, CharacterEncoder.getSafeText(property.getValue()));
                if (property.isConfidential()) {
                    prepStmt.setString(5, "1");
                } else {
                    prepStmt.setString(5, "0");
                }
                prepStmt.executeUpdate();
            }
        } finally {
            IdentityApplicationManagementUtil.closeResultSet(rs);
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
        }
    }

    /**
     * @param dbConnection
     * @param idPName
     * @param userClaimUri
     * @param roleClaimUri
     * @param idpId
     * @param tenantId
     * @return
     * @throws SQLException
     */
    private ClaimConfig getLocalIdPDefaultClaimValues(Connection dbConnection, String idPName,
                                                      String userClaimUri, String roleClaimUri,
                                                      int idpId, int tenantId) throws SQLException {

        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        String sqlStmt;
        ClaimConfig claimConfig = new ClaimConfig();

        try {

            claimConfig.setLocalClaimDialect(true);
            claimConfig.setRoleClaimURI(roleClaimUri);
            claimConfig.setUserClaimURI(userClaimUri);

            sqlStmt = IdPManagementConstants.SQLQueries.GET_LOCAL_IDP_DEFAULT_CLAIM_VALUES_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);

            prepStmt.setInt(1, idpId);
            prepStmt.setInt(2, tenantId);

            List<ClaimMapping> claimMappings = new ArrayList<ClaimMapping>();

            rs = prepStmt.executeQuery();

            while (rs.next()) {
                ClaimMapping claimMapping = new ClaimMapping();

                // empty claim.
                Claim remoteClaim = new Claim();

                Claim localClaim = new Claim();
                localClaim.setClaimUri(rs.getString("CLAIM_URI"));

                claimMapping.setLocalClaim(localClaim);
                claimMapping.setRemoteClaim(remoteClaim);
                claimMapping.setDefaultValue(rs.getString("DEFAULT_VALUE"));

                if (("1").equals(rs.getString("IS_REQUESTED"))) {
                    claimMapping.setRequested(true);
                } else if (rs.getString("IS_REQUESTED").equals("1")) {
                    claimMapping.setRequested(false);
                }

                claimMappings.add(claimMapping);
            }

            claimConfig.setClaimMappings(claimMappings.toArray(new ClaimMapping[claimMappings
                    .size()]));

            return claimConfig;

        } finally {
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
            IdentityApplicationManagementUtil.closeResultSet(rs);
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
        }
    }

    /**
     * @param dbConnection
     * @param idPName
     * @param tenantId
     * @return
     * @throws IdentityApplicationManagementException
     * @throws SQLException
     */
    private ClaimConfig getIdPClaimConfiguration(Connection dbConnection, String idPName,
                                                 String userClaimUri, String roleClaimUri, int idPId, int tenantId)
            throws IdentityApplicationManagementException, SQLException {

        PreparedStatement prepStmt = null;
        ResultSet rs = null;

        try {

            List<Claim> claimList = new ArrayList<Claim>();
            // SP_IDP_CLAIM_ID, SP_IDP_CLAIM
            String sqlStmt = IdPManagementConstants.SQLQueries.GET_IDP_CLAIMS_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, idPId);
            rs = prepStmt.executeQuery();

            ClaimConfig claimConfig = new ClaimConfig();

            while (rs.next()) {
                Claim identityProviderClaim = new Claim();
                identityProviderClaim.setClaimId(rs.getInt(1));
                identityProviderClaim.setClaimUri(rs.getString(2));
                claimList.add(identityProviderClaim);
            }

            prepStmt.clearParameters();
            IdentityApplicationManagementUtil.closeResultSet(rs);
            IdentityApplicationManagementUtil.closeStatement(prepStmt);

            // populate claim configuration with identity provider claims.
            claimConfig.setIdpClaims(claimList.toArray(new Claim[claimList.size()]));

            claimConfig.setUserClaimURI(userClaimUri);
            claimConfig.setRoleClaimURI(roleClaimUri);

            List<ClaimMapping> claimMappings = new ArrayList<ClaimMapping>();

            // SP_IDP_CLAIMS.SP_IDP_CLAIM SP_IDP_CLAIM_MAPPINGS.SP_LOCAL_CLAIM
            sqlStmt = IdPManagementConstants.SQLQueries.GET_IDP_CLAIM_MAPPINGS_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, idPId);
            rs = prepStmt.executeQuery();

            while (rs.next()) {
                ClaimMapping claimMapping = new ClaimMapping();

                Claim idpClaim = new Claim();
                idpClaim.setClaimUri(rs.getString("IDP_CLAIM.CLAIM"));

                Claim localClaim = new Claim();
                localClaim.setClaimUri(rs.getString("IDP_CLAIM_MAPPING.LOCAL_CLAIM"));

                claimMapping.setLocalClaim(localClaim);
                claimMapping.setRemoteClaim(idpClaim);
                claimMapping.setDefaultValue(rs.getString("IDP_CLAIM_MAPPING.DEFAULT_VALUE"));
                if (("1").equals(rs.getString("IDP_CLAIM_MAPPING.IS_REQUESTED"))) {
                    claimMapping.setRequested(true);
                } else if (("0").equals(rs.getString("IDP_CLAIM_MAPPING.IS_REQUESTED"))) {
                    claimMapping.setRequested(false);
                }
                claimMappings.add(claimMapping);

            }

            claimConfig.setClaimMappings(claimMappings.toArray(new ClaimMapping[claimMappings
                    .size()]));

            return claimConfig;
        } finally {
            IdentityApplicationManagementUtil.closeResultSet(rs);
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
        }
    }

    /**
     * @param dbConnection
     * @param idPName
     * @param tenantId
     * @return
     * @throws IdentityApplicationManagementException
     * @throws SQLException
     */
    public PermissionsAndRoleConfig getPermissionsAndRoleConfiguration(Connection dbConnection,
                                                                       String idPName, int idPId, int tenantId)
            throws IdentityApplicationManagementException,
            SQLException {

        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        PermissionsAndRoleConfig permissionRoleConfiguration = new PermissionsAndRoleConfig();

        try {

            List<String> idpRoleList = new ArrayList<String>();
            // SP_IDP_ROLE
            String sqlStmt = IdPManagementConstants.SQLQueries.GET_IDP_ROLES_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, idPId);
            rs = prepStmt.executeQuery();
            while (rs.next()) {
                idpRoleList.add(rs.getString("ROLE"));
            }

            permissionRoleConfiguration.setIdpRoles(idpRoleList.toArray(new String[idpRoleList
                    .size()]));

            prepStmt.clearParameters();
            IdentityApplicationManagementUtil.closeResultSet(rs);
            IdentityApplicationManagementUtil.closeStatement(prepStmt);

            List<RoleMapping> roleMappings = new ArrayList<RoleMapping>();
            // SP_IDP_ROLE_MAPPINGS.SP_USER_STORE_ID, SP_IDP_ROLE_MAPPINGS.SP_LOCAL_ROLE,
            // SP_IDP_ROLES.SP_IDP_ROLE

            sqlStmt = IdPManagementConstants.SQLQueries.GET_IDP_ROLE_MAPPINGS_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, idPId);
            rs = prepStmt.executeQuery();
            while (rs.next()) {
                LocalRole localRole = new LocalRole(rs.getString("IDP_ROLE_MAPPING.USER_STORE_ID"),
                        rs.getString("IDP_ROLE_MAPPING.LOCAL_ROLE"));
                RoleMapping roleMapping = new RoleMapping(localRole, rs.getString("IDP_ROLE.ROLE"));
                roleMappings.add(roleMapping);
            }

            permissionRoleConfiguration.setRoleMappings(roleMappings
                    .toArray(new RoleMapping[roleMappings.size()]));
            return permissionRoleConfiguration;
        } finally {
            IdentityApplicationManagementUtil.closeResultSet(rs);
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
        }
    }

    /**
     * @param provisioningConnectors
     * @param dbConnection
     * @param idpId
     * @throws IdentityApplicationManagementException
     * @throws SQLException
     */
    private void addProvisioningConnectorConfigs(
            ProvisioningConnectorConfig[] provisioningConnectors, Connection dbConnection,
            int idpId, int tenantId) throws IdentityApplicationManagementException, SQLException {

        PreparedStatement prepStmt = null;
        ResultSet rs = null;

        try {
            // SP_IDP_ID,SP_IDP_PROV_CONNECTOR_TYPE, SP_IDP_PROV_CONFIG_KEY,
            // SP_IDP_PROV_CONFIG_VALUE, SP_IDP_PROV_CONFIG_IS_SECRET

            // SP_IDP_PROV_CONFIG_PROPERTY
            // TENANT_ID, PROVISIONING_CONFIG_ID, PROPERTY_KEY, PROPERTY_VALUE, PROPERTY_TYPE,
            // IS_SECRET
            String sqlStmt = IdPManagementConstants.SQLQueries.ADD_IDP_PROVISIONING_PROPERTY_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);

            String sqlBaseStmt = IdPManagementConstants.SQLQueries.ADD_IDP_PROVISIONING_CONFIG_SQL;
            String dbProductName = dbConnection.getMetaData().getDatabaseProductName();
            PreparedStatement prepBaseStmt = dbConnection.prepareStatement(sqlBaseStmt,
                    new String[]{DBUtils.getConvertedAutoGeneratedColumnName(dbProductName, "ID")});

            if (provisioningConnectors != null) {
                for (ProvisioningConnectorConfig connector : provisioningConnectors) {
                    Property[] connctorProperties = connector.getProvisioningProperties();

                    if (connctorProperties != null) {

                        // SP_IDP_PROVISIONING_CONFIG
                        // TENANT_ID, IDP_ID, PROVISIONING_CONNECTOR_TYPE, IS_ENABLED, IS_DEFAULT
                        prepBaseStmt.setInt(1, tenantId);
                        prepBaseStmt.setInt(2, idpId);
                        prepBaseStmt.setString(3, CharacterEncoder.getSafeText(connector.getName()));

                        if (connector.isEnabled()) {
                            prepBaseStmt.setString(4, "1");
                        } else {
                            prepBaseStmt.setString(4, "0");
                        }

                        if (connector.isBlocking()) {
                            prepBaseStmt.setString(5, "1");
                        } else {
                            prepBaseStmt.setString(5, "0");
                        }

                        prepBaseStmt.executeUpdate();
                        rs = prepBaseStmt.getGeneratedKeys();

                        if (rs.next()) {
                            int provisioningConfigID = rs.getInt(1);

                            if (connctorProperties.length > 0) {
                                for (Property config : connctorProperties) {

                                    if (config == null) {
                                        continue;
                                    }

                                    // SP_IDP_PROV_CONFIG_PROPERTY
                                    //TENANT_ID, PROVISIONING_CONFIG_ID, PROPERTY_KEY,
                                    // PROPERTY_VALUE, PROPERTY_BLOB_VALUE, PROPERTY_TYPE, IS_SECRET
                                    prepStmt.setInt(1, tenantId);
                                    prepStmt.setInt(2, provisioningConfigID);
                                    prepStmt.setString(3, CharacterEncoder.getSafeText(config.getName()));

                                    // TODO : Sect property type accordingly
                                    if (IdentityApplicationConstants.ConfigElements.PROPERTY_TYPE_BLOB.equals
                                            (config.getType())) {
                                        prepStmt.setString(4, null);
                                        setBlobValue(config.getValue(), prepStmt, 5);
                                        prepStmt.setString(6, config.getType());
                                    } else {
                                        prepStmt.setString(4, CharacterEncoder.getSafeText(config.getValue()));
                                        setBlobValue(null, prepStmt, 5);
                                        prepStmt.setString(6, IdentityApplicationConstants.ConfigElements.
                                                PROPERTY_TYPE_STRING);
                                    }

                                    if (config.isConfidential()) {
                                        prepStmt.setString(7, "1");
                                    } else {
                                        prepStmt.setString(7, "0");
                                    }
                                    prepStmt.addBatch();

                                }
                            }

                        }

                        // Adding properties for base config
                        prepStmt.executeBatch();

                    }
                }
            }
        } catch (IOException e) {
            String msg = "An error occurred while processing content stream.";
            log.error(msg, e);
            throw new IdentityApplicationManagementException(msg, e);
        } finally {
            IdentityApplicationManagementUtil.closeResultSet(rs);
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
        }
    }

    private void setBlobValue(String value, PreparedStatement prepStmt, int index) throws SQLException, IOException {
        if (value != null) {
            InputStream inputStream = new ByteArrayInputStream(CharacterEncoder.getSafeText(value).getBytes());
            if (inputStream != null) {
                prepStmt.setBinaryStream(index, inputStream, inputStream.available());
            } else {
                prepStmt.setBinaryStream(index, new ByteArrayInputStream(CharacterEncoder.getSafeText("").getBytes()), 0);
            }
        } else {
            prepStmt.setBinaryStream(index, new ByteArrayInputStream(CharacterEncoder.getSafeText("").getBytes()), 0);
        }
    }

    /**
     * @param newProvisioningConnectorConfigs
     * @param dbConnection
     * @param idpId
     * @throws IdentityApplicationManagementException
     * @throws SQLException
     */
    private void updateProvisioningConnectorConfigs(
            ProvisioningConnectorConfig[] newProvisioningConnectorConfigs, Connection dbConnection,
            int idpId, int tenantId) throws IdentityApplicationManagementException, SQLException {

        PreparedStatement prepStmt = null;
        ResultSet rs = null;

        try {
            deleteProvisioningConnectorConfigs(dbConnection, idpId);

            if (newProvisioningConnectorConfigs != null
                    && newProvisioningConnectorConfigs.length > 0) {
                addProvisioningConnectorConfigs(newProvisioningConnectorConfigs, dbConnection,
                        idpId, tenantId);
            }

        } finally {
            IdentityApplicationManagementUtil.closeResultSet(rs);
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
        }
    }

    /**
     * @param dbConnection
     * @param idPName
     * @param tenantId
     * @return
     * @throws IdentityApplicationManagementException
     * @throws SQLException
     */
    public ProvisioningConnectorConfig[] getProvisioningConnectorConfigs(Connection dbConnection,
                                                                         String idPName, int idPId, int tenantId)
            throws IdentityApplicationManagementException, SQLException {

        PreparedStatement prepStmt = null;
        PreparedStatement prepBaseStmt = null;

        ResultSet rs1 = null;
        ResultSet rs2 = null;

        try {
            // SP_IDP_PROV_CONNECTOR_TYPE,SP_IDP_PROV_CONFIG_KEY,
            // SP_IDP_PROV_CONFIG_VALUE,SP_IDP_PROV_CONFIG_IS_SECRET
            String sqlBaseStmt = IdPManagementConstants.SQLQueries.GET_IDP_PROVISIONING_CONFIGS_SQL;
            prepBaseStmt = dbConnection.prepareStatement(sqlBaseStmt);

            prepBaseStmt.setInt(1, idPId);
            rs1 = prepBaseStmt.executeQuery();

            Map<String, ProvisioningConnectorConfig> provisioningConnectorMap = new HashMap<String, ProvisioningConnectorConfig>();

            while (rs1.next()) {

                ProvisioningConnectorConfig provisioningConnector;

                String type = rs1.getString("PROVISIONING_CONNECTOR_TYPE");
                if (!provisioningConnectorMap.containsKey(type)) {
                    provisioningConnector = new ProvisioningConnectorConfig();
                    provisioningConnector.setName(type);

                    if (("1").equals(rs1.getString("IS_ENABLED"))) {
                        provisioningConnector.setEnabled(true);
                    } else {
                        provisioningConnector.setEnabled(false);
                    }

                    if (("1").equals(rs1.getString("IS_BLOCKING"))) {
                        provisioningConnector.setBlocking(true);
                    } else {
                        provisioningConnector.setBlocking(false);
                    }


                    if (provisioningConnector.getProvisioningProperties() == null
                            || provisioningConnector.getProvisioningProperties().length == 0) {

                        String sqlStmt = IdPManagementConstants.SQLQueries.GET_IDP_PROVISIONING_PROPERTY_SQL;
                        prepStmt = dbConnection.prepareStatement(sqlStmt);

                        int configId = rs1.getInt("ID");
                        prepStmt.setInt(1, tenantId);
                        prepStmt.setInt(2, configId);

                        rs2 = prepStmt.executeQuery();

                        List<Property> provisioningProperties = new ArrayList<Property>();
                        while (rs2.next()) {
                            Property Property = new Property();
                            String name = rs2.getString("PROPERTY_KEY");
                            String value = rs2.getString("PROPERTY_VALUE");
                            String blobValue = getBlobValue(rs2.getBinaryStream("PROPERTY_BLOB_VALUE"));

                            String propertyType = rs2.getString("PROPERTY_TYPE");
                            String isSecret = rs2.getString("IS_SECRET");

                            Property.setName(name);
                            if (propertyType != null && IdentityApplicationConstants.ConfigElements.
                                    PROPERTY_TYPE_BLOB.equals(propertyType.trim())) {
                                Property.setValue(blobValue);
                            } else {
                                Property.setValue(value);
                            }

                            Property.setType(propertyType);

                            if (("1").equals(isSecret)) {
                                Property.setConfidential(true);
                            } else {
                                Property.setConfidential(false);
                            }

                            provisioningProperties.add(Property);
                        }
                        provisioningConnector.setProvisioningProperties(provisioningProperties
                                .toArray(new Property[provisioningProperties.size()]));
                    }

                    provisioningConnectorMap.put(type, provisioningConnector);
                }
            }

            return provisioningConnectorMap.values().toArray(
                    new ProvisioningConnectorConfig[provisioningConnectorMap.size()]);

        } finally {
            IdentityApplicationManagementUtil.closeResultSet(rs1);
            IdentityApplicationManagementUtil.closeResultSet(rs2);
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
            IdentityApplicationManagementUtil.closeStatement(prepBaseStmt);
        }
    }

    private String getBlobValue(InputStream is) throws IdentityApplicationManagementException {
        if (is != null) {
            BufferedReader br = null;
            StringBuilder sb = new StringBuilder();

            String line;
            try {

                br = new BufferedReader(new InputStreamReader(is));
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }

            } catch (IOException e) {
                throw new IdentityApplicationManagementException(e);
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                        log.error("Error in retrieving the Blob value", e);
                    }
                }
            }

            return sb.toString();
        }
        return null;
    }

    /**
     * @param dbConnection
     * @param idPName
     * @param tenantId
     * @param tenantDomain
     * @return
     * @throws IdentityApplicationManagementException
     */
    public IdentityProvider getIdPByName(Connection dbConnection, String idPName, int tenantId,
                                         String tenantDomain) throws IdentityApplicationManagementException {

        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        IdentityProvider federatedIdp = null;
        boolean dbConnectionInitialized = true;
        try {
            if (dbConnection == null) {
                dbConnection = IdentityDatabaseUtil.getDBConnection();
            } else {
                dbConnectionInitialized = false;
            }

            // SP_IDP_ID, SP_IDP_PRIMARY, SP_IDP_HOME_REALM_ID,SP_IDP_CERTIFICATE,
            // SP_IDP_TOKEN_EP_ALIAS,
            // SP_IDP_INBOUND_PROVISIONING_ENABLED,SP_IDP_INBOUND_PROVISIONING_USER_STORE_ID,
            // SP_IDP_USER_CLAIM_URI,
            // SP_IDP_ROLE_CLAIM_URI,SP_IDP_DEFAULT_AUTHENTICATOR_NAME,SP_IDP_DEFAULT_PRO_CONNECTOR_NAME
            String sqlStmt = IdPManagementConstants.SQLQueries.GET_IDP_BY_NAME_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, tenantId);
            prepStmt.setInt(2, MultitenantConstants.SUPER_TENANT_ID);
            prepStmt.setString(3, CharacterEncoder.getSafeText(idPName));
            rs = prepStmt.executeQuery();
            int idpId = -1;

            if (rs.next()) {
                federatedIdp = new IdentityProvider();
                federatedIdp.setIdentityProviderName(idPName);

                idpId = rs.getInt("ID");

                if (("1").equals(rs.getString("IS_PRIMARY"))) {
                    federatedIdp.setPrimary(true);
                } else {
                    federatedIdp.setPrimary(false);
                }

                federatedIdp.setHomeRealmId(rs.getString("HOME_REALM_ID"));
                federatedIdp.setCertificate(getBlobValue(rs.getBinaryStream("CERTIFICATE")));
                federatedIdp.setAlias(rs.getString("ALIAS"));

                JustInTimeProvisioningConfig jitProConfig = new JustInTimeProvisioningConfig();
                if (("1").equals(rs.getString("INBOUND_PROV_ENABLED"))) {
                    jitProConfig.setProvisioningEnabled(true);
                } else {
                    jitProConfig.setProvisioningEnabled(false);
                }

                jitProConfig.setProvisioningUserStore(rs.getString("INBOUND_PROV_USER_STORE_ID"));
                federatedIdp.setJustInTimeProvisioningConfig(jitProConfig);

                String userClaimUri = rs.getString("USER_CLAIM_URI");
                String roleClaimUri = rs.getString("ROLE_CLAIM_URI");

                String defaultAuthenticatorName = rs.getString("DEFAULT_AUTHENTICATOR_NAME");
                String defaultProvisioningConnectorConfigName = rs.getString("DEFAULT_PRO_CONNECTOR_NAME");
                federatedIdp.setIdentityProviderDescription(rs.getString("DESCRIPTION"));

                // IS_FEDERATION_HUB_IDP
                if ("1".equals(rs.getString("IS_FEDERATION_HUB"))) {
                    federatedIdp.setFederationHub(true);
                } else {
                    federatedIdp.setFederationHub(false);
                }

                if (federatedIdp.getClaimConfig() == null) {
                    federatedIdp.setClaimConfig(new ClaimConfig());
                }

                // IS_LOCAL_CLAIM_DIALECT
                if ("1".equals(rs.getString("IS_LOCAL_CLAIM_DIALECT"))) {
                    federatedIdp.getClaimConfig().setLocalClaimDialect(true);
                } else {
                    federatedIdp.getClaimConfig().setLocalClaimDialect(false);
                }

                federatedIdp.setProvisioningRole(rs.getString("PROVISIONING_ROLE"));

                if ("1".equals(rs.getString("IS_ENABLED"))) {
                    federatedIdp.setEnable(true);
                } else {
                    federatedIdp.setEnable(false);
                }

                federatedIdp.setDisplayName(rs.getString("DISPLAY_NAME"));

                if (defaultAuthenticatorName != null) {
                    FederatedAuthenticatorConfig defaultAuthenticator = new FederatedAuthenticatorConfig();
                    defaultAuthenticator.setName(defaultAuthenticatorName);
                    federatedIdp.setDefaultAuthenticatorConfig(defaultAuthenticator);
                }

                if (defaultProvisioningConnectorConfigName != null) {
                    ProvisioningConnectorConfig defaultProConnector = new ProvisioningConnectorConfig();
                    defaultProConnector.setName(defaultProvisioningConnectorConfigName);
                    federatedIdp.setDefaultProvisioningConnectorConfig(defaultProConnector);
                }

                // get federated authenticators.
                federatedIdp.setFederatedAuthenticatorConfigs(getFederatedAuthenticatorConfigs(
                        dbConnection, idPName, federatedIdp, tenantId));

                if (federatedIdp.getClaimConfig().isLocalClaimDialect()) {
                    federatedIdp.setClaimConfig(getLocalIdPDefaultClaimValues(dbConnection,
                            idPName, userClaimUri, roleClaimUri, idpId, tenantId));
                } else {
                    // get claim configuration.
                    federatedIdp.setClaimConfig(getIdPClaimConfiguration(dbConnection, idPName,
                            userClaimUri, roleClaimUri, idpId, tenantId));
                }

                // get provisioning connectors.
                federatedIdp.setProvisioningConnectorConfigs(getProvisioningConnectorConfigs(
                        dbConnection, idPName, idpId, tenantId));

                // get permission and role configuration.
                federatedIdp.setPermissionAndRoleConfig(getPermissionsAndRoleConfiguration(
                        dbConnection, idPName, idpId, tenantId));

            }
            dbConnection.commit();
            return federatedIdp;
        } catch (SQLException | IdentityException e) {
            IdentityApplicationManagementUtil.rollBack(dbConnection);
            String msg = "Error occurred while retrieving Identity Provider information for tenant : "
                    + tenantDomain + " and Identity Provider name : " + idPName;
            throw new IdentityApplicationManagementException(msg, e);
        } finally {
            if (dbConnectionInitialized) {
                IdentityApplicationManagementUtil.closeConnection(dbConnection);
            }
        }
    }

    /**
     * @param dbConnection
     * @param property     Property which has a unique value like EntityID to specifically identify a IdentityProvider
     *                     Unless it will return first matched IdentityProvider
     * @param value
     * @param tenantId
     * @param tenantDomain
     * @return
     * @throws IdentityApplicationManagementException
     */
    public IdentityProvider getIdPByAuthenticatorPropertyValue(Connection dbConnection, String property, String value,
                                                               int tenantId, String tenantDomain)
            throws IdentityApplicationManagementException {

        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        IdentityProvider federatedIdp = null;
        boolean dbConnectionInitialized = true;
        try {
            if (dbConnection == null) {
                dbConnection = IdentityDatabaseUtil.getDBConnection();
            } else {
                dbConnectionInitialized = false;
            }

            // SP_IDP_ID, SP_IDP_NAME, SP_IDP_PRIMARY, SP_IDP_HOME_REALM_ID,SP_IDP_CERTIFICATE,
            // SP_IDP_TOKEN_EP_ALIAS,
            // SP_IDP_INBOUND_PROVISIONING_ENABLED,SP_IDP_INBOUND_PROVISIONING_USER_STORE_ID,
            // SP_IDP_USER_CLAIM_URI,
            // SP_IDP_ROLE_CLAIM_URI,SP_IDP_DEFAULT_AUTHENTICATOR_NAME,SP_IDP_DEFAULT_PRO_CONNECTOR_NAME
            String sqlStmt = IdPManagementConstants.SQLQueries.GET_IDP_BY_AUTHENTICATOR_PROPERTY;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setString(1, CharacterEncoder.getSafeText(property));
            prepStmt.setString(2, CharacterEncoder.getSafeText(value));
            prepStmt.setInt(3, tenantId);
            rs = prepStmt.executeQuery();
            int idpId = -1;
            String idPName = "";

            if (rs.next()) {
                federatedIdp = new IdentityProvider();

                idpId = rs.getInt("idp.ID");
                idPName = rs.getString("idp.NAME");

                federatedIdp.setIdentityProviderName(idPName);

                if (("1").equals(rs.getString("idp.IS_PRIMARY"))) {
                    federatedIdp.setPrimary(true);
                } else {
                    federatedIdp.setPrimary(false);
                }

                federatedIdp.setHomeRealmId(rs.getString("idp.HOME_REALM_ID"));
                federatedIdp.setCertificate(getBlobValue(rs.getBinaryStream("idp.CERTIFICATE")));
                federatedIdp.setAlias(rs.getString("idp.ALIAS"));

                JustInTimeProvisioningConfig jitProConfig = new JustInTimeProvisioningConfig();
                if (rs.getString("idp.INBOUND_PROV_ENABLED").equals("1")) {
                    jitProConfig.setProvisioningEnabled(true);
                } else {
                    jitProConfig.setProvisioningEnabled(false);
                }

                jitProConfig.setProvisioningUserStore(rs.getString("idp.INBOUND_PROV_USER_STORE_ID"));
                federatedIdp.setJustInTimeProvisioningConfig(jitProConfig);

                String userClaimUri = rs.getString("idp.USER_CLAIM_URI");
                String roleClaimUri = rs.getString("idp.ROLE_CLAIM_URI");

                String defaultAuthenticatorName = rs.getString("idp.DEFAULT_AUTHENTICATOR_NAME");
                String defaultProvisioningConnectorConfigName = rs.getString("idp.DEFAULT_PRO_CONNECTOR_NAME");
                federatedIdp.setIdentityProviderDescription(rs.getString("idp.DESCRIPTION"));

                // IS_FEDERATION_HUB_IDP
                if ("1".equals(rs.getString("idp.IS_FEDERATION_HUB"))) {
                    federatedIdp.setFederationHub(true);
                } else {
                    federatedIdp.setFederationHub(false);
                }

                if (federatedIdp.getClaimConfig() == null) {
                    federatedIdp.setClaimConfig(new ClaimConfig());
                }

                // IS_LOCAL_CLAIM_DIALECT
                if ("1".equals(rs.getString("idp.IS_LOCAL_CLAIM_DIALECT"))) {
                    federatedIdp.getClaimConfig().setLocalClaimDialect(true);
                } else {
                    federatedIdp.getClaimConfig().setLocalClaimDialect(false);
                }

                federatedIdp.setProvisioningRole(rs.getString("idp.PROVISIONING_ROLE"));

                if ("1".equals(rs.getString("idp.IS_ENABLED"))) {
                    federatedIdp.setEnable(true);
                } else {
                    federatedIdp.setEnable(false);
                }

                federatedIdp.setDisplayName(rs.getString("idp.DISPLAY_NAME"));

                if (defaultAuthenticatorName != null) {
                    FederatedAuthenticatorConfig defaultAuthenticator = new FederatedAuthenticatorConfig();
                    defaultAuthenticator.setName(defaultAuthenticatorName);
                    federatedIdp.setDefaultAuthenticatorConfig(defaultAuthenticator);
                }

                if (defaultProvisioningConnectorConfigName != null) {
                    ProvisioningConnectorConfig defaultProConnector = new ProvisioningConnectorConfig();
                    defaultProConnector.setName(defaultProvisioningConnectorConfigName);
                    federatedIdp.setDefaultProvisioningConnectorConfig(defaultProConnector);
                }

                // get federated authenticators.
                federatedIdp.setFederatedAuthenticatorConfigs(getFederatedAuthenticatorConfigs(
                        dbConnection, idPName, federatedIdp, tenantId));

                if (federatedIdp.getClaimConfig().isLocalClaimDialect()) {
                    federatedIdp.setClaimConfig(getLocalIdPDefaultClaimValues(dbConnection,
                            idPName, userClaimUri, roleClaimUri, idpId, tenantId));
                } else {
                    // get claim configuration.
                    federatedIdp.setClaimConfig(getIdPClaimConfiguration(dbConnection, idPName,
                            userClaimUri, roleClaimUri, idpId, tenantId));
                }

                // get provisioning connectors.
                federatedIdp.setProvisioningConnectorConfigs(getProvisioningConnectorConfigs(
                        dbConnection, idPName, idpId, tenantId));

                // get permission and role configuration.
                federatedIdp.setPermissionAndRoleConfig(getPermissionsAndRoleConfiguration(
                        dbConnection, idPName, idpId, tenantId));

            }
            dbConnection.commit();
            return federatedIdp;
        } catch (SQLException | IdentityException e) {
            IdentityApplicationManagementUtil.rollBack(dbConnection);
            String msg = "Error occurred while retrieving Identity Provider information for Authenticator Property : "
                    + property + " and value : " + value;
            throw new IdentityApplicationManagementException(msg, e);
        } finally {
            if (dbConnectionInitialized) {
                IdentityApplicationManagementUtil.closeConnection(dbConnection);
            }
        }
    }

    /**
     * @param realmId
     * @param tenantId
     * @param tenantDomain
     * @return
     * @throws IdentityApplicationManagementException
     * @throws SQLException
     */
    public IdentityProvider getIdPByRealmId(String realmId, int tenantId, String tenantDomain)
            throws IdentityApplicationManagementException {

        Connection dbConnection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        String idPName = null;

        try {
            dbConnection = IdentityDatabaseUtil.getDBConnection();
            String sqlStmt = IdPManagementConstants.SQLQueries.GET_IDP_NAME_BY_REALM_ID_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, tenantId);
            prepStmt.setInt(2, MultitenantConstants.SUPER_TENANT_ID);
            prepStmt.setString(3, CharacterEncoder.getSafeText(realmId));
            rs = prepStmt.executeQuery();
            if (rs.next()) {
                idPName = rs.getString("NAME");
            }

            IdentityApplicationManagementUtil.closeStatement(prepStmt);
            IdentityApplicationManagementUtil.closeResultSet(rs);

            dbConnection.commit();
            return getIdPByName(dbConnection, idPName, tenantId, tenantDomain);
        } catch (SQLException | IdentityException e) {
            throw new IdentityApplicationManagementException(
                    "Error while retreiving Identity Provider by realm " + realmId, e);
        } finally {
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
            IdentityApplicationManagementUtil.closeResultSet(rs);
            IdentityApplicationManagementUtil.closeConnection(dbConnection);
        }

    }

    /**
     * @param identityProvider
     * @param tenantId
     * @throws IdentityApplicationManagementException
     */
    public void addIdP(IdentityProvider identityProvider, int tenantId)
            throws IdentityApplicationManagementException {

        Connection dbConnection = null;
        try {
            dbConnection = IdentityDatabaseUtil.getDBConnection();

            if (identityProvider.isPrimary()) {
                // this is going to be the primary. Switch off any other primary set up in the
                // system.
                switchOffPrimary(dbConnection, tenantId);
            }

            PreparedStatement prepStmt = null;

            // SP_TENANT_ID, SP_IDP_NAME, SP_IDP_PRIMARY, SP_IDP_HOME_REALM_ID, SP_IDP_CERTIFICATE,
            // SP_IDP_TOKEN_EP_ALIAS,
            // SP_IDP_INBOUND_PROVISIONING_ENABLED,SP_IDP_INBOUND_PROVISIONING_USER_STORE_ID,
            // SP_IDP_USER_CLAIM_URI,SP_IDP_ROLE_CLAIM_URI,SP_IDP_DEFAULT_AUTHENTICATOR_NAME,SP_IDP_DEFAULT_PRO_CONNECTOR_NAME
            String sqlStmt = IdPManagementConstants.SQLQueries.ADD_IDP_SQL;

            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, tenantId);
            prepStmt.setString(2, CharacterEncoder.getSafeText(identityProvider.getIdentityProviderName()));

            if (identityProvider.isPrimary()) {
                prepStmt.setString(3, "1");
            } else {
                prepStmt.setString(3, "0");
            }

            prepStmt.setString(4, CharacterEncoder.getSafeText(identityProvider.getHomeRealmId()));

            if (StringUtils.isNotBlank(identityProvider.getCertificate())) {
                try {
                    IdentityApplicationManagementUtil.getCertData(identityProvider.getCertificate());
                } catch (CertificateException ex) {
                    String msg = "Malformed Public Certificate file has been provided.";
                    throw new IdentityApplicationManagementException(msg, ex);
                }
            }
            setBlobValue(identityProvider.getCertificate(), prepStmt, 5);

            prepStmt.setString(6, CharacterEncoder.getSafeText(identityProvider.getAlias()));

            if (identityProvider.getJustInTimeProvisioningConfig() != null
                    && identityProvider.getJustInTimeProvisioningConfig().isProvisioningEnabled()) {
                // just in time provisioning enabled for this identity provider.
                // based on the authentication response from the identity provider - user will be
                // provisioned locally.
                prepStmt.setString(7, "1");
                // user will be provisioned to the configured user store.
                prepStmt.setString(8, CharacterEncoder.getSafeText(identityProvider.getJustInTimeProvisioningConfig()
                        .getProvisioningUserStore()));
            } else {
                prepStmt.setString(7, "0");
                prepStmt.setString(8, null);
            }

            if (identityProvider.getClaimConfig() != null) {
                // this is how we find the subject name from the authentication response.
                // this claim URI is in identity provider's own dialect.
                prepStmt.setString(9, CharacterEncoder.getSafeText(identityProvider.getClaimConfig().getUserClaimURI()));
                // this is how we find the role name from the authentication response.
                // this claim URI is in identity provider's own dialect.
                prepStmt.setString(10, CharacterEncoder.getSafeText(identityProvider.getClaimConfig().getRoleClaimURI()));
            } else {
                prepStmt.setString(9, null);
                prepStmt.setString(10, null);
            }

            if (identityProvider.getDefaultAuthenticatorConfig() != null) {
                prepStmt.setString(11, CharacterEncoder.getSafeText(identityProvider.getDefaultAuthenticatorConfig()
                        .getName()));
            } else {
                prepStmt.setString(11, null);
            }

            if (identityProvider.getDefaultProvisioningConnectorConfig() != null) {
                prepStmt.setString(12, CharacterEncoder.getSafeText(identityProvider.getDefaultProvisioningConnectorConfig()
                        .getName()));
            } else {
                prepStmt.setString(12, null);
            }

            prepStmt.setString(13, CharacterEncoder.getSafeText(identityProvider.getIdentityProviderDescription()));

            if (identityProvider.isFederationHub()) {
                prepStmt.setString(14, "1");
            } else {
                prepStmt.setString(14, "0");
            }

            if (identityProvider.getClaimConfig() != null
                    && identityProvider.getClaimConfig().isLocalClaimDialect()) {
                prepStmt.setString(15, "1");
            } else {
                prepStmt.setString(15, "0");
            }

            prepStmt.setString(16, CharacterEncoder.getSafeText(identityProvider.getProvisioningRole()));

            // enabled by default
            prepStmt.setString(17, "1");

            prepStmt.setString(18, CharacterEncoder.getSafeText(identityProvider.getDisplayName()));

            prepStmt.executeUpdate();
            prepStmt.clearParameters();
            IdentityApplicationManagementUtil.closeStatement(prepStmt);

            // get the id of the just added identity provider.
            int idPId = getIdentityProviderIdByName(dbConnection,
                    identityProvider.getIdentityProviderName(), tenantId);

            if (idPId <= 0) {
                String msg = "Error adding Identity Provider for tenant " + tenantId;
                throw new IdentityApplicationManagementException(msg);
            }

            // add provisioning connectors.
            if (identityProvider.getProvisioningConnectorConfigs() != null
                    && identityProvider.getProvisioningConnectorConfigs().length > 0) {
                addProvisioningConnectorConfigs(identityProvider.getProvisioningConnectorConfigs(),
                        dbConnection, idPId, tenantId);
            }

            // add federated authenticators.
            addFederatedAuthenticatorConfigs(identityProvider.getFederatedAuthenticatorConfigs(),
                    dbConnection, idPId, tenantId);

            // add role configuration.
            if (identityProvider.getPermissionAndRoleConfig() != null) {
                if (identityProvider.getPermissionAndRoleConfig().getIdpRoles() != null
                        && identityProvider.getPermissionAndRoleConfig().getIdpRoles().length > 0) {
                    // add roles.
                    addIdPRoles(dbConnection, idPId, tenantId, identityProvider
                            .getPermissionAndRoleConfig().getIdpRoles());

                    if (identityProvider.getPermissionAndRoleConfig().getRoleMappings() != null
                            && identityProvider.getPermissionAndRoleConfig().getRoleMappings().length > 0) {
                        // add role mappings.
                        addIdPRoleMappings(dbConnection, idPId, tenantId, identityProvider
                                .getPermissionAndRoleConfig().getRoleMappings());
                    }
                }
            }

            // add claim configuration.
            if (identityProvider.getClaimConfig() != null
                    && identityProvider.getClaimConfig().getClaimMappings() != null
                    && identityProvider.getClaimConfig().getClaimMappings().length > 0) {
                if (identityProvider.getClaimConfig().isLocalClaimDialect()) {
                    // identity provider is using local claim dialect - we do not need to add
                    // claims.
                    addDefaultClaimValuesForLocalIdP(dbConnection, idPId, tenantId,
                            identityProvider.getClaimConfig().getClaimMappings());
                } else {
                    addIdPClaims(dbConnection, idPId, tenantId, identityProvider.getClaimConfig()
                            .getIdpClaims());

                    addIdPClaimMappings(dbConnection, idPId, tenantId, identityProvider
                            .getClaimConfig().getClaimMappings());
                }

            }

            dbConnection.commit();
        } catch (IOException e) {
            String msg = "An error occurred while processing content stream.";
            log.error(msg, e);
            throw new IdentityApplicationManagementException(msg, e);
        } catch (SQLException | IdentityException e) {
            IdentityApplicationManagementUtil.rollBack(dbConnection);
            String msg = "Error occurred while adding Identity Provider for tenant " + tenantId;
            throw new IdentityApplicationManagementException(msg, e);
        } finally {
            IdentityApplicationManagementUtil.closeConnection(dbConnection);
        }
    }

    /**
     * @param newIdentityProvider
     * @param currentIdentityProvider
     * @param tenantId
     * @throws IdentityApplicationManagementException
     */
    public void updateIdP(IdentityProvider newIdentityProvider,
                          IdentityProvider currentIdentityProvider, int tenantId)
            throws IdentityApplicationManagementException {

        Connection dbConnection = null;

        try {

            dbConnection = IdentityDatabaseUtil.getDBConnection();

            int idPId = getIdentityProviderIdByName(dbConnection,
                    currentIdentityProvider.getIdentityProviderName(), tenantId);

            if (idPId <= 0) {
                String msg = "Trying to update non-existent Identity Provider for tenant "
                        + tenantId;
                throw new IdentityApplicationManagementException(msg);
            }

            PreparedStatement prepStmt = null;

            // SP_IDP_NAME=?, SP_IDP_PRIMARY=?,SP_IDP_HOME_REALM_ID=?, SP_IDP_CERTIFICATE=?,
            // SP_IDP_TOKEN_EP_ALIAS=?,
            // SP_IDP_INBOUND_PROVISIONING_ENABLED=?,SP_IDP_INBOUND_PROVISIONING_USER_STORE_ID=?,SP_IDP_USER_CLAIM_URI=?,
            // SP_IDP_ROLE_CLAIM_URI=?,SP_IDP_DEFAULT_AUTHENTICATOR_NAME=?,SP_IDP_DEFAULT_PRO_CONNECTOR_NAME=?
            String sqlStmt = IdPManagementConstants.SQLQueries.UPDATE_IDP_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);

            prepStmt.setString(1, CharacterEncoder.getSafeText(newIdentityProvider.getIdentityProviderName()));

            if (newIdentityProvider.isPrimary()) {
                prepStmt.setString(2, "1");
            } else {
                prepStmt.setString(2, "0");
            }

            prepStmt.setString(3, CharacterEncoder.getSafeText(newIdentityProvider.getHomeRealmId()));

            if (StringUtils.isNotBlank(newIdentityProvider.getCertificate())) {
                try {
                    IdentityApplicationManagementUtil.getCertData(newIdentityProvider.getCertificate());
                } catch (CertificateException ex) {
                    String msg = "Malformed Public Certificate file has been provided.";
                    throw new IdentityApplicationManagementException(msg, ex);
                }
            }
            setBlobValue(newIdentityProvider.getCertificate(), prepStmt, 4);

            prepStmt.setString(5, CharacterEncoder.getSafeText(newIdentityProvider.getAlias()));

            if (newIdentityProvider.getJustInTimeProvisioningConfig() != null
                    && newIdentityProvider.getJustInTimeProvisioningConfig()
                    .isProvisioningEnabled()) {
                prepStmt.setString(6, "1");
                prepStmt.setString(7, CharacterEncoder.getSafeText(newIdentityProvider.getJustInTimeProvisioningConfig()
                        .getProvisioningUserStore()));

            } else {
                prepStmt.setString(6, "0");
                prepStmt.setString(7, null);
            }

            if (newIdentityProvider.getClaimConfig() != null) {
                prepStmt.setString(8, CharacterEncoder.getSafeText(newIdentityProvider.getClaimConfig().
                        getUserClaimURI()));
                prepStmt.setString(9, CharacterEncoder.getSafeText(newIdentityProvider.getClaimConfig().
                        getRoleClaimURI()));
            } else {
                prepStmt.setString(8, null);
                prepStmt.setString(9, null);
            }

            // update the default authenticator
            if (newIdentityProvider.getDefaultAuthenticatorConfig() != null
                    && newIdentityProvider.getDefaultAuthenticatorConfig().getName() != null) {
                prepStmt.setString(10, CharacterEncoder.getSafeText(newIdentityProvider.getDefaultAuthenticatorConfig()
                        .getName()));
            } else {
                // its not a must to have a default authenticator.
                prepStmt.setString(10, null);
            }

            // update the default provisioning connector.
            if (newIdentityProvider.getDefaultProvisioningConnectorConfig() != null
                    && newIdentityProvider.getDefaultProvisioningConnectorConfig().getName() != null) {
                prepStmt.setString(11, CharacterEncoder.getSafeText(newIdentityProvider.
                        getDefaultProvisioningConnectorConfig()
                        .getName()));
            } else {
                // its not a must to have a default provisioning connector..
                prepStmt.setString(11, null);
            }

            prepStmt.setString(12, CharacterEncoder.getSafeText(newIdentityProvider.getIdentityProviderDescription()));

            if (newIdentityProvider.isFederationHub()) {
                prepStmt.setString(13, "1");
            } else {
                prepStmt.setString(13, "0");
            }

            if (newIdentityProvider.getClaimConfig() != null
                    && newIdentityProvider.getClaimConfig().isLocalClaimDialect()) {
                prepStmt.setString(14, "1");
            } else {
                prepStmt.setString(14, "0");
            }

            prepStmt.setString(15, CharacterEncoder.getSafeText(newIdentityProvider.getProvisioningRole()));

            if (newIdentityProvider.isEnable()) {
                prepStmt.setString(16, "1");
            } else {
                prepStmt.setString(16, "0");
            }

            prepStmt.setString(17, CharacterEncoder.getSafeText(newIdentityProvider.getDisplayName()));

            prepStmt.setInt(18, tenantId);
            prepStmt.setString(19, CharacterEncoder.getSafeText(currentIdentityProvider.getIdentityProviderName()));

            prepStmt.executeUpdate();

            prepStmt.clearParameters();
            IdentityApplicationManagementUtil.closeStatement(prepStmt);

            sqlStmt = IdPManagementConstants.SQLQueries.GET_IDP_BY_NAME_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, tenantId);
            prepStmt.setInt(2, MultitenantConstants.SUPER_TENANT_ID);
            prepStmt.setString(3, CharacterEncoder.getSafeText(newIdentityProvider.getIdentityProviderName()));
            ResultSet rs = prepStmt.executeQuery();

            if (rs.next()) {

                // id of the updated identity provider.
                int idpId = rs.getInt("ID");

                // update federated authenticators.
                updateFederatedAuthenticatorConfigs(
                        newIdentityProvider.getFederatedAuthenticatorConfigs(),
                        currentIdentityProvider.getFederatedAuthenticatorConfigs(), dbConnection,
                        idpId, tenantId);

                // update claim configuration.
                updateClaimConfiguration(dbConnection, idpId, tenantId,
                        newIdentityProvider.getClaimConfig());

                // update role configuration.
                updateRoleConfiguration(dbConnection, idpId, tenantId,
                        newIdentityProvider.getPermissionAndRoleConfig());

                // // update provisioning connectors.
                updateProvisioningConnectorConfigs(
                        newIdentityProvider.getProvisioningConnectorConfigs(), dbConnection, idpId,
                        tenantId);

            }

            dbConnection.commit();
        } catch (IOException e) {
            String msg = "An error occurred while processing content stream.";
            log.error(msg, e);
            throw new IdentityApplicationManagementException(msg, e);
        } catch (SQLException | IdentityException e) {
            log.error(e.getMessage(), e);
            IdentityApplicationManagementUtil.rollBack(dbConnection);
            String msg = "Error occurred while updating Identity Provider information  for tenant "
                    + tenantId;
            throw new IdentityApplicationManagementException(msg, e);
        } finally {
            IdentityApplicationManagementUtil.closeConnection(dbConnection);
        }
    }

    public boolean isIdpReferredBySP(String idPName, int tenantId)
            throws IdentityApplicationManagementException {
        boolean isReffered = false;
        Connection dbConnection = null;
        PreparedStatement prepStmtFedIdp = null;
        ResultSet rsFedIdp = null;
        PreparedStatement prepStmtProvIdp = null;
        ResultSet rsProvIdp = null;

        try {
            dbConnection = IdentityDatabaseUtil.getDBConnection();
            String sqlStmt = IdPManagementConstants.SQLQueries.GET_SP_FEDERATED_IDP_REFS;
            prepStmtFedIdp = dbConnection.prepareStatement(sqlStmt);
            prepStmtFedIdp.setInt(1, tenantId);
            prepStmtFedIdp.setInt(2, MultitenantConstants.SUPER_TENANT_ID);
            prepStmtFedIdp.setString(3, CharacterEncoder.getSafeText(idPName));
            rsFedIdp = prepStmtFedIdp.executeQuery();
            if (rsFedIdp.next()) {
                isReffered = rsFedIdp.getInt(1) > 0;
            }
            if (!isReffered) {
                sqlStmt = IdPManagementConstants.SQLQueries.GET_SP_PROVISIONING_CONNECTOR_REFS;
                prepStmtProvIdp = dbConnection.prepareStatement(sqlStmt);
                prepStmtProvIdp.setInt(1, tenantId);
                prepStmtProvIdp.setString(2, CharacterEncoder.getSafeText(idPName));
                rsProvIdp = prepStmtProvIdp.executeQuery();
                if (rsProvIdp.next()) {
                    isReffered = rsProvIdp.getInt(1) > 0;
                }
            }
            dbConnection.commit();
        } catch (SQLException | IdentityException e) {
            log.error(e.getMessage(), e);
            String msg = "Error occurred while searching for IDP references in SP ";
            throw new IdentityApplicationManagementException(msg, e);
        } finally {
            IdentityApplicationManagementUtil.closeStatement(prepStmtFedIdp);
            IdentityApplicationManagementUtil.closeResultSet(rsFedIdp);
            if (prepStmtProvIdp != null) {
                IdentityApplicationManagementUtil.closeStatement(prepStmtProvIdp);
            }
            if (rsProvIdp != null) {
                IdentityApplicationManagementUtil.closeResultSet(rsProvIdp);
            }
            IdentityApplicationManagementUtil.closeConnection(dbConnection);
        }
        return isReffered;
    }

    /**
     * @param idPName
     * @param tenantId
     * @param tenantDomain
     * @throws IdentityApplicationManagementException
     */
    public void deleteIdP(String idPName, int tenantId, String tenantDomain)
            throws IdentityApplicationManagementException {

        Connection dbConnection = null;
        try {
            dbConnection = IdentityDatabaseUtil.getDBConnection();
            IdentityProvider identityProvider = getIdPByName(dbConnection, idPName, tenantId,
                    tenantDomain);
            if (identityProvider == null) {
                String msg = "Trying to delete non-existent Identity Provider for tenant "
                        + tenantDomain;
                log.error(msg);
                return;
            }

            IdentityProvider primaryIdP = getPrimaryIdP(dbConnection, tenantId, tenantDomain);
            if (primaryIdP == null) {
                String msg = "Cannot find primary Identity Provider for tenant " + tenantDomain;
                log.warn(msg);
            }

            deleteIdP(dbConnection, tenantId, idPName);

            if (primaryIdP != null && idPName.equals(primaryIdP.getIdentityProviderName())) {
                doAppointPrimary(dbConnection, tenantId, tenantDomain);
            }

            dbConnection.commit();
        } catch (SQLException | IdentityException e) {
            log.error(e.getMessage(), e);
            IdentityApplicationManagementUtil.rollBack(dbConnection);
            String msg = "Error occurred while deleting Identity Provider of tenant "
                    + tenantDomain;
            throw new IdentityApplicationManagementException(msg);
        } finally {
            IdentityApplicationManagementUtil.closeConnection(dbConnection);
        }
    }

    public IdentityProvider getPrimaryIdP(Connection dbConnection, int tenantId, String tenantDomain)
            throws IdentityApplicationManagementException {

        boolean dbConnInitialized = true;
        PreparedStatement prepStmt = null;
        try {
            if (dbConnection == null) {
                dbConnection = IdentityDatabaseUtil.getDBConnection();
            } else {
                dbConnInitialized = false;
            }
            String sqlStmt = IdPManagementConstants.SQLQueries.GET_PRIMARY_IDP_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, tenantId);
            prepStmt.setString(2, "1");
            ResultSet rs = prepStmt.executeQuery();
            dbConnection.commit();
            if (rs.next()) {
                IdentityProvider identityProviderDO = new IdentityProvider();
                identityProviderDO.setIdentityProviderName(rs.getString(1));
                identityProviderDO.setPrimary(true);
                identityProviderDO.setHomeRealmId(rs.getString("HOME_REALM_ID"));

                if ("1".equals(rs.getString("IS_FEDERATION_HUB"))) {
                    identityProviderDO.setFederationHub(true);
                } else {
                    identityProviderDO.setFederationHub(false);
                }

                if (identityProviderDO.getClaimConfig() == null) {
                    identityProviderDO.setClaimConfig(new ClaimConfig());
                }

                if ("1".equals(rs.getString("IS_LOCAL_CLAIM_DIALECT"))) {
                    identityProviderDO.getClaimConfig().setLocalClaimDialect(true);
                } else {
                    identityProviderDO.getClaimConfig().setLocalClaimDialect(false);
                }

                return identityProviderDO;
            }
        } catch (SQLException | IdentityException e) {
            log.error(e.getMessage(), e);
            IdentityApplicationManagementUtil.rollBack(dbConnection);
            String msg = "Error occurred while retrieving primary Identity Provider for tenant "
                    + tenantDomain;
            throw new IdentityApplicationManagementException(msg);
        } finally {
            if (dbConnInitialized) {
                IdentityApplicationManagementUtil.closeConnection(dbConnection);
            }
        }
        return null;
    }

    public void deleteTenantRole(int tenantId, String role, String tenantDomain)
            throws IdentityApplicationManagementException {

        Connection dbConnection = null;
        PreparedStatement prepStmt = null;
        try {
            dbConnection = IdentityDatabaseUtil.getDBConnection();
            String sqlStmt = IdPManagementConstants.SQLQueries.DELETE_ROLE_LISTENER_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, tenantId);
            prepStmt.setString(2, role);
            prepStmt.executeUpdate();
            dbConnection.commit();
        } catch (SQLException | IdentityException e) {
            log.error(e.getMessage(), e);
            IdentityApplicationManagementUtil.rollBack(dbConnection);
            String msg = "Error occurred while deleting tenant role " + role + " of tenant "
                    + tenantDomain;
            throw new IdentityApplicationManagementException(msg);
        } finally {
            IdentityApplicationManagementUtil.closeConnection(dbConnection);
        }
    }

    public void renameTenantRole(String newRoleName, String oldRoleName, int tenantId,
                                 String tenantDomain) throws IdentityApplicationManagementException {

        Connection dbConnection = null;
        PreparedStatement prepStmt;
        try {
            dbConnection = IdentityDatabaseUtil.getDBConnection();
            String sqlStmt = IdPManagementConstants.SQLQueries.RENAME_ROLE_LISTENER_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setString(1, CharacterEncoder.getSafeText(newRoleName));
            prepStmt.setInt(2, tenantId);
            prepStmt.setString(3, CharacterEncoder.getSafeText(oldRoleName));
            prepStmt.executeUpdate();
            dbConnection.commit();
        } catch (SQLException | IdentityException e) {
            log.error(e.getMessage(), e);
            IdentityApplicationManagementUtil.rollBack(dbConnection);
            String msg = "Error occurred while renaming tenant role " + oldRoleName + " to "
                    + newRoleName + " of tenant " + tenantDomain;
            throw new IdentityApplicationManagementException(msg);
        } finally {
            IdentityApplicationManagementUtil.closeConnection(dbConnection);
        }
    }

    /**
     * @param dbConnection
     * @param idpId
     * @throws IdentityApplicationManagementException
     * @throws SQLException
     */
    private void deleteAllIdPClaims(Connection dbConnection, int idpId)
            throws IdentityApplicationManagementException, SQLException {

        PreparedStatement prepStmt = null;
        try {
            String sqlStmt = IdPManagementConstants.SQLQueries.DELETE_ALL_CLAIMS_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, idpId);
            prepStmt.executeUpdate();
        } finally {
            IdentityApplicationManagementUtil.closeStatement(prepStmt);

        }
    }

    /**
     * @param dbConnection
     * @param idpId
     * @param tenantId
     * @throws IdentityApplicationManagementException
     * @throws SQLException
     */
    private void deleteLocalIdPClaimValues(Connection dbConnection, int idpId, int tenantId)
            throws IdentityApplicationManagementException, SQLException {

        PreparedStatement prepStmt = null;
        try {
            String sqlStmt = IdPManagementConstants.SQLQueries.DELETE_LOCAL_IDP_DEFAULT_CLAIM_VALUES_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, idpId);
            prepStmt.setInt(2, tenantId);

            prepStmt.executeUpdate();
        } finally {
            IdentityApplicationManagementUtil.closeStatement(prepStmt);

        }
    }

    /**
     * @param dbConnection
     * @param idpId
     * @throws IdentityApplicationManagementException
     * @throws SQLException
     */
    private void deleteAllIdPRoles(Connection dbConnection, int idpId)
            throws IdentityApplicationManagementException, SQLException {

        PreparedStatement prepStmt = null;
        try {
            String sqlStmt = IdPManagementConstants.SQLQueries.DELETE_ALL_ROLES_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, idpId);
            prepStmt.executeUpdate();
        } finally {
            IdentityApplicationManagementUtil.closeStatement(prepStmt);

        }
    }

    /**
     * @param newClaimURI
     * @param oldClaimURI
     * @param tenantId
     * @param tenantDomain
     * @throws IdentityApplicationManagementException
     */
    public void renameClaimURI(String newClaimURI, String oldClaimURI, int tenantId,
                               String tenantDomain) throws IdentityApplicationManagementException {

        Connection dbConnection = null;
        PreparedStatement prepStmt = null;
        try {
            dbConnection = IdentityDatabaseUtil.getDBConnection();
            String sqlStmt = IdPManagementConstants.SQLQueries.RENAME_CLAIM_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setString(1, CharacterEncoder.getSafeText(newClaimURI));
            prepStmt.setInt(2, tenantId);
            prepStmt.setString(3, CharacterEncoder.getSafeText(oldClaimURI));
            prepStmt.executeUpdate();
            dbConnection.commit();
        } catch (SQLException | IdentityException e) {
            log.error(e.getMessage(), e);
            String msg = "Error occurred while renaming tenant role " + oldClaimURI + " to "
                    + newClaimURI + " of tenant " + tenantDomain;
            throw new IdentityApplicationManagementException(msg);
        } finally {
            IdentityApplicationManagementUtil.closeConnection(dbConnection);
        }
    }

    /**
     * @param conn
     * @param tenantId
     * @throws SQLException
     */
    private void switchOffPrimary(Connection conn, int tenantId) throws SQLException {

        PreparedStatement prepStmt = null;
        // SP_IDP_PRIMARY
        String sqlStmt = IdPManagementConstants.SQLQueries.SWITCH_IDP_PRIMARY_SQL;

        try {
            prepStmt = conn.prepareStatement(sqlStmt);
            prepStmt.setString(1, "0");
            prepStmt.setInt(2, tenantId);
            prepStmt.setString(3, "1");
            prepStmt.executeUpdate();
        } finally {
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
        }
    }

    private void doAppointPrimary(Connection conn, int tenantId, String tenantDomain)
            throws SQLException, IdentityApplicationManagementException {

        List<IdentityProvider> tenantIdPs = getIdPs(conn, tenantId, tenantDomain);
        if (!tenantIdPs.isEmpty()) {
            PreparedStatement prepStmt = null;
            String sqlStmt = IdPManagementConstants.SQLQueries.SWITCH_IDP_PRIMARY_ON_DELETE_SQL;
            prepStmt = conn.prepareStatement(sqlStmt);
            prepStmt.setString(1, "1");
            prepStmt.setInt(2, tenantId);
            prepStmt.setString(3, CharacterEncoder.getSafeText(tenantIdPs.get(0).getIdentityProviderName()));
            prepStmt.setString(4, "0");
            prepStmt.executeUpdate();
        } else {
            String msg = "No Identity Providers registered for tenant " + tenantDomain;
            log.warn(msg);
        }
    }

    /**
     * @param conn
     * @param idPId
     * @param claims
     * @throws SQLException
     */
    private void addIdPClaims(Connection conn, int idPId, int tenantId, Claim[] claims)
            throws SQLException {
        PreparedStatement prepStmt = null;

        if (claims == null || claims.length == 0) {
            return;
        }

        try {
            // SP_IDP_ID, SP_IDP_CLAIM
            String sqlStmt = IdPManagementConstants.SQLQueries.ADD_IDP_CLAIMS_SQL;
            prepStmt = conn.prepareStatement(sqlStmt);
            for (Claim claim : claims) {
                prepStmt.setInt(1, idPId);
                prepStmt.setInt(2, tenantId);
                prepStmt.setString(3, CharacterEncoder.getSafeText(claim.getClaimUri()));
                prepStmt.addBatch();
                prepStmt.clearParameters();
            }
            prepStmt.executeBatch();
        } finally {
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
        }
    }

    /**
     * @param conn
     * @param idPId
     * @param tenantId
     * @param claimMappings
     * @throws SQLException
     * @throws IdentityApplicationManagementException
     */
    private void addDefaultClaimValuesForLocalIdP(Connection conn, int idPId, int tenantId,
                                                  ClaimMapping[] claimMappings) throws SQLException,
            IdentityApplicationManagementException {

        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        String sqlStmt;

        try {

            if (claimMappings == null || claimMappings.length == 0) {
                return;
            }

            sqlStmt = IdPManagementConstants.SQLQueries.ADD_LOCAL_IDP_DEFAULT_CLAIM_VALUES_SQL;
            prepStmt = conn.prepareStatement(sqlStmt);
            for (ClaimMapping mapping : claimMappings) {
                if (mapping != null && mapping.getLocalClaim() != null
                        && mapping.getLocalClaim().getClaimUri() != null) {

                    prepStmt.setInt(1, idPId);
                    prepStmt.setString(2, CharacterEncoder.getSafeText(mapping.getLocalClaim().getClaimUri()));
                    prepStmt.setString(3, CharacterEncoder.getSafeText(mapping.getDefaultValue()));
                    prepStmt.setInt(4, tenantId);
                    if (mapping.isRequested()) {
                        prepStmt.setString(5, "1");
                    } else {
                        prepStmt.setString(5, "0");
                    }
                    prepStmt.addBatch();
                }
            }

            prepStmt.executeBatch();

        } finally {
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
            IdentityApplicationManagementUtil.closeResultSet(rs);
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
        }
    }

    /**
     * @param conn
     * @param idPId
     * @param tenantId
     * @param claimMappings
     * @throws SQLException
     * @throws IdentityApplicationManagementException
     */
    private void addIdPClaimMappings(Connection conn, int idPId, int tenantId,
                                     ClaimMapping[] claimMappings) throws SQLException,
            IdentityApplicationManagementException {

        Map<String, Integer> claimIdMap = new HashMap<String, Integer>();
        PreparedStatement prepStmt = null;
        ResultSet rs = null;

        try {

            if (claimMappings == null || claimMappings.length == 0) {
                return;
            }

            String sqlStmt = IdPManagementConstants.SQLQueries.GET_IDP_CLAIMS_SQL;
            prepStmt = conn.prepareStatement(sqlStmt);
            prepStmt.setInt(1, idPId);
            rs = prepStmt.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("ID");
                String claim = rs.getString("CLAIM");
                claimIdMap.put(claim, id);
            }

            prepStmt.clearParameters();
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
            IdentityApplicationManagementUtil.closeResultSet(rs);

            if (claimIdMap.isEmpty()) {
                String message = "No Identity Provider claim URIs defined for tenant " + tenantId;
                throw new IdentityApplicationManagementException(message);
            }

            sqlStmt = IdPManagementConstants.SQLQueries.ADD_IDP_CLAIM_MAPPINGS_SQL;
            prepStmt = conn.prepareStatement(sqlStmt);
            for (ClaimMapping mapping : claimMappings) {
                if (mapping != null && mapping.getRemoteClaim() != null
                        && claimIdMap.containsKey(mapping.getRemoteClaim().getClaimUri())) {

                    int idpClaimId = claimIdMap.get(mapping.getRemoteClaim().getClaimUri());
                    String localClaimURI = mapping.getLocalClaim().getClaimUri();

                    prepStmt.setInt(1, idpClaimId);
                    prepStmt.setInt(2, tenantId);
                    prepStmt.setString(3, CharacterEncoder.getSafeText(localClaimURI));
                    prepStmt.setString(4, CharacterEncoder.getSafeText(mapping.getDefaultValue()));

                    if (mapping.isRequested()) {
                        prepStmt.setString(5, "1");
                    } else {
                        prepStmt.setString(5, "0");
                    }

                    prepStmt.addBatch();
                } else {
                    String msg = "Cannot find Identity Provider claim mapping for tenant "
                            + tenantId;
                    throw new IdentityApplicationManagementException(msg);
                }
            }

            prepStmt.executeBatch();

        } finally {
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
            IdentityApplicationManagementUtil.closeResultSet(rs);
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
        }
    }

    /**
     * @param conn
     * @param idPId
     * @param idpRoleNames
     * @throws SQLException
     */
    private void addIdPRoles(Connection conn, int idPId, int tenantId, String[] idpRoleNames)
            throws SQLException {

        PreparedStatement prepStmt = null;
        // SP_IDP_ID, SP_IDP_ROLE
        String sqlStmt = IdPManagementConstants.SQLQueries.ADD_IDP_ROLES_SQL;

        if (idpRoleNames == null || idpRoleNames.length == 0) {
            return;
        }

        try {
            prepStmt = conn.prepareStatement(sqlStmt);

            for (String idpRole : idpRoleNames) {
                prepStmt.setInt(1, idPId);
                prepStmt.setInt(2, tenantId);
                prepStmt.setString(3, CharacterEncoder.getSafeText(idpRole));
                prepStmt.addBatch();
                prepStmt.clearParameters();
            }

            prepStmt.executeBatch();

        } finally {
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
        }
    }

    /**
     * @param conn
     * @param idPId
     * @param tenantId
     * @param roleMappings
     * @throws SQLException
     * @throws IdentityApplicationManagementException
     */
    private void addIdPRoleMappings(Connection conn, int idPId, int tenantId,
                                    RoleMapping[] roleMappings) throws SQLException,
            IdentityApplicationManagementException {

        Map<String, Integer> roleIdMap = new HashMap<String, Integer>();
        PreparedStatement prepStmt = null;
        ResultSet rs = null;

        // SP_IDP_ROLE_ID, SP_IDP_ROL
        String sqlStmt = IdPManagementConstants.SQLQueries.GET_IDP_ROLES_SQL;

        try {

            prepStmt = conn.prepareStatement(sqlStmt);
            prepStmt.setInt(1, idPId);
            rs = prepStmt.executeQuery();

            while (rs.next()) {
                int idpRoleId = rs.getInt("ID");
                String roleName = rs.getString("ROLE");
                roleIdMap.put(roleName, idpRoleId);
            }

            prepStmt.clearParameters();
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
            IdentityApplicationManagementUtil.closeResultSet(rs);

            if (roleIdMap.isEmpty()) {
                String message = "No Identity Provider roles defined for tenant " + tenantId;
                throw new IdentityApplicationManagementException(message);
            }

            sqlStmt = IdPManagementConstants.SQLQueries.ADD_IDP_ROLE_MAPPINGS_SQL;
            prepStmt = conn.prepareStatement(sqlStmt);

            for (RoleMapping mapping : roleMappings) {
                if (mapping.getRemoteRole() != null
                        && roleIdMap.containsKey(mapping.getRemoteRole())) {

                    int idpRoleId = roleIdMap.get(mapping.getRemoteRole());

                    String userStoreId = mapping.getLocalRole().getUserStoreId();
                    String localRole = mapping.getLocalRole().getLocalRoleName();

                    // SP_IDP_ROLE_ID, SP_TENANT_ID, SP_USER_STORE_ID, SP_LOCAL_ROLE
                    prepStmt.setInt(1, idpRoleId);
                    prepStmt.setInt(2, tenantId);
                    prepStmt.setString(3, CharacterEncoder.getSafeText(userStoreId));
                    prepStmt.setString(4, CharacterEncoder.getSafeText(localRole));
                    prepStmt.addBatch();
                } else {
                    String msg = "Cannot find Identity Provider role " + mapping.getRemoteRole()
                            + " for tenant " + tenantId;
                    throw new IdentityApplicationManagementException(msg);
                }
            }

            prepStmt.executeBatch();

        } finally {
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
            IdentityApplicationManagementUtil.closeResultSet(rs);
        }

    }

    /**
     * @param conn
     * @param idPId
     * @param tenantId
     * @param newClaimConfig
     * @throws SQLException
     * @throws IdentityApplicationManagementException
     */
    private void updateClaimConfiguration(Connection conn, int idPId, int tenantId,
                                          ClaimConfig newClaimConfig) throws SQLException,
            IdentityApplicationManagementException {

        // remove all identity provider claims - this will also remove associated claim mappings.
        deleteAllIdPClaims(conn, idPId);

        // delete local claim identity provider claim values.
        deleteLocalIdPClaimValues(conn, idPId, tenantId);

        if (newClaimConfig == null) {
            // bad data - we do not need.
            return;
        }

        if (newClaimConfig.isLocalClaimDialect()) {
            if (newClaimConfig.getClaimMappings() != null && newClaimConfig.getClaimMappings().length > 0) {
                // add claim mappings only.
                addDefaultClaimValuesForLocalIdP(conn, idPId, tenantId,
                        newClaimConfig.getClaimMappings());
            }
        } else {
            boolean addedClaims = false;
            if (newClaimConfig.getIdpClaims() != null && newClaimConfig.getIdpClaims().length > 0) {
                // add identity provider claims.
                addIdPClaims(conn, idPId, tenantId, newClaimConfig.getIdpClaims());
                addedClaims = true;
            }
            if (addedClaims && newClaimConfig.getClaimMappings() != null &&
                    newClaimConfig.getClaimMappings().length > 0) {
                // add identity provider claim mappings if and only if IdP claims are not empty.
                addIdPClaimMappings(conn, idPId, tenantId, newClaimConfig.getClaimMappings());
            }
        }
    }

    /**
     * @param conn
     * @param idPId
     * @param addedRoles
     * @param deletedRoles
     * @param renamedOldRoles
     * @param renamedNewRoles
     * @throws SQLException
     */
    private void updateIdPRoles(Connection conn, int idPId, List<String> addedRoles,
                                List<String> deletedRoles, List<String> renamedOldRoles, List<String> renamedNewRoles)
            throws SQLException {

        PreparedStatement prepStmt = null;
        String sqlStmt = null;

        try {

            for (String deletedRole : deletedRoles) {
                sqlStmt = IdPManagementConstants.SQLQueries.DELETE_IDP_ROLES_SQL;
                prepStmt = conn.prepareStatement(sqlStmt);
                prepStmt.setInt(1, idPId);
                prepStmt.setString(2, deletedRole);
                prepStmt.addBatch();
            }

            prepStmt.executeBatch();
            prepStmt.clearParameters();
            prepStmt.clearBatch();
            IdentityApplicationManagementUtil.closeStatement(prepStmt);

            for (String addedRole : addedRoles) {
                sqlStmt = IdPManagementConstants.SQLQueries.ADD_IDP_ROLES_SQL;
                prepStmt = conn.prepareStatement(sqlStmt);
                prepStmt.setInt(1, idPId);
                prepStmt.setString(2, CharacterEncoder.getSafeText(addedRole));
                prepStmt.addBatch();
            }

            prepStmt.executeBatch();
            prepStmt.clearParameters();
            prepStmt.clearBatch();
            IdentityApplicationManagementUtil.closeStatement(prepStmt);

            for (int i = 0; i < renamedOldRoles.size(); i++) {
                sqlStmt = IdPManagementConstants.SQLQueries.UPDATE_IDP_ROLES_SQL;
                prepStmt = conn.prepareStatement(sqlStmt);
                prepStmt.setString(1, CharacterEncoder.getSafeText(renamedNewRoles.get(i)));
                prepStmt.setInt(2, idPId);
                prepStmt.setString(3, CharacterEncoder.getSafeText(renamedOldRoles.get(i)));
                prepStmt.addBatch();
            }

            prepStmt.executeBatch();

        } finally {
            prepStmt.clearParameters();
            prepStmt.clearBatch();
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
        }

    }

    /**
     * @param conn
     * @param idPId
     * @param tenantId
     * @param newRoleConfiguration
     * @param newRoleConfiguration
     * @throws SQLException
     * @throws IdentityApplicationManagementException
     */
    private void updateRoleConfiguration(Connection conn, int idPId, int tenantId,
                                         PermissionsAndRoleConfig newRoleConfiguration) throws SQLException,
            IdentityApplicationManagementException {

        // delete all identity provider roles - this will also clean up idp role mappings.
        deleteAllIdPRoles(conn, idPId);

        if (newRoleConfiguration == null) {
            // bad data - we do not need to deal with.
            return;
        }

        // add identity provider roles.
        addIdPRoles(conn, idPId, tenantId, newRoleConfiguration.getIdpRoles());

        if (newRoleConfiguration.getRoleMappings() == null
                || newRoleConfiguration.getRoleMappings().length == 0) {
            // we do not have any role mappings in the system.
            return;
        }

        // add identity provider role mappings.
        addIdPRoleMappings(conn, idPId, tenantId, newRoleConfiguration.getRoleMappings());

    }

    /**
     * @param conn
     * @param conn
     * @param idPId
     * @throws SQLException
     */
    private void deleteProvisioningConnectorConfigs(Connection conn, int idPId) throws SQLException {

        PreparedStatement prepStmt = null;
        String sqlStmt = IdPManagementConstants.SQLQueries.DELETE_PROVISIONING_CONNECTORS;

        try {
            prepStmt = conn.prepareStatement(sqlStmt);
            prepStmt.setInt(1, idPId);
            prepStmt.executeUpdate();
        } finally {
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
        }
    }

    /**
     * @param conn
     * @param tenantId
     * @param idPName
     * @throws SQLException
     */
    private void deleteIdP(Connection conn, int tenantId, String idPName) throws SQLException {

        PreparedStatement prepStmt = null;
        String sqlStmt = IdPManagementConstants.SQLQueries.DELETE_IDP_SQL;

        try {
            prepStmt = conn.prepareStatement(sqlStmt);
            prepStmt.setInt(1, tenantId);
            prepStmt.setString(2, idPName);
            prepStmt.executeUpdate();
        } finally {
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
        }
    }

    /**
     * @param dbConnection
     * @param idpName
     * @param tenantId
     * @return
     * @throws SQLException
     * @throws IdentityApplicationManagementException
     */
    private int getIdentityProviderIdByName(Connection dbConnection, String idpName, int tenantId)
            throws SQLException, IdentityApplicationManagementException {

        boolean dbConnInitialized = true;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        try {
            if (dbConnection == null) {
                dbConnection = IdentityDatabaseUtil.getDBConnection();
            } else {
                dbConnInitialized = false;
            }
            String sqlStmt = IdPManagementConstants.SQLQueries.GET_IDP_ROW_ID_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, tenantId);
            prepStmt.setInt(2, MultitenantConstants.SUPER_TENANT_ID);
            prepStmt.setString(3, CharacterEncoder.getSafeText(idpName));
            rs = prepStmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (IdentityException e) {
            throw new IdentityApplicationManagementException("Error occurred while reading Identity Provider by name" +
                    ".", e);
        } finally {
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
            IdentityApplicationManagementUtil.closeResultSet(rs);
            if (dbConnInitialized) {
                IdentityApplicationManagementUtil.closeConnection(dbConnection);
            }
        }
        return 0;
    }

    /**
     * @param o1
     * @param o2
     * @return
     */
    private Property[] concatArrays(Property[] o1, Property[] o2) {
        Property[] ret = new Property[o1.length + o2.length];

        System.arraycopy(o1, 0, ret, 0, o1.length);
        System.arraycopy(o2, 0, ret, o1.length, o2.length);

        return ret;
    }

    /**
     * @param dbConnection
     * @param idPName
     * @param tenantId
     * @return
     * @throws SQLException
     * @throws IdentityApplicationManagementException
     */
    private int getIdentityProviderIdentifier(Connection dbConnection, String idPName, int tenantId)
            throws SQLException, IdentityApplicationManagementException {

        String sqlStmt = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        try {
            sqlStmt = IdPManagementConstants.SQLQueries.GET_IDP_BY_NAME_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, tenantId);
            prepStmt.setInt(2, MultitenantConstants.SUPER_TENANT_ID);
            prepStmt.setString(3, CharacterEncoder.getSafeText(idPName));
            rs = prepStmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("ID");
            } else {
                throw new IdentityApplicationManagementException("Invalid Identity Provider Name "
                        + idPName);
            }
        } finally {
            IdentityApplicationManagementUtil.closeResultSet(rs);
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
        }
    }

    private int getAuthenticatorIdentifier(Connection dbConnection, int idPId, String authnType)
            throws SQLException, IdentityApplicationManagementException {

        String sqlStmt = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        try {
            sqlStmt = IdPManagementConstants.SQLQueries.GET_IDP_AUTH_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, idPId);
            prepStmt.setString(2, CharacterEncoder.getSafeText(authnType));
            rs = prepStmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("ID");
            } else {
                throw new IdentityApplicationManagementException("Cannot find authenticator : "
                        + authnType);
            }
        } finally {
            IdentityApplicationManagementUtil.closeResultSet(rs);
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
        }
    }

    public boolean isSimilarIdPEntityIdsAvailble(String idPEntityId, int tenantId)
            throws IdentityApplicationManagementException {
        boolean isAvailable = false;
        Connection dbConnection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;

        try {
            dbConnection = IdentityDatabaseUtil.getDBConnection();
            String sqlStmt = IdPManagementConstants.SQLQueries.GET_SIMILAR_IDP_ENTITIY_IDS;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setString(1, "IdPEntityId");
            prepStmt.setString(2, idPEntityId);
            prepStmt.setInt(3, tenantId);
            rs = prepStmt.executeQuery();
            if (rs.next()) {
                isAvailable = rs.getInt(1) > 0;
            }
            dbConnection.commit();
        } catch (SQLException | IdentityException e) {
            log.error(e.getMessage(), e);
            String msg = "Error occurred while searching for similar IdP EntityIds";
            throw new IdentityApplicationManagementException(msg, e);
        } finally {
            if (prepStmt != null) {
                IdentityApplicationManagementUtil.closeStatement(prepStmt);
            }
            if (rs != null) {
                IdentityApplicationManagementUtil.closeResultSet(rs);
            }
            IdentityApplicationManagementUtil.closeConnection(dbConnection);
        }
        return isAvailable;
    }
}
