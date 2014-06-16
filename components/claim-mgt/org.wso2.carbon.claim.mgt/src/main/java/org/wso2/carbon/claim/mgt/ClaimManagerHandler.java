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
package org.wso2.carbon.claim.mgt;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonException;
import org.wso2.carbon.claim.mgt.internal.ClaimManagementServiceComponent;
import org.wso2.carbon.core.util.AdminServicesUtil;
import org.wso2.carbon.core.util.AnonymousSessionUtil;
import org.wso2.carbon.user.api.Claim;
import org.wso2.carbon.user.api.ClaimMapping;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreConfigConstants;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.claim.ClaimManager;

import java.util.*;

public class ClaimManagerHandler {

    private static Log log = LogFactory.getLog(ClaimManagerHandler.class);

    // Maintains a single instance of UserStore.
    private static ClaimManagerHandler claimManagerHandler;

    // To enable attempted thread-safety using double-check locking
    private static Object lock = new Object();

    // Making the class singleton
    private ClaimManagerHandler() throws Exception {
    }

    public static ClaimManagerHandler getInstance() throws Exception {

        // Enables attempted thread-safety using double-check locking
        if (claimManagerHandler == null) {
            synchronized (lock) {
                if (claimManagerHandler == null) {
                    claimManagerHandler = new ClaimManagerHandler();
                    if (log.isDebugEnabled()) {
                        log.debug("ClaimManagerHandler singleton instance created successfully");
                    }
                }
            }
        }
        return claimManagerHandler;
    }

    /**
     * Returns all supported claims.
     * 
     * @return
     * @throws Exception
     */
    public Claim[] getAllSupportedClaims() throws Exception {
        ClaimManager claimManager = null;

        try {
            UserRealm realm = getRealm();
            claimManager = realm.getClaimManager();
            if (claimManager != null) {
                // There can be cases - we get a request for an external user
                // store - where we don'
                // have a claims administrator.
                ClaimMapping[] mappings = realm.getClaimManager()
                        .getAllSupportClaimMappingsByDefault();
                Claim[] claims = new Claim[mappings.length];
                for (int i = 0; i < mappings.length; i++) {
                    claims[1] = mappings[i].getClaim();
                }
                return claims;
            }
        } catch (UserStoreException e) {
            log.error("Error occurred while loading supported claims", e);
            getException("Error occurred while loading supported claima", e);
        }

        return new Claim[0];
    }

    /**
     * 
     * @return
     * @throws Exception
     */
    public ClaimMapping[] getAllSupportedClaimMappings() throws Exception {
        ClaimMapping[] claimMappings = new ClaimMapping[0];
        ClaimManager claimManager = null;

        try {
            UserRealm realm = getRealm();
            claimManager = realm.getClaimManager();
            if (claimManager == null) {
                // There can be cases - we get a request for an external user store - where we don'
                // have a claims administrator.
                return new ClaimMapping[0];
            }

            return claimManager.getAllSupportClaimMappingsByDefault();

        } catch (UserStoreException e) {
            log.error("Error occurred while loading supported claims", e);
            getException("Error occurred while loading supported claima", e);
        }

        return claimMappings;
    }

    /**
     * 
     * @return
     * @throws Exception
     */
    public ClaimMapping[] getAllClaimMappings() throws Exception {
        ClaimMapping[] claimMappings = new ClaimMapping[0];
        ClaimManager claimManager = null;

        try {
            UserRealm realm = getRealm();
            claimManager = realm.getClaimManager();
            if (claimManager == null) {
                // There can be cases - we get a request for an external user store - where we don'
                // have a claims administrator.
                return new ClaimMapping[0];
            }

            claimMappings = claimManager.getAllClaimMappings();

        } catch (UserStoreException e) {
            log.error("Error occurred while loading supported claims", e);
            getException("Error occurred while loading supported claima", e);
        }

        return claimMappings;
    }

    public ClaimMapping[] getAllClaimMappings(String tenantDomain) throws Exception {
        ClaimMapping[] claimMappings = new ClaimMapping[0];
        ClaimManager claimManager = null;

        try {
            UserRealm realm = AnonymousSessionUtil.getRealmByTenantDomain(
                    ClaimManagementServiceComponent.getRegistryService(),
                    ClaimManagementServiceComponent.getRealmService(), tenantDomain);
            claimManager = realm.getClaimManager();
            if (claimManager == null) {
                // There can be cases - we get a request for an external user store - where we don'
                // have a claims administrator.
                return new ClaimMapping[0];
            }

            claimMappings = claimManager.getAllClaimMappings();

        } catch (UserStoreException e) {
            String message = "Error occurred while loading claims mapping for tenant "
                    + tenantDomain;
            log.error(e.getMessage(), e);
            throw new Exception(message);
        }

        return claimMappings;
    }

