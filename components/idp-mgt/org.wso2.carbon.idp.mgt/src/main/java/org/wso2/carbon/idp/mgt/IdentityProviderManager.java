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

package org.wso2.carbon.idp.mgt;

import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementUtil;
import org.wso2.carbon.identity.application.common.model.ClaimConfiguration;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.FederatedIdentityProvider;
import org.wso2.carbon.identity.application.common.model.LocalRole;
import org.wso2.carbon.identity.application.common.model.PermissionsAndRoleConfiguration;
import org.wso2.carbon.identity.application.common.model.ResidentIdentityProvider;
import org.wso2.carbon.identity.application.common.model.RoleMapping;
import org.wso2.carbon.idp.mgt.dao.CacheBackedIdPMgtDAO;
import org.wso2.carbon.idp.mgt.dao.IdPManagementDAO;
import org.wso2.carbon.idp.mgt.internal.IdPManagementServiceComponent;
import org.wso2.carbon.idp.mgt.util.IdPManagementUtil;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

public class IdentityProviderManager {

    private static Log log = LogFactory.getLog(IdentityProviderManager.class);

    private static CacheBackedIdPMgtDAO dao = new CacheBackedIdPMgtDAO(new IdPManagementDAO());

    private static IdentityProviderManager instance = new IdentityProviderManager();

    /**
     * 
     * @return
     */
    public static IdentityProviderManager getInstance() {
        return instance;
    }

    private IdentityProviderManager() {
    }

    /**
     * Retrieves resident Identity provider for a given tenant
     * 
     * @param tenantDomain Tenant domain whose resident IdP is requested
     * @return <code>LocalIdentityProvider</code>
     * @throws IdentityProviderMgtException Error when getting Resident Identity Providers
     */
    public ResidentIdentityProvider getResidentIdP(String tenantDomain)
            throws IdentityApplicationManagementException {

        String tenantContext = "";
        if (!MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equalsIgnoreCase(tenantDomain)) {
            tenantContext = MultitenantConstants.TENANT_AWARE_URL_PREFIX + "/" + tenantDomain + "/";
        }
        String hostName = ServerConfiguration.getInstance().getFirstProperty("HostName");
        String mgtTransport = CarbonUtils.getManagementTransport();
        AxisConfiguration axisConfiguration = MessageContext.getCurrentMessageContext()
                .getAxisService().getAxisConfiguration();
        int mgtTransportPort = CarbonUtils.getTransportProxyPort(axisConfiguration, mgtTransport);
        if (mgtTransportPort <= 0) {
            mgtTransportPort = CarbonUtils.getTransportPort(axisConfiguration, mgtTransport);
        }
        String serverUrl = mgtTransport + "://" + hostName + ":" + mgtTransportPort + "/";
        String stsUrl = serverUrl + "services/" + tenantContext + "wso2carbon-sts";
        String openIdUrl = serverUrl + "openid";
        String samlSSOUrl = serverUrl + "samlsso";
        String samlLogoutUrl = serverUrl + "samlsso";
        String authzUrl = serverUrl + "authz";
        String tokenUrl = serverUrl + "token";
        String userUrl = serverUrl + "userinfo";
        ResidentIdentityProvider identityProvider = dao.getResidentIdP(
                getTenantIdOfDomain(tenantDomain), tenantDomain);
        if (identityProvider == null) {
            String mesage = "Could not find Resident Identity Provider for tenant " + tenantDomain;
            log.error(mesage);
            throw new IdentityApplicationManagementException(mesage);
        }
        if (identityProvider.getOpenIdRealm() == null) {
            identityProvider.setOpenIdRealm(identityProvider.getHomeRealmId());
        }
        identityProvider.setOpenIDUrl(openIdUrl);
        if (identityProvider.getIdpEntityId() == null) {
            identityProvider.setIdpEntityId(identityProvider.getHomeRealmId());
        }

        identityProvider.setSaml2SSOUrl(samlSSOUrl);
        identityProvider.setLogoutRequestUrl(samlLogoutUrl);
        identityProvider.setAuthzEndpointUrl(authzUrl);
        identityProvider.setTokenEndpointUrl(tokenUrl);
        identityProvider.setUserInfoEndpointUrl(userUrl);
        if (identityProvider.getPassiveSTSRealm() == null) {
            identityProvider.setPassiveSTSRealm(identityProvider.getHomeRealmId());
        }
        identityProvider.setPassiveSTSUrl(stsUrl);
        return identityProvider;
    }

