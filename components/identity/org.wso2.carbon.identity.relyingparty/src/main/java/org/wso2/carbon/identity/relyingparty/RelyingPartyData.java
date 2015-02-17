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

package org.wso2.carbon.identity.relyingparty;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.identity.base.IdentityConstants.ServerConfig;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.relyingparty.saml.IssuerCertificateUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.List;

public class RelyingPartyData {

    // To enable attempted thread-safety using double-check locking
    private static Object lock = new Object();
    private static RelyingPartyData relyingPartyData = null;
    private static Log log = LogFactory.getLog(RelyingPartyData.class);
    private KeyStore systemStore = null;
    private PrivateKey privateKey = null;
    private String validatePolicy = null;
    private String issuerPolicy = null;
    private KeyStore trustStore = null;
    // One array contains one DN name
    private List<String>[] blackList = null;

    // One array contains one DN name
    private List<String>[] whiteList = null;

    private RelyingPartyData() throws IdentityException {
        loadData();
    }

    public static RelyingPartyData getInstance() throws IdentityException {

        // Enables attempted thread-safety using double-check locking
        if (relyingPartyData == null) {
            synchronized (lock) {
                if (relyingPartyData == null) {
                    relyingPartyData = new RelyingPartyData();
                }
            }
        }
        return relyingPartyData;
    }

    /**
     * This method read parameters from web.xml. Step 1 : Read my private key Step 2 : Read the
     * token validation policy Step 3 : Read parameters that is needed perform token validation
     * if(promiscuous) ->read nothing else if(blackList) ->read the blacklist and key store details
     * else if (whiteList)->read the white-list and key store details
     */
    private void loadData() throws IdentityException {

        ServerConfiguration serverConfig = null;
        KeyStore store = null;
        FileInputStream stream = null;

        serverConfig = ServerConfiguration.getInstance();
        String keyStore = serverConfig.getFirstProperty("Security.KeyStore.Location");
        String storeType = serverConfig.getFirstProperty("Security.KeyStore.Type");
        String storePass = serverConfig.getFirstProperty("Security.KeyStore.Password");
        String keyAlias = serverConfig.getFirstProperty("Security.KeyStore.KeyAlias");
        String keyPass = serverConfig.getFirstProperty("Security.KeyStore.KeyPassword");

        issuerPolicy = IdentityUtil.getProperty(ServerConfig.ISSUER_POLICY);

        if (issuerPolicy == null || issuerPolicy.trim().length() == 0) {
            // Set the default value
            issuerPolicy = TokenVerifierConstants.SELF_AND_MANGED;
        } else if (!(issuerPolicy.equals(TokenVerifierConstants.SELF_ONLY)
                || issuerPolicy.equals(TokenVerifierConstants.MANGED_ONLY) || issuerPolicy
                .equals(TokenVerifierConstants.SELF_AND_MANGED))) {
            throw new IdentityException("Invalid Issuer Policy!");
        }

        try {
            store = KeyStore.getInstance(storeType);
            stream = new FileInputStream(keyStore);
            store.load(stream, storePass.toCharArray());
            privateKey = (PrivateKey) store.getKey(keyAlias, keyPass.toCharArray());
            trustStore = store;
        } catch (Exception e) {
            throw new IdentityException("Cannot load the private key", e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    log.error("Error while closing system keystore file", e);
                }
            }
        }

        // Step 2: Reading Token validate policy - Promiscuous/BlackList/WhiteList/CertValidate

        // Promiscuous - In this mode, all tokens that has a valid signature are allowed
        // CertValidate - In this mode, all tokens that has a valid signature by an IdP who has a
        // trusted certificate are allowed
        // WhiteList - First CertValidity checked and after that if the issuer
        // DN is in the white list, the token is allowed
        // BlackList - First CertValidity checked and after that if the issuer
        // DN is not listed in the BlackList, the token is allowed

        validatePolicy = IdentityUtil.getProperty(ServerConfig.TOKEN_VALIDATE_POLICY);

        if (validatePolicy == null || validatePolicy.trim().length() == 0) {
            validatePolicy = TokenVerifierConstants.CERT_VALIDATE;
        }

        // Step 3: Reading parameters of each policy

