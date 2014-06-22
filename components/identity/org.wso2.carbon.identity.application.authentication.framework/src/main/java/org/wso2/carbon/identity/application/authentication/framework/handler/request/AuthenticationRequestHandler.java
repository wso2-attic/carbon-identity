package org.wso2.carbon.identity.application.authentication.framework.handler.request;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.FrameworkException;

public interface AuthenticationRequestHandler {

    void handle(HttpServletRequest request, HttpServletResponse response,
            AuthenticationContext context) throws FrameworkException;
}
