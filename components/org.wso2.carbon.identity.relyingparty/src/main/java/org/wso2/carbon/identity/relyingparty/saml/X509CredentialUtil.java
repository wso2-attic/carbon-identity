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
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateFactory;
import java.util.Iterator;
import java.util.List;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.dom.factory.OMDOMFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.utils.Base64;
import org.opensaml.xml.security.x509.X509Credential;
import org.opensaml.xml.signature.Exponent;
import org.opensaml.xml.signature.KeyInfo;
import org.opensaml.xml.signature.KeyValue;
import org.opensaml.xml.signature.Modulus;
import org.opensaml.xml.signature.RSAKeyValue;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.X509Certificate;
import org.opensaml.xml.signature.X509Data;
import org.w3c.dom.Element;
import org.wso2.carbon.identity.relyingparty.RelyingPartyException;

/**
 * This class creates the X509CredentialImpl that is needed to verify the signature.
 */
public class X509CredentialUtil {

	public static KeyStore systemKeyStore = null;
	public static final BigInteger DEFAULT_EXPONENET = new BigInteger("65537");
	private static Log log = LogFactory.getLog(X509CredentialUtil.class);

	/**
	 * Creates the X509Credential from the TrustStore certificate.
	 */
	public static X509Credential loadCredentialFromTrustStore(String alias, KeyStore trustStore)
			throws RelyingPartyException {
		X509Credential credential = null;
		java.security.cert.X509Certificate cert = null;

		try {
			if (trustStore.containsAlias(alias)) {
				cert = (java.security.cert.X509Certificate) trustStore.getCertificate(alias);
				credential = new X509CredentialImpl(cert);
			}
		} catch (KeyStoreException e) {
			log.error("Error while loading credentials from trust store", e);
			throw new RelyingPartyException("Error while loading credentials from trust store", e);
		}
		return credential;
	}

	/**
	 * Creates the certificate from the KeyInfo element.
	 */
	public static X509Credential loadCredentialFromSignature(Signature signature)
			throws RelyingPartyException {
		X509Credential credential = null;
		KeyInfo kinfo = signature.getKeyInfo();
		List<X509Data> dataList = null;
		List<KeyValue> keyValueList = null;

		if (kinfo == null) {
			return null;
		}

		try {
			dataList = kinfo.getX509Datas();
			keyValueList = kinfo.getKeyValues();

			if (dataList.size() > 0) {

				if (dataList.size() > 1) {
					throw new RelyingPartyException("invalidKeyValueCount");
				}

				X509Data data = dataList.get(0);
				List<X509Certificate> certList = data.getX509Certificates();
				Iterator<X509Certificate> certIterator = certList.iterator();

				while (certIterator.hasNext()) {
					X509Certificate certElem = null;
					String certValue = null;
					byte[] certInBytes = null;
					ByteArrayInputStream inputStream = null;
					CertificateFactory factory = null;
					java.security.cert.X509Certificate x509Cert = null;

					certElem = (X509Certificate) certIterator.next();
					certValue = certElem.getValue();
					certInBytes = Base64.decode(certValue);
					inputStream = new ByteArrayInputStream(certInBytes);
					factory = CertificateFactory.getInstance("X509");
					x509Cert = (java.security.cert.X509Certificate) factory
							.generateCertificate(inputStream);
					credential = new X509CredentialImpl(x509Cert);
				}
			} else if (keyValueList.size() > 0) {

				if (keyValueList.size() > 1) {
					throw new RelyingPartyException("invalidKeyValueCount");
				}

				KeyValue val = null;
				RSAKeyValue rsaKey = null;
				Element modElem = null;
				Element expElem = null;
				Element elem = null;
				OMElement omElem = null;
				BigInteger mod = null;
				BigInteger exp = null;

				val = (KeyValue) keyValueList.get(0);
				rsaKey = val.getRSAKeyValue();
				elem = rsaKey.getDOM();
				omElem = (OMElement) new OMDOMFactory().getDocument().importNode(elem, true);
				modElem = (Element) omElem.getFirstChildWithName(Modulus.DEFAULT_ELEMENT_NAME);
				expElem = (Element) omElem.getFirstChildWithName(Exponent.DEFAULT_ELEMENT_NAME);
				mod = Base64.decodeBigIntegerFromElement(modElem);

				if (expElem != null) {
					exp = Base64.decodeBigIntegerFromElement(expElem);
				} else {
					exp = DEFAULT_EXPONENET;
				}

				credential = new X509CredentialImpl(mod, exp);
			} else {
				if (log.isDebugEnabled()) {
					log.debug("unknown key info");
				}
			}
        } catch (RuntimeException e) {
            throw e;
		} catch (Exception e) {
			log.error("Error while loading credentials from signature", e);
			throw new RelyingPartyException("Error while loading credentials from signature", e);
		}

		return credential;
	}
}