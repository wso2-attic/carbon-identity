/*
 * Copyright 2005-2008 WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.relyingparty.openid;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.relyingparty.OpenIDRelyingPartyData;
import org.wso2.carbon.identity.relyingparty.RelyingPartyException;
import org.wso2.carbon.identity.relyingparty.TokenVerifierConstants;
import org.wso2.carbon.identity.relyingparty.saml.IssuerCertificateUtil;

public class OpenIDUtil {

	private static OpenIDRelyingPartyData rpData;
	private static Log log = LogFactory.getLog(OpenIDUtil.class);

	/**
	 * When the RP hosted Tomcat is behind an Apache serever, OpenID verification fails since
	 * return_to url mismatches with the returning url. To avoid that - only when the Tomcat is
	 * behind an Apache frontend we need to provide a host/port mapping in the web.xml.
	 * 
	 * @param returnUrl
	 * @param data
	 * @return mapped returing irl
	 * @throws RelyingPartyException
	 */
	public static String getMappedReturningUrl(String returnUrl, OpenIDRelyingPartyData data)
			throws RelyingPartyException {

		URI uri = null;
		URL url = null;
		String hostName = null;
		int portNumber;

		try {
			uri = new URI(returnUrl);
		} catch (URISyntaxException e) {
			log.error("Return_to url is not in the correct syntax", e);
			throw new RelyingPartyException("Return_to url is not in the correct syntax", e);
		}

		try {
			url = uri.normalize().toURL();
		} catch (MalformedURLException e) {
			log.error("Return_to url is malformed", e);
			throw new RelyingPartyException("Return_to url is malformed", e);
		}

		hostName = url.getHost();
		portNumber = url.getPort();

		if (log.isDebugEnabled()) {
			log.debug("Hostname in the received return_to url:" + hostName);
			log.debug("Port number in the received return_to url:" + portNumber);
		}

		if (data != null) {

			if (log.isDebugEnabled()) {
				log.debug("Mapped host name facing Apache:" + data.getMappedHostName());
				log.debug("Mapped port number facing Apache:" + data.getMappedPortNumber());
				log.debug("Mapping host name facing Apache:" + data.getMappingHostName());
				log.debug("Mapping port number facing Apache:" + data.getMappingPortNumber());
			}

			if (data.getMappedHostName() != null && data.getMappingHostName() != null) {
				if (data.getMappingHostName().equals(url.getHost())) {
					hostName = data.getMappedHostName();
				}
			}

			if (data.getMappedPortNumber() != null && data.getMappingPortNumber() != null) {
				if (Integer.parseInt(data.getMappingPortNumber()) == url.getPort()) {
					portNumber = Integer.parseInt(data.getMappedPortNumber());
				}
			}
		}

		try {

			if ((url.getProtocol().toLowerCase().equals("http") && portNumber == 80)
					|| (url.getProtocol().toLowerCase().equals("https") && portNumber == 443)) {
				url = new URL(url.getProtocol().toLowerCase(), hostName, url.getPath());
			} else {
				url = new URL(url.getProtocol().toLowerCase(), hostName, portNumber, url.getPath());
			}

			if (log.isDebugEnabled()) {
				log.debug("Formatted return_to url : " + url.toString());
			}

			return url.toString();
		} catch (MalformedURLException e) {
			log.error("Return_to url is malformed", e);
			throw new RelyingPartyException("Return_to url is malformed", e);
		}
	}

	/**
	 * 
	 * @param url
	 * @return
	 * @throws Exception
	 */
	public static boolean isOpenIDProviderTrusted(String url) throws Exception {

		if (rpData.getOpValidationPolicy() == null) {
			return true;
		} else if (rpData.getOpValidationPolicy().equals(TokenVerifierConstants.WHITE_LIST)) {
			if (IssuerCertificateUtil.isWhiteListed(url, rpData.getOpKeyStore())) {
				if (log.isDebugEnabled()) {
					log.debug("Whitelisted OpenID Provider: " + url);
				}
				return true;
			}
		} else if (rpData.getOpValidationPolicy().equals(TokenVerifierConstants.BLACK_LIST)) {
			if (!IssuerCertificateUtil.isBlackListed(url, rpData.getOpKeyStore())) {
				if (log.isDebugEnabled()) {
					log.debug("Blacklisted OpenID Provider: " + url);
				}
				return true;
			}
		}

		return false;
	}

	public static void setRpData(OpenIDRelyingPartyData rpData) {
		OpenIDUtil.rpData = rpData;
	}
}