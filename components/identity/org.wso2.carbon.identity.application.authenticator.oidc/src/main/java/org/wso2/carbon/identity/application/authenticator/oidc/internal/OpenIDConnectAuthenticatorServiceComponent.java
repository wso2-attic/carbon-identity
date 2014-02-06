package org.wso2.carbon.identity.application.authenticator.oidc.internal;

import java.util.Hashtable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator;
import org.wso2.carbon.identity.application.authenticator.oidc.OpenIDConnectAuthenticator;

/**
 * @scr.component name="identity.application.authenticator.oidc.component" immediate="true"
 */
public class OpenIDConnectAuthenticatorServiceComponent{

    private static Log log = LogFactory.getLog(OpenIDConnectAuthenticatorServiceComponent.class);
    
    protected void activate(ComponentContext ctxt) {
    	try {
    	OpenIDConnectAuthenticator openIDConnectAuthenticator = new OpenIDConnectAuthenticator();
    	Hashtable<String, String> props = new Hashtable<String, String>();
    	
        ctxt.getBundleContext().registerService(ApplicationAuthenticator.class.getName(), openIDConnectAuthenticator, props);
        if(log.isDebugEnabled()) {
            log.debug("OpenID Connect Authenticator bundle is activated");
    	}
    	} catch (Throwable e) {
    		log.fatal(" Error while activating oidc authenticator ", e);
    	}
    }

    protected void deactivate(ComponentContext ctxt) {
        if(log.isDebugEnabled()) {
            log.debug("OpenID Connect Authenticator bundle is deactivated");
    	}
    }
}
