package org.wso2.carbon.identity.sso.cas.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.wso2.carbon.identity.sso.cas.handler.ProxyLoginHandler;

public class CASProxyLoginServlet extends HttpServlet {

	private static final long serialVersionUID = -1008636285159214297L;

	@Override
	protected void doGet(HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse) throws ServletException,
			IOException {
		handleRequest(httpServletRequest, httpServletResponse);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		handleRequest(req, resp);

	}

	private void handleRequest(HttpServletRequest req,
			HttpServletResponse resp) throws ServletException,
			IOException {
		ProxyLoginHandler handler = new ProxyLoginHandler();
		handler.handle(req, resp);
	}
}