    /**
     * Add Resident Identity provider for a given tenant
     * 
     * @param identityProvider <code>ResidentIdentityProvider</code>
     * @param tenantDomain Tenant domain whose resident IdP is requested
     * @throws IdentityProviderMgtException Error when adding Resident Identity Provider
     */
    public void addResidentIdP(ResidentIdentityProvider identityProvider, String tenantDomain)
            throws IdentityApplicationManagementException {

        if (identityProvider.getHomeRealmId() == null
                || identityProvider.getHomeRealmId().equals("")) {
            String msg = "Invalid argument: Resident Identity Provider Home Realm Identifier value is empty";
            log.error(msg);
            throw new IdentityApplicationManagementException(msg);
        }
        if (identityProvider.getOpenIdRealm() != null
                && (identityProvider.getOpenIdRealm().equals("") || identityProvider
                        .getOpenIdRealm().equals(identityProvider.getHomeRealmId()))) {
            identityProvider.setOpenIdRealm(null);
        }
        if (identityProvider.getIdpEntityId() != null
                && (identityProvider.getIdpEntityId().equals("") || identityProvider
                        .getIdpEntityId().equals(identityProvider.getHomeRealmId()))) {
            identityProvider.setIdpEntityId(null);
        }
        if (identityProvider.getPassiveSTSRealm() != null
                && (identityProvider.getPassiveSTSRealm().equals("") || identityProvider
                        .getPassiveSTSRealm().equals(identityProvider.getHomeRealmId()))) {
            identityProvider.setPassiveSTSRealm(null);
        }
        dao.addResidentIdP(identityProvider, getTenantIdOfDomain(tenantDomain), tenantDomain);
    }

    /**
     * Update Resident Identity provider for a given tenant
     * 
     * @param identityProvider <code>ResidentIdentityProvider</code>
     * @param tenantDomain Tenant domain whose resident IdP is requested
     * @throws IdentityProviderMgtException Error when updating Resident Identity Provider
     */
    public void updateResidentIdP(ResidentIdentityProvider identityProvider, String tenantDomain)
            throws IdentityApplicationManagementException {

        if (identityProvider.getHomeRealmId() == null
                || identityProvider.getHomeRealmId().equals("")) {
            String msg = "Invalid argument: Resident Identity Provider Home Realm Identifier value is empty";
            log.error(msg);
            throw new IdentityApplicationManagementException(msg);
        }
        if (identityProvider.getOpenIdRealm() != null
                && (identityProvider.getOpenIdRealm().equals("") || identityProvider
                        .getOpenIdRealm().equals(identityProvider.getHomeRealmId()))) {
            identityProvider.setOpenIdRealm(null);
        }
        if (identityProvider.getIdpEntityId() != null
                && (identityProvider.getIdpEntityId().equals("") || identityProvider
                        .getIdpEntityId().equals(identityProvider.getHomeRealmId()))) {
            identityProvider.setIdpEntityId(null);
        }
        if (identityProvider.getPassiveSTSRealm() != null
                && (identityProvider.getPassiveSTSRealm().equals("") || identityProvider
                        .getPassiveSTSRealm().equals(identityProvider.getHomeRealmId()))) {
            identityProvider.setPassiveSTSRealm(null);
        }
        dao.updateResidentIdP(identityProvider, getTenantIdOfDomain(tenantDomain), tenantDomain);
    }

