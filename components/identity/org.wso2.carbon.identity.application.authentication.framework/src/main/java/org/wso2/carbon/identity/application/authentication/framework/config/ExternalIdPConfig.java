package org.wso2.carbon.identity.application.authentication.framework.config;

import java.util.Map;

import org.wso2.carbon.idp.mgt.dto.TrustedIdPDTO;

public class ExternalIdPConfig {
	
	private String name;
	private Map<String, String> parameterMap;
	
	private String idPName;
	private boolean isPrimary;
	private String publicCert;
	private String[] claims;
	private String[] claimMappings;
	private String[] roles;
	private String[] roleMappings;
	private boolean isSAML2SSOEnabled;
	private String issuer;
	private String ssoUrl;
	private boolean isAuthnRequestSigned;
	private boolean isLogoutEnabled;
	private String logoutRequestUrl;
	private boolean isLogoutRequestSigned;
	private boolean isAuthnResponseSigned;
	private boolean isOIDCEnabled;
	private String authzEndpointUrl;
	private String tokenEndpointUrl;
	private String clientId;
	private String clientSecret;

	public ExternalIdPConfig(){}
	
	public ExternalIdPConfig(TrustedIdPDTO trustedIdPDTO){
		idPName = trustedIdPDTO.getIdPName();
		isPrimary = trustedIdPDTO.isPrimary();
		publicCert = trustedIdPDTO.getPublicCert();
		claims = trustedIdPDTO.getClaims();
		claimMappings = trustedIdPDTO.getClaimMappings();
		roles = trustedIdPDTO.getRoles();
		roleMappings = trustedIdPDTO.getRoleMappings();
		isSAML2SSOEnabled = trustedIdPDTO.isSAML2SSOEnabled();
		issuer = trustedIdPDTO.getIdpEntityId();
		ssoUrl = trustedIdPDTO.getSSOUrl();
		isAuthnRequestSigned = trustedIdPDTO.isAuthnRequestSigned();
		isLogoutEnabled = trustedIdPDTO.isLogoutEnabled();
		logoutRequestUrl = trustedIdPDTO.getLogoutRequestUrl();
		isLogoutRequestSigned = trustedIdPDTO.isLogoutRequestSigned();
		isAuthnResponseSigned = trustedIdPDTO.isAuthnResponseSigned();
		isOIDCEnabled = trustedIdPDTO.isOIDCEnabled();
		authzEndpointUrl = trustedIdPDTO.getAuthzEndpointUrl();
		tokenEndpointUrl = trustedIdPDTO.getTokenEndpointUrl();
		clientId = trustedIdPDTO.getClientId();
		clientSecret = trustedIdPDTO.getClientSecret();
	}
	
	public String getIdPName() {
		return this.idPName;
	}

	public void setIdPName(String idPName) {
		this.idPName = idPName;
	}

	public String getPublicCert() {
		return this.publicCert;
	}

	public void setPublicCert(String publicCert) {
		this.publicCert = publicCert;
	}

	public String[] getClaims() {
		return this.claims;
	}

	public void setClaims(String[] claims) {
		this.claims = claims;
	}

	public String[] getClaimMappings() {
		return this.claimMappings;
	}

	public void setClaimMappings(String[] claimMappings) {
		this.claimMappings = claimMappings;
	}

	public String[] getRoles() {
		return this.roles;
	}

	public void setRoles(String[] roles) {
		this.roles = roles;
	}

	public String[] getRoleMappings() {
		return this.roleMappings;
	}

	public void setRoleMappings(String[] roleMappings) {
		this.roleMappings = roleMappings;
	}

	public boolean isPrimary() {
		return this.isPrimary;
	}

	public void setPrimary(boolean primary) {
		this.isPrimary = primary;
	}

	public boolean isSAML2SSOEnabled() {
		return this.isSAML2SSOEnabled;
	}

	public void setSAML2SSOEnabled(boolean isSAML2SSOEnabled) {
		this.isSAML2SSOEnabled = isSAML2SSOEnabled;
	}

	public String getIssuer() {
		return this.issuer;
	}

	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	public String getSSOUrl() {
		return this.ssoUrl;
	}

	public void setSSOUrl(String ssoUrl) {
		this.ssoUrl = ssoUrl;
	}

	public boolean isAuthnRequestSigned() {
		return this.isAuthnRequestSigned;
	}

	public void setAuthnRequestSigned(boolean authnRequestSigned) {
		this.isAuthnRequestSigned = authnRequestSigned;
	}

	public boolean isLogoutEnabled() {
		return this.isLogoutEnabled;
	}

	public void setLogoutEnabled(boolean isSLOEnabled) {
		this.isLogoutEnabled = isSLOEnabled;
	}

	public String getLogoutRequestUrl() {
		return this.logoutRequestUrl;
	}

	public void setLogoutRequestUrl(String logoutRequestUrl)  {
		this.logoutRequestUrl = logoutRequestUrl;
	}

	public boolean isLogoutRequestSigned() {
		return this.isLogoutRequestSigned;
	}

	public void setLogoutRequestSigned(boolean logoutRequestSigned) {
		this.isLogoutRequestSigned = logoutRequestSigned;
	}

	public boolean isAuthnResponseSigned() {
		return this.isAuthnResponseSigned;
	}

	public void setAuthnResponseSigned(boolean authnResponseSigned) {
		this.isAuthnResponseSigned = authnResponseSigned;
	}

	public boolean isOIDCEnabled() {
		return this.isOIDCEnabled;
	}

	public void setOIDCEnabled(boolean isOIDCEnabled) {
		this.isOIDCEnabled = isOIDCEnabled;
	}

	public String getClientId() {
		return this.clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getClientSecret() {
		return this.clientSecret;
	}

	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	public String getAuthzEndpointUrl() {
		return this.authzEndpointUrl;
	}

	public void setAuthzEndpointUrl(String authzEndpointUrl) {
		this.authzEndpointUrl = authzEndpointUrl;
	}

	public String getTokenEndpointUrl() {
		return this.tokenEndpointUrl;
	}

	public void setTokenEndpointUrl(String tokenEndpointUrl) {
		this.tokenEndpointUrl = tokenEndpointUrl;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public Map<String, String> getParameterMap() {
		return parameterMap;
	}
	
	public void setParameterMap(Map<String, String> parameterMap) {
		this.parameterMap = parameterMap;
	}
}
