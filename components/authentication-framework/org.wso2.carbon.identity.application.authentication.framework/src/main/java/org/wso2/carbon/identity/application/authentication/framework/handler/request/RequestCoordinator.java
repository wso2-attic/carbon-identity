package org.wso2.carbon.identity.application.authentication.framework.handler.request;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface RequestCoordinator {

    void handle(HttpServletRequest request, HttpServletResponse response) throws IOException;
}
