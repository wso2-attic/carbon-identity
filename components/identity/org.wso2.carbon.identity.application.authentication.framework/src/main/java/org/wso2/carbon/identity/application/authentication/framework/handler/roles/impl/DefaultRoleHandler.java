package org.wso2.carbon.identity.application.authentication.framework.handler.roles.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.handler.roles.RoleHandler;

public class DefaultRoleHandler implements RoleHandler {

    private static Log log = LogFactory.getLog(RoleHandler.class);
    private static RoleHandler instance = new DefaultRoleHandler();

    public static RoleHandler getInstance() {
        return instance;
    }
}
