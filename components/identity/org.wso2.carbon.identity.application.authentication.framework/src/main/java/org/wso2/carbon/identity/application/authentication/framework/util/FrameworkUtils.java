package org.wso2.carbon.identity.application.authentication.framework.util;

import org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.internal.ApplicationAuthenticationFrameworkServiceComponent;

public class FrameworkUtils {
	
	public static ApplicationAuthenticator getAppAuthenticatorByName(String name) {
		 for (ApplicationAuthenticator authenticator : ApplicationAuthenticationFrameworkServiceComponent.authenticators) {
            
            if (name.equals(authenticator.getAuthenticatorName())) {
                return authenticator;
            }
        }
		 
		 return null;
	}
}
