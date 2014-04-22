/*
 *Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.idp.mgt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementUtil;
import org.wso2.carbon.identity.application.common.model.Claim;
import org.wso2.carbon.identity.application.common.model.ClaimConfiguration;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.FacebookFederatedAuthenticator;
import org.wso2.carbon.identity.application.common.model.FederatedAuthenticator;
import org.wso2.carbon.identity.application.common.model.FederatedIdentityProvider;
import org.wso2.carbon.identity.application.common.model.JustInTimeProvisioningConfiguration;
import org.wso2.carbon.identity.application.common.model.LocalRole;
import org.wso2.carbon.identity.application.common.model.OpenIDConnectFederatedAuthenticator;
import org.wso2.carbon.identity.application.common.model.OpenIDFederatedAuthenticator;
import org.wso2.carbon.identity.application.common.model.PassiveSTSFederatedAuthenticator;
import org.wso2.carbon.identity.application.common.model.PermissionsAndRoleConfiguration;
import org.wso2.carbon.identity.application.common.model.ProvisioningConnector;
import org.wso2.carbon.identity.application.common.model.ProvisioningProperty;
import org.wso2.carbon.identity.application.common.model.ResidentIdentityProvider;
import org.wso2.carbon.identity.application.common.model.RoleMapping;
import org.wso2.carbon.identity.application.common.model.SAMLFederatedAuthenticator;
import org.wso2.carbon.idp.mgt.persistence.JDBCPersistenceManager;
import org.wso2.carbon.idp.mgt.util.IdPManagementConstants;

public class IdPManagementDAO {

    private static final Log log = LogFactory.getLog(IdPManagementDAO.class);

    /**
     * 
     * @param tenantId
     * @param tenantDomain
     * @return
     * @throws IdentityApplicationManagementException
     */
    public ResidentIdentityProvider getResidentIdP(int tenantId, String tenantDomain)
            throws IdentityApplicationManagementException {

        PreparedStatement prepStmt = null;
        Connection dbConnection = null;
        ResultSet rs = null;
        try {
            dbConnection = JDBCPersistenceManager.getInstance().getDBConnection();
            String sqlStmt = IdPManagementConstants.SQLQueries.GET_RESIDENT_IDP_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, tenantId);
            rs = prepStmt.executeQuery();
            if (rs.next()) {
                ResidentIdentityProvider identityProvider = new ResidentIdentityProvider();
                identityProvider.setHomeRealmId(rs.getString(1));
                identityProvider.setOpenIdRealm(rs.getString(2));
                identityProvider.setIdpEntityId(rs.getString(3));
                identityProvider.setPassiveSTSRealm(rs.getString(4));
                return identityProvider;
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            try {
                IdentityApplicationManagementUtil.rollBack(dbConnection);
            } catch (SQLException e1) {
                log.error("Error occurred while rolling back transactions: " + e1.getMessage(), e);
            }
            String msg = "Error occurred while retrieving Resident Identity Provider for tenant "
                    + tenantDomain;
            throw new IdentityApplicationManagementException(msg);
        } finally {
            try {
                IdentityApplicationManagementUtil.closeStatement(prepStmt);
                IdentityApplicationManagementUtil.closeResultSet(rs);
                IdentityApplicationManagementUtil.closeConnection(dbConnection);
            } catch (SQLException e) {
                log.error("Error occurred while closing connection: " + e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * 
     * @param identityProvider
     * @param tenantId
     * @param tenantDomain
     * @throws IdentityApplicationManagementException
     */
    public void addResidentIdP(ResidentIdentityProvider identityProvider, int tenantId,
            String tenantDomain) throws IdentityApplicationManagementException {

        PreparedStatement prepStmt = null;
        Connection dbConnection = null;
        try {
            dbConnection = JDBCPersistenceManager.getInstance().getDBConnection();
            String sqlStmt = IdPManagementConstants.SQLQueries.ADD_RESIDENT_IDP_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setString(1, identityProvider.getHomeRealmId());
            prepStmt.setString(2, identityProvider.getOpenIdRealm());
            prepStmt.setString(3, identityProvider.getIdpEntityId());
            prepStmt.setString(4, identityProvider.getPassiveSTSRealm());
            prepStmt.setInt(5, tenantId);
            prepStmt.executeUpdate();
            dbConnection.commit();
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            try {
                IdentityApplicationManagementUtil.rollBack(dbConnection);
            } catch (SQLException e1) {
                log.error("Error occurred while rolling back transactions: " + e1.getMessage(), e);
            }
            String msg = "Error occurred while adding Resident Identity Provider for tenant "
                    + tenantDomain;
            throw new IdentityApplicationManagementException(msg);
        } finally {
            try {
                IdentityApplicationManagementUtil.closeStatement(prepStmt);
                IdentityApplicationManagementUtil.closeConnection(dbConnection);
            } catch (SQLException e) {
                log.error("Error occurred while closing connection: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 
     * @param identityProvider
     * @param tenantId
     * @param tenantDomain
     * @throws IdentityApplicationManagementException
     */
    public void updateResidentIdP(ResidentIdentityProvider identityProvider, int tenantId,
            String tenantDomain) throws IdentityApplicationManagementException {

        PreparedStatement prepStmt = null;
        Connection dbConnection = null;
        try {
            dbConnection = JDBCPersistenceManager.getInstance().getDBConnection();
            String sqlStmt = IdPManagementConstants.SQLQueries.UPDATE_RESIDENT_IDP_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setString(1, identityProvider.getHomeRealmId());
            prepStmt.setString(2, identityProvider.getOpenIdRealm());
            prepStmt.setString(3, identityProvider.getIdpEntityId());
            prepStmt.setString(4, identityProvider.getPassiveSTSRealm());
            prepStmt.setInt(5, tenantId);
            prepStmt.executeUpdate();
            dbConnection.commit();
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            try {
                IdentityApplicationManagementUtil.rollBack(dbConnection);
            } catch (SQLException e1) {
                log.error("Error occurred while rolling back transactions: " + e1.getMessage(), e);
            }
            String msg = "Error occurred while updating Resident Identity Provider for tenant "
                    + tenantDomain;
            throw new IdentityApplicationManagementException(msg);
        } finally {
            try {
                IdentityApplicationManagementUtil.closeStatement(prepStmt);
                IdentityApplicationManagementUtil.closeConnection(dbConnection);
            } catch (SQLException e) {
                log.error("Error occurred while closing connection: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 
     * @param dbConnection
     * @param tenantId
     * @param tenantDomain
     * @return
     * @throws IdentityApplicationManagementException
     */
    public List<FederatedIdentityProvider> getIdPs(Connection dbConnection, int tenantId,
            String tenantDomain) throws IdentityApplicationManagementException {

        boolean dbConnInitialized = true;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        List<FederatedIdentityProvider> idps = new ArrayList<FederatedIdentityProvider>();
        try {
            if (dbConnection == null) {
                dbConnection = JDBCPersistenceManager.getInstance().getDBConnection();
            } else {
                dbConnInitialized = false;
            }
            String sqlStmt = IdPManagementConstants.SQLQueries.GET_IDPS_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, tenantId);
            rs = prepStmt.executeQuery();
            while (rs.next()) {
                FederatedIdentityProvider identityProvider = new FederatedIdentityProvider();
                identityProvider.setIdentityProviderName(rs.getString(1));
                if (rs.getString(2).equals("1")) {
                    identityProvider.setPrimary(true);
                } else {
                    identityProvider.setPrimary(false);
                }
                identityProvider.setHomeRealmId(rs.getString(3));
                idps.add(identityProvider);
            }
            return idps;
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            try {
                IdentityApplicationManagementUtil.rollBack(dbConnection);
            } catch (SQLException e1) {
                log.error("Error occurred while rolling back transactions: " + e1.getMessage(), e);
            }
            String msg = "Error occurred while retrieving registered Identity Provider Entity IDs "
                    + "for tenant " + tenantDomain;
            throw new IdentityApplicationManagementException(msg);
        } finally {
            if (dbConnInitialized) {
                try {
                    IdentityApplicationManagementUtil.closeStatement(prepStmt);
                    IdentityApplicationManagementUtil.closeResultSet(rs);
                    IdentityApplicationManagementUtil.closeConnection(dbConnection);
                } catch (SQLException e) {
                    log.error("Error occurred while closing connection: " + e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 
     * @param dbConnection
     * @param idPName
     * @param tenantId
     * @return
     * @throws IdentityApplicationManagementException
     * @throws SQLException
     */
    public FederatedAuthenticator[] getFederatedAuthenticators(Connection dbConnection,
            String idPName, int tenantId) throws IdentityApplicationManagementException,
            SQLException {
        return new FederatedAuthenticator[] {
                getSamlFederatedAuthenticator(dbConnection, idPName, tenantId),
                getOpenIDConnectFederatedAuthenticator(dbConnection, idPName, tenantId),
                getOpenIDFederatedAuthenticator(dbConnection, idPName, tenantId),
                getPassiveSTSFederatedAuthenticator(dbConnection, idPName, tenantId),
                getFacebookFederatedAuthenticator(dbConnection, idPName, tenantId) };

    }

    /**
     * 
     * @param newFederatedAuthenticator
     * @param oldFederatedAuthenticator
     * @param dbConnection
     * @param idpId
     * @param tenantId
     * @throws IdentityApplicationManagementException
     * @throws SQLException
     */
    public void updateFederatedAuthenticators(FederatedAuthenticator[] newFederatedAuthenticators,
            FederatedAuthenticator[] oldFederatedAuthenticators, Connection dbConnection,
            int idpId, int tenantId) throws IdentityApplicationManagementException, SQLException {

        OpenIDConnectFederatedAuthenticator currentOidcAuthenticator = null;
        OpenIDFederatedAuthenticator currentOpenidAuthenticator = null;
        SAMLFederatedAuthenticator currentSamlAuthenticator = null;
        PassiveSTSFederatedAuthenticator currentPassiveSTSAuthenticator = null;
        FacebookFederatedAuthenticator currentFbAuthenticator = null;

        if (oldFederatedAuthenticators != null && oldFederatedAuthenticators.length > 0) {
            for (FederatedAuthenticator fedAuthenticator : oldFederatedAuthenticators) {
                if (fedAuthenticator instanceof OpenIDConnectFederatedAuthenticator) {
                    currentOidcAuthenticator = (OpenIDConnectFederatedAuthenticator) fedAuthenticator;
                } else if (fedAuthenticator instanceof OpenIDFederatedAuthenticator) {
                    currentOpenidAuthenticator = (OpenIDFederatedAuthenticator) fedAuthenticator;
                } else if (fedAuthenticator instanceof SAMLFederatedAuthenticator) {
                    currentSamlAuthenticator = (SAMLFederatedAuthenticator) fedAuthenticator;
                } else if (fedAuthenticator instanceof PassiveSTSFederatedAuthenticator) {
                    currentPassiveSTSAuthenticator = (PassiveSTSFederatedAuthenticator) fedAuthenticator;
                } else if (fedAuthenticator instanceof FacebookFederatedAuthenticator) {
                    currentFbAuthenticator = (FacebookFederatedAuthenticator) fedAuthenticator;
                }
            }
        }

        if (newFederatedAuthenticators != null && newFederatedAuthenticators.length > 0) {
            for (FederatedAuthenticator fedAuthenticator : newFederatedAuthenticators) {
                if (fedAuthenticator instanceof OpenIDConnectFederatedAuthenticator) {
                    if (currentOidcAuthenticator != null && currentOidcAuthenticator.isValid()) {
                        // we already have an openidconnect authenticator in the system - update it.
                        updateOpenIDConnectFederatedAuthenticator(
                                (OpenIDConnectFederatedAuthenticator) fedAuthenticator,
                                dbConnection, idpId);
                    } else {
                        addOpenIDConnectFederatedAuthenticator(
                                (OpenIDConnectFederatedAuthenticator) fedAuthenticator,
                                dbConnection, idpId);
                    }
                } else if (fedAuthenticator instanceof OpenIDFederatedAuthenticator) {
                    if (currentOpenidAuthenticator != null && currentOpenidAuthenticator.isValid()) {
                        // we already have an openid authenticator in the system - update it.
                        updateOpenIDFederatedAuthenticator(
                                (OpenIDFederatedAuthenticator) fedAuthenticator, dbConnection,
                                idpId);
                    } else {
                        // none exists - add.
                        addOpenIDFederatedAuthenticator(
                                (OpenIDFederatedAuthenticator) fedAuthenticator, dbConnection,
                                idpId);
                    }
                } else if (fedAuthenticator instanceof SAMLFederatedAuthenticator) {
                    if (currentSamlAuthenticator != null && currentSamlAuthenticator.isValid()) {
                        // we already have an saml authenticator in the system - update it.
                        updateSamlFederatedAuthenticator(
                                (SAMLFederatedAuthenticator) fedAuthenticator, dbConnection, idpId);
                    } else {
                        // none exists - add.
                        addSamlFederatedAuthenticator(
                                (SAMLFederatedAuthenticator) fedAuthenticator, dbConnection, idpId);
                    }
                } else if (fedAuthenticator instanceof PassiveSTSFederatedAuthenticator) {
                    if (currentPassiveSTSAuthenticator != null
                            && currentPassiveSTSAuthenticator.isValid()) {
                        // we already have a passive sts authenticator in the system - update it.
                        updatePassiveSTSFederatedAuthenticator(
                                (PassiveSTSFederatedAuthenticator) fedAuthenticator, dbConnection,
                                idpId);
                    } else {
                        // none exists - add.
                        addPassiveSTSFederatedAuthenticator(
                                (PassiveSTSFederatedAuthenticator) fedAuthenticator, dbConnection,
                                idpId);
                    }
                } else if (fedAuthenticator instanceof FacebookFederatedAuthenticator) {
                    if (currentFbAuthenticator != null && currentFbAuthenticator.isValid()) {
                        // we already have an facebook authenticator in the system - update it.
                        updateFacebookFederatedAuthenticator(
                                (FacebookFederatedAuthenticator) fedAuthenticator, dbConnection,
                                idpId);
                    } else {
                        // none exists - add.
                        addFacebookFederatedAuthenticator(
                                (FacebookFederatedAuthenticator) fedAuthenticator, dbConnection,
                                idpId);
                    }

                }
            }
        }

    }

    /**
     * 
     * @param federatedAuthenticators
     * @param dbConnection
     * @param idpId
     * @param tenantId
     * @throws IdentityApplicationManagementException
     * @throws SQLException
     */
    public void addFederatedAuthenticators(FederatedAuthenticator[] federatedAuthenticators,
            Connection dbConnection, int idpId, int tenantId)
            throws IdentityApplicationManagementException, SQLException {

        if (federatedAuthenticators != null && federatedAuthenticators.length > 0) {
            for (FederatedAuthenticator fedAuthenticator : federatedAuthenticators) {
                if (fedAuthenticator instanceof OpenIDConnectFederatedAuthenticator) {
                    addOpenIDConnectFederatedAuthenticator(
                            (OpenIDConnectFederatedAuthenticator) fedAuthenticator, dbConnection,
                            idpId);
                } else if (fedAuthenticator instanceof OpenIDFederatedAuthenticator) {
                    addOpenIDFederatedAuthenticator(
                            (OpenIDFederatedAuthenticator) fedAuthenticator, dbConnection, idpId);

                } else if (fedAuthenticator instanceof SAMLFederatedAuthenticator) {
                    addSamlFederatedAuthenticator((SAMLFederatedAuthenticator) fedAuthenticator,
                            dbConnection, idpId);

                } else if (fedAuthenticator instanceof PassiveSTSFederatedAuthenticator) {
                    addPassiveSTSFederatedAuthenticator(
                            (PassiveSTSFederatedAuthenticator) fedAuthenticator, dbConnection,
                            idpId);

                } else if (fedAuthenticator instanceof FacebookFederatedAuthenticator) {
                    addFacebookFederatedAuthenticator(
                            (FacebookFederatedAuthenticator) fedAuthenticator, dbConnection, idpId);

                }
            }
        }

    }

    /**
     * 
     * @param dbConnection
     * @param idPName
     * @param tenantId
     * @return
     * @throws IdentityApplicationManagementException
     * @throws SQLException
     */
    public ClaimConfiguration getIdPClaimConfiguration(Connection dbConnection, String idPName,
            String userClaimUri, String roleClaimUri, int tenantId)
            throws IdentityApplicationManagementException, SQLException {

        int idPId = getIdentityProviderIdentifier(dbConnection, idPName, tenantId);

        PreparedStatement prepStmt = null;
        ResultSet rs = null;

        try {

            List<Claim> claimList = new ArrayList<Claim>();
            // SP_IDP_CLAIM_ID, SP_IDP_CLAIM
            String sqlStmt = IdPManagementConstants.SQLQueries.GET_IDP_CLAIMS_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, idPId);
            rs = prepStmt.executeQuery();

            ClaimConfiguration claimConfig = new ClaimConfiguration();

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
                idpClaim.setClaimUri(rs.getString(1));

                Claim localClaim = new Claim();
                localClaim.setClaimUri(rs.getString(2));

                claimMapping.setLocalClaim(localClaim);
                claimMapping.setIdpClaim(idpClaim);
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
     * 
     * @param dbConnection
     * @param idPName
     * @param tenantId
     * @return
     * @throws IdentityApplicationManagementException
     * @throws SQLException
     */
    public PermissionsAndRoleConfiguration getPermissionsAndRoleConfiguration(
            Connection dbConnection, String idPName, int tenantId)
            throws IdentityApplicationManagementException, SQLException {

        int idPId = getIdentityProviderIdentifier(dbConnection, idPName, tenantId);

        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        PermissionsAndRoleConfiguration permissionRoleConfiguration = new PermissionsAndRoleConfiguration();

        try {

            List<String> idpRoleList = new ArrayList<String>();
            // SP_IDP_ROLE
            String sqlStmt = IdPManagementConstants.SQLQueries.GET_IDP_ROLES_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, idPId);
            rs = prepStmt.executeQuery();
            while (rs.next()) {
                idpRoleList.add(rs.getString(2));
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
                LocalRole localRole = new LocalRole(rs.getString(1), rs.getString(2));
                RoleMapping roleMapping = new RoleMapping(localRole, rs.getString(3));
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
     * 
     * @param dbConnection
     * @param idPName
     * @param tenantId
     * @return
     * @throws IdentityApplicationManagementException
     * @throws SQLException
     */
    public FacebookFederatedAuthenticator getFacebookFederatedAuthenticator(
            Connection dbConnection, String idPName, int tenantId)
            throws IdentityApplicationManagementException, SQLException {

        int idPId = getIdentityProviderIdentifier(dbConnection, idPName, tenantId);

        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        FacebookFederatedAuthenticator facebookAuthenticator = new FacebookFederatedAuthenticator();

        try {
            // SP_IDP_FB_AUTH_ENABLED,SP_IDP_FB_CLIENT_ID, SP_IDP_FB_CLIENT_SECRET,
            // SP_IDP_IS_FB_UID_IN_CLAIMS
            String sqlStmt = IdPManagementConstants.SQLQueries.GET_IDP_FB_AUTH_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, idPId);
            rs = prepStmt.executeQuery();
            if (rs.next()) {
                if ("1".equals(rs.getString(1))) {
                    facebookAuthenticator.setEnabled(true);
                } else {
                    facebookAuthenticator.setEnabled(false);
                }

                facebookAuthenticator.setClientId(rs.getString(2));
                facebookAuthenticator.setClientSecret(rs.getString(3));

                if ("1".equals(rs.getString(4))) {
                    facebookAuthenticator.setUsetIdInClaim(true);
                } else {
                    facebookAuthenticator.setUsetIdInClaim(false);
                }
            }

            return facebookAuthenticator;
        } finally {
            IdentityApplicationManagementUtil.closeResultSet(rs);
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
        }
    }

    /**
     * 
     * @param dbConnection
     * @param idPName
     * @param tenantId
     * @return
     * @throws IdentityApplicationManagementException
     * @throws SQLException
     */
    public OpenIDFederatedAuthenticator getOpenIDFederatedAuthenticator(Connection dbConnection,
            String idPName, int tenantId) throws IdentityApplicationManagementException,
            SQLException {

        int idPId = getIdentityProviderIdentifier(dbConnection, idPName, tenantId);

        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        OpenIDFederatedAuthenticator openIDAuthenticator = new OpenIDFederatedAuthenticator();

        try {
            // SP_IDP_OPENID_ENABLED,SP_IDP_OPENID_URL,SP_IDP_OPENID_UID_IN_CLAIMS
            String sqlStmt = IdPManagementConstants.SQLQueries.GET_IDP_OPENID_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, idPId);
            rs = prepStmt.executeQuery();
            if (rs.next()) {
                if (rs.getString(1).equals("1")) {
                    openIDAuthenticator.setEnabled(true);
                } else {
                    openIDAuthenticator.setEnabled(false);
                }

                openIDAuthenticator.setOpenIDServerUrl(rs.getString(2));

                if (rs.getString(3).equals("1")) {
                    openIDAuthenticator.setUsetIdInClaim(true);
                } else {
                    openIDAuthenticator.setUsetIdInClaim(false);
                }
            }

            return openIDAuthenticator;
        } finally {
            IdentityApplicationManagementUtil.closeResultSet(rs);
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
        }
    }

    /**
     * 
     * @param dbConnection
     * @param idPName
     * @param tenantId
     * @return
     * @throws IdentityApplicationManagementException
     * @throws SQLException
     */
    public PassiveSTSFederatedAuthenticator getPassiveSTSFederatedAuthenticator(
            Connection dbConnection, String idPName, int tenantId)
            throws IdentityApplicationManagementException, SQLException {

        int idPId = getIdentityProviderIdentifier(dbConnection, idPName, tenantId);

        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        PassiveSTSFederatedAuthenticator passiveSTSAuthenticator = new PassiveSTSFederatedAuthenticator();

        try {

            // SP_IDP_PASSIVE_STS_ENABLED,SP_IDP_PASSIVE_STS_REALM,
            // SP_IDP_PASSIVE_STS_URL,SP_IDP_PSVE_STS_UID_IN_CLAIMS
            String sqlStmt = IdPManagementConstants.SQLQueries.GET_IDP_PASSIVE_STS_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, idPId);
            rs = prepStmt.executeQuery();

            if (rs.next()) {
                if (rs.getString(1).equals("1")) {
                    passiveSTSAuthenticator.setEnabled(true);
                } else {
                    passiveSTSAuthenticator.setEnabled(false);
                }

                passiveSTSAuthenticator.setPassiveSTSRealm(rs.getString(2));
                passiveSTSAuthenticator.setPassiveSTSUrl(rs.getString(3));

                if (rs.getString(4).equals("1")) {
                    passiveSTSAuthenticator.setUsetIdInClaim(true);
                } else {
                    passiveSTSAuthenticator.setUsetIdInClaim(false);
                }
            }

            return passiveSTSAuthenticator;
        } finally {
            IdentityApplicationManagementUtil.closeResultSet(rs);
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
        }
    }

    /**
     * 
     * @param dbConnection
     * @param idPName
     * @param tenantId
     * @return
     * @throws IdentityApplicationManagementException
     * @throws SQLException
     */
    public SAMLFederatedAuthenticator getSamlFederatedAuthenticator(Connection dbConnection,
            String idPName, int tenantId) throws IdentityApplicationManagementException,
            SQLException {

        int idPId = getIdentityProviderIdentifier(dbConnection, idPName, tenantId);

        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        SAMLFederatedAuthenticator samlAuthenticator = new SAMLFederatedAuthenticator();

        try {
            // SP_IDP_SAMLSSO_ENABLED, SP_IDP_IDP_ENTITY_ID,
            // SP_IDP_SP_ENTITY_ID, SP_IDP_SSO_URL, SP_IDP_AUTHN_REQ_SIGNED, SP_IDP_LOGOUT_ENABLED,
            // SP_IDP_LOGOUT_URL, SP_IDP_LOGOUT_REQ_SIGNED, SP_IDP_AUTHN_RES_SIGNED,
            // SP_IDP_SAMLSSO_UID_IN_CLAIMS
            String sqlStmt = IdPManagementConstants.SQLQueries.GET_IDP_SAMLSSO_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, idPId);
            rs = prepStmt.executeQuery();

            if (rs.next()) {
                if (rs.getString(1).equals("1")) {
                    samlAuthenticator.setEnabled(true);
                } else {
                    samlAuthenticator.setEnabled(false);
                }

                samlAuthenticator.setIdpEntityId(rs.getString(2));
                samlAuthenticator.setSpEntityId(rs.getString(3));
                samlAuthenticator.setSaml2SSOUrl(rs.getString(4));

                if (rs.getString(5).equals("1")) {
                    samlAuthenticator.setAuthnRequestSigned(true);
                } else {
                    samlAuthenticator.setAuthnRequestSigned(false);
                }
                if (rs.getString(6).equals("1")) {
                    samlAuthenticator.setLogoutEnabled(true);
                } else {
                    samlAuthenticator.setLogoutEnabled(false);
                }

                samlAuthenticator.setLogoutRequestUrl(rs.getString(7));

                if (rs.getString(8).equals("1")) {
                    samlAuthenticator.setLogoutRequestSigned(true);
                } else {
                    samlAuthenticator.setLogoutRequestSigned(false);
                }
                if (rs.getString(9).equals("1")) {
                    samlAuthenticator.setAuthnResponseSigned(true);
                } else {
                    samlAuthenticator.setAuthnResponseSigned(false);
                }
                if (rs.getString(10).equals("1")) {
                    samlAuthenticator.setUsetIdInClaim(true);
                } else {
                    samlAuthenticator.setUsetIdInClaim(false);
                }
            }

            return samlAuthenticator;
        } finally {
            IdentityApplicationManagementUtil.closeResultSet(rs);
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
        }
    }

    /**
     * 
     * @param openidAuthenticator
     * @param dbConnection
     * @param idPName
     * @param tenantId
     * @throws IdentityApplicationManagementException
     * @throws SQLException
     */
    private void addOpenIDFederatedAuthenticator(OpenIDFederatedAuthenticator openidAuthenticator,
            Connection dbConnection, int idpId) throws IdentityApplicationManagementException,
            SQLException {

        PreparedStatement prepStmt = null;
        ResultSet rs = null;

        try {
            // SP_IDP_ID, SP_IDP_OPENID_ENABLED,
            // SP_IDP_OPENID_URL,SP_IDP_OPENID_UID_IN_CLAIMS
            String sqlStmt = IdPManagementConstants.SQLQueries.ADD_IDP_OPENID_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, idpId);

            if (openidAuthenticator.isEnabled()) {
                prepStmt.setString(2, "1");
            } else {
                prepStmt.setString(2, "0");
            }

            prepStmt.setString(3, openidAuthenticator.getOpenIDServerUrl());

            if (openidAuthenticator.isUsetIdInClaim()) {
                prepStmt.setString(4, "1");
            } else {
                prepStmt.setString(4, "0");
            }

            prepStmt.executeUpdate();

        } finally {
            IdentityApplicationManagementUtil.closeResultSet(rs);
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
        }

    }

    /**
     * 
     * @param openidAuthenticator
     * @param dbConnection
     * @param idPName
     * @param idpId
     * @throws IdentityApplicationManagementException
     * @throws SQLException
     */
    private void updateOpenIDFederatedAuthenticator(
            OpenIDFederatedAuthenticator openidAuthenticator, Connection dbConnection, int idpId)
            throws IdentityApplicationManagementException, SQLException {

        PreparedStatement prepStmt = null;
        ResultSet rs = null;

        try {
            // SP_IDP_OPENID_ENABLED=?,SP_IDP_OPENID_URL=?,SP_IDP_OPENID_UID_IN_CLAIMS=?
            String sqlStmt = IdPManagementConstants.SQLQueries.UPDATE_IDP_OPENID_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);

            if (openidAuthenticator.isEnabled()) {
                prepStmt.setString(1, "1");
            } else {
                prepStmt.setString(1, "0");
            }

            prepStmt.setString(2, openidAuthenticator.getOpenIDServerUrl());

            if (openidAuthenticator.isUsetIdInClaim()) {
                prepStmt.setString(3, "1");
            } else {
                prepStmt.setString(3, "0");
            }
            prepStmt.setInt(4, idpId);
            prepStmt.executeUpdate();

        } finally {
            IdentityApplicationManagementUtil.closeResultSet(rs);
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
        }

    }

    /**
     * 
     * @param samlAuthenticator
     * @param dbConnection
     * @param idpId
     * @throws IdentityApplicationManagementException
     * @throws SQLException
     */
    private void updateSamlFederatedAuthenticator(SAMLFederatedAuthenticator samlAuthenticator,
            Connection dbConnection, int idpId) throws IdentityApplicationManagementException,
            SQLException {
        PreparedStatement prepStmt = null;
        ResultSet rs = null;

        try {

            // SP_IDP_SAMLSSO SET SP_IDP_SAMLSSO_ENABLED=?,SP_IDP_IDP_ENTITY_ID=?,
            // SP_IDP_SP_ENTITY_ID=?, SP_IDP_SSO_URL=?, SP_IDP_AUTHN_REQ_SIGNED=?,
            // SP_IDP_LOGOUT_ENABLED=?, SP_IDP_LOGOUT_URL=?,
            // SP_IDP_LOGOUT_REQ_SIGNED=?, SP_IDP_AUTHN_RES_SIGNED=?, SP_IDP_SAMLSSO_UID_IN_CLAIMS=?
            String sqlStmt = IdPManagementConstants.SQLQueries.UPDATE_IDP_SAMLSSO_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);

            if (samlAuthenticator.isEnabled()) {
                prepStmt.setString(1, "1");
            } else {
                prepStmt.setString(1, "0");
            }

            prepStmt.setString(2, samlAuthenticator.getIdpEntityId());
            prepStmt.setString(3, samlAuthenticator.getSpEntityId());
            prepStmt.setString(4, samlAuthenticator.getSaml2SSOUrl());

            if (samlAuthenticator.isAuthnRequestSigned()) {
                prepStmt.setString(5, "1");
            } else {
                prepStmt.setString(5, "0");
            }
            if (samlAuthenticator.isLogoutEnabled()) {
                prepStmt.setString(6, "1");
            } else {
                prepStmt.setString(6, "0");
            }
            prepStmt.setString(7, samlAuthenticator.getLogoutRequestUrl());

            if (samlAuthenticator.isLogoutRequestSigned()) {
                prepStmt.setString(8, "1");
            } else {
                prepStmt.setString(8, "0");
            }

            if (samlAuthenticator.isAuthnResponseSigned()) {
                prepStmt.setString(9, "1");
            } else {
                prepStmt.setString(9, "0");
            }

            if (samlAuthenticator.isUsetIdInClaim()) {
                prepStmt.setString(10, "1");
            } else {
                prepStmt.setString(10, "0");
            }

            prepStmt.setInt(11, idpId);
            prepStmt.executeUpdate();

        } finally {
            IdentityApplicationManagementUtil.closeResultSet(rs);
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
        }
    }

    /**
     * 
     * @param samlAuthenticator
     * @param dbConnection
     * @param idPName
     * @param tenantId
     * @throws IdentityApplicationManagementException
     * @throws SQLException
     */
    private void addSamlFederatedAuthenticator(SAMLFederatedAuthenticator samlAuthenticator,
            Connection dbConnection, int idpId) throws IdentityApplicationManagementException,
            SQLException {
        PreparedStatement prepStmt = null;
        ResultSet rs = null;

        try {
            // SP_IDP_ID, SP_IDP_SAMLSSO_ENABLED, SP_IDP_IDP_ENTITY_ID, SP_IDP_SP_ENTITY_ID,
            // SP_IDP_SSO_URL,SP_IDP_AUTHN_REQ_SIGNED, SP_IDP_LOGOUT_ENABLED, SP_IDP_LOGOUT_URL,
            // SP_IDP_LOGOUT_REQ_SIGNED,SP_IDP_AUTHN_RES_SIGNED, SP_IDP_SAMLSSO_UID_IN_CLAIMS
            String sqlStmt = IdPManagementConstants.SQLQueries.ADD_IDP_SAMLSSO_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, idpId);

            if (samlAuthenticator.isEnabled()) {
                prepStmt.setString(2, "1");
            } else {
                prepStmt.setString(2, "0");
            }

            prepStmt.setString(3, samlAuthenticator.getIdpEntityId());
            prepStmt.setString(4, samlAuthenticator.getSpEntityId());
            prepStmt.setString(5, samlAuthenticator.getSaml2SSOUrl());

            if (samlAuthenticator.isAuthnRequestSigned()) {
                prepStmt.setString(6, "1");
            } else {
                prepStmt.setString(6, "0");
            }
            if (samlAuthenticator.isLogoutEnabled()) {
                prepStmt.setString(7, "1");
            } else {
                prepStmt.setString(7, "0");
            }
            prepStmt.setString(8, samlAuthenticator.getLogoutRequestUrl());

            if (samlAuthenticator.isLogoutRequestSigned()) {
                prepStmt.setString(9, "1");
            } else {
                prepStmt.setString(9, "0");
            }
            if (samlAuthenticator.isAuthnResponseSigned()) {
                prepStmt.setString(10, "1");
            } else {
                prepStmt.setString(10, "0");
            }
            if (samlAuthenticator.isUsetIdInClaim()) {
                prepStmt.setString(11, "1");
            } else {
                prepStmt.setString(11, "0");
            }
            prepStmt.executeUpdate();

        } finally {
            IdentityApplicationManagementUtil.closeResultSet(rs);
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
        }
    }

    /**
     * 
     * @param passiveSTSAuthenticator
     * @param dbConnection
     * @param idpId
     * @throws IdentityApplicationManagementException
     * @throws SQLException
     */
    private void addPassiveSTSFederatedAuthenticator(
            PassiveSTSFederatedAuthenticator passiveSTSAuthenticator, Connection dbConnection,
            int idpId) throws IdentityApplicationManagementException, SQLException {
        PreparedStatement prepStmt = null;
        ResultSet rs = null;

        try {
            // SP_IDP_ID, SP_IDP_PASSIVE_STS_ENABLED,
            // SP_IDP_PASSIVE_STS_REALM,SP_IDP_PASSIVE_STS_URL, SP_IDP_PSVE_STS_UID_IN_CLAIMS
            String sqlStmt = IdPManagementConstants.SQLQueries.ADD_IDP_PASSIVE_STS_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, idpId);

            if (passiveSTSAuthenticator.isEnabled()) {
                prepStmt.setString(2, "1");
            } else {
                prepStmt.setString(2, "0");
            }

            prepStmt.setString(3, passiveSTSAuthenticator.getPassiveSTSRealm());
            prepStmt.setString(4, passiveSTSAuthenticator.getPassiveSTSUrl());

            if (passiveSTSAuthenticator.isUsetIdInClaim()) {
                prepStmt.setString(5, "1");
            } else {
                prepStmt.setString(5, "0");
            }
            prepStmt.executeUpdate();
        } finally {
            IdentityApplicationManagementUtil.closeResultSet(rs);
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
        }
    }

    /**
     * 
     * @param passiveSTSAuthenticator
     * @param dbConnection
     * @param idpId
     * @throws IdentityApplicationManagementException
     * @throws SQLException
     */
    private void updatePassiveSTSFederatedAuthenticator(
            PassiveSTSFederatedAuthenticator passiveSTSAuthenticator, Connection dbConnection,
            int idpId) throws IdentityApplicationManagementException, SQLException {
        PreparedStatement prepStmt = null;
        ResultSet rs = null;

        try {
            // SP_IDP_PASSIVE_STS_ENABLED=?,SP_IDP_PASSIVE_STS_REALM=?, SP_IDP_PASSIVE_STS_URL=?,
            // SP_IDP_PSVE_STS_UID_IN_CLAIMS=?
            String sqlStmt = IdPManagementConstants.SQLQueries.UPDATE_IDP_PASSIVE_STS_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);

            if (passiveSTSAuthenticator.isEnabled()) {
                prepStmt.setString(1, "1");
            } else {
                prepStmt.setString(1, "0");
            }

            prepStmt.setString(2, passiveSTSAuthenticator.getPassiveSTSRealm());
            prepStmt.setString(3, passiveSTSAuthenticator.getPassiveSTSUrl());
            if (passiveSTSAuthenticator.isUsetIdInClaim()) {
                prepStmt.setString(4, "1");
            } else {
                prepStmt.setString(4, "0");
            }
            prepStmt.setInt(4, idpId);
            prepStmt.executeUpdate();

        } finally {
            IdentityApplicationManagementUtil.closeResultSet(rs);
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
        }
    }

    /**
     * 
     * @param facebookAuthenticator
     * @param dbConnection
     * @param idpId
     * @throws IdentityApplicationManagementException
     * @throws SQLException
     */
    private void updateFacebookFederatedAuthenticator(
            FacebookFederatedAuthenticator facebookAuthenticator, Connection dbConnection, int idpId)
            throws IdentityApplicationManagementException, SQLException {

        PreparedStatement prepStmt = null;
        ResultSet rs = null;

        try {
            // SP_IDP_FB_AUTH_ENABLED=?,SP_IDP_FB_CLIENT_ID=?, SP_IDP_FB_CLIENT_SECRET=?,
            // SP_IDP_IS_FB_UID_IN_CLAIMS=?
            String sqlStmt = IdPManagementConstants.SQLQueries.UPDATE_IDP_FB_AUTH_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);

            if (facebookAuthenticator.isEnabled()) {
                prepStmt.setString(1, "1");
            } else {
                prepStmt.setString(1, "0");
            }

            prepStmt.setString(2, facebookAuthenticator.getClientId());
            prepStmt.setString(3, facebookAuthenticator.getClientSecret());

            if (facebookAuthenticator.isUsetIdInClaim()) {
                prepStmt.setString(4, "1");
            } else {
                prepStmt.setString(4, "0");
            }
            prepStmt.setInt(5, idpId);
            prepStmt.executeUpdate();

        } finally {
            IdentityApplicationManagementUtil.closeResultSet(rs);
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
        }
    }

    /**
     * 
     * @param passiveSTSAuthenticator
     * @param dbConnection
     * @param idPName
     * @param tenantId
     * @throws IdentityApplicationManagementException
     * @throws SQLException
     */
    private void addFacebookFederatedAuthenticator(
            FacebookFederatedAuthenticator facebookAuthenticator, Connection dbConnection, int idpId)
            throws IdentityApplicationManagementException, SQLException {

        PreparedStatement prepStmt = null;
        ResultSet rs = null;

        try {
            // SP_IDP_ID, SP_IDP_FB_AUTH_ENABLED, SP_IDP_FB_CLIENT_ID, SP_IDP_FB_CLIENT_SECRET,
            // SP_IDP_IS_FB_UID_IN_CLAIMS
            String sqlStmt = IdPManagementConstants.SQLQueries.ADD_IDP_FB_AUTH_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, idpId);

            if (facebookAuthenticator.isEnabled()) {
                prepStmt.setString(2, "1");
            } else {
                prepStmt.setString(2, "0");
            }

            prepStmt.setString(3, facebookAuthenticator.getClientId());
            prepStmt.setString(4, facebookAuthenticator.getClientSecret());
            if (facebookAuthenticator.isUsetIdInClaim()) {
                prepStmt.setString(5, "1");
            } else {
                prepStmt.setString(5, "0");
            }
            prepStmt.executeUpdate();
        } finally {
            IdentityApplicationManagementUtil.closeResultSet(rs);
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
        }
    }

    /**
     * 
     * @param oidcAuthenticator
     * @param dbConnection
     * @param idpId
     * @throws IdentityApplicationManagementException
     * @throws SQLException
     */
    private void updateOpenIDConnectFederatedAuthenticator(
            OpenIDConnectFederatedAuthenticator oidcAuthenticator, Connection dbConnection,
            int idpId) throws IdentityApplicationManagementException, SQLException {
        PreparedStatement prepStmt = null;
        ResultSet rs = null;

        try {

            // SP_IDP_OIDC_ENABLED=?, SP_IDP_CONSUMER_KEY=?, SP_IDP_CONSUMER_SECRET=?,
            // SP_IDP_AUTHZ_URL=?,SP_IDP_TOKEN_URL=?, SP_IDP_IS_OIDC_UID_IN_CLAIMS=?
            String sqlStmt = IdPManagementConstants.SQLQueries.UPDATE_IDP_OIDC_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);

            if (oidcAuthenticator.isEnabled()) {
                prepStmt.setString(1, "1");
            } else {
                prepStmt.setString(1, "0");
            }

            prepStmt.setString(2, oidcAuthenticator.getClientId());
            prepStmt.setString(3, oidcAuthenticator.getClientSecret());
            prepStmt.setString(4, oidcAuthenticator.getAuthzEndpointUrl());
            prepStmt.setString(5, oidcAuthenticator.getTokenEndpointUrl());

            if (oidcAuthenticator.isUsetIdInClaim()) {
                prepStmt.setString(6, "1");
            } else {
                prepStmt.setString(6, "0");
            }
            prepStmt.setInt(7, idpId);
            prepStmt.executeUpdate();
        } finally {
            IdentityApplicationManagementUtil.closeResultSet(rs);
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
        }
    }

    /**
     * 
     * @param oidcAuthenticator
     * @param dbConnection
     * @param idPName
     * @param tenantId
     * @throws IdentityApplicationManagementException
     * @throws SQLException
     */
    private void addOpenIDConnectFederatedAuthenticator(
            OpenIDConnectFederatedAuthenticator oidcAuthenticator, Connection dbConnection,
            int idpId) throws IdentityApplicationManagementException, SQLException {
        PreparedStatement prepStmt = null;
        ResultSet rs = null;

        try {
            // SP_IDP_ID, SP_IDP_OIDC_ENABLED, SP_IDP_CONSUMER_KEY, SP_IDP_CONSUMER_SECRET,
            // SP_IDP_AUTHZ_URL,SP_IDP_TOKEN_URL, SP_IDP_IS_OIDC_UID_IN_CLAIMS
            String sqlStmt = IdPManagementConstants.SQLQueries.ADD_IDP_OIDC_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, idpId);

            if (oidcAuthenticator.isEnabled()) {
                prepStmt.setString(2, "1");
            } else {
                prepStmt.setString(2, "0");
            }

            prepStmt.setString(3, oidcAuthenticator.getClientId());
            prepStmt.setString(4, oidcAuthenticator.getClientSecret());
            prepStmt.setString(5, oidcAuthenticator.getAuthzEndpointUrl());
            prepStmt.setString(6, oidcAuthenticator.getTokenEndpointUrl());

            if (oidcAuthenticator.isUsetIdInClaim()) {
                prepStmt.setString(7, "1");
            } else {
                prepStmt.setString(7, "0");
            }
            prepStmt.executeUpdate();
        } finally {
            IdentityApplicationManagementUtil.closeResultSet(rs);
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
        }
    }

    /**
     * 
     * @param dbConnection
     * @param idPName
     * @param tenantId
     * @return
     * @throws IdentityApplicationManagementException
     * @throws SQLException
     */
    public OpenIDConnectFederatedAuthenticator getOpenIDConnectFederatedAuthenticator(
            Connection dbConnection, String idPName, int tenantId)
            throws IdentityApplicationManagementException, SQLException {

        int idPId = getIdentityProviderIdentifier(dbConnection, idPName, tenantId);

        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        OpenIDConnectFederatedAuthenticator oidcAuthenticator = new OpenIDConnectFederatedAuthenticator();

        try {
            // SP_IDP_OIDC_ENABLED,SP_IDP_CONSUMER_KEY, SP_IDP_CONSUMER_SECRET, SP_IDP_AUTHZ_URL,
            // SP_IDP_TOKEN_URL,SP_IDP_IS_OIDC_UID_IN_CLAIMS
            String sqlStmt = IdPManagementConstants.SQLQueries.GET_IDP_OIDC_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, idPId);
            rs = prepStmt.executeQuery();
            if (rs.next()) {
                if (rs.getString(1).equals("1")) {
                    oidcAuthenticator.setEnabled(true);
                } else {
                    oidcAuthenticator.setEnabled(false);
                }

                oidcAuthenticator.setClientId(rs.getString(2));
                oidcAuthenticator.setClientSecret(rs.getString(3));
                oidcAuthenticator.setAuthzEndpointUrl(rs.getString(4));
                oidcAuthenticator.setTokenEndpointUrl(rs.getString(5));

                if (rs.getString(6).equals("1")) {
                    oidcAuthenticator.setUsetIdInClaim(true);
                } else {
                    oidcAuthenticator.setUsetIdInClaim(false);
                }
            }

            return oidcAuthenticator;
        } finally {
            IdentityApplicationManagementUtil.closeResultSet(rs);
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
        }
    }

    /**
     * 
     * @param provisioningConnector
     * @param dbConnection
     * @param idpId
     * @throws IdentityApplicationManagementException
     * @throws SQLException
     */
    private void addProvisioningConnectors(ProvisioningConnector[] provisioningConnectors,
            Connection dbConnection, int idpId) throws IdentityApplicationManagementException,
            SQLException {

        PreparedStatement prepStmt = null;
        ResultSet rs = null;

        try {
            // SP_IDP_ID,SP_IDP_PROV_CONNECTOR_TYPE, SP_IDP_PROV_CONFIG_KEY,
            // SP_IDP_PROV_CONFIG_VALUE, SP_IDP_PROV_CONFIG_IS_SECRET
            String sqlStmt = IdPManagementConstants.SQLQueries.ADD_IDP_PROVISIONING_CONFIG_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);

            if (provisioningConnectors != null) {
                for (ProvisioningConnector connector : provisioningConnectors) {
                    ProvisioningProperty[] connctorProperties = connector
                            .getProvisioningProperties();

                    if (connctorProperties != null) {
                        for (ProvisioningProperty config : connctorProperties) {
                            if (config == null) {
                                continue;
                            }
                            prepStmt.setInt(1, idpId);
                            prepStmt.setString(2, connector.getName());
                            prepStmt.setString(3, config.getName());
                            prepStmt.setString(4, config.getValue());

                            if (config.isConfidential()) {
                                prepStmt.setString(5, "1");
                            } else {
                                prepStmt.setString(5, "0");
                            }
                            prepStmt.addBatch();

                        }
                    }
                }
                prepStmt.executeBatch();
            }
        } finally {
            IdentityApplicationManagementUtil.closeResultSet(rs);
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
        }
    }

    /**
     * 
     * @param newProvisioningConnectors
     * @param dbConnection
     * @param idpId
     * @throws IdentityApplicationManagementException
     * @throws SQLException
     */
    private void updateProvisioningConnectors(ProvisioningConnector[] newProvisioningConnectors,
            Connection dbConnection, int idpId) throws IdentityApplicationManagementException,
            SQLException {

        PreparedStatement prepStmt = null;
        ResultSet rs = null;

        try {
            deleteProvisioningConnectors(dbConnection, idpId);

            if (newProvisioningConnectors != null && newProvisioningConnectors.length > 0) {
                addProvisioningConnectors(newProvisioningConnectors, dbConnection, idpId);
            }

        } finally {
            IdentityApplicationManagementUtil.closeResultSet(rs);
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
        }
    }

    /**
     * 
     * @param dbConnection
     * @param idPName
     * @param tenantId
     * @param tenantDomain
     * @return
     * @throws IdentityApplicationManagementException
     * @throws SQLException
     */
    public ProvisioningConnector[] getProvisioningConnectors(Connection dbConnection,
            String idPName, int tenantId) throws IdentityApplicationManagementException,
            SQLException {

        int idPId = getIdentityProviderIdentifier(dbConnection, idPName, tenantId);

        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        List<ProvisioningConnector> provsioningConnectors = new ArrayList<ProvisioningConnector>();

        try {
            // SP_IDP_PROV_CONNECTOR_TYPE,SP_IDP_PROV_CONFIG_KEY,
            // SP_IDP_PROV_CONFIG_VALUE,SP_IDP_PROV_CONFIG_IS_SECRET
            String sqlStmt = IdPManagementConstants.SQLQueries.GET_IDP_PROVISIONING_CONFIGS_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, idPId);
            rs = prepStmt.executeQuery();
            Map<String, ProvisioningConnector> provisioningConnectorMap = new HashMap<String, ProvisioningConnector>();

            while (rs.next()) {
                ProvisioningConnector provisioningConnector = null;

                String type = rs.getString(1);
                if (provisioningConnectorMap.containsKey(type)) {
                    provisioningConnector = provisioningConnectorMap.get(type);
                } else {
                    provisioningConnector = new ProvisioningConnector();
                    provisioningConnector.setName(type);

                    if (provisioningConnector.getProvisioningProperties() == null) {
                        provisioningConnector
                                .setProvisioningProperties(new ProvisioningProperty[0]);
                    }

                    provisioningConnectorMap.put(type, provisioningConnector);
                }

                ProvisioningProperty property = new ProvisioningProperty();
                property.setName(rs.getString(2));
                property.setValue(rs.getString(3));

                if ("1".equals(rs.getString(4))) {
                    property.setConfidential(true);
                } else {
                    property.setConfidential(false);
                }

                if (ProvisioningConnector.PROP_ENABLED.equals(property.getName())
                        && "true".equals(property.getValue())) {
                    provisioningConnector.setEnabled(true);
                } else {
                    provisioningConnector.setEnabled(false);
                }

                provisioningConnector.setProvisioningProperties(concatArrays(
                        new ProvisioningProperty[] { property },
                        provisioningConnector.getProvisioningProperties()));
            }
            
            return provisioningConnectorMap.values().toArray(new ProvisioningConnector[0]);
        } finally {
            IdentityApplicationManagementUtil.closeResultSet(rs);
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
        }
    }

    /**
     * 
     * @param dbConnection
     * @param idPName
     * @param tenantId
     * @param tenantDomain
     * @return
     * @throws IdentityApplicationManagementException
     */
    public FederatedIdentityProvider getIdPByName(Connection dbConnection, String idPName,
            int tenantId, String tenantDomain) throws IdentityApplicationManagementException {

        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        FederatedIdentityProvider federatedIdp = null;
        boolean dbConnectionInitialized = true;
        try {
            if (dbConnection == null) {
                dbConnection = JDBCPersistenceManager.getInstance().getDBConnection();
            } else {
                dbConnectionInitialized = false;
            }

            // SP_IDP_ID, SP_IDP_PRIMARY, SP_IDP_HOME_REALM_ID,SP_IDP_THUMBPRINT,
            // SP_IDP_TOKEN_EP_ALIAS,
            // SP_IDP_INBOUND_PROVISIONING_ENABLED,SP_IDP_INBOUND_PROVISIONING_USER_STORE_ID,
            // SP_IDP_USER_CLAIM_URI,
            // SP_IDP_ROLE_CLAIM_URI,SP_IDP_DEFAULT_AUTHENTICATOR_NAME,SP_IDP_DEFAULT_PRO_CONNECTOR_NAME
            String sqlStmt = IdPManagementConstants.SQLQueries.GET_IDP_BY_NAME_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, tenantId);
            prepStmt.setString(2, idPName);
            rs = prepStmt.executeQuery();

            if (rs.next()) {
                federatedIdp = new FederatedIdentityProvider();
                federatedIdp.setIdentityProviderName(idPName);

                if (rs.getString(2).equals("1")) {
                    federatedIdp.setPrimary(true);
                } else {
                    federatedIdp.setPrimary(false);
                }

                federatedIdp.setHomeRealmId(rs.getString(3));
                federatedIdp.setCertificate(rs.getString(4));
                federatedIdp.setAlias(rs.getString(5));

                JustInTimeProvisioningConfiguration jitProConfig = new JustInTimeProvisioningConfiguration();
                if (rs.getString(6).equals("1")) {
                    jitProConfig.setProvisioningEnabled(true);
                } else {
                    jitProConfig.setProvisioningEnabled(false);
                }

                jitProConfig.setProvisioningUserStore(rs.getString(7));
                federatedIdp.setJustInTimeProvisioningConfiguration(jitProConfig);

                String userClaimUri = rs.getString(8);
                String roleClaimUri = rs.getString(9);

                String defaultAuthenticatorName = rs.getString(10);
                String defaultProvisioningConnectorName = rs.getString(11);

                if (defaultAuthenticatorName != null) {
                    FederatedAuthenticator defaultAuthenticator = new FederatedAuthenticator();
                    defaultAuthenticator.setName(defaultAuthenticatorName);
                    federatedIdp.setDefaultAuthenticator(defaultAuthenticator);
                }

                if (defaultProvisioningConnectorName != null) {
                    ProvisioningConnector defaultProConnector = new ProvisioningConnector();
                    defaultProConnector.setName(defaultProvisioningConnectorName);
                    federatedIdp.setDefaultProvisioinongConnector(defaultProConnector);
                }

                // get federated authenticators.
                federatedIdp.setFederatedAuthenticators(getFederatedAuthenticators(dbConnection,
                        idPName, tenantId));

                // get claim configuration.
                federatedIdp.setClaimConfiguration(getIdPClaimConfiguration(dbConnection, idPName,
                        userClaimUri, roleClaimUri, tenantId));

                // get provisioning connectors.
                federatedIdp.setProvisoningConnectors(getProvisioningConnectors(dbConnection,
                        idPName, tenantId));

                // get permission and role configuration.
                federatedIdp.setPermissionAndRoleConfiguration(getPermissionsAndRoleConfiguration(
                        dbConnection, idPName, tenantId));

            }
            return federatedIdp;
        } catch (SQLException e) {
            try {
                IdentityApplicationManagementUtil.rollBack(dbConnection);
            } catch (SQLException e1) {
                log.error("Error occurred while rolling back transactions: " + e1.getMessage(), e);
            }
            String msg = "Error occurred while retrieving Identity Provider information for tenant : "
                    + tenantDomain + " and Identity Provider name : " + idPName;
            throw new IdentityApplicationManagementException(msg, e);
        } finally {
            if (dbConnectionInitialized) {
                try {
                    IdentityApplicationManagementUtil.closeConnection(dbConnection);
                } catch (SQLException e) {
                    log.error("Error occurred while closing connection: " + e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 
     * @param realmId
     * @param tenantId
     * @param tenantDomain
     * @return
     * @throws IdentityApplicationManagementException
     * @throws SQLException
     */
    public FederatedIdentityProvider getIdPByRealmId(String realmId, int tenantId,
            String tenantDomain) throws IdentityApplicationManagementException {

        Connection dbConnection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        String idPName = null;

        try {
            dbConnection = JDBCPersistenceManager.getInstance().getDBConnection();
            String sqlStmt = IdPManagementConstants.SQLQueries.GET_IDP_NAME_BY_REALM_ID_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, tenantId);
            prepStmt.setString(2, realmId);
            rs = prepStmt.executeQuery();
            if (rs.next()) {
                idPName = rs.getString(1);
            }

            IdentityApplicationManagementUtil.closeStatement(prepStmt);
            IdentityApplicationManagementUtil.closeResultSet(rs);

            return getIdPByName(dbConnection, idPName, tenantId, tenantDomain);
        } catch (SQLException e) {
            throw new IdentityApplicationManagementException(
                    "Error while retreiving Identity Provider by realm " + realmId, e);
        } finally {
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
            IdentityApplicationManagementUtil.closeResultSet(rs);
            try {
                IdentityApplicationManagementUtil.closeConnection(dbConnection);
            } catch (SQLException e) {
                log.error("Error occurred while closing database connection", e);
            }
        }

    }

    /**
     * 
     * @param identityProvider
     * @param tenantId
     * @throws IdentityApplicationManagementException
     */
    public void addIdP(FederatedIdentityProvider identityProvider, int tenantId)
            throws IdentityApplicationManagementException {

        Connection dbConnection = null;
        try {
            dbConnection = JDBCPersistenceManager.getInstance().getDBConnection();

            if (identityProvider.isPrimary()) {
                // this is going to be the primary. Switch off any other primary set up in the
                // system.
                switchOffPrimary(dbConnection, tenantId);
            }

            PreparedStatement prepStmt = null;

            // SP_TENANT_ID, SP_IDP_NAME, SP_IDP_PRIMARY, SP_IDP_HOME_REALM_ID, SP_IDP_THUMBPRINT,
            // SP_IDP_TOKEN_EP_ALIAS,
            // SP_IDP_INBOUND_PROVISIONING_ENABLED,SP_IDP_INBOUND_PROVISIONING_USER_STORE_ID,
            // SP_IDP_USER_CLAIM_URI,SP_IDP_ROLE_CLAIM_URI,SP_IDP_DEFAULT_AUTHENTICATOR_NAME,SP_IDP_DEFAULT_PRO_CONNECTOR_NAME
            String sqlStmt = IdPManagementConstants.SQLQueries.ADD_IDP_SQL;

            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, tenantId);
            prepStmt.setString(2, identityProvider.getIdentityProviderName());

            if (identityProvider.isPrimary()) {
                prepStmt.setString(3, "1");
            } else {
                prepStmt.setString(3, "0");
            }

            prepStmt.setString(4, identityProvider.getHomeRealmId());
            prepStmt.setString(5, identityProvider.getCertificate());
            prepStmt.setString(6, identityProvider.getAlias());

            if (identityProvider.getJustInTimeProvisioningConfiguration() != null
                    && identityProvider.getJustInTimeProvisioningConfiguration()
                            .isProvisioningEnabled()) {
                // just in time provisioning enabled for this identity provider.
                // based on the authentication response from the identity provider - user will be
                // provisioned locally.
                prepStmt.setString(7, "1");
                // user will be provisioned to the configured user store.
                prepStmt.setString(8, identityProvider.getJustInTimeProvisioningConfiguration()
                        .getProvisioningUserStore());
            } else {
                prepStmt.setString(7, "0");
                prepStmt.setString(8, null);
            }

            if (identityProvider.getClaimConfiguration() != null) {
                // this is how we find the subject name from the authentication response.
                // this claim URI is in identity provider's own dialect.
                prepStmt.setString(9, identityProvider.getClaimConfiguration().getUserClaimURI());
                // this is how we find the role name from the authentication response.
                // this claim URI is in identity provider's own dialect.
                prepStmt.setString(10, identityProvider.getClaimConfiguration().getRoleClaimURI());
            } else {
                prepStmt.setString(9, null);
                prepStmt.setString(10, null);
            }

            if (identityProvider.getDefaultAuthenticator() != null) {
                prepStmt.setString(11, identityProvider.getDefaultAuthenticator().getName());
            } else {
                prepStmt.setString(11, null);
            }

            if (identityProvider.getDefaultProvisioinongConnector() != null) {
                prepStmt.setString(12, identityProvider.getDefaultProvisioinongConnector()
                        .getName());
            } else {
                prepStmt.setString(12, null);
            }

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

            // add claim configuration.
            if (identityProvider.getClaimConfiguration() != null
                    && identityProvider.getClaimConfiguration().getClaimMappings() != null
                    && identityProvider.getClaimConfiguration().getClaimMappings().length > 0) {
                addIdPClaimMappings(dbConnection, idPId, tenantId, identityProvider
                        .getClaimConfiguration().getClaimMappings());

            }

            // add provisioning connectors.
            if (identityProvider.getProvisoningConnectors() != null
                    && identityProvider.getProvisoningConnectors().length > 0) {
                addProvisioningConnectors(identityProvider.getProvisoningConnectors(),
                        dbConnection, idPId);
            }

            // add federated authenticators.
            addFederatedAuthenticators(identityProvider.getFederatedAuthenticators(), dbConnection,
                    idPId, tenantId);

            // add role configuration.
            if (identityProvider.getPermissionAndRoleConfiguration() != null) {
                if (identityProvider.getPermissionAndRoleConfiguration().getIdpRoles() != null
                        && identityProvider.getPermissionAndRoleConfiguration().getIdpRoles().length > 0) {
                    // add roles.
                    addIdPRoles(dbConnection, idPId, identityProvider
                            .getPermissionAndRoleConfiguration().getIdpRoles());

                    if (identityProvider.getPermissionAndRoleConfiguration().getRoleMappings() != null
                            && identityProvider.getPermissionAndRoleConfiguration()
                                    .getRoleMappings().length > 0) {
                        // add role mappings.
                        addIdPRoleMappings(dbConnection, idPId, tenantId, identityProvider
                                .getPermissionAndRoleConfiguration().getRoleMappings());
                    }
                }
            }

            // add claim configuration.
            if (identityProvider.getClaimConfiguration() != null) {
                addIdPClaims(dbConnection, idPId, identityProvider.getClaimConfiguration()
                        .getIdpClaims());
                addIdPClaimMappings(dbConnection, idPId, tenantId, identityProvider
                        .getClaimConfiguration().getClaimMappings());
            }

            dbConnection.commit();
        } catch (SQLException e) {
            try {
                IdentityApplicationManagementUtil.rollBack(dbConnection);
            } catch (SQLException e1) {
                log.error("Error occurred while rolling back transactions: " + e1.getMessage(), e);
            }
            String msg = "Error occurred while adding Identity Provider for tenant " + tenantId;
            throw new IdentityApplicationManagementException(msg, e);
        } finally {
            try {
                IdentityApplicationManagementUtil.closeConnection(dbConnection);
            } catch (SQLException e) {
                log.error("Error occurred while closing connection: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 
     * @param newIdentityProvider
     * @param currentIdentityProvider
     * @param tenantId
     * @throws IdentityApplicationManagementException
     */
    public void updateIdP(FederatedIdentityProvider newIdentityProvider,
            FederatedIdentityProvider currentIdentityProvider, int tenantId)
            throws IdentityApplicationManagementException {

        Connection dbConnection = null;

        try {

            dbConnection = JDBCPersistenceManager.getInstance().getDBConnection();

            int idPId = getIdentityProviderIdByName(dbConnection,
                    newIdentityProvider.getIdentityProviderName(), tenantId);

            if (idPId <= 0) {
                String msg = "Trying to update non-existent Identity Provider for tenant "
                        + tenantId;
                throw new IdentityApplicationManagementException(msg);
            }

            PreparedStatement prepStmt = null;

            // SP_IDP_NAME=?, SP_IDP_PRIMARY=?,SP_IDP_HOME_REALM_ID=?, SP_IDP_THUMBPRINT=?,
            // SP_IDP_TOKEN_EP_ALIAS=?,
            // SP_IDP_INBOUND_PROVISIONING_ENABLED=?,SP_IDP_INBOUND_PROVISIONING_USER_STORE_ID=?,SP_IDP_USER_CLAIM_URI=?,
            // SP_IDP_ROLE_CLAIM_URI=?,SP_IDP_DEFAULT_AUTHENTICATOR_NAME=?,SP_IDP_DEFAULT_PRO_CONNECTOR_NAME=?
            String sqlStmt = IdPManagementConstants.SQLQueries.UPDATE_IDP_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);

            prepStmt.setString(1, newIdentityProvider.getIdentityProviderName());

            if (newIdentityProvider.isPrimary()) {
                prepStmt.setString(2, "1");
            } else {
                prepStmt.setString(2, "0");
            }

            prepStmt.setString(3, newIdentityProvider.getHomeRealmId());
            prepStmt.setString(4, newIdentityProvider.getCertificate());
            prepStmt.setString(5, newIdentityProvider.getAlias());

            if (newIdentityProvider.getJustInTimeProvisioningConfiguration() != null
                    && newIdentityProvider.getJustInTimeProvisioningConfiguration()
                            .isProvisioningEnabled()) {
                prepStmt.setString(6, "1");
                prepStmt.setString(7, newIdentityProvider.getJustInTimeProvisioningConfiguration()
                        .getProvisioningUserStore());

            } else {
                prepStmt.setString(6, "0");
                prepStmt.setString(7, null);
            }

            if (newIdentityProvider.getClaimConfiguration() != null) {
                prepStmt.setString(8, newIdentityProvider.getClaimConfiguration().getUserClaimURI());
                prepStmt.setString(9, newIdentityProvider.getClaimConfiguration().getRoleClaimURI());
            } else {
                prepStmt.setString(8, null);
                prepStmt.setString(9, null);
            }

            // update the default authenticator
            if (newIdentityProvider.getDefaultAuthenticator() != null
                    && newIdentityProvider.getDefaultAuthenticator().getName() != null) {
                prepStmt.setString(10, newIdentityProvider.getDefaultAuthenticator().getName());
            } else {
                // its not a must to have a default authenticator.
                prepStmt.setString(10, null);
            }

            // update the default provisioning connector.
            if (newIdentityProvider.getDefaultProvisioinongConnector() != null
                    && newIdentityProvider.getDefaultProvisioinongConnector().getName() != null) {
                prepStmt.setString(11, newIdentityProvider.getDefaultProvisioinongConnector()
                        .getName());
            } else {
                // its not a must to have a default provisioning connector..
                prepStmt.setString(11, null);
            }

            prepStmt.setInt(12, tenantId);
            prepStmt.setString(13, currentIdentityProvider.getIdentityProviderName());

            prepStmt.executeUpdate();

            prepStmt.clearParameters();
            IdentityApplicationManagementUtil.closeStatement(prepStmt);

            sqlStmt = IdPManagementConstants.SQLQueries.GET_IDP_BY_NAME_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, tenantId);
            prepStmt.setString(2, newIdentityProvider.getIdentityProviderName());
            ResultSet rs = prepStmt.executeQuery();

            if (rs.next()) {

                // id of the updated identity provider.
                int idpId = rs.getInt(1);

                // update federated authenticators.
                updateFederatedAuthenticators(newIdentityProvider.getFederatedAuthenticators(),
                        currentIdentityProvider.getFederatedAuthenticators(), dbConnection, idpId,
                        tenantId);

                // update claim configuration.
                updateClaimConfiguration(dbConnection, idpId, tenantId,
                        newIdentityProvider.getClaimConfiguration());

                // update role configuration.
                updateRoleConfiguration(dbConnection, idpId, tenantId,
                        newIdentityProvider.getPermissionAndRoleConfiguration());

                // update provisioning connectors.
                updateProvisioningConnectors(newIdentityProvider.getProvisoningConnectors(),
                        dbConnection, idpId);

            }

            dbConnection.commit();
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            try {
                IdentityApplicationManagementUtil.rollBack(dbConnection);
            } catch (SQLException e1) {
                log.error("Error occurred while rolling back transactions: " + e1.getMessage(), e);
            }
            String msg = "Error occurred while updating Identity Provider information  for tenant "
                    + tenantId;
            throw new IdentityApplicationManagementException(msg, e);
        } finally {
            try {
                IdentityApplicationManagementUtil.closeConnection(dbConnection);
            } catch (SQLException e) {
                log.error("Error occurred while closing connection: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 
     * @param idPName
     * @param tenantId
     * @param tenantDomain
     * @throws IdentityApplicationManagementException
     */
    public void deleteIdP(String idPName, int tenantId, String tenantDomain)
            throws IdentityApplicationManagementException {

        Connection dbConnection = null;
        try {
            dbConnection = JDBCPersistenceManager.getInstance().getDBConnection();
            FederatedIdentityProvider identityProvider = getIdPByName(dbConnection, idPName,
                    tenantId, tenantDomain);
            if (identityProvider == null) {
                String msg = "Trying to delete non-existent Identity Provider for tenant "
                        + tenantDomain;
                log.error(msg);
                return;
            }

            FederatedIdentityProvider primaryIdP = getPrimaryIdP(dbConnection, tenantId,
                    tenantDomain);
            if (primaryIdP == null) {
                String msg = "Cannot find primary Identity Provider for tenant " + tenantDomain;
                log.warn(msg);
            }

            deleteIdP(dbConnection, tenantId, idPName);

            if (idPName.equals(primaryIdP.getIdentityProviderName())) {
                doAppointPrimary(dbConnection, tenantId, tenantDomain);
            }

            dbConnection.commit();
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            try {
                IdentityApplicationManagementUtil.rollBack(dbConnection);
            } catch (SQLException e1) {
                log.error("Error occurred while rolling back transactions: " + e1.getMessage(), e);
            }
            String msg = "Error occurred while deleting Identity Provider of tenant "
                    + tenantDomain;
            throw new IdentityApplicationManagementException(msg);
        } finally {
            try {
                IdentityApplicationManagementUtil.closeConnection(dbConnection);
            } catch (SQLException e) {
                log.error("Error occurred while closing connection: " + e.getMessage(), e);
            }
        }
    }

    public FederatedIdentityProvider getPrimaryIdP(Connection dbConnection, int tenantId,
            String tenantDomain) throws IdentityApplicationManagementException {

        boolean dbConnInitialized = true;
        PreparedStatement prepStmt = null;
        try {
            if (dbConnection == null) {
                dbConnection = JDBCPersistenceManager.getInstance().getDBConnection();
            } else {
                dbConnInitialized = false;
            }
            String sqlStmt = IdPManagementConstants.SQLQueries.GET_PRIMARY_IDP_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, tenantId);
            prepStmt.setString(2, "1");
            ResultSet rs = prepStmt.executeQuery();
            if (rs.next()) {
                FederatedIdentityProvider identityProviderDO = new FederatedIdentityProvider();
                identityProviderDO.setIdentityProviderName(rs.getString(1));
                identityProviderDO.setPrimary(true);
                identityProviderDO.setHomeRealmId(rs.getString(2));
                return identityProviderDO;
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            try {
                IdentityApplicationManagementUtil.rollBack(dbConnection);
            } catch (SQLException e1) {
                log.error("Error occurred while rolling back transactions: " + e1.getMessage(), e);
            }
            String msg = "Error occurred while retrieving primary Identity Provider for tenant "
                    + tenantDomain;
            throw new IdentityApplicationManagementException(msg);
        } finally {
            if (dbConnInitialized) {
                try {
                    IdentityApplicationManagementUtil.closeConnection(dbConnection);
                } catch (SQLException e) {
                    log.error("Error occurred while closing connection: " + e.getMessage(), e);
                }
            }
        }
        return null;
    }

    public void deleteTenantRole(int tenantId, String role, String tenantDomain)
            throws IdentityApplicationManagementException {

        Connection dbConnection = null;
        PreparedStatement prepStmt = null;
        try {
            dbConnection = JDBCPersistenceManager.getInstance().getDBConnection();
            String sqlStmt = IdPManagementConstants.SQLQueries.DELETE_ROLE_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, tenantId);
            prepStmt.setString(2, role);
            prepStmt.executeUpdate();
            dbConnection.commit();
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            try {
                IdentityApplicationManagementUtil.rollBack(dbConnection);
            } catch (SQLException e1) {
                log.error("Error occurred while rolling back transactions: " + e1.getMessage(), e);
            }
            String msg = "Error occurred while deleting tenant role " + role + " of tenant "
                    + tenantDomain;
            throw new IdentityApplicationManagementException(msg);
        } finally {
            try {
                IdentityApplicationManagementUtil.closeConnection(dbConnection);
            } catch (SQLException e) {
                log.error("Error occurred while closing connection: " + e.getMessage(), e);
            }
        }
    }

    public void renameTenantRole(String newRoleName, String oldRoleName, int tenantId,
            String tenantDomain) throws IdentityApplicationManagementException {

        Connection dbConnection = null;
        PreparedStatement prepStmt = null;
        try {
            dbConnection = JDBCPersistenceManager.getInstance().getDBConnection();
            String sqlStmt = IdPManagementConstants.SQLQueries.RENAME_ROLE_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setString(1, newRoleName);
            prepStmt.setInt(2, tenantId);
            prepStmt.setString(3, oldRoleName);
            prepStmt.executeUpdate();
            dbConnection.commit();
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            try {
                IdentityApplicationManagementUtil.rollBack(dbConnection);
            } catch (SQLException e1) {
                log.error("Error occurred while rolling back transactions: " + e1.getMessage(), e);
            }
            String msg = "Error occurred while renaming tenant role " + oldRoleName + " to "
                    + newRoleName + " of tenant " + tenantDomain;
            throw new IdentityApplicationManagementException(msg);
        } finally {
            try {
                IdentityApplicationManagementUtil.closeConnection(dbConnection);
            } catch (SQLException e) {
                log.error("Error occurred while closing connection: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 
     * @param dbConnection
     * @param idpId
     * @throws IdentityApplicationManagementException
     * @throws SQLException
     */
    private void deleteAllIdPClaims(Connection dbConnection, int idpId)
            throws IdentityApplicationManagementException, SQLException {

        PreparedStatement prepStmt = null;
        try {
            String sqlStmt = IdPManagementConstants.SQLQueries.DELETE_CLAIM_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, idpId);
            prepStmt.executeUpdate();
        } finally {
            IdentityApplicationManagementUtil.closeStatement(prepStmt);

        }
    }

    /**
     * 
     * @param dbConnection
     * @param idpId
     * @throws IdentityApplicationManagementException
     * @throws SQLException
     */
    private void deleteAllIdPRoles(Connection dbConnection, int idpId)
            throws IdentityApplicationManagementException, SQLException {

        PreparedStatement prepStmt = null;
        try {
            String sqlStmt = IdPManagementConstants.SQLQueries.DELETE_ROLE_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, idpId);
            prepStmt.executeUpdate();
        } finally {
            IdentityApplicationManagementUtil.closeStatement(prepStmt);

        }
    }

    /**
     * 
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
            dbConnection = JDBCPersistenceManager.getInstance().getDBConnection();
            String sqlStmt = IdPManagementConstants.SQLQueries.RENAME_CLAIM_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setString(1, newClaimURI);
            prepStmt.setInt(2, tenantId);
            prepStmt.setString(3, oldClaimURI);
            prepStmt.executeUpdate();
            dbConnection.commit();
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            try {
                IdentityApplicationManagementUtil.rollBack(dbConnection);
            } catch (SQLException e1) {
                log.error("Error occurred while rolling back transactions: " + e1.getMessage(), e);
            }
            String msg = "Error occurred while renaming tenant role " + oldClaimURI + " to "
                    + newClaimURI + " of tenant " + tenantDomain;
            throw new IdentityApplicationManagementException(msg);
        } finally {
            try {
                IdentityApplicationManagementUtil.closeConnection(dbConnection);
            } catch (SQLException e) {
                log.error("Error occurred while closing connection: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 
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

        List<FederatedIdentityProvider> tenantIdPs = getIdPs(conn, tenantId, tenantDomain);
        if (!tenantIdPs.isEmpty()) {
            PreparedStatement prepStmt = null;
            String sqlStmt = IdPManagementConstants.SQLQueries.SWITCH_IDP_PRIMARY_ON_DELETE_SQL;
            prepStmt = conn.prepareStatement(sqlStmt);
            prepStmt.setString(1, "1");
            prepStmt.setInt(2, tenantId);
            prepStmt.setString(3, tenantIdPs.get(0).getIdentityProviderName());
            prepStmt.setString(4, "0");
            prepStmt.executeUpdate();
        } else {
            String msg = "No Identity Providers registered for tenant " + tenantDomain;
            log.warn(msg);
        }
    }

    /**
     * 
     * @param conn
     * @param idPId
     * @param claims
     * @throws SQLException
     */
    private void addIdPClaims(Connection conn, int idPId, Claim[] claims) throws SQLException {
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
                prepStmt.setString(2, claim.getClaimUri());
                prepStmt.addBatch();
                prepStmt.clearParameters();
            }
            prepStmt.executeBatch();
        } finally {
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
        }
    }

    /**
     * 
     * @param conn
     * @param idPId
     * @param tenantId
     * @param claimMappings
     * @param tenantDomain
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
                int id = rs.getInt(1);
                String claim = rs.getString(2);
                claimIdMap.put(claim, id);
            }

            prepStmt.clearParameters();
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
            IdentityApplicationManagementUtil.closeResultSet(rs);

            if (claimIdMap.isEmpty()) {
                String message = "No Identity Provider claim URIs defined for tenant " + tenantId;
                throw new IdentityApplicationManagementException(message);
            }

            for (ClaimMapping mapping : claimMappings) {
                if (mapping != null && mapping.getIdpClaim() != null
                        && claimIdMap.containsKey(mapping.getIdpClaim().getClaimUri())) {

                    int idpClaimId = claimIdMap.get(mapping.getIdpClaim().getClaimUri());
                    String localClaimURI = mapping.getLocalClaim().getClaimUri();

                    sqlStmt = IdPManagementConstants.SQLQueries.ADD_IDP_CLAIM_MAPPINGS_SQL;
                    prepStmt = conn.prepareStatement(sqlStmt);
                    prepStmt.setInt(1, idpClaimId);
                    prepStmt.setInt(2, tenantId);
                    prepStmt.setString(3, localClaimURI);
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
     * 
     * @param conn
     * @param idPId
     * @param idpRoleNames
     * @throws SQLException
     */
    private void addIdPRoles(Connection conn, int idPId, String[] idpRoleNames) throws SQLException {

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
                prepStmt.setString(2, idpRole);
                prepStmt.addBatch();
                prepStmt.clearParameters();
            }

            prepStmt.executeBatch();

        } finally {
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
        }
    }

    /**
     * 
     * @param conn
     * @param idPId
     * @param tenantId
     * @param roleMappings
     * @throws SQLException
     * @throws IdentityApplicationManagementException
     */
    private void addIdPRoleMappings(Connection conn, int idPId, int tenantId,
            RoleMapping[] roleMappings) throws SQLException, IdentityApplicationManagementException {

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
                int idpRoleId = rs.getInt(1);
                String roleName = rs.getString(2);
                roleIdMap.put(roleName, idpRoleId);
            }

            prepStmt.clearParameters();
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
            IdentityApplicationManagementUtil.closeResultSet(rs);

            if (roleIdMap.isEmpty()) {
                String message = "No Identity Provider roles defined for tenant " + tenantId;
                throw new IdentityApplicationManagementException(message);
            }

            for (RoleMapping mapping : roleMappings) {
                if (mapping.getRemoteRole() != null
                        && roleIdMap.containsKey(mapping.getRemoteRole())) {

                    int idpRoleId = roleIdMap.get(mapping.getRemoteRole());

                    String userStoreId = mapping.getLocalRole().getUserStoreId();
                    String localRole = mapping.getLocalRole().getLocalRoleName();

                    // SP_IDP_ROLE_ID, SP_TENANT_ID, SP_USER_STORE_ID, SP_LOCAL_ROLE
                    sqlStmt = IdPManagementConstants.SQLQueries.ADD_IDP_ROLE_MAPPINGS_SQL;
                    prepStmt = conn.prepareStatement(sqlStmt);
                    prepStmt.setInt(1, idpRoleId);
                    prepStmt.setInt(2, tenantId);
                    prepStmt.setString(3, userStoreId);
                    prepStmt.setString(4, localRole);
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
     * 
     * @param conn
     * @param idPId
     * @param tenantId
     * @param newClaimConfig
     * @throws SQLException
     * @throws IdentityApplicationManagementException
     */
    private void updateClaimConfiguration(Connection conn, int idPId, int tenantId,
            ClaimConfiguration newClaimConfig) throws SQLException,
            IdentityApplicationManagementException {

        // remove all identity provider claims - this will also remove associated claim mappings.
        deleteAllIdPClaims(conn, idPId);

        if (newClaimConfig == null) {
            // bad data - we do not need.
            return;
        }

        if (newClaimConfig.getIdpClaims() == null || newClaimConfig.getIdpClaims().length == 0) {
            // we cannot add claim mappings without claims.
            return;
        }

        // add identity provider claims.
        addIdPClaims(conn, idPId, newClaimConfig.getIdpClaims());

        if (newClaimConfig.getClaimMappings() == null
                || newClaimConfig.getClaimMappings().length == 0) {
            // we do not have any claim mappings - we either didn't have.
            return;
        }

        addIdPClaimMappings(conn, idPId, tenantId, newClaimConfig.getClaimMappings());

    }

    /**
     * 
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
                prepStmt.setString(2, addedRole);
                prepStmt.addBatch();
            }

            prepStmt.executeBatch();
            prepStmt.clearParameters();
            prepStmt.clearBatch();
            IdentityApplicationManagementUtil.closeStatement(prepStmt);

            for (int i = 0; i < renamedOldRoles.size(); i++) {
                sqlStmt = IdPManagementConstants.SQLQueries.UPDATE_IDP_ROLES_SQL;
                prepStmt = conn.prepareStatement(sqlStmt);
                prepStmt.setString(1, renamedNewRoles.get(i));
                prepStmt.setInt(2, idPId);
                prepStmt.setString(3, renamedOldRoles.get(i));
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
     * 
     * @param conn
     * @param idPId
     * @param tenantId
     * @param newRoleConfiguration
     * @param currentRoleConfiguration
     * @throws SQLException
     * @throws IdentityApplicationManagementException
     */
    private void updateRoleConfiguration(Connection conn, int idPId, int tenantId,
            PermissionsAndRoleConfiguration newRoleConfiguration) throws SQLException,
            IdentityApplicationManagementException {

        // delete all identity provider roles - this will also clean up idp role mappings.
        deleteAllIdPRoles(conn, idPId);

        if (newRoleConfiguration == null) {
            // bad data - we do not need to deal with.
            return;
        }

        // add identity provider roles.
        addIdPRoles(conn, idPId, newRoleConfiguration.getIdpRoles());

        if (newRoleConfiguration.getRoleMappings() == null
                || newRoleConfiguration.getRoleMappings().length == 0) {
            // we do not have any role mappings in the system.
            return;
        }

        // add identity provider role mappings.
        addIdPRoleMappings(conn, idPId, tenantId, newRoleConfiguration.getRoleMappings());

    }

    /**
     * 
     * @param conn
     * @param tenantId
     * @param idPName
     * @throws SQLException
     */
    private void deleteProvisioningConnectors(Connection conn, int idPId) throws SQLException {

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
     * 
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
     * 
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
                dbConnection = JDBCPersistenceManager.getInstance().getDBConnection();
            } else {
                dbConnInitialized = false;
            }
            String sqlStmt = IdPManagementConstants.SQLQueries.GET_IDP_ROW_ID_SQL;
            prepStmt = dbConnection.prepareStatement(sqlStmt);
            prepStmt.setInt(1, tenantId);
            prepStmt.setString(2, idpName);
            rs = prepStmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
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
     * 
     * @param o1
     * @param o2
     * @return
     */
    private ProvisioningProperty[] concatArrays(ProvisioningProperty[] o1, ProvisioningProperty[] o2) {
        ProvisioningProperty[] ret = new ProvisioningProperty[o1.length + o2.length];

        System.arraycopy(o1, 0, ret, 0, o1.length);
        System.arraycopy(o2, 0, ret, o1.length, o2.length);

        return ret;
    }

    /**
     * 
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
            prepStmt.setString(2, idPName);
            rs = prepStmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                throw new IdentityApplicationManagementException("Invalid Identity Provider Name");
            }
        } finally {
            IdentityApplicationManagementUtil.closeResultSet(rs);
            IdentityApplicationManagementUtil.closeStatement(prepStmt);
        }
    }
}
