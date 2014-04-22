package org.wso2.carbon.identity.application.common.model;

public class ServiceProviderOwner {

    private int tenantId;
    private String userStoreDomain;
    private String userName;

    /**
     * 
     * @return
     */
    public int getTenantId() {
        return tenantId;
    }

    /**
     * 
     * @param tenantId
     */
    public void setTenantId(int tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * 
     * @return
     */
    public String getUserStoreDomain() {
        return userStoreDomain;
    }

    /**
     * 
     * @param userStoreDomain
     */
    public void setUserStoreDomain(String userStoreDomain) {
        this.userStoreDomain = userStoreDomain;
    }

    /**
     * 
     * @return
     */
    public String getUserName() {
        return userName;
    }

    /**
     * 
     * @param userName
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

}
