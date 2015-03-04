package org.wso2.carbon.identity.application.authentication.framework.handler.request;

import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.FrameworkException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface LogoutRequestHandler {

    void handle(HttpServletRequest request, HttpServletResponse response,
                AuthenticationContext context) throws FrameworkException;
}
