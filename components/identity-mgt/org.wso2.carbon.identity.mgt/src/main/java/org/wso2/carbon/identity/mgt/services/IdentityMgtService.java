package org.wso2.carbon.identity.mgt.services;

import org.wso2.carbon.identity.mgt.event.IdentityMgtEvent;
import org.wso2.carbon.user.core.UserStoreException;

import java.util.Properties;

public interface IdentityMgtService {

    public Properties addConfigurations(int tenantId);

    public Properties getConfigurations(int tenantId);

    public boolean handleEvent(IdentityMgtEvent identityMgtEvent) throws UserStoreException;

}
