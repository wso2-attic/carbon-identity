package org.wso2.carbon.identity.application.common.model;


public class ClaimConfiguration {

    private String roleClaimURI;
    private String userClaimURI;
    private Claim[] idpClaims;
    private ClaimMapping[] claimMappings;

    /**
     * 
     * @return
     */
    public String getRoleClaimURI() {
        return roleClaimURI;
    }

    /**
     * 
     * @param roleClaimURI
     */
    public void setRoleClaimURI(String roleClaimURI) {
        this.roleClaimURI = roleClaimURI;
    }

    /**
     * 
     * @return
     */
    public ClaimMapping[] getClaimMappings() {
        return claimMappings;
    }

    /**
     * 
     * @param claimMappins
     */
    public void setClaimMappings(ClaimMapping[] claimMappins) {
        this.claimMappings = claimMappins;
    }

	public String getUserClaimURI() {
		return userClaimURI;
	}

	public void setUserClaimURI(String userClaimURI) {
		this.userClaimURI = userClaimURI;
	}

	public Claim[] getIdpClaims() {
		return idpClaims;
	}

	public void setIdpClaims(Claim[] idpClaims) {
		this.idpClaims = idpClaims;
	}
}
