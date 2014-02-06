/*
*  Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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


package org.wso2.carbon.ldap.server.tenant;

import org.apache.log4j.Logger;
import org.wso2.carbon.apacheds.AdminInfo;
import org.wso2.carbon.apacheds.PartitionInfo;
import org.wso2.carbon.apacheds.PartitionManager;
import org.wso2.carbon.ldap.server.exception.DirectoryServerException;
import org.wso2.carbon.ldap.server.util.EmbeddingLDAPException;
import org.wso2.carbon.ldap.server.configuration.LDAPConfigurationBuilder;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.tenant.LDAPTenantManager;
import org.wso2.carbon.user.core.tenant.Tenant;


public class LDAPTenantManagerService implements LDAPTenantManager {

    private final Logger logger = Logger.getLogger(LDAPTenantManagerService.class);
    private PartitionManager ldapPartitionManager = null;
    private LDAPConfigurationBuilder ldapConfigurationBuilder = null;

    public LDAPTenantManagerService(PartitionManager partitionManager) {
        this.ldapPartitionManager = partitionManager;

    }

    /**
     * This constructor is used to pass both partition manager and tenant manager references at the
     * creation of LDAPTenantManagerService.
     *
     * @param partitionManager
     * @param ldapConfiguration
     */
    public LDAPTenantManagerService(PartitionManager partitionManager, LDAPConfigurationBuilder
            ldapConfiguration) {
        this.ldapPartitionManager = partitionManager;
        this.ldapConfigurationBuilder = ldapConfiguration;

    }

    /**
     * Add a new partition for a tenant when a new tenant is created.
     *
     * @param tenant object
     * @return tenant id
     * @throws UserStoreException
     */
    public int addTenant(Tenant tenant) throws UserStoreException {

        try {

            this.ldapPartitionManager.addPartition(getPartitionInfo(tenant));

        } catch (DirectoryServerException e) {
            //exception can be caught if addPartition method fails.
            String errorMessage = "Could not create a new partition for tenant id - " +
                                  tenant.getId() + "and for tenant domain - " + tenant.getDomain();
            logger.error(errorMessage, e);
            throw new UserStoreException(errorMessage, e);
        } catch (EmbeddingLDAPException e) {
            //exception can be caught if getPartitionInfo method fails.
            String errorMessage = "Could not create a new partition for tenant id - " +
                                  tenant.getId() + "and for tenant domain - " + tenant.getDomain();
            logger.error(errorMessage, e);
            throw new UserStoreException(errorMessage, e);

        }

        return tenant.getId();
    }

    public void updateTenant(Tenant tenant) throws UserStoreException {

        /*try {
            this.ldapPartitionManager.updatePartition(String.valueOf(tenant.getId()), tenant.getAdminName(),
                                                      getTenantSuffix(tenant.getDomain()), getAdminInfo(tenant));

        } catch (DirectoryServerException e) {
            throw new UserStoreException(
                "Can not update the LDAP partition for tenant id  - " + tenant.getId() + " tenant domain " +
                    tenant.getDomain(), e);
        }*/
    }

    public void deleteTenant(int i) throws UserStoreException {

        try {
            this.ldapPartitionManager.removePartition(String.valueOf(i));
        } catch (Exception e) {
            throw new UserStoreException("Could not remove partition for tenant id " + i, e);
        }
    }

    /**
     * When initializing HybridLDAPTenantManager, the existing partitions for tenants are
     * initialized. This method adds existing partition to each tenant.
     *
     * @param tenant object
     * @throws UserStoreException
     */
    public void addPartitionToTenant(Tenant tenant) throws UserStoreException {
        try {
            ldapPartitionManager.initializeExistingPartition(getPartitionInfo(tenant));

        } catch (Exception e) {
            throw new UserStoreException("Can not add the new partition ", e);
        }
    }

    /**
     * This returns rootDN from the domain. if domain=wso2.com, returns dc=wso2,dc=com
     *
     * @param domain name of the tenant
     * @return rootDN for the tenant
     */
    private String getTenantSuffix(String domain) {
        // here we use a simple algorithm by splitting the domain with .
        String[] domainParts = domain.split("\\.");
        StringBuffer suffixName = new StringBuffer();
        for (String domainPart : domainParts) {
            suffixName.append(",dc=").append(domainPart);
        }

        return suffixName.toString().replaceFirst(",", "");
    }

    /**
     * This constructs AdminInfo which is needed to create partition; from the tenant info
     *
     * @param tenant object
     * @return Admin Info created out of tenant info
     */
    private AdminInfo getAdminInfo(Tenant tenant) throws EmbeddingLDAPException {

        AdminInfo tenantAdminInfo = new AdminInfo();
        /*Set the object class to be used in admin entry by reading it from configuration.*/
        try {
            tenantAdminInfo.addObjectClass(
                    ldapConfigurationBuilder.getLdapConfiguration().getAdminEntryObjectClass());
        } catch (EmbeddingLDAPException e) {
            String errorMessage = "Error in obtaining LDAP Configuration.";
            logger.error(errorMessage, e);
            throw new EmbeddingLDAPException(errorMessage, e);
        }
        /*Following details of tenant admin is captured from the user through ui.*/
        //set admin's user name
        if (tenant.getAdminName() != null) {
            tenantAdminInfo.setAdminUserName(tenant.getAdminName());
        }

        if (tenant.getAdminFirstName() != null) {
            tenantAdminInfo.setAdminCommonName(tenant.getAdminFirstName());
        }
        if (tenant.getAdminLastName() != null) {
            tenantAdminInfo.setAdminLastName(tenant.getAdminLastName());
        }

        if (tenant.getEmail() != null) {
            tenantAdminInfo.setAdminEmail(tenant.getEmail());
        }

        if (tenant.getAdminPassword() != null) {
            tenantAdminInfo.setAdminPassword(tenant.getAdminPassword());
        }
        return tenantAdminInfo;
    }

    /**
     * This constructs PartitionInfo which is needed to create partition; from the tenant info
     *
     * @param tenant object
     * @return partition info created out of tenant info
     */
    private PartitionInfo getPartitionInfo(Tenant tenant) throws EmbeddingLDAPException {

        String partitionID = String.valueOf(tenant.getId());
        //String partitionID = tenant.getDomain();
        String realm = tenant.getDomain();
        String rootDN = getTenantSuffix(tenant.getDomain());
        AdminInfo tenantAdminInfo = getAdminInfo(tenant);

        return new PartitionInfo(partitionID, realm, rootDN, tenantAdminInfo);
    }

}

