package org.wso2.carbon.identity.application.authentication.framework.handler.request;

import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.FrameworkException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface AuthenticationRequestHandler {
	
	void handle(HttpServletRequest request, HttpServletResponse response,
			AuthenticationContext context) throws ServletException,
			IOException, FrameworkException;
}
