package org.wso2.carbon.identity.saml.metadata.internal;

import org.wso2.carbon.user.core.service.RealmService;

/**
 * Created by maduranga on 6/16/15.
 */
public class SAMLMetadataDataholder {
    private static RealmService realmService;

    private static SAMLMetadataDataholder samlMetadataDataholder = new SAMLMetadataDataholder();

    public static SAMLMetadataDataholder getInstance(){
        return samlMetadataDataholder;
    }

    public RealmService getRealmService() {
        return realmService;
    }

    public void setRealmService(RealmService realmService) {
        SAMLMetadataDataholder.realmService = realmService;
    }
}
