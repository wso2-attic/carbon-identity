package org.wso2.carbon.identity.sso.cas.handler;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.sso.cas.util.CASPageTemplates;

public abstract class AbstractLoginHandler {
	public abstract void handle(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException,
			IdentityApplicationManagementException;
	
	protected void showLoginError(HttpServletResponse resp, String errorCode, Locale locale) throws IOException {
		String errorMessage = ResourceBundle.getBundle(HandlerConstants.RESOURCE_BUNDLE, locale).getString(errorCode);
		resp.getWriter().write(
				CASPageTemplates.getInstance().showLoginError(errorMessage, locale)
		);
	}
}
