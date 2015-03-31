/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.provider.openid.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;

import javax.crypto.SecretKey;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.ws.security.WSConstants;
import org.apache.ws.security.processor.EncryptedKeyProcessor;
import org.apache.ws.security.util.WSSecurityUtil;
import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.utils.EncryptionConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.wso2.carbon.base.ServerConfiguration;

public class TokenDecrypter {

	public static Element decryptToken(String token) throws Exception {

		ByteArrayInputStream bais = new ByteArrayInputStream(token.getBytes());
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		Document doc = dbf.newDocumentBuilder().parse(bais);
		Element encryptedTokenElem = doc.getDocumentElement();

		// Decrypt element
		return TokenDecrypter.decryptElement(encryptedTokenElem);

	}

	private static Element decryptElement(Element encryptedToken) throws Exception {

		ServerConfiguration serverConfig = ServerConfiguration.getInstance();
		PrivateKey key = null;
		String keyStoreFile = null;
		String privateKeyPass = null;
		String privateKeyAlias = null;
		String keyStorePass = null;
		String type = null;
		byte[] content = null;

		keyStoreFile = serverConfig.getFirstProperty("Security.KeyStore.Location");
		keyStorePass = serverConfig.getFirstProperty("Security.KeyStore.Password");
		type = serverConfig.getFirstProperty("Security.KeyStore.Type");
		privateKeyAlias = serverConfig.getFirstProperty("Security.KeyStore.KeyAlias");
		privateKeyPass = serverConfig.getFirstProperty("Security.KeyStore.KeyPassword");

		content = readBytesFromFile(keyStoreFile);

		KeyStore keyStore = KeyStore.getInstance(type);
		keyStore.load(new ByteArrayInputStream(content), keyStorePass.toCharArray());

		key = (PrivateKey) keyStore.getKey(privateKeyAlias, privateKeyPass.toCharArray());

		Element kiElem = (Element) encryptedToken.getElementsByTagNameNS(WSConstants.SIG_NS,
				"KeyInfo").item(0);
		Element encrKeyElem = (Element) kiElem.getElementsByTagNameNS(WSConstants.ENC_NS,
				EncryptionConstants._TAG_ENCRYPTEDKEY).item(0);

		EncryptedKeyProcessor encrKeyProcessor = new EncryptedKeyProcessor();
		encrKeyProcessor.handleEncryptedKey(encrKeyElem, key);

		SecretKey secretKey = WSSecurityUtil.prepareSecretKey(
				EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES128, encrKeyProcessor
						.getDecryptedBytes());

		XMLCipher cipher = XMLCipher.getInstance();
		cipher.init(XMLCipher.DECRYPT_MODE, secretKey);

		Document doc = cipher.doFinal(encryptedToken.getOwnerDocument(), encryptedToken);

		return doc.getDocumentElement();
	}

	private static byte[] readBytesFromFile(String filePath) throws IOException {
		InputStream inputStream = null;
		File file = new File(filePath);
		long length;
		byte[] bytes;
		int offset = 0;
		int numRead = 0;

		try {
			inputStream = new FileInputStream(file);
			length = file.length();
			bytes = new byte[(int) length];

			while (offset < bytes.length
					&& (numRead = inputStream.read(bytes, offset, bytes.length - offset)) >= 0) {
				offset += numRead;
			}
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
		}

		return bytes;
	}
}