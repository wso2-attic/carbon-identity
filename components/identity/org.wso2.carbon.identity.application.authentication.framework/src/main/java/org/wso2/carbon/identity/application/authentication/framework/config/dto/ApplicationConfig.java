/*
 *  Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.application.authentication.framework.config.dto;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.wso2.carbon.identity.application.common.model.ApplicationPermission;
import org.wso2.carbon.identity.application.common.model.ClaimConfiguration;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.PermissionsAndRoleConfiguration;
import org.wso2.carbon.identity.application.common.model.RoleMapping;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.common.model.ServiceProviderOwner;

public class ApplicationConfig implements Serializable {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private int applicationID = 0;
    private int tennantID = -1;
    private String applicationName = null;
    private String userName = null;
    private String userStoreDomain = null;
    private String roleClaim = null;
    private String[] permissions = null;
    private Map<String, String> claimMappings = null;
    private Map<String, String> roleMappings = null;

    public ApplicationConfig(ServiceProvider appDTO) {
        applicationID = appDTO.getApplicationID();
        applicationName = appDTO.getApplicationName();

        ServiceProviderOwner owner = appDTO.getOwner();
        userName = owner.getUserName();
        userStoreDomain = owner.getUserStoreDomain();
        tennantID = owner.getTenantId();

        ClaimConfiguration claimConfig = appDTO.getClaimConfiguration();
        roleClaim = claimConfig.getRoleClaimURI();

        PermissionsAndRoleConfiguration permissionRoleConfiguration;
        permissionRoleConfiguration = appDTO.getPermissionAndRoleConfiguration();

        ApplicationPermission[] permissionList = permissionRoleConfiguration.getPermissions();
        if(permissionList == null) {
        	permissionList = new ApplicationPermission[0];
        }
        
        permissions = new String[permissionList.length];
        
        for (int i = 0; i < permissionList.length; i++) {
            ApplicationPermission permission = permissionList[i];
            permissions[i++] = permission.getValue();
        }


        ClaimMapping[] claimMapping = claimConfig.getClaimMappings();

        if (claimMapping != null && claimMapping.length > 0) {
            claimMappings = new HashMap<String, String>();
            for (ClaimMapping claim : claimMapping) {
                claimMappings.put(claim.getLocalClaim().getClaimUri(), claim.getSpClaim().getClaimUri());
            }
        }
        
        RoleMapping[] roleMappings = permissionRoleConfiguration.getRoleMappings();

        if (roleMappings != null && roleMappings.length > 0) {
            claimMappings = new HashMap<String, String>();
            for (RoleMapping roleMapping : roleMappings) {
                claimMappings.put(roleMapping.getLocalRole().getLocalRoleName(), roleMapping.getRemoteRole());
            }
        }
    }

    public int getApplicationID() {
        return applicationID;
    }

    public void setApplicationID(int applicationID) {
        this.applicationID = applicationID;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserStoreDomain() {
        return userStoreDomain;
    }

    public void setUserStoreDomain(String userStoreDomain) {
        this.userStoreDomain = userStoreDomain;
    }

    public int getTennantID() {
        return tennantID;
    }

    public void setTennantID(int tennantID) {
        this.tennantID = tennantID;
    }

    public String getRoleClaim() {
        return roleClaim;
    }

    public void setRoleClaim(String roleClaim) {
        this.roleClaim = roleClaim;
    }

    public String[] getPermissions() {
        return permissions;
    }

    public void setPermissions(String[] permissions) {
        this.permissions = permissions;
    }

    public Map<String, String> getClaimMappings() {
        return claimMappings;
    }

    public void setClaimMappings(Map<String, String> claimMappings) {
        this.claimMappings = claimMappings;
    }

    public Map<String, String> getRoleMappings() {
        return roleMappings;
    }

    public void setRoleMappings(Map<String, String> roleMappings) {
        this.roleMappings = roleMappings;
    }

    public boolean noClaimMapping() {
        return claimMappings == null;
    }

}
