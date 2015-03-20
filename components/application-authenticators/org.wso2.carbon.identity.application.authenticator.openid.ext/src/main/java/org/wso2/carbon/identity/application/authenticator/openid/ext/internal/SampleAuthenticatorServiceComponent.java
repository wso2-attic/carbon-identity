package org.wso2.carbon.identity.application.authenticator.openid.ext.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator;
import org.wso2.carbon.identity.application.authenticator.openid.ext.GoogleOpenIDAuthenticator;
import org.wso2.carbon.identity.application.authenticator.openid.ext.YahooOpenIDAuthenticator;

import java.util.Hashtable;

/**
 * @scr.component name="identity.application.authenticator.openid.ext.component" immediate="true"
 */
public class SampleAuthenticatorServiceComponent {

    private static Log log = LogFactory.getLog(SampleAuthenticatorServiceComponent.class);

    protected void activate(ComponentContext ctxt) {

        Hashtable<String, String> props = new Hashtable<String, String>();

        GoogleOpenIDAuthenticator googleOpenIDAuthenticator = new GoogleOpenIDAuthenticator();

        ctxt.getBundleContext().registerService(ApplicationAuthenticator.class.getName(),
                googleOpenIDAuthenticator, props);

        YahooOpenIDAuthenticator yahooOpenIDAuthenticator = new YahooOpenIDAuthenticator();

        ctxt.getBundleContext().registerService(ApplicationAuthenticator.class.getName(),
                yahooOpenIDAuthenticator, props);

        if (log.isDebugEnabled()) {
            log.info("Sample Authenticator bundle is activated");
        }
    }

    protected void deactivate(ComponentContext ctxt) {
        if (log.isDebugEnabled()) {
            log.info("Sample Authenticator bundle is deactivated");
        }
    }
}