        if (validatePolicy.equals(TokenVerifierConstants.BLACK_LIST)) {
            String value = IdentityUtil.getProperty(ServerConfig.BLACK_LIST);
            if (value != null) {
                blackList = readBlackWhiteList(value);
            }
        } else if (validatePolicy.equals(TokenVerifierConstants.WHITE_LIST)) {
            String value = IdentityUtil.getProperty(ServerConfig.WHITE_LIST);
            if (value != null) {
                whiteList = readBlackWhiteList(value);
            }
        }

        if (validatePolicy.equals(TokenVerifierConstants.WHITE_LIST)
                || validatePolicy.equals(TokenVerifierConstants.BLACK_LIST)
                || validatePolicy.equals(TokenVerifierConstants.CERT_VALIDATE)) {

            String javaHome = null;
            String defaultKeyStore = null;
            FileInputStream fileStream = null;
            KeyStore sysKS = null;

            String defaultStorePass = IdentityUtil.getProperty(ServerConfig.SYSTEM_KEY_STORE_PASS);
            if (defaultStorePass == null) {
                // assume that it hasn't been changed
                defaultStorePass = "changeit";
            }

            javaHome = System.getenv("JAVA_HOME");

            if (javaHome == null) {
                throw new IdentityException("Cannot find JAVA_HOME");
            }

            defaultKeyStore = IdentityUtil.getProperty(ServerConfig.SYSTEM_KEY_STORE);

            if (defaultKeyStore != null && defaultKeyStore.trim().length() > 0) {
                defaultKeyStore = javaHome + defaultKeyStore;
            } else {
                if (File.separator.equals("/")) {
                    defaultKeyStore = javaHome + TokenVerifierConstants.CACERTS_STORE_UNIX;
                } else {
                    defaultKeyStore = javaHome + TokenVerifierConstants.CACERTS_STORE_WIN;
                }
            }

            try {
                fileStream = new FileInputStream(defaultKeyStore);
                sysKS = KeyStore.getInstance("JKS");
                sysKS.load(fileStream, defaultStorePass.toCharArray());
                this.systemStore = sysKS;
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new IdentityException("Cannot load system key store");
            } finally {
                if (fileStream != null) {
                    try {
                        fileStream.close();
                    } catch (IOException e) {
                        log.error("Error while closing system keystore file", e);
                    }
                }
            }
        }
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    protected void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    public String getValidatePolicy() {
        return validatePolicy;
    }

    protected void setValidatePolicy(String validatePolicy) {
        this.validatePolicy = validatePolicy;
    }

    public String getIssuerPolicy() {
        return issuerPolicy;
    }

    protected void setIssuerPolicy(String issuerPolicy) {
        this.issuerPolicy = issuerPolicy;
    }

    public KeyStore getTrustStore() {
        return trustStore;
    }

    protected void setTrustStore(KeyStore trustStore) {
        this.trustStore = trustStore;
    }

    public List<String>[] getBlackList() {
        return Arrays.copyOf(blackList, blackList.length);
    }

    protected void setBlackList(List<String>[] blackList) {
        this.blackList = Arrays.copyOf(blackList, blackList.length);
    }

    public List<String>[] getWhiteList() {
        return Arrays.copyOf(whiteList, whiteList.length);
    }

    protected void setWhiteList(List<String>[] whiteList) {
        this.whiteList = Arrays.copyOf(whiteList, whiteList.length);
    }

    public KeyStore getSystemStore() {
        return systemStore;
    }

    protected void setSystemStore(KeyStore systemStore) {
        this.systemStore = systemStore;
    }

    private List<String>[] readBlackWhiteList(String paramString) {
        List<String>[] dnList = null;
        String[] array = paramString.split("\\},\\{");

        if (array != null) {
            if ((array.length > 1) && array[0].startsWith("{")) {
                StringBuffer buff = new StringBuffer(array[0]);
                buff.deleteCharAt(0);
                array[0] = buff.toString();
            }
            int lastIndex = array.length - 1;
            if ((array.length > 1) && array[lastIndex].endsWith("}")) {
                StringBuffer buff = new StringBuffer(array[lastIndex]);
                buff.deleteCharAt(buff.length() - 1);
                array[lastIndex] = buff.toString();
            }

            dnList = new List[array.length];
            for (int i = 0; i < array.length; i++) {
                List<String> lst = IssuerCertificateUtil.getDNOfIssuer(array[i]);
                dnList[i] = lst;
            }
        }
        return dnList;
    }

}