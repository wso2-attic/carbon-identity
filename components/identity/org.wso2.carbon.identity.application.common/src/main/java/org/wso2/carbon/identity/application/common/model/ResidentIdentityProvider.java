package org.wso2.carbon.identity.application.common.model;

public class ResidentIdentityProvider {

	/*
	 * The IdP's home realm identifier
	 */
	private String homeRealmId;

	/**
	 * The IdP's Certificate
	 */
	private String certificate;

	/**
	 * The Identity provider's Entity Id
	 */
	private String idpEntityId;
	
	/**
	 * 
	 */
	private String saml2SSOUrl;

	/**
	 * If the LogoutRequestUrl is different from ACS URL
	 */
	private String logoutRequestUrl;

	/**
	 * OIDC User Info End-point
	 */
	private String userInfoEndpointUrl;

	/**
	 * The IdP's OpenID Realm
	 */
	private String openIdRealm;

	/**
	 * The IdP's OpenID URL
	 */
	private String openIDUrl;

	/**
	 * The IdP's Passive STS Realm
	 */
	private String passiveSTSRealm;

	/**
	 * The IdP's Passive STS URL
	 */
	private String passiveSTSUrl;

	private String authzEndpointUrl;

	private String tokenEndpointUrl;

	public String getHomeRealmId() {
		return homeRealmId;
	}

	public void setHomeRealmId(String homeRealmId) {
		this.homeRealmId = homeRealmId;
	}

	public String getCertificate() {
		return certificate;
	}

	public void setCertificate(String certificate) {
		this.certificate = certificate;
	}

	public String getIdpEntityId() {
		return idpEntityId;
	}

	public void setIdpEntityId(String idpEntityId) {
		this.idpEntityId = idpEntityId;
	}

	public String getLogoutRequestUrl() {
		return logoutRequestUrl;
	}

	public void setLogoutRequestUrl(String logoutRequestUrl) {
		this.logoutRequestUrl = logoutRequestUrl;
	}

	public String getAuthzEndpointUrl() {
		return authzEndpointUrl;
	}

	public void setAuthzEndpointUrl(String authzEndpointUrl) {
		this.authzEndpointUrl = authzEndpointUrl;
	}

	public String getTokenEndpointUrl() {
		return tokenEndpointUrl;
	}

	public void setTokenEndpointUrl(String tokenEndpointUrl) {
		this.tokenEndpointUrl = tokenEndpointUrl;
	}

	public String getUserInfoEndpointUrl() {
		return userInfoEndpointUrl;
	}

	public void setUserInfoEndpointUrl(String userInfoEndpointUrl) {
		this.userInfoEndpointUrl = userInfoEndpointUrl;
	}

	public String getOpenIdRealm() {
		return openIdRealm;
	}

	public void setOpenIdRealm(String openIdRealm) {
		this.openIdRealm = openIdRealm;
	}

	public String getOpenIDUrl() {
		return openIDUrl;
	}

	public void setOpenIDUrl(String openIDUrl) {
		this.openIDUrl = openIDUrl;
	}

	public String getPassiveSTSRealm() {
		return passiveSTSRealm;
	}

	public void setPassiveSTSRealm(String passiveSTSRealm) {
		this.passiveSTSRealm = passiveSTSRealm;
	}

	public String getPassiveSTSUrl() {
		return passiveSTSUrl;
	}

	public void setPassiveSTSUrl(String passiveSTSUrl) {
		this.passiveSTSUrl = passiveSTSUrl;
	}

    public String getSaml2SSOUrl() {
        return saml2SSOUrl;
    }

    public void setSaml2SSOUrl(String saml2ssoUrl) {
        saml2SSOUrl = saml2ssoUrl;
    }

}
