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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.FederatedIdentityProvider;
import org.wso2.carbon.identity.application.common.model.ResidentIdentityProvider;
import org.wso2.carbon.idp.mgt.cache.IdPCacheByHRI;
import org.wso2.carbon.idp.mgt.cache.IdPCacheByName;
import org.wso2.carbon.idp.mgt.cache.IdPCacheEntry;
import org.wso2.carbon.idp.mgt.cache.IdPHomeRealmIdCacheKey;
import org.wso2.carbon.idp.mgt.cache.IdPNameCacheKey;

public class CacheBackedIdPMgtDAO {

    private static final Log log = LogFactory.getLog(CacheBackedIdPMgtDAO.class);

    private IdPManagementDAO idPMgtDAO = null;

    private IdPCacheByName idPCacheByName = null;
    private IdPCacheByHRI idPCacheByHRI = null;
    private Map<String, FederatedIdentityProvider> primaryIdPs = null;
    private Map<String, ResidentIdentityProvider> residentIdPs = null;

    /**
     * 
     * @param idPMgtDAO
     */
    public CacheBackedIdPMgtDAO(IdPManagementDAO idPMgtDAO) {
        this.idPMgtDAO = idPMgtDAO;
        idPCacheByName = IdPCacheByName.getInstance();
        idPCacheByHRI = IdPCacheByHRI.getInstance();
        primaryIdPs = new ConcurrentHashMap<String, FederatedIdentityProvider>();
        residentIdPs = new ConcurrentHashMap<String, ResidentIdentityProvider>();
    }

    /**
     * 
     * @param tenantId
     * @param tenantDomain
     * @return
     * @throws IdentityApplicationManagementException
     */
    public ResidentIdentityProvider getResidentIdP(int tenantId, String tenantDomain)
            throws IdentityApplicationManagementException {

        ResidentIdentityProvider identityProvider = residentIdPs.get(tenantDomain);
        if (identityProvider != null) {
            return identityProvider;
        }
        identityProvider = idPMgtDAO.getResidentIdP(tenantId, tenantDomain);
        if (identityProvider != null) {
            residentIdPs.put(tenantDomain, identityProvider);
        }
        return identityProvider;
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

        idPMgtDAO.addResidentIdP(identityProvider, tenantId, tenantDomain);
        residentIdPs.put(tenantDomain, identityProvider);
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

        residentIdPs.remove(tenantDomain);
        idPMgtDAO.updateResidentIdP(identityProvider, tenantId, tenantDomain);
        residentIdPs.put(tenantDomain, identityProvider);
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

        return idPMgtDAO.getIdPs(dbConnection, tenantId, tenantDomain);
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

        IdPNameCacheKey cacheKey = new IdPNameCacheKey(idPName);
        IdPCacheEntry entry = ((IdPCacheEntry) idPCacheByName.getValueFromCache(cacheKey));

        if (entry != null) {
            log.debug("Cache entry found for Identity Provider " + idPName);
            FederatedIdentityProvider identityProvider = entry.getIdentityProvider();
            if (identityProvider.getHomeRealmId() != null) {
                IdPHomeRealmIdCacheKey idPHomeRealmIdCacheKey = new IdPHomeRealmIdCacheKey(
                        identityProvider.getHomeRealmId());
                idPCacheByHRI.addToCache(idPHomeRealmIdCacheKey,
                        new IdPCacheEntry(identityProvider));
            }

            if (identityProvider.isPrimary()) {
                primaryIdPs.put(tenantDomain, identityProvider);
            }

            return identityProvider;
        } else {
            log.debug("Cache entry not found for Identity Provider " + idPName
                    + ". Fetching entry from DB");
        }

        FederatedIdentityProvider identityProvider = idPMgtDAO.getIdPByName(dbConnection, idPName,
                tenantId, tenantDomain);

        if (identityProvider != null) {
            log.debug("Entry fetched from DB for Identity Provider " + idPName + ". Updating cache");
            idPCacheByName.addToCache(cacheKey, new IdPCacheEntry(identityProvider));
            if (identityProvider.getHomeRealmId() != null) {
                IdPHomeRealmIdCacheKey homeRealmIdCacheKey = new IdPHomeRealmIdCacheKey(
                        identityProvider.getHomeRealmId());
                idPCacheByHRI.addToCache(homeRealmIdCacheKey, new IdPCacheEntry(identityProvider));
            }
            if (identityProvider.isPrimary()) {
                primaryIdPs.put(tenantDomain, identityProvider);

            }
        } else {
            log.debug("Entry for Identity Provider " + idPName + " not found in cache or DB");
        }

        return identityProvider;
    }

