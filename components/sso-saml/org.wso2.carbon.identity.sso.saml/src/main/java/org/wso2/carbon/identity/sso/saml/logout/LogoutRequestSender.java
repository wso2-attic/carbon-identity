/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.sso.saml.logout;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.core.util.IdentityConfigParser;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.sso.saml.SAMLSSOConstants;
import org.wso2.carbon.identity.sso.saml.dto.SingleLogoutRequestDTO;
import org.wso2.carbon.identity.sso.saml.util.SAMLSSOUtil;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class is used to send logout requests to each and every session participant. It follows a fire and
 * forget approach where the task of sending each and every logout request is submitted to a threadpool
 * as a job. This class implements a singleton, because it is expensive to create thread pool for each
 * and every object.
 */
public class LogoutRequestSender {

    private static Log log = LogFactory.getLog(LogoutRequestSender.class);

    private static ExecutorService threadPool = Executors.newFixedThreadPool(2);

    private static LogoutRequestSender instance = new LogoutRequestSender();

    /**
     * A private constructor since we are implementing a singleton here
     */
    private LogoutRequestSender() {

    }

    /**
     * getInstance method of LogoutRequestSender, as it is a singleton
     *
     * @return LogoutRequestSender instance
     */
    public static LogoutRequestSender getInstance() {
        return instance;
    }

    /**
     * takes an array of SingleLogoutRequestDTO objects, creates and submits each of them as a task
     * to the thread pool
     *
     * @param singleLogoutRequestDTOs Array of SingleLogoutRequestDTO representing all the session participants
     */
    public void sendLogoutRequests(org.wso2.carbon.identity.sso.saml.dto.SingleLogoutRequestDTO[] singleLogoutRequestDTOs) {
        if (singleLogoutRequestDTOs == null) {
            return;
        }
        // For each logoutReq, create a new task and submit it to the thread pool.
        for (SingleLogoutRequestDTO reqDTO : singleLogoutRequestDTOs) {
            threadPool.submit(new LogoutReqSenderTask(reqDTO));
            if (log.isDebugEnabled()) {
                log.debug("A logoutReqSenderTask is assigned to the thread pool");

            }
        }
    }

    /**
     * This method is used to derive the port from the assertion consumer URL.
     *
     * @param assertionConsumerURL Assertion Consumer URL
     * @return Port, if mentioned in the URL, or else 443 as the default value
     * @throws MalformedURLException when the ACS is malformed.
     */
    private int derivePortFromAssertionConsumerURL(String assertionConsumerURL)
            throws URISyntaxException {
        int port = 443;    // use 443 as the default port
        try {
            URI uri = new URI(assertionConsumerURL);
            if (uri.getPort() != -1) {    // if the port is mentioned in the URL
                port = uri.getPort();
            } else if ("http".equals(uri.getScheme())) {  // if it is using http
                port = 80;
            }
        } catch (URISyntaxException e) {
            log.error("Error deriving port from the assertion consumer url", e);
            throw e;
        }
        return port;
    }

    /**
     * This class is used to model a single logout request that is being sent to a session participant.
     * It will send the logout req. to the session participant in its 'run' method when this job is
     * submitted to the thread pool.
     */
    private class LogoutReqSenderTask implements Runnable {

        private SingleLogoutRequestDTO logoutReqDTO;

        public LogoutReqSenderTask(SingleLogoutRequestDTO logoutReqDTO) {
            this.logoutReqDTO = logoutReqDTO;
        }

        @Override
        public void run() {
            List<NameValuePair> logoutReqParams = new ArrayList<NameValuePair>();
            StringBuffer logoutRequestWithSoapBinding = new StringBuffer();
            String decodedSAMLRequest = null;

            boolean propertySAMLSOAPBindingEnabled = IdentityConfigParser.getInstance()
                    .getConfiguration().containsKey(SAMLSSOConstants.SLO_SAML_SOAP_BINDING_ENABLED);
            boolean isSAMLSOAPBindingEnabled;

            if (propertySAMLSOAPBindingEnabled) {
                isSAMLSOAPBindingEnabled = Boolean.parseBoolean(IdentityConfigParser.getInstance()
                        .getConfiguration().get(SAMLSSOConstants.SLO_SAML_SOAP_BINDING_ENABLED).toString());
            } else {
                isSAMLSOAPBindingEnabled = false;
            }

            decodedSAMLRequest = logoutReqDTO.getLogoutResponse();

            if (isSAMLSOAPBindingEnabled) {
                decodedSAMLRequest = decodedSAMLRequest.replaceAll(SAMLSSOConstants.XML_TAG_REGEX, "").trim();
                logoutRequestWithSoapBinding.append(SAMLSSOConstants.START_SOAP_BINDING + decodedSAMLRequest + SAMLSSOConstants.END_SOAP_BINDING);
                // set the logout request
                logoutReqParams.add(new BasicNameValuePair(SAMLSSOConstants.SAML_REQUEST_PARAM_KEY,
                        SAMLSSOUtil.encode(logoutRequestWithSoapBinding.toString())));
            } else {
                // set the logout request
                logoutReqParams.add(new BasicNameValuePair(SAMLSSOConstants.SAML_REQUEST_PARAM_KEY, SAMLSSOUtil.encode(logoutReqDTO.getLogoutResponse())));
            }

            if (log.isDebugEnabled() && IdentityUtil.isTokenLoggable(IdentityConstants.IdentityTokens.SAML_REQUEST)) {
                log.debug("SAMLRequest : " + decodedSAMLRequest);
            }

            try {
                int port = derivePortFromAssertionConsumerURL(logoutReqDTO.getAssertionConsumerURL());
                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(logoutReqParams, SAMLSSOConstants.ENCODING_FORMAT);
                HttpPost httpPost = new HttpPost(logoutReqDTO.getAssertionConsumerURL());
                httpPost.setEntity(entity);
                httpPost.addHeader(SAMLSSOConstants.COOKIE_PARAM_KEY, SAMLSSOConstants.SESSION_ID_PARAM_KEY + logoutReqDTO.getRpSessionId());
                if (isSAMLSOAPBindingEnabled) {
                    httpPost.addHeader(SAMLSSOConstants.SOAP_ACTION_PARAM_KEY, SAMLSSOConstants.SOAP_ACTION);
                }
                TrustManager easyTrustManager = new X509TrustManager() {

                    @Override
                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] x509Certificates,
                            String s)
                            throws java.security.cert.CertificateException {
                        //overridden method, no method body needed here
                    }

                    @Override
                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] x509Certificates,
                            String s)
                            throws java.security.cert.CertificateException {
                        //overridden method, no method body needed here
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                };

