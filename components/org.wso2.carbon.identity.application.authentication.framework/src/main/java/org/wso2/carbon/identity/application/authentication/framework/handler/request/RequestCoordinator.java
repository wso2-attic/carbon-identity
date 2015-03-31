package org.wso2.carbon.identity.application.authentication.framework.handler.request;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface RequestCoordinator {
    
    void handle(HttpServletRequest request, HttpServletResponse response) throws IOException;
}
