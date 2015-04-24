/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
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
