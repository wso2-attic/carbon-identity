/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.claim.custom;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.claim.custom.builder.ClaimBuilder;
import org.wso2.carbon.claim.custom.dao.ClaimDAO;
import org.wso2.carbon.claim.custom.internal.ClaimManagementServiceDataHolder;
import org.wso2.carbon.claim.custom.model.Claim;
import org.wso2.carbon.claim.custom.model.ClaimMapping;
import org.wso2.carbon.claim.custom.model.ClaimToClaimMapping;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.claim.DefaultClaimManager;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.util.DatabaseUtil;

import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class IdentityMgtClaimManager extends DefaultClaimManager implements ClaimManager {

    private static ClaimInvalidationCache claimCache;
    private DataSource datasource;
    private static ClaimDAO claimDAO = null;
    private ClaimBuilder claimBuilder;
    private static Map<String, ClaimMapping> claimMapping = null;
    private Map<String, ClaimMapping> localClaims = null;
    private Map<String, ClaimMapping> additionalClaims = null;
    private static ClaimToClaimMapping claimToClaimMapping;
    private static IdentityMgtClaimManager instance;
    private static Log log = LogFactory.getLog(DatabaseUtil.class);


    public IdentityMgtClaimManager() throws UserStoreException {
    }

    public static IdentityMgtClaimManager getInstance() throws UserStoreException {
        if (instance == null) {
            instance = new IdentityMgtClaimManager();
        }
        return instance;
    }

    public IdentityMgtClaimManager(int tenantId) throws UserStoreException {

        RealmService realmService = ClaimManagementServiceDataHolder.getInstance().getRealmService();
        RealmConfiguration realmConfig;
        realmConfig = realmService.getBootstrapRealmConfiguration();
        DataSource dataSource = DatabaseUtil.getRealmDataSource(realmConfig);
        this.datasource = dataSource;
        this.claimDAO = new ClaimDAO(dataSource, tenantId);
        this.claimCache = ClaimInvalidationCache.getInstance();
        int count = claimDAO.getDialectCount();
        claimMapping = new HashMap<>();
        localClaims = new HashMap<>();
        additionalClaims = new HashMap<>();
        Map<String, ClaimMapping> claims = new HashMap<String, ClaimMapping>();

        if (count > 0) {
            try {
                List<ClaimMapping> lst = claimDAO.loadClaimMappings();
                for (ClaimMapping cm : lst) {
                    String uri = cm.getClaim().getClaimUri();
                    claimMapping.put(uri, cm);
                }
                if (claimMapping.size() > 0) {
                    doClaimCategorize();
                }
            } catch (UserStoreException e) {
                log.error("Error reading claims from database: Error - " + e.getMessage(), e);
                throw new UserStoreException("Database Error - " + e.getMessage(), e);
            }
        } else {
            this.claimBuilder = new ClaimBuilder(claimConfig.getPropertyHolder(), tenantId, dataSource);
            claimMapping = claimBuilder.getClaimMapping();
            if (claimMapping.size() > 0) {
                doClaimCategorize();
            }
            if (count <= 0 && localClaims.size() != 0) {
                if (claimDAO.addClaimMappings(localClaims.values().toArray(new ClaimMapping[localClaims.size()]))) {
                    claimDAO.addClaimMappings(additionalClaims.values().toArray(new ClaimMapping[additionalClaims
                            .size()]));
                }
            }
        }
    }

    /**
     * Retrieves the attribute name of the claim URI.
     *
     * @param claimURI The claim URI
     * @return
     * @throws UserStoreException
     */
    public String getAttributeName(String claimURI) throws UserStoreException {

        if (claimCache.isInvalid()) {
            this.claimMapping = getClaimMapFromDB();
        }
        ClaimMapping mapping = claimMapping.get(claimURI);
        if (mapping != null) {
            return mapping.getMappedAttribute();
        }
        return null;
    }

    /**
     * Categorize the local claims and additional claims
     */
    private void doClaimCategorize() {
        for (Map.Entry<String, ClaimMapping> entry : claimMapping.entrySet()) {
            if (entry.getValue().getClaim().getIsLocalClaim()) {
                localClaims.put(entry.getKey(), entry.getValue());
            } else {
                additionalClaims.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Get attribute name from domain name and claim uri.
     *
     * @param domainName domain name
     * @param claimURI   claim uri
     * @return attribute name specific to the domain
     * @throws UserStoreException
     */
    public String getAttributeName(String domainName, String claimURI) throws UserStoreException {

        if (claimCache.isInvalid()) {
            this.claimMapping = getClaimMapFromDB();
        }
        ClaimMapping mapping = claimMapping.get(claimURI);
        if (mapping != null) {
            if (domainName != null) {
                String mappedAttrib = mapping.getMappedAttribute(domainName.toUpperCase());
                if (mappedAttrib != null) {
                    return mappedAttrib;
                }
                return mapping.getMappedAttribute();
            } else {
                return mapping.getMappedAttribute();
            }
        }
        return null;
    }

    /**
     * The Claim object of the claim URI
     *
     * @param claimURI The claim URI
     * @return claim
     * @throws UserStoreException
     */
    public Claim getClaim(String claimURI) throws UserStoreException {

        if (claimCache.isInvalid()) {
            this.claimMapping = getClaimMapFromDB();
        }
        ClaimMapping mapping = claimMapping.get(claimURI);
        if (mapping != null) {
            return mapping.getClaim();
        }
        return null;
    }

    /**
     * Get claim mapping.
     *
     * @param claimURI claim uri
     * @return claim mapping
     * @throws UserStoreException
     */
    public ClaimMapping getClaimMapping(String claimURI) throws UserStoreException {

        if (claimCache.isInvalid()) {
            this.claimMapping = getClaimMapFromDB();
        }
        return claimMapping.get(claimURI);
    }

    /**
     * Give all the supported claim mappings by default in the system.
     *
     * @return supported claim mapping array.
     * @throws UserStoreException
     */
    public ClaimMapping[] getAllSupportClaimMappingsByDefault() throws UserStoreException {

        if (claimCache.isInvalid()) {
            this.claimMapping = getClaimMapFromDB();
        }
        List<ClaimMapping> claimList = new ArrayList<ClaimMapping>();
        Iterator<Map.Entry<String, ClaimMapping>> iterator = claimMapping.entrySet().iterator();

        for (; iterator.hasNext(); ) {
            ClaimMapping claimMapping = iterator.next().getValue();
            Claim claim = claimMapping.getClaim();
            if (claim.isSupportedByDefault()) {
                claimList.add(claimMapping);
            }
        }

        return claimList.toArray(new ClaimMapping[claimList.size()]);
    }

    /**
     * Give all the claim mappings from the database.
     *
     * @return an array of claim mappings
     * @throws UserStoreException
     */
    public ClaimMapping[] getAllClaimMappings() throws UserStoreException {

        if (claimCache.isInvalid()) {
            this.claimMapping = getClaimMapFromDB();
        }
        List<ClaimMapping> claimList = null;
        claimList = new ArrayList<ClaimMapping>();
        Iterator<Map.Entry<String, ClaimMapping>> iterator = claimMapping.entrySet().iterator();

        for (; iterator.hasNext(); ) {
            ClaimMapping claimMapping = iterator.next().getValue();
            claimList.add(claimMapping);
        }
        return claimList.toArray(new ClaimMapping[claimList.size()]);
    }

    /**
     * Get all claim mappings specific to a dialect.
     *
     * @param dialectUri
     * @return array of claim mappings
     * @throws UserStoreException
     */
    public ClaimMapping[] getAllClaimMappings(String dialectUri)
            throws UserStoreException {

        if (claimCache.isInvalid()) {
            this.claimMapping = getClaimMapFromDB();
        }
        List<ClaimMapping> claimList = null;
        claimList = new ArrayList<ClaimMapping>();
        Iterator<Map.Entry<String, ClaimMapping>> iterator = claimMapping.entrySet().iterator();

        for (; iterator.hasNext(); ) {
            ClaimMapping claimMapping = iterator.next().getValue();
            if (claimMapping.getClaim().getDialectURI().equals(dialectUri)) {
                claimList.add(claimMapping);
            }
        }
        return claimList.toArray(new ClaimMapping[claimList.size()]);
    }

    /**
     * Get all the claims with relations. This contains the additional claim and the
     * related mapped attribute(which is an local claim).
     *
     * @return an array of claimToClaimMappings
     * @throws UserStoreException
     */
    public ClaimToClaimMapping[] getAllClaimToClaimMappings() throws UserStoreException {
        if (claimCache.isInvalid()) {
            this.claimMapping = getClaimMapFromDB();
        }
        List<ClaimToClaimMapping> claimList = null;
        claimList = new ArrayList<ClaimToClaimMapping>();
        for (Map.Entry<String, ClaimMapping> localClaimEntry : localClaims.entrySet()) {
            for (Map.Entry<String, ClaimMapping> addtionalClaimEntry : localClaims.entrySet()) {
                if (localClaimEntry.getValue().equals(addtionalClaimEntry.getValue().getMappedAttribute())) {
                    claimToClaimMapping = new ClaimToClaimMapping(localClaimEntry.getValue().getClaim(),
                            addtionalClaimEntry.getValue().getClaim());
                    claimList.add(claimToClaimMapping);
                }
            }
        }
        return claimList.toArray(new ClaimToClaimMapping[claimList.size()]);
    }

    /**
     * Give all the mandatory claims.
     *
     * @return an array of required claim mappings.
     * @throws UserStoreException
     */
    public ClaimMapping[] getAllRequiredClaimMappings() throws UserStoreException {

        if (claimCache.isInvalid()) {
            this.claimMapping = getClaimMapFromDB();
        }
        List<ClaimMapping> claimList = null;
        claimList = new ArrayList<ClaimMapping>();
        Iterator<Map.Entry<String, ClaimMapping>> iterator = claimMapping.entrySet().iterator();

        for (; iterator.hasNext(); ) {
            ClaimMapping claimMapping = iterator.next().getValue();
            Claim claim = claimMapping.getClaim();
            if (claim.isRequired()) {
                claimList.add(claimMapping);
            }
        }

        return claimList.toArray(new ClaimMapping[claimList.size()]);
    }

    /**
     * Get all the claim uri from the database.
     *
     * @return an array of claim uris.
     * @throws UserStoreException
     */
    public String[] getAllClaimUris() throws UserStoreException {
        if (claimCache.isInvalid()) {
            this.claimMapping = getClaimMapFromDB();
        }
        return claimMapping.keySet().toArray(new String[claimMapping.size()]);
    }

    /**
     * Add new claim dialect.
     *
     * @param mappings new claim mapping, along with the new dialect information.
     * @throws Exception
     */
    public void addNewClaimDialect(ClaimDialect mappings) throws Exception {
        ClaimMapping[] mapping;
        mapping = mappings.getClaimMapping();
        for (ClaimMapping aMapping : mapping) {
            this.addNewClaimMapping(aMapping);
        }
    }

    /**
     * Adds a new claim mapping
     *
     * @param mapping The claim mapping to be added
     * @throws UserStoreException
     */
    public void addNewClaimMapping(ClaimMapping mapping) throws UserStoreException {

        if (mapping != null && mapping.getClaim() != null) {

            if (claimCache.isInvalid()) {
                this.claimMapping = getClaimMapFromDB();
            }
            if (!claimMapping.containsKey(mapping.getClaim().getClaimUri())) {
                claimMapping.put(mapping.getClaim().getClaimUri(), mapping);
                claimDAO.addClaimMapping(mapping);
                this.claimCache.invalidateCache();
            }
        }
    }

    /**
     * Deletes a dialect
     *
     * @param dialectUri uri of the dialect which need to be deleted
     * @throws Exception
     */
    public void removeClaimDialect(String dialectUri) throws Exception {
        ClaimMapping[] mapping;
        mapping = this.getAllClaimMappings(dialectUri);
        if (mapping != null) {
            for (ClaimMapping aMapping : mapping) {
                this.deleteClaimMapping(aMapping);
            }
        }

    }

    /**
     * Deletes a claim mapping
     *
     * @param mapping The claim mapping to be deleted
     * @throws UserStoreException
     */
    public void deleteClaimMapping(ClaimMapping mapping) throws UserStoreException {

        if (mapping != null && mapping.getClaim() != null) {

            if (claimCache.isInvalid()) {
                this.claimMapping = getClaimMapFromDB();
            }
            if (claimMapping.containsKey(mapping.getClaim().getClaimUri())) {
                claimMapping.remove(mapping.getClaim().getClaimUri());
                claimDAO.deleteClaimMapping(getClaimMapping(mapping));
                this.claimCache.invalidateCache();
            }
        }
    }

    /**
     * Updates a claim mapping
     *
     * @param mapping The claim mapping to be updated
     * @throws UserStoreException
     */
    public void updateClaimMapping(ClaimMapping mapping) throws UserStoreException {

        if (mapping != null && mapping.getClaim() != null) {

            if (claimCache.isInvalid()) {
                this.claimMapping = getClaimMapFromDB();
            }
            if (claimMapping.containsKey(mapping.getClaim().getClaimUri())) {
                claimMapping.put(mapping.getClaim().getClaimUri(), getClaimMapping(mapping));
                claimDAO.updateClaim(getClaimMapping(mapping));
                this.claimCache.invalidateCache();
            }
        }
    }

    /**
     * Gets the claim mapping.
     *
     * @param claimMapping The claim mapping
     * @return
     * @throws UserStoreException
     */
    private ClaimMapping getClaimMapping(ClaimMapping claimMapping) {
        ClaimMapping claimMap = null;
        if (claimMapping != null) {
            claimMap = new ClaimMapping(getClaim(claimMapping.getClaim()), claimMapping.getMappedAttribute());
            claimMap.setMappedAttributes(claimMapping.getMappedAttributes());
        } else {
            return new ClaimMapping();
        }
        return claimMap;
    }

    /**
     * The Claim object of the claim URI
     *
     * @param claim The claim
     * @return
     * @throws UserStoreException
     */
    private Claim getClaim(Claim claim) {

        Claim clm = new Claim();
        if (claim != null) {
            clm.setCheckedAttribute(claim.isCheckedAttribute());
            clm.setClaimUri(claim.getClaimUri());
            clm.setDescription(claim.getDescription());
            clm.setDialectURI(claim.getDialectURI());
            clm.setDisplayOrder(claim.getDisplayOrder());
            clm.setDisplayTag(claim.getDisplayTag());
            clm.setReadOnly(claim.isReadOnly());
            clm.setRegEx(claim.getRegEx());
            clm.setRequired(claim.isRequired());
            clm.setSupportedByDefault(claim.isSupportedByDefault());
            clm.setValue(claim.getValue());
        }
        return clm;
    }

    /**
     * Get all the claims from database.
     *
     * @return claim map
     * @throws UserStoreException
     */
    private Map<String, ClaimMapping> getClaimMapFromDB() throws UserStoreException {
        Map<String, ClaimMapping> claimMap = new ConcurrentHashMap<String, ClaimMapping>();
        try {
            Map<String, ClaimMapping> dbClaimMap = this.claimBuilder.buildClaimMappingsFromDatabase(this.datasource,
                    null);
            claimMap.putAll(dbClaimMap);
        } catch (Exception e) {
            throw new UserStoreException(e.getMessage(), e);
        }
        return claimMap;
    }
}