    /**
     * 
     * @param realmId
     * @param tenantId
     * @param tenantDomain
     * @return
     * @throws IdentityApplicationManagementException
     */
    public FederatedIdentityProvider getIdPByRealmId(String realmId, int tenantId,
            String tenantDomain) throws IdentityApplicationManagementException {

        IdPHomeRealmIdCacheKey cacheKey = new IdPHomeRealmIdCacheKey(realmId);
        IdPCacheEntry entry = ((IdPCacheEntry) idPCacheByHRI.getValueFromCache(cacheKey));
        if (entry != null) {
            log.debug("Cache entry found for Identity Provider with Home Realm ID " + realmId);
            FederatedIdentityProvider identityProvider = entry.getIdentityProvider();
            IdPNameCacheKey idPNameCacheKey = new IdPNameCacheKey(
                    identityProvider.getIdentityProviderName());
            idPCacheByName.addToCache(idPNameCacheKey, new IdPCacheEntry(identityProvider));
            if (identityProvider.isPrimary()) {
                primaryIdPs.put(tenantDomain, identityProvider);
            }
            return entry.getIdentityProvider();
        } else {
            log.debug("Cache entry not found for Identity Provider with Home Realm ID " + realmId
                    + ". Fetching entry from DB");
        }

        FederatedIdentityProvider identityProvider = idPMgtDAO.getIdPByRealmId(realmId, tenantId,
                tenantDomain);

        if (identityProvider != null) {
            log.debug("Entry fetched from DB for Identity Provider with Home Realm ID " + realmId
                    + ". Updating cache");
            idPCacheByHRI.addToCache(cacheKey, new IdPCacheEntry(identityProvider));
            IdPNameCacheKey idPNameCacheKey = new IdPNameCacheKey(
                    identityProvider.getIdentityProviderName());
            idPCacheByName.addToCache(idPNameCacheKey, new IdPCacheEntry(identityProvider));
            if (identityProvider.isPrimary()) {
                primaryIdPs.put(tenantDomain, identityProvider);
            }
        } else {
            log.debug("Entry for Identity Provider with Home Realm ID " + realmId
                    + " not found in cache or DB");
        }

        return identityProvider;
    }

    /**
     * 
     * @param identityProvider
     * @param tenantId
     * @param tenantDomain
     * @throws IdentityApplicationManagementException
     */
    public void addIdP(FederatedIdentityProvider identityProvider, int tenantId, String tenantDomain)
            throws IdentityApplicationManagementException {

        idPMgtDAO.addIdP(identityProvider, tenantId);

        identityProvider = idPMgtDAO.getIdPByName(null, identityProvider.getIdentityProviderName(),
                tenantId, tenantDomain);
        if (identityProvider != null) {
            log.debug("Adding new entry for Identity Provider "
                    + identityProvider.getIdentityProviderName() + " to cache");
            IdPNameCacheKey idPNameCacheKey = new IdPNameCacheKey(
                    identityProvider.getIdentityProviderName());
            idPCacheByName.addToCache(idPNameCacheKey, new IdPCacheEntry(identityProvider));
            if (identityProvider.getHomeRealmId() != null) {
                IdPHomeRealmIdCacheKey idPHomeRealmIdCacheKey = new IdPHomeRealmIdCacheKey(
                        identityProvider.getHomeRealmId());
                idPCacheByHRI.addToCache(idPHomeRealmIdCacheKey,
                        new IdPCacheEntry(identityProvider));
            }
            if (identityProvider.isPrimary()) {
                primaryIdPs.put(tenantDomain, identityProvider);
            }
        } else {
            log.debug("Entry for Identity Provider not found in DB");
        }
    }

