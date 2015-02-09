package org.wso2.carbon.identity.application.authenticator.totp.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator;
import org.wso2.carbon.identity.application.authenticator.totp.TOTPAuthenticator;
import org.wso2.carbon.identity.totp.TOTPManager;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.Hashtable;


/**
 * @scr.component name="identity.application.authenticator.totp.component" immediate="true"
 * @scr.reference name="realm.service"
 * interface="org.wso2.carbon.user.core.service.RealmService"cardinality="1..1"
 * policy="dynamic" bind="setRealmService" unbind="unsetRealmService"
 * @scr.reference name="totp.manager"
 * interface="org.wso2.carbon.identity.totp.TOTPManager"cardinality="1..1"
 * policy="dynamic" bind="setTotpManager" unbind="unsetTotpManager"
 */
public class TOTPAuthenticatorServiceComponent {

    private static Log log = LogFactory.getLog(TOTPAuthenticatorServiceComponent.class);

    private static RealmService realmService;
    private static TOTPManager totpManager;
    
    protected void activate(ComponentContext ctxt) {

        TOTPAuthenticator totpAuth = new TOTPAuthenticator();
        Hashtable<String, String> props = new Hashtable<String, String>();

        ctxt.getBundleContext().registerService(ApplicationAuthenticator.class.getName(), totpAuth, props);

        if (log.isDebugEnabled()) {
            log.info("TOTPAuthenticator bundle is activated");
        }
    }

    protected void deactivate(ComponentContext ctxt) {
        if (log.isDebugEnabled()) {
            log.info("TOTPAuthenticator bundle is deactivated");
        }
    }

    protected void setRealmService(RealmService realmService) {
        log.debug("Setting the Realm Service");
        TOTPAuthenticatorServiceComponent.realmService = realmService;
    }

    protected void unsetRealmService(RealmService realmService) {
        log.debug("UnSetting the Realm Service");
        TOTPAuthenticatorServiceComponent.realmService = null;
    }

    public static RealmService getRealmService() {
        return realmService;
    }

    protected void setTotpManager(TOTPManager totpManager) {
        log.debug("Setting the TOTP Service");
        TOTPAuthenticatorServiceComponent.totpManager = totpManager;
    }

    protected void unsetTotpManager(TOTPManager totpManager) {
        log.debug("UnSetting the TOTP Service");
        TOTPAuthenticatorServiceComponent.totpManager = null;
    }

    public static TOTPManager getTotpManager() {
        return totpManager;
    }

}
