package org.wso2.carbon.identity.application.authenticator.oidc.ext.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator;
import org.wso2.carbon.identity.application.authenticator.oidc.ext.WindowsLiveOAuth2Authenticator;

import java.util.Hashtable;

/**
 * @scr.component name="identity.application.authenticator.windows.live.component" immediate="true"
 */
public class WindowsLiceAuthenticatorServiceComponent {

    private static Log log = LogFactory.getLog(WindowsLiceAuthenticatorServiceComponent.class);

    protected void activate(ComponentContext ctxt) {
        try {
            WindowsLiveOAuth2Authenticator windowsLoveAuthenticator = new WindowsLiveOAuth2Authenticator();
            Hashtable<String, String> props = new Hashtable<String, String>();

            ctxt.getBundleContext().registerService(ApplicationAuthenticator.class.getName(),
                    windowsLoveAuthenticator, props);
            if (log.isDebugEnabled()) {
                log.debug("Windows Live Authenticator bundle is activated");
            }
        } catch (Throwable e) {
            log.fatal(" Error while activating windows live authenticator ", e);
        }
    }

    protected void deactivate(ComponentContext ctxt) {
        if (log.isDebugEnabled()) {
            log.debug("Windows Live Authenticator bundle is deactivated");
        }
    }
}
