package org.wso2.carbon.identity.application.authenticator.openid.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator;
import org.wso2.carbon.identity.application.authenticator.openid.OpenIDAuthenticator;

import java.util.Hashtable;

/**
 * @scr.component name="identity.application.authenticator.openid.component" immediate="true"
 */
public class OpenIDAuthenticatorServiceComponent {

    private static Log log = LogFactory.getLog(OpenIDAuthenticatorServiceComponent.class);

    protected void activate(ComponentContext ctxt) {
        OpenIDAuthenticator openIdAuthenticator = new OpenIDAuthenticator();
        Hashtable<String, String> props = new Hashtable<String, String>();

        ctxt.getBundleContext().registerService(ApplicationAuthenticator.class.getName(), openIdAuthenticator, props);

        if (log.isDebugEnabled()) {
            log.info("OpenID Authenticator bundle is activated");
        }
    }

    protected void deactivate(ComponentContext ctxt) {
        if (log.isDebugEnabled()) {
            log.info("OpenID Authenticator bundle is deactivated");
        }
    }
}