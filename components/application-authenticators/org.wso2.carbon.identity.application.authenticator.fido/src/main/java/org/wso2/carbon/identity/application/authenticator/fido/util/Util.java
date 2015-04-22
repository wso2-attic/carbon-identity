package org.wso2.carbon.identity.application.authenticator.fido.util;

import org.apache.commons.logging.Log;

import javax.servlet.http.HttpServletRequest;

/**
 * Util class for FIDO authentication component.
 */
public class Util {
	public static void logTrace(String msg, Log log) {
		if (log.isTraceEnabled()) {
			log.trace(msg);
		}
	}

	public static String getOrigin(HttpServletRequest request) {
		//origin as appID eg.: http://example.com:8080
		return request.getScheme() + "://" + request.getServerName() + ":" +
		       request.getServerPort();
	}

	public static String getSafeText(String text) {
		if (text == null) {
			return text;
		}
		text = text.trim();
		if (text.indexOf('<') > -1) {
			text = text.replace("<", "&lt;");
		}
		if (text.indexOf('>') > -1) {
			text = text.replace(">", "&gt;");
		}
		return text;
	}
	public static String getUniqueUsername(HttpServletRequest request, String username){
		return request.getServerName() + "/" + username;
	}
}
