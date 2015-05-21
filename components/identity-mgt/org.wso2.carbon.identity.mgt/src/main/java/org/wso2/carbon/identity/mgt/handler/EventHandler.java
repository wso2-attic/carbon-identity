package org.wso2.carbon.identity.mgt.handler;

import org.wso2.carbon.identity.mgt.event.IdentityMgtEvent;
import org.wso2.carbon.user.core.UserStoreException;

public interface EventHandler {

    public boolean handleEvent(IdentityMgtEvent identityMgtEvent) throws UserStoreException;

    /**
     * Initiate the EventHandler
     */
    public void init();

    /**
     * This method returns whether this EventHandler can handle the given type of event or not. ie Whether
     * this handler is registered to the passed event or not.
     *
     * @param event Event which is to be executed
     * @return True if the module can handle or registered to the event.
     */
    public boolean isRegistered(IdentityMgtEvent event);


    public String getModuleName();

}
