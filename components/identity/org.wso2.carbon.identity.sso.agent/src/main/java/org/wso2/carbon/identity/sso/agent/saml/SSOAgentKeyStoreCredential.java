/*
*  Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.sso.agent.saml;

import org.wso2.carbon.identity.sso.agent.exception.SSOAgentException;
import org.wso2.carbon.identity.sso.agent.util.SSOAgentConfigs;

import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

public class SSOAgentKeyStoreCredential implements SSOAgentCredential {

    private static Logger LOGGER = Logger.getLogger("InfoLogging");

    private static PublicKey publicKey = null;
    private static PrivateKey privateKey = null;
    private static X509Certificate entityCertificate = null;

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

    private static void readX509Credentials() throws SSOAgentException {

        InputStream keyStoreFile = SSOAgentConfigs.getKeyStore();
        String keyStorePassword = SSOAgentConfigs.getKeyStorePassword();
        String privateKeyAlias = SSOAgentConfigs.getPrivateKeyAlias();
        String privateKeyPassword = SSOAgentConfigs.getPrivateKeyPassword();
        String idpCertAlias = SSOAgentConfigs.getIdPCertAlias();
        
        KeyStore keyStore = readKeyStore(keyStoreFile, keyStorePassword, "JKS");
        X509Certificate cert = null;
        PrivateKey privateKey = null;
        
        try{
            if (privateKeyAlias != null) {
                if(SSOAgentConfigs.isRequestSigned()){
                    privateKey = (PrivateKey) keyStore.getKey(privateKeyAlias, privateKeyPassword.toCharArray());
                    
                    if(privateKey == null){
                        throw new SSOAgentException("RequestSigning is enabled, but cannot find private key with the alias " +
                                privateKeyAlias + " in the key store");
                    }
                }
            }
            
            cert = (X509Certificate) keyStore.getCertificate(idpCertAlias);
            if(cert == null){
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

    /**
     * get the key store instance
     *
     * @param is KeyStore InputStream
     * @param storePassword password of key store
     * @param storeType     key store type
     * @return KeyStore instant
     * @throws SSOAgentException if fails to load key store
     */
    private static KeyStore readKeyStore(InputStream is, String storePassword,
                                        String storeType) throws SSOAgentException {

        if (storePassword == null) {
            throw new SSOAgentException("KeyStore password can not be null");
        }
        if (storeType == null) {
            throw new SSOAgentException ("KeyStore Type can not be null");
        }

        try {
            KeyStore keyStore = KeyStore.getInstance(storeType);
            keyStore.load(is, storePassword.toCharArray());
            return keyStore;
        } catch (Exception e) {
            throw new SSOAgentException("Error while loading key store file" , e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) {
                    throw new SSOAgentException("Error while closing input stream of key store");
                }
            }
        }
    }
}