                SSLContext sslContext = SSLContext.getInstance(SAMLSSOConstants.CRYPTO_PROTOCOL);
                sslContext.init(null, new TrustManager[]{easyTrustManager}, null);
                SSLSocketFactory sf = new SSLSocketFactory(sslContext);
                sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
                Scheme httpsScheme = new Scheme(SAMLSSOConstants.COM_PROTOCOL, sf, port);

                HttpClient httpClient = new DefaultHttpClient();
                httpClient.getConnectionManager().getSchemeRegistry().register(httpsScheme);

                HttpResponse response = null;
                boolean isSuccessfullyLogout = false;
                for (int currentRetryCount = 0; currentRetryCount < SAMLSSOUtil.getSingleLogoutRetryCount(); currentRetryCount++) {
                    int statusCode = 0;

                    //Completely consume the previous response before retrying
                    if (response != null) {
                        HttpEntity httpEntity = response.getEntity();
                        if (httpEntity != null && httpEntity.isStreaming()) {
                            InputStream instream = httpEntity.getContent();
                            if (instream != null)
                                instream.close();
                        }
                    }

                    // send the logout request as a POST
                    try {
                        response = httpClient.execute(httpPost);
                        statusCode = response.getStatusLine().getStatusCode();
                    } catch (IOException e) {
                        if (log.isDebugEnabled()) {
                            log.debug("Error while executing http request.", e);
                        }
                        // ignore this exception since retrying is enabled if response is null.
                    }
                    if (response != null && (SAMLSSOUtil.isHttpSuccessStatusCode(statusCode) || SAMLSSOUtil
                            .isHttpRedirectStatusCode(statusCode))) {
                        if (log.isDebugEnabled()) {
                            log.debug("single logout request is sent to : " + logoutReqDTO.getAssertionConsumerURL() +
                                    " is returned with " + HttpStatus.getStatusText(response.getStatusLine().getStatusCode()));
                        }
                        isSuccessfullyLogout = true;
                        break;
                    } else {
                        if (statusCode != 0) {
                            log.warn("Failed single logout response from " +
                                    logoutReqDTO.getAssertionConsumerURL() + " with status code " +
                                    HttpStatus.getStatusText(statusCode));
                        }
                        try {
                            synchronized (Thread.currentThread()) {
                                Thread.currentThread().wait(SAMLSSOUtil.getSingleLogoutRetryInterval());
                            }
                            log.info("Sending single log out request again with retry count " +
                                    (currentRetryCount + 1) + " after waiting for " +
                                    SAMLSSOUtil.getSingleLogoutRetryInterval() + " milli seconds to " +
                                    logoutReqDTO.getAssertionConsumerURL());
                        } catch (InterruptedException e) {
                            //Todo: handle this in better way.
                        }
                    }

                }
                if (!isSuccessfullyLogout) {
                    log.error("Single logout failed after retrying " + SAMLSSOUtil.getSingleLogoutRetryCount() +
                            " times with time interval " + SAMLSSOUtil.getSingleLogoutRetryInterval() + " in milli seconds.");
                }

            } catch (IOException e) {
                log.error("Error sending logout requests to : " +
                        logoutReqDTO.getAssertionConsumerURL(), e);
            } catch (GeneralSecurityException e) {
                log.error("Error registering the EasySSLProtocolSocketFactory", e);
            } catch (RuntimeException e) {
                log.error("Runtime exception occurred.", e);
            } catch (URISyntaxException e) {
                log.error("Error deriving port from the assertion consumer url", e);
            }
        }
    }


}

