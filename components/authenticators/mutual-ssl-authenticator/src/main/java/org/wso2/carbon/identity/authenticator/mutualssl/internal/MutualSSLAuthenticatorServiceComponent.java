package org.wso2.carbon.identity.authenticator.mutualssl.internal;

import java.util.Hashtable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.core.services.authentication.CarbonServerAuthenticator;
import org.wso2.carbon.identity.authenticator.mutualssl.MutualSSLAuthenticator;
import org.wso2.carbon.user.core.service.RealmService;


/**
 * @scr.component name=
 *                "mutualssl.MutualSSLAuthenticatorServiceComponent"
 *                immediate="true"
 * @scr.reference name="user.realmservice.default"
 *                interface="org.wso2.carbon.user.core.service.RealmService"
 *                cardinality="1..1" policy="dynamic"
 *                bind="setRealmService"
 *                unbind="unsetRealmService"
 */
public class MutualSSLAuthenticatorServiceComponent {

    private static RealmService realmService = null;
    private static BundleContext bundleContext = null;
    private static final Log log = LogFactory.getLog(MutualSSLAuthenticatorServiceComponent.class);
    
    protected void activate(ComponentContext cxt) {
        try {
            MutualSSLAuthenticator authenticator = new MutualSSLAuthenticator();
            MutualSSLAuthenticatorServiceComponent.setBundleContext(cxt.getBundleContext());
            Hashtable<String, String> props = new Hashtable<String, String>();
            props.put(CarbonConstants.AUTHENTICATOR_TYPE, authenticator.getAuthenticatorName());
            cxt.getBundleContext().registerService(CarbonServerAuthenticator.class.getName(),
                                                   authenticator, props);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            // throwing so that server will not start
            throw new RuntimeException("Failed to start the MutualSSL Authenticator Bundle" +
                                       e.getMessage(), e);
        }
        log.debug("Mutual SSL authenticator is activated");
    }

    protected void deactivate(ComponentContext context) {
        if (log.isDebugEnabled()) {
            log.debug("Appfactory common bundle is deactivated");
        }
    }

    protected void setRealmService(RealmService realmService) {
        if (log.isDebugEnabled()) {
            log.debug("RealmService acquired");
        }
        MutualSSLAuthenticatorServiceComponent.realmService = realmService;
    }

    protected void unsetRealmService(RealmService realmService) {
        MutualSSLAuthenticatorServiceComponent.realmService = null;
    }

    public static RealmService getRealmService() {
        return realmService;
    }

	public static BundleContext getBundleContext() {
		return bundleContext;
	}

	public static void setBundleContext(BundleContext bundleContext) {
		MutualSSLAuthenticatorServiceComponent.bundleContext = bundleContext;
	}

}

