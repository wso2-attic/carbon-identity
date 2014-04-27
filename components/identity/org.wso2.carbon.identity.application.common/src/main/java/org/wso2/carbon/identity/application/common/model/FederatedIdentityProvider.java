package org.wso2.carbon.identity.application.common.model;

public class FederatedIdentityProvider {

	private String identityProviderName;
	private String alias;
	private boolean primary;
	private String homeRealmId;
	private FederatedAuthenticator[] federatedAuthenticators;
	private FederatedAuthenticator defaultAuthenticator;
	private ProvisioningConnector[] provisoningConnectors;
	private ProvisioningConnector defaultProvisioinongConnector;
	private ClaimConfiguration claimConfiguration;
	private String certificate;
	private PermissionsAndRoleConfiguration permissionAndRoleConfiguration;
	private JustInTimeProvisioningConfiguration justInTimeProvisioningConfiguration;

	/**
	 * 
	 * @return
	 */
	public FederatedAuthenticator[] getFederatedAuthenticators() {
		return federatedAuthenticators;
	}

	/**
	 * 
	 * @param federatedAuthenticators
	 */
	public void setFederatedAuthenticators(
			FederatedAuthenticator[] federatedAuthenticators) {
		this.federatedAuthenticators = federatedAuthenticators;
	}

	/**
	 * 
	 * @return
	 */
	public FederatedAuthenticator getDefaultAuthenticator() {
		return defaultAuthenticator;
	}

	/**
	 * 
	 * @param defaultAuthenticator
	 */
	public void setDefaultAuthenticator(
			FederatedAuthenticator defaultAuthenticator) {
		this.defaultAuthenticator = defaultAuthenticator;
	}

	/**
	 * 
	 * @return
	 */
	public String getIdentityProviderName() {
		return identityProviderName;
	}

	/**
	 * 
	 * @param identityProviderName
	 */
	public void setIdentityProviderName(String identityProviderName) {
		this.identityProviderName = identityProviderName;
	}

	/**
	 * 
	 * @return
	 */
	public ProvisioningConnector getDefaultProvisioinongConnector() {
		return defaultProvisioinongConnector;
	}

	/**
	 * 
	 * @param defaultProvisioinongConnector
	 */
	public void setDefaultProvisioinongConnector(
			ProvisioningConnector defaultProvisioinongConnector) {
		this.defaultProvisioinongConnector = defaultProvisioinongConnector;
	}

	/**
	 * 
	 * @return
	 */
	public ProvisioningConnector[] getProvisoningConnectors() {
		return provisoningConnectors;
	}

	/**
	 * 
	 * @param provisoningConnectors
	 */
	public void setProvisoningConnectors(
			ProvisioningConnector[] provisoningConnectors) {
		this.provisoningConnectors = provisoningConnectors;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isPrimary() {
		return primary;
	}

	/**
	 * 
	 * @param primary
	 */
	public void setPrimary(boolean primary) {
		this.primary = primary;
	}

	/**
	 * 
	 * @return
	 */
	public String getAlias() {
		return alias;
	}

	/**
	 * 
	 * @param alias
	 */
	public void setAlias(String alias) {
		this.alias = alias;
	}

	/**
	 * 
	 * @return
	 */
	public String getCertificate() {
		return certificate;
	}

	/**
	 * 
	 * @param certificate
	 */
	public void setCertificate(String certificate) {
		this.certificate = certificate;
	}

	/**
	 * 
	 * @return
	 */
	public ClaimConfiguration getClaimConfiguration() {
		return claimConfiguration;
	}

	/**
	 * 
	 * @param claimConfiguration
	 */
	public void setClaimConfiguration(ClaimConfiguration claimConfiguration) {
		this.claimConfiguration = claimConfiguration;
	}

	/**
	 * 
	 * @return
	 */
	public PermissionsAndRoleConfiguration getPermissionAndRoleConfiguration() {
		return permissionAndRoleConfiguration;
	}

	/**
	 * 
	 * @param permissionAndRoleConfiguration
	 */
	public void setPermissionAndRoleConfiguration(
			PermissionsAndRoleConfiguration permissionAndRoleConfiguration) {
		this.permissionAndRoleConfiguration = permissionAndRoleConfiguration;
	}

	/**
	 * 
	 * @return
	 */
	public String getHomeRealmId() {
		return homeRealmId;
	}

	/**
	 * 
	 * @param homeRealmId
	 */
	public void setHomeRealmId(String homeRealmId) {
		this.homeRealmId = homeRealmId;
	}

	/**
	 * 
	 * @return
	 */
	public JustInTimeProvisioningConfiguration getJustInTimeProvisioningConfiguration() {
		return justInTimeProvisioningConfiguration;
	}

	/**
	 * 
	 * @param justTimeProvisioningConfiguration
	 */
	public void setJustInTimeProvisioningConfiguration(
			JustInTimeProvisioningConfiguration justInTimeProvisioningConfiguration) {
		this.justInTimeProvisioningConfiguration = justInTimeProvisioningConfiguration;
	}

}