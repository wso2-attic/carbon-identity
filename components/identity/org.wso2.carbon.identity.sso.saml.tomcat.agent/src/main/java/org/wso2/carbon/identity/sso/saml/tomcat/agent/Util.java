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

package org.wso2.carbon.identity.sso.saml.tomcat.agent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Random;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class Util {

    public static final Log log = LogFactory.getLog(Util.class);

	/**
     * Generates a unique Id for Authentication Requests
     *
     * @return generated unique ID
     */
    public static String createID() {

        byte[] bytes = new byte[20]; // 160 bit
        
        new Random().nextBytes(bytes);
        
        char[] charMapping = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p'};

        char[] chars = new char[40];

        for (int i = 0; i < bytes.length; i++) {
            int left = (bytes[i] >> 4) & 0x0f;
            int right = bytes[i] & 0x0f;
            chars[i * 2] = charMapping[left];
            chars[i * 2 + 1] = charMapping[right];
        }

        return String.valueOf(chars);
    }
    

	/**
	 * Decoding and deflating the encoded AuthReq
	 *
	 * @param encodedStr
	 *            encoded AuthReq
	 * @return decoded AuthReq
	 */
	public static String decode(String encodedStr) throws SSOAgentException {
		try {
			org.apache.commons.codec.binary.Base64 base64Decoder = new org.apache.commons.codec.binary.Base64();
			byte[] xmlBytes = encodedStr.getBytes("UTF-8");
			byte[] base64DecodedByteArray = base64Decoder.decode(xmlBytes);

			try {
				Inflater inflater = new Inflater(true);
				inflater.setInput(base64DecodedByteArray);
				byte[] xmlMessageBytes = new byte[5000];
				int resultLength = inflater.inflate(xmlMessageBytes);

				if (inflater.getRemaining() > 0) {
					throw new RuntimeException("didn't allocate enough space to hold "
							+ "decompressed data");
				}

				inflater.end();
				return new String(xmlMessageBytes, 0, resultLength, "UTF-8");

			} catch (DataFormatException e) {
				ByteArrayInputStream bais = new ByteArrayInputStream(base64DecodedByteArray);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				InflaterInputStream iis = new InflaterInputStream(bais);
				byte[] buf = new byte[1024];
				int count = iis.read(buf);
				while (count != -1) {
					baos.write(buf, 0, count);
					count = iis.read(buf);
				}
				iis.close();
				return new String(baos.toByteArray());
			}
		} catch (IOException e) {
			throw new SSOAgentException("Error when decoding the SAML Request.", e);
		}
	}


    /**
     * Get the X509CredentialImpl object for a particular tenant
     * @return X509CredentialImpl object containing the public certificate of that tenant
     * @throws SSOAgentException Error when creating X509CredentialImpl object
     */
    public static X509CredentialImpl getX509CredentialImpl() throws SSOAgentException {

        X509CredentialImpl credentialImpl;
        String keyStoreFile = SSOConfigs.getTrustStore();
        String keyStorePassword = SSOConfigs.getTrustStorePassword();
        String alias = SSOConfigs.getIdPCertAlias();
        KeyStore keyStore = getKeyStore(keyStoreFile, keyStorePassword, "JKS");
        try {
            java.security.cert.X509Certificate cert = null;
            if (alias != null) {
                cert = (X509Certificate) keyStore.getCertificate(alias);
                if(cert == null){
                    throw new SSOAgentException("Cannot find a certificate with the alias "
                            + alias + "in the trust store");
                }
            }
            credentialImpl = new X509CredentialImpl(cert);
        } catch (Exception e) {
            throw new SSOAgentException("Error instantiating an X509CredentialImpl object " +
                    "for the public cert", e);
        }

        return credentialImpl;
    }


    /**
     * get the key store instance
     *
     * @param location      location of key store
     * @param storePassword password of key store
     * @param storeType     key store type
     * @return KeyStore instant
     * @throws SSOAgentException if fails to load key store
     */
    private static KeyStore getKeyStore(String location, String storePassword,
                                        String storeType) throws SSOAgentException {

        File keyStoreFile = new File(location);
        if (!keyStoreFile.exists()) {
            throw new SSOAgentException("KeyStore can not be found at ' " + keyStoreFile + " '");
        }
        if (storePassword == null) {
             throw new SSOAgentException("KeyStore password can not be null");
        }
        if (storeType == null) {
             throw new SSOAgentException ("KeyStore Type can not be null");
        }
        BufferedInputStream bufferedInputStream = null;
        try {
            bufferedInputStream = new BufferedInputStream(new FileInputStream(keyStoreFile));
            KeyStore keyStore = KeyStore.getInstance(storeType);
            keyStore.load(bufferedInputStream, storePassword.toCharArray());
            return keyStore;
        } catch (Exception e) {
             throw new SSOAgentException("Error while loading trust store file" , e);
        } finally {
            if (bufferedInputStream != null) {
                try {
                    bufferedInputStream.close();
                } catch (IOException ignored) {
                     log.error("Error while closing input stream of key store");
                }
            }
        }
    }
}
