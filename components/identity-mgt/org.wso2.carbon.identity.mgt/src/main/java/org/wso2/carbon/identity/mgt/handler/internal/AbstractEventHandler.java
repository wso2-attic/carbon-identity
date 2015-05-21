package org.wso2.carbon.identity.mgt.handler.internal;

import org.wso2.carbon.identity.mgt.handler.EventHandler;

public abstract class AbstractEventHandler  implements EventHandler{

    @Override
    public String getModuleName() {
        return this.getClass().getSimpleName();
    }

}
