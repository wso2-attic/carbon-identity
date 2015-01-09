/*
* Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.application.authentication.endpoint.util;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Map;

/**
 * Client for calling Admin Services with mutual ssl authentication
 */
public class TenantMgtAdminServiceClient {

	/**
	 * Logger for MutualSSLClient class
	 */
	private static final Log log = LogFactory.getLog(TenantMgtAdminServiceClient.class);

	/**
	 * Default keystore type of the client
	 */
	private static String keyStoreType = "JKS";

	/**
	 * Default truststore type of the client
	 */
	private static String trustStoreType = "JKS";

	/**
	 * Default keymanager type of the client
	 */
	private static String keyManagerType = "SunX509";

	/**
	 * Default trustmanager type of the client
	 */
	private static String trustManagerType = "SunX509";

	/**
	 * Default ssl protocol for client
	 */
	private static String protocol = "SSLv3";

	/**
	 * HTTP POST
	 */
	private static final String HTTP_POST = "POST";

	private static KeyStore keyStore;
	private static KeyStore trustStore;
	private static char [] keyStorePassword;
	private static HttpsURLConnection httpsURLConnection;
	private static SSLSocketFactory sslSocketFactory;

	/**
	 * Load key store with given keystore.jks
	 *
	 * @param keyStorePath Path to keystore
	 * @param keyStorePassoword Password of keystore
	 * @throws KeyStoreException
	 * @throws IOException
	 * @throws CertificateException
	 * @throws NoSuchAlgorithmException
	 */
	public static void loadKeyStore(String keyStorePath, String keyStorePassoword)
			throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
		keyStorePassword = keyStorePassoword.toCharArray();
		keyStore = KeyStore.getInstance(keyStoreType);
		keyStore.load(new FileInputStream(keyStorePath), keyStorePassword);
	}

	/**
	 * Load trust store with given .jks file
	 *
	 * @param trustStorePath Path to truststore
	 * @param trustStorePassoword Password of truststore
	 * @throws KeyStoreException
	 * @throws IOException
	 * @throws CertificateException
	 * @throws NoSuchAlgorithmException
	 */
	public static void loadTrustStore(String trustStorePath, String trustStorePassoword)
			throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {

		trustStore = KeyStore.getInstance(trustStoreType);
		trustStore.load(new FileInputStream(trustStorePath), trustStorePassoword.toCharArray());
	}

	/**
	 * Create basic SSL connection factory
	 *
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws KeyManagementException
	 * @throws IOException
	 * @throws UnrecoverableKeyException
	 */
	public static void initMutualSSLConnection() throws NoSuchAlgorithmException, KeyStoreException,
	        KeyManagementException, IOException, UnrecoverableKeyException {

		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(keyManagerType);
		keyManagerFactory.init(keyStore, keyStorePassword);
		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(trustManagerType);
		trustManagerFactory.init(trustStore);

		// Create and initialize SSLContext for HTTPS communication
		SSLContext sslContext = SSLContext.getInstance(protocol);
		sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
		sslSocketFactory = sslContext.getSocketFactory();
	}

	/**
	 * Send mutual ssl https post request and return data
	 *
	 * @param backendURL URL of the service
	 * @param message Message sent to the URL
	 * @param requestProps Requeset properties
	 * @return Received data
	 * @throws IOException
	 */
	public static String sendPostRequest(String backendURL, String message, Map<String, String> requestProps) {
		OutputStream outputStream = null;
		InputStream inputStream = null;
		BufferedReader reader = null;
		String response = null;
		URL url = null;

		try {
			url = new URL(backendURL);
			httpsURLConnection = (HttpsURLConnection) url.openConnection();
			httpsURLConnection.setSSLSocketFactory(sslSocketFactory);
			httpsURLConnection.setDoOutput(true);
			httpsURLConnection.setDoInput(true);
			httpsURLConnection.setRequestMethod(HTTP_POST);

			if (requestProps != null && requestProps.size() > 0) {
				for (Map.Entry<String, String> entry : requestProps.entrySet()) {
					httpsURLConnection.setRequestProperty(entry.getKey(), entry.getValue());
				}
			}
			outputStream = httpsURLConnection.getOutputStream();

			if (StringUtils.isNotEmpty(message)) {
				outputStream.write(message.getBytes());
			}
			inputStream = httpsURLConnection.getInputStream();
			reader = new BufferedReader(new InputStreamReader(inputStream));
			StringBuilder builder = new StringBuilder();
			String line;

			while (StringUtils.isNotEmpty(line = reader.readLine())) {
				builder.append(line);
			}
			response = builder.toString();
		} catch (IOException e) {
			log.error("Sending " + HTTP_POST + " request to URL : " + url + "failed.", e);
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
				if (inputStream != null) {
					inputStream.close();
				}
				if (outputStream != null) {
					outputStream.close();
				}
			} catch (IOException e) {
				log.error("Closing stream for " + url + " failed", e);
			}
		}
		return response.toString();
	}

	public static String getKeyStoreType() {
		return keyStoreType;
	}

	public static void setKeyStoreType(String keyStoreType) {
		TenantMgtAdminServiceClient.keyStoreType = keyStoreType;
	}

	public static String getTrustStoreType() {
		return trustStoreType;
	}

	public static void setTrustStoreType(String trustStoreType) {
		TenantMgtAdminServiceClient.trustStoreType = trustStoreType;
	}

	public static String getKeyManagerType() {
		return keyManagerType;
	}

	public static void settKeyManagerType(String keyManagerType) {
		TenantMgtAdminServiceClient.keyManagerType = keyManagerType;
	}

	public static String getTrustManagerType() {
		return trustManagerType;
	}

	public static void getTrustManagerType(String trustManagerType) {
		TenantMgtAdminServiceClient.trustManagerType = trustManagerType;
	}

	public static HttpsURLConnection getHttpsURLConnection() {
		return httpsURLConnection;
	}

	public static void setProtocol(String protocol) {
		TenantMgtAdminServiceClient.protocol = protocol;
	}
}