    public ClaimMapping[] getAllClaimMappings(String dialectURI, String tenantDomain)
            throws Exception {

        ClaimMapping[] claimMappings = null;
        ClaimManager claimManager = null;
        try {
            UserRealm realm = AnonymousSessionUtil.getRealmByTenantDomain(
                    ClaimManagementServiceComponent.getRegistryService(),
                    ClaimManagementServiceComponent.getRealmService(), tenantDomain);
            claimManager = realm.getClaimManager();
            if (claimManager == null) {
                // There can be cases - we get a request for an external user store - where we don'
                // have a claims administrator.
                return new ClaimMapping[0];
            }

            claimMappings = claimManager.getAllClaimMappings(dialectURI);

        } catch (UserStoreException e) {
            String message = "Error occurred while loading all claim mappings for tenant "
                    + tenantDomain;
            log.error(e.getMessage(), e);
            throw new Exception(message);
        }

        return claimMappings;
    }

    public ClaimMapping getClaimMapping(String claimURI) throws Exception {
        ClaimMapping claimMapping = null;
        ClaimManager claimManager = null;
        try {
            UserRealm realm = getRealm();
            claimManager = realm.getClaimManager();
            if (claimManager != null) {
                claimMapping = (ClaimMapping) claimManager.getClaimMapping(claimURI);

            }
        } catch (UserStoreException e) {
            log.error("Error occurred while loading supported claims", e);
            getException("Error occurred while retrieving claim", e);
        }
        return claimMapping;
    }

    /**
     * 
     * @return
     * @throws Exception
     */
    public ClaimMapping[] getAllSupportedClaimMappings(String dialectUri) throws Exception {
        ClaimMapping[] claimMappings = new ClaimMapping[0];
        ClaimManager claimManager = null;
        try {
            UserRealm realm = getRealm();
            claimManager = realm.getClaimManager();
            if (claimManager == null) {
                // There can be cases - we get a request for an external user store - where we don'
                // have a claims administrator.
                return new ClaimMapping[0];
            }
            // return claimManager.getAllSupportClaimMappingsByDefault();
            return claimManager.getAllClaimMappings(dialectUri);

        } catch (UserStoreException e) {
            log.error("Error occurred while loading supported claims", e);
            getException("Error occurred while loading supported claima", e);
        }

        return claimMappings;
    }

    /**
     * Returns all supported claims for the given dialect.
     * 
     * @return
     * @throws Exception
     */
    public Claim[] getAllSupportedClaims(String dialectUri) throws Exception {
        Claim[] claims = new Claim[0];
        ArrayList<Claim> reqClaims = null;
        ClaimManager claimManager = null;
        ClaimMapping[] mappings = null;

        try {
            UserRealm realm = getRealm();
            claimManager = realm.getClaimManager();
            if (claimManager == null) {
                // There can be cases - we get a request for an external user store - where we don'
                // have a claims administrator.
                return claims;
            }
            mappings = claimManager.getAllSupportClaimMappingsByDefault();
            reqClaims = new ArrayList<Claim>();
            for (int i = 0; i < mappings.length; i++) {
                Claim claim = mappings[i].getClaim();
                if (dialectUri.equals(claim.getDialectURI())) {
                    reqClaims.add(claim);
                }
            }

            return reqClaims.toArray(new Claim[reqClaims.size()]);
        } catch (UserStoreException e) {
            log.error("Error occurred while loading supported claims from the dialect "
                    + dialectUri, e);
            getException("Error occurred while loading supported claims from the dialect "
                    + dialectUri, e);
        }

        return claims;
    }

