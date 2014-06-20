package org.wso2.carbon.identity.application.authentication.framework.handler.step;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.FrameworkException;

public interface StepHandler {

    public void handle(HttpServletRequest request, HttpServletResponse response,
            AuthenticationContext context) throws FrameworkException;
}
