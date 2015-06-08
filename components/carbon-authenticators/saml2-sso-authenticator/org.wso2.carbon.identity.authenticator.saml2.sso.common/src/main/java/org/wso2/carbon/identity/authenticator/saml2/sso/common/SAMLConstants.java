package org.wso2.carbon.identity.authenticator.saml2.sso.common;

public class SAMLConstants {

    public static final String ASSRTN_CONSUMER_URL = "assertnConsumerURL";
    public static final String SUBJECT = "subject";
    public static final String ASSERTION_STR = "assertionString";
    public static final String RELAY_STATE = "RelayState";
    public static final String FEDERATED_SAML2_SESSION_ID = "federatedSAML2SessionId";

    public class AuthnModes {
        private AuthnModes(){

        }
        public static final String USERNAME_PASSWORD = "usernamePasswordBasedAuthn";
        public static final String OPENID = "openIDBasedAuthn";
    }

}