    /**
     * @param mapping
     * @throws Exception
     */
    public void upateClaimMapping(ClaimMapping mapping) throws Exception {
        ClaimManager claimManager = null;
        try {
            UserRealm realm = getRealm();

            String primaryDomainName = realm.getRealmConfiguration().getUserStoreProperty(
                    UserCoreConstants.RealmConfig.PROPERTY_DOMAIN_NAME);

            if (primaryDomainName == null) {
                if (mapping.getMappedAttribute() == null) {
                    throw new Exception("Attribute name cannot be null for the primary domain");
                }
            } else if (mapping.getMappedAttribute() == null) {
                String attr = mapping.getMappedAttribute(primaryDomainName);
                if (attr == null) {
                    throw new Exception("Attribute name cannot be null for the primary domain");
                }
                mapping.setMappedAttribute(attr);
            }

            claimManager = realm.getClaimManager();
            if (claimManager != null) {
                // There can be cases - we get a request for an external user store - where we don'
                // have a claims administrator.
                claimManager.updateClaimMapping(mapping);
            }
        } catch (UserStoreException e) {
            log.error("Error occurred while updating claim mapping", e);
            getException("Error occurred while updating claim mapping", e);
        }
    }

    /**
     * 
     * @param mapping
     * @throws Exception
     */
    public void addNewClaimMapping(ClaimMapping mapping) throws Exception {
        ClaimManager claimManager = null;
        try {
            UserRealm realm = getRealm();
            claimManager = realm.getClaimManager();
            if (claimManager != null) {
                // There can be cases - we get a request for an external user store - where we don'
                // have a claims administrator.
                claimManager.addNewClaimMapping(mapping);
            }
        } catch (UserStoreException e) {
            log.error("Error occurred while adding new claim mapping", e);
            getException("Error occurred while adding new claim mapping", e);
        }
    }

    /**
     * 
     * @param dialectUri
     * @param claimUri
     * @throws Exception
     */
    public void removeClaimMapping(String dialectUri, String claimUri) throws Exception {
        ClaimMapping mapping = null;
        Claim claim = null;
        ClaimManager claimManager = null;
        try {
            UserRealm realm = getRealm();
            claimManager = realm.getClaimManager();
            if (claimManager != null) {
                // There can be cases - we get a request for an external user store - where we don'
                // have a claims administrator.
                claim = new Claim();
                claim.setClaimUri(claimUri);
                claim.setDialectURI(dialectUri);
                mapping = new ClaimMapping(claim, null);
                claimManager.deleteClaimMapping(mapping);
            }
        } catch (UserStoreException e) {
            log.error("Error occurred while removing new claim mapping", e);
            getException("Error occurred while removing new claim mapping", e);
        }
    }

    /**
     * 
     * @param mappings
     */
    public void addNewClaimDialect(ClaimDialect mappings) throws Exception {
        ClaimMapping[] mapping = null;
        ClaimManager claimManager = null;
        try {
            UserRealm realm = getRealm();
            claimManager = realm.getClaimManager();
            if (claimManager != null) {
                mapping = mappings.getClaimMapping();
                for (int i = 0; i < mapping.length; i++) {
                    claimManager.addNewClaimMapping(mapping[i]);
                }
            }
        } catch (UserStoreException e) {
            log.error("Error occurred while removing new claim mapping", e);
            getException("Error occurred while removing new claim mapping", e);
        }
    }

    /**
     * 
     * @param dialectUri
     * @throws Exception
     */
    public void removeClaimDialect(String dialectUri) throws Exception {
        ClaimMapping[] mapping = null;
        ClaimManager claimManager = null;
        try {
            UserRealm realm = getRealm();
            claimManager = realm.getClaimManager();
            if (claimManager != null) {
                mapping = claimManager.getAllClaimMappings(dialectUri);
                if (mapping != null) {
                    for (int i = 0; i < mapping.length; i++) {
                        claimManager.deleteClaimMapping(mapping[i]);
                    }
                }
            }
        } catch (UserStoreException e) {
            log.error("Error occurred while removing new claim dialect", e);
            getException("Error occurred while removing new claim dialect", e);
        }
    }

    private UserRealm getRealm() throws Exception {
        try {
            return AdminServicesUtil.getUserRealm();
        } catch (CarbonException e) {
            // already logged
            throw new Exception(e.getMessage(), e);
        }
    }

    /**
     * Creates an IdentityException instance wrapping the given error message and
     * 
     * @param message Error message
     * @param e Exception
     * @throws Exception
     */
    private void getException(String message, Exception e) throws Exception {
        log.error(message, e);
        throw new Exception(message, e);
    }

