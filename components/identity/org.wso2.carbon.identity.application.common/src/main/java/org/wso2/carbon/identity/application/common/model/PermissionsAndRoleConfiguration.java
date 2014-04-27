package org.wso2.carbon.identity.application.common.model;


public class PermissionsAndRoleConfiguration {

    private ApplicationPermission[] permissions;
    private RoleMapping[] roleMappings;
    private String[] idpRoles;

    /**
     * 
     * @return
     */
    public ApplicationPermission[] getPermissions() {
        return permissions;
    }

    /**
     * 
     * @param permissions
     */
    public void setPermissions(ApplicationPermission[] permissions) {
        this.permissions = permissions;
    }

    /**
     * 
     * @return
     */
    public RoleMapping[] getRoleMappings() {
        return roleMappings;
    }

    /**
     * 
     * @param roleMappings
     */
    public void setRoleMappings(RoleMapping[] roleMappings) {
        this.roleMappings = roleMappings;
    }

	public String[] getIdpRoles() {
		return idpRoles;
	}

	public void setIdpRoles(String[] idpRoles) {
		this.idpRoles = idpRoles;
	}

}
