package org.wso2.carbon.identity.application.authenticator.basicauth.internal;

import java.util.Hashtable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator;
import org.wso2.carbon.identity.application.authenticator.basicauth.BasicAuthenticator;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * @scr.component name="identity.application.authenticator.basicauth.component" immediate="true"
 * @scr.reference name="realm.service"
 * interface="org.wso2.carbon.user.core.service.RealmService"cardinality="1..1"
 * policy="dynamic" bind="setRealmService" unbind="unsetRealmService"
 */
public class BasicAuthenticatorServiceComponent{

    private static Log log = LogFactory.getLog(BasicAuthenticatorServiceComponent.class);
    
    private static RealmService realmService;
    
    protected void activate(ComponentContext ctxt) {
    	
    	BasicAuthenticator basicAuth = new BasicAuthenticator();
    	Hashtable<String, String> props = new Hashtable<String, String>();
    	
        ctxt.getBundleContext().registerService(ApplicationAuthenticator.class.getName(), basicAuth, props);
        
        if (log.isDebugEnabled()) {
            log.info("BasicAuthenticator bundle is activated");
        }
    }

    protected void deactivate(ComponentContext ctxt) {
        if (log.isDebugEnabled()) {
            log.info("BasicAuthenticator bundle is deactivated");
        }
    }
    
    protected void setRealmService(RealmService realmService) {
        log.debug("Setting the Realm Service");
        BasicAuthenticatorServiceComponent.realmService = realmService;
    }

    protected void unsetRealmService(RealmService realmService) {
        log.debug("UnSetting the Realm Service");
        BasicAuthenticatorServiceComponent.realmService = null;
    }

    public static RealmService getRealmService() {
        return realmService;
    }

}
