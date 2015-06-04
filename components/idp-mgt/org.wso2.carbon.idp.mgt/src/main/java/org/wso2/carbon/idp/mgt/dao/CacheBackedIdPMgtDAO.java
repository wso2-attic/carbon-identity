/*
 *Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants;
import org.wso2.carbon.idp.mgt.cache.*;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CacheBackedIdPMgtDAO {

    private static final Log log = LogFactory.getLog(CacheBackedIdPMgtDAO.class);

    private IdPManagementDAO idPMgtDAO = null;

    private IdPCacheByName idPCacheByName = null;
    private IdPCacheByHRI idPCacheByHRI = null;
    private IdPCacheByAuthProperty idPCacheByAuthProperty = null;
    private Map<String, IdentityProvider> primaryIdPs = null;
    private Map<String, IdentityProvider> residentIdPs = null;

    /**
     * @param idPMgtDAO
     */
    public CacheBackedIdPMgtDAO(IdPManagementDAO idPMgtDAO) {
        this.idPMgtDAO = idPMgtDAO;
        idPCacheByName = IdPCacheByName.getInstance();
        idPCacheByHRI = IdPCacheByHRI.getInstance();
        idPCacheByAuthProperty = IdPCacheByAuthProperty.getInstance();
        primaryIdPs = new ConcurrentHashMap<String, IdentityProvider>();
        residentIdPs = new ConcurrentHashMap<String, IdentityProvider>();
    }

    /**
     * @param dbConnection
     * @param tenantId
     * @param tenantDomain
     * @return
     * @throws IdentityApplicationManagementException
     */
    public List<IdentityProvider> getIdPs(Connection dbConnection, int tenantId,
                                          String tenantDomain) throws IdentityApplicationManagementException {

        return idPMgtDAO.getIdPs(dbConnection, tenantId, tenantDomain);
    }

    /**
     * @param dbConnection
     * @param idPName
     * @param tenantId
     * @param tenantDomain
     * @return
     * @throws IdentityApplicationManagementException
     */
    public IdentityProvider getIdPByName(Connection dbConnection, String idPName,
                                         int tenantId, String tenantDomain) throws IdentityApplicationManagementException {

        IdPNameCacheKey cacheKey = new IdPNameCacheKey(idPName, tenantDomain);
        IdPCacheEntry entry = ((IdPCacheEntry) idPCacheByName.getValueFromCache(cacheKey));

        if (entry != null) {
            log.debug("Cache entry found for Identity Provider " + idPName);
            IdentityProvider identityProvider = entry.getIdentityProvider();
            return identityProvider;
        } else {
            log.debug("Cache entry not found for Identity Provider " + idPName
                    + ". Fetching entry from DB");
        }

        IdentityProvider identityProvider = idPMgtDAO.getIdPByName(dbConnection, idPName,
                tenantId, tenantDomain);

        if (identityProvider != null) {
            log.debug("Entry fetched from DB for Identity Provider " + idPName + ". Updating cache");
            idPCacheByName.addToCache(cacheKey, new IdPCacheEntry(identityProvider));
            if (identityProvider.getHomeRealmId() != null) {
                IdPHomeRealmIdCacheKey homeRealmIdCacheKey = new IdPHomeRealmIdCacheKey(
                        identityProvider.getHomeRealmId(), tenantDomain);
                idPCacheByHRI.addToCache(homeRealmIdCacheKey, new IdPCacheEntry(identityProvider));
            }
            if (identityProvider.isPrimary()) {
                primaryIdPs.put(tenantDomain, identityProvider);

            }
            if (IdentityApplicationConstants.RESIDENT_IDP_RESERVED_NAME.equals(
                    identityProvider.getIdentityProviderName())) {
                residentIdPs.put(tenantDomain, identityProvider);
            }
        } else {
            log.debug("Entry for Identity Provider " + idPName + " not found in cache or DB");
        }

        return identityProvider;
    }

    /**
     * @param dbConnection
     * @param property
     * @param value
     * @param tenantId
     * @param tenantDomain
     * @return
     * @throws IdentityApplicationManagementException
     */
    public IdentityProvider getIdPByAuthenticatorPropertyValue(Connection dbConnection, String property, String value,
                                                               int tenantId, String tenantDomain)
            throws IdentityApplicationManagementException {

        IdPAuthPropertyCacheKey cacheKey = new IdPAuthPropertyCacheKey(property, value, tenantDomain);
        IdPCacheEntry entry = ((IdPCacheEntry) idPCacheByAuthProperty.getValueFromCache(cacheKey));

        if (entry != null) {
            log.debug("Cache entry found for Identity Provider with authenticator property " + property
                    + " and with value " + value);
            IdentityProvider identityProvider = entry.getIdentityProvider();
            return identityProvider;
        } else {
            log.debug("Cache entry not found for Identity Provider with authenticator property " + property
                    + " and with value " + value + ". Fetching entry from DB");
        }

        IdentityProvider identityProvider = idPMgtDAO.getIdPByAuthenticatorPropertyValue(dbConnection, property, value,
                tenantId, tenantDomain);

        if (identityProvider != null) {
            log.debug("Entry fetched from DB for Identity Provider with authenticator property " + property
                    + " and with value " + value + ". Updating cache");
            idPCacheByName.addToCache(cacheKey, new IdPCacheEntry(identityProvider));
            if (identityProvider.getHomeRealmId() != null) {
                IdPHomeRealmIdCacheKey homeRealmIdCacheKey = new IdPHomeRealmIdCacheKey(
                        identityProvider.getHomeRealmId(), tenantDomain);
                idPCacheByHRI.addToCache(homeRealmIdCacheKey, new IdPCacheEntry(identityProvider));
            }
            if (identityProvider.isPrimary()) {
                primaryIdPs.put(tenantDomain, identityProvider);

            }
            if (IdentityApplicationConstants.RESIDENT_IDP_RESERVED_NAME.equals(
                    identityProvider.getIdentityProviderName())) {
                residentIdPs.put(tenantDomain, identityProvider);
            }
        } else {
            log.debug("Entry for Identity Provider with authenticator property " + property + " and with value "
                    + value + " not found in cache or DB");
        }

        return identityProvider;
    }

    /**
     * @param realmId
     * @param tenantId
     * @param tenantDomain
     * @return
     * @throws IdentityApplicationManagementException
     */
    public IdentityProvider getIdPByRealmId(String realmId, int tenantId,
                                            String tenantDomain) throws IdentityApplicationManagementException {

        IdPHomeRealmIdCacheKey cacheKey = new IdPHomeRealmIdCacheKey(realmId, tenantDomain);
        IdPCacheEntry entry = ((IdPCacheEntry) idPCacheByHRI.getValueFromCache(cacheKey));
        if (entry != null) {
            log.debug("Cache entry found for Identity Provider with Home Realm ID " + realmId);
            return entry.getIdentityProvider();
        } else {
            log.debug("Cache entry not found for Identity Provider with Home Realm ID " + realmId
                    + ". Fetching entry from DB");
        }

        IdentityProvider identityProvider = idPMgtDAO.getIdPByRealmId(realmId, tenantId, tenantDomain);

        if (identityProvider != null) {
            log.debug("Entry fetched from DB for Identity Provider with Home Realm ID " + realmId
                    + ". Updating cache");
            idPCacheByHRI.addToCache(cacheKey, new IdPCacheEntry(identityProvider));
            IdPNameCacheKey idPNameCacheKey = new IdPNameCacheKey(
                    identityProvider.getIdentityProviderName(), tenantDomain);
            idPCacheByName.addToCache(idPNameCacheKey, new IdPCacheEntry(identityProvider));
            if (identityProvider.isPrimary()) {
                primaryIdPs.put(tenantDomain, identityProvider);
            }
            if (IdentityApplicationConstants.RESIDENT_IDP_RESERVED_NAME.equals(
                    identityProvider.getIdentityProviderName())) {
                residentIdPs.put(tenantDomain, identityProvider);
            }
        } else {
            log.debug("Entry for Identity Provider with Home Realm ID " + realmId
                    + " not found in cache or DB");
        }

        return identityProvider;
    }

    /**
     * @param identityProvider
     * @param tenantId
     * @param tenantDomain
     * @throws IdentityApplicationManagementException
     */
    public void addIdP(IdentityProvider identityProvider, int tenantId, String tenantDomain)
            throws IdentityApplicationManagementException {

        idPMgtDAO.addIdP(identityProvider, tenantId);

        identityProvider = idPMgtDAO.getIdPByName(null, identityProvider.getIdentityProviderName(),
                tenantId, tenantDomain);
        if (identityProvider != null) {
            log.debug("Adding new entry for Identity Provider "
                    + identityProvider.getIdentityProviderName() + " to cache");
            IdPNameCacheKey idPNameCacheKey = new IdPNameCacheKey(
                    identityProvider.getIdentityProviderName(), tenantDomain);
            idPCacheByName.addToCache(idPNameCacheKey, new IdPCacheEntry(identityProvider));
            if (identityProvider.getHomeRealmId() != null) {
                IdPHomeRealmIdCacheKey idPHomeRealmIdCacheKey = new IdPHomeRealmIdCacheKey(
                        identityProvider.getHomeRealmId(), tenantDomain);
                idPCacheByHRI.addToCache(idPHomeRealmIdCacheKey,
                        new IdPCacheEntry(identityProvider));
            }
            if (identityProvider.isPrimary()) {
                primaryIdPs.put(tenantDomain, identityProvider);
            }
            if (IdentityApplicationConstants.RESIDENT_IDP_RESERVED_NAME.equals(
                    identityProvider.getIdentityProviderName())) {
                residentIdPs.put(tenantDomain, identityProvider);
            }
        } else {
            log.debug("Entry for Identity Provider not found in DB");
        }
    }

    /**
     * @param newIdentityProvider
     * @param currentIdentityProvider
     * @param tenantId
     * @param tenantDomain
     * @throws IdentityApplicationManagementException
     */
    public void updateIdP(IdentityProvider newIdentityProvider,
                          IdentityProvider currentIdentityProvider, int tenantId, String tenantDomain)
            throws IdentityApplicationManagementException {

        log.debug("Removing entry for Identity Provider "
                + currentIdentityProvider.getIdentityProviderName() + " from cache");

        IdPNameCacheKey idPNameCacheKey = new IdPNameCacheKey(
                currentIdentityProvider.getIdentityProviderName(), tenantDomain);

        idPCacheByName.clearCacheEntry(idPNameCacheKey);
        if (currentIdentityProvider.getHomeRealmId() != null) {
            IdPHomeRealmIdCacheKey idPHomeRealmIdCacheKey = new IdPHomeRealmIdCacheKey(
                    currentIdentityProvider.getHomeRealmId(), tenantDomain);
            idPCacheByHRI.clearCacheEntry(idPHomeRealmIdCacheKey);
        }
        if (currentIdentityProvider.isPrimary()) {
            primaryIdPs.remove(tenantDomain);
        }
        if (IdentityApplicationConstants.RESIDENT_IDP_RESERVED_NAME.equals(
                currentIdentityProvider.getIdentityProviderName())) {
            residentIdPs.remove(tenantDomain);
        }

        idPMgtDAO.updateIdP(newIdentityProvider, currentIdentityProvider, tenantId);

        IdentityProvider identityProvider = idPMgtDAO.getIdPByName(null,
                newIdentityProvider.getIdentityProviderName(), tenantId, tenantDomain);

        if (identityProvider != null) {
            log.debug("Adding new entry for Identity Provider "
                    + newIdentityProvider.getIdentityProviderName() + " to cache");
            idPNameCacheKey = new IdPNameCacheKey(identityProvider.getIdentityProviderName(), tenantDomain);
            idPCacheByName.addToCache(idPNameCacheKey, new IdPCacheEntry(identityProvider));
            if (identityProvider.getHomeRealmId() != null) {
                IdPHomeRealmIdCacheKey idPHomeRealmIdCacheKey = new IdPHomeRealmIdCacheKey(
                        identityProvider.getHomeRealmId(), tenantDomain);
                idPCacheByHRI.addToCache(idPHomeRealmIdCacheKey,
                        new IdPCacheEntry(identityProvider));
            }
            if (identityProvider.isPrimary()) {
                primaryIdPs.put(tenantDomain, identityProvider);
            }
            if (IdentityApplicationConstants.RESIDENT_IDP_RESERVED_NAME.equals(
                    identityProvider.getIdentityProviderName())) {
                residentIdPs.put(tenantDomain, identityProvider);
            }
        } else {
            log.debug("Entry for Identity Provider "
                    + newIdentityProvider.getIdentityProviderName() + " not found in DB");
        }
    }

    /**
     * @param idPName
     * @param tenantId
     * @param tenantDomain
     * @throws IdentityApplicationManagementException
     */
    public void deleteIdP(String idPName, int tenantId, String tenantDomain)
            throws IdentityApplicationManagementException {

        if (idPMgtDAO.isIdpReferredBySP(idPName, tenantId)) {
            throw new IdentityApplicationManagementException("Identitiy Provider '" + idPName + "' cannot be deleted as it is reffered by Service Providers.");
        }
        log.debug("Removing entry for Identity Provider " + idPName + " from cache");
        IdentityProvider identityProvider = this.getIdPByName(null, idPName, tenantId,
                tenantDomain);
        IdPNameCacheKey idPNameCacheKey = new IdPNameCacheKey(idPName, tenantDomain);
        idPCacheByName.clearCacheEntry(idPNameCacheKey);
        if (identityProvider.getHomeRealmId() != null) {
            IdPHomeRealmIdCacheKey idPHomeRealmIdCacheKey = new IdPHomeRealmIdCacheKey(
                    identityProvider.getHomeRealmId(), tenantDomain);
            idPCacheByHRI.clearCacheEntry(idPHomeRealmIdCacheKey);
        }
        if (identityProvider.isPrimary()) {
            primaryIdPs.remove(tenantDomain);
        }
        if (IdentityApplicationConstants.RESIDENT_IDP_RESERVED_NAME.equals(
                identityProvider.getIdentityProviderName())) {
            residentIdPs.remove(tenantDomain);
        }

        idPMgtDAO.deleteIdP(idPName, tenantId, tenantDomain);

    }

    /**
     * @param dbConnection
     * @param tenantId
     * @param tenantDomain
     * @return
     * @throws IdentityApplicationManagementException
     */
    public IdentityProvider getPrimaryIdP(Connection dbConnection, int tenantId,
                                          String tenantDomain) throws IdentityApplicationManagementException {

        IdentityProvider identityProvider = primaryIdPs.get(tenantDomain);
        if (identityProvider != null) {
            return identityProvider;
        } else {
            log.debug("Cache entry not found for primary Identity Provider of tenant "
                    + tenantDomain + ". Fetching from DB");
        }

        identityProvider = idPMgtDAO.getPrimaryIdP(dbConnection, tenantId, tenantDomain);

        if (identityProvider != null) {
            log.debug("Entry fetched from DB for primary Identity Provider of tenant "
                    + tenantDomain + ". Updating cache");
            IdPNameCacheKey idPNameCacheKey = new IdPNameCacheKey(
                    identityProvider.getIdentityProviderName(), tenantDomain);
            idPCacheByName.addToCache(idPNameCacheKey, new IdPCacheEntry(identityProvider));
            if (identityProvider.getHomeRealmId() != null) {
                IdPHomeRealmIdCacheKey idPHomeRealmIdCacheKey = new IdPHomeRealmIdCacheKey(
                        identityProvider.getHomeRealmId(), tenantDomain);
                idPCacheByHRI.addToCache(idPHomeRealmIdCacheKey,
                        new IdPCacheEntry(identityProvider));
            }
            primaryIdPs.put(tenantDomain, identityProvider);
            if (IdentityApplicationConstants.RESIDENT_IDP_RESERVED_NAME.equals(
                    identityProvider.getIdentityProviderName())) {
                residentIdPs.put(tenantDomain, identityProvider);
            }
        } else {
            log.debug("Entry for primary Identity Provider of tenant " + tenantDomain
                    + " not found in cache or DB");
        }

        return identityProvider;
    }

    /**
     * @param tenantId
     * @param role
     * @param tenantDomain
     * @throws IdentityApplicationManagementException
     */
    public void deleteTenantRole(int tenantId, String role, String tenantDomain)
            throws IdentityApplicationManagementException {

        log.debug("Removing all cached Identity Provider entries for tenant Domain " + tenantDomain);
        List<IdentityProvider> identityProviders = this.getIdPs(null, tenantId,
                tenantDomain);
        for (IdentityProvider identityProvider : identityProviders) {
            identityProvider = this.getIdPByName(null, identityProvider.getIdentityProviderName(),
                    tenantId, tenantDomain);
            IdPNameCacheKey idPNameCacheKey = new IdPNameCacheKey(
                    identityProvider.getIdentityProviderName(), tenantDomain);
            idPCacheByName.clearCacheEntry(idPNameCacheKey);
            if (identityProvider.getHomeRealmId() != null) {
                IdPHomeRealmIdCacheKey idPHomeRealmIdCacheKey = new IdPHomeRealmIdCacheKey(
                        identityProvider.getHomeRealmId(), tenantDomain);
                idPCacheByHRI.clearCacheEntry(idPHomeRealmIdCacheKey);
            }
            if (identityProvider.isPrimary()) {
                primaryIdPs.remove(tenantDomain);
            }
        }

        idPMgtDAO.deleteTenantRole(tenantId, role, tenantDomain);
    }

    /**
     * @param newRoleName
     * @param oldRoleName
     * @param tenantId
     * @param tenantDomain
     * @throws IdentityApplicationManagementException
     */
    public void renameTenantRole(String newRoleName, String oldRoleName, int tenantId,
                                 String tenantDomain) throws IdentityApplicationManagementException {

        log.debug("Removing all cached Identity Provider entries for tenant Domain " + tenantDomain);
        List<IdentityProvider> identityProviders = this.getIdPs(null, tenantId,
                tenantDomain);
        for (IdentityProvider identityProvider : identityProviders) {
            identityProvider = this.getIdPByName(null, identityProvider.getIdentityProviderName(),
                    tenantId, tenantDomain);
            IdPNameCacheKey idPNameCacheKey = new IdPNameCacheKey(
                    identityProvider.getIdentityProviderName(), tenantDomain);
            idPCacheByName.clearCacheEntry(idPNameCacheKey);
            if (identityProvider.getHomeRealmId() != null) {
                IdPHomeRealmIdCacheKey idPHomeRealmIdCacheKey = new IdPHomeRealmIdCacheKey(
                        identityProvider.getHomeRealmId(), tenantDomain);
                idPCacheByHRI.clearCacheEntry(idPHomeRealmIdCacheKey);
            }
            if (identityProvider.isPrimary()) {
                primaryIdPs.remove(tenantDomain);
            }
        }

        idPMgtDAO.renameTenantRole(newRoleName, oldRoleName, tenantId, tenantDomain);
    }

    /**
     * @param tenantId
     * @param claimURI
     * @param tenantDomain
     * @throws IdentityApplicationManagementException
     */
    public void deleteTenantClaimURI(int tenantId, String claimURI, String tenantDomain)
            throws IdentityApplicationManagementException {

        log.debug("Removing all cached Identity Provider entries for tenant Domain " + tenantDomain);
        List<IdentityProvider> identityProviders = this.getIdPs(null, tenantId,
                tenantDomain);
        for (IdentityProvider identityProvider : identityProviders) {
            identityProvider = this.getIdPByName(null, identityProvider.getIdentityProviderName(),
                    tenantId, tenantDomain);
            IdPNameCacheKey idPNameCacheKey = new IdPNameCacheKey(
                    identityProvider.getIdentityProviderName(), tenantDomain);
            idPCacheByName.clearCacheEntry(idPNameCacheKey);
            if (identityProvider.getHomeRealmId() != null) {
                IdPHomeRealmIdCacheKey idPHomeRealmIdCacheKey = new IdPHomeRealmIdCacheKey(
                        identityProvider.getHomeRealmId(), tenantDomain);
                idPCacheByHRI.clearCacheEntry(idPHomeRealmIdCacheKey);
            }
            if (identityProvider.isPrimary()) {
                primaryIdPs.remove(tenantDomain);
            }
        }

        idPMgtDAO.deleteTenantRole(tenantId, claimURI, tenantDomain);
    }

    /**
     * @param newClaimURI
     * @param oldClaimURI
     * @param tenantId
     * @param tenantDomain
     * @throws IdentityApplicationManagementException
     */
    public void renameTenantClaimURI(String newClaimURI, String oldClaimURI, int tenantId,
                                     String tenantDomain) throws IdentityApplicationManagementException {

        log.debug("Removing all cached Identity Provider entries for tenant Domain " + tenantDomain);
        List<IdentityProvider> identityProviders = this.getIdPs(null, tenantId,
                tenantDomain);
        for (IdentityProvider identityProvider : identityProviders) {
            identityProvider = this.getIdPByName(null, identityProvider.getIdentityProviderName(),
                    tenantId, tenantDomain);
            IdPNameCacheKey idPNameCacheKey = new IdPNameCacheKey(
                    identityProvider.getIdentityProviderName(), tenantDomain);
            idPCacheByName.clearCacheEntry(idPNameCacheKey);
            if (identityProvider.getHomeRealmId() != null) {
                IdPHomeRealmIdCacheKey idPHomeRealmIdCacheKey = new IdPHomeRealmIdCacheKey(
                        identityProvider.getHomeRealmId(), tenantDomain);
                idPCacheByHRI.clearCacheEntry(idPHomeRealmIdCacheKey);
            }
            if (identityProvider.isPrimary()) {
                primaryIdPs.remove(tenantDomain);
            }
        }

        idPMgtDAO.renameTenantRole(newClaimURI, oldClaimURI, tenantId, tenantDomain);
    }

    /**
     * @param idPEntityId
     * @param tenantId
     * @return
     * @throws IdentityApplicationManagementException
     */
    public boolean isSimilarIdPEntityIdsAvailble(String idPEntityId, int tenantId)
            throws IdentityApplicationManagementException {

        return idPMgtDAO.isSimilarIdPEntityIdsAvailble(idPEntityId, tenantId);
    }

}
