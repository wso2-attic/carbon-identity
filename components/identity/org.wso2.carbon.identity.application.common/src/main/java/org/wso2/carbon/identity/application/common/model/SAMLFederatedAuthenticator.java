package org.wso2.carbon.identity.application.common.model;

public class SAMLFederatedAuthenticator extends FederatedAuthenticator {

    /**
     * The IdP's SSO URL.
     */
    private String saml2SSOUrl;

    /**
	 * 
	 */
    private String idpEntityId;

    /**
     * If the LogoutRequestUrl is different from ACS URL
     */
    private String logoutRequestUrl;

    /*
     * The service provider's Entity Id
     */
    private String spEntityId;

    /**
     * If the AuthnRequest has to be signed
     */
    private boolean authnRequestSigned;

    /**
     * If Single Logout is enabled
     */
    private boolean logoutEnabled;

    /**
     * If the LogoutRequest has to be signed
     */
    private boolean logoutRequestSigned;

    /**
     * If SAMLResponse is signed
     */
    private boolean authnResponseSigned;

    /**
     * 
     * @return
     */
    public String getSpEntityId() {
        return spEntityId;
    }

    /**
     * 
     * @param spEntityId
     */
    public void setSpEntityId(String spEntityId) {
        this.spEntityId = spEntityId;
    }

    /**
     * 
     * @return
     */
    public boolean isAuthnRequestSigned() {
        return authnRequestSigned;
    }

    /**
     * 
     * @param authnRequestSigned
     */
    public void setAuthnRequestSigned(boolean authnRequestSigned) {
        this.authnRequestSigned = authnRequestSigned;
    }

    /**
     * 
     * @return
     */
    public boolean isLogoutEnabled() {
        return logoutEnabled;
    }

    /**
     * 
     * @param logoutEnabled
     */
    public void setLogoutEnabled(boolean logoutEnabled) {
        this.logoutEnabled = logoutEnabled;
    }

    /**
     * 
     * @return
     */
    public boolean isLogoutRequestSigned() {
        return logoutRequestSigned;
    }

    /**
     * 
     * @param logoutRequestSigned
     */
    public void setLogoutRequestSigned(boolean logoutRequestSigned) {
        this.logoutRequestSigned = logoutRequestSigned;
    }

    /**
     * 
     * @return
     */
    public boolean isAuthnResponseSigned() {
        return authnResponseSigned;
    }

    /**
     * 
     * @param authnResponseSigned
     */
    public void setAuthnResponseSigned(boolean authnResponseSigned) {
        this.authnResponseSigned = authnResponseSigned;
    }

    /**
     * 
     * @return
     */
    public String getIdpEntityId() {
        return idpEntityId;
    }

    /**
     * 
     * @param idpEntityId
     */
    public void setIdpEntityId(String idpEntityId) {
        this.idpEntityId = idpEntityId;
    }

    /**
     * 
     * @return
     */
    public String getSaml2SSOUrl() {
        return saml2SSOUrl;
    }

    /**
     * 
     * @param saml2ssoUrl
     */
    public void setSaml2SSOUrl(String saml2ssoUrl) {
        saml2SSOUrl = saml2ssoUrl;
    }

    /**
     * 
     * @return
     */
    public String getLogoutRequestUrl() {
        return logoutRequestUrl;
    }

    /**
     * 
     * @param logoutRequestUrl
     */
    public void setLogoutRequestUrl(String logoutRequestUrl) {
        this.logoutRequestUrl = logoutRequestUrl;
    }

    @Override
    public boolean isValid() {
        return saml2SSOUrl != null && spEntityId != null && idpEntityId != null;
    }

    @Override
    public String getName() {
        return "samlsso";
    }

}