    /**
     * Retrieves registered Identity providers for a given tenant
     * 
     * @param tenantDomain Tenant domain whose IdP names are requested
     * @return Set of <code>FederatedIdentityProvider</code>. IdP names, primary IdP and home realm
     *         identifiers of each IdP
     * @throws IdentityProviderMgtException Error when getting list of Identity Providers
     */
    public List<FederatedIdentityProvider> getIdPs(String tenantDomain)
            throws IdentityApplicationManagementException {

        int tenantId = getTenantIdOfDomain(tenantDomain);
        return dao.getIdPs(null, tenantId, tenantDomain);
    }

    /**
     * Retrieves Identity provider information about a given tenant by Identity Provider name
     * 
     * @param idPName Unique name of the Identity provider of whose information is requested
     * @param tenantDomain Tenant domain whose information is requested
     * @return <code>FederatedIdentityProvider</code> Identity Provider information
     * @throws IdentityProviderMgtException Error when getting Identity Provider information by IdP
     *         name
     */
    public FederatedIdentityProvider getIdPByName(String idPName, String tenantDomain)
            throws IdentityApplicationManagementException {

        int tenantId = getTenantIdOfDomain(tenantDomain);
        if (idPName == null || idPName.equals("")) {
            String msg = "Invalid argument: Identity Provider Name value is empty";
            log.error(msg);
            throw new IdentityApplicationManagementException(msg);
        }
        FederatedIdentityProvider identityProvider = dao.getIdPByName(null, idPName, tenantId,
                tenantDomain);
        if (identityProvider != null && identityProvider.getCertificate() != null) {
            identityProvider.setCertificate(IdPManagementUtil.getEncodedIdPCertFromAlias(idPName,
                    tenantId, tenantDomain));
        }
        return identityProvider;
    }

    /**
     * Retrieves Identity provider information about a given tenant by realm identifier
     * 
     * @param realmId Unique realm identifier of the Identity provider of whose information is
     *        requested
     * @param tenantDomain Tenant domain whose information is requested
     * @throws IdentityProviderMgtException Error when getting Identity Provider information by IdP
     *         home realm identifier
     */
    public FederatedIdentityProvider getIdPByRealmId(String realmId, String tenantDomain)
            throws IdentityApplicationManagementException {

        int tenantId = getTenantIdOfDomain(tenantDomain);
        if (realmId == null || realmId.equals("")) {
            String msg = "Invalid argument: Identity Provider Home Realm Identifier value is empty";
            log.error(msg);
            throw new IdentityApplicationManagementException(msg);
        }
        FederatedIdentityProvider identityProvider = dao.getIdPByRealmId(realmId, tenantId,
                tenantDomain);
        if (identityProvider != null && identityProvider.getCertificate() != null) {
            identityProvider.setCertificate(IdPManagementUtil.getEncodedIdPCertFromAlias(realmId,
                    tenantId, tenantDomain));
        }
        return identityProvider;
    }

    /**
     * Retrieves Identity provider information about a given tenant
     * 
     * @param idPName Unique Name of the IdP to which the given IdP claim URIs need to be mapped
     * @param tenantDomain The tenant domain of whose local claim URIs to be mapped
     * @param idPClaimURIs IdP claim URIs which need to be mapped to tenant's local claim URIs
     * @throws IdentityProviderMgtException Error when getting claim mappings
     */
    public Set<ClaimMapping> getMappedLocalClaims(String idPName, String tenantDomain,
            String[] idPClaimURIs) throws IdentityApplicationManagementException {

        int tenantId = getTenantIdOfDomain(tenantDomain);
        if (idPName == null || idPName.equals("")) {
            String msg = "Invalid argument: Identity Provider Name value is empty";
            log.error(msg);
            throw new IdentityApplicationManagementException(msg);
        }

        FederatedIdentityProvider identityProvider = dao.getIdPByName(null, idPName, tenantId,
                tenantDomain);

        ClaimConfiguration claimConfiguration = identityProvider.getClaimConfiguration();

        if (claimConfiguration != null) {

            ClaimMapping[] claimMappings = claimConfiguration.getClaimMappings();

            if (claimMappings != null && claimMappings.length > 0 && idPClaimURIs != null) {
                Set<ClaimMapping> returnSet = new HashSet<ClaimMapping>();
                for (String idpClaim : idPClaimURIs) {
                    for (ClaimMapping claimMapping : claimMappings) {
                        if (claimMapping.getIdpClaim().getClaimUri().equals(idpClaim)) {
                            returnSet.add(claimMapping);
                            break;
                        }
                    }
                }
                return returnSet;
            }
        }

        return new HashSet<ClaimMapping>();
    }