    /**
     * 
     * @param newIdentityProvider
     * @param currentIdentityProvider
     * @param tenantId
     * @param tenantDomain
     * @throws IdentityApplicationManagementException
     */
    public void updateIdP(FederatedIdentityProvider newIdentityProvider,
            FederatedIdentityProvider currentIdentityProvider, int tenantId, String tenantDomain)
            throws IdentityApplicationManagementException {

        log.debug("Removing entry for Identity Provider "
                + currentIdentityProvider.getIdentityProviderName() + " from cache");

        IdPNameCacheKey idPNameCacheKey = new IdPNameCacheKey(
                currentIdentityProvider.getIdentityProviderName());

        idPCacheByName.clearCacheEntry(idPNameCacheKey);
        if (currentIdentityProvider.getHomeRealmId() != null) {
            IdPHomeRealmIdCacheKey idPHomeRealmIdCacheKey = new IdPHomeRealmIdCacheKey(
                    currentIdentityProvider.getHomeRealmId());
            idPCacheByHRI.clearCacheEntry(idPHomeRealmIdCacheKey);
        }
        if (currentIdentityProvider.isPrimary()) {
            primaryIdPs.remove(tenantDomain);
        }

        idPMgtDAO.updateIdP(newIdentityProvider, currentIdentityProvider, tenantId);

        FederatedIdentityProvider identityProvider = idPMgtDAO.getIdPByName(null,
                newIdentityProvider.getIdentityProviderName(), tenantId, tenantDomain);

        if (identityProvider != null) {
            log.debug("Adding new entry for Identity Provider "
                    + newIdentityProvider.getIdentityProviderName() + " to cache");
            idPNameCacheKey = new IdPNameCacheKey(identityProvider.getIdentityProviderName());
            idPCacheByName.addToCache(idPNameCacheKey, new IdPCacheEntry(identityProvider));
            if (identityProvider.getHomeRealmId() != null) {
                IdPHomeRealmIdCacheKey idPHomeRealmIdCacheKey = new IdPHomeRealmIdCacheKey(
                        identityProvider.getHomeRealmId());
                idPCacheByHRI.addToCache(idPHomeRealmIdCacheKey,
                        new IdPCacheEntry(identityProvider));
            }
            if (identityProvider.isPrimary()) {
                primaryIdPs.put(tenantDomain, identityProvider);
            }
        } else {
            log.debug("Entry for Identity Provider "
                    + newIdentityProvider.getIdentityProviderName() + " not found in DB");
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

        log.debug("Removing entry for Identity Provider " + idPName + " from cache");
        FederatedIdentityProvider identityProvider = this.getIdPByName(null, idPName, tenantId,
                tenantDomain);
        IdPNameCacheKey idPNameCacheKey = new IdPNameCacheKey(idPName);
        idPCacheByName.clearCacheEntry(idPNameCacheKey);
        if (identityProvider.getHomeRealmId() != null) {
            IdPHomeRealmIdCacheKey idPHomeRealmIdCacheKey = new IdPHomeRealmIdCacheKey(
                    identityProvider.getHomeRealmId());
            idPCacheByHRI.clearCacheEntry(idPHomeRealmIdCacheKey);
        }
        if (identityProvider.isPrimary()) {
            primaryIdPs.remove(tenantDomain);
        }

        idPMgtDAO.deleteIdP(idPName, tenantId, tenantDomain);

    }

    /**
     * 
     * @param dbConnection
     * @param tenantId
     * @param tenantDomain
     * @return
     * @throws IdentityApplicationManagementException
     */
    public FederatedIdentityProvider getPrimaryIdP(Connection dbConnection, int tenantId,
            String tenantDomain) throws IdentityApplicationManagementException {

        FederatedIdentityProvider identityProvider = primaryIdPs.get(tenantDomain);
        if (identityProvider != null) {
            log.debug("Cache entry found for primary Identity Provider of tenant " + tenantDomain);
            IdPNameCacheKey idPNameCacheKey = new IdPNameCacheKey(
                    identityProvider.getIdentityProviderName());
            idPCacheByName.addToCache(idPNameCacheKey, new IdPCacheEntry(identityProvider));
            if (identityProvider.getHomeRealmId() != null) {
                IdPHomeRealmIdCacheKey idPHomeRealmIdCacheKey = new IdPHomeRealmIdCacheKey(
                        identityProvider.getHomeRealmId());
                idPCacheByHRI.addToCache(idPHomeRealmIdCacheKey,
                        new IdPCacheEntry(identityProvider));
            }
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
                    identityProvider.getIdentityProviderName());
            idPCacheByName.addToCache(idPNameCacheKey, new IdPCacheEntry(identityProvider));
            if (identityProvider.getHomeRealmId() != null) {
                IdPHomeRealmIdCacheKey idPHomeRealmIdCacheKey = new IdPHomeRealmIdCacheKey(
                        identityProvider.getHomeRealmId());
                idPCacheByHRI.addToCache(idPHomeRealmIdCacheKey,
                        new IdPCacheEntry(identityProvider));
            }
            primaryIdPs.put(tenantDomain, identityProvider);
        } else {
            log.debug("Entry for primary Identity Provider of tenant " + tenantDomain
                    + " not found in cache or DB");
        }

        return identityProvider;
    }

