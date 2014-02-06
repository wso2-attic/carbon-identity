/*
*  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.thrift.authentication.client.internal.pool;


import org.apache.commons.pool.BaseKeyedPoolableObjectFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TSSLTransportFactory;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.wso2.carbon.identity.thrift.authentication.client.ThriftAuthenticationClient;
import org.wso2.carbon.identity.thrift.authentication.client.exception.ThriftAuthenticationException;
import org.wso2.carbon.identity.thrift.authentication.client.internal.generatedCode.AuthenticatorService;
import org.wso2.carbon.identity.thrift.authentication.client.internal.util.ThriftAuthenticationClientConstants;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Thrift Authenticator Client factory for both ssl, https connections
 */
public class SecureClientPoolFactory extends BaseKeyedPoolableObjectFactory {

    private TSSLTransportFactory.TSSLTransportParameters params;
    private String trustStorePassword;
    private String trustStore;

    public SecureClientPoolFactory(String trustStorePassword, String trustStore) {
        this.trustStorePassword = trustStorePassword;
        this.trustStore = trustStore;
    }

    @Override
    public AuthenticatorService.Client makeObject(Object key)
            throws ThriftAuthenticationException, TTransportException {
        String[] keyElements = constructKeyElements((String) key);
        if (keyElements[0].equals(ThriftAuthenticationClient.Protocol.SSL.toString())) {
            if (params == null) {
                if (trustStore == null) {
                    trustStore = System.getProperty("javax.net.ssl.trustStore");
                    if (trustStore == null) {
                        throw new ThriftAuthenticationException("No trustStore found");
                    }
                }

                if (trustStorePassword == null) {
                    trustStorePassword = System.getProperty("javax.net.ssl.trustStorePassword");
                    if (trustStorePassword == null) {
                        throw new ThriftAuthenticationException("No trustStore password found");
                    }
                    //trustStorePassword = "wso2carbon";
                }

                params = new TSSLTransportFactory.TSSLTransportParameters();
                params.setTrustStore(trustStore, trustStorePassword);
            }


            TTransport receiverTransport = TSSLTransportFactory.
                    getClientSocket(keyElements[1], Integer.parseInt(keyElements[2]), 0, params);

            TProtocol protocol = new TBinaryProtocol(receiverTransport);
            return new AuthenticatorService.Client(protocol);
        } else {
            try {
                TrustManager easyTrustManager = new X509TrustManager() {
                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] x509Certificates,
                            String s)
                            throws java.security.cert.CertificateException {
                    }

                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] x509Certificates,
                            String s)
                            throws java.security.cert.CertificateException {
                    }

                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                };
//                String[] hostNameAndPort = keyElements[3].split(ThriftAuthenticationClientConstants.HOSTNAME_AND_PORT_SEPARATOR);

                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{easyTrustManager}, null);
                SSLSocketFactory sf = new SSLSocketFactory(sslContext);
                sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
                Scheme httpsScheme = new Scheme("https", sf, Integer.parseInt(keyElements[2]));

                DefaultHttpClient client = new DefaultHttpClient();
                client.getConnectionManager().getSchemeRegistry().register(httpsScheme);

                THttpClient tclient = new THttpClient("https://" + keyElements[1] + ":" + keyElements[2] + "/thriftAuthenticator", client);
                TProtocol protocol = new TCompactProtocol(tclient);
                AuthenticatorService.Client authClient = new AuthenticatorService.Client(protocol);
                tclient.open();
                return authClient;
            } catch (Exception e) {
                throw new ThriftAuthenticationException("Cannot create Secure client for " + keyElements[1] + ":" + keyElements[2], e);
            }
        }
    }

    private String[] constructKeyElements(String key) {
        return key.split("://|:");
    }

    @Override
    public boolean validateObject(Object key, Object obj) {
        AuthenticatorService.Client client = (AuthenticatorService.Client) obj;
        return client.getOutputProtocol().getTransport().isOpen();
    }


}