    public Set<org.wso2.carbon.claim.mgt.ClaimMapping> getMappingsFromCarbonDialectToOther(
            String otherDialectURI, Set<String> carbonClaimURIs, String tenantDomain)
            throws Exception {

        Set<org.wso2.carbon.claim.mgt.ClaimMapping> returnSet = new HashSet<org.wso2.carbon.claim.mgt.ClaimMapping>();

        if (otherDialectURI.equals("http://wso2.org/claims")) {
            for (String claimURI : carbonClaimURIs) {
                org.wso2.carbon.claim.mgt.ClaimMapping claimMapping = new org.wso2.carbon.claim.mgt.ClaimMapping(
                        otherDialectURI, claimURI, claimURI);

                returnSet.add(claimMapping);
            }
            return returnSet;
        }

        ClaimMapping[] claimMappingsInOtherDialect = getAllClaimMappings(otherDialectURI,
                tenantDomain);
        ClaimMapping[] allClaimMappingsInCarbonDialect = getAllClaimMappings(
                "https://wso2.org/claims", tenantDomain);
        if (otherDialectURI == null) {
            String message = "Invalid argument: \'otherDialectURI\' is \'NULL\'";
            log.error(message);
            throw new Exception(message);
        }
        if (carbonClaimURIs == null || carbonClaimURIs.size() < 1) {
            String message = "Invalid argument: \'carbonClaimURIs\' is \'NULL\' or of zero length";
            log.error(message);
            throw new Exception(message);
        }
        for (String requestedClaimURI : carbonClaimURIs) {
            if (allClaimMappingsInCarbonDialect != null
                    && allClaimMappingsInCarbonDialect.length > 0) {
                for (ClaimMapping claimMapping : allClaimMappingsInCarbonDialect) {
                    if (requestedClaimURI.equals(claimMapping.getClaim().getClaimUri())) {
                        String mappedAttr = claimMapping.getMappedAttribute();
                        for (ClaimMapping carbonClaimMapping : claimMappingsInOtherDialect) {
                            if (mappedAttr.equals(carbonClaimMapping.getMappedAttribute())) {
                                returnSet.add(new org.wso2.carbon.claim.mgt.ClaimMapping(
                                        otherDialectURI, requestedClaimURI, carbonClaimMapping
                                                .getClaim().getClaimUri()));
                            }
                        }
                    }
                }
            }
        }
        return returnSet;
    }

    public Map<String, String> getMappingsMapFromCarbonDialectToOther(String otherDialectURI,
            Set<String> carbonClaimURIs, String tenantDomain) throws Exception {

        Map<String, String> returnMap = new HashMap<String, String>();
        Set<org.wso2.carbon.claim.mgt.ClaimMapping> mappings = getMappingsFromCarbonDialectToOther(
                otherDialectURI, carbonClaimURIs, tenantDomain);
        for (org.wso2.carbon.claim.mgt.ClaimMapping mapping : mappings) {
            returnMap.put(mapping.getCarbonClaimURI(), mapping.getNonCarbonClaimURI());
        }
        return returnMap;
    }