    /**
     * Retrieves Identity provider information about a given tenant
     * 
     * @param idPName Unique Name of the IdP to which the given local claim URIs need to be mapped
     * @param tenantDomain The tenant domain of whose local claim URIs to be mapped
     * @param localClaimURIs Local claim URIs which need to be mapped to IdP's claim URIs
     * @throws IdentityProviderMgtException Error when getting claim mappings
     */
    public Set<ClaimMapping> getMappedIdPClaims(String idPName, String tenantDomain,
            List<String> localClaimURIs) throws IdentityApplicationManagementException {

        int tenantId = getTenantIdOfDomain(tenantDomain);
        if (idPName == null || idPName.equals("")) {
            String msg = "Invalid argument: Identity Provider Name value is empty";
            log.error(msg);
            throw new IdentityApplicationManagementException(msg);
        }
        FederatedIdentityProvider identityProvider = dao.getIdPByName(null, idPName, tenantId,
                tenantDomain);

        ClaimConfiguration claimConfiguration = identityProvider.getClaimConfiguration();

        if (claimConfiguration != null) {

            ClaimMapping[] claimMappings = claimConfiguration.getClaimMappings();

            if (claimMappings != null && claimMappings.length > 0 && localClaimURIs != null) {
                Set<ClaimMapping> returnSet = new HashSet<ClaimMapping>();
                for (String localClaimURI : localClaimURIs) {
                    for (ClaimMapping claimMapping : claimMappings) {
                        if (claimMapping.equals(localClaimURI)) {
                            returnSet.add(claimMapping);
                            break;
                        }
                    }
                }
                return returnSet;
            }
        }
        return new HashSet<ClaimMapping>();
    }

    /**
     * Retrieves Identity provider information about a given tenant
     * 
     * @param idPName Unique name of the IdP to which the given IdP roles need to be mapped
     * @param tenantDomain The tenant domain of whose local roles to be mapped
     * @param idPRoles IdP roles which need to be mapped to local roles
     * @throws IdentityProviderMgtException Error when getting role mappings
     */
    public Set<RoleMapping> getMappedLocalRoles(String idPName, String tenantDomain,
            String[] idPRoles) throws IdentityApplicationManagementException {

        int tenantId = getTenantIdOfDomain(tenantDomain);

        if (idPName == null || idPName.equals("")) {
            String msg = "Invalid argument: Identity Provider Name value is empty";
            throw new IdentityApplicationManagementException(msg);
        }

        FederatedIdentityProvider identityProvider = dao.getIdPByName(null, idPName, tenantId,
                tenantDomain);
        PermissionsAndRoleConfiguration roleConfiguration = identityProvider
                .getPermissionAndRoleConfiguration();

        if (roleConfiguration != null) {
            RoleMapping[] roleMappings = roleConfiguration.getRoleMappings();

            if (roleMappings != null && roleMappings.length > 0 && idPRoles != null) {
                Set<RoleMapping> returnSet = new HashSet<RoleMapping>();
                for (String idPRole : idPRoles) {
                    for (RoleMapping roleMapping : roleMappings) {
                        if (roleMapping.getRemoteRole().equals(idPRole)) {
                            returnSet.add(roleMapping);
                            break;
                        }
                    }
                }
                return returnSet;
            }
        }
        return new HashSet<RoleMapping>();
    }

