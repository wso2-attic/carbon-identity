package org.wso2.carbon.identity.application.common.model;

public class PassiveSTSFederatedAuthenticator extends FederatedAuthenticator {

    /**
     * The IdP's Passive STS Realm
     */
    private String passiveSTSRealm;

    /**
     * The IdP's Passive STS URL
     */
    private String passiveSTSUrl;

    /**
     * 
     * @return
     */
    public String getPassiveSTSRealm() {
        return passiveSTSRealm;
    }

    /**
     * 
     * @param passiveSTSRealm
     */
    public void setPassiveSTSRealm(String passiveSTSRealm) {
        this.passiveSTSRealm = passiveSTSRealm;
    }

    /**
     * 
     * @return
     */
    public String getPassiveSTSUrl() {
        return passiveSTSUrl;
    }

    /**
     * 
     * @param passiveSTSUrl
     */
    public void setPassiveSTSUrl(String passiveSTSUrl) {
        this.passiveSTSUrl = passiveSTSUrl;
    }

    @Override
    public boolean isValid() {
        return passiveSTSUrl != null || passiveSTSRealm != null;
    }

    @Override
    public String getName() {
        return "passivests";
    }
}
