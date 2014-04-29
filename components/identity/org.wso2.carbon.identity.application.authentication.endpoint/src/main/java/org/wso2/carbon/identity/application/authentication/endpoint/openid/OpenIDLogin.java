package org.wso2.carbon.identity.application.authentication.endpoint.openid;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class OpenIDLogin extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
    																					 IOException {
		doGet(request, response);
	}
	
	@Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {

    	if (request.getRequestURI().contains("/openid_login.do")) {
    		request.getRequestDispatcher("login.jsp").forward(request, response);
    	} else if (request.getRequestURI().contains("/openid_profile.do")) {
    		request.getRequestDispatcher("openid_profile.jsp").forward(request, response);
    	}
    }
}
