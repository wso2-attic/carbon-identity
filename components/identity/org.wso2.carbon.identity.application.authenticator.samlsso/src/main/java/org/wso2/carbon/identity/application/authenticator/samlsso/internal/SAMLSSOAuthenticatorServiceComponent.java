package org.wso2.carbon.identity.application.authenticator.samlsso.internal;

import java.util.Hashtable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator;
import org.wso2.carbon.identity.application.authenticator.samlsso.SAMLSSOAuthenticator;

/**
 * @scr.component name="identity.application.authenticator.samlsso.component" immediate="true"
 */
public class SAMLSSOAuthenticatorServiceComponent{

    private static Log log = LogFactory.getLog(SAMLSSOAuthenticatorServiceComponent.class);
    
    protected void activate(ComponentContext ctxt) {
    	SAMLSSOAuthenticator samlSSOAuthenticator = new SAMLSSOAuthenticator();
    	Hashtable<String, String> props = new Hashtable<String, String>();
    	
        ctxt.getBundleContext().registerService(ApplicationAuthenticator.class.getName(), samlSSOAuthenticator, props);
        
        if (log.isDebugEnabled()) {
            log.info("SAMLSSO Authenticator bundle is activated");
        }
    }

    protected void deactivate(ComponentContext ctxt) {
        if (log.isDebugEnabled()) {
            log.info("SAMLSSO Authenticator bundle is deactivated");
        }
    }
}
