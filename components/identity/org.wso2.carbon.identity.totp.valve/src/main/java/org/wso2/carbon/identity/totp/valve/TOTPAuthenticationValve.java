package org.wso2.carbon.identity.totp.valve;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;


public class TOTPAuthenticationValve extends ValveBase {

	private static Log log = LogFactory.getLog(TOTPAuthenticationValve.class);

	Map<Integer, TOTPAuthenticationHandler> totpAuthenticationHandlers = new TreeMap<Integer, 
			TOTPAuthenticationHandler>();

	@Override
	public void invoke(Request request, Response response) throws IOException, ServletException {

		if (!Constants.CONTEXT_PATH.equals(request.getContextPath().trim())) {
			getNext().invoke(request, response);
			return;
		}
		TOTPAuthenticationHandler authenticationHandler = getAuthenticator(request);

		if (authenticationHandler != null) {
			if (!authenticationHandler.isAuthenticated(request)) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}
		}
		getNext().invoke(request, response);
	}

	@Override
	protected void initInternal() throws LifecycleException {
		super.initInternal();
		BasicAuthHandler basicAuthHandler = new BasicAuthHandler();
		basicAuthHandler.setDefaultPriority();
		totpAuthenticationHandlers.put(basicAuthHandler.getPriority(), basicAuthHandler);

		OAuthHandler oAuthHandler = new OAuthHandler();
		oAuthHandler.setDefaultPriority();
		oAuthHandler.setDefaultAuthzServer();
		totpAuthenticationHandlers.put(oAuthHandler.getPriority(), oAuthHandler);
	}

	private TOTPAuthenticationHandler getAuthenticator(Request request) {
		for (TOTPAuthenticationHandler authenticationHandler : totpAuthenticationHandlers.values()) {
			if (authenticationHandler.canHandler(request)) {
				return authenticationHandler;
			}
		}
		return null;
	}
}
