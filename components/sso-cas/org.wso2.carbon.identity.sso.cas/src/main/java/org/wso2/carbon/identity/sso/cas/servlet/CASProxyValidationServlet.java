package org.wso2.carbon.identity.sso.cas.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CASProxyValidationServlet extends CASServiceValidationServlet {

	private static final long serialVersionUID = 75592777244742205L;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		handleRequest(req, resp, true);

	}
}