    /**
     * Retrieves Identity provider information about a given tenant
     * 
     * @param idPName Unique name of the IdP to which the given local roles need to be mapped
     * @param tenantDomain The tenant domain of whose local roles need to be mapped
     * @param localRoles Local roles which need to be mapped to IdP roles
     * @throws IdentityProviderMgtException Error when getting role mappings
     */
    public Set<RoleMapping> getMappedIdPRoles(String idPName, String tenantDomain,
            LocalRole[] localRoles) throws IdentityApplicationManagementException {

        int tenantId = getTenantIdOfDomain(tenantDomain);
        if (idPName == null || idPName.equals("")) {
            String msg = "Invalid argument: Identity Provider Name value is empty";
            log.error(msg);
            throw new IdentityApplicationManagementException(msg);
        }
        FederatedIdentityProvider identityProvider = dao.getIdPByName(null, idPName, tenantId,
                tenantDomain);

        PermissionsAndRoleConfiguration roleConfiguration = identityProvider
                .getPermissionAndRoleConfiguration();

        if (roleConfiguration != null) {
            RoleMapping[] roleMappings = roleConfiguration.getRoleMappings();

            if (roleMappings != null && roleMappings.length > 0 && localRoles != null) {
                Set<RoleMapping> returnSet = new HashSet<RoleMapping>();
                for (LocalRole localRole : localRoles) {
                    for (RoleMapping roleMapping : roleMappings) {
                        if (roleMapping.getLocalRole().equals(localRole)) {
                            returnSet.add(roleMapping);
                            break;
                        }
                    }
                }
                return returnSet;
            }
        }
        return new HashSet<RoleMapping>();
    }

    /**
     * Retrieves the primary Identity provider information for a given tenant
     * 
     * @param tenantDomain The tenant domain of whose primary IdP needs to be retrieved
     * @return primary Identity Provider name and home realm identifier
     * @throws IdentityProviderMgtException Error when getting primary Identity Provider information
     */
    public FederatedIdentityProvider getPrimaryIdP(String tenantDomain)
            throws IdentityApplicationManagementException {

        int tenantId = getTenantIdOfDomain(tenantDomain);
        FederatedIdentityProvider identityProvider = dao
                .getPrimaryIdP(null, tenantId, tenantDomain);
        if (identityProvider != null) {
            return identityProvider;
        }
        if (log.isDebugEnabled()) {
            log.debug("Primary Identity Provider not found for tenant " + tenantDomain);
        }
        return null;
    }

    /**
     * Adds an Identity Provider to the given tenant
     * 
     * @param identityProvider new Identity Provider information
     * @throws IdentityProviderMgtException Error when adding Identity Provider information
     */
    public void addIdP(FederatedIdentityProvider identityProvider, String tenantDomain)
            throws IdentityApplicationManagementException {

        String encodedCert = identityProvider.getCertificate();

        int tenantId = getTenantIdOfDomain(tenantDomain);

        if (identityProvider.getIdentityProviderName() == null
                || identityProvider.getIdentityProviderName().equals("")) {
            String msg = "Invalid argument: Identity Provider Name value is empty";
            log.error(msg);
            throw new IdentityApplicationManagementException(msg);
        }

        if (identityProvider.getCertificate() != null) {
            try {
                identityProvider.setCertificate(IdentityApplicationManagementUtil
                        .generateThumbPrint(identityProvider.getCertificate()));
            } catch (NoSuchAlgorithmException e) {
                log.error(e.getMessage(), e);
                throw new IdentityApplicationManagementException(
                        "Error occurred while generating thumbprint of Identity Provider's public certificate");
            }
        }

        PermissionsAndRoleConfiguration roleConfiguration = identityProvider
                .getPermissionAndRoleConfiguration();

        if (roleConfiguration != null && roleConfiguration.getRoleMappings() != null) {
            for (RoleMapping mapping : roleConfiguration.getRoleMappings()) {
                UserStoreManager usm = null;
                try {
                    usm = IdPManagementServiceComponent.getRealmService()
                            .getTenantUserRealm(tenantId).getUserStoreManager();
                    String role = null;
                    if (mapping.getLocalRole().getUserStoreId() != null) {
                        role = mapping.getLocalRole().getUserStoreId()
                                + CarbonConstants.DOMAIN_SEPARATOR
                                + mapping.getLocalRole().getLocalRoleName();
                    }
                    if (usm.isExistingRole(mapping.getLocalRole().getLocalRoleName())
                            || usm.isExistingRole(mapping.getLocalRole().getLocalRoleName(), true)) {
                        String msg = "Cannot find tenant role " + role + " for tenant "
                                + tenantDomain;
                        log.error(msg);
                        throw new IdentityApplicationManagementException(msg);
                    }
                } catch (UserStoreException e) {
                    String msg = "Error occurred while retrieving UserStoreManager for tenant "
                            + tenantDomain;
                    log.error(msg);
                    throw new IdentityApplicationManagementException(msg);
                }
            }
        }

        if (IdentityProviderManager.getInstance().getIdPByName(
                identityProvider.getIdentityProviderName(), tenantDomain) != null) {
            String msg = "An Identity Provider has already been registered with the name "
                    + identityProvider.getIdentityProviderName() + " for tenant " + tenantDomain;
            log.error(msg);
            throw new IdentityApplicationManagementException(msg);
        }

        dao.addIdP(identityProvider, tenantId, tenantDomain);

        if (identityProvider.getCertificate() != null) {
            IdPManagementUtil.importCertToStore(identityProvider.getIdentityProviderName(),
                    encodedCert, tenantId, tenantDomain);
        }
    }

