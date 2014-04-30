package org.wso2.carbon.identity.application.authenticator.requestpath.basicauth.internal;

import java.util.Hashtable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator;
import org.wso2.carbon.identity.application.authenticator.requestpath.basicauth.BasicAuthRequestPathAuthenticator;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * @scr.component name="identity.application.authenticator.requestpath.basicauth.component" immediate="true"
 * @scr.reference name="realm.service"
 * interface="org.wso2.carbon.user.core.service.RealmService"cardinality="1..1"
 * policy="dynamic" bind="setRealmService" unbind="unsetRealmService"
 */
public class BasicAuthRequestPathAuthenticatorServiceComponent{

    private static Log log = LogFactory.getLog(BasicAuthRequestPathAuthenticatorServiceComponent.class);
    
    private static RealmService realmService;
    
    protected void activate(ComponentContext ctxt) {
    	
    	BasicAuthRequestPathAuthenticator auth = new BasicAuthRequestPathAuthenticator();
    	Hashtable<String, String> props = new Hashtable<String, String>();
    	
        ctxt.getBundleContext().registerService(ApplicationAuthenticator.class.getName(), auth, props);
        
        if (log.isDebugEnabled()) {
            log.info("BasicAuthRequestPathAuthenticator bundle is activated");
        }
    }

    protected void deactivate(ComponentContext ctxt) {
        if (log.isDebugEnabled()) {
            log.info("BasicAuthRequestPathAuthenticator bundle is deactivated");
        }
    }
    
    protected void setRealmService(RealmService realmService) {
        log.debug("Setting the Realm Service");
        BasicAuthRequestPathAuthenticatorServiceComponent.realmService = realmService;
    }

    protected void unsetRealmService(RealmService realmService) {
        log.debug("UnSetting the Realm Service");
        BasicAuthRequestPathAuthenticatorServiceComponent.realmService = null;
    }

    public static RealmService getRealmService() {
        return realmService;
    }

}
