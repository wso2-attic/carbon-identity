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

package org.wso2.carbon.identity.relyingparty.saml;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.crypto.SecretKey;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.processor.EncryptedKeyProcessor;
import org.apache.ws.security.util.DOM2Writer;
import org.apache.ws.security.util.WSSecurityUtil;
import org.apache.xml.security.Init;
import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.utils.EncryptionConstants;
import org.opensaml.DefaultBootstrap;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.SignatureValidator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.relyingparty.RelyingPartyData;
import org.wso2.carbon.identity.relyingparty.RelyingPartyException;
import org.wso2.carbon.identity.relyingparty.TokenVerifierConstants;
import org.wso2.carbon.identity.relyingparty.saml.tokens.SAML1TokenHolder;
import org.wso2.carbon.identity.relyingparty.saml.tokens.SAML2TokenHolder;
import org.wso2.carbon.identity.relyingparty.saml.tokens.TokenHolder;

/**
 * A SAML token is sent to a web application in a CardSpace login attempt and this can be used for
 * decryption and verification of those tokens.
 */
public class SAMLTokenVerifier {

	private static Log log = LogFactory.getLog(SAMLTokenVerifier.class);
	private Hashtable<String, String> attributeTable = new Hashtable<String, String>();
	private List<X509Certificate> certificates = new ArrayList<X509Certificate>();
	private Element keyInfoElement = null;
	private String issuerName = null;
	private boolean isMultipleValues = false;
	private X509Certificate signingCert = null;

