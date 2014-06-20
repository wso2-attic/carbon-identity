package org.wso2.carbon.identity.application.authenticator.openid.ext;

import org.wso2.carbon.identity.application.authenticator.openid.OpenIDAuthenticator;

public class YahooOpenIDAuthenticator extends OpenIDAuthenticator {

    /**
     * 
     */
    private static final long serialVersionUID = -782801773114711699L;

    @Override
    public String getFriendlyName() {
        return "yahoo";
    }

    @Override
    public String getName() {
        return "YahooOpenIDAuthenticator";
    }

    protected String getOpenIDServerUrl() {
        return "https://me.yahoo.com/";
    }
}
