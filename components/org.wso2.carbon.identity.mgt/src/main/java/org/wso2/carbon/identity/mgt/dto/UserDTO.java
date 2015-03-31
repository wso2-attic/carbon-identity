/**
 * 
 */
package org.wso2.carbon.identity.mgt.dto;

import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

/**
 * 
 */
public class UserDTO {
    
    private String userId;
    
    private String tenantDomain;
    
    private int tenantId;

    public UserDTO(String userName) {

        this.userId = MultitenantUtils.getTenantAwareUsername(userName);
        this.tenantDomain = MultitenantUtils.getTenantDomain(userName);

    }

    public String getUserId() {
        return userId;
    }

    public String getTenantDomain() {
        return tenantDomain;
    }

    public int getTenantId() {
        return tenantId;
    }

    public void setTenantId(int tenantId) {
        this.tenantId = tenantId;
    }
}
