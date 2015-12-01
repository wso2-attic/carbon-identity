package org.wso2.carbon.identity.application.authentication.endpoint.util;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


public class TestServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) throws ServletException, IOException {
        httpServletResponse.sendRedirect("/authenticationendpoint/login.do");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.sendRedirect("/authenticationendpoint/login.do");
    }
}
