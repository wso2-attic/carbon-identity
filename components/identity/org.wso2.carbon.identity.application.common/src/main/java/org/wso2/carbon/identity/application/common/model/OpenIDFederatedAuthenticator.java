package org.wso2.carbon.identity.application.common.model;

public class OpenIDFederatedAuthenticator extends FederatedAuthenticator {

    private String openIDServerUrl;
    private String openIDRealm;

    /**
     * 
     * @return
     */
    public String getOpenIDRealm() {
        return openIDRealm;
    }

    /**
     * 
     * @param openIDRealm
     */
    public void setOpenIDRealm(String openIDRealm) {
        this.openIDRealm = openIDRealm;
    }

    /**
     * 
     * @return
     */
    public String getOpenIDServerUrl() {
        return openIDServerUrl;
    }

    /**
     * 
     * @param openIDServerUrl
     */
    public void setOpenIDServerUrl(String openIDServerUrl) {
        this.openIDServerUrl = openIDServerUrl;
    }

    @Override
    public boolean isValid() {
        return openIDRealm != null || openIDServerUrl != null;
    }

    @Override
    public String getName() {
        return "openid";
    }
}
