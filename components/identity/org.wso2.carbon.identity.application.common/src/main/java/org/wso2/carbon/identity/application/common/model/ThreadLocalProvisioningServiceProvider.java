package org.wso2.carbon.identity.application.common.model;

import java.io.Serializable;

public class ThreadLocalProvisioningServiceProvider implements Serializable{

    /**
     * 
     */
    private static final long serialVersionUID = -486701265390312767L;
    
    private String serviceProviderName;
    private String claimDialect;
    private boolean justInTimeProvisioning;
    private ProvisioningServiceProviderType serviceProviderType;
    private String tenantDomain;
    //isBulkUserAdd is true indicates bulk user add
    private boolean isBulkUserAdd;

    public boolean isBulkUserAdd() {
        return isBulkUserAdd;
    }

    public void setBulkUserAdd(boolean isBulkUserAdd) {
        this.isBulkUserAdd = isBulkUserAdd;
    }
    /**
     * 
     * @return
     */
    public String getServiceProviderName() {
        return serviceProviderName;
    }

    /**
     * 
     * @param serviceProviderName
     */
    public void setServiceProviderName(String serviceProviderName) {
        this.serviceProviderName = serviceProviderName;
    }

    /**
     * 
     * @return
     */
    public String getClaimDialect() {
        return claimDialect;
    }

    /**
     * 
     * @param claimDialect
     */
    public void setClaimDialect(String claimDialect) {
        this.claimDialect = claimDialect;
    }

    /**
     * 
     * @return
     */
    public boolean isJustInTimeProvisioning() {
        return justInTimeProvisioning;
    }

    /**
     * 
     * @param justInTimeProvisioning
     */
    public void setJustInTimeProvisioning(boolean justInTimeProvisioning) {
        this.justInTimeProvisioning = justInTimeProvisioning;
    }

    /**
     * 
     * @return
     */
    public ProvisioningServiceProviderType getServiceProviderType() {
        return serviceProviderType;
    }

    /**
     * 
     * @param serviceProviderType
     */
    public void setServiceProviderType(ProvisioningServiceProviderType serviceProviderType) {
        this.serviceProviderType = serviceProviderType;
    }

    /**
     * 
     * @return
     */
    public String getTenantDomain() {
        return tenantDomain;
    }

    /**
     * 
     * @param tenantDomain
     */
    public void setTenantDomain(String tenantDomain) {
        this.tenantDomain = tenantDomain;
    }

}
