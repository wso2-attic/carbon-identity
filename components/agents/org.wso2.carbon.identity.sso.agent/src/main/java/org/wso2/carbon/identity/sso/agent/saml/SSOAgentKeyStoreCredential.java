/*
 * Copyright (c) 2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 *
 */

package org.wso2.carbon.identity.sso.agent.saml;

import org.wso2.carbon.identity.sso.agent.exception.SSOAgentException;
import org.wso2.carbon.identity.sso.agent.util.SSOAgentConfigs;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;

public class SSOAgentKeyStoreCredential implements SSOAgentCredential {

    private static PublicKey publicKey = null;
    private static PrivateKey privateKey = null;
    private static X509Certificate entityCertificate = null;

    private static void readX509Credentials() throws SSOAgentException {

        String privateKeyAlias = SSOAgentConfigs.getPrivateKeyAlias();
        String privateKeyPassword = SSOAgentConfigs.getPrivateKeyPassword();
        String idpCertAlias = SSOAgentConfigs.getIdPCertAlias();

        KeyStore keyStore = SSOAgentConfigs.getKeyStore();
        X509Certificate cert = null;
        PrivateKey privateKey = null;

        try {

            if (privateKeyAlias != null && SSOAgentConfigs.isRequestSigned()) {
                privateKey = (PrivateKey) keyStore.getKey(privateKeyAlias, privateKeyPassword.toCharArray());

                if (privateKey == null) {
                    throw new SSOAgentException("RequestSigning is enabled, but cannot find private key with the alias " +
                            privateKeyAlias + " in the key store");
                }
            }


            cert = (X509Certificate) keyStore.getCertificate(idpCertAlias);
            if (cert == null) {
                throw new SSOAgentException("Cannot find IDP certificate with the alias " + idpCertAlias + " in the trust store");
            }
        } catch (KeyStoreException e) {
            throw new SSOAgentException("Error when reading keystore", e);
        } catch (UnrecoverableKeyException e) {
            throw new SSOAgentException("Error when reading keystore", e);
        } catch (NoSuchAlgorithmException e) {
            throw new SSOAgentException("Error when reading keystore", e);
        }

        publicKey = cert.getPublicKey();
        SSOAgentKeyStoreCredential.privateKey = privateKey;
        entityCertificate = cert;
    }

    @Override
    public void init() throws SSOAgentException {
        readX509Credentials();
    }

    @Override
    public PublicKey getPublicKey() {
        return publicKey;
    }

    @Override
    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    @Override
    public X509Certificate getEntityCertificate() {
        return entityCertificate;
    }
}