    /**
     * Deletes an Identity Provider from a given tenant
     * 
     * @param idPName Name of the IdP to be deleted
     * @throws IdentityProviderMgtException Error when deleting Identity Provider information
     */
    public void deleteIdP(String idPName, String tenantDomain)
            throws IdentityApplicationManagementException {

        int tenantId = getTenantIdOfDomain(tenantDomain);
        if (idPName == null || idPName.equals("")) {
            String msg = "Invalid argument: Identity Provider Name value is empty";
            log.error(msg);
            throw new IdentityApplicationManagementException(msg);
        }
        dao.deleteIdP(idPName, tenantId, tenantDomain);
        IdPManagementUtil.deleteCertFromStore(idPName, tenantId, tenantDomain);
    }

    /**
     * Updates a given Identity Provider information
     * 
     * @param oldIdPName existing Identity Provider name
     * @param newIdentityProvider new IdP information
     * @throws IdentityProviderMgtException Error when updating Identity Provider information
     */
    public void updateIdP(String oldIdPName, FederatedIdentityProvider newIdentityProvider,
            String tenantDomain) throws IdentityApplicationManagementException {

        if (newIdentityProvider == null) {
            String msg = "Invalid argument: 'newIdentityProvider' is NULL\'";
            log.error(msg);
            throw new IdentityApplicationManagementException(msg);
        }

        String newEncodedCert = newIdentityProvider.getCertificate();

        int tenantId = getTenantIdOfDomain(tenantDomain);

        if (oldIdPName == null || oldIdPName.equals("")) {
            String msg = "Invalid argument: Existing Identity Provider Name value is empty";
            log.error(msg);
            throw new IdentityApplicationManagementException(msg);
        }

        FederatedIdentityProvider currentIdentityProvider = this.getIdPByName(oldIdPName, tenantDomain);
        if (currentIdentityProvider == null) {
            String msg = "Identity Provider with name " + oldIdPName + " does not exist";
            log.error(msg);
            throw new IdentityApplicationManagementException(msg);
        }

        if (currentIdentityProvider.isPrimary() == true && newIdentityProvider.isPrimary() == false) {
            String msg = "Invalid argument: Cannot unset Identity Provider from primary. "
                    + "Alternatively set new Identity Provider to primary";
            log.error(msg);
            throw new IdentityApplicationManagementException(msg);
        }

        if (currentIdentityProvider.getCertificate() != null) {
            try {
                currentIdentityProvider.setCertificate(IdentityApplicationManagementUtil
                        .generateThumbPrint(currentIdentityProvider.getCertificate()));
            } catch (NoSuchAlgorithmException e) {
                log.error(e.getMessage(), e);
                throw new IdentityApplicationManagementException(
                        "Error occurred while generating thumbprint of Identity Provider's old public certificate");
            }
        }

        if (newIdentityProvider.getIdentityProviderName() == null
                || newIdentityProvider.getIdentityProviderName().equals("")) {
            String msg = "Invalid argument: Identity Provider Name value is empty for \'newIdentityProvider\'";
            log.error(msg);
            throw new IdentityApplicationManagementException(msg);
        }

        if (newIdentityProvider.getCertificate() != null) {
            try {
                newIdentityProvider.setCertificate(IdentityApplicationManagementUtil
                        .generateThumbPrint(newIdentityProvider.getCertificate()));
            } catch (NoSuchAlgorithmException e) {
                log.error(e.getMessage(), e);
                throw new IdentityApplicationManagementException(
                        "Error occurred while generating thumbprint of Identity Provider's new public certificate");
            }
        }

        if (newIdentityProvider.getPermissionAndRoleConfiguration() != null
                && newIdentityProvider.getPermissionAndRoleConfiguration().getRoleMappings() != null) {
            for (RoleMapping mapping : newIdentityProvider.getPermissionAndRoleConfiguration()
                    .getRoleMappings()) {
                UserStoreManager usm = null;
                try {
                    usm = CarbonContext.getThreadLocalCarbonContext().getUserRealm()
                            .getUserStoreManager();
                    String role = null;
                    if (mapping.getLocalRole().getUserStoreId() != null) {
                        role = mapping.getLocalRole().getUserStoreId()
                                + CarbonConstants.DOMAIN_SEPARATOR
                                + mapping.getLocalRole().getLocalRoleName();
                    } else {
                        role = mapping.getLocalRole().getLocalRoleName();
                    }
                    if (!usm.isExistingRole(role) && !usm.isExistingRole(role, true)) {
                        String msg = "Cannot find tenant role " + role + " for tenant "
                                + tenantDomain;
                        log.error(msg);
                        throw new IdentityApplicationManagementException(msg);
                    }
                } catch (UserStoreException e) {
                    String msg = "Error occurred while retrieving UserStoreManager for tenant "
                            + tenantDomain;
                    log.error(msg);
                    throw new IdentityApplicationManagementException(msg);
                }
            }
        }

        dao.updateIdP(newIdentityProvider, currentIdentityProvider, tenantId, tenantDomain);

        if (currentIdentityProvider.getCertificate() != null
                && newIdentityProvider.getCertificate() != null
                && !currentIdentityProvider.getCertificate().equals(
                        newIdentityProvider.getCertificate())) {
            IdPManagementUtil.updateCertToStore(currentIdentityProvider.getIdentityProviderName(),
                    newIdentityProvider.getIdentityProviderName(), newEncodedCert, tenantId,
                    tenantDomain);
        } else if (currentIdentityProvider.getCertificate() == null
                && newIdentityProvider.getCertificate() != null) {
            IdPManagementUtil.importCertToStore(newIdentityProvider.getIdentityProviderName(),
                    newEncodedCert, tenantId, tenantDomain);
        } else if (currentIdentityProvider.getCertificate() != null
                && newIdentityProvider.getCertificate() == null) {
            IdPManagementUtil.deleteCertFromStore(currentIdentityProvider.getIdentityProviderName(),
                    tenantId, tenantDomain);
        }
    }

    /**
     * Get the tenant id of the given tenant domain.
     * 
     * @param tenantDomain Tenant Domain
     * @return Tenant Id of domain user belongs to.
     * @throws IdentityProviderMgtException Error when getting tenant id from tenant domain
     */
    private static int getTenantIdOfDomain(String tenantDomain)
            throws IdentityApplicationManagementException {

        try {
            return IdPManagementUtil.getTenantIdOfDomain(tenantDomain);
        } catch (UserStoreException e) {
            log.error(e.getMessage(), e);
            String msg = "Error occurred while getting Tenant Id from Tenant domain "
                    + tenantDomain;
            throw new IdentityApplicationManagementException(msg);
        }
    }

}
