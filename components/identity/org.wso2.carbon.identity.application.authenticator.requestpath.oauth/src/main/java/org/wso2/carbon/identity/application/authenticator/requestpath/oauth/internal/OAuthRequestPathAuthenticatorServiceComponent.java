package org.wso2.carbon.identity.application.authenticator.requestpath.oauth.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator;
import org.wso2.carbon.identity.application.authenticator.requestpath.oauth.OAuthRequestPathAuthenticator;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.Hashtable;

/**
 * @scr.component name="identity.application.authenticator.requestpath.oauth.component" immediate="true"
 * @scr.reference name="realm.service"
 * interface="org.wso2.carbon.user.core.service.RealmService"cardinality="1..1"
 * policy="dynamic" bind="setRealmService" unbind="unsetRealmService"
 */
public class OAuthRequestPathAuthenticatorServiceComponent {

    private static Log log = LogFactory.getLog(OAuthRequestPathAuthenticatorServiceComponent.class);

    private static RealmService realmService;

    public static RealmService getRealmService() {
        return realmService;
    }

    protected void setRealmService(RealmService realmService) {
        log.debug("Setting the Realm Service");
        OAuthRequestPathAuthenticatorServiceComponent.realmService = realmService;
    }

    protected void activate(ComponentContext ctxt) {

        OAuthRequestPathAuthenticator auth = new OAuthRequestPathAuthenticator();
        Hashtable<String, String> props = new Hashtable<String, String>();

        ctxt.getBundleContext().registerService(ApplicationAuthenticator.class.getName(), auth, props);

        if (log.isDebugEnabled()) {
            log.info("OAuthRequestPathAuthenticator bundle is activated");
        }
    }

    protected void deactivate(ComponentContext ctxt) {
        if (log.isDebugEnabled()) {
            log.info("OAuthRequestPathAuthenticator bundle is deactivated");
        }
    }

    protected void unsetRealmService(RealmService realmService) {
        log.debug("UnSetting the Realm Service");
        OAuthRequestPathAuthenticatorServiceComponent.realmService = null;
    }

}
