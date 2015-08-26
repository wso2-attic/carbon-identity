package org.wso2.carbon.identity.webfinger.internal;

import org.wso2.carbon.user.core.service.RealmService;


public class WebFingerServiceComponentHolder {
    private static RealmService realmService;

    private WebFingerServiceComponentHolder(){

    }

    public static RealmService getRealmService() {
        return realmService;
    }

    public static void setRealmService(RealmService realmService) {
        WebFingerServiceComponentHolder.realmService = realmService;
    }
}