    /**
     * 
     * @param tenantId
     * @param role
     * @param tenantDomain
     * @throws IdentityProviderMgtException
     */
    public void deleteTenantRole(int tenantId, String role, String tenantDomain)
            throws IdentityApplicationManagementException {

        log.debug("Removing all cached Identity Provider entries for tenant Domain " + tenantDomain);
        List<FederatedIdentityProvider> identityProviders = this.getIdPs(null, tenantId,
                tenantDomain);
        for (FederatedIdentityProvider identityProvider : identityProviders) {
            identityProvider = this.getIdPByName(null, identityProvider.getIdentityProviderName(),
                    tenantId, tenantDomain);
            IdPNameCacheKey idPNameCacheKey = new IdPNameCacheKey(
                    identityProvider.getIdentityProviderName());
            idPCacheByName.clearCacheEntry(idPNameCacheKey);
            if (identityProvider.getHomeRealmId() != null) {
                IdPHomeRealmIdCacheKey idPHomeRealmIdCacheKey = new IdPHomeRealmIdCacheKey(
                        identityProvider.getHomeRealmId());
                idPCacheByHRI.clearCacheEntry(idPHomeRealmIdCacheKey);
            }
            if (identityProvider.isPrimary()) {
                primaryIdPs.remove(tenantDomain);
            }
        }

        idPMgtDAO.deleteTenantRole(tenantId, role, tenantDomain);
    }

