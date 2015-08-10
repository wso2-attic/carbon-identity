package org.wso2.carbon.identity.sso.cas.handler;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class AbstractValidationHandler {
	
	public void handle(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		String responseString = buildResponse(req);
		
		resp.getWriter().write(responseString);
	}
	
	protected abstract String buildResponse(HttpServletRequest req);
}