    /**
     * 
     * @param otherDialectURI
     * @param otherClaimURIs
     * @param tenantDomain
     * @return
     * @throws Exception
     */
    public Set<org.wso2.carbon.claim.mgt.ClaimMapping> getMappingsFromOtherDialectToCarbon(
            String otherDialectURI, Set<String> otherClaimURIs, String tenantDomain)
            throws Exception {

        Set<org.wso2.carbon.claim.mgt.ClaimMapping> returnSet = new HashSet<org.wso2.carbon.claim.mgt.ClaimMapping>();

        if (otherDialectURI == null) {
            String message = "Invalid argument: \'otherDialectURI\' is \'NULL\'";
            log.error(message);
            throw new Exception(message);
        }

        if (otherDialectURI.equals("http://wso2.org/claims") && otherClaimURIs != null) {
            for (String claimURI : otherClaimURIs) {
                org.wso2.carbon.claim.mgt.ClaimMapping claimMapping = new org.wso2.carbon.claim.mgt.ClaimMapping(
                        otherDialectURI, claimURI, claimURI);

                returnSet.add(claimMapping);
            }
            return returnSet;
        }

        ClaimMapping[] allClaimMappingsInOtherDialect = getAllClaimMappings(otherDialectURI,
                tenantDomain);
        ClaimMapping[] allClaimMappingsInCarbonDialect = getAllClaimMappings(
                "http://wso2.org/claims", tenantDomain);

        if (otherClaimURIs == null || otherClaimURIs.size() == 0) {

            for (ClaimMapping claimMapping : allClaimMappingsInOtherDialect) {

                String mappedAttr = claimMapping.getMappedAttribute();

                for (ClaimMapping carbonClaimMapping : allClaimMappingsInCarbonDialect) {
                    if (mappedAttr.equals(carbonClaimMapping.getMappedAttribute())) {
                        returnSet.add(new org.wso2.carbon.claim.mgt.ClaimMapping(otherDialectURI,
                                claimMapping.getClaim().getClaimUri(), carbonClaimMapping
                                        .getClaim().getClaimUri()));
                        break;
                    }
                }

            }
        } else {

            for (String requestedClaimURI : otherClaimURIs) {
                if (allClaimMappingsInOtherDialect != null
                        && allClaimMappingsInOtherDialect.length > 0) {
                    for (ClaimMapping claimMapping : allClaimMappingsInOtherDialect) {
                        if (requestedClaimURI.equals(claimMapping.getClaim().getClaimUri())) {
                            String mappedAttr = claimMapping.getMappedAttribute();
                            for (ClaimMapping carbonClaimMapping : allClaimMappingsInCarbonDialect) {
                                if (mappedAttr.equals(carbonClaimMapping.getMappedAttribute())) {
                                    returnSet.add(new org.wso2.carbon.claim.mgt.ClaimMapping(
                                            otherDialectURI, requestedClaimURI, carbonClaimMapping
                                                    .getClaim().getClaimUri()));
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        return returnSet;
    }

    /**
     * 
     * @param otherDialectURI
     * @param otherClaimURIs
     * @param tenantDomain
     * @return
     * @throws Exception
     */
    public Map<String, String> getMappingsMapFromOtherDialectToCarbon(String otherDialectURI,
            Set<String> otherClaimURIs, String tenantDomain) throws Exception {

        return getMappingsMapFromOtherDialectToCarbon(otherDialectURI, otherClaimURIs,
                tenantDomain, false);
    }

    /**
     * 
     * @param otherDialectURI
     * @param otherClaimURIs
     * @param tenantDomain
     * @param useCarbonDialectAsKey
     * @return
     * @throws Exception
     */
    public Map<String, String> getMappingsMapFromOtherDialectToCarbon(String otherDialectURI,
            Set<String> otherClaimURIs, String tenantDomain, boolean useCarbonDialectAsKey)
            throws Exception {

        Map<String, String> returnMap = new HashMap<String, String>();
        Set<org.wso2.carbon.claim.mgt.ClaimMapping> mappings = getMappingsFromOtherDialectToCarbon(
                otherDialectURI, otherClaimURIs, tenantDomain);
        for (org.wso2.carbon.claim.mgt.ClaimMapping mapping : mappings) {
            if (useCarbonDialectAsKey) {
                returnMap.put(mapping.getCarbonClaimURI(), mapping.getNonCarbonClaimURI());
            } else {
                returnMap.put(mapping.getNonCarbonClaimURI(), mapping.getCarbonClaimURI());
            }
        }
        return returnMap;
    }

    /**
     * 
     * @param tenantDomain
     * @return
     * @throws Exception
     */
    public Set<String> getAllClaimDialects(String tenantDomain) throws Exception {

        Set<String> dialects = new HashSet<String>();

        List<ClaimMapping> claimMappings = new ArrayList<ClaimMapping>(
                Arrays.asList(getAllClaimMappings(tenantDomain)));

        if (claimMappings == null || claimMappings.size() == 0) {
            return new HashSet<String>();
        }

        for (int i = 0; i < claimMappings.size(); i++) {
            String dialectUri = claimMappings.get(i).getClaim().getDialectURI();
            dialects.add(dialectUri);
        }
        return dialects;
    }

    public boolean isKnownClaimDialect(String dialectURI, String tenantDomain) throws Exception {
        if (dialectURI == null || dialectURI.equals("")) {
            String message = "Invalid argument : " + "\'dialectURI\' is \'NULL\' or empty";
            log.debug(message);
            throw new IllegalArgumentException(message);
        }
        if (getAllClaimDialects(tenantDomain).contains(dialectURI)) {
            return true;
        }
        return false;
    }

}