	static {
		try {
			DefaultBootstrap.bootstrap();
			Init.init();
		} catch (ConfigurationException e) {
			log.error("SAMLTokenConsumerBootstrapFailure", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Decrypt the given token (as a <code>java.lang.String</code> with the given private key.
	 * 
	 * @param token Serialized SAML token
	 * @param serviceKey Private key to be used for decryption.
	 * @return Decrypted SAML token element.
	 * @throws RelyingPartyException
	 */
	public Element decryptToken(String token, PrivateKey serviceKey) throws RelyingPartyException {
		ByteArrayInputStream bais = null;
		DocumentBuilderFactory dbf = null;
		Document doc = null;
		Element encryptedTokenElem = null;

		try {

			if (log.isDebugEnabled()) {
				log.debug("receivedEncryptedToken");
			}

			bais = new ByteArrayInputStream(token.getBytes());
			dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			doc = dbf.newDocumentBuilder().parse(bais);
			encryptedTokenElem = doc.getDocumentElement();

			// Decrypt element
			return this.decryptElement(serviceKey, encryptedTokenElem);

		} catch (Exception e) {
			log.error("Failed in decryption", e);
			throw new RelyingPartyException("verificationFailure", e);
		}
	}

	/**
	 * This method performs two actions 1) Decrypt the token 2) Verify the token
	 * 
	 * @param decryptedElem SAML token element
	 * @return true if verification is successful and false if unsuccessful.
	 * @throws SAMLException
	 */
	public boolean verifyDecryptedToken(Element decryptedElem, RelyingPartyData rpData)
			throws RelyingPartyException {

		if (log.isDebugEnabled()) {
			log.debug("verifyingDecryptedToken");
		}

		if (log.isDebugEnabled()) {
			try {
				String val = DOM2Writer.nodeToString(decryptedElem);
				log.debug(val);
				FileWriter writer = new FileWriter(new File("last_msg.xml"));
				writer.write(val.toCharArray());
				writer.flush();
				writer.close();
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		}

		try {

			String version = decryptedElem.getNamespaceURI();
			TokenHolder holder = null;

			if (version.equals(IdentityConstants.SAML10_URL)
					|| version.equals(IdentityConstants.SAML11_URL)) {
				holder = new SAML1TokenHolder(decryptedElem);
			} else if (version.equals(IdentityConstants.SAML20_URL)) {
				holder = new SAML2TokenHolder(decryptedElem);
			} else {
				throw new RelyingPartyException("invalidTokenType");
			}

			issuerName = holder.getIssuerName();
			if (issuerName == null) {
				throw new RelyingPartyException("issuerIsNull");
			}

			Signature sig = holder.getSAMLSignature();
			X509CredentialImpl credential = null;

			if (issuerName.equals(IdentityConstants.SELF_ISSUED_ISSUER)) {
				credential = (X509CredentialImpl) X509CredentialUtil
						.loadCredentialFromSignature(sig);
				this.keyInfoElement = sig.getKeyInfo().getDOM();
			} else {

				String validationPolicy = rpData.getValidatePolicy();

				String alias = null;
				URI uri = new URI(issuerName);
				alias = uri.getHost();

				KeyStore trustStore = rpData.getTrustStore();
				KeyStore systemStore = rpData.getSystemStore();

				if (trustStore != null && alias != null) {
					credential = (X509CredentialImpl) X509CredentialUtil
							.loadCredentialFromTrustStore(alias, trustStore);
				}

				boolean isLoadedFromMessage = false;
				if (credential == null) {
					credential = (X509CredentialImpl) X509CredentialUtil
							.loadCredentialFromSignature(sig);

					if (credential == null)
						throw new RelyingPartyException("credentialIsNull");

					isLoadedFromMessage = true;
				}

				if (!validationPolicy.equals(TokenVerifierConstants.PROMISCUOUS)) {

					this.signingCert = credential.getSigningCert();

					if (signingCert == null)
						throw new RelyingPartyException("signingCertNull");

					/*
					 * do certificate validation for blacklist, whitelist and cert-validity
					 */

					signingCert.checkValidity();

					if (isLoadedFromMessage) {
						if (!IssuerCertificateUtil.checkSystemStore(signingCert, systemStore)
								&& !IssuerCertificateUtil.checkSystemStore(signingCert, trustStore)) {
							return false;
						}
					}

					if (validationPolicy.equals(TokenVerifierConstants.BLACK_LIST)) {
						if (IssuerCertificateUtil.isBlackListed(rpData.getBlackList(), signingCert)) {
							return false;
						}
					} else if (validationPolicy.equals(TokenVerifierConstants.WHITE_LIST)) {
						if (!IssuerCertificateUtil
								.isWhiteListed(rpData.getWhiteList(), signingCert)) {
							return false;
						}
					}
				}
			}

			SignatureValidator validator = new SignatureValidator(credential);
			validator.validate(sig);
			holder.populateAttributeTable(this.attributeTable);

		} catch (Exception e) {
			log.debug(e);
			throw new RelyingPartyException("errorInTokenVerification", e);
		}

		if (log.isDebugEnabled()) {
			log.debug("verifyingDecryptedTokenDone");
		}

		// everything is fine :D
		return true;
	}

	private Element decryptElement(PrivateKey privKey, Element encryptedToken) throws Exception {

		Element kiElem = null;
		Element encrKeyElem = null;
		EncryptedKeyProcessor encrKeyProcessor = null;
		SecretKey secretKey = null;
		XMLCipher cipher = null;
		Document doc = null;

		if (log.isDebugEnabled()) {
			log.debug("decryptingToken");
		}

		kiElem = (Element) encryptedToken.getElementsByTagNameNS(WSConstants.SIG_NS, "KeyInfo")
				.item(0);
		encrKeyElem = (Element) kiElem.getElementsByTagNameNS(WSConstants.ENC_NS,
				EncryptionConstants._TAG_ENCRYPTEDKEY).item(0);

		encrKeyProcessor = new EncryptedKeyProcessor();
		encrKeyProcessor.handleEncryptedKey(encrKeyElem, privKey);

		secretKey = WSSecurityUtil.prepareSecretKey(EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES128,
				encrKeyProcessor.getDecryptedBytes());

		cipher = XMLCipher.getInstance();
		cipher.init(XMLCipher.DECRYPT_MODE, secretKey);

		doc = cipher.doFinal(encryptedToken.getOwnerDocument(), encryptedToken);

		if (log.isDebugEnabled()) {
			log.debug("decryptingTokenDone");
		}

		return doc.getDocumentElement();
	}

	public X509Certificate getSigningCert() {
		return signingCert;
	}

	/**
	 * Returns the list of attributes extracted from the SAMLAttributeStatements in the verified
	 * SAML assertion.
	 * 
	 * @return List of attributes as a <code>java.util.Hashtable</code>
	 */
	public Hashtable<String, String> getAttributeTable() {
		return attributeTable;
	}

	public List<X509Certificate> getCertificates() {
		return certificates;
	}

	public Element getKeyInfoElement() {
		return keyInfoElement;
	}

	public String getIssuerName() {
		return issuerName;
	}

	public void setIssuerName(String issuer) {
		this.issuerName = issuer;
	}

	public boolean isMultipleValues() {
		return isMultipleValues;
	}

	public void setMultipleValues(boolean isMultipleValues) {
		this.isMultipleValues = isMultipleValues;
	}

}