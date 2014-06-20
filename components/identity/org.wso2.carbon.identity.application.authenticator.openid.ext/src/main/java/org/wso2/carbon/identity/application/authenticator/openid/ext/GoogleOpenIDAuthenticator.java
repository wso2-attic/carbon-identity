package org.wso2.carbon.identity.application.authenticator.openid.ext;

import org.wso2.carbon.identity.application.authenticator.openid.OpenIDAuthenticator;

public class GoogleOpenIDAuthenticator  extends OpenIDAuthenticator{

	/**
	 * 
	 */
	private static final long serialVersionUID = -5947608036809026467L;
	
    @Override
    public String getFriendlyName() {
        return "google";
    }

    @Override
    public String getName() {
        return "GoogleOpenIDAuthenticator";
    }
    
    protected String getOpenIDServerUrl() {
        return "https://www.google.com/accounts/o8/id";
    }

}
