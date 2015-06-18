/*
 * Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.relyingparty.saml;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ws.security.components.crypto.X509NameTokenizer;
import org.wso2.carbon.identity.relyingparty.RelyingPartyException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IssuerCertificateUtil {

    private static final Log log = LogFactory.getLog(IssuerCertificateUtil.class);

    private IssuerCertificateUtil() {
    }

    /**
     * This method checks whether the certificate is present in the certificate store
     */
    public static boolean checkSystemStore(X509Certificate signedCert, KeyStore systemStore)
            throws RelyingPartyException {
        if (signedCert == null || systemStore == null) {
            throw new RelyingPartyException("invalidInputParams");
        }

        // validity period
        try {
            signedCert.checkValidity();
            return systemStore.containsAlias(signedCert.getIssuerDN().getName());
        } catch (CertificateExpiredException | CertificateNotYetValidException | KeyStoreException e) {
            throw new RelyingPartyException("Error while loading trusted key store", e);
        }
    }

    /**
     * Performs the black list check
     *
     * @param blackList Array of Lists. One Array element contains the Issuer's cert DN
     * @param cert
     * @return
     * @throws RelyingPartyException
     */
    public static boolean isBlackListed(List[] blackList, X509Certificate cert) throws RelyingPartyException {

        if (cert == null) {
            throw new RelyingPartyException("noCertInToken");
        }

        if (blackList != null && blackList.length > 0) {
            List certDN = getDNOfIssuer(cert.getIssuerDN().getName());
            for (int i = 0; i < blackList.length; i++) {
                List issuerDN = blackList[i];
                if (certDN.equals(issuerDN)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Do a white list check
     *
     * @param whiteList Array of Lists. One Array element contains the Issuer's cert DN
     * @param cert
     * @return
     * @throws RelyingPartyException
     */
    public static boolean isWhiteListed(List[] whiteList, X509Certificate cert) throws RelyingPartyException {

        if (cert == null) {
            throw new RelyingPartyException("noCertInToken");
        }

        if (whiteList != null && whiteList.length > 0) {
            List certDN = getDNOfIssuer(cert.getIssuerDN().getName());
            for (int i = 0; i < whiteList.length; i++) {
                List issuerDN = whiteList[i];
                if (certDN.equals(issuerDN)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Retrieves the CN of the subject of the given Certificate
     *
     * @param cert
     * @return
     */
    public static String getCNOfSubject(X509Certificate cert) {
        String distinguishedName = cert.getIssuerDN().getName();

        if (distinguishedName.contains("CN=")) {
            int beginIndex = distinguishedName.indexOf("CN=");
            int endIndex = distinguishedName.indexOf(",", beginIndex);
            return distinguishedName.substring(beginIndex + 3, endIndex).trim();
        }
        return null;
    }

    /**
     * Retrieves the DN Of Issuer
     *
     * @param inString
     * @return
     */
    public static List<String> getDNOfIssuer(String inString) {
        X509NameTokenizer nameTokenizer = new X509NameTokenizer(inString);
        List<String> tokens = new ArrayList<String>();

        while (nameTokenizer.hasMoreTokens()) {
            tokens.add(nameTokenizer.nextToken());
        }
        Collections.sort(tokens);
        return tokens;
    }

    /**
     * @param url
     * @param keyStore
     * @return
     * @throws Exception
     */
    public static boolean isWhiteListed(String url, KeyStore keyStore) throws Exception {
        Certificate certificate = null;
        certificate = readCertFromUrl(url);

        if (isInKeyStore((X509Certificate) certificate, keyStore)) {
            return true;
        }
        return false;
    }

    /**
     * @param url
     * @param keyStore
     * @return
     * @throws Exception
     */
    public static boolean isBlackListed(String url, KeyStore keyStore) throws Exception {
        Certificate certificate = null;
        certificate = readCertFromUrl(url);

        if (isInKeyStore((X509Certificate) certificate, keyStore)) {
            return true;
        }
        return false;
    }

    private static boolean isInKeyStore(X509Certificate signedCert, KeyStore keyStore) throws RelyingPartyException {

        if (signedCert == null || keyStore == null) {
            throw new RelyingPartyException("invalidInputParams");
        }

        // validity period
        try {
            signedCert.checkValidity();
            return keyStore.getCertificateAlias(signedCert) != null;
        } catch (CertificateExpiredException | CertificateNotYetValidException | KeyStoreException e) {
            throw new RelyingPartyException("Error while loading trusted key store", e);
        }
    }

    public static Certificate readCertFromUrl(String url) throws Exception {

        URL hostURL = null;
        String hostname = null;
        int port;
        SSLSocketFactory factory = null;
        SSLSocket socket = null;

        try {
            // Create the client socket
            hostURL = new URL(url);
            hostname = hostURL.getHost();

            // Check whether the url has a port stated explicitly. If its not present default to 443
            port = hostURL.getPort();
            if (port == -1) {
                port = 443;
            }
            factory = HttpsURLConnection.getDefaultSSLSocketFactory();
            socket = (SSLSocket) factory.createSocket(hostname, port);

            // Connect to the server
            socket.startHandshake();

            // Retrieve the server's certificate chain
            Certificate[] serverCerts = socket.getSession().getPeerCertificates();

            // The local certificate first followed by any certificate authorities.
            if (serverCerts != null && serverCerts.length > 0) {
                if (log.isDebugEnabled()) {
                    log.debug("Return any associated certificates suceessfully" + url);
                }
                return serverCerts[0];
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Does not return any associated certificates" + url);
                }
                return null;
            }

        } finally {
            // Close the socket
            if (socket != null) {
                socket.close();
            }
        }
    }
}