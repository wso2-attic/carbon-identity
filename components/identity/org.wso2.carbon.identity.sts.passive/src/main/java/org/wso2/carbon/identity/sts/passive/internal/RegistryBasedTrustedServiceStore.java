/*
* Copyright (c) 2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.wso2.carbon.identity.sts.passive.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.sts.passive.ClaimDTO;
import org.wso2.carbon.registry.api.Resource;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.exceptions.RegistryException;

import java.util.ArrayList;
import java.util.List;

public class RegistryBasedTrustedServiceStore {
    private static final Log log = LogFactory.getLog(RegistryBasedTrustedServiceStore.class);
    public static final String CLAIM_DIALECT = "claimDialect";
    public static final String CLAIMS = "claims";
    public static final String REALM_NAME = "realmName";
    private String registryTrustedServicePath = "repository/identity/passiveSTSTrustedRP/";
    private static final String SLASH_REPLACE_CHARACTER = "BACK_SLASH";


    /**
     * Add a trusted service to which tokens are issued with given claims.
     *
     * @param realmName    - this uniquely represents the trusted service
     * @param claimDialect - claim dialects uris
     * @param claims       - these comma separated default claims are issued when a request is done from the given realm
     * @throws Exception - if fails to add trusted service
     */
    public void addTrustedService(String realmName, String claimDialect, String claims)
            throws Exception {
        realmName = replaceSlashWithConstantString(realmName);
        try {
            Registry registry = IdentityPassiveSTSServiceComponent.getConfigSystemRegistry();
            String trustedServicePath = registryTrustedServicePath + realmName;
            // if registry collection does not exists, create
            if (!registry.resourceExists(trustedServicePath)) {
                Resource resource = registry.newResource();
                resource.addProperty(REALM_NAME, realmName);
                resource.addProperty(CLAIMS, claims);
                resource.addProperty(CLAIM_DIALECT, claimDialect);
                registry.put(trustedServicePath, resource);
            } else {
                throw new Exception(realmName + " already added. Please remove first and add again.");
            }
        } catch (RegistryException e) {
            String error = "Error occurred when adding a trusted service due to error in accessing registry.";
            throw new Exception(error, e);
        }
    }

    /**
     * Remove the given trusted service with realmName
     *
     * @param realmName - the realm of the service
     * @throws Exception
     */
    public void removeTrustedService(String realmName) throws Exception {
        realmName = replaceSlashWithConstantString(realmName);
        try {
            Registry registry = IdentityPassiveSTSServiceComponent.getConfigSystemRegistry();
            String trustedServicePath = registryTrustedServicePath + realmName;
            if (registry.resourceExists(trustedServicePath)) {
                registry.delete(trustedServicePath);
            } else {
                throw new Exception(realmName + " ,No such trusted service exists to delete.");
            }

        } catch (RegistryException e) {
            String error = "Error occurred when removing a trusted service due to error in accessing registry.";
            throw new Exception(error, e);
        }
    }

    /**
     * Get all trusted services
     *
     * @return
     * @throws Exception
     */
    public ClaimDTO[] getAllTrustedServices() throws Exception {
        try {
            Registry registry = IdentityPassiveSTSServiceComponent.getConfigSystemRegistry();
            List<ClaimDTO> trustedServices = new ArrayList<ClaimDTO>();

            if (!registry.resourceExists(registryTrustedServicePath)) {
                return new ClaimDTO[0];
            }
            Collection trustedServiceCollection = (Collection) registry.get(registryTrustedServicePath);
            for (String resourcePath : trustedServiceCollection.getChildren()) {
                Resource resource = registry.get(resourcePath);
                ClaimDTO claimDTO = new ClaimDTO();
                claimDTO.setRealm(resource.getProperty(REALM_NAME).replace(SLASH_REPLACE_CHARACTER, "/"));

                String claims = resource.getProperty(CLAIMS);
                if (claims.startsWith("[")) {
                    claims = claims.replaceFirst("\\[", "");
                }
                if (claims.endsWith("]")) {
                    claims = claims.substring(0, claims.length() - 2);
                }
                claimDTO.setDefaultClaims(claims.split(","));

                claimDTO.setClaimDialect(resource.getProperty(CLAIM_DIALECT));

                trustedServices.add(claimDTO);
            }

            return trustedServices.toArray(new ClaimDTO[trustedServices.size()]);
        } catch (RegistryException e) {
            String error = "Error occurred when getting all trusted services due to error in accessing registry.";
            throw new Exception(error, e);
        }
    }

    /**
     * Get default claims for given trusted service
     *
     * @param realmName - trusted service realm name
     * @return - default claims for given trusted service
     * @throws Exception
     */
    public ClaimDTO getTrustedServiceClaims(String realmName) throws Exception {
        realmName = replaceSlashWithConstantString(realmName);
        try {
            Registry registry = IdentityPassiveSTSServiceComponent.getConfigSystemRegistry();
            String trustedServicePath = registryTrustedServicePath + realmName;

            if (!registry.resourceExists(trustedServicePath)) {
                log.info("No trusted service found with name:" + realmName);
                return new ClaimDTO();
            }
            Resource resource = registry.get(trustedServicePath);
            ClaimDTO claimDTO = new ClaimDTO();
            claimDTO.setRealm(realmName.replace(SLASH_REPLACE_CHARACTER, "/"));
            String claims = resource.getProperty(CLAIMS);

            if (claims.startsWith("[")) {
                claims = claims.replaceFirst("\\[", "");
            }
            if (claims.endsWith("]")) {
                // replace ] and , too. handle better way. ugly code
                claims = claims.substring(0, claims.length() - 3);
            }

            claimDTO.setDefaultClaims(claims.split(","));

            claimDTO.setClaimDialect(resource.getProperty(CLAIM_DIALECT));
            return claimDTO;
        } catch (RegistryException e) {
            String error = "Error occurred when getting a trusted service due to error in accessing registry.";
            throw new Exception(error, e);
        }
    }

    private String replaceSlashWithConstantString(String realmName) throws Exception {
        if (realmName == null || "".equals(realmName)) {
            throw new Exception("realm name can not be empty or null");
        }
        realmName = realmName.trim();
        if (realmName.endsWith("/")) {
            realmName = realmName.substring(0, realmName.length() - 1);
        }
        realmName = realmName.replace("/", SLASH_REPLACE_CHARACTER);
        return realmName;
    }

}