    /**
     * 
     * @param newRoleName
     * @param oldRoleName
     * @param tenantId
     * @param tenantDomain
     * @throws IdentityApplicationManagementException
     */
    public void renameTenantRole(String newRoleName, String oldRoleName, int tenantId,
            String tenantDomain) throws IdentityApplicationManagementException {

        log.debug("Removing all cached Identity Provider entries for tenant Domain " + tenantDomain);
        List<FederatedIdentityProvider> identityProviders = this.getIdPs(null, tenantId,
                tenantDomain);
        for (FederatedIdentityProvider identityProvider : identityProviders) {
            identityProvider = this.getIdPByName(null, identityProvider.getIdentityProviderName(),
                    tenantId, tenantDomain);
            IdPNameCacheKey idPNameCacheKey = new IdPNameCacheKey(
                    identityProvider.getIdentityProviderName());
            idPCacheByName.clearCacheEntry(idPNameCacheKey);
            if (identityProvider.getHomeRealmId() != null) {
                IdPHomeRealmIdCacheKey idPHomeRealmIdCacheKey = new IdPHomeRealmIdCacheKey(
                        identityProvider.getHomeRealmId());
                idPCacheByHRI.clearCacheEntry(idPHomeRealmIdCacheKey);
            }
            if (identityProvider.isPrimary()) {
                primaryIdPs.remove(tenantDomain);
            }
        }

        idPMgtDAO.renameTenantRole(newRoleName, oldRoleName, tenantId, tenantDomain);
    }

    /**
     * 
     * @param tenantId
     * @param claimURI
     * @param tenantDomain
     * @throws IdentityApplicationManagementException
     */
    public void deleteTenantClaimURI(int tenantId, String claimURI, String tenantDomain)
            throws IdentityApplicationManagementException {

        log.debug("Removing all cached Identity Provider entries for tenant Domain " + tenantDomain);
        List<FederatedIdentityProvider> identityProviders = this.getIdPs(null, tenantId,
                tenantDomain);
        for (FederatedIdentityProvider identityProvider : identityProviders) {
            identityProvider = this.getIdPByName(null, identityProvider.getIdentityProviderName(),
                    tenantId, tenantDomain);
            IdPNameCacheKey idPNameCacheKey = new IdPNameCacheKey(
                    identityProvider.getIdentityProviderName());
            idPCacheByName.clearCacheEntry(idPNameCacheKey);
            if (identityProvider.getHomeRealmId() != null) {
                IdPHomeRealmIdCacheKey idPHomeRealmIdCacheKey = new IdPHomeRealmIdCacheKey(
                        identityProvider.getHomeRealmId());
                idPCacheByHRI.clearCacheEntry(idPHomeRealmIdCacheKey);
            }
            if (identityProvider.isPrimary()) {
                primaryIdPs.remove(tenantDomain);
            }
        }

        idPMgtDAO.deleteTenantRole(tenantId, claimURI, tenantDomain);
    }

    /**
     * 
     * @param newClaimURI
     * @param oldClaimURI
     * @param tenantId
     * @param tenantDomain
     * @throws IdentityApplicationManagementException
     */
    public void renameTenantClaimURI(String newClaimURI, String oldClaimURI, int tenantId,
            String tenantDomain) throws IdentityApplicationManagementException {

        log.debug("Removing all cached Identity Provider entries for tenant Domain " + tenantDomain);
        List<FederatedIdentityProvider> identityProviders = this.getIdPs(null, tenantId,
                tenantDomain);
        for (FederatedIdentityProvider identityProvider : identityProviders) {
            identityProvider = this.getIdPByName(null, identityProvider.getIdentityProviderName(),
                    tenantId, tenantDomain);
            IdPNameCacheKey idPNameCacheKey = new IdPNameCacheKey(
                    identityProvider.getIdentityProviderName());
            idPCacheByName.clearCacheEntry(idPNameCacheKey);
            if (identityProvider.getHomeRealmId() != null) {
                IdPHomeRealmIdCacheKey idPHomeRealmIdCacheKey = new IdPHomeRealmIdCacheKey(
                        identityProvider.getHomeRealmId());
                idPCacheByHRI.clearCacheEntry(idPHomeRealmIdCacheKey);
            }
            if (identityProvider.isPrimary()) {
                primaryIdPs.remove(tenantDomain);
            }
        }

        idPMgtDAO.renameTenantRole(newClaimURI, oldClaimURI, tenantId, tenantDomain);
    }

}
